package de.uni_koeln.spinfo.umlauts.data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	
	public void setDictionary(Map<String, String> vocabulary) {
		this.dictionary = (HashMap<String,String>)vocabulary;
	}
	
	public void addEntry(String umlautWord) {
		dictionary.put(replaceUmlaut(umlautWord), umlautWord);
	}
	
	public void addEntries(Map<String, String> dict){
		dictionary.putAll(dict);
	}
	
	public String replaceUmlaut(String umlautWord) {
		String replacement = umlautWord;
		replacement = replacement.replaceAll("Ä", "A");
		replacement = replacement.replaceAll("ä", "a");
		replacement = replacement.replaceAll("Ö", "O");
		replacement = replacement.replaceAll("ö", "o");
		replacement = replacement.replaceAll("Ü", "U");
		replacement = replacement.replaceAll("ü", "u");
//		replacement = replacement.replaceAll("ß", "ss");
		
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
	
	public void loadDictionary(String filePath) throws IOException{
		setDictionary(FileUtils.fileToMap(filePath));
	}
	
	public HashMap<String,HashSet<String>> removeNamesFromAmbiguities(Map<String, HashSet<String>> ambiguities) throws IOException{

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
		
		FileUtils.printList(removed, "output//stats//", "removedNames", ".txt");
		System.out.println("Ambiguitäten ohne Namen: " + remainingAmbiguities.size());
		System.out.println("Namen: " + removed.size());
		
		return remainingAmbiguities;
	}
	
	public HashMap<String,HashSet<String>> removeByProportion(Map<String, HashSet<String>> ambiguities, Vocabulary voc, double logRate) throws IOException {
		HashMap<String, HashSet<String>> remainingAmbiguities = new HashMap<String, HashSet<String>>();
		remainingAmbiguities.putAll(ambiguities);
		
		ArrayList<String> removed = new ArrayList<String>();
		String removedInfo = null;
		StringBuffer sb = null;
		
		for(String key : ambiguities.keySet()){
			removedInfo = null;
			sb = new StringBuffer();
			HashSet<String> variants = ambiguities.get(key);
			if(variants.size() == 2){
				Integer[] occurences = new Integer[2];
				String without = key;
				String with = null;
				for(String var : variants){
					if(!dictionary.containsKey(var)){
						with = var;
						sb.append(key+"("+voc.getOccurenceOf(key)+")"+", ");
						sb.append(var+"("+voc.getOccurenceOf(var)+")");
						break;
					}
				}
				if(without == null){
					System.out.println("Error: " + key);
				}
				occurences[0] = voc.getOccurenceOf(without);
				occurences[1] = voc.getOccurenceOf(with);
				
				if(!occurences[0].equals(occurences[1])){
//					double occurenceRatio = Math.log10(occurences.get(0).doubleValue()) - Math.log10(occurences.get(1).doubleValue());
					double occurenceRatio = Math.log10(occurences[0].doubleValue()/occurences[1].doubleValue());
//					Integer difference = occurences.get(0)-occurences.get(1);
	//					double occurenceRatio = Math.log10(difference.doubleValue());
	//					System.out.println(occurenceRatio);
						
						if(occurenceRatio < logRate*-1){
							// aus den ambiguities herauslöschen
							remainingAmbiguities.remove(key); // d. h. es wird nicht klassifiziert, aber immer direkt korrigiert.
							sb.append(": "+occurenceRatio+"Es wird immer in zu " + dictionary.get(key)+ " korrigiert.");
							removedInfo = sb.toString();
						} else if(occurenceRatio > logRate){
							// aus dem Dict und aus den ambiguities löschen
							dictionary.remove(key); // d. h. es wird nicht korrigiert
							remainingAmbiguities.remove(key); // und nicht klassifiziert
							sb.append(": "+occurenceRatio + " "+ key+ " wird nie korrigiert.");
							removedInfo = sb.toString();
						} else {
							dictionary.remove(key); // d. h. es wird klassifiziert
						}
				} else {
					// wenn es genau gleich viele Vorkommen gibt
					System.out.println(variants + "werden klassifiziert");
					dictionary.remove(key); // d. h. es wird klassifiziert
				}
			} else {
				System.out.println(variants);
			}
			if(removedInfo != null){
				removed.add(removedInfo);
			}
			
		}
		if(!removed.isEmpty()){
			FileUtils.printList(removed, "output//stats//", "decidedByRatio", ".txt");
		}
		System.out.println("Es gibt " + ambiguities.size() + " Wortpaare.");
		System.out.println(removed.size() + " Wortpaare wurden nun aufgrund ihres Verhältnisses bestimmt");
		return remainingAmbiguities;
	}

}
