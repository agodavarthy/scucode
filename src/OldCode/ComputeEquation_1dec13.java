package OldCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

public class ComputeEquation_1dec13 {
	
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
    private Map<String, TestTermParams> testCorpusMap;
    private Map<String, TestTermParams> testCorpusMapBaseline;
    private Map<String, Integer> areaCorpusMap;
    private Map<String, Integer> areaCorpusMapBaseline;
    
    private class TestTermParams{
    	int freq;
    	double prob;
    	public TestTermParams(int freq, double prob){
    		this.freq = freq;
    		this.prob = prob;
    	}
    }
    
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
			br = new BufferedReader(new FileReader("/home/archana/SCU_projects/research_changes/lucene/res_qry_by_year_smaller_subset.json"));
			return br;
		} catch(IOException e){
			e.printStackTrace();
			return br;
		}
    }
    
    private String extractQuery(HashMap query_dic, String year){
    	StringBuffer full_query_str = new StringBuffer();
    	String query_str = new String();
    	if (query_dic.containsKey(year))
    		return (String)query_dic.get(year);
    	else
    		System.out.println("No papers for year " + year);
    	return null;
    }
    
    
    private void getTestCorpus(String year) throws IOException{
		Term t = new Term("year", year);
		Query query = new TermQuery(t);
		double prob = 0.0;
		TopDocs topdocs = yearisearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		
		int doc_no = scoreDocs[0].doc;

		Terms year_terms = yearireader.getTermVector(doc_no,
				"corpus");
		TermsEnum termsEnum = null;
		termsEnum = year_terms.iterator(termsEnum);
		BytesRef name = null;
		testCorpusMap = new HashMap<String, TestTermParams>();
		testCorpusMapBaseline = new HashMap<String, TestTermParams>();
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			TestTermParams ttp = new TestTermParams(freq, prob);
			testCorpusMap.put(term, ttp);
			testCorpusMapBaseline.put(term, ttp);
		}
    	
    }
    
    private void getAreaTerms(String area_name, Map<String, Integer> areaCorpusMaptemp) throws IOException{
    	
		Term t = new Term("name", area_name);
		Query query = new TermQuery(t);
		
		TopDocs topdocs = areaisearcher.search(query, 10);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		int doc_no = scoreDocs[0].doc;
		Terms area_terms = areaireader.getTermVector(doc_no,"corpus");
		TermsEnum termsEnum = null;
		termsEnum = area_terms.iterator(termsEnum);
		BytesRef name = null;
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			areaCorpusMaptemp.put(term, freq);
		}

    }
    
    
    private int calculateTotFreq(Map<String, Integer> areaCorpusMap){
    	int totalFreq = 0;
    	
		for(Object term: areaCorpusMap.keySet()){
			totalFreq += (int)areaCorpusMap.get(term);
		}
    	return totalFreq;
    }
    
    private int calculateTotTestTeFreq(Map<String, TestTermParams> testCorpusMap){
    	int totalFreq = 0;
    	
		for(Object term: testCorpusMap.keySet()){
			TestTermParams ttp = testCorpusMap.get(term);
			totalFreq += ttp.freq;
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

    
    
    
	public static void main(String args[]) throws IOException, ParseException {
		final long startTime = System.currentTimeMillis();
		
		ComputeEquation_1dec13 resAreaScore = new ComputeEquation_1dec13();
		resAreaScore.Init();
		resAreaScore.openAreaIndex();
		resAreaScore.openYearIndex();
		double alpha = 0.85;
		double resIntPerplexity;
		double finalResIntPerplexity;
		double finalBaselinePerplexity;
		double baselinePerplexity;
		double cummResIntPerProb = 0.0;
		double cummBaselinePerProb = 0.0;
				
		resAreaScore.getTestCorpus("2012");
		int totFreqTest = 0;
		totFreqTest = resAreaScore.calculateTotTestTeFreq(resAreaScore.testCorpusMap);
		resAreaScore.getDocSimDic();
		int count = 1;
		BufferedReader br = resAreaScore.openQueryFile();
		if (br == null) {
			System.out.print("Could not open the file");
			System.exit(1);
		}
		JSONParser json_parser = new JSONParser();
		String line = null;
        BufferedWriter bw1 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/regular_probabilities"));
        BufferedWriter bw2 = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/baseline_probabilities"));
		try {
			while ((line = br.readLine()) != null) {
				double researcherProb = 1.0;
				double researcherProbBaseline = 1.0;
				Object obj = json_parser.parse(line);
				JSONObject jsonObject = (JSONObject) obj;
				String researcher_id = (String) jsonObject.get("_id");
				String researcher_name = (String) jsonObject.get("name");
				System.out.println("**************" + researcher_name + ":"	+ researcher_id + " **********************");
				HashMap query_dic = (HashMap) jsonObject.get("year_qry_dic");
				String year = "2011";
				String rawQueryStr = resAreaScore.extractQuery(query_dic, year);
				if (rawQueryStr != null) {
					String escaped_query_Kt = escapeQuery(rawQueryStr);
					if (escaped_query_Kt == null) {
						System.out.println(researcher_name+ " does not have papers for year "+ year);
						Thread.sleep(1000);
						continue;
					}
					QueryParser query_parser_Kt = new QueryParser(Version.LUCENE_43, "corpus", resAreaScore.analyzer);
					Query query_Kt = query_parser_Kt.parse(escaped_query_Kt);

					ScoreDoc[] hits_Kt = resAreaScore.areaisearcher.search(query_Kt, null, 1000).scoreDocs;
					double cummKtProb = 0.0;
						for (int i = 0; i < hits_Kt.length; i++) {
							Document hitDoc = resAreaScore.areaisearcher.doc(hits_Kt[i].doc);
							cummKtProb += hits_Kt[i].score;
						}
						for (int i = 0; i < hits_Kt.length; i++) {
							double firstProb = hits_Kt[i].score / cummKtProb;
							Document hitDoc = resAreaScore.areaisearcher.doc(hits_Kt[i].doc);
							String areaname_Kt = hitDoc.get("name");
							String areaname_Ktplus = "";
							double cummKtPlusProb = 0.0;
							int num_doc_sim = 0;
							for (Object doc_set : resAreaScore.doc_sim_dic.keySet()) {
								Set doc_set1 = (Set) doc_set;
								String docno = "" + hits_Kt[i].doc;
								if (doc_set1.contains(docno)) {
									float val = (float) resAreaScore.doc_sim_dic.get(doc_set);
									num_doc_sim += 1;
									cummKtPlusProb += val;
								}
							}
							for (Object doc_set : resAreaScore.doc_sim_dic.keySet()) {
								Set doc_set1 = (Set) doc_set;
								String docno = "" + hits_Kt[i].doc;
								if (doc_set1.contains(docno)) {
									int totFreq = 0;
									float val = (float) resAreaScore.doc_sim_dic.get(doc_set);
									if (doc_set1.size() == 1){
										int s_integer = hits_Kt[i].doc;
										Document hitDoc1 = resAreaScore.areaisearcher.doc(s_integer);
										areaname_Ktplus = hitDoc1.get("name");
									} else{
										for (Object s : doc_set1) {
											int s_integer = Integer.parseInt((String)s);
											if (s_integer != hits_Kt[i].doc) {
												Document hitDoc1 = resAreaScore.areaisearcher.doc(s_integer);
												areaname_Ktplus = hitDoc1.get("name");
												resAreaScore.areaCorpusMap =  new HashMap<>();
												break;
											}
										}
									}
									resAreaScore.getAreaTerms(areaname_Ktplus, resAreaScore.areaCorpusMap);
									totFreq = resAreaScore.calculateTotFreq(resAreaScore.areaCorpusMap);

									double secondProb = val / cummKtPlusProb;
									for (Object term : resAreaScore.testCorpusMap.keySet()) {
										TestTermParams ttp = resAreaScore.testCorpusMap.get(term);
										int word_freq = ttp.freq;
										double thirdProb = ttp.prob;
										Object look_term = resAreaScore.areaCorpusMap.get(term);
										if (look_term != null) {
											thirdProb = alpha * 1 / totFreq;
										} else {
											thirdProb = (1 - alpha) * 1
													/ totFreq;
										}
										thirdProb *= word_freq;
										double cummProb = firstProb * secondProb * thirdProb;
										bw1.write("Kt = " + areaname_Kt + "\tKtPlus = " + areaname_Ktplus + "\tword = " + term + "\n");
										bw1.write("P(Kt/r) = " + firstProb + "\tP(Kt+1/Kt) = " + secondProb + "\tP(w/Kt+1) = " + thirdProb + "\tcummProb = " + cummProb + "\n");
										ttp.prob += cummProb;
									}
								}
							}
							System.out.println("Done with second loop");
							}
						for (Object term : resAreaScore.testCorpusMap.keySet()) {
							TestTermParams ttp = resAreaScore.testCorpusMap.get(term);
							cummResIntPerProb += Math.log(ttp.prob);
							researcherProb *= ttp.prob;
							bw1.write(term + "\t" + ttp.prob + "\n");
						}
						resIntPerplexity = cummResIntPerProb / totFreqTest;
						finalResIntPerplexity = Math.pow(2, resIntPerplexity);
						finalResIntPerplexity = 1 / finalResIntPerplexity;
						
						final long endTime = System.currentTimeMillis();
						System.out.println("Total execution time: " + (endTime - startTime) );
						System.out.print("Total probability of the researcher:" + researcher_name +" is "+ researcherProb);
						System.out.println("Processing only one researcher");

						/***************** Computing Baseline **********************/
						for (int i = 0; i < hits_Kt.length; i++) {
							double firstProb = hits_Kt[i].score / cummKtProb;
							Document hitDoc = resAreaScore.areaisearcher.doc(hits_Kt[i].doc);
							String areaname_Kt = hitDoc.get("name");
							resAreaScore.areaCorpusMapBaseline =   new HashMap<>();
							System.out.println("Computing baseline prob for area = " + areaname_Kt);
							int totFreq = 0;
							resAreaScore.getAreaTerms(areaname_Kt, resAreaScore.areaCorpusMapBaseline);
							totFreq = resAreaScore.calculateTotFreq(resAreaScore.areaCorpusMapBaseline);

							for (Object term : resAreaScore.testCorpusMapBaseline.keySet()) {
								TestTermParams ttp = resAreaScore.testCorpusMapBaseline.get(term);
								int word_freq = ttp.freq;
								double thirdProb = ttp.prob;
								Object look_term = resAreaScore.areaCorpusMapBaseline.get(term);
								if (look_term != null) {
									thirdProb = alpha * 1 / totFreq;
								} else {
									thirdProb = (1 - alpha) * 1
											/ totFreq;
								}
								thirdProb *= word_freq;
								double cummProb = firstProb * thirdProb;
								bw2.write("Kt = " + areaname_Kt + "\tKtPlus = " + term + "\n");
								bw2.write("P(Kt/r) = " + firstProb + "\tP(w/Kt+1) = " + thirdProb + "\tcummProb = " + cummProb + "\n");

								ttp.prob += cummProb;
							}
							
							System.out.println("Done with second loop");
							}						

						for (Object term : resAreaScore.testCorpusMapBaseline.keySet()) {
							TestTermParams ttp2 = resAreaScore.testCorpusMapBaseline.get(term);
							System.out.println(term + " prob = " + ttp2.prob);
							researcherProbBaseline *= ttp2.prob;
							bw2.write(term + "\t" + ttp2.prob);
							cummBaselinePerProb += Math.log(ttp2.prob);
						}
						baselinePerplexity = cummBaselinePerProb / totFreqTest;
						finalBaselinePerplexity = Math.pow(2, baselinePerplexity);
						finalBaselinePerplexity = 1 / finalBaselinePerplexity;

						System.out.println("researcherProb = " + researcherProb);
						System.out.println("researcherProbBaseline = " + researcherProbBaseline);
						
						System.out.println("cummresIntPerplexity = " + cummResIntPerProb);
						System.out.println("cummBaselinePerplexity = " + cummBaselinePerProb);
						System.out.println("resIntPerplexity = " + resIntPerplexity);
						System.out.println("baselinePerplexity = " + baselinePerplexity);
						System.out.println("finalResIntPerplexity = " + finalResIntPerplexity);
						System.out.println("finlaBaselinePerplexity = " + finalBaselinePerplexity);
						finalResIntPerplexity = 1 / Math.pow(2, Math.log(researcherProb)/ totFreqTest);
						finalBaselinePerplexity = 1 / Math.pow(2, Math.log(researcherProbBaseline)/ totFreqTest);

						double logfinalResIntPerplexity = Math.log(researcherProb);
						double logfinalBaselinePerplexity = Math.log(researcherProbBaseline);

						System.out.println("logfinalResIntPerplexity = " + logfinalResIntPerplexity);
						System.out.println("logfinlaBaselinePerplexity = " + logfinalBaselinePerplexity);
						
						System.out.println("logfinalResIntPerplexity/totfreq = " + logfinalResIntPerplexity / totFreqTest);
						System.out.println("logfinlaBaselinePerplexity/totfreq = " + logfinalBaselinePerplexity / totFreqTest);

						System.out.println("finalResIntPerplexity = " + finalResIntPerplexity);
						System.out.println("finlaBaselinePerplexity = " + finalBaselinePerplexity);
				}
				final long endTime = System.currentTimeMillis();
				System.out.println("Total execution time: " + (endTime - startTime) );
				System.out.print("Total probability of the researcher:" + researcher_name +" is "+ researcherProb);
				System.out.println("Processing only one researcher");
				System.exit(0);
			}
			br.close();
			bw1.close();
			bw2.close();
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