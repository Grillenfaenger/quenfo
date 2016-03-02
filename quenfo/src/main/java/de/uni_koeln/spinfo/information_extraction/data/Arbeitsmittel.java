package de.uni_koeln.spinfo.information_extraction.data;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.hash.HashCode;

public class Arbeitsmittel {
	
	String word;
	boolean complete;
	List<String> context;
	
	
	public Arbeitsmittel(String word, boolean complete){
		this.word = word;
		this.complete = complete;
	}


	public String getWord() {
		return word;
	}


	public void setWord(String word) {
		this.word = word;
	}


	public boolean isComplete() {
		return complete;
	}


	public void setComplete(boolean complete) {
		this.complete = complete;
	}


	public List<String> getContext() {
		return context;
	}


	public void setContext(List<String> required) {
		this.context = required;
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder(3,17).append(word).append(complete).append(context).toHashCode();
	}

	
	@Override
	public boolean equals(Object o){
		Arbeitsmittel am = (Arbeitsmittel) o;
		return new EqualsBuilder().append(word, am.word).append(complete, am.complete).append(context, am.context).isEquals();
	}

}
