package LatestWorkingCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class DynResInt_OnlyWords {
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
	
	private void extractFields(String resYearCorpus) throws ParseException{
		Object resYearObj = jsonParser.parse(resYearCorpus);
		JSONObject resYearJSONObj = (JSONObject)resYearObj;
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
	private String getTestCorpusForYear(String yearTPlus) throws IOException, ParseException, InterruptedException{
		JSONParser jsonParser = new JSONParser();
		BufferedReader testCorpusBuffRead = openTestCorpus();
		String yearCorpus = "";
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
			yearCorpus = (String)yearCorpusMap.get(yearTPlus);
		}
		return yearCorpus;
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

    public int readIndexSpecificDoc(DirectoryReader ireader, int docID, String id, String field) throws IOException{
    		int i = docID;
    		int totwords = 0;
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
   					totwords += freq;
   					System.out.print(term + ":" + freq + " ");
   				}
   	       } else {
   	    	   System.out.println("vector is null");
   	       }
    	    System.out.println();
    	    return totwords;
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

	private HashMap getResearcherCorpusTerms() throws IOException {
		File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	IndexSearcher isearcher = new IndexSearcher(ireader);
		
		Terms year_terms = ireader.getTermVector(0, "researcherCorpus");
		TermsEnum termsEnum = null;
		termsEnum = year_terms.iterator(termsEnum);
		BytesRef name = null;
		HashMap researcherMap = new HashMap<>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			researcherMap.put(term, freq);
		}
		return researcherMap;
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
		ArrayList<String> wordsTobeDeleted = new ArrayList(Arrays.asList("about", "among", "between", "both", "can", "co", "e", "edbt", "els", "etc", "fewer", "find", "give", "hat", "have", "help", "held", "hold", "how", "just", "low", "made", "main", "more", "real", "show", "than", "them", "us", "we", "what", "when", "where", "which", "x", "y", "yet","sham", "symposium", "vldb", "european", "grei", "sang", "doctor", "misguid", "agglom", "amnesia", "taili", "nijmegen", "forag", "subsumpt", "cooper", "winter", "taia2013", "anyth", "fight", "misunderstand", "dyadic", "asweb", "thesauru", "atyp", "basket", "anteced", "heritag", "fruitfulli", "eurohcir2013", "unari", "geocod", "negat", "someon", "singer", "iiix", "subsum", "upperbound", "physiolog", "musician", "pairs", "hawk", "shame", "patti", "subjectpred", "underperform", "fifth", "sector", "trap", "japanes", "august", "looselycoupl", "generatingimpl", "netherland", "searchresultfind", "summer"));
		
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
			if (wordsTobeDeleted.contains(term)) continue;
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
	
    private double computePerplexity(Map<String, TestTermParams> testCorpusMap) throws InterruptedException{
    	double perplexity = 0.0;
    	double sumLogProb = 0.0;
    	double l_perplexity = 0.0;
    	int testFreq = 0;

		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
			if (ttp.prob == 0.0)continue;
			double prob = Math.log(ttp.prob)/ Math.log(2);
			prob = prob * ttp.freq;
			testFreq += ttp.freq;
			sumLogProb += prob;
		}
		l_perplexity = sumLogProb / testFreq;
		perplexity = Math.pow(2, -1.0*l_perplexity);
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
	public void computeBaselineMap(IndexReader iAreaReader, int docID, double firstProb, Map<String, TestTermParams> commonTestCorpusMapBaseline) throws IOException, InterruptedException{
		areaCorpusMap = getAreaTerms(iAreaReader, docID);
		Document d = iAreaReader.document(docID);
		String area = d.get("areaName");
		int totFreq = calculateTotFreq(areaCorpusMap);
		TestTermParams ttp;
//		System.out.println("totFreq:"+totFreq);
//		System.out.println("FirstProb:"+firstProb);
//		System.out.println("Area:"+area);
		LinkedHashMap<String, Double> thirdProbsMAP = new LinkedHashMap<>();
		for (Object term : commonTestCorpusMapBaseline.keySet()) {
			double thirdProb = 0.0;
			int area_term_freq = 0;
			if (areaCorpusMap.containsKey(term)) {
				area_term_freq = areaCorpusMap.get(term);
			}
			thirdProb = (alpha * ((double)area_term_freq / totFreq)) + (1 - alpha) * (1.0 / vocabCnt);
//			System.out.println(area_term_freq+" "+term+"\tthirdProb:"+thirdProb);
			thirdProbsMAP.put(term.toString(), thirdProb);
		}

		double normThirdProb = 0.0;
		for (String term : thirdProbsMAP.keySet()) {
			double prob = thirdProbsMAP.get(term);
			normThirdProb += prob;
		}
		for (Object term : commonTestCorpusMapBaseline.keySet()) {
			ttp = commonTestCorpusMapBaseline.get(term);
			double thirdProb = thirdProbsMAP.get(term);
			thirdProb = thirdProb / normThirdProb;
			double cummProb = firstProb  * thirdProb;
			ttp.prob += cummProb;
//			System.out.println(term+" ttp.prob:"+ttp.prob+" cummProb:"+cummProb);
//			commonTestCorpusMapBaseline.put((String)term, ttp);
		}
		double finprob = 0.0;
		for (Object term : commonTestCorpusMapBaseline.keySet()) {
			ttp = commonTestCorpusMapBaseline.get(term);
			finprob += ttp.prob;
		}
//		System.out.println("Baseline:finalProb="+finprob);
	}

	public double getBaseline2Prob(String areaStrT, String yearT) throws IOException, InterruptedException{
//		double prob = 0.00000000001; // Initialize to a very small probability
		double prob = 0.0;
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
					if (s1.equalsIgnoreCase(yearT)){
						totNate += (long)inner.get(s2);
						if(s2.equalsIgnoreCase(areaStrT)){
							Nate = (long)inner.get(s2);
						}
					}
				}
			}
		}
		if (totNate != 0 && Nate != 0){
			prob = (double)Nate / totNate;
		}
		return prob;
	}

	public double getBaseline3Prob(String areaStrT, String yearT) throws IOException, InterruptedException{
//		double prob = 0.00000000001; // Initialize to a very small probability
		double prob = 0.0;
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
			if (Integer.parseInt(year2) > Integer.parseInt(yearT))
				break;
			HashMap year1map = (HashMap) resMap.get(year1);
			HashMap year2map = (HashMap) resMap.get(year2);
			Set<String> intersection = customintersection(year1map, year2map);
			Set<String> union = customunion(year1map, year2map);
			double jaccard = (double)intersection.size() / union.size();
			betaList.add(jaccard);
		}
		double jaccardAvg = 0.0;
		for(int i = 0; i < betaList.size(); i++){
			jaccardAvg += (double)betaList.get(i);
		}
		if (betaList.size() > 0)
			jaccardAvg = jaccardAvg / betaList.size();
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
	private int getTotResTerms(HashMap researcherMap){
		int tot = 0;
		for(Object term: researcherMap.keySet()){
			tot += (int)researcherMap.get(term);
		}
		return tot;
	}
	public DynResInt_OnlyWords() {
		analyzer = new EnglishAnalyzer(Version.LUCENE_43);
		alpha = 0.85;
		yearT = "2012";
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
		DynResInt_OnlyWords mainClass = new DynResInt_OnlyWords();
		
		String resYearCorpus = null;
		String resYearAreaCorpus = null;
		
		BufferedWriter prob_bw = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/Probabilities_badperplexity"));
		BufferedWriter bw1 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/perplexities"));
		BufferedWriter bw2 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/researcher_representative_words"));
		BufferedWriter bw3 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/researcher_top_areas"));
		bw1.write("Name\tModel Perplexity\tBaseline1 Perplexity\tBaseline2 Perplexity\tBaseline3 Perplexity\tCummulative Perplexity\n");
		if (mainClass.resYearCorpusBuffRead == null){
			System.out.print("Could not open the file");
			System.exit(1);
		}
		mainClass.getYearAreasCntDic();
		mainClass.getResYearAreasCntDic();
		
		try{
			//Vocab count
			mainClass.vocabCnt = 3840;
			mainClass.indexTestCorpusYears(mainClass.yearTPlus);
			DirectoryReader iTestReader = mainClass.getReaderHandler(mainClass.testPath);
			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null)){
				mainClass.extractFields(resYearCorpus);
				String researcherCorpus = "";
				if(mainClass.yearCorpusMap.containsKey(mainClass.yearT)){
					mainClass.researcherCorpus = (String)mainClass.yearCorpusMap.get(mainClass.yearT);
					mainClass.indexResearcherCorpus(mainClass.researcherId, mainClass.researcherCorpus);
//					DirectoryReader ireader = mainClass.getReaderHandler(mainClass.researcherPath);
					mainClass.getTestCorpusTerms(mainClass.yearTPlus);
//					mainClass.readIndexSpecificDoc(ireader, 0, "researcherId", "researcherCorpus");
					HashMap researcherMap = mainClass.getResearcherCorpusTerms();

					int totResTerms = mainClass.getTotResTerms(researcherMap);
					TestTermParams ttp;
					for (Object term : mainClass.testCorpusMap.keySet()) {
						double prob = 0.0;
						ttp = mainClass.testCorpusMap.get(term);
						int word_freq = 0;
						if (researcherMap.containsKey(term)){
							word_freq = (int)researcherMap.get(term);
						}
						double alpha = 0.6;
						double vocabCnt = 3840;
						prob = alpha * (1.0 * word_freq / totResTerms) + (1 - alpha) * (1.0/ vocabCnt);
						ttp.prob = prob;
					}
//					for (Object term : mainClass.testCorpusMap.keySet()) {
//						System.out.print(term.toString()+":"+mainClass.testCorpusMap.get(term).prob+"\t");
//					}
//					System.out.println();
//					System.out.println("*************************");

					double perplexity = mainClass.computePerplexity(mainClass.testCorpusMap);
					System.out.println(mainClass.researcherName+":Perplexity = " + perplexity);
					mainClass.deleteIndexes("researcher");
//					System.exit(1);
				} else continue;
		        
			}
			bw1.close();
			bw2.close();
			bw3.close();
		}catch(IOException e){
			e.printStackTrace();
		}catch (ParseException e) {
			e.printStackTrace();
		}
	}
}