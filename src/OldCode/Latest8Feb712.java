package OldCode;
//Working Clean-Up

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.analysis.Analyzer;
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

public class Latest8Feb712 {
	private Map<String, TestTermParams> testCorpusMap;
	private Map<String, TestTermParams> testCorpusMapBaseline1;
	private Map<String, TestTermParams> testCorpusMapBaseline2;
	private Map<String, TestTermParams> testCorpusMapBaseline3;
	private Map<String, Integer> areaCorpusMap;
	private Analyzer analyzer;
	private double alpha;
	private double beta;
	private String yearT;
	private String yearTPlus;
	private BufferedReader resYearCorpusBuffRead;
	private BufferedReader resYearAreaCorpusBuffRead;
	private File testPath;
	private File areaTPath;
	private File areaALLPath;
	private File areaTPlusPath;
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
	private int vocabCnt;
	
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
        Map<String, Integer> frequencies = new HashMap();
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
    
    private double cosineDocumentSimilarity(int d1, int d2, IndexReader reader1, IndexReader reader2, String content1, String content2) throws IOException {
    	Map<String, Integer> f1 = getTermFrequencies(reader1, d1, content1);
    	Map<String, Integer> f2 = getTermFrequencies(reader2, d2, content2);
        RealVector v1;
        RealVector v2;
        v1 = toRealVector(f1);
        v2 = toRealVector(f2);
        return getCosineSimilarity(v1, v2);
    }

	private static class TestTermParams {
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
		Object resYearAreaObj = jsonParser.parse(resYearWiseAreaCorpus);
		JSONObject resYearAreaJSONObj = (JSONObject)resYearAreaObj;
		researcherYearAreaCorpusMap = (HashMap)resYearAreaJSONObj.get("year_area_corpus");
		researcherId = (String)resYearJSONObj.get("_id");
		researcherName = (String)resYearJSONObj.get("name");
		if (resYearJSONObj.get("year_corpus") != null){
			yearCorpusMap = (HashMap)resYearJSONObj.get("year_corpus");
		}
	}
	private void indexTestCorpusYears(String yearTPlus) throws IOException, ParseException, InterruptedException{
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
	
	private void indexResearcherCorpusForVocab(String researcherCorpus) throws IOException{
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusVocabIndex");
    	Directory directory = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        IndexWriter iwriter;
		iwriter = new IndexWriter(directory, config);
		Document doc = new Document();
//		doc.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
		Field field = new Field("researcherCorpus", researcherCorpus, TYPE_STORED);
		doc.add(field);
		iwriter.addDocument(doc);
		iwriter.close();		
	}
	private void indexAreaTCorpus(String researcherId, String researcherName, String yearT, String areaStrT, String areaCorpusT) throws IOException{
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearTAreaIndex");
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
		iwriter.close();
	}

	private void indexAreaTPlusCorpus(String researcherId, String researcherName, String yearTPlus, String areaStrTPlus, String areaCorpusTPlus) throws IOException{
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearTPlusAreaIndex");
    	directory = FSDirectory.open(path);
    	config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        
		iwriter = new IndexWriter(directory, config);
		Document docT = new Document();
		docT.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
		docT.add(new Field("researcherName", researcherName, Field.Store.YES, Field.Index.NOT_ANALYZED));
		docT.add(new Field("year", yearT, Field.Store.YES, Field.Index.NOT_ANALYZED));
		docT.add(new Field("areaName", areaStrTPlus, Field.Store.YES, Field.Index.NOT_ANALYZED));
		Field fieldT = new Field("areaCorpus", areaCorpusTPlus, TYPE_STORED);
		docT.add(fieldT);
		iwriter.addDocument(docT);
		iwriter.close();
	}

	private void indexALLAreasCorpus(String areaName, String areaCorpus) throws IOException{
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsALLAreasIndex");
    	directory = FSDirectory.open(path);
    	config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
		iwriter = new IndexWriter(directory, config);
		Document docT = new Document();
		docT.add(new Field("areaName", areaName, Field.Store.YES, Field.Index.NOT_ANALYZED));
		Field fieldT = new Field("areaCorpus", areaCorpus, TYPE_STORED);
		docT.add(fieldT);
		iwriter.addDocument(docT);
		iwriter.close();
	}
	
	private void deleteIndexes(String field) throws IOException{
		//Deleting Area Indices
		File path = null;
		if (field.equals("areasT"))
			path = areaTPath;
		else if(field.equals("areasTPlus"))
			path = areaTPlusPath;
		else if(field.equals("test"))
			path = testPath;
		else if(field.equals("researcher"))
			path = researcherPath;

		directory = FSDirectory.open(path);
        config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
		iwriter = new IndexWriter(directory, config);
		iwriter.deleteAll();
		iwriter.commit();
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
		}
	}
	
