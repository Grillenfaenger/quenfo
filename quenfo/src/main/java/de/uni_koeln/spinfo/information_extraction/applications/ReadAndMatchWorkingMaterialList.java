package de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.uni_koeln.spinfo.information_extraction.data.Competence;
import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;
import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import is2.tools.Tool;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class ReadAndMatchWorkingMaterialList {
	
	static Map<String, List<String>> workMaterials = new HashMap<String, List<String>>();
	static Map<String,String> sectors = new HashMap<String, String>();

	private static File workingMaterialFile = new File("information_extraction/data/Liste_konsolidiert.xls");
	private static Tool lemmatizer = new Lemmatizer(
			"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
	private static IETokenizer tokenizer = new IETokenizer();

	public static void main(String[] args) throws IOException {
		readWorkMaterials();
	//matchWorkingMaterialsWithCompetences();
		matchWorkingMaterialsWithCompUnits();
//		for (String wm : workMaterials.keySet()) {
//			for (String sec : workMaterials.get(wm)) {
//				System.out.println("--> " + sec);
//				System.out.println();
//			}
//		}
//		for (String sector : sectors.keySet()) {
//			System.out.println("sector: " + sector);
//			System.out.println("Überbegriff: " + sectors.get(sector));
//			System.out.println();
//		}
	}



	private static void matchWorkingMaterialsWithCompUnits() throws IOException{
		Map<String, List<String>> matches = new HashMap<String, List<String>>();
		Set<CompetenceUnit> matchingCompUnits = new HashSet<CompetenceUnit>();
		int matchCount = 0;
		int totalNumberOfCompUnits = 0;
		IEJobs jobs = new IEJobs();
		List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(new File("src/test/resources/information_extraction/competenceData_newTrainingData2016_3"
				+ "+.txt"));
		totalNumberOfCompUnits = compUnits.size();
		for (CompetenceUnit cu : compUnits) {
					String content = cu.getSentence();
					if(content.equals("")){
						System.out.println("empty content");
						continue;
					}
					SentenceData09 sd = new SentenceData09();
					sd.init(tokenizer.tokenizeSentence(content));
					lemmatizer.apply(sd);
					StringBuffer sb = new StringBuffer();
					for (String lemma : sd.plemmas) {
						sb.append(lemma +" ");
					}
					content = " "+sb.toString();
					for (String workM : workMaterials.keySet()) {				
						if(content.toLowerCase().contains(" "+workM.toLowerCase()+" ")){
							matchingCompUnits.add(cu);
							matchCount++;
							List<String> list = matches.get(workM);
							if(list == null){
								list = new ArrayList<String>();
							}
							list.add(content);
							matches.put(workM, list);
						}
					}
				
			
		}
		System.out.println("total number of compUnits: " + totalNumberOfCompUnits);
		System.out.println("NumberOfMatches: " + matchCount);
		System.out.println("NumberOfMatchingCompUnits: " + matchingCompUnits.size());
	    Map<Integer, List<String>> stats = new TreeMap<Integer, List<String>>();
	    for (String wm : matches.keySet()) {
			int comps = matches.get(wm).size();
			List<String> list = stats.get(comps);
			if(list == null) list = new ArrayList<String>();
			list.add(wm);
			stats.put(comps, list);
		}
	    for (Integer i : stats.keySet()) {
	    	
			System.out.println(i+": ");
			for (String	wm  : stats.get(i)) {
				System.out.println(wm);
				List<String> secs = workMaterials.get(wm);
				for (String s : secs) {
					System.out.println("--> " + s + " --> "+ sectors.get(s));
				}
			}
			
			
		}
	}

	private static void matchWorkingMaterialsWithCompetences() throws IOException {
		Map<String, List<String>> matches = new HashMap<String, List<String>>();
		Set<Competence> matchingCompetences = new HashSet<Competence>();
		int matchCount = 0;
		int totalNumbOfCpmpetences = 0;
		int totalNumberOfCompUnits = 0;
		int numberOfNotEmptyCompUnits = 0;
		int numberOfMatchingCompUnits = 0;
		IEJobs jobs = new IEJobs();
		List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(new File("src/test/resources/information_extraction/competenceData_newTrainingData2016_3.txt"));
		totalNumberOfCompUnits = compUnits.size();
		compUnits = jobs.filterEmptyCompetenceUnits(compUnits);
		numberOfNotEmptyCompUnits = compUnits.size();
		boolean matched = false;
		for (CompetenceUnit cu : compUnits) {
			matched = false;
			if(cu.getCompetences() != null){
				for (Competence c : cu.getCompetences()) {
					totalNumbOfCpmpetences++;
					String content = " "+c.getCompetence()+" ";
					for (String workM : workMaterials.keySet()) {				
						if(content.toLowerCase().contains(" "+workM.toLowerCase()+" ")){
							matchingCompetences.add(c);
							if(!matched){
								numberOfMatchingCompUnits++;
								matched = true;
							}	
							matchCount++;
							List<String> list = matches.get(workM);
							if(list == null){
								list = new ArrayList<String>();
							}
							list.add(content);
							matches.put(workM, list);
						}
					}
				}
			}
		}
		System.out.println("total number of compUnits: " + totalNumberOfCompUnits);
		System.out.println("number of not empty compUnits: " + numberOfNotEmptyCompUnits);
		System.out.println("NumberOfMatches: " + matchCount);
		System.out.println("total number Of competences: " + totalNumbOfCpmpetences );
		System.out.println("NumberOfMatchingCompetences: " + matchingCompetences.size());
		System.out.println("numberOfMatchingComptUnits: " + numberOfMatchingCompUnits);
	    Map<Integer, List<String>> stats = new TreeMap<Integer, List<String>>();
	    for (String wm : matches.keySet()) {
			int comps = matches.get(wm).size();
			List<String> list = stats.get(comps);
			if(list == null) list = new ArrayList<String>();
			list.add(wm);
			stats.put(comps, list);
		}
	    for (Integer i : stats.keySet()) {
	    	
			System.out.println(i+": ");
			for (String	wm  : stats.get(i)) {
				System.out.println(wm);
			}
			
			
		}
	}




	private static void readWorkMaterials() throws IOException {
		IETokenizer tokenizer = new IETokenizer();
		workMaterials = new HashMap<String, List<String>>();
	    sectors = new HashMap<String,String>();

		Workbook w;
		try {
			WorkbookSettings ws = new WorkbookSettings();
			ws.setEncoding("Cp1252");
			w = Workbook.getWorkbook(workingMaterialFile, ws);
			String sector = null;
			String sectorField = null;
			Sheet sheet = w.getSheet(0);
			for (int i = 1; i < sheet.getRows(); i++) {
				// Zeile i
				Cell sectorCell = sheet.getCell(0, i);
				if (sectorCell.getContents().equals("")) {
					String wmContent = sheet.getCell(1, i).getContents();
					//lemmatisieren
					String[] workMats = wmContent.split(",");
					for (String wm : workMats) {
						wm = wm.trim();
						SentenceData09 sd = new SentenceData09();
						sd.init(tokenizer.tokenizeSentence(wm));
						lemmatizer.apply(sd);
						String[] lemmas = sd.plemmas;
						List<String> sectors = workMaterials.get(wm);
						if(sectors == null){
							sectors = new ArrayList<String>();
						}
						sectors.add(sector);
						workMaterials.put(wm, sectors);
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
