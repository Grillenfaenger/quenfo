package ja_prof.umlauts;

import static org.junit.Assert.*;
import is2.data.SentenceData09;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.TranslationVocabulary;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.IEPreprocessingWrapper;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

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
	
	@Ignore
	@Test
	public void listIndexTest(){
		List<String> list = new ArrayList<String>();
		list.add("Eins");
		list.add("Zwei");
		list.add("Drei");
		System.out.println(list.indexOf("Eins"));
	}
	
	@Ignore
	@Test
	public void listIndexTest2(){
		List<String> list = new ArrayList<String>();
		list.add("Eins");
		list.add("Zwei");
		list.add("Drei");
		System.out.println(list.get(0));
	}
	
	@Ignore
	@Test
	public void keywordContextIOTest() throws IOException, SQLException{
		// TODO: später sollen die hier geholten Daten erst einmal persistiert werden, Trainieren erfolgt dann in einer eigenen Methode
				// Trainieren
				Map<String, TreeSet<String>> ambiguities = null;
				KeywordContexts keywordContexts = null;
				
				IETokenizer tokenizer = new IETokenizer();
				ArrayList<String> tokens = new ArrayList<String>();
				
				for (JobAd jobAd : jobAds) {
					tokens.addAll(Arrays.asList(tokenizer.tokenizeSentence(jobAd.getContent())));	
				}
				
				Vocabulary voc = new Vocabulary(tokens);
				System.out.println("Tokens: " + voc.getNumberOfTokens());
				System.out.println("Types: " + voc.vocabulary.size());
				
				
				Vocabulary umlautVoc = voc.getAllByRegex(".*([ÄäÖöÜü]).*");
				umlautVoc.generateNumberOfTokens();
				System.out.println("Token mit Umlaut: " + umlautVoc.getNumberOfTokens());
				System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
				FileUtils.printMap(umlautVoc.vocabulary, "output//classification//", "WörtermitUmlautenTest");
				
				TranslationVocabulary transVoc = new TranslationVocabulary();
				for (String key : umlautVoc.vocabulary.keySet()) {
					transVoc.addEntry(key);
				}
				System.out.println("Wörterbuch erstellt");

				// Suche nach Ambiguitäten
				/*TEST*/String test = "Farb-";
				
				ambiguities = transVoc.findAmbiguities(voc);
				/*TEST*/System.out.println(ambiguities.get(test));
				FileUtils.printMap(ambiguities, "output//classification//", "ambigeWörter");
				System.out.println(ambiguities.size() + " Gruppen mehrdeutiger Wörter gefunden");
				
				// Kontexte der ambigen Wörter holen und ausgeben
				System.out.println("Kontexte suchen...");
				keywordContexts = DBConnector.getKeywordContexts(jobAds, transVoc.createAmbiguitySet(ambiguities));
				keywordContexts.printKeywordContexts("output//classification//", "KontexteTest");
				
				
				
				// read
				KeywordContexts in = keywordContexts.loadKeywordContextsFromFile("output//classification//KontexteTest.txt");
				System.out.println(in);
				in.printKeywordContexts("output//classification//", "KontexteNachErneutemEinlesen");
	}
	
	@Ignore
	@Test
	public void translationVocIOTest() throws IOException, SQLException{
		Map<String, TreeSet<String>> ambiguities = null;
		KeywordContexts keywordContexts = null;
		
		IETokenizer tokenizer = new IETokenizer();
		ArrayList<String> tokens = new ArrayList<String>();
		
		for (JobAd jobAd : jobAds) {
			tokens.addAll(Arrays.asList(tokenizer.tokenizeSentence(jobAd.getContent())));	
		}
		
		Vocabulary voc = new Vocabulary(tokens);
		System.out.println("Tokens: " + voc.getNumberOfTokens());
		System.out.println("Types: " + voc.vocabulary.size());
		
		
		Vocabulary umlautVoc = voc.getAllByRegex(".*([ÄäÖöÜü]).*");
		umlautVoc.generateNumberOfTokens();
		System.out.println("Token mit Umlaut: " + umlautVoc.getNumberOfTokens());
		System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
		FileUtils.printMap(umlautVoc.vocabulary, "output//classification//", "WörtermitUmlautenTest");
		
		TranslationVocabulary transVoc = new TranslationVocabulary();
		for (String key : umlautVoc.vocabulary.keySet()) {
			transVoc.addEntry(key);
		}
		
		transVoc.printToFile("output//classification//", "Ersetzungen");
		transVoc.loadTranslationVocabularyFromFile("output//classification//Ersetzungen.txt");
		
	}
	
	@Ignore
	@Test
	public void ambiguitiesIOTest() throws IOException, SQLException{
		Map<String, TreeSet<String>> ambiguities = null;
		KeywordContexts keywordContexts = null;
		
		IETokenizer tokenizer = new IETokenizer();
		ArrayList<String> tokens = new ArrayList<String>();
		
		for (JobAd jobAd : jobAds) {
			tokens.addAll(Arrays.asList(tokenizer.tokenizeSentence(jobAd.getContent())));	
		}
		
		Vocabulary voc = new Vocabulary(tokens);
		System.out.println("Tokens: " + voc.getNumberOfTokens());
		System.out.println("Types: " + voc.vocabulary.size());
		
		
		Vocabulary umlautVoc = voc.getAllByRegex(".*([ÄäÖöÜü]).*");
		umlautVoc.generateNumberOfTokens();
		System.out.println("Token mit Umlaut: " + umlautVoc.getNumberOfTokens());
		System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
		FileUtils.printMap(umlautVoc.vocabulary, "output//classification//", "WörtermitUmlautenTest");
		
		TranslationVocabulary transVoc = new TranslationVocabulary();
		for (String key : umlautVoc.vocabulary.keySet()) {
			transVoc.addEntry(key);
		}
		System.out.println("Wörterbuch erstellt");

		// Suche nach Ambiguitäten
		/*TEST*/String test = "Farb-";
		
		ambiguities = transVoc.findAmbiguities(voc);
		/*TEST*/System.out.println(ambiguities.get(test));
		FileUtils.printMap(ambiguities, "output//classification//", "ambigeWörter");
		System.out.println(ambiguities.size() + " Gruppen mehrdeutiger Wörter gefunden");
		
		TreeMap<String, TreeSet<String>> fileToAmbiguities = FileUtils.fileToAmbiguities("output//classification//ambigeWörter.txt");
		FileUtils.printMap(fileToAmbiguities, "output//classification//", "ambigeWörter2");
		
	}


}
