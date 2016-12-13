package de.uni_koeln.spinfo.umlauts.dbio;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;

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
	 *creates an empty database (copy of jobAd_DB in BIBB)
	 * @param connection 
	 * @throws SQLException
	 */
	public static void createBIBBDBcorrected(Connection connection) throws SQLException {
		System.out.println("create BIBB_DB");
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "DROP TABLE IF EXISTS DL_ALL_Spinfo";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE DL_ALL_Spinfo (ID  INTEGER PRIMARY KEY AUTOINCREMENT, OrigID INT NOT NULL, ZEILENNR INT NOT NULL, Jahrgang INT NOT NULL, STELLENBESCHREIBUNG TEXT, CORRECTED TEXT)";
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
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE (Jahrgang = '"+jahrgang+"') ";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		JobAd jobAd;
		while(result.next()){
			jobAd = new JobAd(result.getInt(3), result.getInt(2), result.getString(4), result.getInt(1));
			toReturn.add(jobAd);
			
		}
		stmt.close();
		connection.commit();
		return toReturn;
	}
	
	public static List<JobAd> getJobAdsExcept(Connection connection, int notjahrgang) throws SQLException{
		List<JobAd> toReturn = new ArrayList<JobAd>();
		connection.setAutoCommit(false);
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE NOT(Jahrgang = '"+notjahrgang+"') ";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		JobAd jobAd;
		while(result.next()){
			jobAd = new JobAd(result.getInt(3), result.getInt(2), result.getString(4), result.getInt(1));
			toReturn.add(jobAd);
			
		}
		stmt.close();
		connection.commit();
		return toReturn;
	}
	


	/**
	 * Returns contexts of keywords within the sentence of occurence.
	 * @param connection
	 * @param keywords
	 * @return
	 * @throws SQLException
	 */
	public static KeywordContexts getKeywordContexts6(Connection connection, Set<String> keywords) throws SQLException{
		
		KeywordContexts kwCtxts = new KeywordContexts();
		int occurences = 0;
		JobAd jobAd;
		List<List<String>> tokenizedSentences = new ArrayList<List<String>>(); 
		IETokenizer ietokenizer = new IETokenizer();
		ArrayList<JobAd> candidates = new ArrayList<JobAd>();
		
		connection.setAutoCommit(false);
		String sql ="SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
	
		// für jede Anzeige
		while(result.next()){
			jobAd = new JobAd(result.getInt(1), result.getInt(2), result.getString(3));
			candidates.add(jobAd);
		}
		System.out.println("Es bleiben " + candidates.size() + " Kandidaten");
			
			
		for(JobAd cand : candidates){	
			List<String> sentences = ietokenizer.splitIntoSentences(cand.getContent(), false);
			List<String> tokenizedSentence = null;
			for (String sentence : sentences){
				tokenizedSentence = Arrays.asList(ietokenizer.tokenizeSentence(sentence));
				tokenizedSentences.add(tokenizedSentence);
			}
		}	
		
		System.out.println("Sätze insgesamt: "+tokenizedSentences.size());	
		
		// In einem Satz können mehrere ambige Wörter ein- oder mehrmals vorkommen. Alle diese Vorkommen und ihre Kontexte sollen gefunden werden.	
			// jedes Keyword
		for(List<String> sentencetokens : tokenizedSentences){	
			for(String keyword : keywords) {
	//			System.out.println("Suche nach " + keyword);
				
				
				List<Integer> indexes = searchKeyword(sentencetokens,keyword);
				if(indexes!=null){
					List<List<String>> contexts = getContexts(sentencetokens, indexes);
				
					occurences += contexts.size();
				
	//				System.out.println("----------\n\n");
				
					kwCtxts.addContexts(keyword, contexts);
				}
			}	
		}	
						
		stmt.close();
		connection.commit();
				
		System.out.println("getKeywordContexts6: " + occurences);
		return kwCtxts;
	}
	
	public static KeywordContexts getKeywordContextsBibb(Connection connection, Set<String> keywords, int notjahrgang, UmlautExperimentConfiguration config) throws SQLException{
		
		KeywordContexts kwCtxts = new KeywordContexts();
		int occurences = 0;
		boolean storeFullSentences = config.getStoreFullSentences();
		JobAd jobAd;
		IETokenizer ietokenizer = new IETokenizer();
		
		connection.setAutoCommit(false);
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE NOT(Jahrgang = '"+notjahrgang+"') ";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
	
		// for each JobAd
		while(result.next()){
			jobAd = new JobAd(result.getInt(3), result.getInt(2), result.getString(4), result.getInt(1));
			List<String> sentences = ietokenizer.splitIntoSentences(jobAd.getContent(), false);
			List<List<String>> tokenizedSentences = new ArrayList<List<String>>(); 
			
			for (String sentence : sentences){
				List<String> tokenizedSentence = Arrays.asList(ietokenizer.tokenizeSentence(sentence));
				tokenizedSentences.add(tokenizedSentence);	
			}
			
			// Kontrolle
			if(jobAd.getContent().contains("Abendbuffets")){
				System.out.println(jobAd.getContent());
				System.out.println(sentences);
				System.out.println(tokenizedSentences);
			}
			
			
			for(List<String> sentencetokens : tokenizedSentences){	
				for (int i = 0; i < sentencetokens.size(); i++) {
					String token = sentencetokens.get(i);
					if(keywords.contains(token)){
						occurences++;
						if(!storeFullSentences){
							List<String> context = getContext(i, sentencetokens, config);
							kwCtxts.addContext(token, context);
						} else {
							kwCtxts.addContext(token, sentencetokens);
						}
					}
				}
			}	
		}	
		stmt.close();
		connection.commit();
				
		System.out.println("occurences in getKeywordContextsBibb: " + occurences);
		return kwCtxts;
	}
	
	
	public static KeywordContexts getKeywordContexts_new(List<JobAd> jobAds, Set<String> keywords, UmlautExperimentConfiguration config) throws SQLException{
		
		KeywordContexts kwCtxts = new KeywordContexts();
		List<List<String>> tokenizedSentences = new ArrayList<List<String>>(); 
		IETokenizer ietokenizer = new IETokenizer();
		
		for(JobAd cand : jobAds){	
			List<String> sentences = ietokenizer.splitIntoSentences(cand.getContent(), false);
			List<String> tokenizedSentence = null;
			for (String sentence : sentences){
				tokenizedSentence = Arrays.asList(ietokenizer.tokenizeSentence(sentence));
				tokenizedSentences.add(tokenizedSentence);
			}
		}	
		
		for(List<String> sentencetokens : tokenizedSentences){	
			for (int i = 0; i < sentencetokens.size(); i++) {
				String token = sentencetokens.get(i);
				if(keywords.contains(token)){
					if(config.getStoreFullSentences() == false){
						List<String> context = getContext(i, sentencetokens, config);
						kwCtxts.addContext(token, context);
					}
				}
			}
		}	
		return kwCtxts;
	}
	
	private static List<String> getContext(int i, List<String> text,
			UmlautExperimentConfiguration config) {
		
		int fromIndex = i-config.getContextBefore();
		int toIndex = i+config.getContextAfter()+1;
				
		if(fromIndex<0){
			fromIndex = 0;
		}
		if(toIndex>text.size()){
			toIndex = text.size();
		}
		List<String> context = new ArrayList<String>(config.getContextAfter()+config.getContextBefore()+1);
		context = text.subList(fromIndex,toIndex);
		return context;
	}
	
	
	private static List<List<String>> getContexts(List<String> text, List<Integer> indexes){
		ArrayList<List<String>> contexts = new ArrayList<List<String>>();
		
		for(Integer index : indexes){
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
	
	public static void removeUmlautsFromDB(Connection connection, List<JobAd> jobAds) throws SQLException{
			
		connection.setAutoCommit(true);
		String sql ="UPDATE DL_ALL_Spinfo set STELLENBESCHREIBUNG = ? WHERE ID = ? ";
		PreparedStatement preparedStmt = connection.prepareStatement(sql);
		 
		 for (JobAd jobAd : jobAds){
			 String content = jobAd.getContent();
	//			 System.out.println(content);
			 String replacement = replaceUmlaut(content);
	//			 System.out.println(content);
			 preparedStmt.setString(1, replacement);
			 preparedStmt.setInt(2, jobAd.getId());
			 preparedStmt.executeUpdate();
		}
	}
		
	public static String replaceUmlaut(String umlautWord) {
		String replacement = umlautWord;
		replacement = replacement.replaceAll("Ä", "A");
		replacement = replacement.replaceAll("ä", "a");
		replacement = replacement.replaceAll("Ö", "O");
		replacement = replacement.replaceAll("ö", "o");
		replacement = replacement.replaceAll("Ü", "U");
		replacement = replacement.replaceAll("ü", "u");
		replacement = replacement.replaceAll("ß", "ss");
		
		return replacement;
	}

}
