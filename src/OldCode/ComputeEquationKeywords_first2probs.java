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
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ComputeEquationKeywords_first2probs {
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
    
	public static void main(String argv[]) throws InterruptedException {
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
		
		ComputeEquationKeywords_first2probs mainClass = new ComputeEquationKeywords_first2probs();
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
		        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
		        IndexWriter iwriter;
		        IndexReader ireader;
		        
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
				if (yearCorpusMap.containsKey(yearT)){
					corpus = (String)yearCorpusMap.get(yearT);
					queryRes = queryParserRes.parse(corpus);
				} else{
					System.out.println("No corpus for the researcher for year " + yearT);
					continue;
				}
				if (yearAreaCorpusMap.containsKey(yearT) && yearAreaCorpusMap.containsKey(yearTPlus)){
					HashMap yearAreas = (HashMap)yearAreaCorpusMap.get(yearT);
					for(Object areaT: yearAreas.keySet()){
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
						
						iwriter = new IndexWriter(directory, config);
						HashMap yearAreasTPlus = (HashMap)yearAreaCorpusMap.get(yearTPlus);
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
							hitDoc = ireader.document(docID2);
							System.out.println(" with " + hitDoc.get("year") + " area:" + hitDoc.get("areaName"));
							double simScore = mainClass.cosineDocumentSimilarity(docID1, docID2, ireader);
							System.out.println(simScore);
						}
						System.exit(1);
					}
		    	} 
				resCnt++;
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