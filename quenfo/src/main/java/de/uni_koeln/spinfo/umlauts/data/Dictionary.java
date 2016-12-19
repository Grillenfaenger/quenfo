package de.uni_koeln.spinfo.umlauts.data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.uni_koeln.spinfo.umlauts.utils.FileUtils;


public class Dictionary {
	
	public HashMap<String,String> dictionary;
	
	public Dictionary() {
		dictionary = new HashMap<String, String>();
	}
	
	public Dictionary(Vocabulary voc) {
		dictionary = new HashMap<String, String>();
		for (String word : voc.vocabulary.keySet()) {
			addEntry(word);
		}
	}
	
	public Dictionary(HashMap<String,String> dictionary){
		this.dictionary = dictionary;
	}
	
	public void setDictionary(Map<String, String> vocabulary) {
		this.dictionary = (HashMap<String,String>)vocabulary;
	}
	
	public String get(String key){
		return dictionary.get(key);
	}
	
	public void addEntry(String umlautWord) {
		String without = replaceUmlaut(umlautWord);
		if(dictionary.containsKey(umlautWord)){
			System.out.println("Uncaught ambiguity: " + umlautWord + ", " +  without + ", " + dictionary.get(umlautWord));
//			log.log(Level.INFO, "Uncaught ambiguity: " + umlautWord + ", " + dictionary.get(without));
		}
		dictionary.put(umlautWord, without);
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
	
	public Map<String, HashSet<String>> findAmbiguities(Vocabulary referenceVoc) throws IOException {
		
		HashMap<String, HashSet<String>>ambiguities = new HashMap<String, HashSet<String>>();
		
		// Ambiguitäten zwichen umlautbefreiten Wörtern
		
		HashSet<String> ambige = new HashSet<String>();
		HashSet<String> valueSet = new HashSet<String>();
		
		// Liste ambiger umlautbefreiter Wörter erzeugen
		for(String key : dictionary.keySet()){
			String without = dictionary.get(key);
			if (!valueSet.contains(without)) {
				valueSet.add(without);
			} else {
				ambige.add(without);
			}
		}
		
		// Die jeweiligen Lesweisen sichern
		for(String key : dictionary.keySet()){
			String without = dictionary.get(key);
			if(ambige.contains(without)) {
				if (ambiguities.containsKey(without)){
					ambiguities.get(without).add(key);
				} else {
					HashSet<String> value = new HashSet<String>();
					value.add(key);
					ambiguities.put(without, value);
				}
			}
		}
		
		// Ambiguitäten im gesamten Ursprungsvokabular
		for(String key : dictionary.keySet()){ 
			String without = dictionary.get(key);
			if(referenceVoc.vocabulary.containsKey(without)) {
				if (ambiguities.containsKey(without)){
					HashSet<String> newValues = new HashSet<String>();
					newValues.add(without);
					newValues.add(key);
					ambiguities.get(without).addAll(newValues);
				} else {
					HashSet<String> value = new HashSet<String>();
					value.add(without);
					value.add(key);
					ambiguities.put(without, value);
				}
			}
		}
		
		FileUtils.printMap(ambiguities, "output//stats//", "bibbAmbiguitiesZwischenstand");
		System.out.println("Es gibt " + ambiguities.size() + " Fälle mit Ambiguitäten.");
		
		// invert Dictionary for the purpose of correction
		invertDictionary();
		return ambiguities;
	}
	
	public Set<String> createDictSet() {
		Set<String> dictSet = new HashSet<String>();
		
		for(String key : dictionary.keySet()){
			dictSet.add(key);
			dictSet.add(dictionary.get(key));
		}
		return dictSet;
	}
	
	public Set<String> createAmbiguitySet(Map<String, HashSet<String>> ambiguities) {
		Set<String> ambiguitySet = new HashSet<String>();
		
		for (Set<String> ambigueSet : ambiguities.values()) {
			ambiguitySet.addAll(ambigueSet);
		}
		return ambiguitySet;
	}
	
	/**
	 * inverts the Dictionary for the purpose of using for correction.
	 * Note, that by reversion some entries could get lost due to same key. 
	 * @return
	 */
	public Dictionary invertDictionary(){
		HashMap<String, String> invertedMap = new HashMap<String,String>();
		for(String key : dictionary.keySet()){
			invertedMap.put(dictionary.get(key), key);
		}
		Dictionary reverseDictionary = new Dictionary(invertedMap);
		dictionary = invertedMap;
		return reverseDictionary;
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
		for(String name : names){
			String nameWithout = replaceUmlaut(name);
			if(ambiguities.containsKey(nameWithout)){
				ambiguities.remove(nameWithout);
				dictionary.remove(name);
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
		ArrayList<String> ambiguitiesToRemove = new ArrayList<String>();
		String removedInfo = null;
		StringBuffer sb = null;
		
		int keep;
		
		for(String key : ambiguities.keySet()){
			keep = 2;
			removedInfo = null;
			sb = new StringBuffer();
			HashSet<String> variants = ambiguities.get(key);
			if(variants.size() == 2){
				Integer[] occurences = new Integer[2];
				String[] variant = new String[2]; // 0 : without, 1: with
				if(variants.contains(key)){
					variant[0] = key;
					for(String s : variants){
						if(!dictionary.containsKey(s)){
							variant[1] = s;
						}
					}
				} else {
					int i = 0;
					for(String s : variants){
						variant[i] = s;
						i++;
					}
				}
				
				sb.append(variant[0]+"("+voc.getOccurenceOf(variant[0])+")"+", ");
				sb.append(variant[1]+"("+voc.getOccurenceOf(variant[1])+")");
				
				if(variant[1] == null){
					System.out.println("Error: " + key);
				}
				occurences[0] = voc.getOccurenceOf(variant[0]); // without
				occurences[1] = voc.getOccurenceOf(variant[1]);	// with
				
				if(occurences[0] == 0 || occurences[1] == 0){
					System.out.println(variants);
				}
				
				
				if(!occurences[0].equals(occurences[1])){
//					double occurenceRatio = Math.log10(occurences[0].doubleValue()/occurences[1].doubleValue());
					double occurenceRatio = Math.log10(occurences[0].doubleValue())-Math.log10(occurences[1].doubleValue());
					if(occurenceRatio < logRate*-1){
						keep = 1;

					} else if(occurenceRatio > logRate){
						keep = 0;	
					} else {
						dictionary.remove(key); // d. h. es wird klassifiziert
					}
					if(keep != 2){
						if(variant[keep].equals(key)){ // nie korrigieren, nie klassifizieren
							dictionary.remove(key);
							ambiguitiesToRemove.add(key);
							sb.append(": "+occurenceRatio + " "+ key+ " wird nie korrigiert.");
							removedInfo = sb.toString();
						} else { // immer korrigieren zu variant[keep] != key
							int toRemove;
							if(keep == 0){
								toRemove = 1; 
							} else {
								toRemove = 0;
							}
							ambiguitiesToRemove.add(key);
//							ambiguities.get(key).remove(variant[toRemove]);
							sb.append(": "+occurenceRatio+"Es wird immer in zu " + variant[keep]+ " korrigiert.");
							removedInfo = sb.toString();
						}
					}
					
				} else {
					// wenn es genau gleich viele Vorkommen gibt
					System.out.println(variants + "werden klassifiziert");
					dictionary.remove(key); // d. h. es wird klassifiziert
				}
			} else {
				System.out.println(variants + "!= 2");
				dictionary.remove(key);
				System.out.println(variants + "werden klassifiziert");
			}
			
			
			
			
			if(removedInfo != null){
				removed.add(removedInfo);
			}
			
		}
		
		// remove the clear ones from ambiguities
		for(String key : ambiguitiesToRemove){
			ambiguities.remove(key);
		}
		
		
		if(!removed.isEmpty()){
			FileUtils.printList(removed, "output//stats//", "decidedByRatio", ".txt");
		}
		System.out.println("Es gibt " + ambiguities.size() + " Wortpaare.");
		System.out.println(removed.size() + " Wortpaare wurden nun aufgrund ihres Verhältnisses bestimmt");
		return remainingAmbiguities;
	}

}
