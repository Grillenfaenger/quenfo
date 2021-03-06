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
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.DependencyTree;
import de.uni_koeln.spinfo.information_extraction.data.Token;
import de.uni_koeln.spinfo.information_extraction.data.WordNode;
import de.uni_koeln.spinfo.information_extraction.data.competenceExtraction.Competence;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.Tool;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.ToolContext;
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

public class IEJobs_Tools {

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

	private List<ToolContext> readToolContextFile(File file) throws IOException {
		List<ToolContext> toReturn = new ArrayList<ToolContext>();
		BufferedReader in = new BufferedReader(new FileReader(file));
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
		in = new BufferedReader(new FileReader(noToolsFile));
		line = in.readLine();
		while (line != null) {
			noTools.add(line.toLowerCase().trim());
			line = in.readLine();
		}
	}

	public Map<ExtractionUnit,List<Tool>> matchWithToolLists(List<ExtractionUnit> compUnits) throws IOException {
		Map<ExtractionUnit, List<Tool>> toReturn = new HashMap<ExtractionUnit,List<Tool>>();
		for (ExtractionUnit currentCU : compUnits) {
			List<Token> tokens = currentCU.getTokenObjects();
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				String lemma = normalizeLemma(token.getLemma());
				
				if (tools.keySet().contains(lemma)) {

					// potential tool
					for (Tool tool : tools.get(lemma)) {
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
				if (noTools.contains(lemma)) {
					// flag as noAM
					token.setNoTool(true);
				}

			}
		}
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

	public Map<ExtractionUnit, Map<Integer, List<ToolContext>>> extractNewTools(File contextFile,
			List<ExtractionUnit> compUnits) throws IOException {
		// read contexts from file
		List<ToolContext> contexts = readToolContextFile(contextFile);
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
							// move to next context windowe
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

	/**
	 * @param detected
	 *            <CompetenceUnit with potential Tools, < <tokenIndex of
	 *            potential Tool, list of matching contexts>>
	 * @param toolsFile
	 * @param noToolsFile
	 * @param currentIteration
	 * @param maxNumberOfIterations
	 * @return true, if this iteration was not the last iteration. false, if
	 *         this was the last iteration
	 * @throws IOException
	 */
	public boolean annotatePotentialTools(Map<ExtractionUnit, Map<Integer, List<ToolContext>>> detected, File toolsFile,
			File noToolsFile) throws IOException {

		System.out.println("\n annotate potential Tools...");

		Tool lastSingleTool = null;
		Tool lastNestedTool = null;
		String lastNoTool = null;

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

				if (answer.equals("b")) {
					if (lastNoTool == null && lastSingleTool == null & lastNestedTool == null) {
						System.out.println("Sorry, you can't go back. ");
						continue;
					}
					// remove last noTool
					if (lastNoTool != null) {
						noTools.remove(lastNoTool);
						lastNoTool = null;
					}
					// remove last singleTool 
					if (lastSingleTool != null) {
						Set<Tool> toolSet = tools.get(lastSingleTool.getWord());
						toolSet.remove(lastSingleTool);
						if (toolSet.isEmpty()) {
							tools.remove(lastSingleTool.getWord());
						} else {
							tools.put(lastSingleTool.getWord(), toolSet);
						}
						lastSingleTool = null;
					}
					// remove last nestedTool
					if (lastNestedTool != null) {
						Set<Tool> toolSet = tools.get(lastNestedTool.getWord());
						toolSet.remove(lastNestedTool);
						if (toolSet.isEmpty()) {
							tools.remove(lastNestedTool.getWord());
						} else {
							tools.put(lastNestedTool.getWord(), toolSet);
						}
						lastNestedTool = null;
					}
					// go back to removedTool
					listIndex--;
					continue;
				}
				if (answer.equals("stop")) {
					// writeToolFiles
					writeToolFiles(toolsFile, noToolsFile);
					in.close();
					// interrupt iteration
					return false;
				}
				if (answer.equals("y")) {
					// add new single Tool to tools
					Tool newSingleTool = new Tool(currentToolLemma, true);
					Set<Tool> toolsForLemma = tools.get(currentToolLemma);
					if (toolsForLemma == null) {
						toolsForLemma = new HashSet<Tool>();
					}
					toolsForLemma.add(newSingleTool);
					tools.put(currentToolLemma, toolsForLemma);
					// set lastSingleTool to newTool
					lastSingleTool = newSingleTool;
					// setLastNestedTool to null
					lastNestedTool = null;
					// setLastNoTool to null
					lastNoTool = null;
					System.out.println("added: " + newSingleTool);

					// next step: is nestedTool?
				} else if (answer.equals("n")) {
					// add currentToolLemma to noTools
					noTools.add(currentToolLemma);
					// set lastNoTool to currentToolLemma
					lastNoTool = currentToolLemma;
					// set lastSingleTool to null
					lastSingleTool = null;
					// set LastNestedTool to null;
					lastNestedTool = null;

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
						lastNestedTool = newNestedTool;

						// add nestedTool to tools
						Set<Tool> toolsForLemma = tools.get(newNestedTool.getWord());
						if (toolsForLemma == null) {
							toolsForLemma = new HashSet<Tool>();
						}
						toolsForLemma.add(newNestedTool);
						tools.put(newNestedTool.getWord(), toolsForLemma);
						System.out.println("added: " + newNestedTool);

						// continue
						answered = true;
						listIndex++;
						continue;
					}
					if (answer.equals("n")) {
						// set lastNestedTool to null
						lastNestedTool = null;
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
					// remove last noTool
					if (lastNoTool != null) {
						noTools.remove(lastNoTool);
						lastNoTool = null;
					}
					// remove last singleTool
					if (lastSingleTool != null) {
						Set<Tool> toolSet = tools.get(lastSingleTool.getWord());
						toolSet.remove(lastSingleTool);
						if (toolSet.isEmpty()) {
							tools.remove(lastSingleTool.getWord());
						} else {
							tools.put(lastSingleTool.getWord(), toolSet);
						}
						lastSingleTool = null;
					}
					// remove last nestedTool
					if (lastNestedTool != null) {
						Set<Tool> toolSet = tools.get(lastNestedTool.getWord());
						toolSet.remove(lastNestedTool);
						if (toolSet.isEmpty()) {
							tools.remove(lastNestedTool.getWord());
						} else {
							tools.put(lastNestedTool.getWord(), toolSet);
						}
						lastNestedTool = null;
					}
				}
			}
		}

		in.close();
		writeToolFiles(toolsFile, noToolsFile);
		return true;
	}

	

	public Map<ExtractionUnit, List<Tool>> getToolsByCU(List<ExtractionUnit> comps) {
		Map<ExtractionUnit, List<Tool>> toReturn = new HashMap<ExtractionUnit, List<Tool>>();
		for (ExtractionUnit cu : comps) {
			List<Token> tokens = cu.getTokenObjects();
			for (int t = 0; t < tokens.size(); t++) {
				Token token = tokens.get(t);
				if (token.isStartOfTool()) {
					Tool tool = new Tool(normalizeLemma(token.getLemma()), false);
					int r = token.getRequired();
					List<String> context = new ArrayList<String>();
					for (int i = 0; i <= r; i++) {
						context.add(normalizeLemma(tokens.get(t + i).getLemma()));
					}
					tool.setContext(context);
					List<Tool> tools = toReturn.get(cu);
					if (tools == null) {
						tools = new ArrayList<Tool>();
					}
					tools.add(tool);
					toReturn.put(cu, tools);
					continue;
				}
				if (token.isTool()) {
					Tool tool = new Tool(normalizeLemma(token.getLemma()), true);
					List<Tool> tools = toReturn.get(cu);
					if (tools == null) {
						tools = new ArrayList<Tool>();
					}
					tools.add(tool);
					toReturn.put(cu, tools);
				}
			}
		}
		return toReturn;
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

	public Map<Integer, List<String>> countTools(List<ExtractionUnit> comps, File outputfile) throws IOException {
		Map<Integer, List<String>> toReturn = new TreeMap<Integer, List<String>>();
		Map<String, Integer> countMap = new TreeMap<String, Integer>();
		for (ExtractionUnit cu : comps) {
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

	public Set<String> matchToolsWithSectors(File sectorsFile) throws IOException {
		Set<String> matches = new TreeSet<String>();
		readSectorsList(sectorsFile);
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
		is2.tools.Tool lemmatizer = new Lemmatizer(
				"information_extraction/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
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
