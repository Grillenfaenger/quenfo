package de.uni_koeln.spinfo.umlauts.classification;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.umlauts.classification.UmlautClassifyUnit;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.Sentence;
import de.uni_koeln.spinfo.umlauts.data.TranslationVocabulary;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class ConfigurableUmlautClassifier {
	
	static String dbPath = "umlaute_db.db";
	
	public void classify(ExperimentConfiguration config) throws ClassNotFoundException, SQLException, IOException {

		// TODO: später sollen die hier geholten Daten erst einmal persistiert werden, Trainieren erfolgt dann in einer eigenen Methode
		// Trainieren
		Map<String, TreeSet<String>> ambiguities = null;
		Map<String, Model> models= null;
		
		// Gruppierte Lesarten + deren Kontexte holen (Dafür reicht der einfache Tokenisierer)
		KeywordContexts keywordContexts = null;
		
		IETokenizer tokenizer = new IETokenizer();
		ArrayList<String> tokens = new ArrayList<String>();
		
		Connection connection = DBConnector.connect(dbPath);
		List<JobAd> jobAds = DBConnector.getJobAdsExcept(connection, 2012);
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
		FileUtils.printMap(umlautVoc.vocabulary, "output//classification//", "WörtermitUmlauten");
		
		TranslationVocabulary transVoc = new TranslationVocabulary();
		for (String key : umlautVoc.vocabulary.keySet()) {
			transVoc.addEntry(key);
		}

		// Suche nach Ambiguitäten
		ambiguities = transVoc.findAmbiguities(voc);
		FileUtils.printMap(ambiguities, "output//classification//", "ambigeWörter");
		
		// Kontexte der ambigen Wörter holen und ausgeben
		keywordContexts = DBConnector.getKeywordContexts6(connection, transVoc.createAmbiguitySet(ambiguities));
		keywordContexts.printKeywordContexts("output//classification//", "Kontexte6");
	
		
	//Für jede Lesartengruppe Trainingsmodelle erstellen
		List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
		ZoneJobs jobs = new ZoneJobs();
		
		// Classification Units erstellen (diese sind dann schon initialisiert)
		for (Entry<String,TreeSet<String>> entry : ambiguities.entrySet()) {
			String[] senses = entry.getValue().toArray(new String[entry.getValue().size()]);
			for(String string : entry.getValue()){
				List<List<String>> context = keywordContexts.getContext(string);
				ZoneClassifyUnit cu = new UmlautClassifyUnit(tokens, string, senses, true);
				trainingData.add(cu);
			}
			trainingData = jobs.setFeatures(trainingData, config.getFeatureConfiguration(), true);
			trainingData = jobs.setFeatureVectors(trainingData, config.getFeatureQuantifier(), null);

			// build model for each group
			Model model = jobs.getNewModelForClassifier(trainingData, config);
			models.put(entry.getKey(), model);
			}
			
		
	// Modelle den Gruppen zugeordnet vorhalten
	
	
	
	//Klassifizieren
	
	// Im Jahrgang ohne Umlaute nach umlautambigen Wörtern suchen
	
	jobAds = DBConnector.getJobAds(connection, 2012);
	
	// Je eine Anzeige
	for (JobAd jobAd : jobAds){
		// In Sätze splitten und deren Span festhalten
		// Sätze tokenisieren und die Position der Tokens im Satz festhalten
		List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), false);
		
		//TODO Eindeutige Token erkennen und korrigieren
		
		// mehrdeutige Vorkommen erkennen 
		// für jeden Fund einzeln: 
			// Kontext extrahieren
			// Klassifizieren: cu erstellen, setFeatures(), setFeatureVectors, das entsprechende Modell auswählen und klassifizieren
		//cu.getSense() mit cu.getContent() vergleichen. Falls identisch kein Handlungsbedarf
		// ansonsten ersetzen
		
		
	}
	
		
	
	
	}
	

}
