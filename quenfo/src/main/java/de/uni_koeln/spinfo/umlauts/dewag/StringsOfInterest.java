package de.uni_koeln.spinfo.umlauts.dewag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A class to specify strings of interest.
 * @author jhermes
 *
 */
public class StringsOfInterest {
	
	private List<String> stringsOfInterest;
	private Map<String, String> stringsWithParticleConstraint;
	private Map<String, Integer> stringsFound;
	 
	public StringsOfInterest(){
		stringsOfInterest = new ArrayList<String>();
		stringsFound = new HashMap<String, Integer>();
		stringsWithParticleConstraint = new HashMap<String, String>();
	}
	
	/**
	 * Initializes a StringsOfInterest object containing the strings 
	 * specified in the file with the specified name
	 * @param filename Name of file containing the strings of interest
	 * @throws IOException
	 */
	public void setStringsOfInterest(String filename) throws IOException{
		
		
		BufferedReader in = new BufferedReader(new FileReader(new File(filename)));
		String nextString = in.readLine();
		while(nextString!=null){
			nextString = nextString.trim();
			processString(nextString);
			nextString = in.readLine();
		}
		in.close();
		System.out.println(stringsOfInterest);
		System.out.println(stringsWithParticleConstraint);
	}
	
	private void processString(String nextString){
		if(nextString!=""){
			String[] splits = nextString.split(",");
			
			stringsOfInterest.add(splits[0].trim());
			stringsFound.put(splits[0].trim(), 0);
			
			if(splits.length>1){
				stringsWithParticleConstraint.put(splits[0].trim(), splits[1].trim());
			}
			
		}
	}
	
	public void setStringsOfInterest(List<String> ofInterest){
		for (String string : ofInterest) {
			processString(string);
		}
	}
	
	public List<String> getStringsOfInterest() {
		return stringsOfInterest;
	}
	
	public int getStringsFoundOf(String string){
		return stringsFound.get(string);
	}
	
	public String hasConstraint(String string){
		return stringsWithParticleConstraint.get(string);
	}

	
	public int getId(String string){
		int toReturn = -1;
		toReturn = stringsOfInterest.indexOf(string);
		return toReturn;
	}
	
	public boolean isOfInterest(String string){
		boolean toReturn = stringsOfInterest.contains(string);
		if(toReturn){
			stringsFound.put(string, stringsFound.get(string)+1);
		}
//		if(toReturn){
//			System.out.println(string);
//		}
		return toReturn;
	}
	
	public void printMap(){
		Set<String> keySet = stringsFound.keySet();
		for (String string : keySet) {
			System.out.println(string + ": " + stringsFound.get(string));
		}
	}
	
	public String toString(){
		return stringsOfInterest.toString();
	}

}