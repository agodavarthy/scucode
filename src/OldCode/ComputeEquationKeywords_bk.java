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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//import LuceneResearchChanges.src.DocumentCosineSimilarity;


public class ComputeEquationKeywords_bk{
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
    
    
	public static void main(String argv[]) throws InterruptedException {
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsYearAreaIndex");
		
		ComputeEquationKeywords_bk mainClass = new ComputeEquationKeywords_bk();
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
		        IndexWriter iwriter = new IndexWriter(directory, config);
		        
				Object resObj = jsonParser.parse(resYearCorpus);
				JSONObject resJSONObj = (JSONObject)resObj;

				Object resYAObj = jsonParser.parse(resYearWiseAreaCorpus);
				JSONObject resYAJSONObj = (JSONObject)resYAObj;
				HashMap yearAreaCorpusMap = (HashMap)resYAJSONObj.get("year_area_corpus");
				String researcherId = (String)resJSONObj.get("_id");
				String researcherName = (String)resJSONObj.get("name");
				HashMap yearCorpusMap = (HashMap)resJSONObj.get("year_corpus");
				String corpus = null;
				String yearT = "2011";
				String yearTPlus = "2012";
//				System.out.println(researcherName + " Corpus for year " + yearT);
//				if (yearCorpusMap.containsKey(yearT)){
//					corpus = (String)yearCorpusMap.get(yearT);
//					System.out.println(corpus);
//				} else{
//					System.out.println("No corpus for the researcher for year " + yearT);
//				}
//				System.out.println("***************************");
				
//				System.out.println(researcherId + " Areas for year " + yearT);
				if (yearAreaCorpusMap.containsKey(yearT) && yearAreaCorpusMap.containsKey(yearTPlus)){
					System.out.println(researcherId+":"+researcherName+"has corpus for both years " + yearT + " and " + yearTPlus);
					Thread.sleep(1000);
					
					HashMap yearAreasTPlus = (HashMap)yearAreaCorpusMap.get(yearTPlus);
					System.out.println(yearAreasTPlus);
					int cnt = 1;
					System.out.println("#Areas in year " + yearTPlus + " " + yearAreasTPlus.size());
					for(Object area: yearAreasTPlus.keySet()){
						Document doc = new Document();
						System.out.println("cnt = " + cnt);
						cnt++;
						String areaStr = area.toString();
						String areaCorpus = (String)yearAreasTPlus.get(areaStr);
						System.out.println("AreaCorpus = " + areaCorpus);
						doc.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
						doc.add(new Field("researcherName", researcherName, Field.Store.YES, Field.Index.NOT_ANALYZED));
						doc.add(new Field("year", yearTPlus, Field.Store.YES, Field.Index.NOT_ANALYZED));
						doc.add(new Field("areaName", areaStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
						Field field = new Field("areaCorpus", areaCorpus, TYPE_STORED);
						doc.add(field);
						iwriter.addDocument(doc);
					}

					HashMap yearAreas = (HashMap)yearAreaCorpusMap.get(yearT);
					System.out.println(yearAreas);
//					cnt = 1;
//					System.out.println("#Areas in year " + yearT + " " + yearAreas.size());
//					for(Object area: yearAreas.keySet()){
//						Document doc = new Document();
//						System.out.println("cnt = " + cnt);
//						cnt++;
//						String areaStr = area.toString();
//						String areaCorpus = (String)yearAreas.get(areaStr);
//						System.out.println("AreaCorpus = " + areaCorpus);
//						doc.add(new Field("researcherId", researcherId, Field.Store.YES, Field.Index.NOT_ANALYZED));
//						doc.add(new Field("researcherName", researcherName, Field.Store.YES, Field.Index.NOT_ANALYZED));
//						doc.add(new Field("year", yearT, Field.Store.YES, Field.Index.NOT_ANALYZED));
//						doc.add(new Field("areaName", areaStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
//						Field field = new Field("areaCorpus", areaCorpus, TYPE_STORED);
//						doc.add(field);
//						iwriter.addDocument(doc);
//						IndexReader ireader = DirectoryReader.open(directory);
//						int docID1 = ireader.maxDoc() - 1;
//						Document hitDoc = ireader.document(docID1);
//						System.out.print("Comparing " + hitDoc.get("year") + " area:" + hitDoc.get("areaName"));
//						for (int j = 0; j < ireader.maxDoc()-1; j++) {
//							int docID2 = j;
//							hitDoc = ireader.document(docID2);
//							System.out.println(" with " + hitDoc.get("year") + " area:" + hitDoc.get("areaName"));
//							double simScore = mainClass.cosineDocumentSimilarity(docID1, docID2, ireader);
//							System.out.println(simScore);
//						}
//					}
//					System.out.println("***************************");				
//					System.out.println(researcherId + " Areas for year " + yearTPlus);
//					System.out.println("***************************");				
					iwriter.close();
					
		    	}
				resCnt++;
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}