	public DirectoryReader getReaderHandler(File path) throws IOException{
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	return ireader;
	}

    public void readIndexSpecificDoc(DirectoryReader ireader, int docID, String id, String field) throws IOException{
    		int i = docID;
    		Set<String> terms = new HashSet<>();
    		Terms vector = ireader.getTermVector(i, field);	
    	    if (vector != null){
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
    	    System.out.println();
    }

    public void readIndex(File path, String id, String field) throws IOException{
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	System.out.println("The number of documents is " + ireader.maxDoc());
    	for (int i = 0; i < ireader.maxDoc(); i++){
    		Set<String> terms = new HashSet<>();
    		Terms vector = ireader.getTermVector(i, field);	
    	    if (vector != null){
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
		Double prob = new Double(0.0);
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
			TestTermParams ttp3 = new TestTermParams(freq, prob);
			TestTermParams ttp4 = new TestTermParams(freq, prob);
			testCorpusMap.put(term, ttp1);
			testCorpusMapBaseline1.put(term, ttp2);
			testCorpusMapBaseline2.put(term, ttp3);
			testCorpusMapBaseline3.put(term, ttp4);
		}
	}

    private double computePerplexity(Map<String, TestTermParams> testCorpusMap){
    	double perplexity = 0.0;
    	double sumLogProb = 0.0;
    	double l_perplexity = 0.0;
    	int totTermFreqs = 0;
		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
			if (ttp.prob != 0.0){
				totTermFreqs += ttp.freq;
//				System.out.println(term+"  +ve prob:"+ttp.freq+"   "+ ttp.prob);
				double prob = Math.log(ttp.prob)/ Math.log(2);
//				System.out.println("LogBase2Prob = "+prob);
				prob = prob * ttp.freq;
				sumLogProb += prob;
//				System.out.println("sumLogProb="+sumLogProb);
			}
		}
//		System.out.println("Term Total with +ve prob ="+totTermFreqs);
//		System.out.println("sumLogProb="+sumLogProb);
		l_perplexity = sumLogProb / totTermFreqs;
//		System.out.println("l_perplexity=sumLogProb / tot"+l_perplexity);
		perplexity = Math.pow(2, -1.0*l_perplexity);
//		System.out.println("perplexity="+perplexity);
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

	
//	Common computeBaselineMap	
//	public void computeBaselineMap(IndexReader iAreaReader, int docID, double firstProb, Map<String, TestTermParams> commonTestCorpusMapBaseline) throws IOException, InterruptedException{
//		areaCorpusMap = getAreaTerms(iAreaReader, docID);
//		int totFreq = calculateTotFreq(areaCorpusMap);
//		TestTermParams ttp;
//		
//		LinkedHashMap<String, Double> thirdProbsMAP = new LinkedHashMap<>();
//		for (Object term : commonTestCorpusMapBaseline.keySet()) {
//			double thirdProb = 0.0;
//			int area_term_freq = 0;
//			if (areaCorpusMap.containsKey(term)) {
//				area_term_freq = areaCorpusMap.get(term);
//			}
////			if (area_term_freq != 0){
//			thirdProb = (alpha * ((double)area_term_freq / totFreq)) + (1 - alpha) * (1.0 / vocabCnt);
////			} else{
////				thirdProb = 0.0;
////			}
////			System.out.println("computeBaselineMap:ThirdProb = "+thirdProb);
//			thirdProbsMAP.put(term.toString(), thirdProb);
//		}
//		double normThirdProb = 0.0;
//		for (String term : thirdProbsMAP.keySet()) {
//			double prob = thirdProbsMAP.get(term);
//			normThirdProb += prob;
//		}
////		System.out.println("normThirdProb="+normThirdProb);
//		for (Object term : commonTestCorpusMapBaseline.keySet()) {
//			ttp = testCorpusMap.get(term);
//			double thirdProb = thirdProbsMAP.get(term);
//			thirdProb = thirdProb / normThirdProb;
//			System.out.println(term+" FirstProb = "+ firstProb+ " ThirdProb = " + thirdProb);
//			double cummProb = firstProb  * thirdProb;
////			System.out.println(term+" cummProb = " + cummProb);
//			ttp.prob += cummProb;
////			System.out.println(term+" ttp.prob = " + ttp.prob);
//			commonTestCorpusMapBaseline.put((String)term, ttp);
//		}
//		double tot = 0.0;
//		for (Object term : commonTestCorpusMapBaseline.keySet()) {
//			ttp = testCorpusMap.get(term);
//			tot += ttp.prob;
//		}
//		System.out.println("Tot prob = "+ tot);
//	}

//	Common computeBaselineMap	
	public void computeBaselineMap(IndexReader iAreaReader, int docID, double firstProb, Map<String, TestTermParams> testCorpusMapBaseline) throws IOException, InterruptedException{
		areaCorpusMap = getAreaTerms(iAreaReader, docID);
		int totFreq = calculateTotFreq(areaCorpusMap);
		TestTermParams ttp;
		for (Object term : testCorpusMapBaseline.keySet()) {
			ttp = testCorpusMapBaseline.get(term);
			double thirdProb = 0.0;
			int area_term_freq = 0;
			if (areaCorpusMap.containsKey(term)) {
				area_term_freq = areaCorpusMap.get(term);
			}
			thirdProb = (alpha * ((double)area_term_freq / totFreq)) + (1 - alpha) * (1.0 / vocabCnt);
//			if (area_term_freq != 0){
//				thirdProb = (alpha * area_term_freq / totFreq);
//			}
			double cummProb = firstProb * thirdProb;
			System.out.println(alpha+"*"+ area_term_freq+"/"+ totFreq+"="+(alpha * area_term_freq / totFreq));
			
			ttp.prob += cummProb;
			System.out.println(term+":"+firstProb+":"+thirdProb+"="+cummProb+":"+ttp.prob);
			testCorpusMapBaseline.put((String)term, ttp);
		}
	}

	
	public double getBaseline2Prob(String areaStrT, String yearT) throws IOException, InterruptedException{
		double prob = 0.00000000001; // Initialize to a very small probability
		long Nate = 0;
		long totNate = 0;
		if (resYearAreasCnt.containsKey(researcherId)){
			JSONObject YearAreaCnt = (JSONObject)resYearAreasCnt.get(researcherId);
			java.util.Iterator it1 = YearAreaCnt.keySet().iterator();
			while(it1.hasNext()){
				String s1 = (String)it1.next();
				JSONObject inner = (JSONObject)YearAreaCnt.get(s1);
				java.util.Iterator it2 = inner.keySet().iterator();
//				System.out.println(s1);
				while(it2.hasNext()) {
					String s2 = (String)it2.next();
					if (s1.equalsIgnoreCase(yearT)){
						totNate += (long)inner.get(s2);
//						System.out.println("Incrementing totNate to "+totNate);
						if(s2.equalsIgnoreCase(areaStrT)){
							Nate = (long)inner.get(s2);
//							System.out.println("s2="+s2+"areaStrT="+areaStrT);
//							System.out.println("Setting Nate="+Nate);
						}
					}
				}
			}
		}
//		System.out.println("totNate:"+totNate);
		if (totNate != 0 && Nate != 0){
			prob = (double)Nate / totNate;
		}
		return prob;
	}

	public double getBaseline3Prob(String areaStrT, String yearT) throws IOException, InterruptedException{
		double prob = 0.00000000001; // Initialize to a very small probability
		long Nat = 0;
		long totNat = 0;
		HashMap yearAreas = (HashMap)yearAreasCnt.get(yearT);
		if (yearAreas.containsKey(areaStrT)){
			Nat = (long)yearAreas.get(areaStrT);
		}
		java.util.Iterator it = yearAreas.keySet().iterator();
		while(it.hasNext()) {
			String s2 = (String)it.next();
			totNat += (long)yearAreas.get(s2);
		}
		if (totNat != 0 && Nat != 0){
			prob = (double)Nat / totNat;
		}
		return prob;
	}
	
	private Set customintersection(HashMap s1, HashMap s2){
		Set s = new HashSet();
		java.util.Iterator it = s1.keySet().iterator();
		while(it.hasNext()) {
			String str = (String)it.next();
			if (s2.containsKey(str)){
				s.add(str);
			}
		}
		return s;
	}
	
	private Set customunion(HashMap s1, HashMap s2){
		Set s = new HashSet();
		java.util.Iterator it = s1.keySet().iterator();
		while(it.hasNext()) {
			String str = (String)it.next();
			s.add(str);
		}
		it = s2.keySet().iterator();
		while(it.hasNext()) {
			String str = (String)it.next();
			s.add(str);
		}

		return s;
	}

	
	private double computeResearcherConservation(String researcherId, String yearT){
		HashMap resMap = (HashMap)resYearAreasCnt.get(researcherId);
		List<String> unsortList = new ArrayList<String>();
		List betaList = new ArrayList();
//		System.out.println(researcherId);
		java.util.Iterator it = resMap.keySet().iterator();
		while(it.hasNext()) {
			String s2 = (String)it.next();
			unsortList.add(s2);
		}
		Collections.sort(unsortList);
//		System.out.println(unsortList);
		for(int i = 0; i < unsortList.size() - 1; i++){
			int j = i+1;
			String year1 = unsortList.get(i);
			String year2 = unsortList.get(j);
//			System.out.println("year1="+year1+"\tyear2="+year2);
			if (Integer.parseInt(year2) > Integer.parseInt(yearT))
				break;
			HashMap year1map = (HashMap) resMap.get(year1);
			HashMap year2map = (HashMap) resMap.get(year2);
			Set<String> intersection = customintersection(year1map, year2map);
			Set<String> union = customunion(year1map, year2map);
			double jaccard = (double)intersection.size() / union.size();
//			System.out.println("*******************************************");
//			
//			System.out.println("year1map="+year1map+"\tyear2map="+year2map);
//			System.out.println("intersection="+intersection);
//			System.out.println("union size = "+ union.size() + " intersection size = "+ intersection.size()+" jaccard = "+ jaccard);
//			System.out.println("*******************************************");
			betaList.add(jaccard);
		}
		double jaccardAvg = 0.0;
//		System.out.println("betaList size="+betaList.size());
		for(int i = 0; i < betaList.size(); i++){
			jaccardAvg += (double)betaList.get(i);
		}
		if (betaList.size() > 0)
			jaccardAvg = jaccardAvg / betaList.size();
		if (jaccardAvg == 0.0){
//			System.out.println("jaccardAvg="+jaccardAvg);
			jaccardAvg = 0.001;
		}
		return jaccardAvg;
	}

	public void normalizeTestCorpus(Map<String, TestTermParams> testCorpusMapSample) throws IOException, InterruptedException{
		TestTermParams ttp;
		double normalizedProb = 0.0;
		for (Object term : testCorpusMapSample.keySet()) {
			ttp = testCorpusMapSample.get(term);
			normalizedProb += ttp.prob;
//			System.out.println("Before:"+ttp.prob);
		}
		for (Object term : testCorpusMapSample.keySet()) {
			ttp = testCorpusMapSample.get(term);
			ttp.prob = ttp.prob / normalizedProb;
//			System.out.println("After:"+ttp.prob);
//			testCorpusMapSample.put((String)term, ttp);
		}
	}
	
	private static Map sortByComparator(Map unsortMap) {
		 
		List list = new LinkedList(unsortMap.entrySet());
 
		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
                                       .compareTo(((Map.Entry) (o1)).getValue());
			}
		});
 
