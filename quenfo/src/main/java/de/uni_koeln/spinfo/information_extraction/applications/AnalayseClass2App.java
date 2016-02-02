package de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.pentaho.packageManagement.Dependency;

import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.data.DependencyTree;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;

public class AnalayseClass2App {

	private static IEJobs jobs = new IEJobs();
	private static File competenceUnitsFile = new File("src/test/resources/information_extraction/competenceData_newTrainingData2016_2.txt");
	
	public static void main(String[] args) throws IOException {
		List<CompetenceUnit> compUnits = jobs.readCompetenceUnitsFromFile(competenceUnitsFile);
		jobs.setSentenceData(compUnits, null);
		jobs.buildDependencyTrees(compUnits);
		for (CompetenceUnit cu : compUnits) {
			if(cu.getSentence() == ""){
				continue;
			}
			System.out.println("Sentence: "+cu.getSentence());
			DependencyTree tree = cu.getDependencyTree();
			System.out.println(tree);
		}
	}
}
