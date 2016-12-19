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

public class BIBBdbApp {
	
	private static final Logger log = Logger.getLogger( BIBBdbApp.class.getName() );
		
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException{
		
	// logging
		Handler handler = new FileHandler( "output//log.txt" );
		handler.setLevel(Level.INFO);
		log.addHandler(handler);
		log.setLevel(Level.INFO);
		
	try {	
		
		// /////////////////////////////////////////////
		// run variables
		// /////////////////////////////////////////////
		
		String dbPath = "D:/Daten/sqlite/SteA.db3";	
		String intermediateDbPath = "output//production//inter_db.db";
		String correctedDbPath = "output//production//corrected_db.db";
		int excludeYear = 2012;
		String destPath = "output//production//";
		
		// /////////////////////////////////////////////
		// extraction parameters
		// /////////////////////////////////////////////
		
		boolean useDewacVocabulary = false;
		String externalVoc = "output//dewac//DewacVoc.txt";
		boolean filterByProportion = true;
		double filterMeasure = 1d;
		boolean filterNames = false;
		boolean extendContextsWithDewac = false;
		
		if(useDewacVocabulary){
			extendContextsWithDewac = true;
		}
		
		// /////////////////////////////////////////////
		// /////classification parameters
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
		
		// Vocabulary Extraction varibles
		Dictionary dict = new Dictionary();
		Map<String, HashSet<String>> ambiguities;
		KeywordContexts contexts = new KeywordContexts();
		Vocabulary voc = new Vocabulary();
		
		BibbVocabularyBuilder vocBuilder = new BibbVocabularyBuilder(dbPath, expConfig, excludeYear);
		
		
		// extract full Vocabulary and build Dictionary
	
		dict = vocBuilder.buildDictionary(useDewacVocabulary, externalVoc, log);
		
		// find ambiguities and invert Dictionary afterwards
		ambiguities = vocBuilder.findAmbiguities(filterByProportion, filterMeasure, filterNames);
		
		FileUtils.printMap(ambiguities, "output//bibb//", "AmbiguitiesZwischenstand");
		
		// for statistics: comparision between BiBB and sDewac Vocabulary
		vocBuilder.compareVocabulary();
		voc = vocBuilder.fullVoc;
	
		// extract contexts
		contexts = vocBuilder.getContexts();
		
		// save to files
		voc.saveVocabularyToFile(destPath, "bibbVocabulary");
		dict.printToFile(destPath, "bibbDictionary");
		FileUtils.printMap(ambiguities, destPath, "bibbAmbiguities");
		contexts.printKeywordContexts(destPath, "bibbContexts");
		
		if(extendContextsWithDewac) contexts = vocBuilder.extendByDewacContexts(contexts);
		if(useDewacVocabulary) vocBuilder.cleanFiles();
		
		// create outputDB
		Connection intermediateDB = DBConnector.connect(intermediateDbPath);
		DBConnector.createBIBBDBcorrected(intermediateDB);
		
		// correct unambiguous words
		ClassificationTools.correctUnabiguousWords(vocBuilder, 2012, dbPath,intermediateDB, log);
		
		Connection correctedDB = DBConnector.connect(correctedDbPath);
		intermediateDB = DBConnector.connect(intermediateDbPath);
		DBConnector.createBIBBDBcorrected(correctedDB);
		
		// classifiy and correct ambiguous words
		ClassificationTools.correctAmbiguousWords2(vocBuilder, 2012, intermediateDB, correctedDB, expConfig, log);
		
	}catch (Throwable e){
		log.log(Level.SEVERE, "FEHLER: ", e);
	}
	}
	
}
