package de.uni_koeln.spinfo.umlauts.classification;

import java.util.Arrays;
import java.util.List;

import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
/**
 * 
 */
public class UmlautClassifyUnit extends ZoneClassifyUnit{
	
	// String content  - speichert anders als die JASCClassifyUnit nicht den Abschnitt, sondern das Wort.
	String[] senses;
	List<String> fullSentence;
	
	/**
	 * 
	 * @param context - pre-tokenized context of the word
	 * @param word
	 * @param senses
	 * @param training inizialize training data or not (ie data to classify)
	 */
	public UmlautClassifyUnit(List<String> context, String word, String[] senses, boolean training) {
		super(word);
		context.remove(word);
		super.setFeatureUnits(context);
		this.senses = senses;
		if(training){
			setActualClassID(Arrays.asList(senses).indexOf(word)+1);
		} else {
			setActualClassID(-1);
		}
		setClassIDs(null);
		
	}
	
	public String getSense(){
		return senses[actualClassID-1];
		
	}
	
	

}
