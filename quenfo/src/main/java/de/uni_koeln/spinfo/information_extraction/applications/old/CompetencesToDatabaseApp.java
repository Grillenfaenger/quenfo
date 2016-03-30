package de.uni_koeln.spinfo.information_extraction.applications.old;
//package de.uni_koeln.spinfo.information_extraction.applications;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.ArrayList;
//import java.util.List;
//
//import de.uni_koeln.spinfo.classification.jasc.dbIO.DbConnector;
//import de.uni_koeln.spinfo.information_extraction.data.Competence;
//import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
//import de.uni_koeln.spinfo.information_extraction.preprocessing.ClassifiedCompetencesTrainingDataGenerator;
//import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;
//
//public class CompetencesToDatabaseApp {
//	
//
//	
//	// static File competencesFile = new File("information_extraction/data/classifiedCompetences_trainingDataScrambled_3_6.txt");
//	static File comptencesFile = new File("src/test/resources/information_extraction/competenceData_newTrainingData_class3_3_6.txt");
//	static boolean isClassified = false;
//
//	public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
//		String outputPath = "C:/sqlite/";
//		String dbFileName ="ClassifiedCompetences.db";
//		Connection outputConnection = null;	
//		File dbFile = new File(outputPath + dbFileName);		
//		if (dbFile.exists()) {
//			System.out.println("\nOutput-Database " + outputPath + dbFileName + " already exists. "
//					+ "\n - press 'o' to overwrite it (deletes all prior entries)"
//					+ "\n - press 'u' to use it (adds and replaces entries)"
//					+ "\n - press 'c' to create a new Output-Database");
//			
//			boolean answered = false;
//			while (!answered) {
//				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//				String answer = in.readLine();
//	
//				if (answer.toLowerCase().trim().equals("o")) {
//					outputConnection = DbConnector.connect(outputPath + dbFileName);		// conntect to output database
//					DbConnector.createCompetenceOutputTables(outputConnection);
//					answered = true;		
//				} else if (answer.toLowerCase().trim().equals("u")) {
//					outputConnection = DbConnector.connect(outputPath + dbFileName);
//					answered = true;
//				} else if (answer.toLowerCase().trim().equals("c")) {
//					System.out.println("Please enter the name of the new Database. It will be stored in " + outputPath);
//					BufferedReader ndIn = new BufferedReader(new InputStreamReader(System.in));
//					dbFileName = ndIn.readLine();
//					outputConnection = DbConnector.connect(outputPath + dbFileName);
//					DbConnector.createCompetenceOutputTables(outputConnection);
//					answered = true;
//				} else {
//					System.out.println("C: invalid answer! please try again...");
//					System.out.println();
//				}
//			}
//		} else {
//			outputConnection = DbConnector.connect(outputPath + dbFileName);		// conntect to output database
//			DbConnector.createCompetenceOutputTables(outputConnection);
//		}
//		
//		if(isClassified){
//			//read classified Competences From File
//			ClassifiedCompetencesTrainingDataGenerator tdg = new ClassifiedCompetencesTrainingDataGenerator(comptencesFile);
//			List<Competence> classifiedCompetences = tdg.getclassifedCompetences();
//			DbConnector.insertCompetences(outputConnection, classifiedCompetences);
//		}
//		else{
//			//reat competenceUnits from File
//			IEJobs jobs = new IEJobs();
//			List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(comptencesFile);
//			List<Competence> competences = new ArrayList<Competence>();
//			for (CompetenceUnit cu: compUnits) {
//				if(cu.getCompetences()!=null){
//					competences.addAll(cu.getCompetences());
//				}
//			}
//			DbConnector.insertCompetences(outputConnection, competences);
//		}
//		
//	}
//}
//
