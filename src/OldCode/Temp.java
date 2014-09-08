package OldCode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class Temp {
	public static void main(String args[]) throws IOException{
	int vocabCnt = 0;
	BufferedWriter vocab = new BufferedWriter(new FileWriter("/home/archana/SCU_projects/research_changes/lucene/vocab"));
	ArrayList uniqVocab = new ArrayList<>();
	File temppath = new File("/home/archana/SCU_projects/research_changes/lucene/ResIntKeywordsResearcherCorpusVocabIndex");
	Directory directory = FSDirectory.open(temppath);
	DirectoryReader iVocabTReader = DirectoryReader.open(directory);
	System.out.println("Done indexing "+iVocabTReader.maxDoc()+" docs");
	Set vocabulary = new HashSet<>();
	for (int j = 0; j < iVocabTReader.maxDoc(); j++) {
		Terms area_terms = iVocabTReader.getTermVector(j, "researcherCorpus");
		TermsEnum termsEnum = null;
		termsEnum = area_terms.iterator(termsEnum);
		BytesRef name = null;
		while ((name = termsEnum.next()) != null) {
			String term = name.utf8ToString();
			vocab.write("\""+term+"\",");
			vocabulary.add(term);
			int freq = (int) termsEnum.totalTermFreq();
			vocabCnt += 1;
		}
	}
	vocab.write("\n");
	System.out.println("Vocab size = " + vocabulary.size());
	System.out.println();
	System.out.println("Total VocabCnt = "+ vocabCnt);
}
}