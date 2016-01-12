package de.uni_koeln.spinfo.classification.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.dbIO.DbConnector;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;

public class AddTrainingDataToTestDB {

	static String trainingDataFileName = "classification/data/newTrainingData_class3.csv";
	static String outputDBPath = "C:/sqlite/";
	static String outputDBFileName = "ClassifiedParagraphs.db";

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

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
		SingleToMultiClassConverter stmc = new SingleToMultiClassConverter(6,4, translations);
		ZoneJobs jobs = new ZoneJobs(stmc);

		// ConnectToDataBase
		File dbFile = new File(outputDBPath + outputDBFileName);
		Connection connection = null;
		if (!dbFile.exists()) {
			System.out.println(
					"Database don't exists " + dbFile.getName() + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			connection = DbConnector.connect(outputDBPath + outputDBFileName);
		}

		// getDataFromFile
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(new File(trainingDataFileName));
		
		//write into DB
		Statement stmt = connection.createStatement();
		PreparedStatement prepStmtClasses_Correctable = connection.prepareStatement("INSERT OR REPLACE INTO Classes_Correctable (TxtID, ClassONE, ClassTWO, ClassTHREE, ClassFOUR, UseForTraining) VALUES(?,?,?,?,?,?)");
		PreparedStatement prepStmtClasses_Original = connection.prepareStatement("INSERT OR REPLACE INTO Classes_Original (TxtID, ClassONE, ClassTWO, ClassTHREE, ClassFOUR) Values(?,?,?,?,?)");
		PreparedStatement prepStmtClassifiedParaTexts = connection.prepareStatement("INSERT OR REPLACE INTO ClassifiedParaTexts (ParaID,AdID,Text) VALUES(?,?,?)");
		int id;
		for (ClassifyUnit cu : cus) {
			//insert into ClassifiedParaTexts
			prepStmtClassifiedParaTexts.setString(1, cu.getID().toString());
			prepStmtClassifiedParaTexts.setInt(2, ((JASCClassifyUnit) cu).getParentID() );
			prepStmtClassifiedParaTexts.setString(3, cu.getContent());
			prepStmtClassifiedParaTexts.executeUpdate();
			//insert into Classes_Original and Classes_Correctable
			// get ID of last inserted row for use as a foreign key
			ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
			rs.next();
			id = rs.getInt(1);
			prepStmtClasses_Correctable.setInt(1, id);
			prepStmtClasses_Original.setInt(1, id);
			boolean[] classIDs = ((ZoneClassifyUnit) cu).getClassIDs();
			for(int classID = 0; classID < classIDs.length; classID++){
				if(classIDs[classID]){
					prepStmtClasses_Correctable.setInt(2+classID, 1);
					prepStmtClasses_Original.setInt(2+classID, 1);
				}
				else{
					prepStmtClasses_Correctable.setInt(2+classID, 0);
					prepStmtClasses_Original.setInt(2+classID, 0);
				}
			}
			prepStmtClasses_Correctable.setInt(6, 1);
			prepStmtClasses_Correctable.executeUpdate();
			prepStmtClasses_Original.executeUpdate();
		}
		prepStmtClasses_Correctable.close();
		prepStmtClasses_Original.close();
		prepStmtClassifiedParaTexts.close();
	}

}
