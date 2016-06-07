package de.uni_koeln.spinfo.umlauts.dbio;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uni_koeln.spinfo.umlauts.data.Contexts;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;

public class DBConnector {
	
	//Connect to the Database  
	
	/**
	 * @param dbFilePath
	 * @return  connection
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		// register the driver
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		System.out.println("Database " + dbFilePath + " successfully opened");
		return connection;
	}
	
	
	/**
	 *creates an empty database (copy of jobAd_DB in BIBB)
	 * @param connection 
	 * @throws SQLException
	 */
	public static void createBIBBDB(Connection connection) throws SQLException {
		System.out.println("create BIBB_DB");
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "DROP TABLE IF EXISTS DL_ALL_Spinfo";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE DL_ALL_Spinfo (ID  INTEGER PRIMARY KEY AUTOINCREMENT, ZEILENNR INT NOT NULL, Jahrgang INT NOT NULL, STELLENBESCHREIBUNG TEXT)";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}
	
	
	
	/**
	 * read jobAds from DB
	 * 
	 * @param connection
	 * @param jahrgang
	 * @return List of jobAds for the given jahrgang
	 * @throws SQLException
	 */
	public static List<JobAd> getJobAds(Connection connection, int jahrgang) throws SQLException{
		List<JobAd> toReturn = new ArrayList<JobAd>();
		connection.setAutoCommit(false);
		String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE (Jahrgang = '"+jahrgang+"') ";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		JobAd jobAd;
		while(result.next()){
			jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
			toReturn.add(jobAd);
			
		}
		stmt.close();
		connection.commit();
		return toReturn;
	}
	
	public static List<JobAd> getJobAdsExcept(Connection connection, int notjahrgang) throws SQLException{
		List<JobAd> toReturn = new ArrayList<JobAd>();
		connection.setAutoCommit(false);
		String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE NOT(Jahrgang = '"+notjahrgang+"') ";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		JobAd jobAd;
		while(result.next()){
			jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
			toReturn.add(jobAd);
			
		}
		stmt.close();
		connection.commit();
		return toReturn;
	}
	
	
	public static Map<String, ArrayList<List<String>>> getKeywordContexts(Connection connection, Map<String, TreeSet<String>> keywordMap) throws SQLException{
		
		Map<String, ArrayList<List<String>>> toReturn = new TreeMap<String,ArrayList<List<String>>>();
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		JobAd jobAd;
		List<String> tokenList = new ArrayList<String>();
		
		// Separate Untersuchung für jedes Keyword. Weniger effizient aber übersichtlicher
		for(Entry<String, TreeSet<String>> keywordEntry : keywordMap.entrySet()){
			for(String keyword : keywordEntry.getValue()) {
				
				ArrayList<List<String>> contexts = new ArrayList<List<String>>();
				
				System.out.println("\n==============\n Kontexte von "+ keyword+":");
				
				connection.setAutoCommit(false);
				String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE STELLENBESCHREIBUNG LIKE '%"+keyword+"%'";
				Statement stmt = connection.createStatement();
				ResultSet result = stmt.executeQuery(sql);
			
				while(result.next()){
					jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
					tokenList = tokenizer.tokenize(jobAd.getContent());
					
					// die sql-Suche sucht nicht nach tokens sondern Substrings. Zum Ausschluss also noch einmal eine Kontrolle
					if(!tokenList.contains(keyword)){
						continue;
					}
										
					// Kontext +/-3
					int keywordIndex = tokenList.indexOf(keyword);
					int fromIndex = keywordIndex-3;
					int toIndex = keywordIndex+4;
							
					if(fromIndex<0){
						fromIndex = 0;
					}
					if(toIndex>tokenList.size()){
						toIndex = tokenList.size();
					}
						
					List<String> context = new ArrayList<String>(7);
					context = tokenList.subList(fromIndex,toIndex);
					contexts.add(context);
					
					System.out.println(context);
	//				System.out.println(jobAd.getContent());
				
				}
				
				System.out.println("----------\n\n");
				toReturn.put(keyword, contexts);
				
				stmt.close();
				connection.commit();
				
			}
		}
		return toReturn;
	}
/**
 * Wie getKeywordContexts, arbeitet aber mit der neuen Klasse Contexts als Rückgabeklasse statt mit einer Map	
 * @param connection
 * @param keywordMap
 * @return
 * @throws SQLException
 */
public static List<Contexts> getKeywordContexts2(Connection connection, Map<String, TreeSet<String>> keywordMap) throws SQLException{
		
		List<Contexts> contextList = new ArrayList<Contexts>();
		int occurences = 0;
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		JobAd jobAd;
		List<String> tokenList = new ArrayList<String>();
		
		// Separate Untersuchung für jedes Keyword. Weniger effizient aber übersichtlicher
		for(Entry<String, TreeSet<String>> keywordEntry : keywordMap.entrySet()){
			for(String keyword : keywordEntry.getValue()) {
				
				
				ArrayList<List<String>> contexts = new ArrayList<List<String>>();
				
//				System.out.println("\n==============\n Kontexte von "+ keyword+":");
				
				connection.setAutoCommit(false);
				String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE STELLENBESCHREIBUNG LIKE '%"+keyword+"%'";
				Statement stmt = connection.createStatement();
				ResultSet result = stmt.executeQuery(sql);
			
				while(result.next()){
					jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
					tokenList = tokenizer.tokenize(jobAd.getContent());
					
					// die sql-Suche sucht nicht nach tokens sondern Substrings. Zum Ausschluss also noch einmal eine Kontrolle
					if(!tokenList.contains(keyword)){
						continue;
					}
										
					// Kontext +/-3
					int keywordIndex = tokenList.indexOf(keyword);
					int fromIndex = keywordIndex-3;
					int toIndex = keywordIndex+4;
							
					if(fromIndex<0){
						fromIndex = 0;
					}
					if(toIndex>tokenList.size()){
						toIndex = tokenList.size();
					}
						
					List<String> context = new ArrayList<String>(7);
					context = tokenList.subList(fromIndex,toIndex);
					contexts.add(context);
					
//					System.out.println(context);
	//				System.out.println(jobAd.getContent());
				
				}
				
				occurences += contexts.size();
				
//				System.out.println("----------\n\n");
				Contexts contextsObj = new Contexts(keyword, contexts);
				contextList.add(contextsObj);
				
				stmt.close();
				connection.commit();
				
			}
		}
		System.out.println("getKeywordContexts2: " + occurences);
		return contextList;
	}


/**
 * Wie getKeywordContexts2, aber in der Hinsicht performanter, weil es nur einen Durchlauf durch die Datenbank benötigt
 * @param connection
 * @param keywordMap
 * @return
 * @throws SQLException
 */
public static List<Contexts> getKeywordContexts3(Connection connection, Set<String> keywords) throws SQLException{
	
	List<Contexts> contextList = new ArrayList<Contexts>();
	int occurences = 0;
	SimpleTokenizer tokenizer = new SimpleTokenizer();
	JobAd jobAd;
	List<String> tokenList = new ArrayList<String>();
	
	connection.setAutoCommit(false);
	String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo";
	Statement stmt = connection.createStatement();
	ResultSet result = stmt.executeQuery(sql);

	// für jede Anzeige
	while(result.next()){
		jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
		tokenList = tokenizer.tokenize(jobAd.getContent());
	
	// In einer Anzeige können mehrere ambige Wörter ein- oder mehrmals vorkommen. Alle diese Vorkommen und ihre Kontexte sollen gefunden werden.	
		// jedes Keyword
		for(String keyword : keywords) {
			
//			System.out.println("\n==============\n Kontexte von "+ keyword+":");
			
			ArrayList<List<String>> contexts = new ArrayList<List<String>>();
			
			// jedes Token in der Liste
			for(int index = 0; index < tokenList.size(); index++){
				if(keyword.equals(tokenList.get(index))){
					
					// Kontext +/-3 extrahieren
					int fromIndex = index-3;
					int toIndex = index+4;
							
					if(fromIndex<0){
						fromIndex = 0;
					}
					if(toIndex>tokenList.size()){
						toIndex = tokenList.size();
					}
						
					List<String> context = new ArrayList<String>(7);
					context = tokenList.subList(fromIndex,toIndex);
//					System.out.println(context);
					contexts.add(context);
				}
			}
			
			occurences += contexts.size();
			
//			System.out.println("----------\n\n");
			
			if(contexts.size()>0){
				Contexts contextsObj = new Contexts(keyword, contexts);
				int index = contextList.indexOf(contextsObj);
				if(index>-1){
					contextsObj = contextList.get(index);
					contextsObj.addContexts(contexts);
					contextList.set(index, contextsObj);
				} else {
					contextList.add(contextsObj);
				}
			}
		}	
				
	}
			
	stmt.close();
	connection.commit();
			
	System.out.println("getKeywordContexts3: " + occurences);
	return contextList;
}


/**
* Wie getKeywordContexts3, jedoch mit KeywordContexts als Rückgabewert
* @param connection
* @param keywordMap
* @return
* @throws SQLException
*/
public static KeywordContexts getKeywordContexts4(Connection connection, Set<String> keywords) throws SQLException{
	
	KeywordContexts kwCtxts = new KeywordContexts();
	int occurences = 0;
	SimpleTokenizer tokenizer = new SimpleTokenizer();
	JobAd jobAd;
	List<String> tokenList = new ArrayList<String>();
	
	connection.setAutoCommit(false);
	String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo";
	Statement stmt = connection.createStatement();
	ResultSet result = stmt.executeQuery(sql);

	// für jede Anzeige
	while(result.next()){
		jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
		tokenList = tokenizer.tokenize(jobAd.getContent());
	
	// In einer Anzeige können mehrere ambige Wörter ein- oder mehrmals vorkommen. Alle diese Vorkommen und ihre Kontexte sollen gefunden werden.	
		// jedes Keyword
		for(String keyword : keywords) {
			
//			System.out.println("\n==============\n Kontexte von "+ keyword+":");
			
			ArrayList<List<String>> contexts = new ArrayList<List<String>>();
			
			// jedes Token in der Liste
			for(int index = 0; index < tokenList.size(); index++){
				if(keyword.equals(tokenList.get(index))){
					
					// Kontext +/-3 extrahieren
					int fromIndex = index-3;
					int toIndex = index+4;
							
					if(fromIndex<0){
						fromIndex = 0;
					}
					if(toIndex>tokenList.size()){
						toIndex = tokenList.size();
					}
					
					List<String> context = new ArrayList<String>(7);
					context = tokenList.subList(fromIndex,toIndex);
//					System.out.println(context);
					contexts.add(context);
				}
			}
			
			occurences += contexts.size();
			
//			System.out.println("----------\n\n");
			
			if(contexts.size()>0){
				kwCtxts.addContexts(keyword, contexts);
			}
		}	
				
	}
			
	stmt.close();
	connection.commit();
			
	System.out.println("getKeywordContexts4: " + occurences);
	return kwCtxts;
}

public static KeywordContexts getKeywordContexts5(Connection connection, Set<String> keywords) throws SQLException{
	
	KeywordContexts kwCtxts = new KeywordContexts();
	int occurences = 0;
	SimpleTokenizer tokenizer = new SimpleTokenizer();
	JobAd jobAd;
	List<String> tokenList = new ArrayList<String>();
	
	connection.setAutoCommit(false);
	String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo";
	Statement stmt = connection.createStatement();
	ResultSet result = stmt.executeQuery(sql);

	// für jede Anzeige
	while(result.next()){
		jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
		tokenList = tokenizer.tokenize(jobAd.getContent());
	
	// In einer Anzeige können mehrere ambige Wörter ein- oder mehrmals vorkommen. Alle diese Vorkommen und ihre Kontexte sollen gefunden werden.	
		// jedes Keyword
		for(String keyword : keywords) {
			
			List<Integer> indexes = searchKeyword(tokenList,keyword);
			if(indexes!=null){
				List<List<String>> contexts = getContexts(tokenList, indexes);
			
				occurences += contexts.size();
			
//				System.out.println("----------\n\n");
			
				kwCtxts.addContexts(keyword, contexts);
			}
		}	
				
	}
			
	stmt.close();
	connection.commit();
			
	System.out.println("getKeywordContexts5: " + occurences);
	return kwCtxts;
}

private static List<List<String>> getContexts(List<String> text, List<Integer> indexes){
	ArrayList<List<String>> contexts = new ArrayList<List<String>>();
	
	for(Integer index : indexes){
		
//		System.out.println(index);
//		System.out.println(text.get(index));
		
		// Kontext +/-3 extrahieren
		int fromIndex = index-3;
		int toIndex = index+4;
				
		if(fromIndex<0){
			fromIndex = 0;
		}
		if(toIndex>text.size()){
			toIndex = text.size();
		}
		List<String> context = new ArrayList<String>(7);
//		System.out.println(index);
//		System.out.println(text.get(index));
//		System.out.println(fromIndex + ", "+ toIndex);
		context = text.subList(fromIndex,toIndex);
		contexts.add(context);
		
	}
	
	return contexts;
}

private static List<Integer> searchKeyword(List<String> text, String keyword){
	List<Integer> indexes = new ArrayList<Integer>();
	
	int first = text.indexOf(keyword);
	int last = text.lastIndexOf(keyword);
	
	if(first==-1){
		return null;
	} else if(first==last){
		indexes.add(first);
		return indexes;
	} else {
		indexes.add(first);
		indexes.add(last);
		searchKeyword(text,keyword,first,last,indexes);
	}
	
	return indexes;
}
	
private static List<Integer> searchKeyword(List<String> text, String keyword, int left, int right, List<Integer> indexes) {
	
	// Wie suche ich jetzt nur in der SubList, erhalte aber die Indexe der Token?
	
	
	int first = text.subList(left+1, right).indexOf(keyword);
	
	if(first==-1){
		return indexes;
	} else {
		first = first+left+1;
		int last = text.subList(left+1, right).lastIndexOf(keyword)+left+1;

		System.out.println("left im ganzen Text: " + left + ": " + text.get(left));
		System.out.println("first im ganzen Text:"+ first + ": " + text.get(first));
		System.out.println("last im ganzen Text:"+ last + ": " + text.get(last));
		System.out.println("right im ganzen Text:"+ right + ": " + text.get(right));
		
		if(first==last){
			indexes.add(first);
			return indexes;
		} else {
			indexes.add(first);
			indexes.add(last);
			searchKeyword(text,keyword,first,last,indexes);
		}
	}
	return indexes;
}


	public static List<JobAd> getJobAdsWithKeyword(Connection connection, Set<String> keywords) throws SQLException{
		List<JobAd> toReturn = new ArrayList<JobAd>();
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		
		connection.setAutoCommit(false);
		String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		JobAd jobAd;
		List<String> tokenList = new ArrayList<String>();
		boolean containsKeyword = false;
		while(result.next()){
			jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
			tokenList = tokenizer.tokenize(jobAd.getContent());
			for(String keyword : keywords) {
				// Problem mit contains - es ignoriert 
				if(tokenList.contains(keyword)) {
					containsKeyword = true;
					// ambige Wörter sollten noch markiert werden - am besten alle, falls mehrere in einem Paragrafen vorkommen
					jobAd.setContent(jobAd.getContent().replace(keyword, "$$$ "+ keyword + "$$$"));
					int keywordIndex = tokenList.indexOf(keyword);
					
					
					// Kontext +/-2
					int fromIndex = keywordIndex-3;
					int toIndex = keywordIndex+3;
					
					if(fromIndex<0){
						fromIndex = 0;
					}
					if(toIndex>tokenList.size()-1){
						toIndex = tokenList.size()-1;
					}
						
//					System.out.println(tokenList.subList(fromIndex,toIndex));
				
					
				}
			}
			if (containsKeyword == true) {
				toReturn.add(jobAd);
//				System.out.println(jobAd.getContent());
			}
		}
		stmt.close();
		connection.commit();
		return toReturn;
	}

}
