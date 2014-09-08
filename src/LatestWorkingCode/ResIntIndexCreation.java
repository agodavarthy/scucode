package LatestWorkingCode;
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

public class ResIntIndexCreation {
    public static final FieldType TYPE_STORED = new FieldType();

    static {
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.freeze();
    }
    
    
	public static void main(String args[]) throws IOException, ParseException{
//	final Set<String> stopwords = new HashSet<String>();
//	stopwords.add("a");
//	stopwords.add("and");
//	stopwords.add("the");

//	CharArraySet stopwordset = new CharArraySet(Version.LUCENE_43, stopwords, true);
//	File stopwordFile = new File("/home/archana/SCU_projects/research_changes/java_interface/stopwrodlist");
	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
//	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43, stopwords);
//	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);

	
//    Store the index in memory:
//    Directory directory = new RAMDirectory();
//    To store an index on disk, use this instead:
//    Directory directory = FSDirectory.open("/tmp/testindex");
//	Document doc;
	File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntindex");
	Directory directory = FSDirectory.open(path);
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
    IndexWriter iwriter = new IndexWriter(directory, config);
    
    JSONParser parser = new JSONParser();
    Document doc;
    
	try {
		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/area_corpus_json.json"));
		String line;
		while((line = br.readLine()) != null){
			Object obj = parser.parse(line);
			JSONObject jsonObject = (JSONObject) obj;
			String area_name = (String)jsonObject.get("name");
			doc = new Document();
			doc.add(new Field("name", area_name, Field.Store.YES, Field.Index.NO));
			Field field = new Field("corpus", (String)jsonObject.get("corpus"), TYPE_STORED);
			doc.add(field);
			iwriter.addDocument(doc);
		}
		iwriter.close();
		br.close();
		System.out.println("Indexing Done");
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		for (int i = 0; i < ireader.maxDoc(); i++){
			Set<String> terms = new HashSet<>();
			Map<String, Integer> frequencies = new HashMap<>();
			System.out.println("terms for doc# " + i);
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
						// System.out.println("text = " + term);
					}
	        } else {
	        	System.out.println("vector is null");
	        }
	    }
		
	} catch (IOException e) {
		e.printStackTrace();
	} catch (Exception e) {
		e.printStackTrace();
	}
}
}