		// put sorted list into map again
                //LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	public Latest8Feb712() {
		analyzer = new EnglishAnalyzer(Version.LUCENE_43);
		alpha = 0.85;
		yearT = "2008";
		yearTPlus = "2013";
		resYearCorpusBuffRead = openResearcherYearCorpus();
		resYearAreaCorpusBuffRead = openResearcherYearAreaCorpus();
		jsonParser = new JSONParser();
		researcherPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
		areaTPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearTAreaIndex");
		areaALLPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsALLAreasIndex");
		areaTPlusPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearTPlusAreaIndex");
		testPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
	}
	
	public static void main(String argv[]) throws InterruptedException, IOException, ParseException {
		Latest8Feb712 mainClass = new Latest8Feb712();
		
		String resYearCorpus = null;
		String resYearAreaCorpus = null;
		
		BufferedWriter prob_bw = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/Probabilities_badperplexity"));
		BufferedWriter bw1 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/perplexities"));
		bw1.write("ID\tModel Perplexity\tBaseline1 Perplexity\tBaseline2 Perplexity\tBaseline3 Perplexity\tCummulative Perplexity\tbeta\n");
		if (mainClass.resYearCorpusBuffRead == null){
			System.out.print("Could not open the file");
			System.exit(1);
		}
		mainClass.getYearAreasCntDic();
		mainClass.getResYearAreasCntDic();
		
		try{
//			
//			while ((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null){
//				
//				Object resYearObj = mainClass.jsonParser.parse(resYearCorpus);
//				JSONObject resYearJSONObj = (JSONObject)resYearObj;
//				if (resYearJSONObj.get("year_corpus") != null){
//					HashMap temp = (HashMap)resYearJSONObj.get("year_corpus");
//					java.util.Iterator it2 = temp.keySet().iterator();
//					while(it2.hasNext()) {
//						String s2 = (String)it2.next();
//						String corpus = (String) temp.get(s2);
////						System.out.println(corpus);
//						mainClass.indexResearcherCorpusForVocab(corpus);
//					}
//				}
//			}
//			DirectoryReader iVocabTReader;
//			File temppath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusVocabIndex");
//				iVocabTReader = mainClass.getReaderHandler(temppath);
//				System.out.println("Done indexing "+iVocabTReader.maxDoc()+" docs");
//				for (int j = 0; j < iVocabTReader.maxDoc(); j++) {
//					Terms area_terms = iVocabTReader.getTermVector(j, "researcherCorpus");
//					TermsEnum termsEnum = null;
//					termsEnum = area_terms.iterator(termsEnum);
//					BytesRef name = null;
//					while ((name = termsEnum.next()) != null) {
//						String term = name.utf8ToString();
//						int freq = (int) termsEnum.totalTermFreq();
//						mainClass.vocabCnt += 1;
//					}
//				}
//			System.out.println("Total VocabCnt = "+ mainClass.vocabCnt);
//			System.exit(1);
//			mainClass.resYearCorpusBuffRead.close();
//			mainClass.resYearCorpusBuffRead = mainClass.openResearcherYearCorpus();
//			int cnt = 0;
//			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null) && ((resYearAreaCorpus = mainClass.resYearAreaCorpusBuffRead.readLine()) != null)){
//				mainClass.extractFields(resYearCorpus, resYearAreaCorpus);
//				
//				if (mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearT)){
//					cnt += 1;
//				}
//			}
//			System.out.println("#researchers with year:"+mainClass.yearT+"="+cnt);
//			System.exit(1);
			//Vocab count
			mainClass.vocabCnt = 61776;
			mainClass.indexTestCorpusYears(mainClass.yearTPlus);
//			mainClass.readIndex(mainClass.testPath, "yearId", "yearCorpus");
			DirectoryReader iTestReader = mainClass.getReaderHandler(mainClass.testPath);
			DirectoryReader iAreaALLReader = mainClass.getReaderHandler(mainClass.areaALLPath);
			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null) && ((resYearAreaCorpus = mainClass.resYearAreaCorpusBuffRead.readLine()) != null)){
		        IndexReader iResreader;
		        //Extract the necessary fields from the researcher and researcher_area corpus
		        mainClass.extractFields(resYearCorpus, resYearAreaCorpus);
		        
		        
				//Proceed only if the researcher has corpus for both the years T and TPlus
		        
		        if (mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearT)){
		        	System.out.println("Starting process for researcher:" + mainClass.researcherName+":"+mainClass.researcherId);
		        	System.out.println("*********************************");
		        	
		        	mainClass.beta = mainClass.computeResearcherConservation(mainClass.researcherId, mainClass.yearT);
		        	if(mainClass.beta == 0.0) continue;
					mainClass.researcherCorpus = (String)mainClass.yearCorpusMap.get(mainClass.yearT);
					mainClass.indexResearcherCorpus(mainClass.researcherId, mainClass.researcherCorpus);
					HashMap yearAreasT = (HashMap)mainClass.researcherYearAreaCorpusMap.get(mainClass.yearT);
					String areaStrT;
					String areaCorpusT;
					mainClass.getTestCorpusTerms(mainClass.yearTPlus);
					String areaStrTPlus;
					String areaCorpusTPlus;
					DirectoryReader iAreaTReader;
					
					double normFirstProb = 0.0;
					for(Object areaT: yearAreasT.keySet()){
						areaStrT = areaT.toString();
						areaCorpusT = (String)yearAreasT.get(areaStrT);
						mainClass.indexAreaTCorpus(mainClass.researcherId, mainClass.researcherName, mainClass.yearT, areaStrT, areaCorpusT);
						iAreaTReader = mainClass.getReaderHandler(mainClass.areaTPath);
						int docIDT = iAreaTReader.maxDoc() - 1;
						Document doc1 = iAreaTReader.document(docIDT);
						iResreader = mainClass.getReaderHandler(mainClass.researcherPath);
						int resDocID = iResreader.maxDoc() - 1;
						double firstProb = mainClass.cosineDocumentSimilarity(resDocID, docIDT, iResreader, iAreaTReader, "researcherCorpus", "areaCorpus");
						normFirstProb += firstProb;
						mainClass.deleteIndexes("areasT");
					}
					int TotNumAreasT = yearAreasT.size();
					int TotNumAreasTPlus = iAreaALLReader.maxDoc() - yearAreasT.size();
					ArrayList researcherAreasT = new ArrayList();
					for(Object areaT: yearAreasT.keySet()){
						researcherAreasT.add(areaT); 
					}
					System.out.println(researcherAreasT);
					for(Object areaT: yearAreasT.keySet()){
						areaStrT = areaT.toString();
						areaCorpusT = (String)yearAreasT.get(areaStrT);
						mainClass.indexAreaTCorpus(mainClass.researcherId, mainClass.researcherName, mainClass.yearT, areaStrT, areaCorpusT);
						iAreaTReader = mainClass.getReaderHandler(mainClass.areaTPath);
						int docIDT = iAreaTReader.maxDoc() - 1;
						iResreader = mainClass.getReaderHandler(mainClass.researcherPath);
						int resDocID = iResreader.maxDoc() - 1;
						double firstProb = mainClass.cosineDocumentSimilarity(resDocID, docIDT, iResreader, iAreaTReader, "researcherCorpus", "areaCorpus");
						firstProb = firstProb / normFirstProb;
						double baseline2prob = mainClass.getBaseline2Prob(areaStrT, mainClass.yearT);
//						System.out.println("baseline2prob:"+baseline2prob);
						double baseline3prob = mainClass.getBaseline3Prob(areaStrT, mainClass.yearT);
//						System.out.println(areaStrT);
//						mainClass.readIndexSpecificDoc(iAreaTReader, docIDT, "areaName", "areaCorpus");
//						System.out.println("Baseline1 for area="+areaStrT);
						mainClass.computeBaselineMap(iAreaTReader, docIDT, firstProb, mainClass.testCorpusMapBaseline1);
						System.out.println("Baseline2 for area="+areaStrT);
						mainClass.computeBaselineMap(iAreaTReader, docIDT, baseline2prob, mainClass.testCorpusMapBaseline2);
//						for (Object term : mainClass.testCorpusMap.keySet()) {
//							System.out.println("CummProb("+term+")"+":"+mainClass.testCorpusMapBaseline2.get(term).prob);
//						}
//						System.out.println("Baseline3 for area="+areaStrT);
						mainClass.computeBaselineMap(iAreaTReader, docIDT, baseline3prob, mainClass.testCorpusMapBaseline3);
						
//						**********This Loop is only to get the top scores with the areaT*******************
						
//						Map<Integer, Double> unsortMap = new HashMap<Integer, Double>();
//						ArrayList existingDocs = new ArrayList<>();
//						int howManyMatches = 0;
//						for (int j = 0; j < iAreaALLReader.maxDoc(); j++) {
//							int docIDTPlus = j;
//							Document doc1 = iAreaTReader.document(docIDT);
//							Document doc2 = iAreaALLReader.document(docIDTPlus);
//							areaStrTPlus = doc2.get("areaName");
//							if (researcherAreasT.contains(areaStrTPlus)){
//								existingDocs.add(docIDTPlus);
//								howManyMatches++;
//								continue;
//							}
//							double secondProb = mainClass.cosineDocumentSimilarity(docIDT, docIDTPlus, iAreaTReader,iAreaALLReader, "areaCorpus", "areaCorpus");
//							unsortMap.put(docIDTPlus, secondProb);
//						}
						
//						Map<Integer, Double> sortedMap = sortByComparator(unsortMap);
//						LinkedHashMap top5 = new LinkedHashMap<>();
//						int top5Count = 0;
//						for (Object key : sortedMap.keySet()) {
//							int docID = (int)key;
//							Document doc = iAreaALLReader.document(docID);
//							String area = doc.get("areaName");
//							double score = sortedMap.get(key);
//							System.out.println(area+"(: " + key + ") Value : " + sortedMap.get(key));
//							top5Count++;
//							top5.put(docID, score);
//							if (top5Count >= 5)break;
//						}
//						System.out.println("#areas in T = "+ howManyMatches);
//						System.out.println(top5);
//						double normSecondProb = 0.0;
//						for (Object key : top5.keySet()) {
//							double score = (double)top5.get(key);
//							normSecondProb += score;
//						}
//						if (normSecondProb == 0.0){
//							System.out.println("does this happen??????");
//							Thread.sleep(5000);
//							normSecondProb = 0.0000001;
//						}
//						********************* For areas in the current year for the researcher *******************
//						for (Object docIDObj : existingDocs){
//							int docID = (int)docIDObj;
//							Document doc = iAreaALLReader.document(docID);
//							String area = doc.get("areaName");
//							double sameAreaProb = mainClass.getBaseline2Prob(area.toString(), mainClass.yearT);
//							System.out.println(area.toString()+" sameAreaProb="+sameAreaProb);
//							double secondProb = sameAreaProb * mainClass.beta;
//							mainClass.areaCorpusMap = mainClass.getAreaTerms(iAreaALLReader, docID);
//							int totAreaFreq = mainClass.calculateTotFreq(mainClass.areaCorpusMap);
//							TestTermParams ttp;
//							LinkedHashMap<String, Double> thirProbs = new LinkedHashMap<>();
//							for (Object term : mainClass.testCorpusMap.keySet()) {
//								double thirdProb = 0.0;
//								int area_term_freq = 0;
//								if (mainClass.areaCorpusMap.containsKey(term)) {
//									area_term_freq = mainClass.areaCorpusMap.get(term);
//								}
//						thirdProb = (alpha * ((double)area_term_freq / totFreq)) + (1 - alpha) * (1.0 / vocabCnt);
//								if (area_term_freq != 0){
//									thirdProb = (mainClass.alpha * ((double)area_term_freq / totAreaFreq));
//								} else{
//									thirdProb = 0.0;
//								}
//								thirProbs.put(term.toString(), thirdProb);
//							}
//							double normThirdProb = 0.0;
//							for (String term : thirProbs.keySet()) {
//								double prob = thirProbs.get(term);
//								normThirdProb += prob;
//							}
//							for (Object term : mainClass.testCorpusMap.keySet()) {
//								ttp = mainClass.testCorpusMap.get(term);
//								double thirdProb = thirProbs.get(term);
//								double cummProb = firstProb * secondProb * thirdProb;
//								ttp.prob += cummProb;
//								mainClass.testCorpusMap.put((String)term, ttp);
//							}
//						}

//						for (Object key : top5.keySet()) {
//							int docID = (int)key;
//							double similarity = (double)top5.get(key);
//							double secondProb = (1.0 - mainClass.beta) * (similarity / normSecondProb); 
//							mainClass.areaCorpusMap = mainClass.getAreaTerms(iAreaALLReader, docID);
//							int totAreaFreq = mainClass.calculateTotFreq(mainClass.areaCorpusMap);
//							TestTermParams ttp;
//							LinkedHashMap<String, Double> thirProbs = new LinkedHashMap<>();
//							for (Object term : mainClass.testCorpusMap.keySet()) {
//								double thirdProb = 0.0;
//								int area_term_freq = 0;
//								if (mainClass.areaCorpusMap.containsKey(term)) {
//									area_term_freq = mainClass.areaCorpusMap.get(term);
//								}
//								if (area_term_freq != 0){
//									thirdProb = (mainClass.alpha * ((double)area_term_freq / totAreaFreq));
//								} else{
//									thirdProb = 0.0;
//								}
//								thirProbs.put(term.toString(), thirdProb);
//							}
//							double normThirdProb = 0.0;
//							for (String term : thirProbs.keySet()) {
//								double prob = thirProbs.get(term);
//								normThirdProb += prob;
//							}
//							for (Object term : mainClass.testCorpusMap.keySet()) {
//								ttp = mainClass.testCorpusMap.get(term);
//								double thirdProb = thirProbs.get(term);
//								double cummProb = firstProb * secondProb * thirdProb;
//								ttp.prob += cummProb;
//								mainClass.testCorpusMap.put((String)term, ttp);
//							}	
//						}
					mainClass.deleteIndexes("areasT");
					}
//					for (Object term : mainClass.testCorpusMap.keySet()) {
//						System.out.println("CummProb("+term+")"+":"+mainClass.testCorpusMapBaseline1.get(term).prob);
//					}

//					System.out.println("*****************************************");
					mainClass.normalizeTestCorpus(mainClass.testCorpusMap);
					mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline1);
					mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline2);
					mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline3);
