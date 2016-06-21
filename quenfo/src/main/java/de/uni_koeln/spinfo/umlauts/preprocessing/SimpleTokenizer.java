package de.uni_koeln.spinfo.umlauts.preprocessing;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tokenizes texts into tokens (sequences of alphanumeric characters)
 *
 */
public class SimpleTokenizer {
	


	private String delimiter = "[^\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]]";
	
	/**
	 * Tokenizes specified text into sequences of alphanumeric characters
	 * @param text text
	 * @return List of tokens
	 */
	public List<String> tokenize(String text) {
		List<String> tokens = Arrays
				.asList((text.split(delimiter)));
		List<String> result = new ArrayList<String>();
		
		for (String token : tokens) {
			if (token.trim().length() > 0) {
				result.add(token.trim());
			}
		}
		return result;
	}
	
	public List<String> tokenizeWithStartPosition(String text) {
		List<String> tokens = Arrays
				.asList((text.split(delimiter)));
		List<String> result = new ArrayList<String>();
		
		for (String token : tokens) {
			if (token.trim().length() > 0) {
				result.add(token.trim());
			}
		}
		return result;
	}
	
	

}
