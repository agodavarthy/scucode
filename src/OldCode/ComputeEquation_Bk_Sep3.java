package OldCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
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

public class ComputeEquation_Bk_Sep3 {
	
	private static final String CONTENT = "corpus";
    private static Set<String> terms = new HashSet<>();
    private static RealVector v1;
    private static RealVector v2;
    private Analyzer analyzer;
    private Directory areadirectory;
    private IndexReader areaireader;
    private IndexSearcher areaisearcher;
    private Directory yeardirectory;
    private IndexReader yearireader;
    private IndexSearcher yearisearcher;
    private Map<String, Integer> testCorpusMap;
    private Map<String, Integer> areaCorpusMap;
    

    private HashMap doc_sim_dic;
    
    public void Init() {
    	doc_sim_dic = new HashMap<>();
    	analyzer = new StandardAnalyzer(Version.LUCENE_43);
	}

    private void getDocSimDic(){
    	try{
    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/doc_similarity.txt"));
			String line = null;
			Set doc_pair = null;
			while ((line = br.readLine()) != null) {
				String[] words = line.split(" ", 3);
//				System.out.println(words[0] + ":" + words[1] + ":" + words[2]);
				Float score = new Float(words[2]);
				doc_pair = new HashSet();
				doc_pair.add((Object)words[0]);
				doc_pair.add((Object)words[1]);
				doc_sim_dic.put(doc_pair, score);
			}

//			for (Object doc_set: doc_sim_dic.keySet()){
//				Set doc_set1 = (Set)doc_set;
//				System.out.println(doc_set1);
//				int docno = 1;
//				if (doc_set1.contains("1")){
//				System.out.println(doc_set + ":" + doc_sim_dic.get(doc_set));
//				}
//			}
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }

    private void openAreaIndex(){
    	try{
		File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntAreaIndex");
		areadirectory = FSDirectory.open(path);
		areaireader = DirectoryReader.open(areadirectory);
		areaisearcher = new IndexSearcher(areaireader);
    	} catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    private void openYearIndex(){
    	try{
		File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntYearIndex");
		yeardirectory = FSDirectory.open(path);
		yearireader = DirectoryReader.open(yeardirectory);
		yearisearcher = new IndexSearcher(yearireader);
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
    	if (query_dic.containsKey("2010"))
    		return (String)query_dic.get("2010");
    	else
    	return null;
    }
    
    
    private void getTestCorpus(String year) throws IOException{
    	System.out.println("In getTestCorpus!!!!");
		Term t = new Term("year", year);
		Query query = new TermQuery(t);

		TopDocs topdocs = yearisearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		System.out.println("scoreDocs size = " + scoreDocs.length);
		int doc_no = scoreDocs[0].doc;

		Terms year_terms = yearireader.getTermVector(doc_no,
				"corpus");
		TermsEnum termsEnum = null;
		termsEnum = year_terms.iterator(termsEnum);
		BytesRef name = null;
		testCorpusMap = new HashMap<>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			testCorpusMap.put(term, freq);
//			System.out.println("text = " + term);
		}
    	
    }
    
    private void getAreaTerms(String area_name) throws IOException{
    	System.out.println("In getAreaCorpus!!!!");
//    	area_name = "systems";
    	System.out.println("getAreaTerms: area_name " + area_name);
    	
		Term t = new Term("name", area_name);
		Query query = new TermQuery(t);
		
		TopDocs topdocs = areaisearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		System.out.println("scoreDocs size = " + scoreDocs.length);
		int doc_no = scoreDocs[0].doc;
		System.out.println("Doc no = " + doc_no);
		Terms area_terms = areaireader.getTermVector(doc_no,"corpus");
		TermsEnum termsEnum = null;
		termsEnum = area_terms.iterator(termsEnum);
		BytesRef name = null;
		areaCorpusMap = new HashMap<>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			areaCorpusMap.put(term, freq);
//			System.out.println("text = " + term);
		}

    }
    
    private int calculateTotFreq(Map<String, Integer> areaCorpusMap){
    	int totalFreq = 0;
    	
		for(Object term: areaCorpusMap.keySet()){
			System.out.println(term + ":" + areaCorpusMap.get(term));
			totalFreq += (int)areaCorpusMap.get(term);
		}
    	return totalFreq;
    }
    
    private static String escapeQuery(String line){
		final String LUCENE_ESCAPE_CHARS = "[\\\\+\\-\\!\\(\\)\\:\\^\\[\\]\\{\\}\\~\\*\\?\\/\\%]";
		final Pattern LUCENE_PATTERN = Pattern.compile(LUCENE_ESCAPE_CHARS);
		final Pattern ALPHANUM = Pattern.compile("[a-zA-Z 0-9]");
		final String REPLACEMENT_STRING = "\\\\$0";
		return LUCENE_PATTERN.matcher(line).replaceAll(REPLACEMENT_STRING);
    }

    
    
    
//    private void computeBaseline(Map<String, Integer> new_frequencies) throws IOException{
//    	for(Object word : new_frequencies.keySet()){
//    		int word_freq = new_frequencies.get(word);
//    	}
//		Term t = new Term("year", "2011");
//		Query query = new TermQuery(t);
//
//		TopDocs topdocs = yearisearcher.search(query, 10);
//		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
//		int doc_no = scoreDocs[0].doc;
//
//		Terms year_terms = yearireader.getTermVector(doc_no, "corpus");
//		TermsEnum termsEnum = null;
//		termsEnum = year_terms.iterator(termsEnum);
//		BytesRef name = null;
//		Map<String, Integer> old_frequencies = new HashMap<>();
//		while ((name = termsEnum.next()) != null) {
//			String term = name.utf8ToString();
//			int freq = (int) termsEnum.totalTermFreq();
//			old_frequencies.put(term, freq);
//			System.out.println("text = " + term);
//		}
//		int totalFreq = 0;
//		for(Object term: old_frequencies.keySet()){
//			System.out.println(term+":"+old_frequencies.get(term));
//			totalFreq += old_frequencies.get(term);
//		}
//		System.out.println("Total Frequencies = "+ totalFreq);
//		double smoothingFactor = 0.1;
//		for(Object term: old_frequencies.keySet()){
//			if (new_frequencies.containsKey(term)){
//				double freq = new_frequencies.get(term);
//				double prob = freq / totalFreq;
//				double final_prob = prob * (1 - smoothingFactor);
//			} else{
//				double final_prob = smoothingFactor;
//			}
//				
//		}
//    }
    
    
	public static void main(String args[]) throws IOException, ParseException {

		ComputeEquation_Bk_Sep3 resAreaScore = new ComputeEquation_Bk_Sep3();
		resAreaScore.Init();
		resAreaScore.openAreaIndex();
		resAreaScore.openYearIndex();
		Term t = new Term("year", "2012");
		Query query = new TermQuery(t);
		double alpha = 0.85;
		resAreaScore.getTestCorpus("2012");
		resAreaScore.openQueryFile();
		resAreaScore.getDocSimDic();
		int count = 1;
		BufferedReader br = resAreaScore.openQueryFile();
		if (br == null) {
			System.out.print("Could not open the file");
			System.exit(1);
		}
		JSONParser json_parser = new JSONParser();
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				Object obj = json_parser.parse(line);
				JSONObject jsonObject = (JSONObject) obj;
				String researcher_id = (String) jsonObject.get("_id");
				String researcher_name = (String) jsonObject.get("name");
				System.out.println("**************" + researcher_name + ":"
						+ researcher_id + " **********************");
				HashMap query_dic = (HashMap) jsonObject.get("year_qry_dic");
				String rawQueryStr = resAreaScore.extractQuery(query_dic);
				if (rawQueryStr != null) {
					String escaped_query_Kt = escapeQuery(rawQueryStr);
					if (escaped_query_Kt == null) {
						System.out.println(researcher_name
								+ " does not have papers for year 2009");
						Thread.sleep(1000);
						continue;
					}
					QueryParser query_parser_Kt = new QueryParser(
							Version.LUCENE_43, "corpus", resAreaScore.analyzer);
					Query query_Kt = query_parser_Kt.parse(escaped_query_Kt);

					ScoreDoc[] hits_Kt = resAreaScore.areaisearcher.search(
							query_Kt, null, 1000).scoreDocs;
					double cummKtProb = 0.0;
					System.out.println("Total # docs indexed = "
							+ resAreaScore.areaireader.maxDoc());
					
					for (Object term : resAreaScore.testCorpusMap.keySet()) {
						System.out.println(term + ":"
								+ resAreaScore.testCorpusMap.get(term));
						int word_freq = resAreaScore.testCorpusMap.get(term);
						System.out.println("#hits = " + hits_Kt.length);
						for (int i = 0; i < hits_Kt.length; i++) {
							Document hitDoc = resAreaScore.areaisearcher
									.doc(hits_Kt[i].doc);
							cummKtProb += hits_Kt[i].score;
						}
						System.out.println(cummKtProb);
						for (int i = 0; i < hits_Kt.length; i++) {
							double currScore = hits_Kt[i].score / cummKtProb;
							Document hitDoc = resAreaScore.areaisearcher
									.doc(hits_Kt[i].doc);
							String doc_name = hitDoc.get("name");
							System.out.println(doc_name + "\t" + hits_Kt[i].doc
									+ "\t" + hits_Kt[i].score + "\t"
									+ currScore);
							
							double cummKtPlusProb = 0.0;
							System.out.println("doc_sim_dic count = " + resAreaScore.doc_sim_dic.size());
							int num_doc_sim = 0;
							for (Object doc_set : resAreaScore.doc_sim_dic
									.keySet()) {
								Set doc_set1 = (Set) doc_set;
								String docno = "" + hits_Kt[i].doc;
								if (doc_set1.contains(docno)) {
									float val = (float) resAreaScore.doc_sim_dic
											.get(doc_set);
									num_doc_sim += 1;
									cummKtPlusProb += val;
								}
							}
							System.out.println("num_doc_sim = " + num_doc_sim);
							System.out.println("out of loop");
							for (Object doc_set : resAreaScore.doc_sim_dic
									.keySet()) {
								
								Set doc_set1 = (Set) doc_set;
								String docno = "" + hits_Kt[i].doc;
								if (doc_set1.contains(docno)) {
									int totFreq = 0;
									System.out.println("234234234234234234 printing doc_set1 : " +
											 doc_set1 + " " + docno);
									 Thread.sleep(2000);
									float val = (float) resAreaScore.doc_sim_dic
											.get(doc_set);
									for (Object s : doc_set1) {
										int s_integer = Integer.parseInt((String)s);
										System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
										
										if (s_integer != hits_Kt[i].doc) {
											System.out.println("Here?!" + hits_Kt[i].doc);
											Document hitDoc1 = resAreaScore.areaisearcher
													.doc(s_integer);
											String area_name = hitDoc1.get("name");
											resAreaScore.getAreaTerms(area_name);
											System.out.println("A, I coming here??");
											totFreq = resAreaScore
													.calculateTotFreq(resAreaScore.areaCorpusMap);
											System.out.println("TotFreq = " + totFreq);
											break;
										}
									}
									double docSim = val / cummKtPlusProb;
									System.out.println("TotFreq = " + totFreq);
									for (int loop_cnt = 0; loop_cnt < word_freq; loop_cnt++) {
										Object look_term = resAreaScore.areaCorpusMap.get(term);
										System.out.println("Helloooooooooo term = " + term);
										if (look_term != null) {
											double word_prob = alpha * 1 / totFreq;
										} else {
											double word_prob = (1 - alpha) * 1
													/ totFreq;
										}

									}
								}
							}

							}
						}

					}
				System.out.println("Processing only one researcher");
				System.exit(0);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}
}