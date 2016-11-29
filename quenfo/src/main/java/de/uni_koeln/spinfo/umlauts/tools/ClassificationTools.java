package de.uni_koeln.spinfo.umlauts.tools;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
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
import opennlp.tools.util.Span;

public class ClassificationTools {
	
	public static void correctAmbiguousWords(Map<String, HashSet<String>> ambiguities, KeywordContexts keywordContexts, int year, Connection input, Connection dbOut, UmlautExperimentConfiguration config) throws SQLException, IOException {
		IETokenizer tokenizer = new IETokenizer();
		Map<String, Model> models= new HashMap<String, Model>();
		Map<ClassifyUnit, boolean[]> allClassified = new HashMap<ClassifyUnit, boolean[]>();
		
		ZoneJobs jobs = new ZoneJobs();
		
		// Classification Units erstellen (diese sind dann schon initialisiert)
		for (Entry<String, HashSet<String>> entry : ambiguities.entrySet()) {
			System.out.println("build model for " + entry.getKey() + ", Varianten: " + entry.getValue());
			List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
			String[] senses = entry.getValue().toArray(new String[entry.getValue().size()]);
			HashSet<String> variants = entry.getValue();
			for(String string : variants){
				List<List<String>> contexts = keywordContexts.getContext(string);
//				System.out.println(contexts.size() + " Kontexte mit " + string);
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
		
		int correct = 0;
		int failure = 0;
		
		// Jede Anzeige durchgehen, evtl. klassifizieren und korrigieren.
		JobAd jobAd = null;
		while(resultSet.next()){
			jobAd = new JobAd(resultSet.getInt(3), resultSet.getInt(2), resultSet.getString(4), resultSet.getInt(1));
			StringBuffer buf = new StringBuffer();
			buf.append(jobAd.getContent());
			List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), config.getStoreFullSentences());
			
			for(Sentence s : tokenizedSentences){
			
				List<String> tokens2 = s.getTokens();
				for (int i = 0; i < tokens2.size(); i++) {
					String word = tokens2.get(i);
					if(ambiguities.containsKey(word)){
						// Kontext extrahieren
						List<String> context = extractContext(tokens2, i, config.getContextBefore(),config.getContextAfter());
						// cu erstellen
						List<ClassifyUnit> cus = new ArrayList<ClassifyUnit>();
						ZoneClassifyUnit zcu = new UmlautClassifyUnit(context, word, ambiguities.get(word).toArray(new String[ambiguities.get(word).size()]), true);
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
						
						System.out.print("CLASSIFICATION: Im Text: "+ word + " Klassifiziert: "+ wordAsClassified);
						// ist nur aussagekräftig für korrekte Korpora
						if(word.equals(wordAsClassified)){
							System.out.print(" CORRECT\n ");
							correct++;
						} else {
							System.out.print(" FALSE\n ");
							failure++;
						}
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
			jobAd.replaceContent(buf.toString());
			
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
		
		double ratio = (correct * 1d /(correct+failure))*100d;
		
		System.out.println("not changed/correct(in Test mode): " + correct + ", changed/failure (test Mode): " + failure + ", not changed ratio:" + ratio);
	}
	
	public static void correctAmbiguousWords2(Map<String, HashSet<String>> ambiguities, KeywordContexts keywordContexts, int year, Connection input, Connection dbOut, UmlautExperimentConfiguration config) throws SQLException, IOException {
		IETokenizer tokenizer = new IETokenizer();
		Map<String, Model> models= new HashMap<String, Model>();
		Map<ClassifyUnit, boolean[]> allClassified = new HashMap<ClassifyUnit, boolean[]>();
		
		ZoneJobs jobs = new ZoneJobs();
		
		
		
		
		dbOut.setAutoCommit(false);
		PreparedStatement outPStmt = dbOut.prepareStatement("INSERT INTO DL_ALL_Spinfo (OrigID,ZEILENNR, Jahrgang, STELLENBESCHREIBUNG, CORRECTED) VALUES(?,?,?,?,?)");
		
		// Verbindung zur Datenbank aufbauen und JobAds abrufen
		input.setAutoCommit(false);
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE(Jahrgang = '"+year+"') ";
		Statement stmt = input.createStatement();
		ResultSet resultSet = stmt.executeQuery(sql);
		
		int correct = 0;
		int failure = 0;
		
		// Jede Anzeige durchgehen, evtl. klassifizieren und korrigieren.
		JobAd jobAd = null;
		while(resultSet.next()){
			jobAd = new JobAd(resultSet.getInt(3), resultSet.getInt(2), resultSet.getString(4), resultSet.getInt(1));
			StringBuffer buf = new StringBuffer();
			buf.append(jobAd.getContent());
			List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), config.getStoreFullSentences());
			
			for(Sentence s : tokenizedSentences){
			
				List<String> tokens2 = s.getTokens();
				for (int i = 0; i < tokens2.size(); i++) {
					String word = tokens2.get(i);
					if(ambiguities.containsKey(word)){
						if(!models.containsKey(word)){
							// Classification Units erstellen (diese sind dann schon initialisiert)
							System.out.println("build model for " + word + ", Varianten: " + ambiguities.get(word));
							List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
							HashSet<String> variants = ambiguities.get(word);
							String[] senses = variants.toArray(new String[variants.size()]);
							
							for(String string : variants){
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
							Model model = jobs.getNewModelForClassifier(trainingData, config);
							models.put(word, model);
						}
						// Kontext extrahieren
						List<String> context = extractContext(tokens2, i, config.getContextBefore(),config.getContextAfter());
						// cu erstellen
						List<ClassifyUnit> cus = new ArrayList<ClassifyUnit>();
						ZoneClassifyUnit zcu = new UmlautClassifyUnit(context, word, ambiguities.get(word).toArray(new String[ambiguities.get(word).size()]), true);
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
						
						System.out.print("CLASSIFICATION: Im Text: "+ word + " Klassifiziert: "+ wordAsClassified);
						// ist nur aussagekräftig für korrekte Korpora
						if(word.equals(wordAsClassified)){
							System.out.print(" CORRECT\n ");
							correct++;
						} else {
							System.out.print(" FALSE\n ");
							failure++;
						}
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
			jobAd.replaceContent(buf.toString());
			
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
		
		double ratio = (correct * 1d /(correct+failure))*100d;
		
		System.out.println("not changed/correct(in Test mode): " + correct + ", changed/failure (test Mode): " + failure + ", not changed ratio:" + ratio);
	}
	
	public static List<Integer> correctAmbiguousWordsEval(Map<String, HashSet<String>> ambiguities, KeywordContexts keywordContexts, int year, Connection input, Connection dbOut, UmlautExperimentConfiguration config) throws SQLException, IOException {
		
		List<Integer> eval = new ArrayList<Integer>();
		
		IETokenizer tokenizer = new IETokenizer();
		Map<String, Model> models= new HashMap<String, Model>();
		Map<ClassifyUnit, boolean[]> allClassified = new HashMap<ClassifyUnit, boolean[]>();
		
		ZoneJobs jobs = new ZoneJobs();
		
		Set<String> ambiguitySet = createAmbiguitySet(ambiguities);
		
		dbOut.setAutoCommit(false);
		PreparedStatement outPStmt = dbOut.prepareStatement("INSERT INTO DL_ALL_Spinfo (OrigID,ZEILENNR, Jahrgang, STELLENBESCHREIBUNG, CORRECTED) VALUES(?,?,?,?,?)");
		
		// Verbindung zur Datenbank aufbauen und JobAds abrufen
		input.setAutoCommit(false);
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE(Jahrgang = '"+year+"') ";
		Statement stmt = input.createStatement();
		ResultSet resultSet = stmt.executeQuery(sql);
		
		int correct = 0;
		int failure = 0;
		
		// Jede Anzeige durchgehen, evtl. klassifizieren und korrigieren.
		JobAd jobAd = null;
		while(resultSet.next()){
			jobAd = new JobAd(resultSet.getInt(3), resultSet.getInt(2), resultSet.getString(4), resultSet.getInt(1));
			StringBuffer buf = new StringBuffer();
			buf.append(jobAd.getContent());
			List<Sentence> tokenizedSentences = tokenizer.tokenizeWithPositions(jobAd.getContent(), config.getStoreFullSentences());
			
			for(Sentence s : tokenizedSentences){
			
				List<String> tokens2 = s.getTokens();
				for (int i = 0; i < tokens2.size(); i++) {
					String word = tokens2.get(i);
					if(ambiguitySet.contains(word)){
						String umlWord = word;
						word = replaceUmlaut(word);
						if(word.equals("Fon")){
							System.out.println(word);
						}
						if(!models.containsKey(word)){
							// Classification Units erstellen (diese sind dann schon initialisiert)
							System.out.println("build model for " + word + ", Varianten: " + ambiguities.get(word));
							List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
							HashSet<String> variants = ambiguities.get(word);
							String[] senses = variants.toArray(new String[variants.size()]);
							
							for(String string : variants){
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
							Model model = jobs.getNewModelForClassifier(trainingData, config);
							models.put(word, model);
						}
						// Kontext extrahieren
						List<String> context = extractContext(tokens2, i, config.getContextBefore(),config.getContextAfter());
						// cu erstellen
						List<ClassifyUnit> cus = new ArrayList<ClassifyUnit>();
						ZoneClassifyUnit zcu = new UmlautClassifyUnit(context, word, ambiguities.get(word).toArray(new String[ambiguities.get(word).size()]), true);
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
						
						System.out.print("CLASSIFICATION: Im Text: "+ word + " Klassifiziert: "+ wordAsClassified);
						// ist nur aussagekräftig für korrekte Korpora
						if(word.equals(wordAsClassified)){
							System.out.print(" CORRECT\n ");
							correct++;
						} else {
							System.out.print(" FALSE\n ");
							failure++;
						}
						// Falls hier ein Umlaut rekonstruiert werden muss 
						if(!wordAsClassified.equals(umlWord)){
							// Korrektur
							Span span = s.getAbsoluteSpanOfToken(i);
							int start = span.getStart();
						    int end = span.getEnd();
						    buf.replace(start, end, wordAsClassified);
						      
						}		
					}
				}
			}
			jobAd.replaceContent(buf.toString());
			
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
		
		double ratio = (correct * 1d /(correct+failure))*100d;
		System.out.println("not changed/correct(in Test mode): " + correct + ", changed/failure (test Mode): " + failure + ", not changed ratio:" + ratio);
		eval.add(correct+failure);
		eval.add(correct);
		eval.add(failure);
		return eval;
	}

	public static void correctUnabiguousWords(Dictionary dict, Vocabulary voc, int year, String dbIn, Connection dbOut) throws ClassNotFoundException, SQLException {
		IETokenizer tokenizer = new IETokenizer();
		
		int corrections = 0;
		int correct = 0;
		
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
				
				// extract unambiguous tokens
				simpleTokens.keySet().retainAll(dict.dictionary.keySet());
				// and correct them
				for(Entry<String, List<Span>> occurence : simpleTokens.entrySet()){
					for(Span span : occurence.getValue()){
						
					        int start = span.getStart();
					        int end = span.getEnd();
					        String replacement = dict.dictionary.get(occurence.getKey());
					        System.out.println(occurence.getKey() + ","+ voc.vocabulary.get(occurence.getKey()) +": " + dict.dictionary.get(occurence.getKey()) + ", " + voc.vocabulary.get(replacement));
					        
					        buf.replace(start, end, replacement);
					        corrections++;
					        System.out.println(occurence.getKey() + " wurde durch " + replacement + " ersetzt.");
					    
					        
					}
				}
			}
			
			// write corrected JobAd to db
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
		
		System.out.println("Corrections: " + corrections);
	}
	
	public static List<Integer> correctUnabiguousWordsEval(Dictionary dict, Vocabulary voc, int year, String dbIn, Connection dbOut) throws ClassNotFoundException, SQLException {
		
		List<Integer> eval = new ArrayList<Integer>();
		
		IETokenizer tokenizer = new IETokenizer();
		
		int corrections = 0;
		int total = 0;
		int totaltokens = 0;
		Set<String> dictSet = createDictSet(dict);
		
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
				totaltokens = totaltokens + s.getTokens().size();
				Map<String,List<Span>> simpleTokens = s.getTokenPos();
				
				for(String token : simpleTokens.keySet()){
					if(dictSet.contains(token)){
						total++;
					}
				}
				
				// extract unambiguous tokens
				simpleTokens.keySet().retainAll(dict.dictionary.keySet());
				// and correct them
				for(Entry<String, List<Span>> occurence : simpleTokens.entrySet()){
					for(Span span : occurence.getValue()){
						
					        int start = span.getStart();
					        int end = span.getEnd();
					        String replacement = dict.dictionary.get(occurence.getKey());
					        System.out.println(occurence.getKey() + ","+ voc.vocabulary.get(occurence.getKey()) +": " + dict.dictionary.get(occurence.getKey()) + ", " + voc.vocabulary.get(replacement));
					        
					        buf.replace(start, end, replacement);
					        corrections++;
					        System.out.println(occurence.getKey() + " wurde durch " + replacement + " ersetzt.");
					    
					        
					}
				}
			}
			
			// write corrected JobAd to db
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
		
		
		System.out.println("Corrections: " + corrections + " of " + total);
		int correct = total-corrections;
		int failure = corrections;
		eval.add(total);
		eval.add(correct);
		eval.add(failure);
		eval.add(totaltokens);
		return eval;
	}
	
	private static Set<String> createDictSet(Dictionary dict) {
		Set<String> dictSet = new HashSet<String>();
		
		for(String key : dict.dictionary.keySet()){
			dictSet.add(key);
			dictSet.add(dict.get(key));
		}
		return dictSet;
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
	
	public static Set<String> createAmbiguitySet(Map<String, HashSet<String>> ambiguities) {
		Set<String> ambiguitySet = new HashSet<String>();
		
		for (Set<String> ambigueSet : ambiguities.values()) {
			ambiguitySet.addAll(ambigueSet);
		}
		return ambiguitySet;
	}

	public static String replaceUmlaut(String umlautWord) {
		String replacement = umlautWord;
		replacement = replacement.replaceAll("Ä", "A");
		replacement = replacement.replaceAll("ä", "a");
		replacement = replacement.replaceAll("Ö", "O");
		replacement = replacement.replaceAll("ö", "o");
		replacement = replacement.replaceAll("Ü", "U");
		replacement = replacement.replaceAll("ü", "u");
		replacement = replacement.replaceAll("ß", "ss");
		
		return replacement;
	}
}
