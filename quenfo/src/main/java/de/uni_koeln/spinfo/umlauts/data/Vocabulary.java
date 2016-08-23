package de.uni_koeln.spinfo.umlauts.data;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.uni_koeln.spinfo.umlauts.utils.FileUtils;


public class Vocabulary {
	
	public HashMap<String,Integer> vocabulary;
	private int numberOfTokens;
	
	
	public Vocabulary(List<String> tokens) {
		
		numberOfTokens = tokens.size();
		vocabulary = new HashMap<String,Integer>();
		
		for (String token : tokens) {
			if (!vocabulary.containsKey(token)) {
				vocabulary.put(token, 1);
			} else {
				vocabulary.put(token, vocabulary.get(token)+1);
			}
		}
	}
	
	public Vocabulary() {
		vocabulary = new HashMap<String,Integer>();
	}
	
	public int getNumberOfTokens() {
		generateNumberOfTokens();
		return numberOfTokens;
	}
	
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
	
	public void addTokens(List<String> tokens){
		for (String token : tokens) {
			if (!vocabulary.containsKey(token)) {
				vocabulary.put(token, 1);
			} else {
				vocabulary.put(token, vocabulary.get(token)+1);
			}
		}
	}
	
	public void printTokensWithOccurenceLowerThan(int numberOfMinOccurences) throws IOException{
		Map<String, Integer> mapToReturn = new TreeMap<String, Integer>();
		for(String key : vocabulary.keySet()){
			if(vocabulary.get(key)<=numberOfMinOccurences){
				mapToReturn.put(key, vocabulary.get(key));
				System.out.println(key + ": " + vocabulary.get(key));
			}
		}
		FileUtils.printMap(mapToReturn, "output//classification//", "tokensWithLessThan" + numberOfMinOccurences + "Occurences");
	}
	

	
	

}
