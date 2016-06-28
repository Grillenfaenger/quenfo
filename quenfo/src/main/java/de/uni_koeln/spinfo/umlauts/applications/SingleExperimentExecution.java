package de.uni_koeln.spinfo.umlauts.applications;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.lazy.IBk;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.LogLikeliHoodFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.TFIDFFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.WekaClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneAbstractClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneKNNClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneNaiveBayesClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneRocchioClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.svm.SVMClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ExperimentResult;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ExperimentSetupUI;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneSingleExperimentExecutor;

/**
 * @author geduldia
 * @author avogt
 * 
 *         Application to execute a single Experiment for the specified
 *         experiment-parameters. Stores the result in specified "outputFolder"
 * 
 */
public class SingleExperimentExecution {

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		
	

		// /////////////////////////////////////////////
		// /////experiment parameters
		// /////////////////////////////////////////////
		
		
		boolean preClassify = true;
		File outputFolder = new File("umlauts/classification/output/singleResults/preClassified");
		int knnValue = 5;
		boolean ignoreStopwords = false;
		boolean normalizeInput = true;
		boolean useStemmer = true;
		boolean suffixTrees = true;
		int[] nGrams = new int[]{3,4};
		int miScoredFeaturesPerClass = 0;
		Distance distance = Distance.COSINUS;
		ZoneAbstractClassifier classifier = new ZoneKNNClassifier(false, knnValue, distance);
		AbstractFeatureQuantifier quantifier = new  TFIDFFeatureQuantifier();
		List<Integer> evaluationCategories = new ArrayList<Integer>();
		evaluationCategories.add(1);
		evaluationCategories.add(2);
		evaluationCategories.add(3);
		// ///////////////////////////////////////////////
		// ////////END///
		// //////////////////////////////////////////////

	
		ZoneJobs jobs = new ZoneJobs();		
		
		FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(
				normalizeInput, useStemmer, ignoreStopwords, nGrams, false,
				miScoredFeaturesPerClass, suffixTrees);
		ExperimentConfiguration expConfig = new ExperimentConfiguration(fuc,
				quantifier, classifier, inputFile, "umlauts/classification/output");	
		ExperimentResult result = ZoneSingleExperimentExecutor.crossValidate(
				expConfig, jobs, inputFile, 4, 6, null, preClassify, evaluationCategories);
		
		System.out.println("F Measure: \t" + result.getF1Measure());
		System.out.println("Precision: \t" + result.getPrecision());
		System.out.println("Recall: \t" + result.getRecall());
		System.out.println("Accuracy: \t" + result.getAccuracy());
		
		// store result
		jobs.persistExperimentResult(result, outputFolder);
	}


}
