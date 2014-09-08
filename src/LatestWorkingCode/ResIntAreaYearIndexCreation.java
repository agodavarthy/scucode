package LatestWorkingCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ResIntAreaYearIndexCreation {
    public static final FieldType TYPE_STORED = new FieldType();

    static {
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.setTokenized(true);
    }
    
    private static boolean createTestIndex(IndexWriter iwriter) throws IOException {
    	Document doc = new Document();
    	doc.add(new Field("name", "not index indexed", Field.Store.YES, Field.Index.NOT_ANALYZED));
		Field field = new Field("corpus", "adapt adapted", TYPE_STORED);
		doc.add(field);
		iwriter.addDocument(doc);
		iwriter.close();
		System.out.println("Read done");
		return true;
    }
    
    private static void createAreaIndex() throws IOException, ParseException{
//    	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
//    	final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
//    	Analyzer analyzer = new ClassicAnalyzer(Version.LUCENE_43);
//    	Analyzer analyzer = new ClassicAnalyzer(Version.LUCENE_43, STOP_WORDS_SET);
//    	Analyzer analyzer = new StopAnalyzer(Version.LUCENE_43);
    	Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
//    	Analyzer analyzer = new SnowballAnalyzer(Version.LUCENE_35,"English");
    	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntAreaIndex");
    	Directory directory = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);
        
        JSONParser parser = new JSONParser();
        
        if (false && createTestIndex(iwriter)) {
        	return;
        }
        
    	try {
//    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/area_corpus_json.json"));
//    		trying with smaller set.
    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/area_corpus_json.json"));
    		String line;
    		while((line = br.readLine()) != null){
    			Object obj = parser.parse(line);
//    			StringBuffer result = new StringBuffer();
    			JSONObject jsonObject = (JSONObject) obj;
    			String area_name = (String)jsonObject.get("name");
    			System.out.println("area_name = " + area_name);
    			Document doc = new Document();
//    			Field field = new Field("name", area_name, TYPE_STORED);
//    			doc.add(field);
    			doc.add(new Field("name", area_name, Field.Store.YES, Field.Index.NOT_ANALYZED));
//    			doc.add(new Field("corpus", (String)jsonObject.get("corpus"), Field.Store.YES, Field.Index.ANALYZED));
//    			String corpusLowerCase = jsonObject.get("corpus").toString().toLowerCase();	
    			//New Test Code
//    			StringReader tReader = new StringReader(corpusLowerCase);
//    			TokenStream tStream = analyzer.tokenStream("contents", tReader);
//    			CharTermAttribute term = tStream.addAttribute(CharTermAttribute.class);
//    			try {
//                    while (tStream.incrementToken()){
//                        result.append(term.buffer());
//                        result.append(" ");
//                    }
//                } catch (IOException ioe){
//                    System.out.println("Error: "+ioe.getMessage());
//                }    			
    			//New Test Code
//    			TokenStream ts = new PorterStemFilter(new LowerCaseTokenizer(reader));
    			Field field = new Field("corpus", (String)jsonObject.get("corpus"), TYPE_STORED);
//    			System.out.println(result);
//    			Field field = new Field("corpus", corpusLowerCase, TYPE_STORED);
//    			Field field = new Field("corpus", result.toString(), TYPE_STORED);
    			doc.add(field);
    			iwriter.addDocument(doc);
//    			break;
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
    	BufferedWriter bw1 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/areaIndex"));
    	Directory directory = FSDirectory.open(path);
    		DirectoryReader ireader = DirectoryReader.open(directory);
    		IndexSearcher isearcher = new IndexSearcher(ireader);
    		System.out.println("The number of documents is " + ireader.maxDoc());
    		for (int i = 0; i < ireader.maxDoc(); i++){
    			Set<String> terms = new HashSet<>();
    			Map<String, Integer> frequencies = new HashMap<>();
    			Document hitDoc = ireader.document(i);
    			System.out.println("terms for doc# " + hitDoc.get("name"));
    			bw1.write("terms for area:'" + hitDoc.get("name")+"'\n");
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
    						bw1.write(term + "\t");
    					}
    					bw1.write("\n");
    	        } else {
    	        	System.out.println("vector is null");
    	        }
    	        System.out.println("*****************************");
    	        
    	    }
    		bw1.close();
    }

    
    private static void createYearIndex() throws IOException, ParseException{
    	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
    	String[] stopWSet = {"a", "about", "basic"};
    	CharArraySet cas = new CharArraySet(Version.LUCENE_43, stopWSet.length, true);
//    	EnglishAnalyzer englishanalyzer = new EnglishAnalyzer(Version.LUCENE_43, cas);
    	EnglishAnalyzer englishanalyzer = new EnglishAnalyzer(Version.LUCENE_43);
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
    	BufferedWriter bw1 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/yearIndex"));
    	Directory directory = FSDirectory.open(path);
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		for (int i = 0; i < ireader.maxDoc(); i++){
			Set<String> terms = new HashSet<>();
			Map<String, Integer> frequencies = new HashMap<>();
			Document hitDoc = ireader.document(i);
			Terms vector = ireader.getTermVector(i, "corpus");
	        if (vector != null){
    			System.out.println("terms for doc# " + hitDoc.get("year"));
    			bw1.write("terms for year:'" + hitDoc.get("year")+"'\n");
					TermsEnum termsEnum = null;
					termsEnum = vector.iterator(termsEnum);
					BytesRef name = null;
					while ((name = termsEnum.next()) != null) {
						String term = name.utf8ToString();
						int freq = (int) termsEnum.totalTermFreq();
						frequencies.put(term, freq);
						terms.add(term);
						System.out.print(term + "\t");
						bw1.write(term + "\t");
					}
					bw1.write("\n");
					System.exit(1);
	        } else {
	        	System.out.println("vector is null");
	        }
	        System.out.println();
//			System.out.println("terms for doc# " + i);
    	    }
		bw1.close();
    }
    
	public static void main(String args[]) throws IOException, ParseException{
//		createAreaIndex();
		createYearIndex();
//		createResearcherIndex();
//		readAreaIndex();
		readYearIndex();
//		readResearcherIndex();
	}
//	final Set<String> stopwd}
}