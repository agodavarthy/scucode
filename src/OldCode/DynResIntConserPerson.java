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
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

public class DynResIntConserPerson {
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
    	double logSumProb = 0.0;
    	double l_perplexity = 0.0;
    	int totFreq = calculateTotTestCorpusTerms(testCorpusMap);
    	int tot = 0;
    	System.out.println("totFreq="+totFreq);
		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
			System.out.println(term+"prob="+ttp.prob);
			double prob = Math.log(ttp.prob)/ Math.log(2);
			logSumProb += prob;
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

//	Common computeBaselineMap	
	public void computeBaselineMap(IndexReader iAreaReader, int docID, double firstProb, Map<String, TestTermParams> testCorpusMapBaseline, BufferedWriter prob_bw) throws IOException, InterruptedException{
		areaCorpusMap = getAreaTerms(iAreaReader, docID);
		int totFreq = calculateTotFreq(areaCorpusMap);
		TestTermParams ttp;
		int cummZeroCnt = 0;
		for (Object term : testCorpusMapBaseline.keySet()) {
			ttp = testCorpusMapBaseline.get(term);
			int word_freq = ttp.freq;
			double thirdProb = ttp.prob;
			int area_term_freq = 0;
			if (areaCorpusMap.containsKey(term)) {
				area_term_freq = areaCorpusMap.get(term);
			}
			thirdProb = (alpha * area_term_freq / totFreq) + (1 - alpha) * (1 / vocabCnt);
			double cummProb = firstProb * thirdProb;
			if(thirdProb <= 0.0){
				thirdProb = 0.0000001;
			}
			if (cummProb == 0.0){
				cummZeroCnt += 1;
				cummProb = 0.0000001;
			}
			ttp.prob += cummProb;
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
				while(it2.hasNext()) {
					String s2 = (String)it2.next();
					if (s1.equalsIgnoreCase(yearT) && s2.equalsIgnoreCase(areaStrT)){
						Nate = (long)inner.get(s2);
					}
					totNate += (long)inner.get(s2);
				}
			}
		}
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
	
//	public double getBaseline3Prob(String areaStrT, String yearT) throws IOException, InterruptedException{
//		double prob = 0.00000000001; // Initialize to a very small probability
//		long Nat = 0;
//		long totNat = 0;
//		java.util.Iterator it1 = yearAreasCnt.keySet().iterator();
//		while(it1.hasNext()){
//			String s1 = (String)it1.next();
//			JSONObject inner = (JSONObject)yearAreasCnt.get(s1);
//			java.util.Iterator it2 = inner.keySet().iterator();
//			while(it2.hasNext()) {
//				String s2 = (String)it2.next();
//				if (s1.equalsIgnoreCase(yearT) && s2.equalsIgnoreCase(areaStrT)){
//					Nat = (long)inner.get(s2);
//				}
//				totNat += (long)inner.get(s2);
//			}
//		}
//		if (totNat != 0 && Nat != 0){
//			prob = (double)Nat / totNat;
//		}
//		return prob;
//	}
	
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

	
	private double computeResearcherConservation(String researcherId){
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
		
		for(int i = 0; i < unsortList.size() - 1; i++){
			int j = i+1;
			String year1 = unsortList.get(i);
			String year2 = unsortList.get(j);
			HashMap year1map = (HashMap) resMap.get(year1);
			HashMap year2map = (HashMap) resMap.get(year2);
			Set<String> intersection = customintersection(year1map, year2map);
			Set<String> union = customunion(year1map, year2map);
			double jaccard = (double)intersection.size() / union.size();
//			System.out.println("*******************************************");
//			System.out.println("year1="+year1+"\tyear2="+year2);
//			System.out.println("year1map="+year1map+"\tyear2map="+year2map);
//			System.out.println("intersection="+intersection);
//			System.out.println("union size = "+ union.size() + " intersection size = "+ intersection.size()+" jaccard = "+ jaccard);
//			System.out.println("*******************************************");
			betaList.add(jaccard);
		}
		double jaccardAvg = 0.0;
		System.out.println("BetaList:"+betaList);
		for(int i = 0; i < betaList.size(); i++){
			jaccardAvg += (double)betaList.get(i);
		}
		jaccardAvg = jaccardAvg / betaList.size();
		if (jaccardAvg == 0.0){
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
			testCorpusMapSample.put((String)term, ttp);
		}
	}
	
