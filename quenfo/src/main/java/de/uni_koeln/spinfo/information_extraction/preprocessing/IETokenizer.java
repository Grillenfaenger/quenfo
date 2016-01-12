package de.uni_koeln.spinfo.information_extraction.preprocessing;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import de.uni_koeln.spinfo.classification.jasc.preprocessing.ClassifyUnitSplitter;

public class IETokenizer {

	private TokenizerModel tokenizeModel;
	private SentenceModel sentenceModel;
	
	public IETokenizer(){
		setTokenizeModel("information_extraction/data/openNLPmodels/de-token.bin");
		setSentenceSplittingModel("information_extraction/data/openNLPmodels/de-sent.bin");
	}
	
	private void setSentenceSplittingModel(String model){
		InputStream modelIn = null;
		try {
			modelIn = new FileInputStream(model);
			sentenceModel = new SentenceModel(modelIn);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void setTokenizeModel(String model) {
		InputStream modelIn = null;
		try {
			modelIn = new FileInputStream(model);
			tokenizeModel = new TokenizerModel(modelIn);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	
	public List<String> splitListItems(String text){
		List<String> toReturn = new ArrayList<String>();
		List<String> splitted = ClassifyUnitSplitter.splitAtNewLine(text);
		Pattern pattern = Pattern.compile("(^[0-9](\\.)?(\\))?)|^(-|\\+|¿)");
		for (String string : splitted) {
			string = string.trim();
			Matcher m = pattern.matcher(string);
			if(m.find()){
				string = string.substring(m.end());
				string = string.trim();
			}
			
			if (string.length() > 0 && !ClassifyUnitSplitter.containsOnlyNonWordChars(string)) {
				toReturn.add(string);
			}
		}
		return toReturn;
	}
	
	public List<String> splitIntoSentences(String text, boolean innerSentenceSplitting){
		String[] sentences = null;

		SentenceDetectorME detector = new SentenceDetectorME(sentenceModel);
		sentences = detector.sentDetect(text);
		List<String> toReturn = new ArrayList<String>();
		for (String string : sentences) {
			List<String> splitted = splitListItems(string);
			if(innerSentenceSplitting){
				for (String s : splitted) {
					String[] split = s.split(" und | oder | sowie |, ");
					toReturn.addAll(Arrays.asList(split));
				}
			}
			else{
				toReturn.addAll(splitted);
			}
			
		}
		return toReturn;
	}
	
	/**
	 * Splits a sentence into tokens.
	 * 
	 * @param sentence
	 *            String to be tokenized, should be result of sentence splitting
	 * @return Array of tokens
	 */
	public String[] tokenizeSentence(String sentence) {
		String tokens[] = null;
		Tokenizer tokenizer = new TokenizerME(tokenizeModel);
		tokens = tokenizer.tokenize(sentence);
		return tokens;
	}
}
