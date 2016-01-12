package de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class ReadWorkingMaterialList {

	private static File workingMaterialFile = new File("information_extraction/data/Liste_konsolidiert.xls");

	public static void main(String[] args) throws IOException {
		readWorkMaterials();
	}

	private static void readWorkMaterials() throws IOException {
		Map<String, List<String>> workMaterials = new HashMap<String, List<String>>();
		Map<String, String> sectors = new HashMap<String, String>();

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
			for (int i = 0; i < sheet.getRows(); i++) {
				// Zeile i
				Cell sectorCell = sheet.getCell(0, i);
				if (sectorCell.getContents().equals("")) {
					String workMat = sheet.getCell(1, i).getContents();
					List<String> sectorList = workMaterials.get(workMat);
					if (sectorList == null) {
						sectorList = new ArrayList<String>();
					}
					sectorList.add(sector);
					workMaterials.put(workMat, sectorList);
					continue;
				} else {
					sector = sectorCell.getContents();
					Cell nextCell = sheet.getCell(0, i + 1);
					if (nextCell.getContents().equals("")) {
						// sectors.put(sector, sectorFields);
						// sectorFields = new ArrayList<String>();
						sectors.put(sector, sectorField);
					} else {
						sectorField = sector;
					}
				}

			}
		} catch (BiffException e) {
			e.printStackTrace();
		}
		for (String sector : sectors.keySet()) {
			System.out.println("sector: " + sector);
			System.out.println("--> " + sectors.get(sector));
		}
	}
}
