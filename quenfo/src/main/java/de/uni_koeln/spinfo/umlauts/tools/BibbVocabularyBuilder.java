package de.uni_koeln.spinfo.umlauts.tools;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
	
	public Dictionary buildDictionary(boolean useExternalVocabulary, String externalVoc) throws ClassNotFoundException, SQLException, IOException {
		
		System.out.println("build dictionary");
		
		List<String> log = new ArrayList<String>();
		
		// extract Vocabulary
		fullVoc = extractVocabulary(dbPath, excludeYear);
		log.add("Tokens: " + fullVoc.getNumberOfTokens());
		log.add("Types: " + fullVoc.vocabulary.size());
//		FileUtils.printMap(fullVoc.vocabulary, "output//", "SteADBVocabulary");
		
		if(useExternalVocabulary){
			// load voc
			System.out.println("extend vocabulary with sdewac voc");
			Vocabulary dewacVoc = new Vocabulary();
			HashMap<String,String> loadVoc = FileUtils.fileToMap(externalVoc);
			HashMap<String,Integer> vocabulary = new HashMap<String,Integer>();
			for(String key : loadVoc.keySet()){
				vocabulary.put(key, Integer.valueOf(loadVoc.get(key)));
			}
			dewacVoc.setVocabulary(vocabulary);
			log.add("dewac Tokens: " + dewacVoc.getNumberOfTokens());
			log.add("dewac Types: " + dewacVoc.vocabulary.size());
			
			fullVoc.mergeVocabularies(dewacVoc);
			log.add("total Tokens: " + fullVoc.getNumberOfTokens());
			log.add("total Types: " + fullVoc.vocabulary.size());
//			FileUtils.printMap(fullVoc.vocabulary, "output//", "mergedVocabulary");
		}
		
		// reduce Vocabulary to Umlaut words
		Vocabulary umlautVoc = fullVoc.getAllByRegex(".*([ÄäÖöÜüß]).*");
		umlautVoc.generateNumberOfTokens();
		log.add("Token with Umlaut: " + umlautVoc.getNumberOfTokens());
		log.add("Types with Umlaut: " + umlautVoc.vocabulary.size());
		
		// for Statistics: words with dark Vowels
		Vocabulary darkVowelVoc = fullVoc.getAllByRegex(".*([AaOoUu]).*");
		darkVowelVoc.generateNumberOfTokens();
		log.add("Token with dark vowels: " + darkVowelVoc.getNumberOfTokens());
		log.add("Types with dark vowels: " + darkVowelVoc.vocabulary.size());
		
		// create Dictionary for correcting
		System.out.println("create dictionary");
		dict = new Dictionary(umlautVoc);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		FileUtils.printList(log, "output//logs//", "VocLog"+sdf.format(new Timestamp(System.currentTimeMillis())), ".txt");
		
		return dict;
	}
	
	public Map<String, HashSet<String>> findAmbiguities(boolean filterByProportion, double filterMeasure, boolean filterNames) throws IOException{
		System.out.println("find ambiguities");
		ambiguities = dict.findAmbiguities(fullVoc);
		FileUtils.printMap(ambiguities, "output//bibb//", "allAmbiguities");
		
		// filter Ambiguities 
		System.out.println("filter ambiguities");
			// by Proportion
			if(filterByProportion){
				ambiguities = dict.removeByProportion(ambiguities, fullVoc, filterMeasure);
			}
			
			// if it is a name (from the names List)
			if(filterNames){
				ambiguities = dict.removeNamesFromAmbiguities(ambiguities);
			}
			
//			FileUtils.printMap(ambiguities, "output//bibb//", "bibbFilteredAmbiguities");
			
		return ambiguities;
	}
	
	public Vocabulary extractVocabulary(String dbPath, int excludeYear) throws ClassNotFoundException, SQLException, IOException{
		
		System.out.println("extract vocabulary");
		
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
//		voc.saveVocabularyToFile("output//bibb//", "bibbVocabulary");
		return voc;
	}
	
	public void compareVocabulary() throws IOException {
		
		System.out.println("compare vocabulary");
		
		List<String> log = new ArrayList<String>();
		
		// load sDewac Dictionary
		Dictionary dewacDic = new Dictionary();
		dewacDic.loadDictionary("output//dewac//DewacDictionary.txt");
		
		log.add("sDewac Dictionary: " + dewacDic.dictionary.size());
		log.add("bibb Dictionary: " + dict.dictionary.size());
		
		
		// wörter, die nur im dewacDic enthalten sind
		for(String key : dict.dictionary.keySet()){
			if(dewacDic.dictionary.containsKey(key)){
				dewacDic.dictionary.remove(key);
			}
		}
		log.add("sDewac exclusive Words: " + dewacDic.dictionary.size());
		
		// load sDewac ambiguities
		HashMap<String, HashSet<String>> dewacAmbiguities = FileUtils.fileToAmbiguities("output//dewac//DewacAmbiguities6.txt");
		
		// work with copies!!! deleteAll??
		HashMap<String, HashSet<String>> ambiguitiesCopy = new HashMap<String, HashSet<String>>();
		ambiguitiesCopy.putAll(ambiguities);
		
		log.add("ambiguous words in sdewac: " + dewacAmbiguities.size());
		log.add("ambiguous words in db: " + ambiguitiesCopy.size());
		
		Set<String> dewacKeyset = new HashSet<String>();
		dewacKeyset.addAll(dewacAmbiguities.keySet());
		
		for(String key : dewacKeyset){
			if(ambiguitiesCopy.containsKey(key)){
				ambiguitiesCopy.remove(key);
				dewacAmbiguities.remove(key);
			}	
		}
		
		log.add("sDewac exclusive Ambiguities: " + dewacAmbiguities.size());
		log.add("db exclusive Ambiguities: " + ambiguitiesCopy.size());
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		FileUtils.printList(log, "output//logs//", "CompareLog"+sdf.format(new Timestamp(System.currentTimeMillis())), ".txt");
	}

	public KeywordContexts getContexts() throws ClassNotFoundException, SQLException, IOException {
		
		System.out.println("get contexts");
		
		// get Contexts
		Connection connection = DBConnector.connect(dbPath);
		KeywordContexts cont = new KeywordContexts();
		
		cont = DBConnector.getKeywordContextsBibb(connection, dict.createAmbiguitySet(ambiguities), 2012, expConfig);
		
//		cont.printKeywordContexts("output//bibb//", "BibbKontexte");
		return cont;
	}
	
	public KeywordContexts extendByDewacContexts(KeywordContexts cont) throws IOException{
		
		System.out.println("extend contexts");
		
		KeywordContexts dewacContexts = new KeywordContexts();
		dewacContexts = dewacContexts.loadKeywordContextsFromFile("output//dewac//DewacKontexte.txt");
		
		Set<String> ambiguitySet = dict.createAmbiguitySet(ambiguities);
		
		for(String key : ambiguitySet){
			if(!cont.keywordContextsMap.containsKey(key)){
				cont.addContext(key, new ArrayList<String>());
			}
			int size = cont.keywordContextsMap.get(key).size();
			if(size < 100){
				System.out.print("Add contexts from Dewac for " + key + ", " + size + " contexts");
				if(dewacContexts.keywordContextsMap.containsKey(key)){
					if(dewacContexts.keywordContextsMap.get(key).size() > 100){
						
						cont.addContexts(key, dewacContexts.getContext(key).subList(0, 100-size));
					} else {
						cont.addContexts(key, dewacContexts.getContext(key));
					}
				}
				System.out.print(", new count: " + cont.keywordContextsMap.get(key).size() + "\n");
			}
		}
		return cont;
	}

}
