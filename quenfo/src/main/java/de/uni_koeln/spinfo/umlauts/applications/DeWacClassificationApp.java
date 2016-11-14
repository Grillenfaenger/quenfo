package de.uni_koeln.spinfo.umlauts.applications;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import de.uni_koeln.spinfo.umlauts.data.Dictionary;
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
//		vocExtraction();
//		onlyVocExtraction();
//		moreStatsAboutContexts();
//		sentenceExtraction();
//		dataExtraction();
//		createContexts();
//		ambiguitiesFilter();
//		removeNamesFromAmbiguities();
//		nameFinder();
//		showContextsOfNames();
//		removeNamesFromContexts();
//		createFinalNamesList();
//		decisionByRatioStats();
		cleanDictionary();
	}
		
	private static void cleanDictionary() throws IOException {
		
		//load
		// load voc
		Vocabulary voc = new Vocabulary();
		HashMap<String,String> loadVoc = FileUtils.fileToMap("output//dewac//DewacVoc.txt");
		HashMap<String,Integer> vocabulary = new HashMap<String,Integer>();
		for(String key : loadVoc.keySet()){
			vocabulary.put(key, Integer.valueOf(loadVoc.get(key)));
		}
		voc.setVocabulary(vocabulary);
		// load Dictionary
		Dictionary dict = new Dictionary();
		dict.loadDictionary("output//dewac//DewacDictionary.txt");
		// load ambiguities
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//dewac//DewacAmbiguities5.txt");
		
		
		System.out.println("das: " + voc.getOccurenceOf("das"));
		System.out.println("däs: " + voc.getOccurenceOf("däs"));
		
		System.out.println("das: " + ambiguities.get("das"));
		
		//clean
		
		System.out.println(dict.dictionary.size());
		
		ambiguities = dict.removeNamesFromAmbiguities(ambiguities);
		ambiguities = dict.removeByProportion(ambiguities, voc, 1d);
		
		
		System.out.println("das: " + dict.dictionary.get("das"));
		
		System.out.println(dict.dictionary.size());
		dict.printToFile("output//dewac//", "dewacDictionary2");
		
		FileUtils.printMap(ambiguities, "output//dewac//", "DewacAmbiguities6");
	}

	private static void decisionByRatioStats() throws IOException {
		// load voc
		Vocabulary voc = new Vocabulary();
		HashMap<String,String> loadVoc = FileUtils.fileToMap("output//stats//DewacVoc.txt");
		HashMap<String,Integer> vocabulary = new HashMap<String,Integer>();
		for(String key : loadVoc.keySet()){
			vocabulary.put(key, Integer.valueOf(loadVoc.get(key)));
		}
		voc.setVocabulary(vocabulary);
		// load Dictionary
		Dictionary dict = new Dictionary();
		dict.loadDictionary("output//dewac//DewacDictionary.txt");
		// load ambiguities
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter4.txt");
		
		
		ambiguities = dict.removeNamesFromAmbiguities(ambiguities);
		// run RemoveByProportion
		dict.removeByProportion(ambiguities, voc, 1d);
		
		
	}

	public static void vocExtraction() throws IOException{
		
		UmlautExperimentConfiguration config = new UmlautExperimentConfiguration(null, null, null, null, null, false, 3, 3);
		
		DewacSplitter dewac = new DewacSplitter("output//dewac//");
		
//		for(int i = 1000; i <=2000; i++){
//			dewag.sentencesWithUmlautsToFile(new File("input//dewac//sdewac-v3.tagged_"+i), 10000, i);
//		}
		List<List<String>> allSentences = new ArrayList<List<String>>();
		Vocabulary voc = new Vocabulary();
		for(int i = 1000; i <=2000; i++){
			List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//old//dewac//ofInterest"+i+".txt"));
			allSentences.addAll(tokenizedSentences);
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
	
		Dictionary transVoc = new Dictionary();
		for (String key : umlautVoc.vocabulary.keySet()) {
			transVoc.addEntry(key);
		}
		System.out.println("Vokabular erstellt");
		
		Map<String, HashSet<String>> ambiguities = null;
		
		ambiguities = transVoc.findAmbiguities(voc);
		
		// print Ambiguities!
		FileUtils.printMap(ambiguities, "output//dewac//", "DewacAmbiguities5");
		
		List<String> ofInterest = new ArrayList<String>();
		for(HashSet<String> set : ambiguities.values()){
			for(String word : set){
				ofInterest.add(word);
			}
		}
//		KeywordContexts contexts = new KeywordContexts();
////		List<List<String>> ambigSentences = new ArrayList<List<String>>();
//		for(List<String> sentence : allSentences){
//			for(String word : sentence){
//				if(ofInterest.contains(word)){
////					ambigSentences.add(sentence);
//					contexts.addContext(word, sentence);
//				}
//			}
//		}
//		contexts.printKeywordContexts("output//classification//", "DewacAmbigSentences");
////		FileUtils.printListOfList(ambigSentences, "output//classification//", "DewacAmbigSentences");
//		
////		// find Ambiguities which are rare
////		int numberOfMinOccurences = 10;
////		Map<String, Integer> lowFrequencyAmbig = new TreeMap<String, Integer>();
////		for(String key : ambiguities.keySet()){
////			for(String value : ambiguities.get(key)){
////				if(voc.vocabulary.get(value)<=numberOfMinOccurences){
////					lowFrequencyAmbig.put(value, voc.vocabulary.get(value));
////				}
////			}
////		}
////		FileUtils.printMap(lowFrequencyAmbig, "output//classification//", "tokensWithLessThan" + numberOfMinOccurences + "Occurences");
////		System.out.println("Ambiguities which occur less than" + " times: " + lowFrequencyAmbig.size());
//		
		
		System.out.println(ambiguities.size() + " Gruppen mehrdeutiger Wörter gefunden");
//		FileUtils.printMap(ambiguities, "output//classification//", "DewacAmbigeWörter4");
		
		
		
	}
	
public static void onlyVocExtraction() throws IOException{
		
		UmlautExperimentConfiguration config = new UmlautExperimentConfiguration(null, null, null, null, null, false, 3, 3);
		
		DewacSplitter dewac = new DewacSplitter("output//dewac//");
		
//		for(int i = 1000; i <=2000; i++){
//			dewag.sentencesWithUmlautsToFile(new File("input//dewac//sdewac-v3.tagged_"+i), 10000, i);
//		}
		List<List<String>> allSentences = new ArrayList<List<String>>();
		Vocabulary voc = new Vocabulary();
		for(int i = 1000; i <=2000; i++){
			List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//old//dewac//ofInterest"+i+".txt"));
			allSentences.addAll(tokenizedSentences);
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
		
		TreeMap<String,Integer> sortedVoc = new TreeMap<String,Integer>(Collator.getInstance(Locale.GERMAN));
		sortedVoc.putAll(voc.vocabulary);
		
		FileUtils.printMap(sortedVoc, "output//stats//", "DewacVoc");
		
		
		Vocabulary umlautVoc = voc.getAllByRegex(".*([ÄäÖöÜü]).*");
		System.out.println("Token mit Umlaut: " + umlautVoc.getNumberOfTokens());
		System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
		
		FileUtils.printSet(umlautVoc.vocabulary.keySet(), "output//dewac//", "umlautsInDeWac");
		
		Dictionary dict = new Dictionary(umlautVoc);
		dict.printToFile("output//dewac//", "DewacDictionary");
		
		
	}
	
	public static void statsAboutContexts() throws IOException{
		KeywordContexts contexts = new KeywordContexts();
		contexts = contexts.loadKeywordContextsFromFile("output//classification//DewacAmbigSentences.txt");
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter4.txt");
		List<String> output = new ArrayList<String>();
		
		System.out.println(contexts.keywordContextsMap.size());
		
		for (String ambigGroup : ambiguities.keySet()){
			for(String key : ambiguities.get(ambigGroup)){
				if(contexts.keywordContextsMap.containsKey(key)){
					System.out.println("Key:"+ key + " Anzahl der Kontexte: "+ contexts.keywordContextsMap.get(key).size());
					output.add("Key:"+ key + " Anzahl der Kontexte: "+ contexts.keywordContextsMap.get(key).size());
					if(contexts.keywordContextsMap.get(key).size() == 1){
						System.out.println(contexts.keywordContextsMap.get(key));
						output.add(contexts.keywordContextsMap.get(key).toString());
					}
				} else {
					System.out.println("***Keine Kontexte im split-Korpus gefunden");
				}
			}
		}
		FileUtils.printList(output, "output//stats//", "ContextStats", ".txt");
		
	}
	
	public static void moreStatsAboutContexts() throws IOException{
//		KeywordContexts contexts = new KeywordContexts();
//		contexts = contexts.loadKeywordContextsFromFile("output//classification//DewacAmbigSentences.txt");
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter4.txt");
		List<String> names = FileUtils.fileToList("output//stats//familynames.txt");
		names.addAll(FileUtils.fileToList("output//stats//extractedNames.txt"));
		names.addAll(FileUtils.fileToList("output//stats//DE-Ortsnamen.txt", "#"));
		HashSet<String> nameSet = new HashSet<String>();
		nameSet.addAll(names);
		names.clear();
		names.addAll(nameSet);
		
		Vocabulary namesVoc = new Vocabulary(names);
		Vocabulary umlautNamesVoc = namesVoc.getAllByRegex(".*([ÄäÖöÜüß]).*");
		System.out.println("Namen mit Umlauten: " + umlautNamesVoc.vocabulary.size());
		
		Dictionary transVoc = new Dictionary();
		for (String key : umlautNamesVoc.vocabulary.keySet()) {
			transVoc.addEntry(key);
		}
		Map<String, HashSet<String>> namesAmbiguities = transVoc.findAmbiguities(namesVoc);
		System.out.println("Ambiguitäten in Namen: " + namesAmbiguities.size());
		System.out.println(namesAmbiguities);
		
		List<String> umlautsInDewac = FileUtils.fileToList("output//stats//umlautsInDeWac.txt");
		
		List<String> namesInDewac = new ArrayList<String>();
		for (String name : umlautNamesVoc.vocabulary.keySet()) {
			if(umlautsInDewac.contains(name)){
				namesInDewac.add(name);
			}
		}
		System.out.println("Eigennamen mit Umlauten im Dewac: " + namesInDewac.size());
		FileUtils.printList(namesInDewac, "output//stats//", "umlautNamesInDewac", ".txt");
		
		Set<String> allAmbiguities = new HashSet<String>();
		for(String key : ambiguities.keySet()){
			allAmbiguities.addAll(ambiguities.get(key));
		}
		System.out.println(allAmbiguities.size());
		
		int namesInAmbiguities = 0;
		for(String name : names){
			if(allAmbiguities.contains(name)){
				namesInAmbiguities++;
			}
		}
		System.out.println(namesInAmbiguities + " bekannte Namen sind in den Dewac-Ambiguitäten.");	
		
	}
	
	public static void removeNamesFromAmbiguities() throws IOException{
//		KeywordContexts contexts = new KeywordContexts();
//		contexts = contexts.loadKeywordContextsFromFile("output//classification//DewacAmbigSentences.txt");
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter4.txt");
		HashMap<String, HashSet<String>> remainingAmbiguities = new HashMap<String, HashSet<String>>();
		List<String> notRemove = FileUtils.fileToList("output//stats//betterNotRemove.txt");
		
		
		List<String> names = FileUtils.fileToList("output//stats//familynames.txt");
		names.addAll(FileUtils.fileToList("output//stats//extractedNames.txt"));
		names.addAll(FileUtils.fileToList("output//stats//DE-Ortsnamen.txt", "#"));
		HashSet<String> nameSet = new HashSet<String>();
		nameSet.addAll(names);
		names.clear();
		names.addAll(nameSet);
		System.out.println(names.size());
		names.removeAll(notRemove);
		System.out.println(names.size());
		Collections.sort(names, Collator.getInstance(Locale.GERMAN));
		
		remainingAmbiguities.putAll(ambiguities);
		System.out.println("Ambiguitäten inkl. Namen: " + remainingAmbiguities.size());
		
		List<String> removed = new ArrayList<String>();
		List<String> toRemove = new ArrayList<String>();
		
		for(String name : names){
			for(String key : ambiguities.keySet()){
				if(ambiguities.get(key).contains(name)) {
					System.out.println(name);
					removed.add(name);
					toRemove.addAll(ambiguities.get(key));
					remainingAmbiguities.remove(key);
				} 
			}
		}
		
//		Collections.sort(removed, Collator.getInstance(Locale.GERMAN));
//		FileUtils.printList(removed, "output//stats//", "removed2", ".txt");
		
		Collections.sort(toRemove, Collator.getInstance(Locale.GERMAN));
		FileUtils.printList(toRemove, "output//stats//", "toRemoveFromContexts", ".txt");
		
		System.out.println("Ambiguitäten ohne Namen: " + remainingAmbiguities.size());
//		FileUtils.printMap(remainingAmbiguities, "output//classification//", "DewacAmbigeWörterOhneNamen2");
		
	}
	
	public static void createFinalNamesList() throws IOException{
		List<String> notRemove = FileUtils.fileToList("output//stats//betterNotRemove.txt");
		List<String> names = FileUtils.fileToList("output//stats//familynames.txt");
		names.addAll(FileUtils.fileToList("output//stats//extractedNames.txt"));
		names.addAll(FileUtils.fileToList("output//stats//DE-Ortsnamen.txt", "#"));
		HashSet<String> nameSet = new HashSet<String>();
		nameSet.addAll(names);
		names.clear();
		names.addAll(nameSet);
		System.out.println(names.size());
		names.removeAll(notRemove);
		System.out.println(names.size());
		Collections.sort(names, Collator.getInstance(Locale.GERMAN));
		FileUtils.printList(names, "output//stats//", "finalNames", ".txt");
	}
	
	public static void removeNamesFromContexts() throws IOException{
		List<String> names = FileUtils.fileToList("output//stats//toRemoveFromContexts.txt");
		KeywordContexts contexts = new KeywordContexts();
		contexts = contexts.loadKeywordContextsFromFile("output//classification//DewacKontexte.txt");
		System.out.println(contexts.keywordContextsMap.size());
		System.out.println("names: " + names.size());
		
		for(String name : names){
			
			if(!contexts.keywordContextsMap.containsKey(name)) System.out.println(name);
			contexts.keywordContextsMap.remove(name);
		}
		System.out.println(contexts.keywordContextsMap.size());
		contexts.printKeywordContexts("output//classification//", "DewacAmbigSentencesWithoutNames");
	}
	
	public static void showContextsOfNames() throws IOException{
		
		List<String> wordList = FileUtils.fileToList("output//stats//betterNotRemove.txt");
		Set<String> wordSet = new HashSet<String>();
		wordSet.addAll(wordList);
		wordList.clear();
		wordList.addAll(wordSet);
		Collections.sort(wordList, Collator.getInstance(Locale.GERMAN));
		showContexts(wordList);
	}
	
	public static void showContexts(List<String> wordList) throws IOException{
		
		KeywordContexts contexts = new KeywordContexts();
		contexts = contexts.loadKeywordContextsFromFile("output//classification//DewacKontexte.txt");
		KeywordContexts toPrint = new KeywordContexts();
		
		for(String word : wordList){
			if(contexts.keywordContextsMap.containsKey(word)){
				List<List<String>> context = contexts.getContext(word);
				if(context.size() < 11){
					System.out.println(word + ": " + context.size() + " Kontexte");
					for(List<String> ctxt : context){
						System.out.println(ctxt);
					}
					
				}
				toPrint.addContexts(word, context);
//				System.out.println(word);
//				
//				
			}
		}
//		toPrint.printKeywordContexts("output//stats//", "ContextsOfSomeNames");
		
	}
	
	public static void sentenceExtraction() throws IOException{
		
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter2.txt");
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
		dewac.sentencesOfInterestToFile2(new File("input//dewac_singlefile//sdewac-v3.tagged"), soi, 100, 1800);
		FileUtils.printString(soi.stringsFoundToString(), "output//classification//", "DewacStingsFound", ".txt");
		
		
//		List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//dewac//byWord//ofInterest3.txt"));
//		System.out.println("Anzahl der Kontexte insgesamt: " + tokenizedSentences.size());
//		FileUtils.printList(tokenizedSentences, "output//dewac//", "tokenizedSentences3", ".txt");
	}
	
	public static void createContexts() throws IOException{
		System.out.println("createContexts");
		KeywordContexts contexts = new KeywordContexts();
		// load
		List<List<String>> tokenizedSentences = FileUtils.fileToListOfLists("output//classification//DewacAmbigSentences.txt");
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter4.txt");
		List<String> ofInterest = new ArrayList<String>();
		for(HashSet<String> set : ambiguities.values()){
			for(String word : set){
				ofInterest.add(word);
			}
		}
		
		System.out.println("loaded");
		
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
		contexts.printKeywordContexts("output//classification//", "DewacKontexte4");
//		contexts.printStats();
	}
	
	public static void ambiguitiesFilter() throws IOException{
		HashMap<String, HashSet<String>> ambiguities = FileUtils.fileToAmbiguities("output//classification//DewacAmbigeWörter4.txt");
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
		FileUtils.printMap(ambiguities, "output//classification//", "DewacAmbigeWörter4");
//		FileUtils.printMap(ambigeStopwords, "output//classification//", "DewacAmbigeStopwords");
		
	}
	
	public static void nameFinder() throws IOException{
		
		Set<String> names = new HashSet<String>();
		
		List<String> preNames = new ArrayList<String>();
		preNames.add("Herr");
		preNames.add("Frau");
		preNames.add("Mr");
		preNames.add("Mrs");
		
		UmlautExperimentConfiguration config = new UmlautExperimentConfiguration(null, null, null, null, null, false, 3, 3);
		
		DewacSplitter dewac = new DewacSplitter("output//dewac//");
		
//		for(int i = 1000; i <=2000; i++){
//			dewag.sentencesWithUmlautsToFile(new File("input//dewac//sdewac-v3.tagged_"+i), 10000, i);
//		}
		List<List<String>> allSentences = new ArrayList<List<String>>();
		Vocabulary voc = new Vocabulary();
		for(int i = 1000; i <=2000; i++){
			List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//dewac//ofInterest"+i+".txt"));
			allSentences.addAll(tokenizedSentences);
			for (List<String> list : tokenizedSentences) {
				voc.addTokens(list);
			}
		}

		for(List<String> sentence : allSentences){
			for(String preName : preNames){
				if(sentence.contains(preName)){
					int index = sentence.indexOf(preName);
					if(sentence.size()>index) {
						String name = sentence.get(index+1);
						if(name.matches("^[A-Z].*")){
							System.out.println(name);
							names.add(name);
						}
					}
				}
			}
		}
		
		System.out.println(names);
		System.out.println(names.size());
		FileUtils.printSet(names, "output//stats//", "extractedNames");
		
		
	}

}
