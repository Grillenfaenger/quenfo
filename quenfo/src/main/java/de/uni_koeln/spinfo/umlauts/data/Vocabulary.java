package de.uni_koeln.spinfo.umlauts.data;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;


public class Vocabulary {
	
	public TreeMap<String,Integer> vocabulary;
	private int numberOfTokens;
	
	
	public Vocabulary(List<String> tokens) {
		
		numberOfTokens = tokens.size();
		vocabulary = new TreeMap<String,Integer>();
		
		for (String token : tokens) {
			if (!vocabulary.containsKey(token)) {
				vocabulary.put(token, 1);
			} else {
				vocabulary.put(token, vocabulary.get(token)+1);
			}
		}
	}
	
	public Vocabulary() {
		vocabulary = new TreeMap<String,Integer>();
	}
	
	public int getNumberOfTokens() { return numberOfTokens;}
	
	public void generateNumberOfTokens() {
		
		int total = 0;
		for (Entry<String, Integer> entry : vocabulary.entrySet()) {
			total = total + entry.getValue();
		}
		numberOfTokens = total;
	}

	public Vocabulary getAllByRegex(String regexPattern) {
		
		Vocabulary regexVoc = new Vocabulary();
		
		for (Entry<String, Integer> entry : vocabulary.entrySet()) {
			if(entry.getKey().matches(regexPattern)) {
				regexVoc.vocabulary.put(entry.getKey(), entry.getValue());
			}
			
		}
		return regexVoc;	
	}
	

	
	

}