//					double totProb = 0.0;
//					System.out.println("After Normalization");
//					for (Object term : mainClass.testCorpusMap.keySet()) {
//						System.out.println("CummProb("+term+")"+":"+mainClass.testCorpusMapBaseline1.get(term).prob);
//					}
//					
//					System.out.println("*****************************************");
//					System.out.println("ModelPerplexity");
					double perplexity = mainClass.computePerplexity(mainClass.testCorpusMap);
//					System.out.println("*****************************************");
//					System.out.println("Baseline1Perplexity");
					double baseline1perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline1);
//					System.out.println("*****************************************");
//					System.out.println("Baseline2Perplexity");
					for (Object term : mainClass.testCorpusMapBaseline2.keySet()) {
						System.out.println("CummProb("+term+")"+":"+mainClass.testCorpusMapBaseline2.get(term).prob);
					}
					double baseline2perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline2);
//					System.out.println("*****************************************");
//					System.out.println("Baseline3Perplexity");
					double baseline3perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline3);
					System.out.println("*****************************************");
//					double cummPerplexity = (0.5 * perplexity) +(0.3 * baseline1perplexity) + (0.1 * baseline2perplexity) + (0.1 * baseline3perplexity);
					System.out.println("ModelPerplexity = " + perplexity);
					System.out.println("Baseline1Perplexity = " + baseline1perplexity);
					System.out.println("Baseline2Perplexity = " + baseline2perplexity);
					System.out.println("Baseline3Perplexity = " + baseline3perplexity);
//					System.out.println("cummPerplexity = " + cummPerplexity);
//					bw1.write(mainClass.researcherId+"\t"+perplexity +"\t"+baseline1perplexity+"\t"+baseline2perplexity+"\t"+baseline3perplexity+"\t"+cummPerplexity+"\t"+Math.round(mainClass.beta)+"\n");
					System.exit(1);
		    	}
				mainClass.deleteIndexes("areasT");
//				mainClass.deleteIndexes("areasTPlus");
//				mainClass.deleteIndexes("test");
				mainClass.deleteIndexes("researcher");
			}
			bw1.close();
		}catch(IOException e){
			e.printStackTrace();
		}catch (ParseException e) {
			e.printStackTrace();
		}
	}
}