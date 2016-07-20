package de.uni_koeln.spinfo.umlauts.classification.workflow;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.RegexClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ExperimentResult;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.dbIO.DbConnector;
import de.uni_koeln.spinfo.umlauts.classification.UmlautClassifyUnit;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class ZoneSingleUmlautExperimentExecutor {
	

	
	public static ExperimentResult crossValidate(ExperimentConfiguration expConfig, ZoneJobs jobs, File trainingDataFile, int numCategories, int numClasses, Map<Integer,List<Integer>> translations, boolean preClassify, List<Integer> evaluationCategories) throws IOException {
		long before = System.nanoTime();
		//load ambiguities
		TreeMap<String, TreeSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//ambigeWörter.txt");
		//load KeyWordContexts
		KeywordContexts kwctxt = new KeywordContexts();
		kwctxt.loadKeywordContextsFromFile("output//classification//KontexteTest.txt");
		
		long after = System.nanoTime();
		System.out.println("load data: " + (after - before)/1000000000d);
		
		
		// CU erstellen
		before = System.nanoTime();
		
		// Classification Units erstellen (diese sind dann schon initialisiert)
		
		int textGroupRatio = 10;
		List<ClassifyUnit> testData = new ArrayList<ClassifyUnit>();
		Map<String, Model> models= new HashMap<String, Model>();
		
			for (Entry<String, TreeSet<String>> entry : ambiguities.entrySet()) {
				List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
				String[] senses = entry.getValue().toArray(new String[entry.getValue().size()]);
				
				for(String string : entry.getValue()){
					System.out.println("build model for " + entry.getKey());
					System.out.println(entry.getValue());
					List<List<String>> contexts = kwctxt.getContext(string);
					// durchmischen
					Collections.shuffle(contexts, new Random(0));
					System.out.println(contexts.size() + " Kontexte mit " + string);
					for (int i = 0; i < contexts.size(); i++) {
						ZoneClassifyUnit cu = new UmlautClassifyUnit(contexts.get(i), string, senses, true);
						if((i % textGroupRatio) == 0){
							testData.add(cu);
						} else {
							trainingData.add(cu);
						}
					}
					trainingData = jobs.setFeatures(trainingData, expConfig.getFeatureConfiguration(), true);	
					trainingData = jobs.setFeatureVectors(trainingData, expConfig.getFeatureQuantifier(), null);
				}
				Model model = jobs.getNewModelForClassifier(trainingData, expConfig);
				models.put(entry.getKey(), model);
			}
		after = System.nanoTime();
		System.out.println("build and initialize CU, set Features, set FeatureVectors and build models: " + (after - before)/1000000000d);
		
		
		

		
		
		//classify
		before = System.nanoTime();
		
		after = System.nanoTime();
		//system.out.println("crossvalidate: " + (after - before)/1000000000d);
	    
		
	    //evaluate
	    before = System.nanoTime();
		ExperimentResult result = jobs.evaluate(classified, evaluationCategories, expConfig);
		after = System.nanoTime();
		//system.out.println("evaluate: " + (after - before)/1000000000d);
		return result;
	}

	
}