	public DynResIntConserPerson() {
		analyzer = new EnglishAnalyzer(Version.LUCENE_43);
		alpha = 0.85;
		yearT = "2010";
		yearTPlus = "2012";
		resYearCorpusBuffRead = openResearcherYearCorpus();
		resYearAreaCorpusBuffRead = openResearcherYearAreaCorpus();
		jsonParser = new JSONParser();
		researcherPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
		areaTPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearTAreaIndex");
		areaTPlusPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearTPlusAreaIndex");
		testPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
	}
	
	public static void main(String argv[]) throws InterruptedException, IOException, ParseException {
		DynResIntConserPerson mainClass = new DynResIntConserPerson();
		
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
//		mainClass.testCorpusMap = new HashMap<>();
//		mainClass.testCorpusMap.put("one", new TestTermParams(4, 0.4));
//		mainClass.testCorpusMap.put("two", new TestTermParams(4, 0.3));
//		mainClass.testCorpusMap.put("three", new TestTermParams(4, 0.3));
//		double perpl = mainClass.computePerplexity(mainClass.testCorpusMap);
//		System.out.println(perpl);
//		mainClass.testCorpusMap = new HashMap<>();
//		mainClass.testCorpusMap.put("one", new TestTermParams(4, 0.04));
//		mainClass.testCorpusMap.put("two", new TestTermParams(4, 0.03));
//		mainClass.testCorpusMap.put("threeo", new TestTermParams(4, 0.3));
//		perpl = mainClass.computePerplexity(mainClass.testCorpusMap);
//		System.out.println(perpl);
//		mainClass.testCorpusMap = new HashMap<>();
//		mainClass.testCorpusMap.put("one", new TestTermParams(4, 0.0000004));
//		mainClass.testCorpusMap.put("two", new TestTermParams(4, 0.000003));
//		mainClass.testCorpusMap.put("three", new TestTermParams(4, 0.00003));
//		perpl = mainClass.computePerplexity(mainClass.testCorpusMap);
//		System.out.println(perpl);
//
//		System.exit(1);
		
		try{
			
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
//			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null) && ((resYearAreaCorpus = mainClass.resYearAreaCorpusBuffRead.readLine()) != null)){
//				mainClass.extractFields(resYearCorpus, resYearAreaCorpus);
////				if(mainClass.researcherId.equals("1015097")){
//					System.out.println("ResearchID:"+ mainClass.researcherId+"ResearchName:"+mainClass.researcherName);
//					double testbet = 10 * mainClass.computeResearcherConservation(mainClass.researcherId);
//					System.out.println(testbet);
//					Thread.sleep(1000);
////				}
//			}
//			System.exit(1);
			//Vocab count
			mainClass.vocabCnt = 61776;
			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null) && ((resYearAreaCorpus = mainClass.resYearAreaCorpusBuffRead.readLine()) != null)){
		        IndexReader iResreader;
		        //Extract the necessary fields from the researcher and researcher_area corpus
		        mainClass.extractFields(resYearCorpus, resYearAreaCorpus);
		        
		        System.out.println("Starting process for researcher:" + mainClass.researcherName+":"+mainClass.researcherId);
				//Proceed only if the researcher has corpus for both the years T and TPlus
		        
				if (mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearT) && mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearTPlus)){
					mainClass.beta = mainClass.computeResearcherConservation(mainClass.researcherId);
					mainClass.beta = 10*mainClass.beta;
//					System.out.println(mainClass.beta);
					mainClass.researcherCorpus = (String)mainClass.yearCorpusMap.get(mainClass.yearT);
					mainClass.indexTestCorpusYears(mainClass.yearTPlus);
//					mainClass.readIndex(mainClass.testPath, "yearId", "yearCorpus");
					mainClass.indexResearcherCorpus(mainClass.researcherId, mainClass.researcherCorpus);
					HashMap yearAreasT = (HashMap)mainClass.researcherYearAreaCorpusMap.get(mainClass.yearT);
					String areaStrT;
					String areaCorpusT;
					mainClass.getTestCorpusTerms(mainClass.yearTPlus);
					HashMap yearAreasTPlus = (HashMap)mainClass.researcherYearAreaCorpusMap.get(mainClass.yearTPlus);
					String areaStrTPlus;
					String areaCorpusTPlus;
					DirectoryReader iAreaTReader;
					DirectoryReader iAreaTPlusReader;
					
					for(Object areaTPlus: yearAreasTPlus.keySet()){
						areaStrTPlus = areaTPlus.toString();
						areaCorpusTPlus = (String)yearAreasTPlus.get(areaStrTPlus);
						mainClass.indexAreaTPlusCorpus(mainClass.researcherId, mainClass.researcherName, mainClass.yearTPlus, areaStrTPlus, areaCorpusTPlus);
					}
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
						double baseline3prob = mainClass.getBaseline3Prob(areaStrT, mainClass.yearT);
//						System.out.println("baseline1:"+firstProb+" baseline2:"+baseline2prob+" baseline3:"+baseline3prob);
						
//						if (mainClass.researcherId.equals("527530")){
////							prob_bw.write("Baseline1 Probabilities\n");
//							prob_bw.write("Baseline1 Probabilities\n");
//						}
//						System.out.println("Baseline1");
						mainClass.computeBaselineMap(iAreaTReader, docIDT, firstProb, mainClass.testCorpusMapBaseline1, prob_bw);
						mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline1);
