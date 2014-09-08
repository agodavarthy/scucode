package OldCode;
//Working Clean-Up

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.compound.hyphenation.TernaryTree.Iterator;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DynamicInterestChanges_Jan30 {
	private Map<String, TestTermParams> testCorpusMap;
	private Map<String, TestTermParams> testCorpusMapBaseline1;
	private Map<String, TestTermParams> testCorpusMapBaseline2;
	private Map<String, TestTermParams> testCorpusMapBaseline3;
	private Map<String, Integer> areaCorpusMap;
	private Analyzer analyzer;
	private double alpha;
	private String yearT;
	private String yearTPlus;
	private BufferedReader resYearCorpusBuffRead;
	private BufferedReader resYearAreaCorpusBuffRead;
	private File testPath;
	private File areaPath;
	private File researcherPath;
	private JSONParser jsonParser;
	private HashMap researcherYearAreaCorpusMap;
	private HashMap resYearAreasCnt;
	private HashMap yearAreasCnt;
	private String researcherId;
	private String researcherName;
	private HashMap yearCorpusMap;
	private String researcherCorpus;
	private Directory directory;
	private IndexWriterConfig config;
	private IndexWriter iwriter;
	
    public static final FieldType TYPE_STORED = new FieldType();
    private static Set<String> terms = new HashSet<>();
    static {
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.tokenized();
        TYPE_STORED.storeTermVectorPayloads();
        TYPE_STORED.storeTermVectors();
    }
    
	private BufferedReader openResearcherYearCorpus() {
		BufferedReader br = null;
		try {
//			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_corpus.json"));
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_corpus_arnet_IR.json"));
			return br;
		} catch (IOException e) {
			e.printStackTrace();
			return br;
		}
	}

	private BufferedReader openResearcherYearAreaCorpus() {
		BufferedReader br = null;
		try {
//			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_areas.json"));
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_areas_arnet_IR.json"));
			return br;
		} catch (IOException e) {
			e.printStackTrace();
			return br;
		}
	}
	
	private BufferedReader openTestCorpus() {
		BufferedReader br = null;
		try {
//			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/test_year_corpus.json"));
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/test_year_corpus_arnet_IR.json"));
			return br;
		} catch (IOException e) {
			e.printStackTrace();
			return br;
		}
	}	
	
    private Map<String, Integer> getTermFrequencies(IndexReader reader, int docId, String content)
            throws IOException {
        Terms vector = reader.getTermVector(docId, content);
        TermsEnum termsEnum = null;
        termsEnum = vector.iterator(termsEnum);
        Map<String, Integer> frequencies = new HashMap<>();
        BytesRef text = null;
        while ((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();
            int freq = (int) termsEnum.totalTermFreq();
            frequencies.put(term, freq);
            terms.add(term);
        }
        return frequencies;
    }
    private RealVector toRealVector(Map<String, Integer> map) {
        RealVector vector = new ArrayRealVector(terms.size());
        int i = 0;
        for (String term : terms) {
            int value = map.containsKey(term) ? map.get(term) : 0;
            vector.setEntry(i++, value);
        }
        return (RealVector) vector.mapDivide(vector.getL1Norm());
    }
    private double getCosineSimilarity(RealVector v1, RealVector v2) {
    	double result = 0.0;
    	try{
    		result = (v1.dotProduct(v2)) / (v1.getNorm() * v2.getNorm());
    	}catch(Exception e){
    		e.printStackTrace();
    	}
        return result;
    }
    private double cosineDocumentSimilarity(int d1, int d2, IndexReader reader, String content) throws IOException {
        Map<String, Integer> f1 = getTermFrequencies(reader, d1, content);
        Map<String, Integer> f2 = getTermFrequencies(reader, d2, content);
        RealVector v1;
        RealVector v2;

        v1 = toRealVector(f1);
        v2 = toRealVector(f2);
        return getCosineSimilarity(v1, v2);
    }
  
    private double cosineDocumentSimilarity(int d1, int d2, IndexReader reader1, IndexReader reader2, String content1, String content2) throws IOException {
    	Map<String, Integer> f1 = getTermFrequencies(reader1, d1, content1);
    	Map<String, Integer> f2 = getTermFrequencies(reader2, d2, content2);
        RealVector v1;
        RealVector v2;

        v1 = toRealVector(f1);
        v2 = toRealVector(f2);
        return getCosineSimilarity(v1, v2);
    }

	private class TestTermParams {
		int freq;
		double prob;

		public TestTermParams(int freq, double prob) {
			this.freq = freq;
			this.prob = prob;
		}
	}	

	private Map<String, Integer> getAreaTerms(IndexReader ireader, int docID) throws IOException {
		areaCorpusMap = new HashMap<String, Integer>();
		Terms area_terms = ireader.getTermVector(docID, "areaCorpus");
		TermsEnum termsEnum = null;
		termsEnum = area_terms.iterator(termsEnum);
		BytesRef name = null;
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			areaCorpusMap.put(term, freq);
		}
		return areaCorpusMap;
	}
	private void extractFields(String resYearCorpus, String resYearWiseAreaCorpus) throws ParseException{
		Object resYearObj = jsonParser.parse(resYearCorpus);
		JSONObject resYearJSONObj = (JSONObject)resYearObj;
		System.out.println(resYearWiseAreaCorpus);
		Object resYearAreaObj = jsonParser.parse(resYearWiseAreaCorpus);
		JSONObject resYearAreaJSONObj = (JSONObject)resYearAreaObj;
		researcherYearAreaCorpusMap = (HashMap)resYearAreaJSONObj.get("year_area_corpus");
		System.out.println("ExtractFields:"+(HashMap)resYearAreaJSONObj.get("year_area_corpus"));
//		researcherYearAreaCorpusMap = (HashMap)resYearAreaJSONObj.get("year_corpus");
		researcherId = (String)resYearJSONObj.get("_id");
		researcherName = (String)resYearJSONObj.get("name");
		String resID = (String)resYearAreaJSONObj.get("_id");
		String resName = (String)resYearAreaJSONObj.get("name");
//		System.out.println(researcherId+":"+resID);
//		System.out.println(researcherName+":"+resName);
		if (resYearJSONObj.get("year_corpus") != null){
			System.out.println("OK");
			yearCorpusMap = (HashMap)resYearJSONObj.get("year_corpus");
			System.out.println(yearCorpusMap);
		}
	}
	private void indexTestCorpusYears(String yearTPlus) throws IOException, ParseException{
		JSONParser jsonParser = new JSONParser();
		BufferedReader testCorpusBuffRead = openTestCorpus();
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
    	Directory directory = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        IndexWriter iwriter;
    	String testCorpus;
		iwriter = new IndexWriter(directory, config);
		while ((testCorpus = testCorpusBuffRead.readLine()) != null){
			Object testCorpusObj = jsonParser.parse(testCorpus);
			JSONObject testCorpusJSONObj = (JSONObject)testCorpusObj;
			Document doc = new Document();
			HashMap yearCorpusMap = (HashMap)testCorpusJSONObj.get("test_corpus");
			String yearCorpus = (String)yearCorpusMap.get(yearTPlus);
			doc.add(new Field("id", yearTPlus, Field.Store.YES, Field.Index.NOT_ANALYZED));
			Field field = new Field("yearCorpus", yearCorpus, TYPE_STORED);
			doc.add(field);
			iwriter.addDocument(doc);
		}
		iwriter.close();
	}
	
	private void indexResearcherCorpus(String researcherId, String researcherCorpus) throws IOException{
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
    	Directory directory = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        IndexWriter iwriter;
		iwriter = new IndexWriter(directory, config);
		Document doc = new Document();
		doc.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
		Field field = new Field("researcherCorpus", researcherCorpus, TYPE_STORED);
		doc.add(field);
		iwriter.addDocument(doc);
		iwriter.close();		
	}

	private void indexAreaCorpus(String researcherId, String researcherName, String yearT, String areaStrT, String areaCorpusT) throws IOException{
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
    	directory = FSDirectory.open(path);
    	config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        
		iwriter = new IndexWriter(directory, config);
		Document docT = new Document();
		docT.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
		docT.add(new Field("researcherName", researcherName, Field.Store.YES, Field.Index.NOT_ANALYZED));
		docT.add(new Field("year", yearT, Field.Store.YES, Field.Index.NOT_ANALYZED));
		docT.add(new Field("areaName", areaStrT, Field.Store.YES, Field.Index.NOT_ANALYZED));
		Field fieldT = new Field("areaCorpus", areaCorpusT, TYPE_STORED);
		docT.add(fieldT);
		iwriter.addDocument(docT);
//		System.out.println("Indexed doc = " + areaStrT);
		iwriter.close();
	}
	
	private void deleteIndexes(String field) throws IOException{
		//Deleting Area Indices
		File path = null;
		if (field.equals("areas"))
			path = areaPath;
		else if(field.equals("test"))
			path = testPath;
		else if(field.equals("researcher"))
			path = researcherPath;

		directory = FSDirectory.open(path);
        config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
		iwriter = new IndexWriter(directory, config);
		iwriter.deleteAll();
		iwriter.close();
	}

	private void deleteDoc(int docID) throws IOException{
		//Deleting Area Indices
		
		directory = FSDirectory.open(areaPath);
        config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
    	Directory directory = FSDirectory.open(areaPath);
    	DirectoryReader ireader = DirectoryReader.open(directory);
		iwriter = new IndexWriter(directory, config);
		iwriter.tryDeleteDocument(ireader, docID);
		iwriter.close();
	}

	private void getYearAreasCntDic() throws IOException, ParseException{
		yearAreasCnt = new HashMap();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/year_areas_cnt_arnet_IR.json"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (br != null){
			String year_area_cnt;
			while ((year_area_cnt = br.readLine()) != null){
				Object yearAreaCntObj = jsonParser.parse(year_area_cnt);
				JSONObject yearAreaCntJSONObj = (JSONObject)yearAreaCntObj;
				String year  = (String)yearAreaCntJSONObj.get("_id");
				HashMap yearCorpusMap = (HashMap)yearAreaCntJSONObj.get("year_areas_cnt");
				if (yearCorpusMap != null){
					yearAreasCnt.put(year, yearCorpusMap);
				}
			}
//			System.out.println(yearAreasCnt.size());
		}
		
	}

	private void getResYearAreasCntDic() throws IOException, ParseException{
		resYearAreasCnt = new HashMap();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_areas_cnt_arnet_IR.json"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (br != null){
			String res_year_area_cnt;
			while ((res_year_area_cnt = br.readLine()) != null){
				Object resYearAreaCntObj = jsonParser.parse(res_year_area_cnt);
				JSONObject resYearAreaCntJSONObj = (JSONObject)resYearAreaCntObj;
				String researcherID  = (String)resYearAreaCntJSONObj.get("_id");
				String researcherName  = (String)resYearAreaCntJSONObj.get("name");
				HashMap resYearMap = (HashMap)resYearAreaCntJSONObj.get("year_areas_cnt");
				if (resYearMap != null){
					resYearAreasCnt.put(researcherID, resYearMap);
				}
				
			}
//			for (Object item : resYearAreasCnt.keySet()){
////				System.out.println(item.toString() + resYearAreasCnt.get(item));
//				String year = "2008";
//				HashMap hm = (HashMap)resYearAreasCnt.get(item);
//				if (hm.containsKey(year)){
//					System.out.println(hm.get(year));
//				}
//			}
		}
		
	}
	
	public DirectoryReader getReaderHandler(File path) throws IOException{
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	return ireader;
	}
	
    public void readIndex(File path, String id, String field) throws IOException{
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	System.out.println("The number of documents is " + ireader.maxDoc());
    	for (int i = 0; i < ireader.maxDoc(); i++){
    		Set<String> terms = new HashSet<>();
    		Document hitDoc = ireader.document(i);
    		System.out.print("terms for doc# " + hitDoc.get(id));
    		Terms vector = ireader.getTermVector(i, field);	
    	    if (vector != null){
    			System.out.println(" = " + vector.size());
    			TermsEnum termsEnum = null;
  				termsEnum = vector.iterator(termsEnum);
   				BytesRef name = null;
   				while ((name = termsEnum.next()) != null) {
   					String term = name.utf8ToString();
   					int freq = (int) termsEnum.totalTermFreq();
   					terms.add(term);
   					System.out.print(term + ":" + freq + " ");
   				}
   	       } else {
   	    	   System.out.println("vector is null");
   	       }
    	       System.out.println("*****************************");
    	   }
    }

	private void getTestCorpusTerms(String year) throws IOException {
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	IndexSearcher isearcher = new IndexSearcher(ireader);
		Term t = new Term("id", year);
		Query query = new TermQuery(t);
		double prob = 0.0;
		TopDocs topdocs = isearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;

		int doc_no = scoreDocs[0].doc;

		Terms year_terms = ireader.getTermVector(doc_no, "yearCorpus");
		TermsEnum termsEnum = null;
		termsEnum = year_terms.iterator(termsEnum);
		BytesRef name = null;
		testCorpusMap = new HashMap<String, TestTermParams>();
		testCorpusMapBaseline1 = new HashMap<String, TestTermParams>();
		testCorpusMapBaseline2 = new HashMap<String, TestTermParams>();
		testCorpusMapBaseline3 = new HashMap<String, TestTermParams>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			TestTermParams ttp1 = new TestTermParams(freq, prob);
			TestTermParams ttp2 = new TestTermParams(freq, prob);
			testCorpusMap.put(term, ttp1);
			testCorpusMapBaseline1.put(term, ttp2);
			testCorpusMapBaseline2.put(term, ttp2);
			testCorpusMapBaseline3.put(term, ttp2);
		}
	}

    private double computePerplexity(Map<String, TestTermParams> testCorpusMap){
    	double perplexity = 0.0;
    	double logSumProb = 0.0;
    	double l_perplexity = 0.0;
    	int totFreq = calculateTotTestCorpusTerms(testCorpusMap);
		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
			logSumProb += Math.log(ttp.prob);
		}
		l_perplexity = logSumProb / totFreq;
		perplexity = Math.pow(2, -l_perplexity);
    	return perplexity;
    }
    
    private double computeBaseline2Perplexity(Map<String, TestTermParams> testCorpusMap){
    	double perplexity = 0.0;
    	double logSumProb = 0.0;
    	double l_perplexity = 0.0;
    	int totFreq = calculateTotTestCorpusTerms(testCorpusMap);
		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
			logSumProb += Math.log(ttp.prob);
		}
		l_perplexity = logSumProb / totFreq;
		perplexity = Math.pow(2, -l_perplexity);
    	return perplexity;
    }
    
    private double computeBaseline3Perplexity(Map<String, TestTermParams> testCorpusMap){
    	double perplexity = 0.0;
    	double logSumProb = 0.0;
    	double l_perplexity = 0.0;
    	int totFreq = calculateTotTestCorpusTerms(testCorpusMap);
		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
			logSumProb += Math.log(ttp.prob);
		}
		l_perplexity = logSumProb / totFreq;
		perplexity = Math.pow(2, -l_perplexity);
    	return perplexity;
    }
    
	private int calculateTotFreq(Map<String, Integer> areaCorpusMap) {
		int totalFreq = 0;

		for (Object term : areaCorpusMap.keySet()) {
			totalFreq += (int) areaCorpusMap.get(term);
		}
		return totalFreq;
	}
	
	private int calculateTotTestCorpusTerms(Map<String, TestTermParams> testCorpusMap) {
		int totalFreq = 0;

		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
			totalFreq += ttp.freq;
		}
		return totalFreq;
	}
	
	public void computeBaseline1Map(IndexReader iAreaReader, int docID, double firstProb) throws IOException, InterruptedException{
		areaCorpusMap = getAreaTerms(iAreaReader, docID);
		int totFreq = calculateTotFreq(areaCorpusMap);
		getTestCorpusTerms(yearTPlus);
		TestTermParams ttp;
		for (Object term : testCorpusMapBaseline1.keySet()) {
			ttp = testCorpusMapBaseline1.get(term);
			int word_freq = ttp.freq;
			double thirdProb = ttp.prob;
			int area_term_freq = 0;
			if (areaCorpusMap.containsKey(term)) {
				area_term_freq = areaCorpusMap.get(term);
			}
			if (area_term_freq != 0) {
				thirdProb = alpha * area_term_freq / totFreq;
			} else {
				thirdProb = (1 - alpha) * 1 / totFreq;
			}
			thirdProb *= word_freq;
			double cummProb = firstProb * thirdProb;
			ttp.prob += cummProb;
			testCorpusMapBaseline1.put((String)term, ttp);
		}
	}

	public void computeBaseline2Map(IndexReader iAreaReader, int docID, double firstProb) throws IOException, InterruptedException{
		areaCorpusMap = getAreaTerms(iAreaReader, docID);
		int totFreq = calculateTotFreq(areaCorpusMap);
		getTestCorpusTerms(yearTPlus);
		TestTermParams ttp;
		for (Object term : testCorpusMapBaseline2.keySet()) {
			ttp = testCorpusMapBaseline2.get(term);
			int word_freq = ttp.freq;
			double thirdProb = ttp.prob;
			int area_term_freq = 0;
			if (areaCorpusMap.containsKey(term)) {
				area_term_freq = areaCorpusMap.get(term);
			}
			if (area_term_freq != 0) {
				thirdProb = alpha * area_term_freq / totFreq;
			} else {
				thirdProb = (1 - alpha) * 1 / totFreq;
			}
			thirdProb *= word_freq;
			double cummProb = firstProb * thirdProb;
			ttp.prob += cummProb;
			testCorpusMapBaseline2.put((String)term, ttp);
		}
	}
	
	public void computeBaseline3Map(IndexReader iAreaReader, int docID, double firstProb) throws IOException, InterruptedException{
		areaCorpusMap = getAreaTerms(iAreaReader, docID);
		int totFreq = calculateTotFreq(areaCorpusMap);
		getTestCorpusTerms(yearTPlus);
		TestTermParams ttp;
		for (Object term : testCorpusMapBaseline3.keySet()) {
			ttp = testCorpusMapBaseline3.get(term);
			int word_freq = ttp.freq;
			double thirdProb = ttp.prob;
			int area_term_freq = 0;
			if (areaCorpusMap.containsKey(term)) {
				area_term_freq = areaCorpusMap.get(term);
			}
			if (area_term_freq != 0) {
				thirdProb = alpha * area_term_freq / totFreq;
			} else {
				thirdProb = (1 - alpha) * 1 / totFreq;
			}
			thirdProb *= word_freq;
			double cummProb = firstProb * thirdProb;
			ttp.prob += cummProb;
			testCorpusMapBaseline3.put((String)term, ttp);
		}
	}

	public double getBaseline2Prob(String areaStrT, String yearT) throws IOException, InterruptedException{
		double prob = 0.00000000001; // Initialize to a very small probability
		long Nate = 0;
		long totNate = 0;
		if (resYearAreasCnt.containsKey(researcherId)){
			JSONObject YearAreaCnt = (JSONObject)resYearAreasCnt.get(researcherId);
			System.out.println(YearAreaCnt);
			System.out.println("***************************");
			java.util.Iterator it1 = YearAreaCnt.keySet().iterator();
			while(it1.hasNext()){
				String s1 = (String)it1.next();
				System.out.println("key = "+ s1+':'+ YearAreaCnt.get(s1));
				JSONObject inner = (JSONObject)YearAreaCnt.get(s1);
				java.util.Iterator it2 = inner.keySet().iterator();
				while(it2.hasNext()) {
					String s2 = (String)it2.next();
					if (s1.equalsIgnoreCase(yearT) && s2.equalsIgnoreCase(areaStrT)){
						Nate = (long)inner.get(s2);
					}
					System.out.println("key = " +s2+':'+ inner.get(s2));
					totNate += (long)inner.get(s2);
				}
			}
		}
		System.out.println(totNate);
		System.out.println(Nate);
		if (totNate != 0 && Nate != 0){
			prob = Nate / totNate;
		}
		return prob;
	}

	public double getBaseline3Prob(String areaStrT, String yearT) throws IOException, InterruptedException{
		double prob = 0.00000000001; // Initialize to a very small probability
		long Nate = 0;
		long totNate = 0;
		System.out.println("getBaseline3Prob");
		java.util.Iterator it1 = yearAreasCnt.keySet().iterator();
		while(it1.hasNext()){
			String s1 = (String)it1.next();
			System.out.println("key = "+ s1+':'+ yearAreasCnt.get(s1));
			JSONObject inner = (JSONObject)yearAreasCnt.get(s1);
			java.util.Iterator it2 = inner.keySet().iterator();
			while(it2.hasNext()) {
				String s2 = (String)it2.next();
				if (s1.equalsIgnoreCase(yearT) && s2.equalsIgnoreCase(areaStrT)){
					Nate = (long)inner.get(s2);
				}
				System.out.println("key = " +s2+':'+ inner.get(s2));
				totNate += (long)inner.get(s2);
			}
		}
		System.out.println(totNate);
		System.out.println(Nate);
		if (totNate != 0 && Nate != 0){
			prob = Nate / totNate;
		}
		return prob;
	}
	
	public DynamicInterestChanges_Jan30() {
		analyzer = new EnglishAnalyzer(Version.LUCENE_43);
		alpha = 0.85;
		yearT = "2010";
		yearTPlus = "2012";
		resYearCorpusBuffRead = openResearcherYearCorpus();
		resYearAreaCorpusBuffRead = openResearcherYearAreaCorpus();
		jsonParser = new JSONParser();
		researcherPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
		areaPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
		testPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
	}
	
	public static void main(String argv[]) throws InterruptedException, IOException, ParseException {
		DynamicInterestChanges_Jan30 mainClass = new DynamicInterestChanges_Jan30();
		
		String resYearCorpus = null;
		String resYearAreaCorpus = null;
		BufferedWriter bw1 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/perplexities"));
//		BufferedWriter model_prob_bw = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/model_probs"));
//		BufferedWriter baseline_prob_bw = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/baseline_probs"));
		BufferedWriter test_temp = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/test_temp"));
		BufferedWriter num_areas = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/num_areas"));
		if (mainClass.resYearCorpusBuffRead == null){
			System.out.print("Could not open the file");
			System.exit(1);
		}
		mainClass.getYearAreasCntDic();
		mainClass.getResYearAreasCntDic();
//		System.exit(1);
		try{
			int resCnt = 1;
			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null) && ((resYearAreaCorpus = mainClass.resYearAreaCorpusBuffRead.readLine()) != null)){
		        IndexReader iResreader;
//		        System.out.println("Are you here");
		        //Extract the necessary fields from the researcher and researcher_area corpus
		        mainClass.extractFields(resYearCorpus, resYearAreaCorpus);
		        System.out.println(mainClass.researcherYearAreaCorpusMap.size());
		        System.out.println("Starting process for researcher:" + mainClass.researcherName);
				//Proceed only if the researcher has corpus for both the years T and TPlus
				if (mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearT) && mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearTPlus)){
					if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
						test_temp.write("*************"+mainClass.researcherName+"*************"+"\n");
					}
					System.out.println("YearT = "+mainClass.yearT+":"+mainClass.yearCorpusMap.get(mainClass.yearT));
					mainClass.researcherCorpus = (String)mainClass.yearCorpusMap.get(mainClass.yearT);
