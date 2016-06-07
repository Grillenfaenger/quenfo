package de.uni_koeln.spinfo.umlauts.data;

public class JobAd {
	
	
	private String content;
	private int jahrgang;
	private int zeilennummer;
	
	public JobAd(int jahrgang, int zeilennummer, String content) {
		this.jahrgang = jahrgang;
		this.zeilennummer = zeilennummer;
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content){
		this.content = content;
	}


	public int getJahrgang() {
		return jahrgang;
	}


	public int getZeilennummer() {
		return zeilennummer;
	}


	


}
