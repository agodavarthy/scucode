package OldCode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.math3.linear.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ResearcherAreaScoring1 {
	
	private static final String CONTENT = "corpus";
    private static Set<String> terms = new HashSet<>();
    private static RealVector v1;
    private static RealVector v2;
    private Analyzer analyzer;
    private Directory directory;
    private IndexReader ireader;
    private IndexSearcher isearcher;
    private HashMap doc_sim_dic;
    
    public void Init() {
    	//Stopword must be implemented better
//    	Collection<String> stopwords = new HashSet<String>();
//    	stopwords.add("a");
//    	stopwords.add("and");
//    	stopwords.add("the");
//
//    	CharArraySet stopwordset = new CharArraySet(Version.LUCENE_43,
//    			stopwords, true);
    	doc_sim_dic = new HashMap<>();
    	analyzer = new StandardAnalyzer(Version.LUCENE_43);
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
    
    private double cosineDocumentSimilarity(int d1, int d2, IndexReader reader) throws IOException {
      Map<String, Integer> f1 = getTermFrequencies(reader, d1);
      Map<String, Integer> f2 = getTermFrequencies(reader, d2);
      v1 = toRealVector(f1);
      v2 = toRealVector(f2);
      return getCosineSimilarity();
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
    private void openIndex(){
    	try{
		File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntindex");
		directory = FSDirectory.open(path);
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
    	} catch(IOException e){
    		e.printStackTrace();
    	}
    }
    private BufferedReader openQueryFile(){
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/res_qry_by_year.json"));
			return br;
		} catch(IOException e){
			e.printStackTrace();
			return br;
		}
    }
    
    private String extractQuery(HashMap query_dic){
    	StringBuffer full_query_str = new StringBuffer();
    	String query_str = new String();
//    	for (int year = 2002; year <=2012; year++){
//    		query_str = (String) query_dic.get(""+year);
//    		full_query_str.append(" ");
//    		full_query_str.append(query_str);
//    	}
//    	return full_query_str.toString();
    	if (query_dic.containsKey("2010"))
    		return (String)query_dic.get("2010");
    	else
    	return null;
    }
    
    private void computeDocumentSimilarity() throws InterruptedException{
    	try{
    		HashMap temp1;
			for (int i = 0; i < ireader.maxDoc(); i++){
				int docID1 = i;
				Document hitDoc1 = ireader.document(i);
				
				if (!doc_sim_dic.containsKey(docID1)){
					temp1 = new HashMap<>();
					doc_sim_dic.put(docID1, temp1);
				} else{
					temp1 = (HashMap)doc_sim_dic.get(docID1);
				}
//				double simScore_new = 0.0;
//				double simScore_old = 0.0;
				String matched_area = new String();
				HashMap temp2;
				for (int j = i + 1; j < ireader.maxDoc(); j++) {
					int docID2 = j;
					Document hitDoc2 = ireader.document(j);
					if (!doc_sim_dic.containsKey(docID2)){
						temp2 = new HashMap<>();
						doc_sim_dic.put(docID2, temp2);
					} else{
						temp2 = (HashMap)doc_sim_dic.get(docID2); 
					}
					if (docID1 != docID2) {
						double simScore = cosineDocumentSimilarity(docID1, docID2, ireader);
						temp1.put(docID2, simScore);
						temp2.put(docID1, simScore);
//						if (simScore_new >= simScore_old){
//							simScore_old = simScore_new;
//							matched_area = hitDoc2.get("name");
//						}
					}
				}
//				System.out.println("Done with " + hitDoc1.get("name"));
			}
			
			String area_name = new String();
			int count = 0;
			for(Object area1 : doc_sim_dic.keySet()){
				double highest_prev = 0.0;
				HashMap val = (HashMap) doc_sim_dic.get(area1);
				int area1_id = (int)area1;
				Document doc1 = ireader.document(area1_id);
				System.out.println("Looking for matches for "+ doc1.get("name"));
				if (count > 5 ) System.exit(0);
				count++;
				System.out.println("# keys for " + doc1.get("name") + " = " + val.size());
				for(Object area2 : val.keySet()){
					double current_score =(double)val.get(area2);
					System.out.println("prev score:" + highest_prev + " area:" + area_name);
					if ( current_score > highest_prev){
						highest_prev = (double)val.get(area2);
						int area2_id = (int)area2;
						Document doc2 = ireader.document(area2_id);
						area_name = (String)doc2.get("name");
//						System.out.println("Found a better matched area "+ doc2.get("name") + " with score = " + current_score);
					}
				}
				System.out.println(doc1.get("name") + ":" + area_name);
			}
			
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    private static String escapeQuery(String line){
		final String LUCENE_ESCAPE_CHARS = "[\\\\+\\-\\!\\(\\)\\:\\^\\[\\]\\{\\}\\~\\*\\?\\/\\%]";
		final Pattern LUCENE_PATTERN = Pattern.compile(LUCENE_ESCAPE_CHARS);
		final Pattern ALPHANUM = Pattern.compile("[a-zA-Z 0-9]");
		final String REPLACEMENT_STRING = "\\\\$0";
		return LUCENE_PATTERN.matcher(line).replaceAll(REPLACEMENT_STRING);
    }
    public static void main(String args[]) throws IOException, ParseException {
		
		ResearcherAreaScoring1 resAreaScore = new ResearcherAreaScoring1();
		resAreaScore.Init();
		resAreaScore.openIndex();
		resAreaScore.openQueryFile();

			int count = 1;
			BufferedReader br = resAreaScore.openQueryFile();
			if (br == null){
				System.out.print("Could not open the file");
				System.exit(1);
			}
			JSONParser json_parser = new JSONParser();
	    	String line = null;
	    	try{
	    		Document doc = resAreaScore.ireader.document(8);
	    		System.out.println("doc name = " +  doc.get("name"));
	    		Terms terms_doc8 = resAreaScore.ireader.getTermVector(8, "corpus");
	    		if (terms_doc8 == null){
	    			System.out.println("empty");
	    			System.exit(1);
	    		} else{
	    			System.out.println("not empty");
	    		}
	    		resAreaScore.computeDocumentSimilarity();
	    		System.exit(0);
			while ((line = br.readLine()) != null) {
				
				Object obj = json_parser.parse(line);
				JSONObject jsonObject = (JSONObject) obj;
				String researcher_id = (String) jsonObject.get("_id");
				String researcher_name = (String) jsonObject.get("name");
				System.out.println("**************" + researcher_name  +":" + researcher_id	+ " **********************");
				HashMap query_dic = (HashMap) jsonObject.get("year_qry_dic");
				String rawQueryStr = resAreaScore.extractQuery(query_dic);
				if (rawQueryStr != null){
				String escaped_query_Kt = escapeQuery(rawQueryStr);
				if (escaped_query_Kt == null){
					System.out.println(researcher_name + " does not have papers for year 2009");
					Thread.sleep(1000);
					continue;
				}
				QueryParser query_parser_Kt = new QueryParser(Version.LUCENE_43, "corpus", resAreaScore.analyzer);
				System.out.println("Fine till here??");
				Query query_Kt = query_parser_Kt.parse(escaped_query_Kt);
				ScoreDoc[] hits_Kt = resAreaScore.isearcher.search(query_Kt, null, 1000).scoreDocs;
				if (hits_Kt.length > 0){
					Document hitDoc = resAreaScore.isearcher.doc(hits_Kt[0].doc);
					System.out.println("Highest Scored Area = " + hitDoc.get("name") + " score : " + hits_Kt[0].score);
					Thread.sleep(1000);
					double cummKtProb = 0.0;
					double simScore_old = 0.0;
					double simScore_new = 0.0;
					String matched_area = new String();
					int docID1 = hits_Kt[0].doc;
					System.out.println("Total # docs indexed = "+ resAreaScore.ireader.maxDoc());
					Thread.sleep(1000);
					for (int i = 0; i < resAreaScore.ireader.maxDoc(); i++){
//						System.out.println("trying to acces docID "+ i);
						int docID2 = i;
						Document hitDoc2 = resAreaScore.ireader.document(i);
//						Document hitDoc2 = resAreaScore.isearcher.doc(hits_Kt[i].doc);
//						System.out.println("trying to acces docID "+ i + " : " + hitDoc2.get("name"));
						if (docID1 != docID2){
							simScore_new = resAreaScore.cosineDocumentSimilarity(docID1, docID2, resAreaScore.ireader);
							if (simScore_new >= simScore_old){
								simScore_old = simScore_new;
								matched_area = hitDoc2.get("name");
							}
							System.out.println((i+1) + "Score between " + hitDoc.get("name") + " and " + hitDoc2.get("name") + " is " + simScore_new);
						}
//						Document hitDoc2 = resAreaScore.isearcher.doc(hits_Kt[i].doc);
						
					}
					System.out.println(hitDoc.get("name") + " matches most with " + matched_area);
					break;
					//					for (int i = 0; i < hits_Kt.length; i++){
//						cummKtProb += hits_Kt[i].doc;
//					}
////					for (int i = 0; i < hits_Kt.length; i++) {
////						int docID1 = hits_Kt[i].doc;
//						int docID1 = hits_Kt[0].doc;
////						Document hitDoc1 = resAreaScore.isearcher.doc(hits_Kt[i].doc);
//						Document hitDoc1 = resAreaScore.isearcher.doc(hits_Kt[0].doc);
//						String area1_name = hitDoc1.get("name");
//						String area2_name = new String();
//						double simScore = 0.0;
//						for(int j = 0; j < hits_Kt.length; j++){
//							int docID2 = hits_Kt[j].doc;
//							simScore = resAreaScore.cosineDocumentSimilarity(docID1, docID2, resAreaScore.ireader);
//							Document hitDoc2 = resAreaScore.isearcher.doc(hits_Kt[j].doc);
//							area2_name = hitDoc2.get("name");
//						}
////						System.out.println("Similarity between " + area1_name + " & " + area2_name + " is : " + simScore);
////					}
					}	
				System.out.print("ok");
				}
//						System.out.println((i + 1) + ":" + hitDoc.get("name") + ":"	+ hits_Kt[i].score);
//					System.out.println("**************" + researcher_name  +":" + researcher_id	+ " **********************");
//						String query_str_Ktplus1 = (String)hitDoc.get("corpus");
//////						System.out.println("#$@#$@#$@#$%@^%#^$%^$%^$%^$%6");
//////						System.out.println(area_name + " #clauses = " + query_str_Ktplus1.length());
//////						System.out.println("#$@#$@#$@#$%@^%#^$%^$%^$%^$%6");
//////						Thread.sleep(1000);
//						String escaped_query_Ktplus1 = LUCENE_PATTERN.matcher(query_str_Ktplus1).replaceAll(REPLACEMENT_STRING);
//////						String non_alpha_num_src  = ALPHANUM.matcher(query_str_Ktplus1).replaceAll("");
//////						String non_alpha_num_dst  = ALPHANUM.matcher(escaped_query_Ktplus1).replaceAll("");
//////						System.out.println("src = " + non_alpha_num_src);
//////						System.out.println("dst = " + non_alpha_num_dst);
//////						System.out.println("Testing...1");
//						BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
//						QueryParser query_parser_Ktplus1 = new QueryParser(Version.LUCENE_43, "corpus", analyzer);
//////						System.out.println("Before THIS?????");
//////						System.out.println(escaped_query_Ktplus1);
//////						Thread.sleep(1000);
//////						System.out.println("OR THIS?????");
//						Query query_Ktplus1 = query_parser_Ktplus1.parse(escaped_query_Ktplus1);
////////						System.out.println("Testing...3");
//						ScoreDoc[] hits_Ktplus1 = isearcher.search(query_Ktplus1, null, 1000).scoreDocs;
//						System.out.println("compared with " + hits_Ktplus1.length + "# of items");
//						if (hits_Ktplus1.length > 1){
//							Document highestDoc_Ktplus1 = isearcher.doc(hits_Kt[1].doc);
//							System.out.println(area_name + " Matches MOST with " + highestDoc_Ktplus1.get("name") + ":" + hits_Ktplus1[1].score);
//						}
			
			}
	    	
			}catch(ParseException e){
				e.printStackTrace();
			} catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
				
			}
	    	
    }
}