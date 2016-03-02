package de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.synonym.WordnetSynonymParser;

import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.data.DependencyTree;
import de.uni_koeln.spinfo.information_extraction.data.WordNode;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;

public class AnalayseClass2App {

	// ComptenceData sollte in Klasse zwei am besten ohne InnerSentenceSplitting
	// erstellt werden, da vollständige Sätze benötigt werden

	// extrahiert alle Verben (= alle PosTags, die mit "V" beginnen)
	// inkl. deren Subjekte (= erster WordNode im Subtree mit Role "SB") und
	// inkl. aller Objekte (= alle WordNodes im Subtree mit Role "OA", "OA2",
	// "DA", "OC" oder "OG" und alle WordNodes mit Role "OP" oder "MO" inkl.
	// aller NNs im Subtree)

	private static IEJobs jobs = new IEJobs();
	private static File competenceUnitsFile = new File(
			"src/test/resources/information_extraction/competenceData_newTrainingData2016_2.txt");

	public static void main(String[] args) throws IOException {
		File outputFile = new File("src/test/resources/information_extraction/output/Subj_Verb_Obj_Data.txt");
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));

		List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(competenceUnitsFile);
		jobs.setSentenceData(compUnits, null);
		jobs.buildDependencyTrees(compUnits);
		for (CompetenceUnit cu : compUnits) {
			// System.out.println("SENTENCE: "+cu.getSentence());
			DependencyTree tree = cu.getDependencyTree();
			List<String> s_v_o = getSubjVerbObj(tree);
			if (s_v_o.size() > 0) {
				System.out.println("SENTENCE: " + cu.getSentence());
				out.write("SENTENCE: " + cu.getSentence() + "\n");
				System.out.println(tree);
				for (String string : s_v_o) {
					System.out.println(string);
					out.write(string + "\n");
				}
				out.write("\n");
				System.out.println();
			}
		}
		out.close();
	}

	private static List<String> getSubjVerbObj(DependencyTree tree) {
		List<String> toReturn = new ArrayList<String>();
		Map<WordNode, WordNode> verbNodes = new HashMap<WordNode, WordNode>();
		WordNode current = tree.getRoot();
		if (current.getPosTag().startsWith("V")) {
			// verbNodes.add(current);
			verbNodes.put(current, null);
		}
		if (current.getChilds() != null) {
			for (WordNode child : current.getChilds()) {
				getVerbNodes(child, verbNodes);
			}
		}

		for (WordNode verbNode : verbNodes.keySet()) {
			List<WordNode> obj = findObject(verbNode, new ArrayList<WordNode>());
			WordNode subj = findSubject(verbNode);
			StringBuffer sb = new StringBuffer();
			if (subj != null) {
				sb.append(subj.getRole() + " : " + subj.getLemma() + " ");
			}
			sb.append("V: " + verbNode.getLemma() + " ");
			if(verbNodes.get(verbNode) != null){
				sb.append(verbNodes.get(verbNode).getLemma()+" ");
			}
			if (obj.size() > 0) {
				for (int i = 0; i < obj.size(); i++) {
					WordNode o = obj.get(i);
					if(o.getParent().getPosTag().equals("KON")){
						toReturn.add(sb.toString());
						addKonjObj(obj,i,toReturn, verbNode, subj);
						break;
					}
					sb.append("   " + o.getRole() + ": " + o.getLemma() + "(" + o.getPosTag() + ")");
				}
			}
			toReturn.add(sb.toString());
		}
		return toReturn;
	}



	private static void addKonjObj(List<WordNode> obj, int i, List<String> toReturn, WordNode verbNode, WordNode subj) {
		StringBuffer sb = new StringBuffer();
		if(subj != null){
			sb.append(subj.getRole() + " : " + subj.getLemma() + " ");
		}
		sb.append("V: " + verbNode.getLemma() + " ");
		for(int j = i; j < obj.size(); j++){
			WordNode o = obj.get(j);
			sb.append("   " + o.getRole() + ": " + o.getLemma() + "(" + o.getPosTag() + ")");
		}
		toReturn.add(sb.toString());
	}

	private static WordNode findSubject(WordNode verbNode) {
		if (verbNode.getChilds() != null) {
			for (WordNode child : verbNode.getChilds()) {
				if (child.getRole().equals("SB")) {
					return child;
				}
				findSubject(child);
			}
		}
		return null;
	}

	private static List<WordNode> findObject(WordNode verbNode, List<WordNode> obj) {
		if (verbNode.getChilds() != null) {
			for (WordNode child : verbNode.getChilds()) {
				if (child.getRole().equals("OA") || child.getRole().equals("OA2") || child.getRole().equals("DA")
						|| child.getRole().equals("OC") || child.getRole().equals("OG")) {
					obj.add(child);
				}
				if (child.getRole().equals("OP") ||child.getRole().equals("MO")){
					List<WordNode> subTree = getSubTree(child);
					obj.addAll(subTree);

				}
				findObject(child, obj);
			}
		}
		return obj;
	}

	private static List<WordNode> getSubTree(WordNode wordNode) {
		List<WordNode> subTree = new ArrayList<WordNode>();
		subTree.add(wordNode);
		if (wordNode.getChilds() != null) {
			for (WordNode child : wordNode.getChilds()) {
				if (child.getPosTag().equals("NN") || child.getPosTag().equals("NE")) {
					getSubtree(child, subTree);
				}
			}
		}
		return subTree;
	}

	private static void getSubtree(WordNode wordNode, List<WordNode> subTree) {
		if (wordNode.getPosTag().equals("NN") || wordNode.getPosTag().equals("NE")) {
			subTree.add(wordNode);
		}
		if (wordNode.getChilds() != null) {
			for (WordNode child : wordNode.getChilds()) {
				getSubtree(child, subTree);
			}
		}
	}

	private static void getVerbNodes(WordNode node, Map<WordNode, WordNode> verbNodes) {
		if (node.getPosTag().startsWith("V")) {
			// verbNodes.add(node)
			verbNodes.put(node, null);
		}
		if (node.getRole().equals("SVP")) {
			verbNodes.put(node.getParent(),node);
		}
		if (node.getChilds() != null) {
			for (WordNode child : node.getChilds()) {
				getVerbNodes(child, verbNodes);
			}
		}
	}
}
