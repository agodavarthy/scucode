package OldCode;
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
import java.util.regex.Pattern;

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

public class ComputeEquationKeywords_21Dec2013_832 {
	private Map<String, TestTermParams> testCorpusMap;
	private Map<String, TestTermParams> testCorpusMapBaseline;
	private Map<String, Integer> areaCorpusMap;
	private Map<String, Integer> areaCorpusMapBaseline;

    public static final FieldType TYPE_STORED = new FieldType();
    private static final String CONTENT = "areaCorpus";
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

	private BufferedReader openResearcherYearWiseAreaCorpus() {
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
        System.out.println("Done with researcher freqs");

    	Map<String, Integer> f2 = getTermFrequencies(reader2, d2, content2);
        System.out.println("Done with area freqs");
        RealVector v1;
        RealVector v2;

        v1 = toRealVector(f1);
        v2 = toRealVector(f2);
        return getCosineSimilarity(v1, v2);
    }

    private static String escapeQuery(String line) {
		final String LUCENE_ESCAPE_CHARS = "[\\\\+\\-\\!\\(\\)\\:\\^\\[\\]\\{\\}\\~\\*\\?\\/\\%]";
		final Pattern LUCENE_PATTERN = Pattern.compile(LUCENE_ESCAPE_CHARS);
		final Pattern ALPHANUM = Pattern.compile("[a-zA-Z 0-9]");
		final String REPLACEMENT_STRING = "\\\\$0";
		return LUCENE_PATTERN.matcher(line).replaceAll(REPLACEMENT_STRING);
	}
	
	private class TestTermParams {
		int freq;
		double prob;

		public TestTermParams(int freq, double prob) {
			this.freq = freq;
			this.prob = prob;
		}
	}	
//	private void getTestCorpus(String testCorpus) throws IOException {
//
//		while ((name = termsEnum.next()) != null) {
//			String term = name.utf8ToString();
//			int freq = (int) termsEnum.totalTermFreq();
//			TestTermParams ttp = new TestTermParams(freq, prob);
//			testCorpusMap.put(term, ttp);
//			testCorpusMapBaseline.put(term, ttp);
//		}
//	}
	private Map<String, Integer> getAreaTerms(IndexReader ireader, int docID) throws IOException {
		Map<String, Integer> areaCorpusMaptemp = new HashMap<String, Integer>();
		
		Terms area_terms = ireader.getTermVector(docID, "areaCorpus");
		TermsEnum termsEnum = null;
		termsEnum = area_terms.iterator(termsEnum);
		BytesRef name = null;
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
//			System.out.println("getAreaTerms putting term:" + term + " freq:"+freq);
			areaCorpusMaptemp.put(term, freq);
		}
		System.out.println("getAreaTerms: "+areaCorpusMaptemp);
		return areaCorpusMaptemp;
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
//			System.out.println(yearCorpus);
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

	public static DirectoryReader readerHandler(File path) throws IOException{
//    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	return ireader;
	}
	
    public static void readResearcherIndex() throws IOException{
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	System.out.println("The number of documents is " + ireader.maxDoc());
    	for (int i = 0; i < ireader.maxDoc(); i++){
    		Set<String> terms = new HashSet<>();
    		Document hitDoc = ireader.document(i);
    		System.out.print("terms for doc# " + hitDoc.get("id"));
    		Terms vector = ireader.getTermVector(i, "researcherCorpus");	
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
    	ireader.close();
    }
	
    public static void readAreaIndex() throws IOException{
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	System.out.println("The number of documents is " + ireader.maxDoc());
    	for (int i = 0; i < ireader.maxDoc(); i++){
    		Set<String> terms = new HashSet<>();
    		Document hitDoc = ireader.document(i);
    		System.out.print("terms for doc# " + hitDoc.get("areaName"));
    		Terms vector = ireader.getTermVector(i, "areaCorpus");	
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
			TestTermParams ttp = new TestTermParams(freq, prob);
			testCorpusMap.put(term, ttp);
			testCorpusMapBaseline.put(term, ttp);
		}
	}

	private void getAreaTerms(String area_name,
			Map<String, Integer> areaCorpusMaptemp) throws IOException {
		File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	IndexSearcher isearcher = new IndexSearcher(ireader);
		Term t = new Term("name", area_name);
		Query query = new TermQuery(t);

		TopDocs topdocs = isearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		int doc_no = scoreDocs[0].doc;
		Terms area_terms = ireader.getTermVector(doc_no, "corpus");
		TermsEnum termsEnum = null;
		termsEnum = area_terms.iterator(termsEnum);
		BytesRef name = null;
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			areaCorpusMaptemp.put(term, freq);
		}
	}
    
    
    public static void readTestIndex() throws IOException{
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	System.out.println("The number of documents is " + ireader.maxDoc());
//    	System.exit(1);
    	for (int i = 0; i < ireader.maxDoc(); i++){
    		Set<String> terms = new HashSet<>();
    		Document hitDoc = ireader.document(i);
    		System.out.print("terms for doc# " + hitDoc.get("id"));
    		Terms vector = ireader.getTermVector(i, "yearCorpus");	
    	    if (vector != null){
    			System.out.println(" = " + vector.size());
    			TermsEnum termsEnum = null;
  				termsEnum = vector.iterator(termsEnum);
   				BytesRef name = null;
   				while ((name = termsEnum.next()) != null) {
   					String term = name.utf8ToString();
   					terms.add(term);
   					System.out.print(term + "\t");
   				}
   	       } else {
   	    	   System.out.println("vector is null");
   	       }
    	       System.out.println("*****************************");
    	   }
    }