//						System.out.println("**********************");
//						if (mainClass.researcherId.equals("527530")){
////							prob_bw.write("Baseline2 Probabilities\n");
//							prob_bw.write("Baseline2 Probabilities\n");
//						}
//						System.out.println("Baseline2");
						mainClass.computeBaselineMap(iAreaTReader, docIDT, baseline2prob, mainClass.testCorpusMapBaseline2, prob_bw);
						mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline2);
//						System.out.println("**********************");
//						if (mainClass.researcherId.equals("527530")){
////							prob_bw.write("Baseline3 Probabilities\n");
//							prob_bw.write("Baseline3 Probabilities\n");
//						}
//						System.out.println("Baseline3");
						mainClass.computeBaselineMap(iAreaTReader, docIDT, baseline3prob, mainClass.testCorpusMapBaseline3, prob_bw);
						mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline3);
//						System.out.println("**********************");
//						for (Object term : mainClass.testCorpusMap.keySet()) {
//							System.out.println(term + ":"+mainClass.testCorpusMap.get(term).prob+":"+mainClass.testCorpusMapBaseline1.get(term).prob+":"+mainClass.testCorpusMapBaseline2.get(term).prob+":"+mainClass.testCorpusMapBaseline3.get(term).prob+"\n");
//						}
//						System.exit(1);
						double normSecondProb = 0.0;
						iAreaTPlusReader = mainClass.getReaderHandler(mainClass.areaTPlusPath);
						for (int j = 0; j < iAreaTPlusReader.maxDoc(); j++) {
							int docIDTPlus = j;
							Document doc1 = iAreaTReader.document(docIDT);
							Document doc2 = iAreaTPlusReader.document(docIDTPlus);
//							System.out.println(doc1.get("areaName")+"&"+doc2.get("areaName"));
							double secondProb = mainClass.cosineDocumentSimilarity(docIDT, docIDTPlus, iAreaTReader,iAreaTPlusReader, "areaCorpus", "areaCorpus");
							normSecondProb += secondProb;
//							System.out.println("secondProb="+secondProb+" normSecondProb="+normSecondProb);
						}
//						System.out.println("Whats up!!!!!!!!");
						if (normSecondProb == 0.0){
							System.out.println("234324563456537457465678789679867896890678967889789678967899698");
							System.out.println(mainClass.researcherName);
							System.out.println("234324563456537457465678789679867896890678967889789678967899698");
							normSecondProb = 0.0000001;
						}

//						if (mainClass.researcherId.equals("782980")){
//							System.out.println("normSecondProb="+normSecondProb);
//						}

