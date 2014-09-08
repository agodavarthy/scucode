package OldCode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class PredictAreaTPlusIndexALLAreas {
	private static Directory directory;
	private static IndexWriterConfig config;
	private static IndexWriter iwriter;
	public static final FieldType TYPE_STORED = new FieldType();
    static {
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.tokenized();
        TYPE_STORED.storeTermVectorPayloads();
        TYPE_STORED.storeTermVectors();
    }
	private static void indexALLAreasCorpus(String areaName, String areaCorpus) throws IOException{
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
	public DirectoryReader getReaderHandler(File path) throws IOException{
    	Directory directory = FSDirectory.open(path);
    	DirectoryReader ireader = DirectoryReader.open(directory);
    	return ireader;
	}
	public static void main(String args[]) throws IOException, ParseException{
		BufferedReader allAreaCorpusBuffRead;
		BufferedReader br = null;
		String keywordCorpus = null;
		File areaALLPath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsALLAreasIndex");
		JSONParser jsonParser;jsonParser = new JSONParser();
		allAreaCorpusBuffRead = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/keyword_corpus_arnet_IR.json"));
		while ((keywordCorpus = allAreaCorpusBuffRead.readLine()) != null){
			Object keyCorpObj = jsonParser.parse(keywordCorpus);
			JSONObject keyCorpJSONObj = (JSONObject)keyCorpObj;
			String areaName = (String)keyCorpJSONObj.get("_id");
			String areaCorpus = (String)keyCorpJSONObj.get("corpus");
			indexALLAreasCorpus(areaName, areaCorpus);
	    	Directory directory = FSDirectory.open(areaALLPath);
	    	DirectoryReader ireader = DirectoryReader.open(directory);
			IndexReader iALLAreasReader = ireader;
			System.out.println(iALLAreasReader.maxDoc());
		}
		allAreaCorpusBuffRead.close();
	}
}