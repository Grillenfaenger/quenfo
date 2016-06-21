package de.uni_koeln.spinfo.umlauts.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import is2.parser.Parser;
import is2.tag.Tagger;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;

public class IEPreprocessingWrapper {
	
	public static IETokenizer tokenizer = new IETokenizer();
	
	public static List<List<String>> tokenizeWithIETokenizer(String string){
		List<String> sentences = tokenizer.splitIntoSentences(string, false);
		List<List<String>> tokenizedSentences = new ArrayList<List<String>>();
		
		for(String sentence : sentences) {
			String[] tokens = tokenizer.tokenizeSentence(sentence);
			List<String> tokenizedSentence = new ArrayList<String>(tokens.length);
			tokenizedSentence.addAll(Arrays.asList(tokens));
			tokenizedSentences.add(tokenizedSentence);
		}
		
		return tokenizedSentences;
		
	}
	
	public static List<List<String>> tokenizeWithPositions(String string){
		List<String> sentences = tokenizer.splitIntoSentences(string, false);
		List<List<String>> tokenizedSentences = new ArrayList<List<String>>();
		
		for(String sentence : sentences) {
			String[] tokens = tokenizer.tokenizeSentence(sentence);
			List<String> tokenizedSentence = new ArrayList<String>(tokens.length);
			tokenizedSentence.addAll(Arrays.asList(tokens));
			tokenizedSentences.add(tokenizedSentence);
		}
		
		return tokenizedSentences;
		
	}
	
	public static List<SentenceData09> completeLinguisticPreprocessing(String string, boolean onlyLemmata){
		List<String> sentences = tokenizer.splitIntoSentences(string, false);
		List<SentenceData09> sdList = new ArrayList<SentenceData09>();
		
		is2.tools.Tool lemmatizer = new Lemmatizer(
				"information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		is2.mtag.Tagger morphTagger  = null;
		is2.tools.Tool  tagger = null;
		is2.tools.Tool  parser = null;
		if(!onlyLemmata){
			morphTagger = new is2.mtag.Tagger(
					"information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/morphology-ger-3.6.model");
			tagger = new Tagger("information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");
			parser = new Parser(
					"information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/parser-ger-3.6.model");
		}
		

		SentenceData09 sd = new SentenceData09();
		for (String sentence : sentences){
			sd.init(tokenizer.tokenizeSentence("<root> " + sentence));
			lemmatizer.apply(sd);
			if(!onlyLemmata){
				morphTagger.apply(sd);
				tagger.apply(sd);
				sd = parser.apply(sd);
			}		
			sdList.add(sd);
		}
		return sdList;
	}

}
