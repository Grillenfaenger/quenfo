package de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.information_extraction.CompetenceDetector;
import de.uni_koeln.spinfo.information_extraction.data.DependencyTree;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.Token;
import de.uni_koeln.spinfo.information_extraction.data.WordNode;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.Tool;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.ToolContext;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import is2.data.SentenceData09;
import is2.io.CONLLWriter09;
import is2.lemmatizer.Lemmatizer;
import is2.parser.Parser;
import is2.tag.Tagger;


public class IEJobs_Comps {

	private Map<String, Set<Tool>> comps;
	private Set<String> noComps;
	private Map<String, Set<Tool>> tools;


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
	 * erzeugt für jeden Satz ein CompetenceUnit-Object. Bestehend aus sentence,
	 * JobAdID(=parentID der ClassifyUnit) und ClassifyUnitID (= UUID der
	 * ClassifyUnit)
	 * 
	 *
	 * @param cus
	 * @return
	 */
	public List<ExtractionUnit> initializeCompetenceUnits(List<ClassifyUnit> cus, boolean splitInSentence) {
		List<ExtractionUnit> toReturn = new ArrayList<ExtractionUnit>();
		IETokenizer tokenizer = new IETokenizer();
		for (ClassifyUnit cu : cus) {
			List<String> sentences = tokenizer.splitIntoSentences(cu.getContent(), splitInSentence);
			for (String sentence : sentences) {
				if (sentence.length() > 1) {
					ExtractionUnit compUnit = new ExtractionUnit();
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
	
	public void setSentenceData(List<ExtractionUnit> compUnits, String sdOutputFileName) throws IOException{
		setSentenceData(compUnits, sdOutputFileName, false);
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
	public void setSentenceData(List<ExtractionUnit> compUnits, String sdOutputFileName, boolean onlyLemmata) throws IOException {
		IETokenizer tokenizer = new IETokenizer();
		is2.tools.Tool lemmatizer = new Lemmatizer(
				"information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		is2.mtag.Tagger morphTagger  = null;
		is2.tools.Tool  tagger = null;
		is2.tools.Tool  parser = null;
		if(!onlyLemmata){
			morphTagger = new is2.mtag.Tagger(
					"information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/morphology-ger-3.6.model");
			tagger = new Tagger("information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");
			parser = new Parser(
					"information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/parser-ger-3.6.model");
		}
		
		CONLLWriter09 writer = null;
		if (sdOutputFileName != null) {
			File file = new File(sdOutputFileName + ".csv");
			if (!file.exists()) {
				file.createNewFile();
			}
			writer = new is2.io.CONLLWriter09(sdOutputFileName + ".csv");
		}
		for (ExtractionUnit compUnit : compUnits) {
			SentenceData09 sd = new SentenceData09();
			sd.init(tokenizer.tokenizeSentence("<root> " + compUnit.getSentence()));
			lemmatizer.apply(sd);
			if(!onlyLemmata){
				morphTagger.apply(sd);
				tagger.apply(sd);
				sd = parser.apply(sd);
			}		
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
	
	
	public void readCompLists(File compsFile, File noCompsFile, File toolsFile) throws IOException{
		comps = new TreeMap<String, Set<Tool>>();
		tools = new TreeMap<String, Set<Tool>>();
		noComps = new TreeSet<String>();
		// read AMs from File
		BufferedReader in = new BufferedReader(new FileReader(compsFile));
		String line = in.readLine();
		while (line != null) {
			String split[] = line.split(" ");
			Set<Tool> toolSet = comps.get(split[0]);
			if (toolSet == null)
				toolSet = new HashSet<Tool>();
			Tool tool = new Tool(split[0], split.length == 1);
			if (!tool.isComplete()) {
				tool.setContext(Arrays.asList(split));
			}
			toolSet.add(tool);
			comps.put(split[0], toolSet);
			line = in.readLine();
		}
		in.close();
		// read Tools from File
		in = new BufferedReader(new FileReader(toolsFile));
		line = in.readLine();
		while (line != null) {
			String split[] = line.split(" ");
			Set<Tool> toolSet = tools.get(split[0]);
			if (toolSet == null)
				toolSet = new HashSet<Tool>();
			Tool tool = new Tool(split[0], split.length == 1);
			if (!tool.isComplete()) {
				tool.setContext(Arrays.asList(split));
			}
			toolSet.add(tool);
			tools.put(split[0], toolSet);
			line = in.readLine();
		}
		in.close();
		// read noAMs From File
		in = new BufferedReader(new FileReader(noCompsFile));
		line = in.readLine();
		while (line != null) {
			noComps.add(line.toLowerCase().trim());
			line = in.readLine();
		}
	}
	
	public Map<ExtractionUnit,List<Tool>> matchWithCompLists(List<ExtractionUnit> compUnits) throws IOException {
		Map<ExtractionUnit, List<Tool>> toReturn = new HashMap<ExtractionUnit,List<Tool>>();
		for (ExtractionUnit currentCU : compUnits) {
			List<Token> tokens = currentCU.getTokenObjects();
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				String lemma = normalizeLemma(token.getLemma());
				
				if (comps.keySet().contains(lemma)) {

					// potential tool
					for (Tool tool : comps.get(lemma)) {
						if (tool.isComplete()) {
							// token is single AM
							token.setTool(true);
							List<Tool> toolsForUnit = toReturn.get(currentCU);
							if(toolsForUnit == null) toolsForUnit = new ArrayList<Tool>();
							toolsForUnit.add(tool);
							toReturn.put(currentCU, toolsForUnit);
							continue;
						}
						// token could be start of AM
						boolean matches = false;
						// check if token has required context
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
							List<Tool> toolsForUnit = toReturn.get(currentCU);
							if(toolsForUnit == null) toolsForUnit = new ArrayList<Tool>();
							toolsForUnit.add(tool);
							toReturn.put(currentCU, toolsForUnit);
						}
					}
				}
				if (noComps.contains(lemma)) {
					// flag as noAM
					token.setNoTool(true);
				}

			}
		}
		return toReturn;
	}
	
	
	
	public Map<ExtractionUnit, Map<Integer, List<ToolContext>>> extractNewComps(File contextFile,
			List<ExtractionUnit> compUnits) throws IOException {
		// read contexts from file
		List<ToolContext> contexts = readCompContextFile(contextFile);
		// <cu, <Tokenindex, contexts>>
		Map<ExtractionUnit, Map<Integer, List<ToolContext>>> toReturn = new HashMap<ExtractionUnit, Map<Integer, List<ToolContext>>>();

		for (ExtractionUnit cu : compUnits) {
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
							// move to next context window
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
		for (ExtractionUnit exUnit : toReturn.keySet()) {
			System.out.println(exUnit.getSentence());
			for (Integer i : toReturn.get(exUnit).keySet()) {
				Token token = exUnit.getTokenObjects().get(i);
				System.out.println("potential comp: " + token);
			}
		}
		return toReturn;
	}

private List<ToolContext> readCompContextFile(File contextFile) throws IOException {
	List<ToolContext> toReturn = new ArrayList<ToolContext>();
	BufferedReader in = new BufferedReader(new FileReader(contextFile));
	String line = in.readLine();
	ToolContext context = new ToolContext();
	while (line != null) {
		if (line.startsWith("//")) {
			line = in.readLine();
			continue;
		}
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
		if (line.startsWith("COMP:")) {
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



public boolean annotatePotentialComps(Map<ExtractionUnit, Map<Integer, List<ToolContext>>> detected, File compsFile,
		File noCompsFile, File toolsFile) throws IOException {

	System.out.println("\n annotate potential Tools...");

	Tool lastSingleComp = null;
	Tool lastNestedComp = null;
	String lastNoComp = null;
	Tool lastTool = null;

	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	String answer;
	List<ExtractionUnit> compUnits = new ArrayList<ExtractionUnit>(detected.keySet());

	boolean continueLastIteration = false;
	int compUnitIndex = 0;
	int listIndex;
	while (compUnitIndex < compUnits.size()) {
		if (compUnitIndex < 0)
			compUnitIndex = 0;
		ExtractionUnit currentCompUnit = compUnits.get(compUnitIndex);
		System.out.println("\n"+"\n" + currentCompUnit.getSentence() + "\n");
		// all potential toolTokenIndices
		List<Integer> toolTokenIndices = new ArrayList<Integer>(detected.get(currentCompUnit).keySet());
		listIndex = 0;
		if (continueLastIteration) {
			listIndex = toolTokenIndices.size() - 1;
		}
		while (listIndex < toolTokenIndices.size()) {

			if (listIndex < 0) {
				// back to previous compUnit :
				compUnitIndex--;
				compUnitIndex--;
				break;
			}
			int toolTokenIndex = toolTokenIndices.get(listIndex);
			Token currentToolToken = currentCompUnit.getTokenObjects().get(toolTokenIndex);
			String currentToolLemma = normalizeLemma(currentToolToken.getLemma());

			System.out.println(" --> " + currentToolLemma);
			System.out.println("\n is single Tool? (press 'y' or 'n')");

			answer = in.readLine().trim();
			
			if(answer.equals("t")){
				//it's a tool!
				//add to toolsList
				Tool newSingleTool = new Tool(currentToolLemma, true);
				Set<Tool> toolsForLemma = tools.get(currentToolLemma);
				if (toolsForLemma == null) {
					toolsForLemma = new HashSet<Tool>();
				}
				toolsForLemma.add(newSingleTool);
				tools.put(currentToolLemma, toolsForLemma);
				//addToNoCompsList
				noComps.add(currentToolLemma);
				//set lastTool to current Tool
				lastTool = newSingleTool;
				// set lastNoTool to currentToolLemma
				lastNoComp = currentToolLemma;
				// set lastSingleTool to null
				lastSingleComp = null;
				// set LastNestedTool to null;
				lastNestedComp = null;
				listIndex++;
				continue;
				
			}
			if (answer.equals("b")) {
				if (lastNoComp == null && lastSingleComp == null & lastNestedComp == null) {
					System.out.println("Sorry, you can't go back. ");
					continue;
				}
				// remove last noTool
				if (lastNoComp != null) {
					noComps.remove(lastNoComp);
					lastNoComp = null;
				}
				// remove last singleTool 
				if (lastSingleComp != null) {
					Set<Tool> toolSet = comps.get(lastSingleComp.getWord());
					toolSet.remove(lastSingleComp);
					if (toolSet.isEmpty()) {
						comps.remove(lastSingleComp.getWord());
					} else {
						comps.put(lastSingleComp.getWord(), toolSet);
					}
					lastSingleComp = null;
				}
				// remove last nestedTool
				if (lastNestedComp != null) {
					Set<Tool> toolSet = comps.get(lastNestedComp.getWord());
					toolSet.remove(lastNestedComp);
					if (toolSet.isEmpty()) {
						comps.remove(lastNestedComp.getWord());
					} else {
						comps.put(lastNestedComp.getWord(), toolSet);
					}
					lastNestedComp = null;
				}
				//remove last Tool
				if(lastTool != null){
					Set<Tool> toolSet = tools.get(lastTool.getWord());
					toolSet.remove(lastTool);
					if (toolSet.isEmpty()) {
						tools.remove(lastTool.getWord());
					} else {
						tools.put(lastTool.getWord(), toolSet);
					}
					lastTool = null;
				}
				// go back to removedTool
				listIndex--;
				continue;
			}
			if (answer.equals("stop")) {
				// writeToolFiles
				writeCompFiles(compsFile, noCompsFile, toolsFile);
				in.close();
				// interrupt iteration
				return false;
			}
			if (answer.equals("y")) {
				// add new single Tool to tools
				Tool newSingleTool = new Tool(currentToolLemma, true);
				Set<Tool> toolsForLemma = comps.get(currentToolLemma);
				if (toolsForLemma == null) {
					toolsForLemma = new HashSet<Tool>();
				}
				toolsForLemma.add(newSingleTool);
				comps.put(currentToolLemma, toolsForLemma);
				// set lastSingleTool to newTool
				lastSingleComp = newSingleTool;
				// setLastNestedTool to null
				lastNestedComp = null;
				// setLastNoTool to null
				lastNoComp = null;
				lastTool = null;
				System.out.println("added: " + newSingleTool);

				// next step: is nestedTool?
			} else if (answer.equals("n")) {
				// add currentToolLemma to noTools
				noComps.add(currentToolLemma);
				// set lastNoTool to currentToolLemma
				lastNoComp = currentToolLemma;
				// set lastSingleTool to null
				lastSingleComp = null;
				// set LastNestedTool to null;
				lastNestedComp = null;
				lastTool = null;

				// nextStep: is nestedTool?
			} else {
				System.out.println("\n invalid answer! Please try again...\n");
				continue;
			}
			boolean answered = false;
			while (!answered) {
				System.out.println("\n --> " + currentToolLemma);
				System.out.println("\n is nested Tool? (press 'y' or 'n')");

				answer = in.readLine().trim();

				if (answer.equals("y")) {
					Tool newNestedTool = null;
					// create new nestedTool to tools

					while (true) {
						System.out.println("enter number of words before '" + currentToolLemma + "'");
						answer = in.readLine().trim();
						try {
							int before = Integer.parseInt(answer);
							String startOfTool = normalizeLemma(
									currentCompUnit.getTokenObjects().get(toolTokenIndex - before).getLemma());
							newNestedTool = new Tool(normalizeLemma(startOfTool), false);
							List<String> context = new ArrayList<String>();
							for (int b = before; b >= 0; b--) {
								context.add(currentCompUnit.getTokenObjects().get(toolTokenIndex - b).getLemma());
							}
							newNestedTool.setContext(context);
							break;
						} catch (Exception e) {
							System.out.println("invalid answer! Try again...");
						}
					}
					while (true) {
						System.out.println("enter number of words after '" + currentToolLemma + "'");
						answer = in.readLine().trim();
						try {
							int after = Integer.parseInt(answer);
							for (int a = 1; a <= after; a++) {
								newNestedTool.addToContext(
										currentCompUnit.getTokenObjects().get(toolTokenIndex + a).getLemma());
							}
							break;
						} catch (Exception e) {
							System.out.println("invalid answer! Try again...");
						}
					}
					// set lastNestedTool to newNestedTool
					lastNestedComp = newNestedTool;

					// add nestedTool to tools
					Set<Tool> toolsForLemma = comps.get(newNestedTool.getWord());
					if (toolsForLemma == null) {
						toolsForLemma = new HashSet<Tool>();
					}
					toolsForLemma.add(newNestedTool);
					comps.put(newNestedTool.getWord(), toolsForLemma);
					System.out.println("added: " + newNestedTool);

					// continue
					answered = true;
					listIndex++;
					continue;
				}
				if (answer.equals("n")) {
					// set lastNestedTool to null
					lastNestedComp = null;
					// continue with next ToolToken
					answered = true;
					listIndex++;
					continue;
				} else {
					System.out.println("\n invalid answer! Please try again...\n");
				}
			}
		}
		compUnitIndex++;
		if (compUnitIndex == compUnits.size()) {
			System.out.println("\n End of list. Press 'b' to edit last tool or 'enter' to save annotated tools");
			answer = in.readLine();
			if (answer.equals("b")) {
				compUnitIndex--;
				continueLastIteration = true;
				// remove last noComp
				if (lastNoComp != null) {
					noComps.remove(lastNoComp);
					lastNoComp = null;
				}
				// remove last singleTool
				if (lastSingleComp != null) {
					Set<Tool> toolSet = comps.get(lastSingleComp.getWord());
					toolSet.remove(lastSingleComp);
					if (toolSet.isEmpty()) {
						comps.remove(lastSingleComp.getWord());
					} else {
						comps.put(lastSingleComp.getWord(), toolSet);
					}
					lastSingleComp = null;
				}
				// remove last nestedTool
				if (lastNestedComp != null) {
					Set<Tool> toolSet = comps.get(lastNestedComp.getWord());
					toolSet.remove(lastNestedComp);
					if (toolSet.isEmpty()) {
						comps.remove(lastNestedComp.getWord());
					} else {
						comps.put(lastNestedComp.getWord(), toolSet);
					}
					lastNestedComp = null;
				}
				//remove lastTool
				if(lastTool != null){
					Set<Tool> toolSet = tools.get(lastTool.getWord());
					toolSet.remove(lastTool);
					if (toolSet.isEmpty()) {
						tools.remove(lastTool.getWord());
					} else {
						tools.put(lastTool.getWord(), toolSet);
					}
					lastTool = null;
				}
			}
		}
	}

	in.close();
	writeCompFiles(compsFile, noCompsFile, toolsFile);
	return true;
}

private void writeCompFiles(File compsFile, File noCompsFile, File toolsFile) throws IOException {
	PrintWriter out = new PrintWriter(new FileWriter(noCompsFile));
	for (String string : noComps) {
		out.write(string + "\n");
	}
	out.close();
	
	out = new PrintWriter(new FileWriter(compsFile));
	for (String string : comps.keySet()) {
		for (Tool tool : comps.get(string)) {
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

//	/**
//	 * schreibt alle gefilterten ClassifyUnits in eine Textdatei
//	 * 
//	 * @param toWrite
//	 * @param outputFile
//	 * @throws IOException
//	 */
//	public void writeFilteredClassifyUnits(List<ClassifyUnit> toWrite, File outputFile) throws IOException {
//		TrainingDataGenerator tdg = new TrainingDataGenerator(null);
//		tdg.writeTrainingDataFile(outputFile, toWrite);
//	}

//	/**
//	 * Methode zum Einlesen von (gefilterten) ClassifyUnits
//	 * 
//	 * @param inputFile
//	 * @return toReturn Liste von ClassifyUnits
//	 * @throws IOException
//	 */
//	public List<ClassifyUnit> readFilteredClassifyUnitsFromFile(File inputFile) throws IOException {
//		TrainingDataGenerator tdg = new TrainingDataGenerator(inputFile);
//		List<ClassifyUnit> toReturn = tdg.getTrainingData();
//		return toReturn;
//	}

//	/**
//	 * (nur zum Testen. Wird im Workflow nicht benötigt -->
//	 * initializeCompetenceUnits) Ordnet jeder ClassifyUnit eine Liste ihrer
//	 * Sätze zu (potentielle CompetenceUnits)
//	 * 
//	 * 
//	 * @param classifyUnits
//	 * @return toReturn Map von ClassifyUnits (key) und Liste ihrer Sentences
//	 *         (value)
//	 */
//	public Map<ClassifyUnit, List<String>> getSentences(List<ClassifyUnit> classifyUnits, boolean splitInSentence) {
//		Map<ClassifyUnit, List<String>> toReturn = new HashMap<ClassifyUnit, List<String>>();
//		IETokenizer tokenizer = new IETokenizer();
//		for (ClassifyUnit compCU : classifyUnits) {
//			List<String> currentSentences = tokenizer.splitIntoSentences(compCU.getContent(), splitInSentence);
//			toReturn.put(compCU, currentSentences);
//		}
//		return toReturn;
//	}

//	/**
//	 * Methode zum Herausschreiben von ClassifyUnits und ihren Sentences
//	 * 
//	 * @param toWrite
//	 * @param outputFile
//	 * @throws IOException
//	 */
//	public void writeSentencesFile(Map<ClassifyUnit, List<String>> sentences, File outputFile) throws IOException {
//		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
//		for (ClassifyUnit cu : sentences.keySet()) {
//			out.print(cu.getID() + "\t");
//			if (cu instanceof JASCClassifyUnit) {
//				out.print(((JASCClassifyUnit) cu).getParentID() + "\t");
//			}
//			out.print(((ZoneClassifyUnit) cu).getActualClassID() + "\n");
//			for (String sentence : sentences.get(cu)) {
//				out.println("SENTENCE: " + sentence + "\n");
//			}
//		}
//		out.flush();
//		out.close();
//	}

	

//	private boolean containsCompetences(String sentence) {
//		if (sentence.endsWith(":")) {
//			return false;
//		}
//		return true;
//	}
	
	
	

	public void buildDependencyTrees(List<ExtractionUnit> cus) {
		for (ExtractionUnit cu : cus) {
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

	public void buildDependencyTree(ExtractionUnit cu) {
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

	public void setCompetences(List<ExtractionUnit> compUnits) throws IOException {
		CompetenceDetector detector = new CompetenceDetector();
		detector.setCompetences(compUnits);
	}

//	public List<ExtractionUnit> filterEmptyCompetenceUnits(List<ExtractionUnit> toFilter) {
//		List<ExtractionUnit> toReturn = new ArrayList<>();
//		for (ExtractionUnit cu : toFilter) {
//			if (cu.getCompetences() != null) {
//				toReturn.add(cu);
//			}
//		}
//		return toReturn;
//	}

//	public void writeCompetenceData(List<ExtractionUnit> toWrite, File outputFile) throws IOException {
//		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
//		for (ExtractionUnit compUnit : toWrite) {
//			out.write("ID: " + compUnit.getClassifyUnitID() + "\n");
//			out.write("JobAdID: " + compUnit.getJobAdID() + "-" + compUnit.getSecondJobAdID() + "\n");
//
//			out.write("SENTENCE: " + compUnit.getSentence() + "\n");
//			if (compUnit.getCompetences() != null) {
//				for (Competence comp : compUnit.getCompetences()) {
//					out.write("COMP: " + comp.getCompetence() + "\t" + comp.getQuality() + "\t" + comp.getImportance()
//							+ "\n");
//				}
//			}
//			out.write("\n");
//		}
//		out.close();
//	}

//	public List<ExtractionUnit> readCompetenceUnitsFromFile(File toRead) throws IOException {
//		List<ExtractionUnit> toReturn = new ArrayList<ExtractionUnit>();
//		BufferedReader in = new BufferedReader(new FileReader(toRead));
//		String line = in.readLine();
//		ExtractionUnit compUnit = null;
//		while (line != null) {
//			if (line.startsWith("ID:")) {
//				if (compUnit != null) {
//					toReturn.add(compUnit);
//				}
//				compUnit = new ExtractionUnit();
//				compUnit.setClassifyUnitID(UUID.fromString(line.split(":")[1].trim()));
//				line = in.readLine();
//				continue;
//			}
//			if (line.startsWith("JobAdID:")) {
//				int jobAdID = Integer.parseInt(line.split(":")[1].split("-")[0].trim());
//				int secondJobAdID = Integer.parseInt(line.split(":")[1].split("-")[1].trim());
//				compUnit.setJobAdID(jobAdID);
//				compUnit.setSecondJobAdID(secondJobAdID);
//				line = in.readLine();
//				continue;
//			}
//			if (line.startsWith("COMP:")) {
//				String[] split = line.split("COMP:")[1].split("\t");
//				Competence comp = new Competence(split[0], compUnit.getJobAdID(), compUnit.getSecondJobAdID());
//				comp.setQuality(split[1]);
//				comp.setImportance(split[2]);
//
//				compUnit.setCompetence(comp);
//				line = in.readLine();
//				continue;
//			}
//			if (line.startsWith("SENTENCE: ")) {
//				compUnit.setSentence(line.split("SENTENCE:")[1].trim());
//				line = in.readLine();
//				continue;
//			}
//			line = in.readLine();
//		}
//		in.close();
//		return toReturn;
//	}

	

	
	

	

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
	private String normalizeLemma(String lemma) {
		if (lemma.startsWith("-")) {
			lemma = lemma.substring(1);
		}
		if (lemma.endsWith("/")) {
			lemma = lemma.substring(0, lemma.length() - 1);
		}
		return lemma;
	}
}

	