package OldCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

public class ComputeEquationRunnable {
	
	public static final String CONTENT = "corpus";
	public static Set<String> terms = new HashSet<>();
	public static RealVector v1;
	public static RealVector v2;
	public Analyzer analyzer;
	public Directory areadirectory;
	public IndexReader areaireader;
	public IndexSearcher areaisearcher;
	public Directory yeardirectory;
	public IndexReader yearireader;
	public IndexSearcher yearisearcher;
	public Map<String, TestTermParams> testCorpusMap;
	public Map<String, Integer> areaCorpusMap;
    
	public class TestTermParams{
    	int freq;
    	double prob;
    	public TestTermParams(int freq, double prob){
    		this.freq = freq;
    		this.prob = prob;
    	}
    }
    
	public HashMap doc_sim_dic;
    
    public void Init() {
    	doc_sim_dic = new HashMap<>();
    	analyzer = new StandardAnalyzer(Version.LUCENE_43);
	}

    public void getDocSimDic(){
    	try{
    		BufferedReader br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/doc_similarity.txt"));
			String line = null;
			Set doc_pair = null;
			while ((line = br.readLine()) != null) {
				String[] words = line.split(" ", 3);
				Float score = new Float(words[2]);
				doc_pair = new HashSet();
				doc_pair.add((Object)words[0]);
				doc_pair.add((Object)words[1]);
				doc_sim_dic.put(doc_pair, score);
			}
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }

    public void openAreaIndex(){
    	try{
			File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntAreaIndex");
			areadirectory = FSDirectory.open(path);
			areaireader = DirectoryReader.open(areadirectory);
			areaisearcher = new IndexSearcher(areaireader);
    	} catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    public void openYearIndex(){
    	try{
			File path = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntYearIndex");
			yeardirectory = FSDirectory.open(path);
			yearireader = DirectoryReader.open(yeardirectory);
			yearisearcher = new IndexSearcher(yearireader);
    	} catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    public BufferedReader openQueryFile(){
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/res_qry_by_year.json"));
			return br;
		} catch(IOException e){
			e.printStackTrace();
			return br;
		}
    }
    
    public String extractQuery(HashMap query_dic){
    	StringBuffer full_query_str = new StringBuffer();
    	String query_str = new String();
    	if (query_dic.containsKey("2010"))
    		return (String)query_dic.get("2010");
    	else
    	return null;
    }
    
    
    public void getTestCorpus(String year) throws IOException{
		Term t = new Term("year", year);
		Query query = new TermQuery(t);
		double prob = 0.0;
		TopDocs topdocs = yearisearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
//		System.out.println("scoreDocs size = " + scoreDocs.length);
		int doc_no = scoreDocs[0].doc;

		Terms year_terms = yearireader.getTermVector(doc_no,
				"corpus");
		TermsEnum termsEnum = null;
		termsEnum = year_terms.iterator(termsEnum);
		BytesRef name = null;
		testCorpusMap = new HashMap<String, TestTermParams>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			TestTermParams ttp = new TestTermParams(freq, prob);
			testCorpusMap.put(term, ttp);
		}
    	
    }
    
    public void getAreaTerms(String area_name) throws IOException{
//    	System.out.println("In getAreaCorpus!!!!");
//    	System.out.println("getAreaTerms: area_name " + area_name);
    	
		Term t = new Term("name", area_name);
		Query query = new TermQuery(t);
		
		TopDocs topdocs = areaisearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
//		System.out.println("scoreDocs size = " + scoreDocs.length);
		int doc_no = scoreDocs[0].doc;
//		System.out.println("Doc no = " + doc_no);
		Terms area_terms = areaireader.getTermVector(doc_no,"corpus");
		TermsEnum termsEnum = null;
		termsEnum = area_terms.iterator(termsEnum);
		BytesRef name = null;
		areaCorpusMap = new HashMap<>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			areaCorpusMap.put(term, freq);
		}

    }
    
    public int calculateTotFreq(Map<String, Integer> areaCorpusMap){
    	int totalFreq = 0;
    	
		for(Object term: areaCorpusMap.keySet()){
//			System.out.println(term + ":" + areaCorpusMap.get(term));
			totalFreq += (int)areaCorpusMap.get(term);
		}
    	return totalFreq;
    }
    
