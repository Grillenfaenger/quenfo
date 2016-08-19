package de.uni_koeln.spinfo.umlauts.applications;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.umlauts.classification.UmlautClassifyUnit;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.TranslationVocabulary;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dewag.DewacSplitter;
import de.uni_koeln.spinfo.umlauts.dewag.StringsOfInterest;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class DeWacClassificationApp {
	
	/* Ziele
	 * 1. mehr Kontexte zu bekannten ambigen Wörtern
	 * 2. mehr ambige Wörter
	 */
	
	public static void main(String[] args) throws IOException{
		sentenceExtraction();
//		dataExtraction();
//		createContexts();
//		ambiguitiesFilter();
	}
		
	public static void vocExtraction() throws IOException{
		
		UmlautExperimentConfiguration config = new UmlautExperimentConfiguration(null, null, null, null, null, false, 3, 3);
		
		DewacSplitter dewac = new DewacSplitter("output//dewac//");
		
//		for(int i = 1000; i <=2000; i++){
//			dewag.sentencesWithUmlautsToFile(new File("input//dewac//sdewac-v3.tagged_"+i), 10000, i);
//		}
		
		Vocabulary voc = new Vocabulary();
		for(int i = 1000; i <=2000; i++){
			List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//dewac//ofInterest"+i+".txt"));
			for (List<String> list : tokenizedSentences) {
				voc.addTokens(list);
			}
		}
		
//		List<List<String>> tokenizedSentences = dewag.getTokenizedSentences(new File("output//dewac//ofInterest1033.txt"));
//		System.out.println("zum vokabular hinzufügen");
//		for (List<String> list : tokenizedSentences) {
//			voc.addTokens(list);
//		}
		
		System.out.println("Tokens: " + voc.getNumberOfTokens());
		System.out.println("Types: " + voc.vocabulary.size());
		
		
		Vocabulary umlautVoc = voc.getAllByRegex(".*([ÄäÖöÜü]).*");
		System.out.println("Token mit Umlaut: " + umlautVoc.getNumberOfTokens());
		System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
		
		Vocabulary darkVowelVoc = umlautVoc.getAllByRegex(".*([AaOoUu]).*");
		darkVowelVoc.generateNumberOfTokens();
		System.out.println("Wörter mit dunklem Vokal: " + darkVowelVoc.getNumberOfTokens());
		System.out.println("Types mit dunklem Vokal: " + darkVowelVoc.vocabulary.size());
	
		TranslationVocabulary transVoc = new TranslationVocabulary();
		for (String key : umlautVoc.vocabulary.keySet()) {
			transVoc.addEntry(key);
		}
		System.out.println("Vokabular erstellt");
		
		Map<String, HashSet<String>> ambiguities = null;
		
		ambiguities = transVoc.findAmbiguities(voc);
		
		
		System.out.println(ambiguities.size() + " Gruppen mehrdeutiger Wörter gefunden");
		FileUtils.printMap(ambiguities, "output//classification//", "DewacAmbigeWörter2");
		
		
		
	}
	
	public static void sentenceExtraction() throws IOException{
		
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter3.txt");
		DewacSplitter dewac = new DewacSplitter("output//dewac//");
		
		// Kontexte suchen
		// dewac-Splitter funktionen verwenden!
		
		List<String> ofInterest = new ArrayList<String>();
		for(HashSet<String> set : ambiguities.values()){
			for(String word : set){
				ofInterest.add(word);
			}
		}
		StringsOfInterest soi = new StringsOfInterest();
		soi.setStringsOfInterest(ofInterest);
		dewac.sentencesOfInterestToFile2(new File("input//dewac_singlefile//sdewac-v3.tagged"), soi, 100);
		
		
//		List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//dewac//byWord//ofInterest3.txt"));
//		System.out.println("Anzahl der Kontexte insgesamt: " + tokenizedSentences.size());
//		FileUtils.printList(tokenizedSentences, "output//dewac//", "tokenizedSentences3", ".txt");
	}
	
	public static void createContexts() throws IOException{
		KeywordContexts contexts = new KeywordContexts();
		// load
		List<List<String>> tokenizedSentences = FileUtils.fileToListOfLists("output//dewac//tokenizedSentences3.txt");
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter3.txt");
		List<String> ofInterest = new ArrayList<String>();
		for(HashSet<String> set : ambiguities.values()){
			for(String word : set){
				ofInterest.add(word);
			}
		}
		
		for(List<String> tokenizedSentence : tokenizedSentences){
			List<String> tempList = new ArrayList<String>();
			tempList.addAll(ofInterest);
			List<String> tempSentence = new ArrayList<String>();
			tempSentence.addAll(tokenizedSentence);
			tempList.retainAll(tempSentence);
			if(tempList.size() != 0){
				for(String word : tempList){
					contexts.addContext(word, tokenizedSentence);
				}
			}
		}
		contexts.printKeywordContexts("output//classification//", "DewacKontexte3");
//		contexts.printStats();
	}
	
	public static void ambiguitiesFilter() throws IOException{
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter2.txt");
		List<String> stopwords = FileUtils.snowballStopwordReader("input//stop.txt");
		
		Set<String> keys = ambiguities.keySet();
		
		stopwords.retainAll(keys);
		System.out.println(stopwords);
		
		HashMap<String, HashSet<String>> ambigeStopwords = new HashMap<String, HashSet<String>>();
		
		ambigeStopwords.put("andere", ambiguities.get("andere"));
		ambigeStopwords.put("andern", ambiguities.get("andern"));
		ambigeStopwords.put("hatte", ambiguities.get("hatte"));
		ambigeStopwords.put("hatten", ambiguities.get("hatten"));
		ambigeStopwords.put("musste", ambiguities.get("musste"));
		ambigeStopwords.put("waren", ambiguities.get("waren"));
		ambigeStopwords.put("warst", ambiguities.get("warst"));
		
		
		System.out.println(ambiguities.size());
		for (String stopword : stopwords) {
			ambiguities.remove(stopword);
		}
		System.out.println(ambiguities.size());
		FileUtils.printMap(ambiguities, "output//classification//", "DewacAmbigeWörter3");
		FileUtils.printMap(ambigeStopwords, "output//classification//", "DewacAmbigeStopwords");
		
	}

}
