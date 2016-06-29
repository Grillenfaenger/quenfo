package de.uni_koeln.spinfo.umlauts.applications;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.TFIDFFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneAbstractClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneKNNClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneRocchioClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.umlauts.classification.ConfigurableUmlautClassifier;

public class UmlautClassificationApp {
	
	public static void main(String[] args) throws ClassNotFoundException,
	IOException, SQLException {
		
		// /////////////////////////////////////////////
		// /////experiment parameters
		// /////////////////////////////////////////////
		
		
		boolean preClassify = false;
		File outputFolder = new File("umlauts/classification/output/singleResults/preClassified");
		//int knnValue = 3;
		boolean ignoreStopwords = false;
		boolean normalizeInput = false;
		boolean useStemmer = false;
		boolean suffixTrees = false;
		int[] nGrams = null; //new int[]{3,4};
		int miScoredFeaturesPerClass = 0;
		Distance distance = Distance.COSINUS;
		ZoneAbstractClassifier classifier = new ZoneRocchioClassifier(false, distance);//new ZoneKNNClassifier(false, knnValue, distance);
		AbstractFeatureQuantifier quantifier = new AbsoluteFrequencyFeatureQuantifier();//new  TFIDFFeatureQuantifier();
		
		// ///////////////////////////////////////////////
		// ////////END///
		// //////////////////////////////////////////////
		
		FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(
				normalizeInput, useStemmer, ignoreStopwords, nGrams, false,
				miScoredFeaturesPerClass, suffixTrees);
		ExperimentConfiguration expConfig = new ExperimentConfiguration(fuc,
				quantifier, classifier, null, "umlauts/classification/output");
		
		ConfigurableUmlautClassifier umlautClassify = new ConfigurableUmlautClassifier();
		umlautClassify.classify(expConfig);
	}

}
