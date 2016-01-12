package de.uni_koeln.spinfo.classification.jasc.dbIO;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.information_extraction.data.Competence;

public class DbConnector {
	
	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		// register the driver 
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		System.out.println("Database " + dbFilePath + " successfully opened");		
		return connection;
	}
	

	public static void createParagraphOutputTables (Connection connection, boolean executeInBIBB) throws SQLException {

		
		String sql;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		sql = "DROP TABLE IF EXISTS ClassifiedParaTexts";
		stmt.executeUpdate(sql);
		if(executeInBIBB){
			  //   SteA-DB
		    sql = "CREATE TABLE ClassifiedParaTexts " +
	                "(ID		INTEGER PRIMARY KEY AUTOINCREMENT ," +
	                " ParaID TEXT    NOT NULL, " + 
	                " Jahrgang   INT     NOT NULL, " + 
	                " ZEILENNR   INT     NOT NULL, " + 
	                " STELLENBESCHREIBUNG   TEXT)"; 
		     stmt.executeUpdate(sql);
		}
		else{
			// TrainingData.db
		    sql = "CREATE TABLE ClassifiedParaTexts " +
		                   "(ID		INTEGER PRIMARY KEY AUTOINCREMENT ," +
		                   " ParaID TEXT    NOT NULL, " + 
		                   " AdID   INT     NOT NULL, " + 
		                   " Text   TEXT    NOT NULL)";
		    stmt.executeUpdate(sql);
		} 
	      sql = "DROP TABLE IF EXISTS Classes_Original";
	      stmt.executeUpdate(sql);
	      sql = "CREATE TABLE Classes_Original" +
	    		  "(OrigID	INTEGER PRIMARY KEY AUTOINCREMENT ," +
                  " TxtID   	INT     NOT NULL, " + 
                  " ClassONE   	INT     NOT NULL, " + 
                  " ClassTWO    INT    	NOT NULL, " + 
                  " ClassTHREE  INT    	NOT NULL, " +
                  " ClassFOUR  INT    	NOT NULL, " +
                  " FOREIGN KEY(TxtID) REFERENCES ClassifiedParaTexts(ID))";
	      
	      // Wo ist das?
	      stmt.executeUpdate(sql);
	      
	      sql = "DROP TABLE IF EXISTS Classes_Correctable";
	      stmt.executeUpdate(sql);
	      sql = "CREATE TABLE Classes_Correctable" +
	    		  "(CorrID	INTEGER PRIMARY KEY AUTOINCREMENT ," +
                  " TxtID   	INT     NOT NULL, " + 
                  " ClassONE   	INT     NOT NULL, " + 
                  " ClassTWO    INT    	NOT NULL, " + 
                  " ClassTHREE  INT    	NOT NULL, " +
                  " ClassFOUR  INT    	NOT NULL, " +
                  " UseForTraining	INT	    NOT NULL,"  +
                  " FOREIGN KEY(TxtID) REFERENCES ClassifiedParaTexts(ID))";
	      stmt.executeUpdate(sql);    
	      stmt.close();
	      connection.commit();
	      System.out.println("Initialized new output-database.");
		
	}
	
	public static void createCompetenceOutputTables(Connection connection) throws SQLException{
		String sql;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		sql = "DROP TABLE IF EXISTS ClassifiedCompetences";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE  ClassifiedCompetences"+
				"(CompetenceID	INTEGER	PRIMARY KEY AUTOINCREMENT,"+
				"CompetenceText	TEXT,"+
				"Quality	TEXT,"+
				"Importance TEXT,"+
				"Type	VARCHAR(2),"+
				"AdID INT NOT NULL)"; 
		stmt.executeUpdate(sql);
		stmt.close();
	    connection.commit();
	}
	
	public static boolean insertCompetences(Connection connection, List<Competence> toAdd) throws SQLException{
		connection.setAutoCommit(false);
		PreparedStatement prepComp = connection.prepareStatement("INSERT OR REPLACE INTO ClassifiedCompetences (CompetenceText, Quality, Importance, Type, AdID) VALUES(?,?,?,?,?)");
		for (Competence competence : toAdd) {
			prepComp.setString(1, competence.getCompetence());
			prepComp.setString(2, competence.getQuality());
			prepComp.setString(3, competence.getImportance());
			if(competence.getType()!= null){
				prepComp.setString(4, competence.getType().toString());
			}
			prepComp.setInt(5, competence.getJobAdID());
			prepComp.executeUpdate();
			System.out.println("insert: " + competence);
		}
		if (prepComp != null ) prepComp.close();
		connection.commit();
		return true;
	}
	
	public static void createClassifedCompetencesTable(Connection connection) throws SQLException{
		String sql;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		sql = "DROP TABLE IF EXISTS ClassifiedCompetences";
		stmt.executeUpdate(sql);
		
		sql = "CREATE TABLE ClassifiedCompetences"+
				"(CompetenceID	INTEGER	PRIMARY KEY AUTOINCREMENT,"+
				"CompetenceText	TEXT,"+
				"Quality	TEXT,"+
				"Importance TEXT,"+
				"Type	VARCHAR(2),"+
				"FOREIGN KEY(AdID) REFERENCES ClassifiedParaTexts(AdID))";
	}
	
	public static void insertIntoClassifiedCompetencesTable(Connection connection, List<Competence> toAdd){
		
	}
	
	/**
	 * Inserts  a List of Classified Paragraphs into the outpud database. 
	 * @param outputConnection
	 * @param results
	 * @param jobAdId
	 * @return true, if instertion succeeded, false, if not
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public static boolean insertParagraphsInTestDB(Connection outputConnection, List<ClassifyUnit> results, int jobAdId) throws SQLException {
		
		boolean[] classIDs;
		int txtTblID;
		
		try {
			outputConnection.setAutoCommit(false);
		
			Statement stmt = outputConnection.createStatement();
			PreparedStatement prepTxtTbl = outputConnection.prepareStatement("INSERT OR REPLACE INTO ClassifiedParaTexts (ParaID,AdID,Text) VALUES(?,?,?)");
			PreparedStatement prepClfyOrig = outputConnection.prepareStatement("INSERT OR REPLACE INTO Classes_Original (TxtID,ClassONE,ClassTWO,ClassTHREE,ClassFOUR) VALUES(?,?,?,?,?)");
			PreparedStatement prepClfyCorbl = outputConnection.prepareStatement("INSERT OR REPLACE INTO Classes_Correctable (TxtID,ClassONE,ClassTWO,ClassTHREE,ClassFOUR, UseForTraining) VALUES(?,?,?,?,?,?)");
			
			for (ClassifyUnit cu : results) {
				
				// Update ClassifiedParaTexts
				prepTxtTbl.setString(1, cu.getID().toString());
				prepTxtTbl.setInt(2, jobAdId);
				prepTxtTbl.setString(3, cu.getContent());
				prepTxtTbl.executeUpdate();
				
				// get ID of last inserted row for use as a foreign key
				ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
				rs.next();
				txtTblID = rs.getInt(1);
				
				int booleanRpl;			// replaces true/false for saving into sqliteDB
				classIDs = ((ZoneClassifyUnit) cu).getClassIDs();
				
				// Update Classes_Original
				prepClfyOrig.setInt(1, txtTblID);
				for (int classID = 0; classID <= 3; classID++) {
					if (classIDs[classID]) {
						booleanRpl = 1;
					} else {
						booleanRpl = 0;
					}
					prepClfyOrig.setInt(2 + classID, booleanRpl);
				}
				prepClfyOrig.executeUpdate();
				
				// Update Classes_Correctable
				prepClfyCorbl.setInt(1, txtTblID);
				for (int classID = 0; classID <= 3; classID++) {
					if (classIDs[classID]) {
						booleanRpl = 1;
					} else {
						booleanRpl = 0;
					}
					prepClfyCorbl.setInt(2 + classID, booleanRpl);
				}
				prepClfyCorbl.setInt(6, 0);
				prepClfyCorbl.executeUpdate();
			}
			
			if (prepTxtTbl != null ) prepTxtTbl.close();
			if (prepClfyOrig != null) prepClfyOrig.close();
			if (prepClfyCorbl != null) prepClfyCorbl.close();
			if (stmt != null) stmt.close();
			
			outputConnection.commit();
			return true;
			
		} catch (SQLException e) {
			outputConnection.rollback();
			e.printStackTrace();
			return false;
		}

	}
	
	public static boolean insertParagraphsInBIBBDB(Connection outputConnection, List<ClassifyUnit> results, int jahrgang, int zeilennummer) throws SQLException {
	
		boolean[] classIDs;
		int txtTblID;
		
		try {
			outputConnection.setAutoCommit(false);
		
			Statement stmt = outputConnection.createStatement();
			PreparedStatement prepTxtTbl = outputConnection.prepareStatement("INSERT INTO ClassifiedParaTexts (ParaID,jahrgang,ZEILENNR,STELLENBESCHREIBUNG) VALUES(?,?,?,?)");
			PreparedStatement prepClfyOrig = outputConnection.prepareStatement("INSERT INTO Classes_Original (TxtID,ClassONE,ClassTWO,ClassTHREE,ClassFOUR) VALUES(?,?,?,?,?)");
			PreparedStatement prepClfyCorbl = outputConnection.prepareStatement("INSERT INTO Classes_Correctable (TxtID,ClassONE,ClassTWO,ClassTHREE,ClassFOUR, UseForTraining) VALUES(?,?,?,?,?,?)");
			
			for (ClassifyUnit cu : results) {
				
				// Update ClassifiedParaTexts
				prepTxtTbl.setString(1, cu.getID().toString());
				prepTxtTbl.setInt(2, jahrgang);
				prepTxtTbl.setInt(3, zeilennummer);
				prepTxtTbl.setString(4, cu.getContent());
				prepTxtTbl.executeUpdate();
				
				// get ID of last inserted row for use as a foreign key
				ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
				rs.next();
				txtTblID = rs.getInt(1);
				
				int booleanRpl;			// replaces true/false for saving into sqliteDB
				classIDs = ((ZoneClassifyUnit) cu).getClassIDs();
				
				// Update Classes_Original
				prepClfyOrig.setInt(1, txtTblID);
				for (int classID = 0; classID <= 3; classID++) {
					if (classIDs[classID]) {
						booleanRpl = 1;
					} else {
						booleanRpl = 0;
					}
					prepClfyOrig.setInt(2 + classID, booleanRpl);
				}
				prepClfyOrig.executeUpdate();
				
				// Update Classes_Correctable
				prepClfyCorbl.setInt(1, txtTblID);
				for (int classID = 0; classID <= 3; classID++) {
					if (classIDs[classID]) {
						booleanRpl = 1;
					} else {
						booleanRpl = 0;
					}
					prepClfyCorbl.setInt(2 + classID, booleanRpl);
				}
				prepClfyCorbl.setInt(6, 0);
				prepClfyCorbl.executeUpdate();
			}
			
			prepTxtTbl.close();
			prepClfyOrig.close();
			prepClfyCorbl.close();
			stmt.close();
			outputConnection.commit();
			
			return true;
			
		} catch (SQLException e) {
			outputConnection.rollback();
			e.printStackTrace();
			return false;
		}
	
	}
}