//						System.out.println("#areasTPlus="+iAreaTPlusReader.maxDoc());
						boolean sameArea = false;
						for (int j = 0; j < iAreaTPlusReader.maxDoc(); j++) {
							int docIDTPlus = j;
//							System.out.println("docIDTPlus="+docIDTPlus);
							Document hitDocTPlus = iAreaTPlusReader.document(docIDTPlus);
							areaStrTPlus = hitDocTPlus.get("areaName");
							double secondProb = mainClass.cosineDocumentSimilarity(docIDT, docIDTPlus, iAreaTReader,iAreaTPlusReader, "areaCorpus", "areaCorpus");
							secondProb = secondProb / normSecondProb;
//							System.out.println("before secondProb = "+ secondProb);
							if (areaStrT.equals(areaStrTPlus)){
								sameArea = true;
//								System.out.println("same areas:"+areaStrT+":"+areaStrTPlus);
								secondProb = mainClass.beta * secondProb;
							} else{
//								System.out.println(" same areas:"+areaStrT+":"+areaStrTPlus);
								secondProb = (1 - mainClass.beta) * secondProb;
							}
//							System.out.println("after secondProb = "+ secondProb);
//							mainClass.readIndex(mainClass.areaTPlusPath, "areaName", "areaCorpus");
							mainClass.areaCorpusMap = mainClass.getAreaTerms(iAreaTPlusReader, docIDTPlus);
							int totFreq = mainClass.calculateTotFreq(mainClass.areaCorpusMap);
							TestTermParams ttp;
//							if (mainClass.researcherId.equals("527530")){
//								prob_bw.write("Model Probabilities"+"\n");
//							}
							for (Object term : mainClass.testCorpusMap.keySet()) {
								ttp = mainClass.testCorpusMap.get(term);
								int word_freq = ttp.freq;
								double thirdProb = 0.0;
								int area_term_freq = 0;
								if (mainClass.areaCorpusMap.containsKey(term)) {
									area_term_freq = mainClass.areaCorpusMap.get(term);
								}
//								if (area_term_freq != 0) {
////									System.out.println("Hit!!!");
//									thirdProb = mainClass.alpha * area_term_freq / totFreq;
//								} else {
////									System.out.println("Miss!!!");
//									thirdProb = (1 - mainClass.alpha) * 1 / totFreq;
//								}

								thirdProb = (mainClass.alpha * (area_term_freq / totFreq)) + (1 - mainClass.alpha) * (1 / mainClass.vocabCnt);
								if(thirdProb <= 0.0){
									thirdProb = 0.0000001;
								}
								double cummProb = firstProb * secondProb * thirdProb;
								if (mainClass.researcherId.equals("782980")){
									prob_bw.write(term+":"+firstProb+":"+secondProb+":"+thirdProb+"\n");
								}
								if (cummProb == 0.0){
									cummProb = 0.0000001;
								}
								ttp.prob += cummProb;
								mainClass.testCorpusMap.put((String)term, ttp);
							}
						}
						if (sameArea == false){
						//Accounting for staying in the same area
							double secondProb = mainClass.cosineDocumentSimilarity(docIDT, docIDT, iAreaTReader,iAreaTReader, "areaCorpus", "areaCorpus");
							secondProb = mainClass.beta * secondProb;
							mainClass.areaCorpusMap = mainClass.getAreaTerms(iAreaTReader, docIDT);
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
//								if (area_term_freq != 0) {
//									System.out.println("Hit!!!");
//									thirdProb = mainClass.alpha * area_term_freq / totFreq;
//								} else {
//									System.out.println("Miss!!!");
//									thirdProb = (1 - mainClass.alpha) * 1 / totFreq;
//								}

								thirdProb = (mainClass.alpha * (area_term_freq / totFreq)) + (1 - mainClass.alpha) * (1 / mainClass.vocabCnt);
								double cummProb = firstProb * secondProb * thirdProb;
//								if (mainClass.researcherId.equals("782980")){
//									prob_bw.write(term+":"+firstProb+":"+secondProb+":"+thirdProb+"\n");
//								}
								if (cummProb == 0.0){
									cummProb = 0.0000001;
								}
								ttp.prob += cummProb;
								mainClass.testCorpusMap.put((String)term, ttp);
							}
						}
					mainClass.deleteIndexes("areasT");
					}
//					System.out.println("Normalizing Model Test Corpus");
//					System.out.println("*****************************");
					mainClass.normalizeTestCorpus(mainClass.testCorpusMap);
