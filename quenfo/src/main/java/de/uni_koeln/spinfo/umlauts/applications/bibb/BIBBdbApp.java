package de.uni_koeln.spinfo.umlauts.applications.bibb;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneAbstractClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneKNNClassifier;
import de.uni_koeln.spinfo.umlauts.data.Dictionary;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.tools.BibbVocabularyBuilder;
import de.uni_koeln.spinfo.umlauts.tools.ClassificationTools;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class BIBBdbApp {
		
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException{
		
		// /////////////////////////////////////////////
		// run mode
		// /////////////////////////////////////////////
		
		boolean loadFromFiles = false;
		
		// run variables
		String dbPath = "umlaute_db.db";	
		String intermediateDbPath = "inter_db.db";
		String correctedDbPath = "corrected_db.db";
		int excludeYear = 2012;
		String destPath = "output//bibb//";
		
		// /////////////////////////////////////////////
		// extraction parameters
		// /////////////////////////////////////////////
		
		boolean useDewacVocabulary = true;
		String externalVoc = "output//dewac//DewacVoc.txt";
		boolean filterByProportion = true;
		double filterMeasure = 1d;
		boolean filterNames = true;
		boolean extendContextsWithDewac = true;
		
		// /////////////////////////////////////////////
		// /////experiment parameters
		// /////////////////////////////////////////////
		
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
		
		// inizialize
		FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(
				normalizeInput, useStemmer, ignoreStopwords, nGrams, false,
				miScoredFeaturesPerClass, suffixTrees);
		UmlautExperimentConfiguration expConfig = new UmlautExperimentConfiguration(fuc,
				quantifier, classifier, null, "umlauts/classification/output", getFullSentences, wordsBefore, wordsAfter);
		
		// Vocabulary Extraction varibles
		Dictionary dict = new Dictionary();
		Map<String, HashSet<String>> ambiguities;
		KeywordContexts contexts = new KeywordContexts();
		Vocabulary voc = new Vocabulary();
		
		BibbVocabularyBuilder vocBuilder = new BibbVocabularyBuilder(dbPath, expConfig, excludeYear);
		
		if(loadFromFiles == false){
			// extract full Vocabulary and build Dictionary
		
			dict = vocBuilder.buildDictionary(useDewacVocabulary, externalVoc);
			
			// find ambiguities
			ambiguities = vocBuilder.findAmbiguities(filterByProportion, filterMeasure, filterNames);
			dict = vocBuilder.dict;
			
			FileUtils.printMap(ambiguities, "output//bibb//", "AmbiguitiesZwischenstand");
			
			dict.printToFile("output//bibb//", "bibbDictionary");
			
			// for statistics: comparision between BiBB and sDewac Vocabulary
			vocBuilder.compareVocabulary();
			voc = vocBuilder.fullVoc;
		
			// extract contexts
			contexts = vocBuilder.getContexts();
			
			// save to files
			voc.saveVocabularyToFile(destPath, "bibbVocabulary");
			dict = vocBuilder.dict;
			dict.printToFile(destPath, "bibbDictionary");
			FileUtils.printMap(ambiguities, destPath, "bibbAmbiguities");
			contexts.printKeywordContexts(destPath, "bibbContexts");
			
		} else {
			
			dict.loadDictionary("output//bibb//bibbDictionary.txt");
			ambiguities = FileUtils.fileToAmbiguities("output//bibb//bibbAmbiguities.txt");
			contexts = contexts.loadKeywordContextsFromFile("output//bibb//BibbContexts.txt");
			voc.loadVocabularyFromFile("output//bibb//BibbVocabulary.txt");
		}
		
		if(extendContextsWithDewac){
			// fill up Contexts with Dewac Contexts
			contexts = vocBuilder.extendByDewacContexts(contexts);
		}
		
		// create outputDB
		Connection intermediateDB = DBConnector.connect(intermediateDbPath);
		DBConnector.createBIBBDBcorrected(intermediateDB);
		
		// correct unambiguous words
		ClassificationTools.correctUnabiguousWords(dict, voc, 2012, dbPath,intermediateDB);
		
		Connection correctedDB = DBConnector.connect(correctedDbPath);
		intermediateDB = DBConnector.connect(intermediateDbPath);
		DBConnector.createBIBBDBcorrected(correctedDB);
		
		// classifiy and correct ambiguous words
		ClassificationTools.correctAmbiguousWords(ambiguities, contexts, 2012, intermediateDB, correctedDB, expConfig);
	}
	
}
