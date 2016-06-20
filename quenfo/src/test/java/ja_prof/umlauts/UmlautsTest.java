package ja_prof.umlauts;

import static org.junit.Assert.*;
import is2.data.SentenceData09;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.IEPreprocessingWrapper;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;

public class UmlautsTest {
	
	static String dbPath = "umlaute_db.db";
	static List<JobAd> jobAds;
	
	@BeforeClass
	public static void getParagraphs() throws SQLException, ClassNotFoundException{
		Connection connection = DBConnector.connect(dbPath);
		jobAds = DBConnector.getJobAdsExcept(connection, 2012);
	}

	@Ignore
	@Test
	public void testTokenizeWithIETokenizer() {
		List<List<List<String>>> tokenizedParagraphs = new ArrayList<List<List<String>>>();
	
		for (JobAd jobAd : jobAds) {
			List<List<String>> tokenizedSentences = IEPreprocessingWrapper.tokenizeWithIETokenizer(jobAd.getContent());
			tokenizedParagraphs.add(tokenizedSentences);
		}
		
		for (List<List<String>> list : tokenizedParagraphs) {
			for (List<String> list2 : list) {
				System.out.println(list2);
			}
		}
	}
	
	@Ignore
	@Test
	public void testCompleteLinguisticProcessing() {
		List<List<SentenceData09>> tokenizedParagraphs = new ArrayList<List<SentenceData09>>();
	
		for(int i = 0; i <1; i++){
			System.out.println(i);
			List<SentenceData09> completeLinguisticPreprocessing = IEPreprocessingWrapper.completeLinguisticPreprocessing(jobAds.get(i).getContent(), true);
			tokenizedParagraphs.add(completeLinguisticPreprocessing);
		}
		
		for (List<SentenceData09> sdList : tokenizedParagraphs) {
			for (SentenceData09 sd : sdList) {
				System.out.println(Arrays.asList(sd.plemmas));
//				System.out.println(Arrays.asList(sd.ppos));
//				System.out.println(Arrays.asList(sd.plabels));
			}
		}
	}
	
	@Ignore
	@Test
	public void testSimpleTokenizer(){
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		List<List<String>> tokenizedSentences = new ArrayList<List<String>>();
		IETokenizer iet = new IETokenizer();
		
		for (JobAd jobAd : jobAds) {
			System.out.println("jobad");
			
			List<String> sentences = iet.splitIntoSentences(jobAd.getContent(), false);
			
			for(String sentence : sentences){
				System.out.println("sentence");
				ArrayList<String> tokens = new ArrayList<String>();
				tokens.addAll(tokenizer.tokenize(sentence));
				tokenizedSentences.add(tokens); 
			}
		}
		
		for (List<String> list : tokenizedSentences) {
			System.out.println(list);
		}
	}
	
	@Test
	public void listIndexTest(){
		List<String> list = new ArrayList<String>();
		list.add("Eins");
		list.add("Zwei");
		list.add("Drei");
		System.out.println(list.indexOf("Eins"));
	}

}
