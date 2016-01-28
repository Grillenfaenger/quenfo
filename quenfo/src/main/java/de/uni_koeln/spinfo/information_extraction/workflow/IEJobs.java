package de.uni_koeln.spinfo.information_extraction.workflow;

import is2.data.SentenceData09;
import is2.io.CONLLWriter09;
import is2.lemmatizer.Lemmatizer;
import is2.parser.Parser;
import is2.tag.Tagger;
import is2.tools.Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.output.FileWriterWithEncoding;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.preprocessing.TrainingDataGenerator;
import de.uni_koeln.spinfo.information_extraction.CompetenceDetector;
import de.uni_koeln.spinfo.information_extraction.data.Competence;
import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.data.DependencyTree;
import de.uni_koeln.spinfo.information_extraction.data.WordNode;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;

/**
 * 
 * @author geduldia
 * 
 */

public class IEJobs {

	/**
	 * Methode zum Filtern von ClassifyUnits bestimmter Klassen
	 * 
	 * @param toFilter
	 * @param classes
	 *            die Klassen die gefiltert werden sollen
	 * @return filtered
	 */
	public List<ClassifyUnit> filterClassifyUnits(List<ClassifyUnit> toFilter,
			Integer[] classes) {
		List<ClassifyUnit> filtered = new ArrayList<ClassifyUnit>();
		for (ClassifyUnit classifyUnit : toFilter) {
			ZoneClassifyUnit zCU = (ZoneClassifyUnit) classifyUnit;
			for (Integer i : classes) {
				if (zCU.getActualClassID() == i) {
					filtered.add(classifyUnit);
				}
			}
		}
		return filtered;
	}

	/**
	 * schreibt alle gefilterten ClassifyUnits in eine Textdatei
	 * 
	 * @param toWrite
	 * @param outputFile
	 * @throws IOException
	 */
	public void writeFilteredClassifyUnits(List<ClassifyUnit> toWrite,
			File outputFile) throws IOException {
		TrainingDataGenerator tdg = new TrainingDataGenerator(null);
		tdg.writeTrainingDataFile(outputFile, toWrite);
	}

	/**
	 * Methode zum Einlesen von (gefilterten) ClassifyUnits
	 * 
	 * @param inputFile
	 * @return toReturn Liste von ClassifyUnits
	 * @throws IOException
	 */
	public List<ClassifyUnit> readFilteredClassifyUnitsFromFile(File inputFile)
			throws IOException {
		TrainingDataGenerator tdg = new TrainingDataGenerator(inputFile);
		List<ClassifyUnit> toReturn = tdg.getTrainingData();
		return toReturn;
	}

	/**
	 * (nur zum Testen. Wird im Workflow nicht benötigt -->
	 * initializeCompetenceUnits) Ordnet jeder ClassifyUnit eine Liste ihrer
	 * Sätze zu (potentielle CompetenceUnits)
	 * 
	 * 
	 * @param classifyUnits
	 * @return toReturn Map von ClassifyUnits (key) und Liste ihrer Sentences
	 *         (value)
	 */
	public Map<ClassifyUnit, List<String>> getSentences(
			List<ClassifyUnit> classifyUnits, boolean splitInSentence) {
		Map<ClassifyUnit, List<String>> toReturn = new HashMap<ClassifyUnit, List<String>>();
		IETokenizer tokenizer = new IETokenizer();
		for (ClassifyUnit compCU : classifyUnits) {
			List<String> currentSentences = tokenizer.splitIntoSentences(
					compCU.getContent(), splitInSentence);
			toReturn.put(compCU, currentSentences);
		}
		return toReturn;
	}

