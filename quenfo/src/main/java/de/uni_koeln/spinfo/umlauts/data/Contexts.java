package de.uni_koeln.spinfo.umlauts.data;
import java.util.List;


public class Contexts implements Comparable {
	
	private String keyword;
	private List<List<String>> contextList;
	
	public Contexts(String keyword, List<List<String>> contextList) {
		super();
		this.keyword = keyword;
		this.contextList = contextList;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public List<List<String>> getContextList() {
		return contextList;
	}
	public void addContext(List<String> context) {
		this.contextList.add(context);
	}
	public void addContexts(List<List<String>> contexts) {
		this.contextList.addAll(contexts);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(List<String> context : contextList){
			sb.append(context+"\n");
		}
		return "\n===========\n keyword=" + keyword + "\n contexts=\n" + sb.toString() + "]==========\n";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Contexts other = (Contexts) obj;
		if (keyword == null) {
			if (other.keyword != null)
				return false;
		} else if (!keyword.equals(other.keyword))
			return false;
		return true;
	}
	public int compareTo(Object arg0) {
		if (getClass() != arg0.getClass()) {
			return 0;
		} else {
			Contexts context2 = (Contexts) arg0;
			return context2.getKeyword().compareTo(keyword);
		}	
		
	}
	
	
	
	

}
