package de.uni_koeln.spinfo.classification.applications;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.preprocessing.TrainingDataGenerator;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;

public class GetTrainingDataStatisticsApp {

	private static String tdFile = "classification/data/newTrainingData2016.csv";

	public static void main(String[] args) throws IOException {
		// Translations
		Map<Integer, List<Integer>> translations = new HashMap<Integer, List<Integer>>();
		List<Integer> categories = new ArrayList<Integer>();
		categories.add(1);
		categories.add(2);
		translations.put(5, categories);
		categories = new ArrayList<Integer>();
		categories.add(2);
		categories.add(3);
		translations.put(6, categories);
		SingleToMultiClassConverter stmc = new SingleToMultiClassConverter(6, 4, translations);
		ZoneJobs jobs = new ZoneJobs(stmc);
		List<ClassifyUnit> data = jobs.getCategorizedParagraphsFromFile(new File(tdFile));
		int[] counts = new int[stmc.getNumberOfClasses()];
		for (ClassifyUnit cu : data) {
			int id = ((JASCClassifyUnit) cu).getActualClassID();
			counts[id-1]++;
		}
		for (int i = 0; i < counts.length; i++) {
			System.out.println("class "+(i+1)+": " + counts[i]);
		}
	}

}
