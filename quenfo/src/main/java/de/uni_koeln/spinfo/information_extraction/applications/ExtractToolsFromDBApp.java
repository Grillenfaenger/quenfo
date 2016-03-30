package de.uni_koeln.spinfo.information_extraction.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.uni_koeln.spinfo.dbIO.DbConnector;
import de.uni_koeln.spinfo.information_extraction.ConfigurableToolExtractor;
import weka.gui.beans.Startable;

public class ExtractToolsFromDBApp {

	static int startPos = 650;
	static int maxCount = 50;
	static boolean executeAtBIBB = false;
	
	static File toolsFile = new File("information_extraction/data/tools/tools.txt");
	static File noToolsFile = new File("information_extraction/data/tools/no_tools.txt");
	static File contextFile = new File("information_extraction/data/tools/toolContexts.txt");

	// Path to input database
	static String dbInputPath = /* "C:/sqlite/SteA.db3"; */"C:/sqlite/ClassifiedParagraphs.db";
	// Path to output database
	static String stdOutputPath = "C:/sqlite/";
	// name of output database
	static String dbFileName = "Tools.db";

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
		Connection outputConnection = DbConnector.connect(stdOutputPath + dbFileName);

		//check count and startPos 
		String query = "SELECT COUNT(*) FROM Classes_Correctable;";
		Statement stmt = inputConnection.createStatement();
		ResultSet countResult = stmt.executeQuery(query);
		int tableSize = countResult.getInt(1);
		stmt.close();
		if(tableSize <= startPos){
			System.out.println("startPosition ("+startPos+")is greater than tablesize ("+tableSize+")");
			System.out.println("please select a new startPosition and try again");
			System.exit(0);
		}
		if(maxCount > tableSize - startPos){
			maxCount = tableSize-startPos;
		}
		//start extraction
		ConfigurableToolExtractor toolExtractor = new ConfigurableToolExtractor();
		toolExtractor.extractTools(executeAtBIBB, startPos, maxCount, tableSize, inputConnection, outputConnection, toolsFile, noToolsFile, contextFile);
	}

}