	/**
	 * Methode zum Herausschreiben von ClassifyUnits und ihren Sentences
	 * 
	 * @param toWrite
	 * @param outputFile
	 * @throws IOException
	 */
	public void writeSentencesFile(Map<ClassifyUnit, List<String>> sentences,
			File outputFile) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		for (ClassifyUnit cu : sentences.keySet()) {
			out.print(cu.getID() + "\t");
			if (cu instanceof JASCClassifyUnit) {
				out.print(((JASCClassifyUnit) cu).getParentID() + "\t");
			}
			out.print(((ZoneClassifyUnit) cu).getActualClassID() + "\n");
			for (String sentence : sentences.get(cu)) {
				out.println("SENTENCE: " + sentence + "\n");
			}
		}
		out.flush();
		out.close();
	}

	/**
	 * erzeugt für jeden Satz ein CompetenceUnit-Object. Bestehend aus sentence,
	 * JobAdID(=parentID der ClassifyUnit) und ClassifyUnitID (= UUID der
	 * ClassifyUnit)
	 * 
	 * @param cus
	 * @return
	 */
	public List<CompetenceUnit> initializeCompetenceUnits(
			List<ClassifyUnit> cus, boolean splitInSentence) {
		List<CompetenceUnit> toReturn = new ArrayList<CompetenceUnit>();
		IETokenizer tokenizer = new IETokenizer();
		for (ClassifyUnit cu : cus) {
			List<String> sentences = tokenizer.splitIntoSentences(
					cu.getContent(), splitInSentence);
			for (String sentence : sentences) {
				if (containsCompetences(sentence) && sentence.length() > 1) {
					CompetenceUnit compUnit = new CompetenceUnit();
					compUnit.setSentence(sentence);
					compUnit.setJobAdID(((JASCClassifyUnit) cu).getParentID());
					compUnit.setSecondJobAdID(((JASCClassifyUnit) cu).getSecondParentID());
					compUnit.setClassifyUnitID(cu.getID());
					compUnit.setJobAdID(((JASCClassifyUnit) cu).getParentID());
					toReturn.add(compUnit);
				}
			}
		}
		return toReturn;
	}

	private boolean containsCompetences(String sentence) {
		if (sentence.endsWith(":")) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param compUnits
	 *            List of CompetenceUnits (initialized by
	 *            initializeSentenceData())
	 * @param sdOutputFileName
	 *            outputFile for CONLLWriter09
	 * @throws IOException
	 */
	public void setSentenceData(List<CompetenceUnit> compUnits,
			String sdOutputFileName) throws IOException {
		IETokenizer tokenizer = new IETokenizer();
		Tool lemmatizer = new Lemmatizer(
				"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		is2.mtag.Tagger morphTagger = new is2.mtag.Tagger(
				"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/morphology-ger-3.6.model");
		Tool tagger = new Tagger(
				"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");
		Tool parser = new Parser(
				"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/parser-ger-3.6.model");
		CONLLWriter09 writer = null;
		if (sdOutputFileName != null) {
			writer = new is2.io.CONLLWriter09(sdOutputFileName + ".csv");
		}
		for (CompetenceUnit compUnit : compUnits) {
			SentenceData09 sd = new SentenceData09();
			sd.init(tokenizer.tokenizeSentence("<root> "
					+ compUnit.getSentence()));
			lemmatizer.apply(sd);
			morphTagger.apply(sd);
			tagger.apply(sd);
			sd = parser.apply(sd);
			compUnit.setSentenceData(sd);
			if (writer != null) {
				writer.write(sd);
			}
		}
		if (writer != null) {
			writer.finishWriting();
		}
		lemmatizer = null;
		morphTagger = null;
		tagger = null;
	}

	public void buildDependencyTrees(List<CompetenceUnit> cus) {
		for (CompetenceUnit cu : cus) {
			DependencyTree tree = new DependencyTree();
			SentenceData09 sd = cu.getSentenceData();
			Set<Integer> listed = new HashSet<Integer>();
			Set<Integer> notListed = new HashSet<Integer>();

			int[] heads = sd.pheads;
			for (int i = 0; i < heads.length; i++) {
				int id = i + 1;
				int head = heads[i];
				if (head == 0) {
					// is Root
					tree.setRoot(new WordNode(id, sd.plemmas[i], sd.ppos[i],
							sd.pfeats[i], sd.plabels[i]));
					listed.add(id);
				} else {
					if (listed.contains(head)) {
						WordNode node = new WordNode(id, sd.plemmas[i],
								sd.ppos[i], sd.pfeats[i], sd.plabels[i]);
						tree.add(node, head);
						listed.add(id);
					} else {
						notListed.add(id);
					}
				}
			}

			while (listed.size() < heads.length) {
				List<Integer> toRemove = new ArrayList<Integer>();
				for (int id : notListed) {
					int head = heads[id - 1];
					if (listed.contains(head)) {
						WordNode node = new WordNode(id, sd.plemmas[id - 1],
								sd.ppos[id - 1], sd.pfeats[id - 1],
								sd.plabels[id - 1]);
						tree.add(node, head);
						listed.add(id);
						toRemove.add(id);
					}
				}
				notListed.removeAll(toRemove);
			}
			cu.setDependencyTree(tree);
		}

	}

	public void buildDependencyTree(CompetenceUnit cu) {
		DependencyTree tree = new DependencyTree();
		SentenceData09 sd = cu.getSentenceData();
		Set<Integer> listed = new HashSet<Integer>();
		Set<Integer> notListed = new HashSet<Integer>();

		int[] heads = sd.pheads;
		for (int i = 0; i < heads.length; i++) {
			int id = i + 1;
			int head = heads[i];
			if (head == 0) {
				// is Root
				tree.setRoot(new WordNode(id, sd.plemmas[i], sd.ppos[i],
						sd.pfeats[i], sd.plabels[i]));
				listed.add(id);
			} else {
				if (listed.contains(head)) {
					WordNode node = new WordNode(id, sd.plemmas[i], sd.ppos[i],
							sd.pfeats[i], sd.plabels[i]);
					tree.add(node, head);
					listed.add(id);
				} else {
					notListed.add(id);
				}
			}
		}

		while (listed.size() < heads.length) {
			List<Integer> toRemove = new ArrayList<Integer>();
			for (int id : notListed) {
				int head = heads[id - 1];
				if (listed.contains(head)) {
					WordNode node = new WordNode(id, sd.plemmas[id - 1],
							sd.ppos[id - 1], sd.pfeats[id - 1],
							sd.plabels[id - 1]);
					tree.add(node, head);
					listed.add(id);
					toRemove.add(id);
				}
			}
			notListed.removeAll(toRemove);
		}
		cu.setDependencyTree(tree);
	}

	public void setCompetences(List<CompetenceUnit> compUnits)
			throws IOException {
		CompetenceDetector detector = new CompetenceDetector();
		detector.setCompetences(compUnits);
	}
	
	public List<CompetenceUnit> filterEmptyCompetenceUnits(List<CompetenceUnit> toFilter){
		List<CompetenceUnit> toReturn = new ArrayList<>();
		for (CompetenceUnit cu : toFilter) {
			if(cu.getCompetences() != null){
				toReturn.add(cu);
			}
		}
		return toReturn;
	}

	public void writeCompetenceData(List<CompetenceUnit> toWrite,
			File outputFile) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		for (CompetenceUnit compUnit : toWrite) {
			out.write("ID: " + compUnit.getClassifyUnitID() + "\n");
			out.write("JobAdID: "+ compUnit.getJobAdID()+"-"+compUnit.getSecondJobAdID()+"\n");
			
			out.write("SENTENCE: "+compUnit.getSentence() + "\n");
			if (compUnit.getCompetences() != null) {
				for (Competence comp : compUnit.getCompetences()) {
					out.write("COMP: " + comp.getCompetence() + "\t"
							+ comp.getQuality() + "\t" + comp.getImportance()
							+ "\n");
				}
			}
			out.write("\n");
		}
		out.close();
	}

	public List<CompetenceUnit> readCompetenceUnitsFromFile(File toRead) throws IOException{
		List<CompetenceUnit> toReturn = new ArrayList<CompetenceUnit>();
		BufferedReader in = new BufferedReader(new FileReader(toRead));
		String line = in.readLine();
		CompetenceUnit compUnit = null;
		while(line != null){
		
			
			if(line.startsWith("ID:")){
				if(compUnit!= null){
					toReturn.add(compUnit);
				}
				compUnit = new CompetenceUnit();
				compUnit.setClassifyUnitID(UUID.fromString(line.split(":")[1].trim()));
				line = in.readLine(); continue;
			}
			if(line.startsWith("JobAdID:")){
				int jobAdID = Integer.parseInt(line.split(":")[1].split("-")[0].trim());
				int secondJobAdID = Integer.parseInt(line.split(":")[1].split("-")[1].trim());
				compUnit.setJobAdID(jobAdID);
				compUnit.setSecondJobAdID(secondJobAdID);
				line = in.readLine(); continue;
			}
			if(line.startsWith("COMP:")){
				String[] split = line.split("COMP:")[1].split("\t");
				Competence comp = new Competence(split[0], compUnit.getJobAdID(), compUnit.getSecondJobAdID());
				comp.setQuality(split[1]);
				comp.setImportance(split[2]);
				compUnit.setCompetence(comp);
				line = in.readLine(); continue;
			}
			if(line.startsWith("SENTENCE: ")){
				compUnit.setSentence(line.split(":")[1].trim());
				line = in.readLine(); continue;
			}
			line = in.readLine();
		}
		in.close();
		return toReturn;
	}

	public List<ClassifyUnit> treatEncoding(List<ClassifyUnit> competenceCUs) {
		List<ClassifyUnit> toReturn = new ArrayList<ClassifyUnit>();
		for (ClassifyUnit classifyUnit : competenceCUs) {
			String content = classifyUnit.getContent();
			content = content.replace("Ã¤", "ä");
			content = content.replace("Ã¼", "ü");
			content = content.replace("Ã¶", "ö");
			content = content.replace("Ãœ", "Ü");
			content = content.replace("ã¶", "ö");
			content = content.replace("ã¤", "ä");
			content = content.replace("ÃŸ", "ß");
			content =  content.replace("ãÿ", "ß");
			content = content.replace("ã¼", "ü");
			content =  content.replace("Ã„", "Ä");
			content = content.replace("Ã–", "Ö");
			content = content.replace("ãœ", "ü");
			content = content.replace("Â¿", "-");	
			content = content.replace("Â€", "€");
			classifyUnit.setContent(content);
			toReturn.add(classifyUnit);
		}
		return toReturn;
	}

	

}
