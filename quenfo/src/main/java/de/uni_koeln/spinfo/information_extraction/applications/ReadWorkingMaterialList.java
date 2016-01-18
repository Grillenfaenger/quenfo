package de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class ReadWorkingMaterialList {
	
	static Map<String, String> workMaterials = new HashMap<String, String>();
	static Map<String, String> sectors = new HashMap<String, String>();

	private static File workingMaterialFile = new File("information_extraction/data/Liste_konsolidiert.xls");
	private static Tool lemmatizer = new Lemmatizer(
			"models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");

	public static void main(String[] args) throws IOException {
		readWorkMaterials();
		compareWithComptences();
	}

	private static void compareWithComptences() throws IOException {
		IEJobs jobs = new IEJobs();
		List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(new File("src/test/resources/information_extraction/competenceData_newTrainingData2016_2_3_6.txt"));
		for (CompetenceUnit cu : compUnits) {
			if(cu.getCompetences() != null){
				for (Competence c : cu.getCompetences()) {
					String content = c.getCompetence();
					for (String workM : workMaterials.keySet()) {
						//compare
					}
				}
			}
		}
		
	}

	private static void readWorkMaterials() throws IOException {
		IETokenizer tokenizer = new IETokenizer();
		workMaterials = new HashMap<String, String>();
	    sectors = new HashMap<String, String>();

		Workbook w;
		try {
			WorkbookSettings ws = new WorkbookSettings();
			ws.setEncoding("Cp1252");
			w = Workbook.getWorkbook(workingMaterialFile, ws);
			String sector = null;
			String sectorField = null;
			// List<String> sectorFields = new ArrayList<String>();
			// Get the first sheet
			Sheet sheet = w.getSheet(0);
			for (int i = 1; i < sheet.getRows(); i++) {
				// Zeile i
				Cell sectorCell = sheet.getCell(0, i);
				if (sectorCell.getContents().equals("")) {
					String content = sheet.getCell(1, i).getContents();
					SentenceData09 sd = new SentenceData09();
					sd.init(tokenizer.tokenizeSentence(content));
					lemmatizer.apply(sd);
					String[] lemmas = sd.plemmas;
					StringBuffer workMat = new StringBuffer();
					for (String string : lemmas) {
						workMat.append(" "+string);
					}
					workMaterials.put(workMat.toString().trim(), sector);
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
