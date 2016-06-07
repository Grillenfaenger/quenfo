package de.uni_koeln.spinfo.classification.jasc.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.core.helpers.EncodingProblemTreatment;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.preprocessing.ClassifyUnitSplitter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.RegexClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ExperimentSetupUI;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.dbIO.DbConnector;

public class ConfigurableDatabaseClassifier {

	private Connection inputDb, outputDb;
	int queryLimit, fetchSize, currentId;
	private boolean executeAtBIBB;
	private boolean trainWithDB;
	private String trainingDataFileName;

	public ConfigurableDatabaseClassifier(Connection inputDb, Connection outputDb, int queryLimit, int fetchSize,
			int currentId, boolean trainWithDB, String trainingDataFileName)
					throws FileNotFoundException, UnsupportedEncodingException {
		this.inputDb = inputDb;
		this.outputDb = outputDb;
		this.queryLimit = queryLimit;
		this.fetchSize = fetchSize;
		this.currentId = currentId;
		this.trainWithDB = trainWithDB;
		this.trainingDataFileName = trainingDataFileName;
	}

	public void classify() throws ClassNotFoundException, IOException, SQLException {
		// get ExperimentConfiguration
		ExperimentSetupUI ui = new ExperimentSetupUI();
		ExperimentConfiguration expConfig = ui.getExperimentConfiguration(trainingDataFileName);
		classify(expConfig);
	}

