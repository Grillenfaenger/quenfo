package de.uni_koeln.spinfo.information_extraction.data.competenceExtraction;



/**
 * 
 * @author geduldia
 * 
 * represents a single Competence of a competenceUnit
 *
 */
public class Competence {
	/*competence-description*/
	private String competence = null;
	private String quality = null;
	private String importance;
//	private TYPE type = null;
	private int jobAdID = -1;;
	private int secondJobAdID = -1;
	
	public Competence(String competence, int jobAdID, int secondJobAdID){
		this.competence = competence;
		this.jobAdID = jobAdID;
		this.secondJobAdID = secondJobAdID;
	}
	
	public Competence(int jobAdID, int secondJobAdID){
		this.jobAdID = jobAdID;
	}
	
//	public Competence(String competence){
//		this.competence = competence;
//	}
	
//	public Competence(){
//		
//	}
	
	public int getJobAdID() {
		return jobAdID;
	}

	public int getSecondJobAdID(){
		return secondJobAdID;
	}

//	public void setType(TYPE type){
//		this.type = type;
//	}
//	
//	public TYPE getType(){
//		return type;
//	}
	
	
	
	public String getCompetence() {
		return competence;
	}

	public void setCompetence(String competence) {
		this.competence = competence;
	}
	public String getQuality() {
		return quality;
	}
	public void setQuality(String quality) {
		this.quality = quality;
	}
	public String getImportance() {
		return importance;
	}
	public void setImportance(String importance) {
		this.importance = importance;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("COMP:"+"\t" + competence+"\t" + quality+"\t"+ importance);
		return sb.toString();
	}

}
