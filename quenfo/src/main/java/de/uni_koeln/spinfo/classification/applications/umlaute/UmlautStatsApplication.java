package de.uni_koeln.spinfo.classification.applications.umlaute;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.uni_koeln.spinfo.dbIO.DbConnector;

public class UmlautStatsApplication {
	
	// Path to input database
	static String dbInputPath = "classification/db/bibbDB.db";  
	// overall results fetched from db, no Limit: -1
	static int queryLimit = -1;
	// start query from entry with id larger than currentID
	static int currentId = 0;
	// number of results fetched in one step
	static int fetchSize = 100;
	
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
	// Connect to input database
	Connection inputDb = null;
			
	if (!new File(dbInputPath).exists()) {
		System.out
				.println("Database doesn't exist " + dbInputPath + "\nPlease change configuration and start again.");
		System.exit(0);
	} else {
		inputDb = DbConnector.connect(dbInputPath);
	}
	
	
	String query = null;
	int zeilenNr = 0, jahrgang = 0;

	query = "SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo LIMIT ? OFFSET ?;";
	
	PreparedStatement prepStmt = inputDb.prepareStatement(query);
	prepStmt.setInt(1, queryLimit);
	prepStmt.setInt(2, currentId);
	prepStmt.setFetchSize(fetchSize);
	// execute
	ResultSet queryResult = prepStmt.executeQuery();
	
	while (queryResult.next()) {
		String jobAd = null;
//		if (executeAtBIBB) {
			// SteA-DB
			zeilenNr = queryResult.getInt("ZEILENNR");
			jahrgang = queryResult.getInt("Jahrgang");
			// TODO Andreas!!
			 //int nextID = queryResult.getInt("ID");
			jobAd = queryResult.getString("STELLENBESCHREIBUNG");
	}
		
		

}
}