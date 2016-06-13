package de.uni_koeln.spinfo.umlauts.applications;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;

public class replaceUmlautApp {
	
	static String dbPath = "replaceUmlaute_db.db";
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		Connection connection = DBConnector.connect(dbPath);
		List<JobAd> jobAds = DBConnector.getJobAds(connection, 2012);
		
//		System.out.println(jobAds);
		
		DBConnector.removeUmlautsFromDB(connection, jobAds);
		connection.close();
		
		connection = DBConnector.connect(dbPath);
		
		List<JobAd> jobAds2 = DBConnector.getJobAds(connection, 2012);
		
		System.out.println(jobAds2);
		
		
			
			
	}
}