	private void classify(ExperimentConfiguration config) throws IOException, SQLException, ClassNotFoundException {

		// set Translations
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

		// get trainingdata from file (and db)
		File trainingDataFile = new File(trainingDataFileName);
		List<ClassifyUnit> trainingData = null;
		if (config.getFeatureConfiguration().isTreatEncoding()) {
			trainingData = jobs.getCategorizedASCIIParagraphsFromFile(trainingDataFile);
			if (trainWithDB) {
				List<ClassifyUnit> dbTrainingData = jobs.getCategorizedASCIIParagraphsFromDB(outputDb, executeAtBIBB);
				trainingData.addAll(dbTrainingData);
				System.out.println("read " + dbTrainingData.size()+" training-paragraphs from DB");
			}
		} else {
			trainingData = jobs.getCategorizedParagraphsFromFile(trainingDataFile);
			if (trainWithDB) {
				List<ClassifyUnit> dbTrainingData = jobs.getCategorizedParagraphsFromDB(outputDb, executeAtBIBB);
				trainingData.addAll(dbTrainingData);
				System.out.println("read " + dbTrainingData.size()+" training-paragraphs from DB");
			}
		}
		trainingData = jobs.initializeClassifyUnits(trainingData);
		trainingData = jobs.setFeatures(trainingData, config.getFeatureConfiguration(), true);
		trainingData = jobs.setFeatureVectors(trainingData, config.getFeatureQuantifier(), null);

		// build model
		Model model = jobs.getNewModelForClassifier(trainingData, config);
		if (config.getModelFileName().contains("/myModels/")) {
			jobs.exportModel(config.getModelFile(), model);
		}
		
		
		// get data from db
		int done = 0;
		String query = null;
		int zeilenNr = 0, jahrgang = 0;
		;
//		if (executeAtBIBB) {
			// SteA-DB
			query = "SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo LIMIT ? OFFSET ?;";
//		} else {
//			// TrainingData-Db
//			query = "SELECT jobAd_ID, jobAdText FROM jobAds LIMIT ? OFFSET ?;";	
//		}

		PreparedStatement prepStmt = inputDb.prepareStatement(query);
		prepStmt.setInt(1, queryLimit);
		prepStmt.setInt(2, currentId);
		prepStmt.setFetchSize(fetchSize);
		// execute
		ResultSet queryResult = prepStmt.executeQuery();

		// total entries to process:
		if (queryLimit < 0) {
			
			String countQuery = "SELECT COUNT(*) FROM Classes_Correctable;";
			Statement stmt = inputDb.createStatement();
			ResultSet countResult = stmt.executeQuery(countQuery);
			int tableSize = countResult.getInt(1);
			stmt.close();
			stmt = inputDb.createStatement();
			ResultSet rs = null;
//			if (executeAtBIBB) {
				rs = stmt.executeQuery("SELECT COALESCE("+tableSize+"+1, 0) FROM DL_ALL_Spinfo;");
//			} else {
//				rs = stmt.executeQuery("SELECT COALESCE(MAX(jobAd_ID)+1, 0) FROM jobAds;");
//			}
			queryLimit = rs.getInt(1);
		}

		boolean goOn = true;
		boolean askAgain = true;
		long start = System.currentTimeMillis();

		while (queryResult.next() && goOn) {
			String jobAd = null;
//			if (executeAtBIBB) {
				// SteA-DB
				zeilenNr = queryResult.getInt("ZEILENNR");
				jahrgang = queryResult.getInt("Jahrgang");
				// TODO Andreas!!
				 //int nextID = queryResult.getInt("ID");
				jobAd = queryResult.getString("STELLENBESCHREIBUNG");
//			} else {
//				// TrainingData
//				jobAd = queryResult.getString("jobAdText");
//				currentId = queryResult.getInt("jobAd_ID");
//			}

			// if there is an empty job description, classifying is of no use,
			// so skip
			if (jobAd == null) {
				System.out.println("jobAd ist null");
				continue;
			}
			if (jobAd.isEmpty()) {
				System.out.println("Ist leer!");
				continue;
			}

			// 1. Split into paragraphs and create a ClassifyUnit per paragraph
			List<String> paragraphs = ClassifyUnitSplitter.splitIntoParagraphs(jobAd);

			// if treat enc
			if (config.getFeatureConfiguration().isTreatEncoding()) {
				paragraphs = EncodingProblemTreatment.normalizeEncoding(paragraphs);
			}
			List<ClassifyUnit> classifyUnits = new ArrayList<ClassifyUnit>();
			for (String string : paragraphs) {
			//	if (executeAtBIBB) {
					// SteA-DB
					// TODO
					classifyUnits.add(new JASCClassifyUnit(string, jahrgang, zeilenNr));
//				} else {
//					// TrainingData.db
//					classifyUnits.add(new JASCClassifyUnit(string, currentId));
//				}

			}
			// prepare ClassifyUnits
			classifyUnits = jobs.initializeClassifyUnits(classifyUnits);
			classifyUnits = jobs.setFeatures(classifyUnits, config.getFeatureConfiguration(), false);
			classifyUnits = jobs.setFeatureVectors(classifyUnits, config.getFeatureQuantifier(), model.getFUOrder());

			// 2. Classify
			RegexClassifier regexClassifier = new RegexClassifier("classification/data/regex.txt");
			Map<ClassifyUnit, boolean[]> preClassified = new HashMap<ClassifyUnit, boolean[]>();
			for (ClassifyUnit cu : classifyUnits) {
				boolean[] classes = regexClassifier.classify(cu, model);
				preClassified.put(cu, classes);
			}
			Map<ClassifyUnit, boolean[]> classified = jobs.classify(classifyUnits, config, model);
			classified = jobs.mergeResults(classified, preClassified);
			classified = jobs.translateClasses(classified);

			List<ClassifyUnit> results = new ArrayList<ClassifyUnit>();
			for (ClassifyUnit cu : classified.keySet()) {
				((ZoneClassifyUnit) cu).setClassIDs(classified.get(cu));
				System.out.println();
				System.out.println(cu.getContent());
				System.out.print("----->  CLASS: ");
				boolean[] ids = ((ZoneClassifyUnit) cu).getClassIDs();
				boolean b = false;
				for (int i = 0; i < ids.length; i++) {
					if (ids[i]) {
						if (b) {
							System.out.print("& " + (i + 1));
						} else {
							System.out.println((i + 1));
						}
						b = true;
					}
				}
				results.add(cu);
			}
			// 3. Write ClassifyUnits with classes into database
	//		if (executeAtBIBB) {
				// SteA
				DbConnector.insertParagraphsInBIBBDB(outputDb, results, jahrgang, zeilenNr);
//			} else {
//				// TrainingData
//				DbConnector.insertParagraphsInTestDB(outputDb, results, currentId);
//			}

			// progressbar
			done++;
			// ProgressBar.updateProgress((float) done/queryLimit);

			// time needed
			if (done % fetchSize == 0) {
				long end = System.currentTimeMillis();
				long time = (end - start) / 1000;

				// continue?
				if (askAgain) {

					System.out.println(
							"\n\n" + "continue (c),\n" + "don't interrupt again (d),\n" + "or stop (s) classifying?");

					boolean answered = false;
					while (!answered) {
						BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
						String answer = in.readLine();

						if (answer.toLowerCase().trim().equals("c")) {
							goOn = true;
							answered = true;
						} else if (answer.toLowerCase().trim().equals("d")) {
							goOn = true;
							askAgain = false;
							answered = true;

						} else if (answer.toLowerCase().trim().equals("s")) {
							goOn = false;
							answered = true;
						} else {
							System.out.println("C: invalid answer! please try again...");
							System.out.println();
						}
					}
				}
				start = System.currentTimeMillis();
			}
		}
		System.out.println("Classifying was fun! GoodBye!");
	}

}