    public static String escapeQuery(String line){
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
    public class runnableClass implements Runnable{
    	ComputeEquationRunnable resAreaScore;
    	ScoreDoc[] hits_Kt;
    	double cummKtProb;
    	double alpha = 0.85;
    	Object term;
    	TestTermParams ttp;
    	int word_freq;
    	double wordProb;
    	public runnableClass(ComputeEquationRunnable resAreaScore, ScoreDoc[] hits_Kt, Object term){
    		this.resAreaScore = resAreaScore;
    		this.hits_Kt = hits_Kt;
			TestTermParams ttp = resAreaScore.testCorpusMap.get(term);
    	}
    	public void run(){
    		try{
    			for (int i = 0; i < hits_Kt.length; i++) {
			double firstProb = hits_Kt[i].score / cummKtProb;
			Document hitDoc = resAreaScore.areaisearcher
					.doc(hits_Kt[i].doc);
			String doc_name = hitDoc.get("name");
//			System.out.println(doc_name + "\t" + hits_Kt[i].doc
//					+ "\t" + hits_Kt[i].score + "\t"
//					+ firstProb);
			
			double cummKtPlusProb = 0.0;
//			System.out.println("doc_sim_dic count = " + resAreaScore.doc_sim_dic.size());
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
//			System.out.println("num_doc_sim = " + num_doc_sim);
//			System.out.println("out of loop");
			for (Object doc_set : resAreaScore.doc_sim_dic
					.keySet()) {
				
				Set doc_set1 = (Set) doc_set;
				String docno = "" + hits_Kt[i].doc;
				if (doc_set1.contains(docno)) {
					int totFreq = 0;
//					System.out.println("234234234234234234 printing doc_set1 : " +
//							 doc_set1 + " " + docno);
					 Thread.sleep(2000);
					float val = (float) resAreaScore.doc_sim_dic
							.get(doc_set);
					if (doc_set1.size() == 1){
						int s_integer = hits_Kt[i].doc;
						Document hitDoc1 = resAreaScore.areaisearcher
								.doc(s_integer);
						String area_name = hitDoc1.get("name");
						resAreaScore.getAreaTerms(area_name);
						System.out.println("OK till HERE!@!@!@!@!");
						totFreq = resAreaScore
								.calculateTotFreq(resAreaScore.areaCorpusMap);
						System.out.println("OK till HERE!@!@!@!@! tooo");
					} else{
						for (Object s : doc_set1) {
							int s_integer = Integer.parseInt((String)s);
							
							if (s_integer != hits_Kt[i].doc) {
								Document hitDoc1 = resAreaScore.areaisearcher
										.doc(s_integer);
								String area_name = hitDoc1.get("name");
								resAreaScore.getAreaTerms(area_name);
								System.out.println("OK till HERE!@!@!@!@!");
								totFreq = resAreaScore
										.calculateTotFreq(resAreaScore.areaCorpusMap);
								System.out.println("OK till HERE!@!@!@!@! tooo");
//								System.out.println("TotFreq = " + totFreq);
								break;
							}
						}
					}
					double secondProb = val / cummKtPlusProb;
//					System.out.println("TotFreq = " + totFreq);
						double thirdProb = 0.0;
						Object look_term = resAreaScore.areaCorpusMap.get(term);
//									System.out.println("Helloooooooooo term = " + term);
						if (look_term != null) {
							thirdProb = alpha * 1 / totFreq;
						} else {
							thirdProb = (1 - alpha) * 1
									/ totFreq;
						}
						thirdProb *= word_freq;
						double cummProb = firstProb * secondProb * thirdProb;
//									System.out.println("cummProb = " + cummProb);
						wordProb += cummProb;
					System.out.println("Done with test word parsing");
				}
			}
			System.out.println("Done with second loop");
    			}
    			ttp.prob = wordProb;
    		}catch (IOException e){
    			e.printStackTrace();
    		}catch(InterruptedException e){
    			e.printStackTrace();
    		}
    	}
    }
    
	public static void main(String args[]) throws IOException, ParseException {
		final long startTime = System.currentTimeMillis();
		
		ComputeEquationRunnable resAreaScore = new ComputeEquationRunnable();
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
				double researcherProb = 0.0;
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
//					System.out.println("Total # docs indexed = "
//							+ resAreaScore.areaireader.maxDoc());
					
//						System.out.println(term + ":"
//								+ resAreaScore.testCorpusMap.get(term));
//						System.out.println("#hits = " + hits_Kt.length);
						for (int i = 0; i < hits_Kt.length; i++) {
							Document hitDoc = resAreaScore.areaisearcher
									.doc(hits_Kt[i].doc);
							cummKtProb += hits_Kt[i].score;
						}
//						System.out.println(cummKtProb);
						for (Object term : resAreaScore.testCorpusMap.keySet()) {
							TestTermParams ttp = resAreaScore.testCorpusMap.get(term);
							int word_freq = ttp.freq;
							double wordProb = 0.0;
							new Thread(resAreaScore.new runnableClass(resAreaScore, hits_Kt, term)).start();
							ttp.prob = wordProb;
//						for (int i = 0; i < hits_Kt.length; i++) {
							
//							double firstProb = hits_Kt[i].score / cummKtProb;
//							Document hitDoc = resAreaScore.areaisearcher
//									.doc(hits_Kt[i].doc);
//							String doc_name = hitDoc.get("name");
////							System.out.println(doc_name + "\t" + hits_Kt[i].doc
////									+ "\t" + hits_Kt[i].score + "\t"
////									+ firstProb);
//							
//							double cummKtPlusProb = 0.0;
////							System.out.println("doc_sim_dic count = " + resAreaScore.doc_sim_dic.size());
//							int num_doc_sim = 0;
//							for (Object doc_set : resAreaScore.doc_sim_dic
//									.keySet()) {
//								Set doc_set1 = (Set) doc_set;
//								String docno = "" + hits_Kt[i].doc;
//								if (doc_set1.contains(docno)) {
//									float val = (float) resAreaScore.doc_sim_dic
//											.get(doc_set);
//									num_doc_sim += 1;
//									cummKtPlusProb += val;
//								}
//							}
////							System.out.println("num_doc_sim = " + num_doc_sim);
////							System.out.println("out of loop");
//							for (Object doc_set : resAreaScore.doc_sim_dic
//									.keySet()) {
//								
//								Set doc_set1 = (Set) doc_set;
//								String docno = "" + hits_Kt[i].doc;
//								if (doc_set1.contains(docno)) {
//									int totFreq = 0;
////									System.out.println("234234234234234234 printing doc_set1 : " +
////											 doc_set1 + " " + docno);
//									 Thread.sleep(2000);
//									float val = (float) resAreaScore.doc_sim_dic
//											.get(doc_set);
//									if (doc_set1.size() == 1){
//										int s_integer = hits_Kt[i].doc;
//										Document hitDoc1 = resAreaScore.areaisearcher
//												.doc(s_integer);
//										String area_name = hitDoc1.get("name");
//										resAreaScore.getAreaTerms(area_name);
//										totFreq = resAreaScore
//												.calculateTotFreq(resAreaScore.areaCorpusMap);
//									} else{
//										for (Object s : doc_set1) {
//											int s_integer = Integer.parseInt((String)s);
//											
//											if (s_integer != hits_Kt[i].doc) {
//												Document hitDoc1 = resAreaScore.areaisearcher
//														.doc(s_integer);
//												String area_name = hitDoc1.get("name");
//												resAreaScore.getAreaTerms(area_name);
//												totFreq = resAreaScore
//														.calculateTotFreq(resAreaScore.areaCorpusMap);
////												System.out.println("TotFreq = " + totFreq);
//												break;
//											}
//										}
//									}
//									double secondProb = val / cummKtPlusProb;
////									System.out.println("TotFreq = " + totFreq);
//									for (Object term : resAreaScore.testCorpusMap.keySet()) {
//										double wordProb = 0.0;
//										TestTermParams ttp = resAreaScore.testCorpusMap.get(term);
//										int word_freq = ttp.freq;
//										double thirdProb = ttp.prob;
//										Object look_term = resAreaScore.areaCorpusMap.get(term);
//	//									System.out.println("Helloooooooooo term = " + term);
//										if (look_term != null) {
//											thirdProb = alpha * 1 / totFreq;
//										} else {
//											thirdProb = (1 - alpha) * 1
//													/ totFreq;
//										}
//										thirdProb *= word_freq;
//										double cummProb = firstProb * secondProb * thirdProb;
//	//									System.out.println("cummProb = " + cummProb);
//										ttp.prob += cummProb;
//								}
//									System.out.println("Done with test word parsing");
//								}
//							}
//							System.out.println("Done with second loop");
//							}
							
						}
//							researcherProb *= wordProb;
							System.out.println("researcherProb = " + researcherProb);
						
					}
				final long endTime = System.currentTimeMillis();
				System.out.println("Total execution time: " + (endTime - startTime) );
				System.out.print("Total probability of the researcher:" + researcher_name +" is "+ researcherProb);
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