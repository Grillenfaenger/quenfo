package de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.synonym.WordnetSynonymParser;

import com.maxgarfinkel.suffixTree.Word;

public class WordNode {

	private List<WordNode> childs = null;;
	private WordNode parent = null;
	private String lemma;
	private String posTag;
	private String morphTag;
	private String role;
	private int id;



	public WordNode(int id, String lemma, String posTag, String morphTag,
			String role) {
		this.id = id;
		this.lemma = lemma;
		this.morphTag = morphTag;
		this.posTag = posTag;
		this.role = role;
	}


	public String getRole() {
		return role;
	}

	public int getId() {
		return id;
	}

	public String getLemma() {
		return lemma;
	}

	public String getPosTag() {
		return posTag;
	}

	public String getMorphTag() {
		return morphTag;
	}

	public void setParent(WordNode parent) {
		this.parent = parent;
	}

	public List<WordNode> getChilds() {
		return childs;
	}

	public WordNode getParent() {
		return parent;
	}

	public void addChild(WordNode child) {
		if (childs == null) {
			childs = new ArrayList<WordNode>();
			//child.setChildID(0);
		}
		childs.add(child);		
	}


}
