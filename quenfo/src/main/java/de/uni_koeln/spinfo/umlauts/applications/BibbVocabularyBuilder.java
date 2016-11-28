package de.uni_koeln.spinfo.umlauts.applications;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
	
	public Vocabulary fullVoc;
	public Map<String, HashSet<String>> ambiguities;
	public Dictionary dict;
	//KeywordContexts contexts;
	
	public BibbVocabularyBuilder(String dbPath, UmlautExperimentConfiguration expConfig, int excludeYear){
		this.dbPath = dbPath;
		this.expConfig = expConfig;
		this.excludeYear = excludeYear;
	}
	
	public Dictionary buildDictionary(boolean useExternalVocabulary) throws ClassNotFoundException, SQLException, IOException {
		// extract Vocabulary
		fullVoc = extractVocabulary(dbPath, excludeYear);
		System.out.println("Tokens: " + fullVoc.getNumberOfTokens());
		System.out.println("Types: " + fullVoc.vocabulary.size());
		FileUtils.printMap(fullVoc.vocabulary, "output//", "SteADBVocabulary");
		
		if(useExternalVocabulary){
			// load voc
			Vocabulary dewacVoc = new Vocabulary();
			HashMap<String,String> loadVoc = FileUtils.fileToMap("output//dewac//DewacVoc.txt");
			HashMap<String,Integer> vocabulary = new HashMap<String,Integer>();
			for(String key : loadVoc.keySet()){
				vocabulary.put(key, Integer.valueOf(loadVoc.get(key)));
			}
			dewacVoc.setVocabulary(vocabulary);
			System.out.println("dewac Tokens: " + dewacVoc.getNumberOfTokens());
			System.out.println("dewac Types: " + dewacVoc.vocabulary.size());
			
			fullVoc.mergeVocabularies(dewacVoc);
			System.out.println("Tokens: " + fullVoc.getNumberOfTokens());
			System.out.println("Types: " + fullVoc.vocabulary.size());
			FileUtils.printMap(fullVoc.vocabulary, "output//", "mergedVocabulary");
		}
		
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
		
		return dict;
	}
	
	public Map<String, HashSet<String>> findAmbiguities(boolean filterByProportion, boolean filterNames) throws IOException{
		ambiguities = dict.findAmbiguities(fullVoc);
		FileUtils.printMap(ambiguities, "output//bibb//", "allAmbiguities");
		
		// filter Ambiguities 
			// by Proportion
			if(filterByProportion){
				ambiguities = dict.removeByProportion(ambiguities, fullVoc, 1d);
			}
			
			// if it is a name (from the names List)
			if(filterNames){
				ambiguities = dict.removeNamesFromAmbiguities(ambiguities);
			}
			
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
		voc.saveVocabularyToFile("output//bibb//", "bibbVocabulary");
		return voc;
	}
	
	public void compareVocabulary() throws IOException {
		
		// load sDewac Dictionary
		Dictionary dewacDic = new Dictionary();
		dewacDic.loadDictionary("output//dewac//DewacDictionary2.txt");
		
		System.out.println("sDewac Dictionary: " + dewacDic.dictionary.size());
		System.out.println("bibb Dictionary: " + dict.dictionary.size());
		
		
		// wörter, die nur im dewacDic enthalten sind
		for(String key : dict.dictionary.keySet()){
			if(dewacDic.dictionary.containsKey(key)){
				dewacDic.dictionary.remove(key);
			}
		}
		System.out.println("sDewac Dictionary exklusive Wörter: " + dewacDic.dictionary.size());
		
		// load sDewac ambiguities
		HashMap<String, HashSet<String>> dewacAmbiguities = FileUtils.fileToAmbiguities("output//dewac//DewacAmbiguities6.txt");
		
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
	}

	public KeywordContexts getContexts() throws ClassNotFoundException, SQLException, IOException {
		// get Contexts
		Connection connection = DBConnector.connect(dbPath);
		KeywordContexts cont = new KeywordContexts();
		
		cont = DBConnector.getKeywordContextsBibb(connection, dict.createAmbiguitySet(ambiguities), 2012, expConfig);
		
		cont.printKeywordContexts("output//bibb//", "BibbKontexte");
		return cont;
	}
	
	public KeywordContexts extendByDewacContexts(KeywordContexts cont) throws IOException{
		KeywordContexts dewacContexts = new KeywordContexts();
		dewacContexts = dewacContexts.loadKeywordContextsFromFile("output//dewac//DewacKontexte.txt");
		
		// TODO run over ambiguites, not Contexts!
		Set<String> ambiguitySet = dict.createAmbiguitySet(ambiguities);
		
		for(String key : ambiguitySet){
			if(!cont.keywordContextsMap.containsKey(key)){
				cont.addContext(key, new ArrayList<String>());
			}
			int size = cont.keywordContextsMap.get(key).size();
			if(size < 100){
				System.out.println("Add contexts from Dewac for " + key + ", " + size + " Kontexte");
				if(dewacContexts.keywordContextsMap.containsKey(key)){
					if(dewacContexts.keywordContextsMap.get(key).size() > 100){
						
						cont.addContexts(key, dewacContexts.getContext(key).subList(0, 100-size));
					} else {
						cont.addContexts(key, dewacContexts.getContext(key));
					}
				}
				System.out.print(", neue Anzahl: " + cont.keywordContextsMap.get(key).size() + "\n");
			}
		}
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
