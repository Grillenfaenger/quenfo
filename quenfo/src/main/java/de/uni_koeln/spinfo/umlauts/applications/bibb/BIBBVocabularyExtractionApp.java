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
		boolean extendContextsWithDewac = true;
		String externalVoc = "output//dewac//DewacVoc.txt";
		boolean filterByProportion = true;
		double filterMeasure = 1d;
		boolean filterNames = true;
		
		if(useDewacVocabulary){
			extendContextsWithDewac = true;
		}
		
		// /////////////////////////////////////////////
		// /////experiment parameters
		// /////////////////////////////////////////////
		
		boolean getFullSentences = true;
		int wordsBefore = 3;
		int wordsAfter = 3;
		
		// ///////////////////////////////////////////////
		// ////////END///
		// //////////////////////////////////////////////
		
		// inizialize
		UmlautExperimentConfiguration expConfig = new UmlautExperimentConfiguration(null,
				null, null, null, "umlauts/classification/output", getFullSentences, wordsBefore, wordsAfter);
		
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
		
		
		// for statistics: comparison between BiBB and sDewac Vocabulary
		vocBuilder.compareVocabulary();
	
		// extract contexts
		contexts = vocBuilder.getContexts();
		
		if(extendContextsWithDewac) contexts = vocBuilder.extendByDewacContexts(contexts);
		if(useDewacVocabulary) vocBuilder.cleanFiles();
		
		// print
		FileUtils.printMap(ambiguities, destPath, "bibbAmbiguities");
		contexts.printKeywordContexts(destPath, "bibbContexts");

	}

}
