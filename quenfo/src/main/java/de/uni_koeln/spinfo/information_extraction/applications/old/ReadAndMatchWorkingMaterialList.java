//
//package de.uni_koeln.spinfo.information_extraction.applications.old;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeMap;
//
//import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
//import de.uni_koeln.spinfo.information_extraction.data.competenceExtraction.Competence;
//import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
//import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;
//import is2.data.SentenceData09;
//import is2.lemmatizer.Lemmatizer;
//import is2.tools.Tool;
//import jxl.Cell;
//import jxl.Sheet;
//import jxl.Workbook;
//import jxl.WorkbookSettings;
//import jxl.read.biff.BiffException;
//
//public class ReadAndMatchWorkingMaterialList {
//
//	// a mapping from workmaterial to sectors
//	static Map<String, Set<String>> workMaterials = new HashMap<String, Set<String>>();
//	// a mapping from sector to superior sectorfields
//	static Map<String, String> sectors = new HashMap<String, String>();
//
//	private static File workingMaterialFile = new File("information_extraction/data/Liste_konsolidiert.xls");
//	private static File competenceUnitsFile = new File(
//			"src/test/resources/information_extraction/competenceData_newTrainingData2016_2_3_6.txt");
//	private static Tool lemmatizer = new Lemmatizer(
//			"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
//	private static IETokenizer tokenizer = new IETokenizer();
//	private static IEJobs jobs = new IEJobs();
//	private static File stopwordFile = new File("classification/data/stopwords.txt");
//	
//	
//	public static void main(String[] args) throws IOException {
//		readWorkMaterials();
////		Map<CompetenceUnit, List<String>> matchesWithCompetenceUnits = matchWorkingMaterialsWithCompUnits();
////		Map<CompetenceUnit, List<String>> matchesWithCompetences = matchWorkingMaterialsWithCompetences();
//		//Map<String, Map<CompetenceUnit, List<String>>> matchingJobAds = matchJobAdsWithSectors(matches);
//
//	}
//
//	private static void printWorkMaterials() {
//		for (String wm : workMaterials.keySet()) {
//			System.out.println(wm);
//			for (String sector : workMaterials.get(wm)) {
//				System.out.println("\t" + sector);
//			}
//			System.out.println();
//		}
//	}
//
//	private static Map<CompetenceUnit, List<String>> matchWorkingMaterialsWithCompUnits() throws IOException {
//		int totalNumberOfMatches = 0;
//		List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(competenceUnitsFile);
//		int totalNumberOfCompUnits = compUnits.size();
//		Map<CompetenceUnit, List<String>> compUnitsWithWorkMats = new HashMap<CompetenceUnit, List<String>>();
//		List<CompetenceUnit> notMatchingCompUnits = new ArrayList<CompetenceUnit>();
//		// get content to compare with workMats
//		for (CompetenceUnit cu : compUnits) {
//			boolean isMatching = false;
//			String content = cu.getSentence().trim();
//			if (content.equals("")) {
//				continue;
//			}
//			SentenceData09 sd = new SentenceData09();
//			sd.init(tokenizer.tokenizeSentence(content));
//			lemmatizer.apply(sd);
//			StringBuffer sb = new StringBuffer();
//			for (String lemma : sd.plemmas) {
//				sb.append(lemma + " ");
//			}
//			content = sb.toString().trim().replace("--", "");
//			// compare
//			for (String workM : workMaterials.keySet()) {
//				if ((" " + content.toLowerCase() + " ").contains(" " + workM.toLowerCase() + " ")) {
//					isMatching = true;
//					totalNumberOfMatches++;
//					List<String> workMatSet = compUnitsWithWorkMats.get(cu);
//					if (workMatSet == null)
//						workMatSet = new ArrayList<String>();
//					workMatSet.add(workM);
//					compUnitsWithWorkMats.put(cu, workMatSet);
//				}
//			}
//			if (!isMatching) {
//				notMatchingCompUnits.add(cu);
//			}
//		}
//		printNotMatchingCompUnits(notMatchingCompUnits, compUnits.size());
//		printMatchingCompUnits(totalNumberOfMatches, compUnitsWithWorkMats, totalNumberOfCompUnits);
//		return compUnitsWithWorkMats;
//	}
//
//	private static void printNotMatchingCompUnits(List<CompetenceUnit> notMatchingCompUnits, int all) {
//		System.out.println("Not Matching CompUnits: " + notMatchingCompUnits.size() + " of " + all);
//	}
//
//	private static void printMatchingCompUnits(int totalNumberOfMatches,
//			Map<CompetenceUnit, List<String>> compUnitsWithWorkMats, int totalNumberOfCompUnits) {
//		System.out.println("Result of Match with CompeteneUnits: ");
//		System.out.println("TotalNumberOfCompetenceUnist: " + totalNumberOfCompUnits);
//		System.out.println("TotalNumberOfMatches: " + totalNumberOfMatches);
//		System.out.println("Matching CompUnits:   " + compUnitsWithWorkMats.keySet().size());
////		for (CompetenceUnit cu : compUnitsWithWorkMats.keySet()) {
////			System.out.println(cu);
////			for (String wm : compUnitsWithWorkMats.get(cu)) {
////				System.out.println("WorkMat: " + wm);
////				for (String sec : workMaterials.get(wm)) {
////					System.out.println("Sector: " + sec);
////				}
////				System.out.println();
////			}
////			System.out.println("____________________________________________________");
////		}
//	}
//	
//
//	private static void readWorkMaterials() throws IOException {
//		IETokenizer tokenizer = new IETokenizer();
//		workMaterials = new HashMap<String, Set<String>>();
//		sectors = new HashMap<String, String>();
//
//		Workbook w;
//		try {
//			WorkbookSettings ws = new WorkbookSettings();
//			ws.setEncoding("Cp1252");
//			w = Workbook.getWorkbook(workingMaterialFile, ws);
//			String sector = null;
//			String sectorField = null;
//			Sheet sheet = w.getSheet(0);
//			for (int i = 1; i < sheet.getRows(); i++) {
//				// Zeile i
//				Cell sectorCell = sheet.getCell(0, i);
//				if (sectorCell.getContents().equals("")) {
//					String wmContent = sheet.getCell(1, i).getContents();
//					// lemmatisieren
//					String[] workMats = wmContent.split(",");
//					for (String wm : workMats) {
//						wm = wm.trim();
//						SentenceData09 sd = new SentenceData09();
//						sd.init(tokenizer.tokenizeSentence(wm));
//						lemmatizer.apply(sd);
//						String[] lemmas = sd.plemmas;
//						Set<String> sectors = workMaterials.get(wm);
//						if (sectors == null) {
//							sectors = new HashSet<String>();
//						}
//						sectors.add(sector);
//						workMaterials.put(wm, sectors);
//					}
//					continue;
//				} else {
//					sector = sectorCell.getContents();
//					Cell nextCell = sheet.getCell(0, i + 1);
//					if (nextCell.getContents().equals("")) {
//						sectors.put(sector, sectorField);
//					} else {
//						sectorField = sector;
//					}
//				}
//
//			}
//		} catch (BiffException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static Map<String, Map<CompetenceUnit, List<String>>> matchJobAdsWithSectors(
//			Map<CompetenceUnit, List<String>> matchingCompUnits) {
//		Map<String, Map<CompetenceUnit, List<String>>> jobAdIdsWithSectors = new HashMap<String, Map<CompetenceUnit, List<String>>>();
//		for (CompetenceUnit cu : matchingCompUnits.keySet()) {
//			String jobAdId = cu.getJobAdID() + " " + cu.getSecondJobAdID();
//			Map<CompetenceUnit, List<String>> comps = jobAdIdsWithSectors.get(jobAdId);
//			if (comps == null) {
//				comps = new HashMap<CompetenceUnit, List<String>>();
//			}
//			List<String> workMats = matchingCompUnits.get(cu);
//			List<String> sectorList2 = comps.get(cu);
//			if (sectorList2 == null) {
//				sectorList2 = new ArrayList<String>();
//			}
//			for (String workMat : workMats) {
//				Set<String> sectors = workMaterials.get(workMat);
//				sectorList2.addAll(sectors);
//				comps.put(cu, sectorList2);
//				jobAdIdsWithSectors.put(jobAdId, comps);
//			}
//		}
//		// printJobAdsWithSectors(jobAdIdsWithSectors);
//		return jobAdIdsWithSectors;
//	}
//
//	private static void printJobAdsWithSectors(Map<String, Map<CompetenceUnit, List<String>>> jobAdIdsWithSectors) {
//		for (String id : jobAdIdsWithSectors.keySet()) {
//			int includingCompUnits = 0;
//			System.out.println("ID: " + id);
//			List<String> allSectors = new ArrayList<String>();
//			Map<CompetenceUnit, List<String>> map = jobAdIdsWithSectors.get(id);
//			System.out.println("compUnits: ");
//			for (CompetenceUnit comp : map.keySet()) {
//				System.out.println("- " + comp.getSentence());
//				includingCompUnits++;
//				allSectors.addAll(map.get(comp));
//			}
//			System.out.println("compUnits: " + includingCompUnits);
//			for (String sector : allSectors) {
//				System.out.println("Sector: " + sector);
//			}
//			System.out.println();
//		}
//	}
//
//	private static Map<CompetenceUnit, List<String>> matchWorkingMaterialsWithCompetences() throws IOException {
//		List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(competenceUnitsFile);
//		int all = compUnits.size();
//		compUnits = jobs.filterEmptyCompetenceUnits(compUnits);
//		int filtered = compUnits.size();
//		Map<CompetenceUnit, List<String>> matchingCompUnits = new HashMap<CompetenceUnit, List<String>>();
//		List<CompetenceUnit> notMatchingCompUnits = new ArrayList<CompetenceUnit>();
//		List<Competence> notMatchingCompetences = new ArrayList<Competence>();
//		int matchCount = 0;
//		int matchingCompetences = 0;
//		for (CompetenceUnit cu : compUnits) {
//			boolean isMatchingCompUnit = false;
//			for (Competence c : cu.getCompetences()) {
//				boolean isMatchingCompetence = false;
//				String content = " " + c.getCompetence() + " ";
//				for (String workM : workMaterials.keySet()) {
//					if (content.toLowerCase().contains(" " + workM.toLowerCase() + " ")) {
//						isMatchingCompetence = true;
//						isMatchingCompUnit = true;
//						matchCount++;
//						List<String> workMas = matchingCompUnits.get(cu);
//						if (workMas == null) {
//							workMas = new ArrayList<String>();
//						}
//						workMas.add(workM);
//						matchingCompUnits.put(cu, workMas);
//					}
//				}
//				if(isMatchingCompetence){
//					matchingCompetences++;
//				}
//				else{
//					notMatchingCompetences.add(c);
//				}
//			}
//			if(!isMatchingCompUnit){
//				notMatchingCompUnits.add(cu);
//			}
//		}
//		printMatchingCompetences(matchingCompUnits, all, filtered, matchCount, matchingCompetences);
//		printNotMatchingCompUnits(notMatchingCompUnits, all);
//		analyseNotMatchingCompetences(notMatchingCompetences);
//		return matchingCompUnits;
//	}
//
//	private static void printMatchingCompetences(Map<CompetenceUnit, List<String>> compUnitsWithWorkMaterials,int compUnitsNum, int filteredCompUnits, int matchCount, int matchingCompetences) {
//		System.out.println();
//		System.out.println("Result of Match with Competences: ");
//		System.out.println("TotalNumberOfCompetenceUnist: " + compUnitsNum);
//		System.out.println("TotalNumberOfMatches: " + matchCount);
//		System.out.println("Matching CompUnits:   " + compUnitsWithWorkMaterials.keySet().size());
//		System.out.println("Matching Competences: " + matchingCompetences);
////		for (CompetenceUnit cu : compUnitsWithWorkMaterials.keySet()) {
////			System.out.println(cu);
////			for (String wm : compUnitsWithWorkMaterials.get(cu)) {
////				System.out.println("WorkMat: " + wm);
////				for (String sec : workMaterials.get(wm)) {
////					System.out.println("Sector: " + sec);
////				}
////				System.out.println();
////			}
////			System.out.println("____________________________________________________");
////		}
//	}
//
//	private static void analyseNotMatchingCompetences(List<Competence> notMatchingCompetences) throws IOException{
//		Set<String> stopwords = readStopwords();
//		Map<String, Integer> tokenCounts = new HashMap<String,Integer>();
//		for (Competence c : notMatchingCompetences) {
//			List<String> tokens = Arrays.asList(tokenizer.tokenizeSentence(c.getCompetence()));
//			for (String token : tokens) {
//				if(stopwords.contains(token)){
//					continue;
//				}
//				if(tokenCounts.keySet().contains(token)){
//					int i = tokenCounts.get(token);
//					i++;
//					tokenCounts.put(token, i);
//				}
//				else{
//					tokenCounts.put(token, 1);
//				}	
//			}
//		}
//		Map<Integer, List<String>> sortedTokenCounts = new TreeMap<Integer,List<String>>();
//		for (String s : tokenCounts.keySet()) {
//			int i = tokenCounts.get(s);
//			List<String> list = sortedTokenCounts.get(i);
//			if(list == null) list = new ArrayList<String>();
//			list.add(s);
//			sortedTokenCounts.put(i++, list);
//		}
//		System.out.println("Tokens in not matching Competences:");
//		List<Integer> keys = new ArrayList<Integer>(sortedTokenCounts.keySet());
//		for (Integer i = sortedTokenCounts.size()-1; i >=0; i--) {
//			int count = keys.get(i);
//			System.out.println();
//			System.out.println(count);
//			for (String s : sortedTokenCounts.get(count)) {
//				System.out.println(s);
//			}
//		}
//	}
//	
//	private static Set<String> readStopwords() throws IOException{
//		Set<String> stopwords = new HashSet<String>();
//		BufferedReader in = new BufferedReader(new FileReader(stopwordFile));
//		String line = in.readLine();
//		while(line!=null){
//			stopwords.add(line.trim());
//			line = in.readLine();
//		}
//		in.close();
//		return stopwords;
//	}
//	
//}