//					Commented out Indexing the test corpus
					mainClass.indexTestCorpusYears(mainClass.yearTPlus);
//					mainClass.readIndex(mainClass.testPath, "yearId", "yearCorpus");
					mainClass.indexResearcherCorpus(mainClass.researcherId, mainClass.researcherCorpus);
					HashMap yearAreas = (HashMap)mainClass.researcherYearAreaCorpusMap.get(mainClass.yearT);
					String areaStrT;
					String areaCorpusT;
//					mainClass.getTestCorpusTerms(mainClass.yearTPlus);
					HashMap yearAreasTPlus = (HashMap)mainClass.researcherYearAreaCorpusMap.get(mainClass.yearTPlus);
					String areaStrTPlus;
					String areaCorpusTPlus;
					DirectoryReader iAreaReader;
					num_areas.write(mainClass.researcherName+ " "+mainClass.yearT +" "+yearAreas.size()+" "+mainClass.yearTPlus +" "+yearAreasTPlus.size()+"\n");
					for(Object areaTPlus: yearAreasTPlus.keySet()){
						areaStrTPlus = areaTPlus.toString();
						areaCorpusTPlus = (String)yearAreasTPlus.get(areaStrTPlus);
						mainClass.indexAreaCorpus(mainClass.researcherId, mainClass.researcherName, mainClass.yearTPlus, areaStrTPlus, areaCorpusTPlus);
					}
					for(Object areaT: yearAreas.keySet()){
						areaStrT = areaT.toString();
						areaCorpusT = (String)yearAreas.get(areaStrT);
						mainClass.indexAreaCorpus(mainClass.researcherId, mainClass.researcherName, mainClass.yearT, areaStrT, areaCorpusT);
						iAreaReader = mainClass.getReaderHandler(mainClass.areaPath);
						int docIDT = iAreaReader.maxDoc() - 1;
						Document hitDocT = iAreaReader.document(docIDT);
						iResreader = mainClass.getReaderHandler(mainClass.researcherPath);
						int resDocID = iResreader.maxDoc() - 1;
						double firstProb = mainClass.cosineDocumentSimilarity(resDocID, docIDT, iResreader, iAreaReader, "researcherCorpus", "areaCorpus");
						double baseline2prob = mainClass.getBaseline2Prob(areaStrT, mainClass.yearT);
						double baseline3prob = mainClass.getBaseline3Prob(areaStrT, mainClass.yearT);
						if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
							test_temp.write(hitDocT.get("year")+" "+hitDocT.get("areaName")+":firstProb="+firstProb+"\n");
						}
