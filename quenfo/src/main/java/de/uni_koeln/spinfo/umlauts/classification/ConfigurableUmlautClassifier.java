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

import opennlp.tools.util.Span;
import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.dbIO.DbConnector;
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
	
	static String inputDbPath = "umlaute_db.db";
	static String outputDbPath = "umlauteCORRECTED_db.db";
	
	public void classify(ExperimentConfiguration config) throws ClassNotFoundException, SQLException, IOException {

		// TODO: später sollen die hier geholten Daten erst einmal persistiert werden, Trainieren erfolgt dann in einer eigenen Methode
		// Trainieren
		Map<String, TreeSet<String>> ambiguities = null;
		Map<String, Model> models= null;
		
		// Gruppierte Lesarten + deren Kontexte holen (Dafür reicht der einfache Tokenisierer)
		KeywordContexts keywordContexts = null;
		
		IETokenizer tokenizer = new IETokenizer();
		ArrayList<String> tokens = new ArrayList<String>();
		
		Connection connection = DBConnector.connect(inputDbPath);
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
	StringBuffer buf = null;
	
	
	// Je eine Anzeige
	for (JobAd jobAd : jobAds){
		// In Sätze splitten und deren Span festhalten
		// Sätze tokenisieren und die Position der Tokens im Satz festhalten
		List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), false);
		
		
		Map<String,String> simpleReplacements = transVoc.createSimpleReplacementMap(ambiguities.keySet());
		for(Sentence s : tokenizedSentences){
			Map<String,List<Span>> simpleTokens = s.getTokenPos();
			
			// Eindeutige Token erkennen 
			simpleTokens.keySet().retainAll(simpleReplacements.keySet());
			
			//und korrigieren
			for(Entry<String, List<Span>> occurence : simpleTokens.entrySet()){
				for(Span span : occurence.getValue()){
					 buf = new StringBuffer(jobAd.getContent());

				        int start = span.getStart();
				        int end = span.getEnd();
				        String replacement = simpleReplacements.get(occurence.getKey());
				        System.out.println(occurence.getKey() + " wurde durch " + replacement + " ersetzt.");
				        buf.replace(start, end, replacement);
				      
				        //Kontrolle:
				        System.out.println(buf);
				        
				}
			}
			
			
			// mehrdeutige Vorkommen erkennen
			
			// TODO leider funktioniert dies hier nicht
//			Map<String,List<Span>> ambiguousTokens = s.getTokenPos();
//			ambiguousTokens.keySet().retainAll(ambiguities.keySet());
			
			// statt dessen:
			
			List<String> tokens2 = s.getTokens();
			for (int i = 0; i < tokens.size(); i++) {
				String word = tokens2.get(i);
				if(ambiguities.containsKey(word)){
					// Kontext extrahieren
					List<String> context = extractContext(tokens2, i, 2,2);
					// cu erstellen
					List<ClassifyUnit> cus = new ArrayList<ClassifyUnit>();
					ZoneClassifyUnit zcu = new UmlautClassifyUnit(context, word, ambiguities.get(word).toArray(new String[0]), false);
					cus.add(zcu);
					cus = jobs.setFeatures(cus, config.getFeatureConfiguration(), false);
					cus = jobs.setFeatureVectors(cus, config.getFeatureQuantifier(), null);
					
					// classify
					Map<ClassifyUnit, boolean[]> classified = jobs.classify(cus, config, models.get(word));
					
					for (ClassifyUnit cu : classified.keySet()) {
						((ZoneClassifyUnit) cu).setClassIDs(classified.get(cu));
					}
					List<ClassifyUnit> cuList = new ArrayList<ClassifyUnit>(classified.keySet());
					UmlautClassifyUnit result = (UmlautClassifyUnit) cuList.get(0);
					String wordAsClassified = result.getSense();
					System.out.println("Im Text: "+ word + "\nKlassifiziert: "+ wordAsClassified);
					
					// Falls hier ein Umlaut rekonstruiert werden muss 
					if(!wordAsClassified.equals(word)){
						// Korrektur
						Span span = s.getAbsoluteSpanOfToken(i);
						int start = span.getStart();
					    int end = span.getEnd();
					    buf.replace(start, end, wordAsClassified);
					      
					        //Kontrolle:
					        System.out.println(buf);
					}		
				}
			}
		}
		String corrected = buf.toString();
		jobAd.replaceContent(corrected);
		
	}
	// In neue Datenbank schreiben - egal ob eine Änderung vorgenommen wurde oder nicht.
	// Dies muss für eine kleine Evaluation nicht gemacht werden.
			Connection outputConnection = DbConnector.connect(outputDbPath);
			DbConnector.createBIBBDB(outputConnection);
			DbConnector.insertJobAdsInBIBBDB(outputConnection, jobAds);
	
	}
	
	private List<String> extractContext(List<String> text, int index, int left, int right){
		
			int fromIndex = index-left;
			int toIndex = index+right;
					
			if(fromIndex<0){
				fromIndex = 0;
			}
			if(toIndex>text.size()){
				toIndex = text.size();
			}
		return text.subList(fromIndex,toIndex);
	}
	

}
