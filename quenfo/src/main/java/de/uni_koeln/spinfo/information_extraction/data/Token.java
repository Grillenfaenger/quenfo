package de.uni_koeln.spinfo.information_extraction.data;

public class Token {

	private String posTag;
	private String lemma;
	private String string;
	private boolean isAM;
	private boolean isStartOfAM;
	private boolean isNoAM;
	private boolean canBeRepeated;
	private int required;
	
	
	
	
	
	public boolean isNoAM() {
		return isNoAM;
	}

	public void setNoAM(boolean isNoAM) {
		this.isNoAM = isNoAM;
	}

	public boolean canBeRepeated() {
		return canBeRepeated;
	}

	public void setCanBeRepeated(boolean canBeRepeated) {
		this.canBeRepeated = canBeRepeated;
	}

	public boolean isStartOfAM() {
		return isStartOfAM;
	}

	public void setIsStartOfAM(boolean isPartOfAM) {
		this.isStartOfAM = isPartOfAM;
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
		this.isAM = isAM;
	}
	
	public void setAM(boolean isAm){
		this.isAM = isAm;
	}
	
	public String getPosTag() {
		return posTag;
	}

	public String getLemma() {
		return lemma;
	}

	public boolean isAM(){
		return isAM;
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
		if(contextToken.isAM){
			if(this.isStartOfAM) return isStartOfAM;
			return isAM;
		}
		return true;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(string+"\t"+lemma+"\t"+posTag+"\t"+isAM+"\t"+isStartOfAM+"\t"+required);
		return sb.toString();
	}

}
