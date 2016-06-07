package de.uni_koeln.spinfo.umlauts.dbio;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.uni_koeln.spinfo.umlauts.data.JobAd;


public class DBConnectorTest {
	
	public static Connection connection;
	
	@BeforeClass
	public static void initialize() throws ClassNotFoundException, SQLException{
		connection = DBConnector.connect("umlaute_db.db");
	}

	@Ignore
	@Test
	public void testGetJobAds() {
		fail("Not yet implemented");
	}

	
	@Test
	public void testGetJobAdsExcept() throws SQLException {
		List<JobAd> jobAds = DBConnector.getJobAdsExcept(connection, 2012);
		for(JobAd jobAd : jobAds){
			System.out.println("_______");
			System.out.println(jobAd.getContent());
			System.out.println("_______");
		}
		fail("Not yet implemented");
	}

	@Ignore
	@Test
	public void testGetKeywordContexts5() {
		fail("Not yet implemented");
	}

}
