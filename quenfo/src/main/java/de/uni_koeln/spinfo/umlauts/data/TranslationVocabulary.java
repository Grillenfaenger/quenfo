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


public class TranslationVocabulary {
	
	public HashMap<String,String> vocabulary;
	
	public TranslationVocabulary() {
		vocabulary = new HashMap<String, String>();
	}
	
	public void setVocabulary(Map<String, String> vocabulary) {
		this.vocabulary = (HashMap<String,String>)vocabulary;
	}

	public void addEntry(String umlautWord) {
		vocabulary.put(replaceUmlaut(umlautWord), umlautWord);
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
		for(String key : vocabulary.keySet()){
			if (!valueList.contains(key)) {
				valueList.add(key);
			} else {
				ambige.add(key);
			}
		}
		
//		System.out.println(valueList);
//		System.out.println(ambige.size() + " Ambiguitäten zwischen umlautbefreiten Wörtern: " + ambige);
		
		// Die jeweiligen Lesweisen sichern
		for(String key : vocabulary.keySet()){
			if(ambige.contains(key)) {
				if (ambiguities.containsKey(key)){
					ambiguities.get(key).add(vocabulary.get(key));
				} else {
					HashSet<String> value = new HashSet<String>();
					value.add(vocabulary.get(key));
					ambiguities.put(key, value);
				}
			}
		}
		
		
		
		// Ambiguitäten im gesamten Ursprungsvokabular
		for(String key : vocabulary.keySet()){
			if(referenceVoc.vocabulary.containsKey(key)) {
				if (ambiguities.containsKey(key)){
					ambiguities.get(key).add(key);
					ambiguities.get(key).add(vocabulary.get(key));
				} else {
					HashSet<String> value = new HashSet<String>();
					value.add(key);
					value.add(vocabulary.get(key));
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
		HashMap<String, String> simpleReplacements = vocabulary;
		for(String word : ambiguousWords){
			simpleReplacements.remove(word);
		}
		return simpleReplacements;
	}
	
	public void printToFile(String destPath, String fileName) throws IOException{
		TreeMap<String, String> sortedVocabulary = new TreeMap<String,String>(vocabulary);
		FileUtils.printMap(sortedVocabulary, destPath, fileName);
	}
	
	public void loadTranslationVocabularyFromFile(String filePath) throws IOException{
		setVocabulary(FileUtils.fileToMap(filePath));
	}

}
