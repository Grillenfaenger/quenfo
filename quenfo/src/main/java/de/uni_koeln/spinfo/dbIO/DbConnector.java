package de.uni_koeln.spinfo.dbIO;

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
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.competenceExtraction.Competence;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.Tool;
import de.uni_koeln.spinfo.umlauts.data.JobAd;

public class DbConnector {

	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		// register the driver
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		System.out.println("Database " + dbFilePath + " successfully opened");
		return connection;
	}

	public static void createBIBBDB(Connection connection) throws SQLException {
		System.out.println("create inputDB");
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "DROP TABLE IF EXISTS DL_ALL_Spinfo";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE DL_ALL_Spinfo (ID  INTEGER PRIMARY KEY AUTOINCREMENT, ZEILENNR INT NOT NULL, Jahrgang INT NOT NULL, STELLENBESCHREIBUNG TEXT)";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}

	public static void createClassificationOutputTables(Connection connection)
			throws SQLException {

		String sql;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		sql = "DROP TABLE IF EXISTS ClassifiedParaTexts";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE ClassifiedParaTexts " + "(ID		INTEGER PRIMARY KEY AUTOINCREMENT ,"
				+ " ParaID TEXT    NOT NULL, " + " Jahrgang   INT     NOT NULL, " + " ZEILENNR   INT     NOT NULL, "
				+ " STELLENBESCHREIBUNG   TEXT)";
		stmt.executeUpdate(sql);

		sql = "DROP TABLE IF EXISTS Classes_Original";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE Classes_Original" + "(OrigID	INTEGER PRIMARY KEY AUTOINCREMENT ,"
				+ " TxtID   	INT     NOT NULL, " + " ClassONE   	INT     NOT NULL, "
				+ " ClassTWO    INT    	NOT NULL, " + " ClassTHREE  INT    	NOT NULL, "
				+ " ClassFOUR  INT    	NOT NULL, " + " FOREIGN KEY(TxtID) REFERENCES ClassifiedParaTexts(ID))";
		stmt.executeUpdate(sql);

		sql = "DROP TABLE IF EXISTS Classes_Correctable";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE Classes_Correctable" + "(CorrID	INTEGER PRIMARY KEY AUTOINCREMENT ,"
				+ " TxtID   	INT     NOT NULL, " + " ClassONE   	INT     NOT NULL, "
				+ " ClassTWO    INT    	NOT NULL, " + " ClassTHREE  INT    	NOT NULL, "
				+ " ClassFOUR  INT    	NOT NULL, " + " UseForTraining	INT	    NOT NULL,"
				+ " FOREIGN KEY(TxtID) REFERENCES ClassifiedParaTexts(ID))";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
		System.out.println("Initialized new output-database.");

	}

	public static void createToolOutputTables(Connection connection) throws SQLException {
		String sql;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		sql = "DROP TABLE IF EXISTS Tools";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE Tools (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Tool TEXT NOT NULL)";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
		System.out.println("initialized ne output-database 'Tools'");
	}
	
	
	public static boolean insertParagraphsInBIBBDB(Connection outputConnection, List<ClassifyUnit> results,
			int jahrgang, int zeilennummer) throws SQLException {

		boolean[] classIDs;
		int txtTblID;

		try {
			outputConnection.setAutoCommit(false);

			Statement stmt = outputConnection.createStatement();
			PreparedStatement prepTxtTbl = outputConnection.prepareStatement(
					"INSERT INTO ClassifiedParaTexts (ParaID,jahrgang,ZEILENNR,STELLENBESCHREIBUNG) VALUES(?,?,?,?)");
			PreparedStatement prepClfyOrig = outputConnection.prepareStatement(
					"INSERT INTO Classes_Original (TxtID,ClassONE,ClassTWO,ClassTHREE,ClassFOUR) VALUES(?,?,?,?,?)");
			PreparedStatement prepClfyCorbl = outputConnection.prepareStatement(
					"INSERT INTO Classes_Correctable (TxtID,ClassONE,ClassTWO,ClassTHREE,ClassFOUR, UseForTraining) VALUES(?,?,?,?,?,?)");

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

				int booleanRpl; // replaces true/false for saving into sqliteDB
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

	//writes ClassifyUnits (treated as job-Ads) in BIBB-DB
	public static void writeInBIBBDB(List<ClassifyUnit> input, Connection conn) throws SQLException {
		conn.setAutoCommit(false);
		PreparedStatement prep = conn
				.prepareStatement("INSERT INTO DL_ALL_Spinfo (ZEILENNR, Jahrgang, STELLENBESCHREIBUNG) VALUES(?,?,?)");
		for (ClassifyUnit classifyUnit : input) {
			prep.setInt(1, ((JASCClassifyUnit) classifyUnit).getSecondParentID());
			prep.setInt(2, ((JASCClassifyUnit) classifyUnit).getParentID());
			prep.setString(3, classifyUnit.getContent());
			prep.executeUpdate();
		}
		prep.close();
		conn.commit();
	}
	
	public static boolean insertJobAdsInBIBBDB(Connection outputConnection, List<JobAd> jobAds) throws SQLException {

		try {
			outputConnection.setAutoCommit(false);

			Statement stmt = outputConnection.createStatement();
			PreparedStatement prepTxtTbl = outputConnection.prepareStatement(
					"INSERT INTO ClassifiedParaTexts (ID,jahrgang,ZEILENNR,STELLENBESCHREIBUNG) VALUES(?,?,?,?)");
			

			for (JobAd jobAd : jobAds) {
				prepTxtTbl.setInt(1, jobAd.getId());
				prepTxtTbl.setInt(2, jobAd.getJahrgang());
				prepTxtTbl.setInt(3, jobAd.getZeilennummer());
				prepTxtTbl.setString(4, jobAd.getContent());
				prepTxtTbl.executeUpdate();
			}

			prepTxtTbl.close();
			stmt.close();
			outputConnection.commit();

			return true;

		} catch (SQLException e) {
			outputConnection.rollback();
			e.printStackTrace();
			return false;
		}

	}


	public static void writeToolsInDB(ExtractionUnit cu, List<Tool> tools, Connection outputConnection) throws SQLException {
		int jahrgang = cu.getJobAdID();
		int zeilennr = cu.getSecondJobAdID();
		String paraID = cu.getClassifyUnitID().toString();
		PreparedStatement prepStmt = outputConnection.prepareStatement("INSERT INTO Tools (Jahrgang, Zeilennr, ParaID, Tool) VALUES("+jahrgang+", "+zeilennr+", '"+paraID+"',?)");
		for (Tool tool : tools) {
			prepStmt.setString(1, tool.toString());
			prepStmt.executeUpdate();
		}
		prepStmt.close();
		outputConnection.commit();
	}

//	public static void createCompetenceOutputTables(Connection connection) throws SQLException {
//	String sql;
//	connection.setAutoCommit(false);
//	Statement stmt = connection.createStatement();
//	sql = "DROP TABLE IF EXISTS ClassifiedCompetences";
//	stmt.executeUpdate(sql);
//	sql = "CREATE TABLE  ClassifiedCompetences" + "(CompetenceID	INTEGER	PRIMARY KEY AUTOINCREMENT,"
//			+ "CompetenceText	TEXT," + "Quality	TEXT," + "Importance TEXT," + "Type	VARCHAR(2),"
//			+ "AdID INT NOT NULL)";
//	stmt.executeUpdate(sql);
//	stmt.close();
//	connection.commit();
//}

//public static boolean insertCompetences(Connection connection, List<Competence> toAdd) throws SQLException {
//	connection.setAutoCommit(false);
//	PreparedStatement prepComp = connection.prepareStatement(
//			"INSERT OR REPLACE INTO ClassifiedCompetences (CompetenceText, Quality, Importance, Type, AdID) VALUES(?,?,?,?,?)");
//	for (Competence competence : toAdd) {
//		prepComp.setString(1, competence.getCompetence());
//		prepComp.setString(2, competence.getQuality());
//		prepComp.setString(3, competence.getImportance());
//		if (competence.getType() != null) {
//			prepComp.setString(4, competence.getType().toString());
//		}
//		prepComp.setInt(5, competence.getJobAdID());
//		prepComp.executeUpdate();
//		System.out.println("insert: " + competence);
//	}
//	if (prepComp != null)
//		prepComp.close();
//	connection.commit();
//	return true;
//}

//public static void createClassifedCompetencesTable(Connection connection) throws SQLException {
//	String sql;
//	connection.setAutoCommit(false);
//	Statement stmt = connection.createStatement();
//	sql = "DROP TABLE IF EXISTS ClassifiedCompetences";
//	stmt.executeUpdate(sql);
//
//	sql = "CREATE TABLE ClassifiedCompetences" + "(CompetenceID	INTEGER	PRIMARY KEY AUTOINCREMENT,"
//			+ "CompetenceText	TEXT," + "Quality	TEXT," + "Importance TEXT," + "Type	VARCHAR(2),"
//			+ "FOREIGN KEY(AdID) REFERENCES ClassifiedParaTexts(AdID))";
//}
}
