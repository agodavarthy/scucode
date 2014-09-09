
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ResIntAreaYearResearcherIndexCreation {
    public static final FieldType TYPE_STORED = new FieldType();

    static {
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
    }
    
    private static void createAreaIndex() throws IOException, ParseException{
//    	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
    	Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntAreaIndex");
    	Directory directory = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);
        
        JSONParser parser = new JSONParser();
        Document doc;
        
    	try {
//    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/area_corpus_json.json"));
//    		trying with smaller set.
    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/area_corpus_json.json"));
    		String line;
    		while((line = br.readLine()) != null){
    			Object obj = parser.parse(line);
    			JSONObject jsonObject = (JSONObject) obj;
    			String area_name = (String)jsonObject.get("name");
    			System.out.println("area_name = " + area_name);
    			doc = new Document();
    			TYPE_STORED.setTokenized(false);
//    			Field field = new Field("name", area_name, TYPE_STORED);
//    			doc.add(field);
    			doc.add(new Field("name", area_name, Field.Store.YES, Field.Index.NOT_ANALYZED));
//    			doc.add(new Field("corpus", (String)jsonObject.get("corpus"), Field.Store.YES, Field.Index.ANALYZED));
    			TYPE_STORED.setTokenized(true);
    			Field field = new Field("corpus", (String)jsonObject.get("corpus"), TYPE_STORED);
    			doc.add(field);
    			iwriter.addDocument(doc);
    		}
    		iwriter.close();
    		br.close();
    		System.out.println("Area Indexing Done");
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public static void readAreaIndex() throws IOException{
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntAreaIndex");
    		Directory directory = FSDirectory.open(path);
    		DirectoryReader ireader = DirectoryReader.open(directory);
    		IndexSearcher isearcher = new IndexSearcher(ireader);
    		
    		for (int i = 0; i < ireader.maxDoc(); i++){
    			Set<String> terms = new HashSet<>();
    			Map<String, Integer> frequencies = new HashMap<>();
    			Document hitDoc = ireader.document(i);
    			System.out.println("terms for doc# " + hitDoc.get("name"));
    			Terms vector = ireader.getTermVector(i, "corpus");	
    	        if (vector != null){
    					System.out.println("#terms = " + vector.size());
    					TermsEnum termsEnum = null;
    					termsEnum = vector.iterator(termsEnum);
    					BytesRef name = null;
    					while ((name = termsEnum.next()) != null) {
    						String term = name.utf8ToString();
    						int freq = (int) termsEnum.totalTermFreq();
//    						frequencies.put(term, freq);
    						terms.add(term);
    						System.out.print(term + "\t");
    					}
    	        } else {
    	        	System.out.println("vector is null");
    	        }
    	        System.out.println("*****************************");
    	    }
	
    }

    private static void createResearcherIndex() throws IOException, ParseException{
//    	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
    	Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntResearcherIndex");
    	Directory directory = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);
        
        JSONParser parser = new JSONParser();
        Document doc;
        
    	try {
//    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/area_corpus_json.json"));
//    		trying with smaller set.
    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/res_qry_by_year.json"));
    		String line;
    		while((line = br.readLine()) != null){
    			Object obj = parser.parse(line);
    			JSONObject jsonObject = (JSONObject) obj;
    			String researcher_name = (String)jsonObject.get("_id");
    			System.out.println("researcher_name = " + researcher_name);
    			doc = new Document();
//    			TYPE_STORED.setTokenized(false);
//    			Field field = new Field("name", area_name, TYPE_STORED);
//    			doc.add(field);
    			doc.add(new Field("name", researcher_name, Field.Store.YES, Field.Index.NOT_ANALYZED));
//    			doc.add(new Field("corpus", (String)jsonObject.get("corpus"), Field.Store.YES, Field.Index.ANALYZED));
//    			TYPE_STORED.setTokenized(true);
    			Field field = new Field("year_qry_dic", (String)jsonObject.get("year_qry_dic"), TYPE_STORED);
    			doc.add(field);
    			iwriter.addDocument(doc);
    		}
    		iwriter.close();
    		br.close();
    		System.out.println("Researcher Indexing Done");
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public static void readResearcherIndex() throws IOException{
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntResearcherIndex");
    		Directory directory = FSDirectory.open(path);
    		DirectoryReader ireader = DirectoryReader.open(directory);
    		IndexSearcher isearcher = new IndexSearcher(ireader);
    		
    		for (int i = 0; i < ireader.maxDoc(); i++){
    			Set<String> terms = new HashSet<>();
    			Map<String, Integer> frequencies = new HashMap<>();
    			Document hitDoc = ireader.document(i);
    			System.out.println("terms for doc# " + hitDoc.get("_id"));
    			Terms vector = ireader.getTermVector(i, "year_qry_dic");	
    	        if (vector != null){
    					System.out.println("#terms = " + vector.size());
    					TermsEnum termsEnum = null;
    					termsEnum = vector.iterator(termsEnum);
    					BytesRef name = null;
    					while ((name = termsEnum.next()) != null) {
    						String term = name.utf8ToString();
    						int freq = (int) termsEnum.totalTermFreq();//    						frequencies.put(term, freq);
    						terms.add(term);
    						System.out.print(term + "\t");
    					}
    	        } else {
    	        	System.out.println("vector is null");
    	        }
    	        System.out.println("*****************************");
    	    }
	
    }
    
    
    
    private static void createYearIndex() throws IOException, ParseException{
    	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
//    	Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntYearIndex");
    	Directory directory = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);
        
        JSONParser parser = new JSONParser();
        Document doc;
        
    	try {
    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/year_corpus.json"));
    		String line;
    		while((line = br.readLine()) != null){
    			Object obj = parser.parse(line);
    			JSONObject jsonObject = (JSONObject) obj;
    			String year_name = (String)jsonObject.get("year").toString();
    			doc = new Document();
    			Field field = new Field("year", year_name, TYPE_STORED);
    			doc.add(field);
//    			doc.add(new Field("year", year_name, Field.Store.YES, Field.Index.NO));
    			field = new Field("corpus", (String)jsonObject.get("corpus"), TYPE_STORED);
    			doc.add(field);
    			iwriter.addDocument(doc);
    		}
    		iwriter.close();
    		br.close();
    		System.out.println("Year Indexing Done");
//    			System.out.println("terms for doc# " + vector.);
//    			vector = ireader.getTermVector(i, "corpus");
//    	        if (vector != null){
//    					System.out.println("#terms = " + vector.size());
//    					TermsEnum termsEnum = null;
//    					termsEnum = vector.iterator(termsEnum);
//    					BytesRef name = null;
//    					while ((name = termsEnum.next()) != null) {
//    						String term = name.utf8ToString();
//    						int freq = (int) termsEnum.totalTermFreq();
//    						frequencies.put(term, freq);
//    						terms.add(term);
//    						 System.out.println("text = " + term);
//    					}
//    	        } else {
//    	        	System.out.println("vector is null");
//    	        }
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public static void readYearIndex() throws IOException{
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntYearIndex");
    	Directory directory = FSDirectory.open(path);
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		for (int i = 0; i < ireader.maxDoc(); i++){
			Set<String> terms = new HashSet<>();
			Map<String, Integer> frequencies = new HashMap<>();
			Terms vector = ireader.getTermVector(i, "corpus");
	        if (vector != null){
					System.out.println("#terms = " + vector.size());
					TermsEnum termsEnum = null;
					termsEnum = vector.iterator(termsEnum);
					BytesRef name = null;
					while ((name = termsEnum.next()) != null) {
						String term = name.utf8ToString();
						int freq = (int) termsEnum.totalTermFreq();
						frequencies.put(term, freq);
						terms.add(term);
//						 System.out.println("text = " + term);
					}
	        } else {
	        	System.out.println("vector is null");
	        }

			System.out.println("terms for doc# " + i);
    	    }
	
    }

    public static void readALLAreasIndex() throws IOException{
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsBioinfoALLAreasIndex");
    	Directory directory = FSDirectory.open(path);
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		System.out.println("Max #Docs = "+ ireader.maxDoc()+"\n");
//		System.exit(1);
		for (int i = 0; i < ireader.maxDoc(); i++){
			Set<String> terms = new HashSet<>();
//			Map<String, Integer> frequencies = new HashMap<>();
//			Terms vector = ireader.getTermVector(i, "areaName");
			Document d = ireader.document(i);
			System.out.println(d.getValues("areaName")[0]);
    	    }

//		for (int i = 0; i < ireader.maxDoc(); i++){
//			Set<String> terms = new HashSet<>();
//			Map<String, Integer> frequencies = new HashMap<>();
//			Terms vector = ireader.document(i);
//			Terms vector = ireader.getTermVector(i, "areaName");
//			System.out.println(vector);
//	        if (vector != null){
//					System.out.println("#terms = " + vector.size());
//					TermsEnum termsEnum = null;
//					termsEnum = vector.iterator(termsEnum);
//					BytesRef name = null;
//					while ((name = termsEnum.next()) != null) {
//						String term = name.utf8ToString();
//						int freq = (int) termsEnum.totalTermFreq();
//						frequencies.put(term, freq);
//						terms.add(term);
////						 System.out.println("text = " + term);
//					}
//	        } else {
//	        	System.out.println("vector is null");
//	        }
//
//			System.out.println("terms for doc# " + i);
//    	    }
	
    }

    
	public static void main(String args[]) throws IOException, ParseException{
//		createAreaIndex();
//		createYearIndex();
//		createResearcherIndex();
//		readAreaIndex();
//		readYearIndex();
//		readResearcherIndex();
		readALLAreasIndex();
	}
//	final Set<String> stopwd}
}