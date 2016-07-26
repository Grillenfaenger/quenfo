package de.uni_koeln.spinfo.umlauts.classification;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import opennlp.tools.util.Span;
import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.CategoryResult;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ExperimentResult;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ExperimentSetupUI;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.dbIO.DbConnector;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.umlauts.classification.UmlautClassifyUnit;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.Sentence;
import de.uni_koeln.spinfo.umlauts.data.TranslationVocabulary;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class ConfigurableUmlautClassifier {
	
	static String inputDbPath = "umlaute_db.db";
	static String outputDbPath = "umlauteCORRECTED_db.db";
	
//	public void classify(){
//		// get ExperimentConfiguration
//		ExperimentSetupUI ui = new ExperimentSetupUI();
//		ExperimentConfiguration expConfig = ui.getExperimentConfiguration(trainingDataFileName);
//		classify(expConfig);
//	}
	
	
	public void classify(UmlautExperimentConfiguration config) throws ClassNotFoundException, SQLException, IOException {

		// TODO: später sollen die hier geholten Daten erst einmal persistiert werden, Trainieren erfolgt dann in einer eigenen Methode
		// Trainieren
		Map<String, TreeSet<String>> ambiguities = null;
		Map<String, Model> models= new HashMap<String, Model>();
		
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
		System.out.println("Wörterbuch erstellt");

		// Suche nach Ambiguitäten
		/*TEST*/String test = "Farb-";
		
		ambiguities = transVoc.findAmbiguities(voc);
		/*TEST*/System.out.println(ambiguities.get(test));
		FileUtils.printMap(ambiguities, "output//classification//", "ambigeWörter");
		System.out.println(ambiguities.size() + " Gruppen mehrdeutiger Wörter gefunden");
		
		// Kontexte der ambigen Wörter holen und ausgeben
		System.out.println("Kontexte suchen...");
		keywordContexts = DBConnector.getKeywordContexts_new(jobAds, transVoc.createAmbiguitySet(ambiguities), config);
		keywordContexts.printKeywordContexts("output//classification//", "Kontexte_new");
		
	
		System.out.println("training");
	//Für jede Lesartengruppe Trainingsmodelle erstellen
		
		ZoneJobs jobs = new ZoneJobs();
		
		// Classification Units erstellen (diese sind dann schon initialisiert)
		for (Entry<String, TreeSet<String>> entry : ambiguities.entrySet()) {
			List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
			String[] senses = entry.getValue().toArray(new String[entry.getValue().size()]);
			for(String string : entry.getValue()){
				System.out.println("build model for " + entry.getKey());
				System.out.println(entry.getValue());
				List<List<String>> contexts = keywordContexts.getContext(string);
				System.out.println(contexts.size() + " Kontexte mit " + string);
				for (List<String> context : contexts){
					
					ZoneClassifyUnit cu = new UmlautClassifyUnit(context, string, senses, true);
					trainingData.add(cu);
				}
				
			}
			trainingData = jobs.setFeatures(trainingData, config.getFeatureConfiguration(), true);
			trainingData = jobs.setFeatureVectors(trainingData, config.getFeatureQuantifier(), null);

			// build model for each group
			System.out.println("build models");
			Model model = jobs.getNewModelForClassifier(trainingData, config);
			models.put(entry.getKey(), model);
		}
			
		
	// Modelle den Gruppen zugeordnet vorhalten
	
	
	
	//Klassifizieren
	
	// Im Jahrgang ohne Umlaute nach umlautambigen Wörtern suchen
		
	Map<ClassifyUnit, boolean[]> allClassified = new HashMap<ClassifyUnit, boolean[]>();
	
	jobAds = DBConnector.getJobAds(connection, 2012);
	System.out.println(jobAds.size() +  " zu korrigierende Anzeigen");
	StringBuffer buf = new StringBuffer();
	
	System.out.println("Umlaute rekonstruieren");
	
	// Je eine Anzeige
	for (JobAd jobAd : jobAds){
		buf.append(jobAd.getContent());
		// In Sätze splitten und deren Span festhalten
		// Sätze tokenisieren und die Position der Tokens im Satz festhalten
		List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), false);
		
		
		Map<String,String> simpleReplacements = transVoc.createSimpleReplacementMap(ambiguities.keySet());
		for(Sentence s : tokenizedSentences){
			Map<String,List<Span>> simpleTokens = s.getTokenPos();
			
			// Eindeutige Token erkennen 
			simpleTokens.keySet().retainAll(simpleReplacements.keySet());
			
//			System.out.println("Es müssen " + simpleTokens.size() + " Vorkommen korrigiert werden.");
			
			//und korrigieren
			for(Entry<String, List<Span>> occurence : simpleTokens.entrySet()){
				for(Span span : occurence.getValue()){
					
				        int start = span.getStart();
				        int end = span.getEnd();
				        String replacement = simpleReplacements.get(occurence.getKey());
				        
				        buf.replace(start, end, replacement);
				        System.out.println(occurence.getKey() + " wurde durch " + replacement + " ersetzt.");
				    
				        
				}
			}
			
			
			// mehrdeutige Vorkommen erkennen
			
			// TODO leider funktioniert dies hier nicht
//			Map<String,List<Span>> ambiguousTokens = s.getTokenPos();
//			ambiguousTokens.keySet().retainAll(ambiguities.keySet());
			
			// statt dessen:
			
			List<String> tokens2 = s.getTokens();
			for (int i = 0; i < tokens2.size(); i++) {
				String word = tokens2.get(i);
				if(ambiguities.containsKey(word)){
					// Kontext extrahieren
					List<String> context = extractContext(tokens2, i, 2,2);
					// cu erstellen
					List<ClassifyUnit> cus = new ArrayList<ClassifyUnit>();
					ZoneClassifyUnit zcu = new UmlautClassifyUnit(context, word, ambiguities.get(word).toArray(new String[0]), true);
					cus.add(zcu);
					cus = jobs.setFeatures(cus, config.getFeatureConfiguration(), false);
					cus = jobs.setFeatureVectors(cus, config.getFeatureQuantifier(), models.get(word).getFUOrder());
					
					// classify
					Map<ClassifyUnit, boolean[]> classified = jobs.classify(cus, config, models.get(word));
					
					for (Entry<ClassifyUnit,boolean[]> classiEntry : classified.entrySet()) {
						allClassified.put(classiEntry.getKey(), classiEntry.getValue());
					}
					
					
					List<ClassifyUnit> cuList = new ArrayList<ClassifyUnit>(classified.keySet());
					UmlautClassifyUnit result = (UmlautClassifyUnit) cuList.get(0);
					String wordAsClassified = result.getSense(classified.get(result));
					System.out.println("CLASSIFICATION: Im Text: "+ word + " Klassifiziert: "+ wordAsClassified);
					
					// Falls hier ein Umlaut rekonstruiert werden muss 
					if(!wordAsClassified.equals(word)){
						// Korrektur
						Span span = s.getAbsoluteSpanOfToken(i);
						int start = span.getStart();
					    int end = span.getEnd();
					    buf.replace(start, end, wordAsClassified);
					      
					}		
				}
			}
		}
		String corrected = buf.toString();
		jobAd.replaceContent(corrected);
		
	}
	
	// evaluate
	List<Integer> evaluationCategories = new ArrayList<Integer>();
	evaluationCategories.add(1);
	evaluationCategories.add(2);
	System.out.println("Es wurden " + allClassified.size() + " Wörter klassifiziert");
	ExperimentResult result = jobs.evaluate(allClassified, evaluationCategories, config);
	
	System.out.println("F Measure: \t" + result.getF1Measure());
	System.out.println("Precision: \t" + result.getPrecision());
	System.out.println("Recall: \t" + result.getRecall());
	System.out.println("Accuracy: \t" + result.getAccuracy());
	
	System.out.println("TP: \t" + result.getTP());
	System.out.println("TN: \t" + result.getTN());
	System.out.println("FP: \t" + result.getFP());
	System.out.println("FN: \t" + result.getFN());
	
	System.out.println(result.getCategoryEvaluations());
	
	List<CategoryResult> categoryEvaluations = result.getCategoryEvaluations();
	
	
//	// In neue Datenbank schreiben - egal ob eine Änderung vorgenommen wurde oder nicht.
//	// Dies muss für eine kleine Evaluation nicht gemacht werden.
//			Connection outputConnection = DbConnector.connect(outputDbPath);
//			DbConnector.createBIBBDB(outputConnection);
//			DbConnector.insertJobAdsInBIBBDB_Umlauts(outputConnection, jobAds);
	
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
