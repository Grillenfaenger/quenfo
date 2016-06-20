//package de.uni_koeln.spinfo.information_extraction.applications.old;
//
//import java.io.BufferedOutputStream;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
//import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
//import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
//import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
//import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
//import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;
//import is2.data.SentenceData09;
//import is2.lemmatizer.Lemmatizer;
//import is2.parser.Parser;
//import is2.tag.Tagger;
//import is2.tools.Tool;
//import opennlp.tools.formats.ad.ADSentenceStream.Sentence;
//
//public class MatchWorkMatWithClassifyUnits {
//
//	private static File classifyUnitsFile = new File("classification/data/newTrainingData2016.csv");
//	private static List<String> arbeitsmittel = new ArrayList<String>();
//	private static IETokenizer tokenizer = new IETokenizer();
//	private static Tool lemmatizer = new Lemmatizer(
//			"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
//	private static  File outputFile = new File("information_extraction/data/AM_classifyUnits.txt");
//	private static is2.mtag.Tagger morphTagger = new is2.mtag.Tagger(
//			"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/morphology-ger-3.6.model");
//	private static Tool tagger = new Tagger(
//			"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");
//
//	public static void main(String[] args) throws IOException {
//		readArbeitsmittel();
//		List<ClassifyUnit> toMatch = getClassifyUnits();
//		Map<String,List<ClassifyUnit>> matches = match(toMatch);
//		writeFile(matches);
////		for (String ma : matches.keySet()) {
////			System.out.println("______________________"+ma+"________________________________");
////			System.out.println();
////			for (ClassifyUnit cu : matches.get(ma)) {
////				System.out.println(cu.getContent());
////				System.out.println();
////			}
////		}
//	}
//
//	private static void writeFile(Map<String, List<ClassifyUnit>> matches) throws IOException {
//		if(!outputFile.exists()){
//			outputFile.createNewFile();
//		}
//		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
//		for (String am : matches.keySet()) {
//			for (ClassifyUnit cu : matches.get(am)) {
//				out.write("______________"+am+"_______________\n");
//				out.write(cu.getContent());
//				
//				SentenceData09 sd = new SentenceData09();
//				sd.init(tokenizer.tokenizeSentence("<root> "
//						+ cu.getContent()));
//				lemmatizer.apply(sd);
//				morphTagger.apply(sd);
//				tagger.apply(sd);
//				for (int i = 0; i < sd.plemmas.length;i++) {
//					out.write(sd.plemmas[i]+": "+sd.ppos[i]+"\n");
//				}
//			}
//		}
//		out.close();
//	}
//
//	private static Map<String,List<ClassifyUnit>> match(List<ClassifyUnit> toMatch) {
//		Map<String,List<ClassifyUnit>> matches = new HashMap<String,List<ClassifyUnit>>();
//		for (ClassifyUnit classifyUnit : toMatch) {
//			String content = classifyUnit.getContent();
//			 SentenceData09 sd = new SentenceData09();
//			 sd.init(tokenizer.tokenizeSentence(content));
//			 lemmatizer.apply(sd);
//			 String[] lemmas = sd.plemmas;
//			 StringBuffer sb = new StringBuffer();
//			 for (String lemma : lemmas) {
//				sb.append(" "+lemma+" ");
//			}
//			for (String am : arbeitsmittel) {
//				if(sb.toString().contains(" "+am+" ")){
//					List<ClassifyUnit> matchingCus = matches.get(am);
//					if(matchingCus == null) matchingCus = new ArrayList<ClassifyUnit>();
//					matchingCus.add(classifyUnit);
//					matches.put(am, matchingCus);
//				}
//				
//			} 
//		}
//		return matches;
//	}
//
//	private static List<ClassifyUnit> getClassifyUnits() throws IOException {
//		// Translations
//		Map<Integer, List<Integer>> translations = new HashMap<Integer, List<Integer>>();
//		List<Integer> categories = new ArrayList<Integer>();
//		categories.add(1);
//		categories.add(2);
//		translations.put(5, categories);
//		categories = new ArrayList<Integer>();
//		categories.add(2);
//		categories.add(3);
//		translations.put(6, categories);
//		SingleToMultiClassConverter stmc = new SingleToMultiClassConverter(6, 4, translations);
//		ZoneJobs jobs = new ZoneJobs(stmc);
//		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
//		IEJobs iejobs = new IEJobs();
//		return iejobs.filterClassifyUnits(cus, new Integer[]{2,3,6});
//	}
//
//	private static void readArbeitsmittel() throws IOException {
//		BufferedReader in = new BufferedReader(
//				new FileReader(new File("information_extraction/data/arbeitsmittel.txt")));
//		String line = in.readLine();
//		while (line != null) {
//			arbeitsmittel.add(line.trim());
//			line = in.readLine();
//		}
//		in.close();
//	}
//
//}
