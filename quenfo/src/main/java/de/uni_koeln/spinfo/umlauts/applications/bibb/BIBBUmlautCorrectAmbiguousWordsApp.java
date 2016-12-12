package de.uni_koeln.spinfo.umlauts.applications.bibb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.LogLikeliHoodFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.TFIDFFeatureQuantifier;
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

public class BIBBUmlautCorrectAmbiguousWordsApp {
	
	private static final Logger log = Logger.getLogger( BIBBUmlautCorrectAmbiguousWordsApp.class.getName() );
	
	public static void main(String[] args) throws ClassNotFoundException,
	IOException, SQLException {
		
		// logging
		Handler handler = new FileHandler( "output//log.txt" );
		log.addHandler(handler);
		
		try{
		
		// /////////////////////////////////////////////
		// run variables
		// /////////////////////////////////////////////
		
		String dbPath = "umlaute_db.db";	
		String intermediateDbPath = "inter_db.db";
		String correctedDbPath = "corrected_db.db";
		int excludeYear = 2012;
		
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
		Distance distance = Distance.COSINUS;
		ZoneAbstractClassifier classifier = new ZoneKNNClassifier(false, knnValue, distance); //new ZoneRocchioClassifier(false, distance);
		AbstractFeatureQuantifier quantifier = new  TFIDFFeatureQuantifier(); //new LogLikeliHoodFeatureQuantifier(); //new AbsoluteFrequencyFeatureQuantifier(); // //
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
		
		// Vocabulary Extraction variables
		Dictionary dict = new Dictionary();
		Map<String, HashSet<String>> ambiguities;
		KeywordContexts contexts = new KeywordContexts();
		Vocabulary voc = new Vocabulary();
		
		BibbVocabularyBuilder vocBuilder = new BibbVocabularyBuilder(dbPath, expConfig, excludeYear);
		
		// load Vocabulary from Files
		dict.loadDictionary("output//bibb//bibbDictionary.txt");
		ambiguities = FileUtils.fileToAmbiguities("output//bibb//bibbAmbiguities.txt");
		contexts = contexts.loadKeywordContextsFromFile("output//bibb//BibbContexts.txt");
		voc.loadVocabularyFromFile("output//bibb//BibbVocabulary.txt");
		
		vocBuilder.setAmbiguities(ambiguities);
		vocBuilder.setDict(dict);
		vocBuilder.setFullVoc(voc);
		vocBuilder.setContexts(contexts);
		
		Connection correctedDB = DBConnector.connect(correctedDbPath);
		Connection intermediateDB = DBConnector.connect(intermediateDbPath);
		DBConnector.createBIBBDBcorrected(correctedDB);
		
		// classifiy and correct ambiguous words
		ClassificationTools.correctAmbiguousWords2(vocBuilder, 2012, intermediateDB, correctedDB, expConfig, log);
		
		}catch (Exception e){
			log.log(Level.SEVERE, "FEHLER: ", e);
		}
	}
}
