package de.uni_koeln.spinfo.information_extraction.data.toolExtraction;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Tool {
	
	String word;
	boolean complete;
	List<String> context;
	
	
	public Tool(String word, boolean complete){
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
	
	public void addToContext(String lemma){
		if(context == null){
			context = new ArrayList<String>();
		}
		context.add(lemma);
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
		Tool am = (Tool) o;
		return new EqualsBuilder().append(word, am.word).append(complete, am.complete).append(context, am.context).isEquals();
	}
	
	@Override
	public String toString(){
		if(isComplete()){
			return word;
		}
		else{
			StringBuffer sb = new StringBuffer();
			for (String string : context) {
				sb.append(string+" ");
			}
			return sb.toString().substring(0,sb.length()-1);
		}
	}
	

}