	private int calculateTotFreq(Map<String, Integer> areaCorpusMap) {
		int totalFreq = 0;

		for (Object term : areaCorpusMap.keySet()) {
			totalFreq += (int) areaCorpusMap.get(term);
		}
		return totalFreq;
	}
	
	public static void main(String argv[]) throws InterruptedException {
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
    	File respath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
		ComputeEquationKeywords_21Dec2013_832 mainClass = new ComputeEquationKeywords_21Dec2013_832();
		BufferedReader resAreaCorpusBuffRead = mainClass.openResearcherYearCorpus();
		BufferedReader resYearWiseAreaCorpusBuffRead = mainClass.openResearcherYearWiseAreaCorpus();
		
		JSONParser jsonParser = new JSONParser();
		String resYearCorpus = null;
		String resYearWiseAreaCorpus = null;

		double alpha = 0.85;
		if (resAreaCorpusBuffRead == null){
			System.out.print("Could not open the file");
			System.exit(1);
		}
		
		try{
			int resCnt = 1;
			while (((resYearCorpus = resAreaCorpusBuffRead.readLine()) != null) && ((resYearWiseAreaCorpus = resYearWiseAreaCorpusBuffRead.readLine()) != null)){
				Directory directory = FSDirectory.open(path);
				Directory resdirectory = FSDirectory.open(respath);
		        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
		        IndexWriter iwriter;
		        IndexReader ireader;
		        IndexReader iResreader;
		        
				Object resObj = jsonParser.parse(resYearCorpus);
				JSONObject resJSONObj = (JSONObject)resObj;
				QueryParser queryParserRes = new QueryParser(Version.LUCENE_43, "areaCorpus", analyzer);
				Query queryRes;
				Object resYAObj = jsonParser.parse(resYearWiseAreaCorpus);
				JSONObject resYAJSONObj = (JSONObject)resYAObj;
				HashMap yearAreaCorpusMap = (HashMap)resYAJSONObj.get("year_area_corpus");
				String researcherId = (String)resJSONObj.get("_id");
				String researcherName = (String)resJSONObj.get("name");
				HashMap yearCorpusMap = (HashMap)resJSONObj.get("year_corpus");
				String corpus = null;
				String yearT = "2011";
				String yearTPlus = "2012";
				System.out.println("Starting process for researcher:"+researcherName);
				
				//Indexing the test corpus
				mainClass.indexTestCorpusYears(yearTPlus);
				//Indexing the researcher corpus
				
				if (yearAreaCorpusMap.containsKey(yearT) && yearAreaCorpusMap.containsKey(yearTPlus)){
					//Processing researcher corpus
					if (yearCorpusMap.containsKey(yearT)){
						corpus = (String)yearCorpusMap.get(yearT);
						System.out.println("Researcher Corpus:"+corpus);
						queryRes = queryParserRes.parse(corpus);
//						mainClass.indexResearcherCorpus(researcherId, corpus);
//						readResearcherIndex();
						File researcherpath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusIndex");
//						iResreader = readerHandler(researcherpath);
					} else{
						System.out.println("No corpus for the researcher for year " + yearT);
						continue;
					}
					HashMap yearAreas = (HashMap)yearAreaCorpusMap.get(yearT);
					for(Object areaT: yearAreas.keySet()){
						//Indexing the areaT
						iwriter = new IndexWriter(directory, config);
						Document docT = new Document();
						String areaStrT = areaT.toString();
						String areaCorpusT = (String)yearAreas.get(areaStrT);
						docT.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
						docT.add(new Field("researcherName", researcherName, Field.Store.YES, Field.Index.NOT_ANALYZED));
						docT.add(new Field("year", yearT, Field.Store.YES, Field.Index.NOT_ANALYZED));
						docT.add(new Field("areaName", areaStrT, Field.Store.YES, Field.Index.NOT_ANALYZED));
						Field fieldT = new Field("areaCorpus", areaCorpusT, TYPE_STORED);
//						System.out.println("Indexing the document:"+areaCorpusT);
						docT.add(fieldT);
						iwriter.addDocument(docT);
						iwriter.close();
						System.out.println("Reading index after first set");
						readAreaIndex();
						System.out.println("**************************");
						
						ireader = DirectoryReader.open(directory);
						int docID1 = ireader.maxDoc() - 1;
						System.out.println("docID1 = " + docID1);
						Document hitDoc = ireader.document(docID1);
						IndexSearcher iSearcher = new IndexSearcher(ireader);
						//Temp code
						queryParserRes = new QueryParser(Version.LUCENE_43, "areaCorpus", analyzer);
						queryRes = queryParserRes.parse(areaCorpusT);
						System.out.println("queryRes = " + queryRes);
						mainClass.indexResearcherCorpus(researcherId, areaCorpusT);
						iResreader = DirectoryReader.open(resdirectory);
						double resScore = mainClass.cosineDocumentSimilarity(0, 0, iResreader, ireader, "researcherCorpus", "areaCorpus");
						System.out.println("resScore = " + resScore);
						System.exit(1);
						//Temp code
						TopDocs totalHits = iSearcher.search(queryRes, 10);
						ScoreDoc[] hits_Kt = totalHits.scoreDocs;
						System.out.println("Total # docs = " + hits_Kt.length);
						Thread.sleep(1000);
						double firstProb = hits_Kt[0].score;
						System.out.println("firstProb = "+firstProb);
						System.exit(1);
						int firstDocID = hits_Kt[0].doc;
						Document tempDoc = ireader.document(firstDocID);
						iwriter = new IndexWriter(directory, config);
						HashMap yearAreasTPlus = (HashMap)yearAreaCorpusMap.get(yearTPlus);
//						System.out.println("#Areas in year "+yearTPlus + " = " + yearAreasTPlus.size());
						//Indexing the areaTPlus
						for(Object areaTPlus: yearAreasTPlus.keySet()){
							Document docTPlus = new Document();
							String areaStrTPlus = areaTPlus.toString();
							String areaCorpusTplus = (String)yearAreasTPlus.get(areaStrTPlus);
							docTPlus.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
							docTPlus.add(new Field("researcherName", researcherName, Field.Store.YES, Field.Index.NOT_ANALYZED));
							docTPlus.add(new Field("year", yearTPlus, Field.Store.YES, Field.Index.NOT_ANALYZED));
							docTPlus.add(new Field("areaName", areaStrTPlus, Field.Store.YES, Field.Index.NOT_ANALYZED));
							Field fieldTPlus = new Field("areaCorpus", areaCorpusTplus, TYPE_STORED);
							docTPlus.add(fieldTPlus);
							iwriter.addDocument(docTPlus);
						}
						iwriter.close();
//						System.out.println("Reading index after second set");
//						readAreaIndex();
						ireader = DirectoryReader.open(directory);
						System.out.println("Comparing " + hitDoc.get("year") + " area:" + hitDoc.get("areaName"));
						for (int j = 1; j < ireader.maxDoc(); j++) {
							int docID2 = j;
							Document hitDoc2 = ireader.document(docID2);
							System.out.println(" with " + hitDoc2.get("year") + " area:" + hitDoc2.get("areaName"));
							double simScore = mainClass.cosineDocumentSimilarity(docID1, docID2, ireader, "areaCorpus");
							System.out.println(simScore);
							double secondProb = simScore;
							System.out.println("secondProb = "+secondProb);
							mainClass.areaCorpusMap = mainClass.getAreaTerms(ireader,	docID2);
							if (mainClass.areaCorpusMap != null){
								for (String term : mainClass.areaCorpusMap.keySet()){
//									System.out.println(term+":"+areaCorpusMaptemp.get(term));
								}
							}
							System.out.println("areaCorpusMap = " + mainClass.areaCorpusMap);
							int totFreq = mainClass.calculateTotFreq(mainClass.areaCorpusMap);
							mainClass.getTestCorpusTerms(yearTPlus);
							for (Object term : mainClass.testCorpusMap.keySet()) {
								TestTermParams ttp = mainClass.testCorpusMap.get(term);
								int word_freq = ttp.freq;
								double thirdProb = ttp.prob;
								int area_term_freq = 0;
								if (mainClass.areaCorpusMap.containsKey(term)) {
									area_term_freq = mainClass.areaCorpusMap.get(term);
								}

								if (area_term_freq != 0) {
									thirdProb = alpha * area_term_freq / totFreq;
								} else {
									thirdProb = (1 - alpha) * 1 / totFreq;
								}
								thirdProb *= word_freq;
								double cummProb = firstProb * secondProb * thirdProb;
								ttp.prob += cummProb;
								System.out.println("thirdProb = "+thirdProb);
							}
							
						}
//						System.exit(1);
						iwriter = new IndexWriter(directory, config);
						iwriter.deleteAll();
						iwriter.close();
						System.exit(1);
					}
		    	} 
				resCnt++;
				System.out.println("Completed process for researcher:"+researcherName);
//				System.exit(1);
			}
		}catch(IOException e){
			e.printStackTrace();
		}catch (ParseException e) {
			e.printStackTrace();
		}catch (org.apache.lucene.queryparser.classic.ParseException e) {
			e.printStackTrace();
		}
	}
}