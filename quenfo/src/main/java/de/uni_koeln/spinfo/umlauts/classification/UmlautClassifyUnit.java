package de.uni_koeln.spinfo.umlauts.classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
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
	 * @param training inizialize training data/ evaluation data or data to classify
	 */
	public UmlautClassifyUnit(List<String> context, String word, String[] senses, boolean training) {
		super(word, new SingleToMultiClassConverter(senses.length,senses.length,null));
		List<String> cuContext = new ArrayList<String>();
		cuContext.addAll(context);
		cuContext.remove(word);
		super.setFeatureUnits(cuContext);
		this.senses = senses;
		if(training){
			setActualClassID(Arrays.asList(senses).indexOf(word)+1);
		} else {
			setActualClassID(-1);
		}
		
	}
	

	public String getSense(){
		return senses[actualClassID-1];
		
	}
	
	public String getSense(boolean[] classification){
		int classID = 0;
		for (int i = 0; i < classification.length; i++) {
			if(classification[i]){
				classID = i+1;
			}
		}
		//failsafe
		if(classID-1<0 || classID-1>senses.length-1){
			return senses[0];
		} else {
			return senses[classID-1];
		}
		
	}
	
	

}
