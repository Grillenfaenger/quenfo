package de.uni_koeln.spinfo.classification.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.dbIO.DbConnector;

//creates a simulation of the jobAd-DB from the BIBB

public class createBIBBDB {

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		Connection conn = DbConnector.connect("classification/data/db/bibbDB.db");
		DbConnector.createBIBBDB(conn);

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
		List<ClassifyUnit> input = jobs.getCategorizedParagraphsFromFile(new File("classification/data/notAnnotatedTrainingData_March2016.csv"));
		DbConnector.writeInBIBBDB(input, conn);
	}
}
