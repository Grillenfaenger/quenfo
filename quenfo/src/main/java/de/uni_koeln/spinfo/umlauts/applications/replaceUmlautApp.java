package de.uni_koeln.spinfo.umlauts.applications;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;

public class replaceUmlautApp {
	
	static String dbPath = "umlaute_db.db";
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		Connection connection = DBConnector.connect(dbPath);
		List<JobAd> jobAds = DBConnector.getJobAds(connection, 2012);
		for (JobAd jobAd : jobAds) {
			
			
		}
	}

}
