package de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.preprocessing.TrainingDataGenerator;
import de.uni_koeln.spinfo.information_extraction.CompetenceDetector;
import de.uni_koeln.spinfo.information_extraction.data.Competence;
import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.data.DependencyTree;
import de.uni_koeln.spinfo.information_extraction.data.Token;
import de.uni_koeln.spinfo.information_extraction.data.Tool;
import de.uni_koeln.spinfo.information_extraction.data.ToolContext;
import de.uni_koeln.spinfo.information_extraction.data.WordNode;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import is2.data.SentenceData09;
import is2.io.CONLLWriter09;
import is2.lemmatizer.Lemmatizer;
import is2.parser.Parser;
import is2.tag.Tagger;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

/**
 * 
 * @author geduldia
 * 
 */

public class IEJobs {

	private Map<String, Set<Tool>> tools;
	private Set<String> noTools;

	Map<String, Set<String>> sectorsFileTools = new HashMap<String, Set<String>>();
	Map<String, String> sectors = new HashMap<String, String>();

	/**
	 * Methode zum Filtern von ClassifyUnits bestimmter Klassen
	 * 
	 * @param toFilter
	 * @param classes
	 *            die Klassen die gefiltert werden sollen
	 * @return filtered
	 */
	public List<ClassifyUnit> filterClassifyUnits(List<ClassifyUnit> toFilter, Integer[] classes) {
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
	public void writeFilteredClassifyUnits(List<ClassifyUnit> toWrite, File outputFile) throws IOException {
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
	public List<ClassifyUnit> readFilteredClassifyUnitsFromFile(File inputFile) throws IOException {
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
	public Map<ClassifyUnit, List<String>> getSentences(List<ClassifyUnit> classifyUnits, boolean splitInSentence) {
		Map<ClassifyUnit, List<String>> toReturn = new HashMap<ClassifyUnit, List<String>>();
		IETokenizer tokenizer = new IETokenizer();
		for (ClassifyUnit compCU : classifyUnits) {
			List<String> currentSentences = tokenizer.splitIntoSentences(compCU.getContent(), splitInSentence);
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
	public void writeSentencesFile(Map<ClassifyUnit, List<String>> sentences, File outputFile) throws IOException {
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
	public List<CompetenceUnit> initializeCompetenceUnits(List<ClassifyUnit> cus, boolean splitInSentence) {
		List<CompetenceUnit> toReturn = new ArrayList<CompetenceUnit>();
		IETokenizer tokenizer = new IETokenizer();
		for (ClassifyUnit cu : cus) {
			List<String> sentences = tokenizer.splitIntoSentences(cu.getContent(), splitInSentence);
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
	public void setSentenceData(List<CompetenceUnit> compUnits, String sdOutputFileName) throws IOException {
		IETokenizer tokenizer = new IETokenizer();
		is2.tools.Tool lemmatizer = new Lemmatizer("models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		is2.mtag.Tagger morphTagger = new is2.mtag.Tagger(
				"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/morphology-ger-3.6.model");
		is2.tools.Tool tagger = new Tagger("models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");
		is2.tools.Tool parser = new Parser("models/ger-tagger+lemmatizer+morphology+graph-based-3.6/parser-ger-3.6.model");
		CONLLWriter09 writer = null;
		if (sdOutputFileName != null) {
			File file = new File(sdOutputFileName + ".csv");
			if (!file.exists()) {
				file.createNewFile();
			}
			writer = new is2.io.CONLLWriter09(sdOutputFileName + ".csv");
		}
		for (CompetenceUnit compUnit : compUnits) {
			SentenceData09 sd = new SentenceData09();
			sd.init(tokenizer.tokenizeSentence("<root> " + compUnit.getSentence()));
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
					tree.setRoot(new WordNode(id, sd.plemmas[i], sd.ppos[i], sd.pfeats[i], sd.plabels[i]));
					listed.add(id);
				} else {
					if (listed.contains(head)) {
						WordNode node = new WordNode(id, sd.plemmas[i], sd.ppos[i], sd.pfeats[i], sd.plabels[i]);
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
						WordNode node = new WordNode(id, sd.plemmas[id - 1], sd.ppos[id - 1], sd.pfeats[id - 1],
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
				tree.setRoot(new WordNode(id, sd.plemmas[i], sd.ppos[i], sd.pfeats[i], sd.plabels[i]));
				listed.add(id);
			} else {
				if (listed.contains(head)) {
					WordNode node = new WordNode(id, sd.plemmas[i], sd.ppos[i], sd.pfeats[i], sd.plabels[i]);
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
					WordNode node = new WordNode(id, sd.plemmas[id - 1], sd.ppos[id - 1], sd.pfeats[id - 1],
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

	public void setCompetences(List<CompetenceUnit> compUnits) throws IOException {
		CompetenceDetector detector = new CompetenceDetector();
		detector.setCompetences(compUnits);
	}

	public List<CompetenceUnit> filterEmptyCompetenceUnits(List<CompetenceUnit> toFilter) {
		List<CompetenceUnit> toReturn = new ArrayList<>();
		for (CompetenceUnit cu : toFilter) {
			if (cu.getCompetences() != null) {
				toReturn.add(cu);
			}
		}
		return toReturn;
	}

	public void writeCompetenceData(List<CompetenceUnit> toWrite, File outputFile) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		for (CompetenceUnit compUnit : toWrite) {
			out.write("ID: " + compUnit.getClassifyUnitID() + "\n");
			out.write("JobAdID: " + compUnit.getJobAdID() + "-" + compUnit.getSecondJobAdID() + "\n");

			out.write("SENTENCE: " + compUnit.getSentence() + "\n");
			if (compUnit.getCompetences() != null) {
				for (Competence comp : compUnit.getCompetences()) {
					out.write("COMP: " + comp.getCompetence() + "\t" + comp.getQuality() + "\t" + comp.getImportance()
							+ "\n");
				}
			}
			out.write("\n");
		}
		out.close();
	}

	public List<CompetenceUnit> readCompetenceUnitsFromFile(File toRead) throws IOException {
		List<CompetenceUnit> toReturn = new ArrayList<CompetenceUnit>();
		BufferedReader in = new BufferedReader(new FileReader(toRead));
		String line = in.readLine();
		CompetenceUnit compUnit = null;
		while (line != null) {
			if (line.startsWith("ID:")) {
				if (compUnit != null) {
					toReturn.add(compUnit);
				}
				compUnit = new CompetenceUnit();
				compUnit.setClassifyUnitID(UUID.fromString(line.split(":")[1].trim()));
				line = in.readLine();
				continue;
			}
			if (line.startsWith("JobAdID:")) {
				int jobAdID = Integer.parseInt(line.split(":")[1].split("-")[0].trim());
				int secondJobAdID = Integer.parseInt(line.split(":")[1].split("-")[1].trim());
				compUnit.setJobAdID(jobAdID);
				compUnit.setSecondJobAdID(secondJobAdID);
				line = in.readLine();
				continue;
			}
			if (line.startsWith("COMP:")) {
				String[] split = line.split("COMP:")[1].split("\t");
				Competence comp = new Competence(split[0], compUnit.getJobAdID(), compUnit.getSecondJobAdID());
				comp.setQuality(split[1]);
				comp.setImportance(split[2]);

				compUnit.setCompetence(comp);
				line = in.readLine();
				continue;
			}
			if (line.startsWith("SENTENCE: ")) {
				compUnit.setSentence(line.split("SENTENCE:")[1].trim());
				line = in.readLine();
				continue;
			}
			line = in.readLine();
		}
		in.close();
		return toReturn;
	}

	private List<ToolContext> readToolContextFile(File file) throws IOException {
		List<ToolContext> toReturn = new ArrayList<ToolContext>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = in.readLine();
		ToolContext context = new ToolContext();
		while (line != null) {
			String[] split = line.split("\t");
			if (line.startsWith("TOKEN:")) {
				String string = split[1];
				if (string.equals("null"))
					string = null;
				String lemma = split[2];
				if (lemma.equals("null"))
					lemma = null;
				String posTag = split[3];
				if (posTag.equals("null"))
					posTag = null;
				Token token = new Token(string, lemma, posTag, Boolean.parseBoolean(split[4]));
				context.addToken(token);
			}
			if (line.startsWith("TOOL:")) {
				context.setAMPointer(Integer.parseInt(split[1]));
			}
			if (line.startsWith("CONF:")) {
				context.setConf(Double.parseDouble(split[1]));
				toReturn.add(context);
				context = new ToolContext();
			}
			line = in.readLine();
		}
		in.close();
		return toReturn;
	}

	public void writeToolContextFile(List<ToolContext> contexts, File file, boolean overwrite) throws IOException {
		List<ToolContext> toWrite = new ArrayList<ToolContext>();
		if (!overwrite) {
			List<ToolContext> read = readToolContextFile(file);
			toWrite.addAll(read);
		}
		toWrite.addAll(contexts);
		PrintWriter out = new PrintWriter(new FileWriter(file));
		for (ToolContext context : toWrite) {
			for (Token token : context.getTokens()) {
				out.write("TOKEN:\t");
				out.write(token.getString() + "\t");
				out.write(token.getLemma() + "\t");
				out.write(token.getPosTag() + "\t");
				out.write(token.isTool() + "\n");
				out.write("TOOL:\t");
				out.write(context.getToolPointer() + "\n");
				out.write("CONF:\t");
				out.write(context.getConf() + "\n");
			}
		}
		out.close();
	}

	public void readToolLists(File toolsFile, File noToolsFile) throws IOException {
		tools = new TreeMap<String, Set<Tool>>();
		noTools = new TreeSet<String>();
		// read AMs from File
		BufferedReader in = new BufferedReader(new FileReader(toolsFile));
		String line = in.readLine();
		while (line != null) {
			String split[] = line.split(" ");
			Set<Tool> set = tools.get(split[0]);
			if (set == null)
				set = new HashSet<Tool>();
			Tool tool = new Tool(split[0], split.length == 1);
			tool.setContext(Arrays.asList(split));
			set.add(tool);
			tools.put(split[0], set);
			line = in.readLine();
		}
		in.close();
		// read noAMs From File
		in = new BufferedReader(new FileReader(noToolsFile));
		line = in.readLine();
		while (line != null) {
			noTools.add(line.toLowerCase().trim());
			line = in.readLine();
		}
	}

	public void matchWithToolLists(List<CompetenceUnit> compUnits) throws IOException {

		// search for and flag as AMs or noAMs in competenceUnits
		for (CompetenceUnit cu : compUnits) {
			List<Token> tokens = cu.getTokenObjects();
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				String lemma = normalizeLemma(token.getLemma());
				if (noTools.contains(lemma)) {
					// flas as noAM
					token.setNoTool(true);
					continue;
				}
				if (tools.keySet().contains(lemma)) {

					for (Tool tool : tools.get(lemma)) {
						if (tool.isComplete()) {
							// token is single AM
							token.setTool(true);
							continue;
						}
						// token could be start of AM
						boolean matches = false;
						for (int c = 1; c < tool.getContext().size(); c++) {
							if (tokens.size() <= i + c) {
								matches = false;
								break;
							}
							matches = tool.getContext().get(c).equals(tokens.get(i + c).getLemma());
							if (!matches) {
								break;
							}
						}
						// Token is start of AM
						if (matches) {
							token.setIsStartOfTool(true);
							token.setRequired(tool.getContext().size() - 1);
						}
					}
				}
			}
		}
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
			content = content.replace("ãÿ", "ß");
			content = content.replace("ã¼", "ü");
			content = content.replace("Ã„", "Ä");
			content = content.replace("Ã–", "Ö");
			content = content.replace("ãœ", "ü");
			content = content.replace("Â¿", "-");
			content = content.replace("Â€", "€");
			classifyUnit.setContent(content);
			toReturn.add(classifyUnit);
		}
		return toReturn;
	}

	public Map<CompetenceUnit, Map<Integer, List<ToolContext>>> extractNewTools(File contextFile,
			List<CompetenceUnit> compUnits) throws IOException {
		// read contexts from file
		List<ToolContext> contexts = readToolContextFile(contextFile);
		// <cu, <Tokenindex, contexts>>
		Map<CompetenceUnit, Map<Integer, List<ToolContext>>> toReturn = new HashMap<CompetenceUnit, Map<Integer, List<ToolContext>>>();

		for (CompetenceUnit cu : compUnits) {
			List<Token> tokens = cu.getTokenObjects();

			for (ToolContext context : contexts) {
				// compare tokens with context
				for (int i = 0; i <= tokens.size() - context.getSize(); i++) {

					boolean match = false;
					int plus = 0;
					// context window
					for (int c = 0; c < context.getSize(); c++) {
						if ((i + c + plus) >= tokens.size()) {
							continue;
						}
						Token token = tokens.get(i + c + plus);
						Token contextToken = context.getTokenAt(c);
						match = token.isEqualsContextToken(contextToken);
						if (!match) {
							// move to next context windoew
							break;
						}
						// skip over required tokens
						if (c < context.getSize() - 1) {
							plus = plus + token.getRequired();
						}

					}
					// if context window matches context:
					if (match && (i + context.getToolPointer() + plus < tokens.size())) {
						Token toolToken = tokens.get(i + context.getToolPointer() + plus);
						int toolTokenIndex = i + context.getToolPointer() + plus;
						plus = plus + toolToken.getRequired();
						boolean isNew = !toolToken.isTool();
						if (isNew) {
							isNew = !toolToken.isStartOfTool();
						}
						boolean isNoTool = toolToken.isNoTool();
						if (isNew && !(isNoTool)) {
							// token is not in AMs or noAMs
							Map<Integer, List<ToolContext>> map = toReturn.get(cu);
							if (map == null)
								map = new HashMap<Integer, List<ToolContext>>();
							List<ToolContext> list = map.get(toolTokenIndex);
							if (list == null)
								list = new ArrayList<ToolContext>();
							list.add(context);
							map.put(toolTokenIndex, list);
							toReturn.put(cu, map);
						}
					}
				}
			}
		}
		return toReturn;
	}

	public boolean annotateDetectedAMs(Map<CompetenceUnit, Map<Integer, List<ToolContext>>> detected, File toolsFile,
			File noToolsFile, int currentIteration, int maxNumberOfIterations) throws IOException {
		System.out.println("annotate...");
		boolean lastWasTool = false;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String answer;
		Tool lastTool = null;
		String lastNoTool = null;
		for (CompetenceUnit cu : detected.keySet()) {
			System.out.println(cu);
			System.out.println();
			List<Integer> tokenIndices = new ArrayList<Integer>(detected.get(cu).keySet());
			int i = 0;
			while (i < tokenIndices.size()) {
				if (i < 0)
					continue;
				int toolTokenIndex = tokenIndices.get(i);
				Token toolToken = cu.getTokenObjects().get(toolTokenIndex);
				String potentialTool = normalizeLemma(toolToken.getLemma());
				System.out.println("--> " + potentialTool);
				answer = in.readLine().toLowerCase().trim();
				if (answer.equals("y")) {
					lastWasTool = true;
					System.out.println("is complete?");
					answer = in.readLine();
					if (answer.trim().toLowerCase().equals("y")) {
						Tool newTool = new Tool(potentialTool, true);
						System.out.println("added: " + potentialTool);
						Set<Tool> list = tools.get(potentialTool);
						if (list == null)
							list = new HashSet<Tool>();
						list.add(newTool);
						tools.put(potentialTool, list);
						lastTool = newTool;
						System.out.println("added new AM");
					}
					if (answer.trim().toLowerCase().equals("n")) {
						boolean answered = false;
						while (!answered) {
							System.out.println("enter number of required words");
							answer = in.readLine();
							try {
								int required = Integer.parseInt(answer);
								Tool newTool = new Tool(potentialTool, false);
								System.out.println("added: " + potentialTool);
								List<String> context = new ArrayList<String>();
								for (int r = 0; r <= required; r++) {
									context.add(cu.getTokenObjects().get(toolTokenIndex + r).getLemma());
								}
								newTool.setContext(context);
								Set<Tool> list = tools.get(potentialTool);
								if (list == null) {
									list = new HashSet<Tool>();
								}
								list.add(newTool);
								tools.put(potentialTool, list);
								lastTool = newTool;
								System.out.println("added new AM: ");
								answered = true;
							} catch (Exception e) {
								System.out.println("invalid answer. Try again...");
							}
						}
					}
					i++;
					continue;
				}
				if (answer.equals("n")) {
					lastWasTool = false;
					noTools.add(toolToken.getLemma());
					lastNoTool = toolToken.getLemma();
					System.out.println("saved as noAM");
					i++;
					continue;
				}
				if (answer.equals("stop")) {
					writeToolFiles(toolsFile, noToolsFile);
					return false;
				}
				// if(answer.equals("b")){
				// if(lastWasAM && lastAM != null){
				// Set<Arbeitsmittel> amSet = ams.get(lastAM.getWord());
				// amSet.remove(lastAM);
				// StringBuffer removed = new StringBuffer(lastAM.getWord());
				// if(lastAM.getContext() != null){
				// for (String s : lastAM.getContext()) {
				// removed.append(" "+s);
				// }
				// }
				// System.out.println("removed: " + removed+" from AMList");
				// i--;
				// i--;
				// continue;
				// }
				// if(!lastWasAM && lastNotAM != null){
				// noAMs.remove(lastNotAM);
				// System.out.println("removed " + lastNotAM+" from notAMList");
				// i--;
				// i--;
				// continue;
				// }
				// }
				else {
					System.out.println("invalid answer...");
					i--;
				}
			}
		}
		writeToolFiles(toolsFile, noToolsFile);
		if (currentIteration < maxNumberOfIterations) {
			return true;
		} else {
			return false;
		}
	}

	private void writeToolFiles(File toolsFile, File noToolsFile) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(noToolsFile));
		for (String string : noTools) {
			out.write(string + "\n");
		}
		out.close();
		out = new PrintWriter(new FileWriter(toolsFile));
		for (String string : tools.keySet()) {
			for (Tool tool : tools.get(string)) {
				StringBuffer sb = new StringBuffer();
				sb.append(string);
				if (!tool.isComplete()) {
					for (int i = 1; i < tool.getContext().size(); i++) {
						sb.append(" " + tool.getContext().get(i));
					}
				}
				out.write(sb.toString() + "\n");
			}
		}
		out.close();
	}

	public Map<Integer, List<String>> countTools(List<CompetenceUnit> comps, File outputfile) throws IOException {
		Map<Integer, List<String>> toReturn = new TreeMap<Integer, List<String>>();
		Map<String, Integer> countMap = new TreeMap<String, Integer>();
		for (CompetenceUnit cu : comps) {
			for (int t = 0; t < cu.getTokenObjects().size(); t++) {
				Token token = cu.getTokenObjects().get(t);
				String lemma = normalizeLemma(token.getLemma());
				if (token.isTool()) {
					int count = 0;
					if (countMap.keySet().contains(lemma)) {
						count = countMap.get(lemma);
					}
					count++;
					countMap.put(lemma, count);
				}
				if (token.isStartOfTool()) {
					int count = 0;
					StringBuffer sb = new StringBuffer();
					sb.append(lemma);
					for (int i = t + 1; i <= token.getRequired(); i++) {
						String contextLemma = normalizeLemma(cu.getTokenObjects().get(i).getLemma());

						sb.append(" " + contextLemma);
					}
					String am = sb.toString();
					if (countMap.keySet().contains(am)) {
						count = countMap.get(am);
					}
					count++;
					countMap.put(am, count);
				}
			}
		}
		for (String s : countMap.keySet()) {
			List<String> list = toReturn.get(countMap.get(s));
			if (list == null)
				list = new ArrayList<String>();
			list.add(s);
			toReturn.put(countMap.get(s), list);
		}
		if (outputfile != null) {
			PrintWriter out = new PrintWriter(new FileWriter(outputfile));
			List<Integer> reordered = new ArrayList<Integer>(toReturn.keySet());
			for (int i = reordered.size() - 1; i >= 0; i--) {
				int count = reordered.get(i);
				for (String am : toReturn.get(count)) {
					out.write(count + "\t" + am + "\n");
				}
			}
			out.close();
		}
		return toReturn;
	}

	private String normalizeLemma(String lemma) {
		if (lemma.startsWith("-")) {
			lemma = lemma.substring(1);
		}
		if (lemma.endsWith("/")) {
			lemma = lemma.substring(0, lemma.length() - 1);
		}
		return lemma;
	}

	public Set<String> matchToolsWithSectors(List<CompetenceUnit> compUnits, File sectorsFile) throws IOException {
		Set<String> matches = new TreeSet<String>();
		readSectorsList(sectorsFile);
		for (String s : sectorsFileTools.keySet()) {
			System.out.println(s);
		}
		for (String s : tools.keySet()) {
			for (Tool tool : tools.get(s)) {
				String expression;
				if (tool.getContext() != null) {
					StringBuffer sb = new StringBuffer();
					for (String con : tool.getContext()) {
						sb.append(" " + con);
					}
					expression = sb.toString().substring(1);
				} else {
					expression = tool.getWord();
				}
				if (sectorsFileTools.keySet().contains(expression)) {
					matches.add(expression);
				}
			}
		}
		return matches;
	}

	private void readSectorsList(File sectorsFile) throws IOException {
		is2.tools.Tool lemmatizer = new Lemmatizer("models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		IETokenizer tokenizer = new IETokenizer();

		sectorsFileTools = new HashMap<String, Set<String>>();
		sectors = new HashMap<String, String>();
		Workbook w;
		try {
			WorkbookSettings ws = new WorkbookSettings();
			ws.setEncoding("Cp1252");
			w = Workbook.getWorkbook(sectorsFile, ws);
			String sector = null;
			String sectorField = null;
			Sheet sheet = w.getSheet(0);
			for (int i = 1; i < sheet.getRows(); i++) {
				// Zeile i
				Cell sectorCell = sheet.getCell(0, i);
				if (sectorCell.getContents().equals("")) {
					String toolContent = sheet.getCell(1, i).getContents();

					// lemmatisieren
					String[] tools = toolContent.split(",");

					for (String t : tools) {
						t = t.trim();
						SentenceData09 sd = new SentenceData09();
						sd.init(tokenizer.tokenizeSentence(t));
						lemmatizer.apply(sd);
						StringBuffer sb = new StringBuffer();
						String[] lemmas = sd.plemmas;
						for (String lemma : lemmas) {
							sb.append(" " + lemma);
						}
						Set<String> sectors = sectorsFileTools.get(sb.toString().substring(1));
						if (sectors == null) {
							sectors = new HashSet<String>();
						}
						sectors.add(sector);
						sectorsFileTools.put(sb.toString().substring(1), sectors);
					}
					continue;
				} else {
					sector = sectorCell.getContents();
					Cell nextCell = sheet.getCell(0, i + 1);
					if (nextCell.getContents().equals("")) {
						sectors.put(sector, sectorField);
					} else {
						sectorField = sector;
					}
				}

			}
		} catch (BiffException e) {
			e.printStackTrace();
		}
	}
}
