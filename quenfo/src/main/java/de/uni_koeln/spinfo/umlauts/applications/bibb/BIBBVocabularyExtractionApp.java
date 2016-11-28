package de.uni_koeln.spinfo.umlauts.applications.bibb;

import java.io.IOException;
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
import de.uni_koeln.spinfo.umlauts.tools.BibbVocabularyBuilder;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class BIBBVocabularyExtractionApp {

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		
		// /////////////////////////////////////////////
		// run variables
		// /////////////////////////////////////////////
		
		String dbPath = "umlaute_db.db";	
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
		
		// extract full Vocabulary and build Dictionary
		dict = vocBuilder.buildDictionary(useDewacVocabulary, externalVoc);
		
		// print
		voc = vocBuilder.fullVoc;
		voc.saveVocabularyToFile(destPath, "bibbVocabulary");
		
		// find ambiguities
		ambiguities = vocBuilder.findAmbiguities(filterByProportion, filterMeasure, filterNames);
		
		// print
		dict = vocBuilder.dict;
		dict.printToFile(destPath, "bibbDictionary");
		FileUtils.printMap(ambiguities, destPath, "bibbAmbiguities");
		
		// for statistics: comparison between BiBB and sDewac Vocabulary
		vocBuilder.compareVocabulary();
	
		// extract contexts
		contexts = vocBuilder.getContexts();
		
		if(extendContextsWithDewac){
			// fill up Contexts with Dewac Contexts
			contexts = vocBuilder.extendByDewacContexts(contexts);
		}
		
		// print
		contexts.printKeywordContexts(destPath, "bibbContexts");

	}

}
