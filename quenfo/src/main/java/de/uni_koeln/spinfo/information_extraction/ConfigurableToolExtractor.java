package de.uni_koeln.spinfo.information_extraction;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.dbIO.DbConnector;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.Tool;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.ToolContext;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs_Tools;
import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;

public class ConfigurableToolExtractor {

	public void extractTools(int startPos, int count, boolean matchWithDB, int tableSize, Connection inputConnection, Connection outputConnection, File toolsFile, File noToolsFile, File contextFile) throws SQLException, IOException {
		
		IEJobs_Tools jobs = new IEJobs_Tools();
		
		//set number of single- and multiclasses
		setNumberOfSingleAndMultiClasses();	
		//get 2- or 3-classified Paragraphs from ClassesCorrectable
		String query = "SELECT TxtID, ClassTWO, ClassTHREE FROM Classes_Correctable WHERE(ClassTWO = '1' OR ClassTHREE = '1') LIMIT ? OFFSET ?;";
		
		List<ClassifyUnit> classifyUnits = getClassifyUnitsFromDB(query,count, startPos, inputConnection, jobs);
		
		System.out.println("\n extracted classifyUnits for Tool-detection: "+classifyUnits.size()+"\n");
		
		//create CompetenceUnits
		List<ExtractionUnit> compUnits = jobs.initializeCompetenceUnits(classifyUnits, false);
		//set SentenceData
		jobs.setSentenceData(compUnits, null);
		
		//read (no-)Tool-List
		jobs.readToolLists(toolsFile, noToolsFile);
		
		
		
		boolean goOn = true;
		boolean changed = false;
		while(goOn){
			//match competenceUnits with current (no-)ToolList  (flags tokens as tool/noTool/startOfTool)
			jobs.matchWithToolLists(compUnits);
			//detect new Tools
			Map<ExtractionUnit, Map<Integer, List<ToolContext>>> potentialTools = jobs.extractNewTools(contextFile, compUnits);
			if(potentialTools.isEmpty()){
				System.out.println("\n no potential tools \n");
				break;
			}
			//annotate potential Tools  (update (no-)ToolsList / (no-)ToolsFile)
			goOn = jobs.annotatePotentialTools(potentialTools, toolsFile, noToolsFile);
			changed = true;
		}
		
	//write ToolsByJobAd in DB (for each ClassifyUnit in DB)
		if(changed && matchWithDB){
			System.out.println("\n match (new) Tools with database. (this will take a while. please do not interrupt) \n");
			DbConnector.createToolOutputTables(outputConnection);
			int offset = 0;
			query = "SELECT TxtID, ClassTWO, ClassTHREE FROM Classes_Correctable WHERE(ClassTWO = '1' OR ClassTHREE = '1') LIMIT ? OFFSET ?";	
			while(true){
				List<ClassifyUnit> allUnits = getClassifyUnitsFromDB(query, 1000, offset, inputConnection, jobs);
				if(allUnits.isEmpty()){
					break;
				}
				System.out.println("match ClassifyUnits " + offset +" - "+(offset+allUnits.size()));
				offset = offset+allUnits.size();
				List<ExtractionUnit> competenceUnits = jobs.initializeCompetenceUnits(allUnits, false);
				jobs.setSentenceData(competenceUnits, null, true);
				Map<ExtractionUnit, List<Tool>> toolsByUnit = jobs.matchWithToolLists(competenceUnits);
				for (ExtractionUnit extractionUnit : toolsByUnit.keySet()) {
					DbConnector.writeToolsInDB(extractionUnit, toolsByUnit.get(extractionUnit), outputConnection);
				}
			}
	
		}
		if(matchWithDB){
			System.out.println("\n finished DB-update! \n");
		}
		else{
			System.out.println("finished Tools-update!");
		}
		
	}

	private List<ClassifyUnit> getClassifyUnitsFromDB(String query, int count, int startPos, Connection inputConnection, IEJobs_Tools jobs) throws SQLException {
		ResultSet result;

			PreparedStatement prepStmt = inputConnection.prepareStatement(query);
			prepStmt.setInt(1, count);
			prepStmt.setInt(2, startPos);	
			result = prepStmt.executeQuery();	
		List<ClassifyUnit> classifyUnits = new ArrayList<ClassifyUnit>();
		String sql;
		ClassifyUnit classifyUnit;
		Statement stmt;
		ResultSet cuResult;
		while(result.next()){
			int textID = result.getInt(1);
			int class2 = result.getInt(2);
			int class3 = result.getInt(3);
			int classID;
			if(class2 == 1){
				if(class3==1){
					classID = 6;
				}
				else{
					classID = 2;
				}
			}
			else{
				classID = 3;
			}
			sql = "SELECT ParaID, Jahrgang, ZEILENNR, STELLENBESCHREIBUNG FROM ClassifiedParaTexts WHERE (ID = '"+textID+"')";
			stmt = inputConnection.createStatement();
			cuResult = stmt.executeQuery(sql);
			classifyUnit = new JASCClassifyUnit(cuResult.getString(4), cuResult.getInt(2), cuResult.getInt(3), UUID.fromString(cuResult.getString(1)));
			((ZoneClassifyUnit) classifyUnit).setActualClassID(classID);
			classifyUnits.add(classifyUnit);
		}
		
		//treat encoding
		classifyUnits = jobs.treatEncoding(classifyUnits);
		return classifyUnits;
	}

	private void setNumberOfSingleAndMultiClasses(){
		Map<Integer, List<Integer>> translations = new HashMap<Integer, List<Integer>>();
		List<Integer> categories = new ArrayList<Integer>();
		categories.add(1);
		categories.add(2);
		translations.put(5, categories);
		categories = new ArrayList<Integer>();
		categories.add(2);
		categories.add(3);
		translations.put(6, categories);
		SingleToMultiClassConverter stmc = new SingleToMultiClassConverter(6, 4, translations);
//		JASCClassifyUnit.setNumberOfCategories(stmc.getNumberOfCategories(), stmc.getNumberOfClasses(), stmc.getTranslations());
	}
	

}
