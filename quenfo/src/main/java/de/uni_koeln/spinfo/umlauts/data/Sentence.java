package de.uni_koeln.spinfo.umlauts.data;

import java.util.List;

import opennlp.tools.util.Span;

public class Sentence {
	
	private Span sentenceSpan;
	private List<String> tokens;
	private Span[] tokenSpans;
	
	public Sentence(Span sentenceSpan, List<String> tokens, Span[] tokenSpans) {
		super();
		this.sentenceSpan = sentenceSpan;
		this.tokens = tokens;
		this.tokenSpans = tokenSpans;
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
	
	public Span getAbsoluteSpanOfToken(int tokenIndex) {
		return new Span(tokenSpans[tokenIndex].getStart()+sentenceSpan.getStart(),tokenSpans[tokenIndex].getEnd()+sentenceSpan.getEnd());
	}
	
	
}
