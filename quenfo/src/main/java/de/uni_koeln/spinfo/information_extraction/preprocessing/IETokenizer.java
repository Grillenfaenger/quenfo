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
import opennlp.tools.util.Span;
import de.uni_koeln.spinfo.classification.jasc.preprocessing.ClassifyUnitSplitter;
import de.uni_koeln.spinfo.umlauts.data.Sentence;


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
//		/*TEST*/ Span[] positions = null;

		SentenceDetectorME detector = new SentenceDetectorME(sentenceModel);
		sentences = detector.sentDetect(text);
//		/*TEST*/positions = detector.sentPosDetect(text);
		
//		/*TEST start*/
//		for (int i = 0; i < positions.length; i++) {
//			System.out.println(positions[i].getStart() + " " + positions[i].getEnd());
//			System.out.println(text.substring(positions[i].getStart(), positions[i].getEnd()));
//		}
//		/*TEST end*/
		
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
	
	public List<Sentence> tokenizeWithPositions(String text, boolean innerSentenceSplitting){
		if(text==null){
			text="";
		}
		String[] sentences = null;
		Span[] spans = null;
		Tokenizer tokenizer = new TokenizerME(tokenizeModel);
		
		// Split into sentences, keep spans
		SentenceDetectorME detector = new SentenceDetectorME(sentenceModel);
		sentences = detector.sentDetect(text);
		spans = detector.sentPosDetect(text);
		
		// Split sentence into tokens, keep Positions within sentence
		
		List<Sentence> sentenceTokens = new ArrayList<Sentence>(sentences.length);
		for (int i = 0; i < sentences.length; i++) {
			String sentence = sentences[i];
			List<String> tokens = null;
			Span[] tokenSpans = null;
			tokens = Arrays.asList(tokenizer.tokenize(sentence));
			tokenSpans = tokenizer.tokenizePos(sentence);
			
			sentenceTokens.add(new Sentence(spans[i],tokens, tokenSpans));
		}
		return sentenceTokens;
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
		try {
			tokens = tokenizer.tokenize(sentence);
		} catch (NullPointerException e) {
			tokens = new String[0];
		}
		return tokens;
	}
}
