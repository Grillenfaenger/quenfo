package de.uni_koeln.spinfo.umlauts.applications.old;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.Dictionary;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class vocabStatsApp {
	
	static String dbPath = "umlaute_db.db";
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		ArrayList<String> tokens = new ArrayList<String>();
		
		// Verbindung aufbauen und JobAds im Speicher halten
		Connection connection = DBConnector.connect(dbPath);
		List<JobAd> jobAds = DBConnector.getJobAdsExcept(connection, 2012);
		for (JobAd jobAd : jobAds) {
			tokens.addAll(tokenizer.tokenize(jobAd.getContent()));	
		}
		
		Vocabulary withUmlauts = new Vocabulary(tokens);
		System.out.println("Tokens: " + withUmlauts.getNumberOfTokens());
		System.out.println("Types: " + withUmlauts.vocabulary.size());
//		System.out.println(withUmlauts.vocabulary);
		
		
		Vocabulary umlautVoc = withUmlauts.getAllByRegex(".*([ÄäÖöÜüß]).*");
		umlautVoc.generateNumberOfTokens();
		System.out.println("Wörter mit Umlaut: " + umlautVoc.getNumberOfTokens());
		System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
//		System.out.println(umlautVoc.vocabulary);
		FileUtils.printMap(umlautVoc.vocabulary, "output//", "WörtermitUmlauten");
		
		Dictionary transVoc = new Dictionary(umlautVoc);
		System.out.println(transVoc.dictionary);
		
		Vocabulary darkVowelVoc = withUmlauts.getAllByRegex(".*([AaOoUu]).*");
		darkVowelVoc.generateNumberOfTokens();
		System.out.println("Wörter mit dunklem Vokal: " + darkVowelVoc.getNumberOfTokens());
		System.out.println("Types mit dunklem Vokal: " + darkVowelVoc.vocabulary.size());
//		System.out.println(darkVowelVoc.vocabulary);
		
		
		// ToDo: Stats die Zweite: suche nach ambiguitäten
		Map<String, HashSet<String>> ambiguities = transVoc.findAmbiguities(withUmlauts);
		FileUtils.printMap(ambiguities, "output//", "ambigeWörter");
		
		// Kontexte der ambigen Wörter ausgeben
		KeywordContexts keywordContexts6 = DBConnector.getKeywordContexts6(connection, transVoc.createAmbiguitySet(ambiguities));
		keywordContexts6.printKeywordContexts("output//", "Kontexte6");
		
	}

}
