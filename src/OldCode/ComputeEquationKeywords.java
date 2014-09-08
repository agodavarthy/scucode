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

public class ComputeEquationKeywords {
	private Map<String, TestTermParams> testCorpusMap;
	private Map<String, TestTermParams> testCorpusMapBaseline;
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
			br = new BufferedReader(
					new FileReader(
							"/home/archana/SCU_projects/research_changes/lucene/researcher_year_corpus.json"));
			return br;
		} catch (IOException e) {
			e.printStackTrace();
			return br;
		}
	}

	private BufferedReader openResearcherYearAreaCorpus() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new FileReader(
							"/home/archana/SCU_projects/research_changes/lucene/researcher_year_areas.json"));
			return br;
		} catch (IOException e) {
			e.printStackTrace();
			return br;
		}
	}
	
	private BufferedReader openTestCorpus() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new FileReader(
							"/home/archana/SCU_projects/research_changes/lucene/test_year_corpus.json"));
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
		Object resYearAreaObj = jsonParser.parse(resYearWiseAreaCorpus);
		JSONObject resYearAreaJSONObj = (JSONObject)resYearAreaObj;
		researcherYearAreaCorpusMap = (HashMap)resYearAreaJSONObj.get("year_area_corpus");
		researcherId = (String)resYearJSONObj.get("_id");
		researcherName = (String)resYearJSONObj.get("name");
		String resID = (String)resYearAreaJSONObj.get("_id");
		String resName = (String)resYearAreaJSONObj.get("name");
		System.out.println(researcherId+":"+resID);
		System.out.println(researcherName+":"+resName);
		if (resYearJSONObj.get("year_corpus") != null){
			yearCorpusMap = (HashMap)resYearJSONObj.get("year_corpus");
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
		testCorpusMapBaseline = new HashMap<String, TestTermParams>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			TestTermParams ttp1 = new TestTermParams(freq, prob);
			TestTermParams ttp2 = new TestTermParams(freq, prob);
			testCorpusMap.put(term, ttp1);
			testCorpusMapBaseline.put(term, ttp2);
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
	
	public void computeBaselineMap(IndexReader iAreaReader, int docID, double firstProb) throws IOException, InterruptedException{
		areaCorpusMap = getAreaTerms(iAreaReader, docID);
		int totFreq = calculateTotFreq(areaCorpusMap);
		getTestCorpusTerms(yearTPlus);
		TestTermParams ttp;
		for (Object term : testCorpusMapBaseline.keySet()) {
			ttp = testCorpusMapBaseline.get(term);
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
			testCorpusMapBaseline.put((String)term, ttp);
		}
	}
//	public HashMap computeResYearAreas(){
//		HashMap resYearAreas = new Hash
//	}
	public ComputeEquationKeywords(){
		analyzer = new EnglishAnalyzer(Version.LUCENE_43);
		alpha = 0.85;
		yearT = "2011";
		yearTPlus = "2012";
		resYearCorpusBuffRead = openResearcherYearCorpus();
		resYearAreaCorpusBuffRead = openResearcherYearAreaCorpus();
		jsonParser = new JSONParser();
		researcherPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
		areaPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
		testPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
	}
	
	public static void main(String argv[]) throws InterruptedException, IOException {
		ComputeEquationKeywords mainClass = new ComputeEquationKeywords();
		
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
		
		try{
			int resCnt = 1;
			while (((resYearCorpus = mainClass.resYearCorpusBuffRead.readLine()) != null) && ((resYearAreaCorpus = mainClass.resYearAreaCorpusBuffRead.readLine()) != null)){
		        IndexReader iResreader;
		        //Extract the necessary fields from the researcher and researcher_area corpus
		        mainClass.extractFields(resYearCorpus, resYearAreaCorpus);
		        System.exit(1);
		        System.out.println("Starting process for researcher:" + mainClass.researcherName);
				//Proceed only if the researcher has corpus for both the years T and TPlus
				if (mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearT) && mainClass.researcherYearAreaCorpusMap.containsKey(mainClass.yearTPlus)){
//					model_prob_bw.write("*************"+mainClass.researcherName+"*************"+"\n");
//					baseline_prob_bw.write("*************"+mainClass.researcherName+"*************"+"\n");
					
					if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
						test_temp.write("*************"+mainClass.researcherName+"*************"+"\n");
					}
					mainClass.researcherCorpus = (String)mainClass.yearCorpusMap.get(mainClass.yearT);
					mainClass.indexTestCorpusYears(mainClass.yearTPlus);
//					mainClass.readIndex(mainClass.testPath, "yearId", "yearCorpus");
					mainClass.indexResearcherCorpus(mainClass.researcherId, mainClass.researcherCorpus);
					HashMap yearAreas = (HashMap)mainClass.researcherYearAreaCorpusMap.get(mainClass.yearT);
					String areaStrT;
					String areaCorpusT;
					mainClass.getTestCorpusTerms(mainClass.yearTPlus);
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
						if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
							test_temp.write(hitDocT.get("year")+" "+hitDocT.get("areaName")+":firstProb="+firstProb+"\n");
						}
//						model_prob_bw.write(hitDocT.get("year")+" "+hitDocT.get("areaName")+" "+firstProb+"\n");
//						baseline_prob_bw.write(hitDocT.get("year")+" "+hitDocT.get("areaName")+" "+firstProb+"\n");
						mainClass.computeBaselineMap(iAreaReader, docIDT, firstProb);
						
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
					}
					double perplexity = mainClass.computePerplexity(mainClass.testCorpusMap);
					double baseline1perplexity = mainClass.computePerplexity(mainClass.testCorpusMapBaseline);
					double baseline2perplexity = mainClass.computeBaseline2Perplexity(mainClass.testCorpusMapBaseline);
					double baseline3perplexity = mainClass.computeBaseline3Perplexity(mainClass.testCorpusMapBaseline);
					System.out.println("ModelPerplexity = " + perplexity);
					System.out.println("BaselinePerplexity = " + baseline1perplexity);
//					model_prob_bw.write("*************"+mainClass.researcherName+"*************"+"\n");
//					baseline_prob_bw.write("*************"+mainClass.researcherName+"*************"+"\n");
					if (mainClass.researcherId.equals("LfF2zfQAAAAJ") || mainClass.researcherId.equals("MlZq4XwAAAAJ")){
						test_temp.write("*************"+mainClass.researcherName+"*************"+"\n");
					}

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