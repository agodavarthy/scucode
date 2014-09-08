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


public class ComputeEquationKeywords_dec2213_1251 {
    public static final FieldType TYPE_STORED = new FieldType();
    private static RealVector v1;
    private static RealVector v2;
    private static final String CONTENT = "areaCorpus";
    private static Set<String> terms = new HashSet<>();
    static {
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.setTokenized(true);
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
	
    private Map<String, Integer> getTermFrequencies(IndexReader reader, int docId)
            throws IOException {
        Terms vector = reader.getTermVector(docId, CONTENT);
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
    private double getCosineSimilarity() {
    	double result = 0.0;
    	try{
    		result = (v1.dotProduct(v2)) / (v1.getNorm() * v2.getNorm());
    	}catch(Exception e){
    		e.printStackTrace();
    	}
        return result;
    }
    private double cosineDocumentSimilarity(int d1, int d2, IndexReader reader) throws IOException {
        Map<String, Integer> f1 = getTermFrequencies(reader, d1);
        Map<String, Integer> f2 = getTermFrequencies(reader, d2);
        v1 = toRealVector(f1);
        v2 = toRealVector(f2);
        return getCosineSimilarity();
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
			System.out.println("getAreaTerms putting term:" + term + " freq:"+freq);
			areaCorpusMaptemp.put(term, freq);
		}
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
			System.out.println(yearCorpus);
			doc.add(new Field("id", yearTPlus, Field.Store.YES, Field.Index.NOT_ANALYZED));
			Field field = new Field("yearCorpus", yearCorpus, TYPE_STORED);
			doc.add(field);
			iwriter.addDocument(doc);
		}
		iwriter.close();
	}
	
//    public static void readTestIndex() throws IOException{
//    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
//    	Directory directory = FSDirectory.open(path);
//    	DirectoryReader ireader = DirectoryReader.open(directory);
//    	System.out.println("The number of documents is " + ireader.maxDoc());
////    	System.exit(1);
//    	for (int i = 0; i < ireader.maxDoc(); i++){
//    		Set<String> terms = new HashSet<>();
//    		Document hitDoc = ireader.document(i);
//    		System.out.print("terms for doc# " + hitDoc.get("id"));
//    		Terms vector = ireader.getTermVector(i, "yearCorpus");	
//    	    if (vector != null){
//    			System.out.println(" = " + vector.size());
//    			TermsEnum termsEnum = null;
//  				termsEnum = vector.iterator(termsEnum);
//   				BytesRef name = null;
//   				while ((name = termsEnum.next()) != null) {
//   					String term = name.utf8ToString();
//   					terms.add(term);
//   					System.out.print(term + "\t");
//   				}
//   	       } else {
//   	    	   System.out.println("vector is null");
//   	       }
//    	       System.out.println("*****************************");
//    	   }
//    }
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

	
	public static void main(String argv[]) throws InterruptedException {
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
    	File testpath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsTestCorpusIndex");
		ComputeEquationKeywords_dec2213_1251 mainClass = new ComputeEquationKeywords_dec2213_1251();
		BufferedReader resAreaCorpusBuffRead = mainClass.openResearcherYearCorpus();
		BufferedReader resYearWiseAreaCorpusBuffRead = mainClass.openResearcherYearWiseAreaCorpus();
		
		JSONParser jsonParser = new JSONParser();
		String resYearCorpus = null;
		String resYearWiseAreaCorpus = null;

		if (resAreaCorpusBuffRead == null){
			System.out.print("Could not open the file");
			System.exit(1);
		}
		
		try{
			int resCnt = 1;

			while (((resYearCorpus = resAreaCorpusBuffRead.readLine()) != null) && ((resYearWiseAreaCorpus = resYearWiseAreaCorpusBuffRead.readLine()) != null)){
				Directory directory = FSDirectory.open(path);
				Directory testdirectory = FSDirectory.open(testpath);
		        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
		        IndexWriter iwriter;
		        IndexReader ireader;
		        IndexReader iTestreader;
		        
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
				readTestIndex();
				System.exit(1);
//				QueryParser queryParser = new QueryParser(Version.LUCENE_43, "id", analyzer);
//				Query query = queryParser.parse(yearTPlus);
//				
//				iTestreader = DirectoryReader.open(testdirectory);
//				IndexSearcher iTestSearcher = new IndexSearcher(iTestreader);
//				ScoreDoc[] testCorpustotalHits = iTestSearcher.search(query, 1).scoreDocs;
//				int testCorpusdocID = testCorpustotalHits[0].doc;
//				Document testCorpushitDoc = iTestreader.document(testCorpusdocID);
//				System.out.println("**************yearCorpus for "+yearTPlus+"**************");
//				System.out.println(testCorpushitDoc.get("yearCorpus"));
//				System.out.println("****************************");
//				System.exit(1);
//				Document testhitDoc = ireader.document("year");
//				
				if (yearAreaCorpusMap.containsKey(yearT) && yearAreaCorpusMap.containsKey(yearTPlus)){
					//Processing researcher corpus
					if (yearCorpusMap.containsKey(yearT)){
						corpus = (String)yearCorpusMap.get(yearT);
						System.out.println(corpus);
						queryRes = queryParserRes.parse(corpus);
					} else{
						System.out.println("No corpus for the researcher for year " + yearT);
						continue;
					}
//					if (testCorpusJSONObj.containsKey(yearTPlus)){
//						String testCorpusStr = (String)testCorpusJSONObj.get(yearTPlus);
//						Document docTest = new Document();
//						docTest.add(new Field("Year", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
////						System.out.println(testCorpusStr);
//					} else{
//						System.out.println("No Test corpus for year " + yearTPlus);
//						continue;
//					}

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
						docT.add(fieldT);
						iwriter.addDocument(docT);
						iwriter.close();
						ireader = DirectoryReader.open(directory);
						int docID1 = ireader.maxDoc() - 1;
						Document hitDoc = ireader.document(docID1);
						IndexSearcher iSearcher = new IndexSearcher(ireader);
						TopDocs totalHits = iSearcher.search(queryRes, 1);
						ScoreDoc[] hits_Kt = totalHits.scoreDocs;
						double firstProb = hits_Kt[0].score;
						int firstDocID = hits_Kt[0].doc;
						Document tempDoc = ireader.document(firstDocID);
						System.out.println("*******first doc***********");
						System.out.println(tempDoc.get("areaCorpus"));
						System.out.println("******************");
						System.exit(1);
						iwriter = new IndexWriter(directory, config);
						HashMap yearAreasTPlus = (HashMap)yearAreaCorpusMap.get(yearTPlus);
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
						ireader = DirectoryReader.open(directory);
						System.out.println("Comparing " + hitDoc.get("year") + " area:" + hitDoc.get("areaName"));
						for (int j = 1; j < ireader.maxDoc()-1; j++) {
							int docID2 = j;
							Map<String, Integer> areaCorpusMaptemp = null;
							Document hitDoc2 = ireader.document(docID2);
							System.out.println(" with " + hitDoc.get("year") + " area:" + hitDoc.get("areaName"));
							double simScore = mainClass.cosineDocumentSimilarity(docID1, docID2, ireader);
							System.out.println(simScore);
							double secondProb = simScore;
							areaCorpusMaptemp = mainClass.getAreaTerms(ireader,	docID2);
							if (areaCorpusMaptemp != null){
								for (String term : areaCorpusMaptemp.keySet()){
									System.out.println(term+":"+areaCorpusMaptemp.get(term));
								}
							}
						}
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