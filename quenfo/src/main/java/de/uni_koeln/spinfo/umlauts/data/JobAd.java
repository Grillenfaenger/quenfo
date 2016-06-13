package de.uni_koeln.spinfo.umlauts.data;

public class JobAd {
	
	
	private String content;
	private int jahrgang;
	private int zeilennummer;
	private int id;
	
	public JobAd(int jahrgang, int zeilennummer, String content, int id) {
		this.jahrgang = jahrgang;
		this.zeilennummer = zeilennummer;
		this.content = content;
		this.id = id;
	}
	
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
	
	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return "JobAd\n[id=" + id + "\njahrgang=" + jahrgang
				+ "\nzeilennummer=" + zeilennummer + "\ncontent=" + content +  "]";
	}
	
	


	


}