//						model_prob_bw.write(hitDocT.get("year")+" "+hitDocT.get("areaName")+" "+firstProb+"\n");
//						baseline_prob_bw.write(hitDocT.get("year")+" "+hitDocT.get("areaName")+" "+firstProb+"\n");
						mainClass.computeBaseline1Map(iAreaReader, docIDT, firstProb);
						mainClass.computeBaseline2Map(iAreaReader, docIDT, baseline2prob);
						mainClass.computeBaseline3Map(iAreaReader, docIDT, baseline3prob);
						
//						System.out.println("Comparing " + hitDocT.get("year") + " area:" + hitDocT.get("areaName"));
						for (int j = 0; j < iAreaReader.maxDoc()-1; j++) {
							int docIDTPlus = j;
							Document hitDocTPlus = iAreaReader.document(docIDTPlus);
							double secondProb = mainClass.cosineDocumentSimilarity(docIDT, docIDTPlus, iAreaReader, "areaCorpus");
							if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
								test_temp.write(hitDocTPlus.get("year")+" "+hitDocTPlus.get("areaName")+":secondProb="+secondProb+"\n");
							}
							mainClass.areaCorpusMap = mainClass.getAreaTerms(iAreaReader, docIDTPlus);
							int totFreq = mainClass.calculateTotFreq(mainClass.areaCorpusMap);
							TestTermParams ttp;
							for (Object term : mainClass.testCorpusMap.keySet()) {
								ttp = mainClass.testCorpusMap.get(term);
								int word_freq = ttp.freq;
								double thirdProb = 0.0;
								int area_term_freq = 0;
								if (mainClass.areaCorpusMap.containsKey(term)) {
									area_term_freq = mainClass.areaCorpusMap.get(term);
								}
								if (area_term_freq != 0) {
									thirdProb = mainClass.alpha * area_term_freq / totFreq;
								} else {
									thirdProb = (1 - mainClass.alpha) * 1 / totFreq;
								}
								thirdProb *= word_freq;
								double cummProb = firstProb * secondProb * thirdProb;
								ttp.prob += cummProb;
								mainClass.testCorpusMap.put((String)term, ttp);
							}
							if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
							for (Object term : mainClass.testCorpusMap.keySet()) {
								test_temp.write(term+":"+mainClass.testCorpusMap.get(term).prob+"\t");
								num_areas.write(term+":"+mainClass.testCorpusMap.get(term).prob+"\t");
							}
							test_temp.write("\n");
							num_areas.write("\n");
							}
						}
						mainClass.deleteDoc(iAreaReader.maxDoc() - 1);