//					System.out.println("Normalized Model Test Corpus");
//					System.out.println("*****************************");
//					for (Object term : mainClass.testCorpusMap.keySet()) {
//						System.out.println(mainClass.testCorpusMap.get(term).prob+"\t"+mainClass.testCorpusMapBaseline1.get(term).prob+"\t"+mainClass.testCorpusMapBaseline2.get(term).prob+"\t"+mainClass.testCorpusMapBaseline3.get(term).prob);
//					}
					TestTermParams ttp;
					TestTermParams ttp1;
					TestTermParams ttp2;
					TestTermParams ttp3;
					for (Object term : mainClass.testCorpusMap.keySet()) {
						ttp = mainClass.testCorpusMap.get(term);
						ttp1 = mainClass.testCorpusMapBaseline1.get(term);
						ttp2 = mainClass.testCorpusMapBaseline2.get(term);
						ttp3 = mainClass.testCorpusMapBaseline3.get(term);
						int word_freq = ttp.freq;
						double cummProb = ttp.prob;
						double cummProb1 = ttp1.prob;
						double cummProb2 = ttp2.prob;
						double cummProb3 = ttp3.prob;
						System.out.println("Cumm1\t"+cummProb+"\t"+cummProb1+"\t"+cummProb2+"\t"+cummProb3);
						cummProb = Math.pow(cummProb, word_freq);
						cummProb1 = Math.pow(cummProb1, word_freq);
						cummProb2 = Math.pow(cummProb2, word_freq);
						cummProb3 = Math.pow(cummProb3, word_freq);
						if (cummProb == 0.0){
							cummProb = 0.0000001;
						}
						if (cummProb1 == 0.0){
							cummProb1 = 0.0000001;
						}
						if (cummProb2 == 0.0){
							cummProb2 = 0.0000001;
						}
						if (cummProb3 == 0.0){
							cummProb3 = 0.0000001;
						}

						ttp.prob = cummProb;
						ttp1.prob = cummProb1;
						ttp2.prob = cummProb2;
						ttp3.prob = cummProb3;
						System.out.println("Cumm2\t"+cummProb+"\t"+cummProb1+"\t"+cummProb2+"\t"+cummProb3);
						mainClass.testCorpusMap.put((String)term, ttp);
						mainClass.testCorpusMapBaseline1.put((String)term, ttp1);
						mainClass.testCorpusMapBaseline2.put((String)term, ttp2);
						mainClass.testCorpusMapBaseline3.put((String)term, ttp3);
					}
					System.out.println("*****************************************");
					for (Object term : mainClass.testCorpusMap.keySet()) {
						System.out.println(mainClass.testCorpusMap.get(term).prob+"\t"+mainClass.testCorpusMapBaseline1.get(term).prob+"\t"+mainClass.testCorpusMapBaseline2.get(term).prob+"\t"+mainClass.testCorpusMapBaseline3.get(term).prob);
					}
					System.out.println("*****************************************");
					System.out.println("ModelPerplexity");
					double perplexity = mainClass.computePerplexity(mainClass.testCorpusMap);
					System.out.println("*****************************************");
					System.out.println("Baseline1Perplexity");
					double baseline1perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline1);
					System.out.println("*****************************************");
					System.out.println("Baseline2Perplexity");
					double baseline2perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline2);
					System.out.println("*****************************************");
					System.out.println("Baseline3Perplexity");
					double baseline3perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline3);
					System.out.println("*****************************************");
					double cummPerplexity = (0.5 * perplexity) +(0.3 * baseline1perplexity) + (0.1 * baseline2perplexity) + (0.1 * baseline3perplexity);
						System.out.println("ModelPerplexity = " + perplexity);
						System.out.println("Baseline1Perplexity = " + baseline1perplexity);
						System.out.println("Baseline2Perplexity = " + baseline2perplexity);
						System.out.println("Baseline3Perplexity = " + baseline3perplexity);
						System.out.println("cummPerplexity = " + cummPerplexity);
					bw1.write(mainClass.researcherId+"\t"+perplexity +"\t"+baseline1perplexity+"\t"+baseline2perplexity+"\t"+baseline3perplexity+"\t"+cummPerplexity+"\t"+Math.round(mainClass.beta)+"\n");
		    	}
				mainClass.deleteIndexes("areasT");
				mainClass.deleteIndexes("areasTPlus");
				mainClass.deleteIndexes("test");
				mainClass.deleteIndexes("researcher");
				System.exit(1);
			}
			bw1.close();
		}catch(IOException e){
			e.printStackTrace();
		}catch (ParseException e) {
			e.printStackTrace();
		}
	}
}