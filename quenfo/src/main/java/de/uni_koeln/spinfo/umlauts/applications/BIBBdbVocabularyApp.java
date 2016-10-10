package de.uni_koeln.spinfo.umlauts.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneAbstractClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneKNNClassifier;
import de.uni_koeln.spinfo.umlauts.data.Dictionary;
import de.uni_koeln.spinfo.umlauts.data.JobAd;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dbio.DBConnector;
import de.uni_koeln.spinfo.umlauts.preprocessing.SimpleTokenizer;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class BIBBdbVocabularyApp {
	
		private static Vocabulary fullVoc;
		private static Map<String, HashSet<String>> ambiguities;
		private static Dictionary dict;
		
		
		
	
	
	public static void main() throws ClassNotFoundException, SQLException, IOException{
		
		// /////////////////////////////////////////////
				// /////experiment parameters
				// /////////////////////////////////////////////
				
				
				boolean preClassify = false;
				File outputFolder = new File("umlauts/classification/output/singleResults/preClassified");
				int knnValue = 3;
				boolean ignoreStopwords = false;
				boolean normalizeInput = false;
				boolean useStemmer = false;
				boolean suffixTrees = false;
				int[] nGrams = null; //new int[]{3,4};
				int miScoredFeaturesPerClass = 0;
				Distance distance = Distance.EUKLID;
				ZoneAbstractClassifier classifier = new ZoneKNNClassifier(false, knnValue, distance);//new ZoneRocchioClassifier(false, distance);//new ZoneKNNClassifier(false, knnValue, distance);
				AbstractFeatureQuantifier quantifier = new AbsoluteFrequencyFeatureQuantifier();//new  TFIDFFeatureQuantifier();
				
				// ///////////////////////////////////////////////
				// ////////END///
				// //////////////////////////////////////////////
		
		FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(
				normalizeInput, useStemmer, ignoreStopwords, nGrams, false,
				miScoredFeaturesPerClass, suffixTrees);
		UmlautExperimentConfiguration expConfig = new UmlautExperimentConfiguration(fuc,
				quantifier, classifier, null, "umlauts/classification/output", false, 3,3);
		
		String dbPath = "umlaute_db.db";
		int excludeYear = 2012;
		
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
		
		ambiguities = dict.findAmbiguities(fullVoc);
		FileUtils.printMap(ambiguities, "output//", "allAmbiguities");
		
		// filter Ambiguities 
			// if it is a name (from the names List)
			ambiguities = removeNamesFromAmbiguities(ambiguities);
			// by Proportion
			ambiguities = dict.removeByProportion(ambiguities, fullVoc, 1d);
		
			
		// get Contexts
		Connection connection = DBConnector.connect(dbPath);
		KeywordContexts contexts = new KeywordContexts();
		contexts = DBConnector.getKeywordContextsBibb(connection, dict.createAmbiguitySet(ambiguities), 2012, expConfig);
		
		contexts.printKeywordContexts("output//classification//", "AmbigSentences");
		
		// correct unambiguous words
		
		// classifiy and correct ambiguous words
	}
		
	

	public static Vocabulary extractVocabulary(String dbPath, int excludeYear) throws ClassNotFoundException, SQLException{
	
		Vocabulary fullVoc = new Vocabulary();
		
		
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		Connection connection = DBConnector.connect(dbPath);
		
		connection.setAutoCommit(false);
		String sql ="SELECT ID, ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM DL_ALL_Spinfo WHERE NOT(Jahrgang = '"+excludeYear+"') ";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		JobAd jobAd = null;
		while(result.next()){
			jobAd = new JobAd(result.getInt(3), result.getInt(2), result.getString(4), result.getInt(1));
			List<String> tokens = tokenizer.tokenize(jobAd.getContent());
			fullVoc.addTokens(tokens);
		}
		stmt.close();
		connection.commit();
		
		return fullVoc;
	}
	
	private static HashMap<String,HashSet<String>> removeNamesFromAmbiguities(Map<String, HashSet<String>> ambiguities) throws IOException{

		HashMap<String, HashSet<String>> remainingAmbiguities = new HashMap<String, HashSet<String>>();
		List<String> names = FileUtils.fileToList("output//stats//finalNames.txt");
		
		remainingAmbiguities.putAll(ambiguities);
		System.out.println("Ambiguitäten inkl. Namen: " + remainingAmbiguities.size());
		
		List<String> removed = new ArrayList<String>();
		List<String> toRemove = new ArrayList<String>();
		
		for(String name : names){
			for(String key : ambiguities.keySet()){
				if(ambiguities.get(key).contains(name)) {
					System.out.println(name);
					removed.add(name);
					remainingAmbiguities.remove(key);
				} 
			}
		}
		
		FileUtils.printList(removed, "//output//stats", "removedNames", ".txt");
		System.out.println("Ambiguitäten ohne Namen: " + remainingAmbiguities.size());
		
		return remainingAmbiguities;
	}
	
	
	
}
