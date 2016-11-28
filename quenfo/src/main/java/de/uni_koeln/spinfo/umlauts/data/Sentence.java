package de.uni_koeln.spinfo.umlauts.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.util.Span;

public class Sentence {
	
	private Span sentenceSpan;
	private List<String> tokens;
	private Span[] tokenSpans;
	private Map<String,List<Span>> tokenPos;
	
	public Sentence(Span sentenceSpan, List<String> tokens, Span[] tokenSpans) {
		super();
		this.sentenceSpan = sentenceSpan;
		this.tokens = tokens;
		this.tokenSpans = tokenSpans;
		tokenPos = new HashMap<String,List<Span>>();
		createTokenPos();
	}

	public Span getSentenceSpan() {
		return sentenceSpan;
	}

	public List<String> getTokens() {
		return tokens;
	}

	public Span[] getTokenSpans() {
		return tokenSpans;
	}
	
	public Map<String,List<Span>> getTokenPos(){
		return tokenPos;
	}
	
	public Span getAbsoluteSpanOfToken(int tokenIndex) {
		return new Span(tokenSpans[tokenIndex].getStart()+sentenceSpan.getStart(),tokenSpans[tokenIndex].getEnd()+sentenceSpan.getStart());
	}
	
	private void createTokenPos(){
		for (int i = 0; i < tokens.size(); i++) {
			if(tokenPos.containsKey(tokens.get(i))){
				tokenPos.get(tokens.get(i)).add(getAbsoluteSpanOfToken(i));
			} else {
				List<Span> spans = new ArrayList<Span>();
				spans.add(getAbsoluteSpanOfToken(i));
				tokenPos.put(tokens.get(i),spans);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private List<String> getContext(int index, int rangeBefore, int rangeAfter){
				
				int fromIndex = index-rangeBefore;
				int toIndex = index+rangeAfter;
						
				if(fromIndex<0){
					fromIndex = 0;
				}
				if(toIndex>tokens.size()){
					toIndex = tokens.size();
				}
			
		return tokens.subList(fromIndex, toIndex);
	}
	
	
}
