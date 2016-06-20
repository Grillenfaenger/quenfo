//package de.uni_koeln.spinfo.information_extraction.applications.old;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
//import de.uni_koeln.spinfo.information_extraction.data.competenceExtraction.Competence;
//import de.uni_koeln.spinfo.information_extraction.data.old.ClassifiedCompetencesTrainingDataGenerator;
//import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;
//
//public class AnnotateCompetencesApp {
//	
//	static IEJobs jobs = new IEJobs();
//	static File competenceData = new File("src/test/resources/information_extraction/competenceData_newTrainingData2016_2_3_6.txt");
//	static File outputFile = new File("src/test/resources/information_extraction/classifedCompetences_newTrainingData2016.txt");
//	
//	public static void main(String[] args) throws IOException {
//		List<CompetenceUnit> comps = jobs.readCompetenceUnitsFromFile(competenceData);
//		List<Competence> competences = new ArrayList<Competence>();
//		for (CompetenceUnit cu : comps) {
//			if(cu.getCompetences() != null){
//				competences.addAll(cu.getCompetences());
//			}
//		}
//		ClassifiedCompetencesTrainingDataGenerator tdg = new ClassifiedCompetencesTrainingDataGenerator(outputFile);
//		tdg.annotate(competences);	
//	}
//	
//
//}
