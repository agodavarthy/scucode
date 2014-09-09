
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

public class AllModelsExtension {
	private Map<String, TestTermParams> testCorpusMap;
	private Map<String, TestTermParams> testCorpusMapBaseline1;
	private Map<String, TestTermParams> testCorpusMapBaseline2;
	private Map<String, TestTermParams> testCorpusMapBaseline3;
	private Map<String, Integer> areaCorpusMap;
	private Analyzer analyzer;
	private double alpha;
	private double beta;
	private double gamma;
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
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_corpus_arnet_bioinfo.json"));
			return br;
		} catch (IOException e) {
			e.printStackTrace();
			return br;
		}
	}
	
	private BufferedReader openResearcherYearAreaCorpus() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_areas_arnet_bioinfo.json"));
			return br;
		} catch (IOException e) {
			e.printStackTrace();
			return br;
		}
	}
	
	
	private BufferedReader openTestCorpus() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/test_year_corpus_arnet_bioinfo.json"));
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
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoTestCorpusIndex");
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
			System.out.println(yearCorpusMap);
			System.out.println(yearTPlus);
			Thread.sleep(1000);
			System.out.println("In indexTestCorpus\n");
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
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoResearcherCorpusIndex");
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
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoResearcherCorpusVocabIndex");
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
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoYearTAreaIndex");
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
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoYearTPlusAreaIndex");
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
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoALLAreasIndex");
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
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/year_areas_cnt_arnet_bioinfo.json"));
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
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/researcher_year_areas_cnt_arnet_bioinfo.json"));
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
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoTestCorpusIndex");
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
		System.out.println(testCorpusMap.size());
	}
	
    private double computePerplexity(Map<String, TestTermParams> testCorpusMap) throws InterruptedException{
    	double perplexity = 0.0;
    	double sumLogProb = 0.0;
    	double l_perplexity = 0.0;
    	int totTermFreqs = calculateTotTestCorpusTerms(testCorpusMap);
    	int testFreq = 0;

		for (Object term : testCorpusMap.keySet()) {
			TestTermParams ttp = testCorpusMap.get(term);
//			System.out.println("prob:"+ttp.prob);
				double prob = Math.log(ttp.prob)/ Math.log(2);
//				System.out.println("logbase2:"+prob);
//				System.out.print("logbase2:"+prob);
				prob = prob * ttp.freq;
				testFreq += ttp.freq;
//				System.out.println("prob freq:"+prob);
				sumLogProb += prob;
//				System.out.println("sumLogProb:"+sumLogProb);
//				System.out.println();
		}
		l_perplexity = sumLogProb / totTermFreqs;
//		System.out.println("sumLogProb / totTermFreqs:"+l_perplexity);
		perplexity = Math.pow(2, -1.0*l_perplexity);
//    	System.out.println("totTermFreq="+totTermFreqs);
//    	System.out.println("testFreq="+testFreq);
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
//		System.out.println(yearT);
//		System.out.println(yearAreasCnt);
//		System.exit(1);
		HashMap yearAreas = (HashMap)yearAreasCnt.get(yearT);
		if (yearAreas.containsKey(areaStrT)){
			Nat = (long)yearAreas.get(areaStrT);
		} else {
//			System.out.println("@#!@#!!#!@#!#!@#!   "+areaStrT+"    does NOT appear in "+yearT);
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
	
	public AllModelsExtension() {
		analyzer = new EnglishAnalyzer(Version.LUCENE_43);
		alpha = 0.85;
		gamma = 0.5;
		yearT = "2001";
		yearTPlus = "2003";
		resYearCorpusBuffRead = openResearcherYearCorpus();
		resYearAreaCorpusBuffRead = openResearcherYearAreaCorpus();
		jsonParser = new JSONParser();
		researcherPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoResearcherCorpusIndex");
		areaTPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoYearTAreaIndex");
		areaALLPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoALLAreasIndex");
		areaTPlusPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoYearTPlusAreaIndex");
		testPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoTestCorpusIndex");
	}
	
	public static void main(String argv[]) throws InterruptedException, IOException, ParseException {
		AllModelsExtension mainClass = new AllModelsExtension();
		System.out.println("Trying GitHub");
		String resYearCorpus = null;
		String resYearAreaCorpus = null;
		
		if (mainClass.resYearCorpusBuffRead == null){
			System.out.print("Could not open the file");
			System.exit(1);
		}
		mainClass.getYearAreasCntDic();
		mainClass.getResYearAreasCntDic();
		System.out.println("Done with YearAreasCntDic");
		
		try{
			//Vocab count
//			mainClass.indexResearcherCorpusForVocab(String researcherCorpus);
			mainClass.vocabCnt = 3755;
			System.out.println(mainClass.yearTPlus);
			mainClass.deleteIndexes("test");
			mainClass.indexTestCorpusYears(mainClass.yearTPlus);
			mainClass.readIndex(mainClass.testPath, "yearId", "yearCorpus");
			DirectoryReader iTestReader = mainClass.getReaderHandler(mainClass.testPath);
			DirectoryReader iAreaALLReader = mainClass.getReaderHandler(mainClass.areaALLPath);
			int yeargap = Integer.parseInt(mainClass.yearTPlus) - Integer.parseInt(mainClass.yearT);
			BufferedWriter bw1 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/perplexities"+yeargap));
			BufferedWriter bw3 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/researcher_top_areas_predict_year"+yeargap));
			BufferedWriter currentTopAreas = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/researcher_top_areas_current_year"+yeargap));
			bw1.write("Name\tModel Perplexity\tBaseline1 Perplexity\tBaseline2 Perplexity\tBaseline3 Perplexity\tCummulative Perplexity\n");

			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null) && ((resYearAreaCorpus = mainClass.resYearAreaCorpusBuffRead.readLine()) != null)){
		        IndexReader iResreader;
		        //Extract the necessary fields from the researcher and researcher_area corpus
		        mainClass.extractFields(resYearCorpus, resYearAreaCorpus);
		        
				//Proceed only if the researcher has corpus for both the years T and TPlus
		        
		        if (mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearT)){
					HashMap unSortedResearcherAreas = new HashMap();
					Map sortedResearcherAreas = new HashMap();

					Set unSortedResearcherAreasT = new HashSet();
//					Map sortedResearcherAreasT = new HashMap();
					
		        	System.out.println("Starting process for researcher:" + mainClass.researcherName+":"+mainClass.researcherId);
		        	System.out.println("*********************************");
		        	mainClass.beta = mainClass.computeResearcherConservation(mainClass.researcherId, mainClass.yearT);
		        	
//		        	if(mainClass.beta == 0.0) continue;
//		        	mainClass.beta = 10 * mainClass.beta;
//		        	if (mainClass.beta > 1.0){
////		        		System.out.println("BETA MORE THAN 1 for researcher="+ mainClass.researcherName);
//		        		mainClass.beta = 1.0;
//		        	}
		        	System.out.println("mainClass.beta:"+mainClass.beta);
					mainClass.researcherCorpus = (String)mainClass.yearCorpusMap.get(mainClass.yearT);
					mainClass.indexResearcherCorpus(mainClass.researcherId, mainClass.researcherCorpus);
					HashMap yearAreasT = (HashMap)mainClass.researcherYearAreaCorpusMap.get(mainClass.yearT);
					String areaStrT;
					String areaCorpusT;
					mainClass.getTestCorpusTerms(mainClass.yearTPlus);
					String areaStrTPlus;
					String areaCorpusTPlus;
					DirectoryReader iAreaTReader;
					
					for(Object areaT: yearAreasT.keySet()){
						areaStrT = areaT.toString();
						areaCorpusT = (String)yearAreasT.get(areaStrT);
						mainClass.indexAreaTCorpus(mainClass.researcherId, mainClass.researcherName, mainClass.yearT, areaStrT, areaCorpusT);
					}
					double normFirstProb = 0.0;
					iAreaTReader = mainClass.getReaderHandler(mainClass.areaTPath);
					iResreader = mainClass.getReaderHandler(mainClass.researcherPath);
					for(int i = 0; i < iAreaTReader.maxDoc(); i++){
						int docID = i;
						Document doc = iAreaTReader.document(docID);
						String area = doc.get("areaName");
						unSortedResearcherAreasT.add(area);
						double score = mainClass.beta * mainClass.getBaseline2Prob(area, mainClass.yearT);
						if (score > 0.0){
							unSortedResearcherAreas.put(area, score);
							
						}
					}
					
					int resDocID = iResreader.maxDoc() - 1;
					for(int i = 0; i < iAreaTReader.maxDoc(); i++){
						int docIDT = i;
						Document doc1 = iAreaTReader.document(docIDT);
						double firstProb = mainClass.cosineDocumentSimilarity(resDocID, docIDT, iResreader, iAreaTReader, "researcherCorpus", "areaCorpus");
						normFirstProb += firstProb;
					}
					int TotNumAreasT = yearAreasT.size();
					int TotNumAreasTPlus = iAreaALLReader.maxDoc() - yearAreasT.size();
					ArrayList researcherAreasT = new ArrayList();
					for(Object areaT: yearAreasT.keySet()){
						researcherAreasT.add(areaT); 
					}
//					System.out.println(researcherAreasT);
					double baseline3NormProb = 0.0;
					for(int i = 0; i < iAreaTReader.maxDoc(); i++){
						Document doc = iAreaTReader.document(i);
						String area = doc.get("areaName");
						double prob = mainClass.getBaseline3Prob(area, mainClass.yearT);
						baseline3NormProb += prob;
					}
					for(int i = 0; i < iAreaTReader.maxDoc(); i++){
						int docIDT = i;
						Document doc = iAreaTReader.document(docIDT);
						areaStrT = doc.get("areaName");
						double firstProb = mainClass.cosineDocumentSimilarity(resDocID, docIDT, iResreader, iAreaTReader, "researcherCorpus", "areaCorpus");
						firstProb = firstProb / normFirstProb;
						double baseline2prob = mainClass.getBaseline2Prob(areaStrT, mainClass.yearT);
						double baseline3prob = mainClass.getBaseline3Prob(areaStrT, mainClass.yearT);
						baseline3prob = baseline3prob / baseline3NormProb;
						mainClass.computeBaselineMap(iAreaTReader, docIDT, firstProb, mainClass.testCorpusMapBaseline1);
						mainClass.computeBaselineMap(iAreaTReader, docIDT, baseline2prob, mainClass.testCorpusMapBaseline2);
						mainClass.computeBaselineMap(iAreaTReader, docIDT, baseline3prob, mainClass.testCorpusMapBaseline3);
//						**********This Loop is only to get the top scores with the areaT*******************
						Map<Integer, Double> unsortMap = new HashMap<Integer, Double>();
						int howManyMatches = 0;
						for (int j = 0; j < iAreaALLReader.maxDoc(); j++) {
							int docIDTPlus = j;
							Document doc1 = iAreaTReader.document(docIDT);
							Document doc2 = iAreaALLReader.document(docIDTPlus);
							areaStrTPlus = doc2.get("areaName");
							if (researcherAreasT.contains(areaStrTPlus)){
								howManyMatches++;
								continue;
							}
							double secondProb = mainClass.cosineDocumentSimilarity(docIDT, docIDTPlus, iAreaTReader,iAreaALLReader, "areaCorpus", "areaCorpus");
							unsortMap.put(docIDTPlus, secondProb);
						}
						Map<Integer, Double> sortedMap = sortByComparator(unsortMap);
						LinkedHashMap top5 = new LinkedHashMap<>();
						int top5Count = 0;
						for (Object key : sortedMap.keySet()) {
							int docID = (int)key;
							doc = iAreaALLReader.document(docID);
							String area = doc.get("areaName");
							double score = sortedMap.get(key);
//							This is done for the new model to include 
							if (mainClass.getBaseline3Prob(area, mainClass.yearTPlus) == 0.0)
								continue;
							top5Count++;
							top5.put(docID, score);
							if (top5Count > 5)break;
						}
						double normSecondProb = 0.0;
						for (Object key : top5.keySet()) {
							double score = (double)top5.get(key);
							normSecondProb += score;
						}
						
//						********************* For areas in the current year for the researcher *******************
						for (int newi = 0; newi < iAreaTReader.maxDoc(); newi++){
							int docID = newi;
							doc = iAreaTReader.document(docID);
							String area = doc.get("areaName");
							double sameAreaProb = mainClass.getBaseline2Prob(area.toString(), mainClass.yearT);
							
							double secondProb = sameAreaProb * mainClass.beta;
							mainClass.areaCorpusMap = mainClass.getAreaTerms(iAreaTReader, docID);
							int totAreaFreq = mainClass.calculateTotFreq(mainClass.areaCorpusMap);
							LinkedHashMap<String, Double> thirProbs = new LinkedHashMap<>();
							for (Object term : mainClass.testCorpusMap.keySet()) {
								double thirdProb = 0.0;
								int area_term_freq = 0;
								if (mainClass.areaCorpusMap.containsKey(term)) {
									area_term_freq = mainClass.areaCorpusMap.get(term);
								}
								thirdProb = (mainClass.alpha * ((double)area_term_freq / totAreaFreq)) + (1 - mainClass.alpha) * (1.0 / mainClass.vocabCnt);
								thirProbs.put(term.toString(), thirdProb);
							}
							double normThirdProb = 0.0;
							for (String term : thirProbs.keySet()) {
								double prob = thirProbs.get(term);
								normThirdProb += prob;
							}
							for (Object term : mainClass.testCorpusMap.keySet()) {
								TestTermParams ttp;
								ttp = mainClass.testCorpusMap.get(term);
								double thirdProb = thirProbs.get(term);
								thirdProb = thirdProb / normThirdProb;
								double cummProb = firstProb * secondProb * thirdProb;
								ttp.prob += cummProb;
								mainClass.testCorpusMap.put((String)term, ttp);
							}
						}
						
						double baseline3AreaTPlusNormProb = 0.0;
						for (Object key : top5.keySet()) {
							int docID = (int)key;
							doc = iAreaALLReader.document(docID);
							String area = doc.get("areaName");

							double prob = mainClass.getBaseline3Prob(area, mainClass.yearTPlus);
							baseline3AreaTPlusNormProb += prob;
						}

						
						for (Object key : top5.keySet()) {
							int docID = (int)key;
							doc = iAreaALLReader.document(docID);
							String area = doc.get("areaName");

							double similarity = (double)top5.get(key);
							double secondProb = (similarity / normSecondProb);
							double newThirdprob = mainClass.getBaseline3Prob(area, mainClass.yearTPlus);
							double secondProb_hotness = mainClass.gamma * secondProb + (1.0 - mainClass.gamma) * (newThirdprob/baseline3AreaTPlusNormProb);
							secondProb_hotness = secondProb_hotness * (1.0 - mainClass.beta);
							secondProb = secondProb_hotness;
							if (secondProb > 0.0){
								unSortedResearcherAreas.put(area, secondProb);
							}

							mainClass.areaCorpusMap = mainClass.getAreaTerms(iAreaALLReader, docID);
							int totAreaFreq = mainClass.calculateTotFreq(mainClass.areaCorpusMap);
							LinkedHashMap<String, Double> thirProbs = new LinkedHashMap<>();
							
							for (Object term : mainClass.testCorpusMap.keySet()) {
								double thirdProb = 0.0;
								int area_term_freq = 0;
								if (mainClass.areaCorpusMap.containsKey(term)) {
									area_term_freq = mainClass.areaCorpusMap.get(term);
								}
								thirdProb = (mainClass.alpha * ((double)area_term_freq / totAreaFreq)) + (1 - mainClass.alpha) * (1.0/mainClass.vocabCnt);
								thirProbs.put(term.toString(), thirdProb);
							}
							double normThirdProb = 0.0;
							for (String term : thirProbs.keySet()) {
								double prob = thirProbs.get(term);
								normThirdProb += prob;
							}
							for (Object term : mainClass.testCorpusMap.keySet()) {
								TestTermParams ttp;
								ttp = mainClass.testCorpusMap.get(term);
								double thirdProb = thirProbs.get(term);
								thirdProb = thirdProb / normThirdProb;
								double cummProb = firstProb * secondProb * thirdProb;
								ttp.prob += cummProb;
								mainClass.testCorpusMap.put((String)term, ttp);
							}
						}
					}
					mainClass.normalizeTestCorpus(mainClass.testCorpusMap);
					mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline1);
					mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline2);
					mainClass.normalizeTestCorpus(mainClass.testCorpusMapBaseline3);


					double perplexity = mainClass.computePerplexity(mainClass.testCorpusMap);
					double baseline1perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline1);
					double baseline2perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline2);
					double baseline3perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline3);
					double cummPerplexity = (0.5 * perplexity) +(0.3 * baseline1perplexity) + (0.1 * baseline2perplexity) + (0.1 * baseline3perplexity);
					System.out.println("ModelPerplexity = " + perplexity);
					System.out.println("Baseline1Perplexity = " + baseline1perplexity);
					System.out.println("Baseline2Perplexity = " + baseline2perplexity);
					System.out.println("Baseline3Perplexity = " + baseline3perplexity);
					System.out.println("cummPerplexity = " + cummPerplexity);
					if (Double.isNaN(cummPerplexity) == false){
						bw1.write(mainClass.researcherName+"\t"+perplexity +"\t"+baseline1perplexity+"\t"+baseline2perplexity+"\t"+baseline3perplexity+"\t"+cummPerplexity+"\n");
						sortedResearcherAreas = sortByComparator(unSortedResearcherAreas);
//						Collecting Top areas of researcher
						bw3.write(mainClass.researcherName+"("+mainClass.beta+")\t");
						int resAreasCnt = 0;
						for(Object resArea: sortedResearcherAreas.keySet()){
							resAreasCnt++;
							if (resAreasCnt >=10) break;
							bw3.write(resArea+"\t");
						}
						bw3.write("\n");

//						sortedResearcherAreasT = sortByComparator(unSortedResearcherAreasT);
						currentTopAreas.write(mainClass.researcherName+"\t");
						for(Object resArea: unSortedResearcherAreasT){
							currentTopAreas.write(resArea+"\t");
						}
						currentTopAreas.write("\n");
					}
//					System.exit(1);
		    	}
				mainClass.deleteIndexes("areasT");
				mainClass.deleteIndexes("researcher");
			}
			bw1.close();
			bw3.close();
			currentTopAreas.close();
		}catch(IOException e){
			e.printStackTrace();
		}catch (ParseException e) {
			e.printStackTrace();
		}
	}
}