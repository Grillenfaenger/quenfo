package de.uni_koeln.spinfo.umlauts.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import opennlp.tools.util.Span;
import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneAbstractClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneKNNClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.umlauts.classification.UmlautClassifyUnit;
import de.uni_koeln.spinfo.umlauts.data.Dictionary;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.Sentence;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class BIBBdbApp {
	
//		private static String dbPath = "umlaute_db.db";	
//		private static String intermediateDbPath = "inter_db.db";
//		private static String correctedDbPath = "corrected_db.db";
//		private static Vocabulary fullVoc;
//		private static Map<String, HashSet<String>> ambiguities;
//		private static Dictionary dict;
//		private static KeywordContexts contexts;
//		
//		private static UmlautExperimentConfiguration expConfig;
//		private static int excludeYear = 2012;
		
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException{
		
		String dbPath = "umlaute_db.db";	
		String intermediateDbPath = "inter_db.db";
		String correctedDbPath = "corrected_db.db";
		UmlautExperimentConfiguration expConfig;
		int excludeYear = 2012;
		
		Dictionary dict;
		Map<String, HashSet<String>> ambiguities;
		KeywordContexts contexts;
		
		// /////////////////////////////////////////////
		// /////experiment parameters
		// /////////////////////////////////////////////
		
		
		boolean preClassify = false;
		File outputFolder = new File("umlauts/classification/output/singleResults/preClassified");
		int knnValue = 3;
		boolean ignoreStopwords = false;
		boolean normalizeInput = false;
		boolean useStemmer = false;
		boolean suffixTrees = false;
		int[] nGrams = null; //new int[]{3,4};
		int miScoredFeaturesPerClass = 0;
		Distance distance = Distance.EUKLID;
		ZoneAbstractClassifier classifier = new ZoneKNNClassifier(false, knnValue, distance);//new ZoneRocchioClassifier(false, distance);//new ZoneKNNClassifier(false, knnValue, distance);
		AbstractFeatureQuantifier quantifier = new AbsoluteFrequencyFeatureQuantifier();//new  TFIDFFeatureQuantifier();
		boolean getFullSentences = true;
		int wordsBefore = 3;
		int wordsAfter = 3;
		
		// ///////////////////////////////////////////////
		// ////////END///
		// //////////////////////////////////////////////
		
		FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(
				normalizeInput, useStemmer, ignoreStopwords, nGrams, false,
				miScoredFeaturesPerClass, suffixTrees);
		expConfig = new UmlautExperimentConfiguration(fuc,
				quantifier, classifier, null, "umlauts/classification/output", getFullSentences, wordsBefore, wordsAfter);
		
		
		BibbVocabularyBuilder vocBuilder = new BibbVocabularyBuilder(dbPath, expConfig, excludeYear);
		
		// extract full Vocabulary and build Dictionary
		dict = vocBuilder.buildDictionary();
		
		System.out.println("das: " + dict.dictionary.get("das"));
		System.out.println("und: " + dict.dictionary.get("und"));
		System.out.println("an: " + dict.dictionary.get("an"));
		
		// find ambiguities
		ambiguities = vocBuilder.findAmbiguities();
		
		dict.printToFile("output//bibb//", "bibbDictionary");
		
		// for statistics: comparision between BiBB and sDewac Vocabulary
		boolean extendDictionary = true;
		boolean extendAmbiguities = false;
		vocBuilder.compareVocabulary(extendDictionary, extendAmbiguities);
		
		System.out.println("das: " + dict.dictionary.get("das"));
		System.out.println("und: " + dict.dictionary.get("und"));
		System.out.println("an: " + dict.dictionary.get("an"));
		
		// extract contexts
		boolean getContextsFromDewac = true;
		contexts = vocBuilder.getContexts(getContextsFromDewac);
		
		// create outputDB
		Connection intermediateDB = DBConnector.connect(intermediateDbPath);
		DBConnector.createBIBBDBcorrected(intermediateDB);
		
		// correct unambiguous words
		correctUnabiguousWords(dict,2012, dbPath,intermediateDB);
		
		Connection correctedDB = DBConnector.connect(correctedDbPath);
		intermediateDB = DBConnector.connect(intermediateDbPath);
		DBConnector.createBIBBDBcorrected(correctedDB);
		
		// classifiy and correct ambiguous words
		correctAmbiguousWords(ambiguities, contexts, 2012, intermediateDB, correctedDB, expConfig);
	}

	private static void correctAmbiguousWords(Map<String, HashSet<String>> ambiguities, KeywordContexts keywordContexts, int year, Connection input, Connection dbOut, UmlautExperimentConfiguration config) throws SQLException, IOException {
		IETokenizer tokenizer = new IETokenizer();
		Map<String, Model> models= new HashMap<String, Model>();
		Map<ClassifyUnit, boolean[]> allClassified = new HashMap<ClassifyUnit, boolean[]>();
		
		ZoneJobs jobs = new ZoneJobs();
		
		// Classification Units erstellen (diese sind dann schon initialisiert)
		for (Entry<String, HashSet<String>> entry : ambiguities.entrySet()) {
			List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
			String[] senses = entry.getValue().toArray(new String[entry.getValue().size()]);
			HashSet<String> variants = entry.getValue();
			for(String string : variants){
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
		
		dbOut.setAutoCommit(false);
		PreparedStatement outPStmt = dbOut.prepareStatement("INSERT INTO DL_ALL_Spinfo (OrigID,ZEILENNR, Jahrgang, STELLENBESCHREIBUNG, CORRECTED) VALUES(?,?,?,?,?)");
		
		// Verbindung zur Datenbank aufbauen und JobAds abrufen
		input.setAutoCommit(false);
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE(Jahrgang = '"+year+"') ";
		Statement stmt = input.createStatement();
		ResultSet resultSet = stmt.executeQuery(sql);
		
		// Jede Anzeige durchgehen, evtl. klassifizieren und korrigieren.
		JobAd jobAd = null;
		while(resultSet.next()){
			jobAd = new JobAd(resultSet.getInt(3), resultSet.getInt(2), resultSet.getString(4), resultSet.getInt(1));
			StringBuffer buf = new StringBuffer();
			buf.append(jobAd.getContent());
			List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), false);
			
			for(Sentence s : tokenizedSentences){
			
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
			outPStmt.setInt(1, jobAd.getId());
			outPStmt.setInt(2, jobAd.getZeilennummer());
			outPStmt.setInt(3, jobAd.getJahrgang());
			outPStmt.setString(4, jobAd.getContent());
			outPStmt.setString(5, buf.toString());
			outPStmt.executeUpdate();
			
		}
		outPStmt.close();
		dbOut.commit();
		dbOut.close();
	}

	private static void correctUnabiguousWords(Dictionary dict, int year, String dbIn, Connection dbOut) throws ClassNotFoundException, SQLException {
		IETokenizer tokenizer = new IETokenizer();
		
		 System.out.println("das: " + dict.dictionary.get("das"));
		
		Connection inputConnection = DBConnector.connect(dbIn);
		inputConnection.setAutoCommit(false);
		
		dbOut.setAutoCommit(false);
		PreparedStatement outPStmt = dbOut.prepareStatement("INSERT INTO DL_ALL_Spinfo (OrigID,ZEILENNR, Jahrgang, STELLENBESCHREIBUNG, CORRECTED) VALUES(?,?,?,?,?)");
		
		
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE(Jahrgang = '"+year+"') ";
		Statement stmt = inputConnection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		
		
		
		JobAd jobAd = null;
		while(result.next()){
			jobAd = new JobAd(result.getInt(3), result.getInt(2), result.getString(4), result.getInt(1));
			StringBuffer buf = new StringBuffer();
			buf.append(jobAd.getContent());
			List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), false);
			for(Sentence s : tokenizedSentences){
				Map<String,List<Span>> simpleTokens = s.getTokenPos();
				
				// Eindeutige Token erkennen 
				simpleTokens.keySet().retainAll(dict.dictionary.keySet());
				//und korrigieren
				for(Entry<String, List<Span>> occurence : simpleTokens.entrySet()){
					for(Span span : occurence.getValue()){
						
					        int start = span.getStart();
					        int end = span.getEnd();
					        String replacement = dict.dictionary.get(occurence.getKey());
					        System.out.println(occurence.getKey() + ": " + dict.dictionary.get(occurence.getKey()));
					        
					        buf.replace(start, end, replacement);
					        System.out.println(occurence.getKey() + " wurde durch " + replacement + " ersetzt.");
					    
					        
					}
				}
			}
			//jobAd.replaceContent(buf.toString());
			// korrigierte Stellenanzeige in neue Datenbank schreiben
			outPStmt.setInt(1, jobAd.getId());
			outPStmt.setInt(2, jobAd.getZeilennummer());
			outPStmt.setInt(3, jobAd.getJahrgang());
			outPStmt.setString(4, jobAd.getContent());
			outPStmt.setString(5, buf.toString());
			outPStmt.executeUpdate();
			
		}
		outPStmt.close();
		dbOut.commit();
		dbOut.close();
		
		
	}
	
	private static List<String> extractContext(List<String> text, int index, int left, int right){
		
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
