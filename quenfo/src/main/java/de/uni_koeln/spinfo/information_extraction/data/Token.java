package de.uni_koeln.spinfo.information_extraction.data;

public class Token {

	private String posTag;
	private String lemma;
	private String string;
	private boolean isTool;
	private boolean isStartOfTool;
	private boolean isNoTool;
	private int required;
	
	private int potentialContextCount;
	
	
	
	
	public boolean isNoTool() {
		return isNoTool;
	}

	public void setNoTool(boolean isNoTool) {
		this.isNoTool = isNoTool;
	}

	public boolean isStartOfTool() {
		return isStartOfTool;
	}

	public void setIsStartOfTool(boolean isStartOfTool) {
		this.isStartOfTool = isStartOfTool;
	}

	public int getRequired() {
		return required;
	}

	public void setRequired(int required) {
		this.required = required;
	}

	public Token(String string, String lemma, String posTag){
		this.posTag = posTag;
		this.string = string;
		this.lemma = lemma;
	}
	
	public Token(String string, String lemma, String posTag, boolean isAM){
		this.posTag = posTag;
		this.string = string;
		this.lemma = lemma;
		this.isTool = isAM;
	}
	
	public void setTool(boolean isTool){
		this.isTool = isTool;
	}
	
	public String getPosTag() {
		return posTag;
	}

	public String getLemma() {
		return lemma;
	}

	public boolean isTool(){
		return isTool;
	}
	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}
	
	
	public boolean isEqualsContextToken(Token contextToken){
		if(contextToken.getString() != null){
			if(!contextToken.getString().equals(string)){
				return false;
			}
		}
		if(contextToken.getPosTag() != null){
			String[] tags = contextToken.getPosTag().split("\\|");
			boolean match = false;
			for (String tag : tags) {
				match = tag.equals(this.posTag);
				if(match) break;
			}
			if(!match){
				return false;
			}
		}
		if(contextToken.getLemma()!= null){
			String[] lemmas = contextToken.getLemma().split("\\|");
			boolean match = false;
			for (String lemma : lemmas) {
				match = this.lemma.equals(lemma);
				if(match) break;
			}
			if(!match){
				return false;
			}
		}
		if(contextToken.isTool){
			if(this.isStartOfTool) return isStartOfTool;
			return isTool;
		}
		return true;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(string+"\t"+lemma+"\t"+posTag+"\t"+isTool+"\t"+isStartOfTool+"\t"+isNoTool+"\t"+required);
		return sb.toString();
	}

	public int getPotentialContextCount() {
		return potentialContextCount;
	}

	public void setPotentialContextCount(int potentialStartOfTool) {
		this.potentialContextCount = potentialStartOfTool;
	}

}
