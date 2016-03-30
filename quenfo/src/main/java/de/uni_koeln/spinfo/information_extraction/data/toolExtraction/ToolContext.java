package de.uni_koeln.spinfo.information_extraction.data.toolExtraction;

import java.util.ArrayList;
import java.util.List;

import de.uni_koeln.spinfo.information_extraction.data.Token;
import javafx.scene.AmbientLight;

public class ToolContext {
	
	List<Token> tokens = new ArrayList<Token>();
	private int amPointer;
	double conf = 0.0;

	
	public void setConf(double conf){
		this.conf = conf;
	}
	
	public double getConf(){
		return conf;
	}
	
	public void addToken(Token toAdd){
		tokens.add(toAdd);
	}
	
	public void addToken(Token toAdd, boolean isExtractionElement){
		tokens.add(toAdd);
		if(isExtractionElement){
			amPointer = tokens.size()-1;
		}
	}
	
	public int getSize(){
		return tokens.size();
	}
	
	public Token getTokenAt(int index){
		return tokens.get(index);
	}
	public List<Token> getTokens(){
		return tokens;
	}
	
	public Token getAM(){
		return tokens.get(amPointer);
	}
	

	public int getToolPointer(){
		return amPointer;
	}
	
	public void setAMPointer(int pointer){
		this.amPointer = pointer;
	}
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i <tokens.size();i++) {
			Token token = tokens.get(i);
			if(i == amPointer){
				sb.append(token.toString()+" <-- \n");
			}
			else{
				sb.append(token.toString()+"\n");
			}
		}
		return sb.toString();
	}

}
