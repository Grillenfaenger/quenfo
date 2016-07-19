package de.uni_koeln.spinfo.umlauts.data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uni_koeln.spinfo.umlauts.utils.FileUtils;


public class TranslationVocabulary {
	
	public TreeMap<String,String> vocabulary;
	
	public TranslationVocabulary() {
		vocabulary = new TreeMap<String, String>();
	}
	
	public void setVocabulary(TreeMap<String, String> vocabulary) {
		this.vocabulary = vocabulary;
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
	
	public Map<String, TreeSet<String>> findAmbiguities(Vocabulary referenceVoc) {
		
		TreeMap<String, TreeSet<String>>ambiguities = new TreeMap<String, TreeSet<String>>();
		
		// Ambiguitäten zwichen umlautbefreiten Wörtern
		
		ArrayList<String> ambige = new ArrayList<String>();
		ArrayList<String> valueList = new ArrayList<String>();
		
		// Liste ambiger umlautbefreiter Wörter erzeugen
		for(Entry<String, String> entry : vocabulary.entrySet()){
			if (!valueList.contains(entry.getKey())) {
				valueList.add(entry.getKey());
			} else {
				ambige.add(entry.getKey());
			}
		}
		
		System.out.println(valueList);
		System.out.println(ambige.size() + " Ambiguitäten zwischen umlautbefreiten Wörtern: " + ambige);
		
		// Die jeweiligen Lesweisen sichern
		for(Entry<String, String> entry : vocabulary.entrySet()){
			if(ambige.contains(entry.getKey())) {
				if (ambiguities.containsKey(entry.getKey())){
					ambiguities.get(entry.getKey()).add(entry.getValue());
				} else {
					TreeSet<String> value = new TreeSet<String>();
					value.add(entry.getValue());
					ambiguities.put(entry.getKey(), value);
				}
			}
		}
		
		
		
		// Ambiguitäten im gesamten Ursprungsvokabular
		for(Entry<String, String> entry : vocabulary.entrySet()){
			if(referenceVoc.vocabulary.containsKey(entry.getKey())) {
				if (ambiguities.containsKey(entry.getKey())){
					ambiguities.get(entry.getKey()).add(entry.getKey());
					ambiguities.get(entry.getKey()).add(entry.getValue());
				} else {
					TreeSet<String> value = new TreeSet<String>();
					value.add(entry.getKey());
					value.add(entry.getValue());
					ambiguities.put(entry.getKey(), value);
				}
			}
		}
		
		System.out.println("Es gibt " + ambiguities.size() + " Fälle mit Ambiguitäten.");
		System.out.println(ambiguities);
		
		return ambiguities;
		
	}
	
	public Set<String> createAmbiguitySet(Map<String, TreeSet<String>> ambiguities) {
		Set<String> ambiguitySet = new TreeSet<String>();
		
		for (TreeSet<String> ambigueSet : ambiguities.values()) {
			ambiguitySet.addAll(ambigueSet);
		}
//		ambiguitySet.remove("a");
//		ambiguitySet.remove("A");
//		ambiguitySet.remove("Ä");
//		ambiguitySet.remove("ä");
//		System.out.println(ambiguitySet);
		return ambiguitySet;
	}
	
	public TreeMap<String,String> createSimpleReplacementMap(Set<String> ambiguousWords){
		TreeMap<String, String> simpleReplacements = vocabulary;
		for(String word : ambiguousWords){
			simpleReplacements.remove(word);
		}
		return simpleReplacements;
	}
	
	public void printToFile(String destPath, String fileName) throws IOException{
		FileUtils.printMap(vocabulary, destPath, fileName);
	}
	
	public void loadTranslationVocabularyFromFile(String filePath) throws IOException{
		setVocabulary(FileUtils.fileToMap(filePath));
	}

}