//						double perplexity = mainClass.computeNewPerplexity(mainClass.testCorpusMap);
//						double baseline1perplexity = mainClass.computeNewPerplexity(mainClass.testCorpusMapBaseline);
//						double baseline2perplexity = mainClass.computeNewBaseline2Perplexity(mainClass.testCorpusMapBaseline);
//						double baseline3perplexity = mainClass.computeNewBaseline3Perplexity(mainClass.testCorpusMapBaseline);
//						System.out.println("ModelPerplexity = " + perplexity);
//						System.out.println("Baseline1Perplexity = " + baseline1perplexity);
//						System.out.println("Baseline2Perplexity = " + baseline2perplexity);
//						System.out.println("Baseline3Perplexity = " + baseline3perplexity);
					}
					double perplexity = mainClass.computePerplexity(mainClass.testCorpusMap);
					double baseline1perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline1);
					double baseline2perplexity = mainClass.computeBaseline2Perplexity(mainClass.testCorpusMapBaseline2);
					double baseline3perplexity = mainClass.computeBaseline3Perplexity(mainClass.testCorpusMapBaseline3);
					System.out.println("ModelPerplexity = " + perplexity);
					System.out.println("Baseline1Perplexity = " + baseline1perplexity);
					System.out.println("Baseline2Perplexity = " + baseline2perplexity);
					System.out.println("Baseline3Perplexity = " + baseline3perplexity);
//					model_prob_bw.write("*************"+mainClass.researcherName+"*************"+"\n");
//					baseline_prob_bw.write("*************"+mainClass.researcherName+"*************"+"\n");
//					if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
//						test_temp.write("*************"+mainClass.researcherName+"*************"+"\n");
//					}
					bw1.write(mainClass.researcherId+"("+mainClass.researcherName+")"+ "Model Perplexity = "+ perplexity +"\tBaseline Perplexity = "+baseline1perplexity+"\n");
		    	}
				mainClass.deleteIndexes("areas");
				mainClass.deleteIndexes("test");
				mainClass.deleteIndexes("researcher");
				resCnt++;
				System.out.println("Completed process for researcher:"+mainClass.researcherName);
			}
			bw1.close();
			num_areas.close();
//			model_prob_bw.close();
//			baseline_prob_bw.close();
			test_temp.close();
		}catch(IOException e){
			e.printStackTrace();
		}catch (ParseException e) {
			e.printStackTrace();
		}
	}
}