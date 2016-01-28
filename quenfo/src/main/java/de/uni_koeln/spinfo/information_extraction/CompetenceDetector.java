package de.uni_koeln.spinfo.information_extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.tree.TreeNode;

import org.apache.hadoop.io.SortedMapWritable;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;

import com.maxgarfinkel.suffixTree.Word;
import com.sun.media.jfxmedia.track.SubtitleTrack;

import de.uni_koeln.spinfo.information_extraction.data.Competence;
import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.data.DependencyTree;
import de.uni_koeln.spinfo.information_extraction.data.WordNode;

public class CompetenceDetector {
	BufferedReader in;
	List<String> importanceTerms;
	List<String> qualityTerms;

	// Einlesen der Wortlisten
	public CompetenceDetector() throws IOException {
		in = new BufferedReader(new FileReader(new File(
				"information_extraction/data/importance_terms.txt")));
		String line = in.readLine();
		importanceTerms = new ArrayList<String>();
		while (line != null) {
			if (!(line.startsWith("//")) && !(line.isEmpty())) {
				importanceTerms.add(line.trim());
			}
			line = in.readLine();
		}
		in.close();
		in = new BufferedReader(new FileReader(new File(
				"information_extraction/data/quality_terms.txt")));
		line = in.readLine();
		qualityTerms = new ArrayList<String>();
		while (line != null) {
			if (!(line.startsWith("//")) && !(line.isEmpty())) {
				qualityTerms.add(line.trim());
			}
			line = in.readLine();
		}
		in.close();
	}

	public void setCompetences(List<CompetenceUnit> cus) {
		for (CompetenceUnit cu : cus) {
			setVerbCompetences(cu);
			setNounCompetences(cu);
			if (cu.getCompetences() == null) {
				setAdjectiveCompetences(cu);
			}
		}
	}

	
	public void setAdjectiveCompetences(CompetenceUnit cu){
		List<WordNode> adj = getAdj(cu.getDependencyTree().getRoot());
		for (WordNode a : adj) {
			String mod = "";
			if (a.getRole().equals("MO")) {
				mod = a.getLemma();
			} else {
				Competence comp = new Competence(mod + " "
						+ a.getLemma(), cu.getJobAdID(), cu.getSecondJobAdID());
				cu.setCompetence(comp);
			}
		}
	}

	public void setVerbCompetences(CompetenceUnit cu) {
		DependencyTree tree = cu.getDependencyTree();
		if(tree.getRoot().getChilds()!= null){
			if (tree.getRoot().getLemma().equals("sein")||(tree.getRoot().getLemma().equals("werden"))) {
				List<WordNode> childs = tree.getRoot().getChilds();
				Map<Integer, WordNode> verbTree = new TreeMap<Integer, WordNode>();
				for (WordNode wordNode : childs) {
					if (wordNode.getPosTag().equals("NN")
							|| wordNode.getPosTag().equals("NE")
							|| wordNode.getPosTag().equals("ADJD")
							|| wordNode.getPosTag().equals("ADJA")
							|| wordNode.getPosTag().equals("VVPP")
							|| wordNode.getPosTag().equals("APPR")
							|| wordNode.getPosTag().equals("APPRART")) {
						Map<Integer, WordNode> subTree = getSubTree(wordNode);
						verbTree.putAll(subTree);
					}

				}
				Competence comp = new Competence(cu.getJobAdID(), cu.getSecondJobAdID());
				setCompValues(comp, verbTree.values());
				cu.setCompetence(comp);
			}
		}
	}

	public void setNounCompetences(CompetenceUnit cu) {
		List<Map<Integer, WordNode>> NNTrees = getNounTrees(cu);
		for (int i = 0; i < NNTrees.size(); i++) {
			//Map<Integer, WordNode> map = NNTrees.get(i);
			Competence comp = new Competence(cu.getJobAdID(), cu.getSecondJobAdID());
			setCompValues(comp, NNTrees.get(i).values());
			cu.setCompetence(comp);

		}
	}

	

	private Map<Integer, WordNode> getSubTree(WordNode wordNode) {
		Map<Integer, WordNode> subTree = new TreeMap<Integer, WordNode>();
		if (!wordNode.getLemma().equals("--")) {
			subTree.put(wordNode.getId(), wordNode);
		}
		if (wordNode.getChilds() != null) {
			for (WordNode child : wordNode.getChilds()) {
				getSubtree(child, subTree);
			}
		}
		return subTree;
	}

	private void getSubtree(WordNode wordNode, Map<Integer, WordNode> subTree) {
		if (!wordNode.getLemma().equals("--")) {
			subTree.put(wordNode.getId(), wordNode);
		}
		if (wordNode.getChilds() != null) {
			for (WordNode child : wordNode.getChilds()) {
				getSubtree(child, subTree);
			}
		}
	}

	
	private List<WordNode> getAdj(WordNode root) {
		List<WordNode> toReturn = new ArrayList<WordNode>();
		if (root.getPosTag().equals("ADJA") || root.getPosTag().equals("ADJD")) {
			toReturn.add(root);
		}
		if (root.getChilds() != null) {
			for (WordNode child : root.getChilds()) {
				getAdj(child, toReturn);
			}
		}
		return toReturn;
	}

	private void getAdj(WordNode child, List<WordNode> toReturn) {
		if (child.getPosTag().equals("ADJA")
				|| child.getPosTag().equals("ADJD")) {
			toReturn.add(child);
		}
		if (child.getChilds() != null) {
			for (WordNode node : child.getChilds()) {
				getAdj(node, toReturn);
			}
		}
	}

	private void setCompValues(Competence comp, Collection<WordNode> nodes) {
		Iterator<WordNode> it = nodes.iterator();
		StringBuffer sb = new StringBuffer();
		while (it.hasNext()) {
			WordNode next = it.next();
			if(!next.getPosTag().equals("ART"))
			sb.append(next.getLemma().trim() + " ");
		}
		String text = " "+sb.toString();
		for (String q : qualityTerms) {
			if (text.contains(" "+q+" ")) {
				comp.setQuality(q);
				text = text.replace(q, "");
				break;
			}
		}
		for (String i : importanceTerms) {
			if (text.contains(" "+i+" ")) {
				comp.setImportance(i);
				text = text.replace(i, "");
				break;
			}
		}
		comp.setCompetence(text.trim());
	}

	public List<Map<Integer, WordNode>> getNounTrees(CompetenceUnit cu) {
		List<Map<Integer, WordNode>> NNTrees = new ArrayList<Map<Integer, WordNode>>();
		List<WordNode> NNs = new ArrayList<WordNode>();
		DependencyTree tree = cu.getDependencyTree();
		WordNode current = tree.getRoot();
		if (current.getPosTag().equals("NN") || current.getPosTag().equals("NE")) {
			NNs.add(current);
		}
		if (current.getChilds() != null) {
			for (WordNode child : current.getChilds()) {
				getNodesWithPosTag(child, NNs, "NN");
				getNodesWithPosTag(child, NNs, "NE");
			}
		}

		for (WordNode wordNode : NNs) {
			Map<Integer, WordNode> subTree = new TreeMap<Integer, WordNode>();
			getSubtree(wordNode, subTree);
			NNTrees.add(subTree);
		}
		return NNTrees;
	}

	private void getNodesWithPosTag(WordNode node, List<WordNode> NNs,
			String posTag) {
		if (node.getPosTag().equals(posTag)) {
			NNs.add(node);
		}
		if (node.getChilds() != null) {
			for (WordNode child : node.getChilds()) {
				getNodesWithPosTag(child, NNs, posTag);
			}
		}
	}

}
