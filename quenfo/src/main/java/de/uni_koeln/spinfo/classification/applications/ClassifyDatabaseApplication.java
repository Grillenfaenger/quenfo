package de.uni_koeln.spinfo.classification.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.jasc.workflow.ConfigurableDatabaseClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.dbIO.DbConnector;

/**
 * 
 * @author avogt
 * 
 *         the main application to classify a data-base
 *
 */
public class ClassifyDatabaseApplication {
	

	static String trainingDataFileName = "classification/data/trainingSets/trainingDataScrambled.csv";

	
	
	/////////////////////////////
	// Database connection parameters
	/////////////////////////////
	
	//add manually classified paragraphs from 'Classes_Correctable' to trainingData
	static boolean trainWithDB = true;

	// Path to input database
	static String dbInputPath = /*"D:/Daten/sqlite/SteA.db3";*/"classification/db/bibbDB.db";  
	// Path to output database
	static String stdOutputPath = /*D:/Daten/sqlite/*/"C:/sqlite/";
	// name of output database
	static String dbFileName = "ClassifiedParagraphs.db";
	// overall results fetched from db, no Limit: -1
	static int queryLimit = -1;
	// start query from entry with id larger than currentID
	static int currentId = 0;
	// number of results fetched in one step
	static int fetchSize = 100;

	/////////////////////////////
	// END
	/////////////////////////////
	
	
	
	

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		// Connect to input database
		Connection inputConnection = null;
		if (!new File(dbInputPath).exists()) {
			System.out
					.println("Database don't exists " + dbInputPath + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = DbConnector.connect(dbInputPath);
		}

		// Connect to output database
		Connection outputConnection = null;
		File dbFile = new File(stdOutputPath + dbFileName);
		if (dbFile.exists()) {
			System.out.println("\nOutput-Database " + stdOutputPath + dbFileName + " already exists. "
					+ "\n - press 'o' to overwrite it (deletes all prior entries)"
					+ "\n - press 'u' to use it (adds and replaces entries)"
					+ "\n - press 'c' to create a new Output-Database");
			boolean answered = false;
			while (!answered) {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				String answer = in.readLine();
				if (answer.toLowerCase().trim().equals("o")) {
					outputConnection = DbConnector.connect(stdOutputPath + dbFileName);
					DbConnector.createClassificationOutputTables(outputConnection);
					answered = true;
				} else if (answer.toLowerCase().trim().equals("u")) {
					outputConnection = DbConnector.connect(stdOutputPath + dbFileName);
					answered = true;
				} else if (answer.toLowerCase().trim().equals("c")) {
					System.out.println(
							"Please enter the name of the new Database. It will be stored in " + stdOutputPath);
					BufferedReader ndIn = new BufferedReader(new InputStreamReader(System.in));
					dbFileName = ndIn.readLine();
					outputConnection = DbConnector.connect(stdOutputPath + dbFileName);
					DbConnector.createClassificationOutputTables(outputConnection);
					answered = true;
				} else {
					System.out.println("C: invalid answer! please try again...");
					System.out.println();
				}
			}
		} else {
			outputConnection = DbConnector.connect(stdOutputPath + dbFileName); 
			DbConnector.createClassificationOutputTables(outputConnection);
		}

		// create output-directory if not exists
		if (!new File("classification/output").exists()) {
			new File("classification/output").mkdirs();
		}
		
		//start classifying
		ConfigurableDatabaseClassifier dbClassfy = new ConfigurableDatabaseClassifier(inputConnection, outputConnection, queryLimit, fetchSize, currentId, trainWithDB, trainingDataFileName);
		try {
			dbClassfy.classify();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
