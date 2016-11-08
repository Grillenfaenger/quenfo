package de.uni_koeln.spinfo.umlauts.applications;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.umlauts.data.Dictionary;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class BibbVocabularyBuilder {
	
	String dbPath;	
	UmlautExperimentConfiguration expConfig;
	int excludeYear;
	
	Vocabulary fullVoc;
	Map<String, HashSet<String>> ambiguities;
	Dictionary dict;
	//KeywordContexts contexts;
	
	public BibbVocabularyBuilder(String dbPath, UmlautExperimentConfiguration expConfig, int excludeYear){
		this.dbPath = dbPath;
		this.expConfig = expConfig;
		this.excludeYear = excludeYear;
	}
	
	public Dictionary buildDictionary() throws ClassNotFoundException, SQLException, IOException {
		// extract Vocabulary
		fullVoc = extractVocabulary(dbPath, excludeYear);
		System.out.println("Tokens: " + fullVoc.getNumberOfTokens());
		System.out.println("Types: " + fullVoc.vocabulary.size());
		FileUtils.printMap(fullVoc.vocabulary, "output//", "SteADBVocabulary");
		
		// reduce Vocabulary to Umlaut words
		Vocabulary umlautVoc = fullVoc.getAllByRegex(".*([ÄäÖöÜüß]).*");
		umlautVoc.generateNumberOfTokens();
		System.out.println("Wörter mit Umlaut: " + umlautVoc.getNumberOfTokens());
		System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
		
		// for Statistics: words with dark Vowels
		Vocabulary darkVowelVoc = fullVoc.getAllByRegex(".*([AaOoUu]).*");
		darkVowelVoc.generateNumberOfTokens();
		System.out.println("Wörter mit dunklem Vokal: " + darkVowelVoc.getNumberOfTokens());
		System.out.println("Types mit dunklem Vokal: " + darkVowelVoc.vocabulary.size());
		
		// create Dictionary for correcting
		dict = new Dictionary(umlautVoc);
		
		dict.printToFile("output//bibb//", "bibbDictionary");
		return dict;
	}
	
	public Map<String, HashSet<String>> findAmbiguities() throws IOException{
		ambiguities = dict.findAmbiguities(fullVoc);
		FileUtils.printMap(ambiguities, "output//bibb//", "allAmbiguities");
		
		// filter Ambiguities 
			// if it is a name (from the names List)
			ambiguities = dict.removeNamesFromAmbiguities(ambiguities);
			// by Proportion
			ambiguities = dict.removeByProportion(ambiguities, fullVoc, 1d);
			
			FileUtils.printMap(ambiguities, "output//bibb//", "bibbFilteredAmbiguities");
			
		return ambiguities;
	}
	
	public Vocabulary extractVocabulary(String dbPath, int excludeYear) throws ClassNotFoundException, SQLException, IOException{
		
		Vocabulary voc = new Vocabulary();
		
//		SimpleTokenizer tokenizer = new SimpleTokenizer();
		IETokenizer ietokenizer = new IETokenizer();
		Connection connection = DBConnector.connect(dbPath);
		
		connection.setAutoCommit(false);
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE NOT(Jahrgang = '"+excludeYear+"') ";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		JobAd jobAd = null;
		while(result.next()){
			jobAd = new JobAd(result.getInt(3), result.getInt(2), result.getString(4), result.getInt(1));
			String[] tokens = ietokenizer.tokenizeSentence(jobAd.getContent());
			voc.addTokens(Arrays.asList(tokens));
		}
		stmt.close();
		connection.commit();
		
		fullVoc = voc;
		voc.saveVocabularyToFile("output//bibb//", "bibbVocabulary.txt");
		return voc;
	}
	
	public void compareVocabulary(boolean extendDictionary, boolean extendAmbiguities) throws IOException {
		
		// load sDewac Dictionary
		Dictionary dewacDic = new Dictionary();
		dewacDic.loadDictionary("output//dewac//DewacDictionary.txt");
		
		System.out.println("sDewac Dictionary: " + dewacDic.dictionary.size());
		System.out.println("bibb Dictionary: " + dict.dictionary.size());
		
		
		// wörter, die nur im dewacDic enthalten sind
		for(String key : dict.dictionary.keySet()){
			if(dewacDic.dictionary.containsKey(key)){
				dewacDic.dictionary.remove(key);
			}
		}
		
		System.out.println("sDewac Dictionary exklusive Wörter: " + dewacDic.dictionary.size());
		
		if(extendDictionary == true){
			dict.addEntries(dewacDic.dictionary);
		}
		
		// load sDewac ambiguities
		HashMap<String, HashSet<String>> dewacAmbiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter4.txt");
		
		// work with copies!!! deleteAll??
		HashMap<String, HashSet<String>> ambiguitiesCopy = new HashMap<String, HashSet<String>>();
		ambiguitiesCopy.putAll(ambiguities);
		
		System.out.println("Ambige Wörter im sDewac-Korpus: " + dewacAmbiguities.size());
		System.out.println("Ambige Wörter in der Datenbank: " + ambiguitiesCopy.size());
		
		Set<String> dewacKeyset = new HashSet<String>();
		dewacKeyset.addAll(dewacAmbiguities.keySet());
		
		for(String key : dewacKeyset){
			if(ambiguitiesCopy.containsKey(key)){
				ambiguitiesCopy.remove(key);
				dewacAmbiguities.remove(key);
			}	
		}
		
		System.out.println("Einzigartige ambige Wörter im sDewac-Korpus: " + dewacAmbiguities.size());
		System.out.println("Einzigartige ambige Wörter in der Datenbank: " + ambiguitiesCopy.size());
		
		if(extendAmbiguities == true){
			ambiguities.putAll(dewacAmbiguities);
		}
	}

	public KeywordContexts getContexts(boolean getContextsFromDewac) throws ClassNotFoundException, SQLException, IOException {
		// get Contexts
			Connection connection = DBConnector.connect(dbPath);
			KeywordContexts cont = new KeywordContexts();
			cont = DBConnector.getKeywordContextsBibb(connection, dict.createAmbiguitySet(ambiguities), 2012, expConfig);
			
			cont.printKeywordContexts("output//classification//", "AmbigSentences");
	
		if(getContextsFromDewac){
			KeywordContexts dewacContexts = new KeywordContexts();
			dewacContexts.loadKeywordContextsFromFile("ouput//dewac//DewacKontexte.txt");
			
			for(String key : cont.keywordContextsMap.keySet()){
				int size = cont.keywordContextsMap.get(key).size();
				if(size < 100){
					if(dewacContexts.keywordContextsMap.get(key).size() > 100){
						cont.addContexts(key, dewacContexts.getContext(key).subList(0, 99-size));
					} else {
						cont.addContexts(key, dewacContexts.getContext(key));
					}
				}
			}
		}
		cont.printKeywordContexts("output//bibb//", "BibbKontexte");
		return cont;
	}
	
	private List<String> extractContext(List<String> text, int index, int left, int right){
		
		int fromIndex = index-left;
		int toIndex = index+right;
				
		if(fromIndex<0){
			fromIndex = 0;
		}
		if(toIndex>text.size()){
			toIndex = text.size();
		}
		return text.subList(fromIndex,toIndex);
	}

}
