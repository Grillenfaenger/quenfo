package de.uni_koeln.spinfo.umlauts.data;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


public class TranslationVocabulary {
	
	public TreeMap<String,String> vocabulary;
	
	public TranslationVocabulary() {
		vocabulary = new TreeMap<String, String>();
	}
	
	public void addEntry(String umlautWord) {
		vocabulary.put(umlautWord, replaceUmlaut(umlautWord));
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
	
	public TreeMap<String, TreeSet<String>> findAmbiguities(Vocabulary referenceVoc) {
		
		TreeMap<String, TreeSet<String>>ambiguities = new TreeMap<String, TreeSet<String>>();
		
		// Ambiguitäten zwichen umlautbefreiten Wörtern
		
		ArrayList<String> ambige = new ArrayList<String>();
		ArrayList<String> valueList = new ArrayList<String>();
		
		// Liste ambiger umlautbefreiter Wörter erzeugen
		for(Entry<String, String> entry : vocabulary.entrySet()){
			if (!valueList.contains(entry.getValue())) {
				valueList.add(entry.getValue());
			} else {
				ambige.add(entry.getValue());
			}
		}
		
		System.out.println(valueList);
		System.out.println(ambige.size() + " Ambiguitäten zwischen umlautbefreiten Wörtern: " + ambige);
		
		// Die jeweiligen Lesweisen sichern
		for(Entry<String, String> entry : vocabulary.entrySet()){
			if(ambige.contains(entry.getValue())) {
				if (ambiguities.containsKey(entry.getValue())){
					ambiguities.get(entry.getValue()).add(entry.getKey());
				} else {
					TreeSet<String> value = new TreeSet<String>();
					value.add(entry.getKey());
					ambiguities.put(entry.getValue(), value);
				}
			}
		}
		
		
		
		// Ambiguitäten im gesamten Ursprungsvokabular
		for(Entry<String, String> entry : vocabulary.entrySet()){
			if(referenceVoc.vocabulary.containsKey(entry.getValue())) {
				if (ambiguities.containsKey(entry.getValue())){
					ambiguities.get(entry.getValue()).add(entry.getKey());
					ambiguities.get(entry.getValue()).add(entry.getValue());
				} else {
					TreeSet<String> value = new TreeSet<String>();
					value.add(entry.getKey());
					value.add(entry.getValue());
					ambiguities.put(entry.getValue(), value);
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
		System.out.println(ambiguitySet);
		return ambiguitySet;
	}

}
