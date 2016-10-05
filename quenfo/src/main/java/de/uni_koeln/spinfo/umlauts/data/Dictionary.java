package de.uni_koeln.spinfo.umlauts.data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uni_koeln.spinfo.umlauts.utils.FileUtils;


public class Dictionary {
	
	public HashMap<String,String> dictionary;
	
	public Dictionary() {
		dictionary = new HashMap<String, String>();
	}
	
	public Dictionary(Vocabulary voc) {
		dictionary = new HashMap<String, String>();
		for (String key : voc.vocabulary.keySet()) {
			addEntry(key);
		}
	}
	
	public void setVocabulary(Map<String, String> vocabulary) {
		this.dictionary = (HashMap<String,String>)vocabulary;
	}

	public void addEntry(String umlautWord) {
		dictionary.put(replaceUmlaut(umlautWord), umlautWord);
	}
	
	public String replaceUmlaut(String umlautWord) {
		String replacement = umlautWord;
		replacement = replacement.replaceAll("Ä", "A");
		replacement = replacement.replaceAll("ä", "a");
		replacement = replacement.replaceAll("Ö", "O");
		replacement = replacement.replaceAll("ö", "o");
		replacement = replacement.replaceAll("Ü", "U");
		replacement = replacement.replaceAll("ü", "u");
		
		return replacement;
	}
	
	public Map<String, HashSet<String>> findAmbiguities(Vocabulary referenceVoc) {
		
		HashMap<String, HashSet<String>>ambiguities = new HashMap<String, HashSet<String>>();
		
		// Ambiguitäten zwichen umlautbefreiten Wörtern
		
		ArrayList<String> ambige = new ArrayList<String>();
		ArrayList<String> valueList = new ArrayList<String>();
		
		// Liste ambiger umlautbefreiter Wörter erzeugen
		for(String key : dictionary.keySet()){
			if (!valueList.contains(key)) {
				valueList.add(key);
			} else {
				ambige.add(key);
			}
		}
		
//		System.out.println(valueList);
//		System.out.println(ambige.size() + " Ambiguitäten zwischen umlautbefreiten Wörtern: " + ambige);
		
		// Die jeweiligen Lesweisen sichern
		for(String key : dictionary.keySet()){
			if(ambige.contains(key)) {
				if (ambiguities.containsKey(key)){
					ambiguities.get(key).add(dictionary.get(key));
				} else {
					HashSet<String> value = new HashSet<String>();
					value.add(dictionary.get(key));
					ambiguities.put(key, value);
				}
			}
		}
		
		
		
		// Ambiguitäten im gesamten Ursprungsvokabular
		for(String key : dictionary.keySet()){
			if(referenceVoc.vocabulary.containsKey(key)) {
				if (ambiguities.containsKey(key)){
					ambiguities.get(key).add(key);
					ambiguities.get(key).add(dictionary.get(key));
				} else {
					HashSet<String> value = new HashSet<String>();
					value.add(key);
					value.add(dictionary.get(key));
					ambiguities.put(key, value);
				}
			}
		}
		
		System.out.println("Es gibt " + ambiguities.size() + " Fälle mit Ambiguitäten.");
//		System.out.println(ambiguities);
		
		return ambiguities;
		
	}
	
	public Set<String> createAmbiguitySet(Map<String, HashSet<String>> ambiguities) {
		Set<String> ambiguitySet = new HashSet<String>();
		
		for (Set<String> ambigueSet : ambiguities.values()) {
			ambiguitySet.addAll(ambigueSet);
		}
//		ambiguitySet.remove("a");
//		ambiguitySet.remove("A");
//		ambiguitySet.remove("Ä");
//		ambiguitySet.remove("ä");
//		System.out.println(ambiguitySet);
		return ambiguitySet;
	}
	
	public Map<String,String> createSimpleReplacementMap(Set<String> ambiguousWords){
		HashMap<String, String> simpleReplacements = dictionary;
		for(String word : ambiguousWords){
			simpleReplacements.remove(word);
		}
		return simpleReplacements;
	}
	
	public void printToFile(String destPath, String fileName) throws IOException{
		TreeMap<String, String> sortedVocabulary = new TreeMap<String,String>(dictionary);
		FileUtils.printMap(sortedVocabulary, destPath, fileName);
	}
	
	public void loadTranslationVocabularyFromFile(String filePath) throws IOException{
		setVocabulary(FileUtils.fileToMap(filePath));
	}

}
