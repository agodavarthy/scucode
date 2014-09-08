package OldCode;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class DocumentCosineSimilarity {

	private static final String CONTENT = "corpus";
    private static Set<String> terms = new HashSet<>();
    private static RealVector v1;
    private static RealVector v2;
    private Analyzer analyzer;
    private Directory directory;
    private IndexReader ireader;
    private IndexSearcher isearcher;
    private HashMap doc_sim_dic;
	
    private void openIndex(){
    	try{
		File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntAreaIndex");
		directory = FSDirectory.open(path);
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
    	} catch(IOException e){
    		e.printStackTrace();
    	}
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
    private double cosineDocumentSimilarity(int d1, int d2, IndexReader reader) throws IOException {
        Map<String, Integer> f1 = getTermFrequencies(reader, d1);
        Map<String, Integer> f2 = getTermFrequencies(reader, d2);
        v1 = toRealVector(f1);
        v2 = toRealVector(f2);
        return getCosineSimilarity();
    }
    
    private void computeDocumentSimilarity() throws InterruptedException{
    	try{
    		File file = new File("/home/archana/SCU_projects/research_changes/lucene/doc_similarity.txt");
    		if (!file.exists()) {
				file.createNewFile();
			}
    		FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = 0; i < ireader.maxDoc(); i++){
				int docID1 = i;
				Document hitDoc1 = ireader.document(i);
				String matched_area = new String();
				HashMap temp2;
				for (int j = i; j < ireader.maxDoc(); j++) {
					int docID2 = j;
					Document hitDoc2 = ireader.document(j);
					double simScore = cosineDocumentSimilarity(docID1, docID2, ireader);
					bw.write(docID1+" "+ docID2+ " " + simScore+"\n");
				}
			}
			bw.close();
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    public static void main(String args[]) throws IOException, ParseException, InterruptedException {
    	DocumentCosineSimilarity docSimClass = new DocumentCosineSimilarity();
    	docSimClass.openIndex();
    	docSimClass.computeDocumentSimilarity();
    }    
}
