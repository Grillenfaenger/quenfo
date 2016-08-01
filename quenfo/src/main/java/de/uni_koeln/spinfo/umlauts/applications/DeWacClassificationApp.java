package de.uni_koeln.spinfo.umlauts.applications;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.umlauts.classification.UmlautClassifyUnit;
import de.uni_koeln.spinfo.umlauts.data.KeywordContexts;
import de.uni_koeln.spinfo.umlauts.data.TranslationVocabulary;
import de.uni_koeln.spinfo.umlauts.data.UmlautExperimentConfiguration;
import de.uni_koeln.spinfo.umlauts.data.Vocabulary;
import de.uni_koeln.spinfo.umlauts.dewag.DewacSplitter;
import de.uni_koeln.spinfo.umlauts.dewag.StringsOfInterest;
import de.uni_koeln.spinfo.umlauts.utils.FileUtils;

public class DeWacClassificationApp {
	
	/* Ziele
	 * 1. mehr Kontexte zu bekannten ambigen Wörtern
	 * 2. mehr ambige Wörter
	 */
	
	public static void main(String[] args) throws IOException{
		dataExtraction();
	}
		
	public static void dataExtraction() throws IOException{
		
		UmlautExperimentConfiguration config = new UmlautExperimentConfiguration(null, null, null, null, null, false, 3, 3);
		
		DewacSplitter dewac = new DewacSplitter("output//dewac//");
		
//		for(int i = 1000; i <=2000; i++){
//			dewag.sentencesWithUmlautsToFile(new File("input//dewac//sdewac-v3.tagged_"+i), 10000, i);
//		}
		
		Vocabulary voc = new Vocabulary();
		for(int i = 1000; i <=2000; i++){
			List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//dewac//ofInterest"+i+".txt"));
			for (List<String> list : tokenizedSentences) {
				voc.addTokens(list);
			}
		}
		
//		List<List<String>> tokenizedSentences = dewag.getTokenizedSentences(new File("output//dewac//ofInterest1033.txt"));
//		System.out.println("zum vokabular hinzufügen");
//		for (List<String> list : tokenizedSentences) {
//			voc.addTokens(list);
//		}
		
		System.out.println("Tokens: " + voc.getNumberOfTokens());
		System.out.println("Types: " + voc.vocabulary.size());
		
		
		Vocabulary umlautVoc = voc.getAllByRegex(".*([ÄäÖöÜü]).*");
		System.out.println("Token mit Umlaut: " + umlautVoc.getNumberOfTokens());
		System.out.println("Types mit Umlaut: " + umlautVoc.vocabulary.size());
		
		Vocabulary darkVowelVoc = umlautVoc.getAllByRegex(".*([AaOoUu]).*");
		darkVowelVoc.generateNumberOfTokens();
		System.out.println("Wörter mit dunklem Vokal: " + darkVowelVoc.getNumberOfTokens());
		System.out.println("Types mit dunklem Vokal: " + darkVowelVoc.vocabulary.size());
	
		TranslationVocabulary transVoc = new TranslationVocabulary();
		for (String key : umlautVoc.vocabulary.keySet()) {
			transVoc.addEntry(key);
		}
		
		Map<String, HashSet<String>> ambiguities = null;
		
		ambiguities = transVoc.findAmbiguities(voc);
		FileUtils.printMap(ambiguities, "output//classification//", "DewacAmbigeWörter");
		System.out.println(ambiguities.size() + " Gruppen mehrdeutiger Wörter gefunden");
		
		
		
		
		// Kontexte suchen
		// dewac-Splitter funktionen verwenden!
		
		
		for(HashSet<String> set : ambiguities.values()){
			for(String word : set){
				System.out.println("Extract Sentences for " + word );
				List<String> ofInterest = new ArrayList<String>(1);
				ofInterest.add(word);
				StringsOfInterest soi = new StringsOfInterest();
				soi.setStringsOfInterest(ofInterest);
				dewac.sentencesOfInterestToFile(new File("input//dewac_singlefile//sdewac-v3.tagged"), soi, 50);
			}
		}
		
		KeywordContexts contexts = new KeywordContexts();
		for(HashSet<String> set : ambiguities.values()){
			for(String word : set){
				System.out.println("Extract context for " + word );
				List<List<String>> tokenizedSentences = dewac.getTokenizedSentences(new File("output//dewac//byWord//ofInterest"+word+".txt"));
				contexts.addContexts(word, tokenizedSentences);
			}
		}
		contexts.printKeywordContexts("output//classification//", "DewacKontexte");
		contexts.printStats();
		
		
	
	
	}

}
