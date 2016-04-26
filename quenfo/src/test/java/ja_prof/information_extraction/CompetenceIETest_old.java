package ja_prof.information_extraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs_Comps;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs_Tools;

public class CompetenceIETest_old {

	static ZoneJobs jobs;
	static IEJobs_Comps ieJobs;
	static File classifyUnitsFile;
//	static File filteredUnitsFile;
//	static File sentencesFile;
	static Integer[] relevantClasses = new Integer[] {3};
	static String sentenceDataFileName;
//	static File competenceDataOutputFile;
	static boolean innerSentenceSplitting = false;

	@BeforeClass
	public static void prepare() throws IOException {
		classifyUnitsFile = new File(
				// "src/test/resources/classification/cuTestData.csv");
				// "classification/data/trainingDataScrambled.csv");
				"classification/data/trainingSets/verified_for_IE_TrainingData_Jan2016.csv");
				//"src/test/resources/information_extraction/TestTrainingData2016.csv");
		// Translations
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
		jobs = new ZoneJobs(stmc);
		ieJobs = new IEJobs_Comps();
		StringBuffer sb = new StringBuffer();
		sb.append(classifyUnitsFile.getName().split("\\.")[0] + "_");
		for (int i = 0; i < relevantClasses.length; i++) {
			sb.append(relevantClasses[i]);
			if (i < relevantClasses.length - 1) {
				sb.append("_");
			}
		}

//		filteredUnitsFile = new File(
//				"src/test/resources/information_extraction/output/filtered_" + sb.toString() + ".txt");
//		sentencesFile = new File(
//				"src/test/resources/information_extraction/output/sentences_" + sb.toString() + ".txt");
		sentenceDataFileName = "src/test/resources/information_extraction/output/sentenceData_" + sb.toString();
//		competenceDataOutputFile = new File(
//				"src/test/resources/information_extraction/competenceData_" + sb.toString() + ".txt");
	}

	@Test
	public void filterClassifyUnitsTest() throws IOException {
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
		List<ClassifyUnit> filteredCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		Assert.assertTrue(filteredCUs.size() > 0);
		Assert.assertTrue(filteredCUs.size() <= cus.size());
		for (ClassifyUnit classifyUnit : cus) {
			ZoneClassifyUnit zCU = (ZoneClassifyUnit) classifyUnit;
			int classID = zCU.getActualClassID();
			if (filteredCUs.contains(classifyUnit)) {
				Assert.assertTrue(Arrays.asList(relevantClasses).contains(classID));
			} else {
				Assert.assertFalse(Arrays.asList(relevantClasses).contains(classID));
			}
		}
	}

//	@Test
//	public void writeFilteredClassifyUnits() throws IOException {
//		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
//		List<ClassifyUnit> filteredCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
//		ieJobs.writeFilteredClassifyUnits(filteredCUs, filteredUnitsFile);
//	}

//	@Test
//	public void readFilteredClassifyUnitsFromFile() throws IOException {
//		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
//		List<ClassifyUnit> filteredCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
//		ieJobs.writeFilteredClassifyUnits(filteredCUs, filteredUnitsFile);
//		List<ClassifyUnit> filteredClassifyUnits = ieJobs.readFilteredClassifyUnitsFromFile(filteredUnitsFile);
//		Assert.assertTrue(filteredClassifyUnits.size() == filteredCUs.size());
//	}

//	@Test
//	public void getSentencesTest() throws IOException {
//		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
//		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
//		Map<ClassifyUnit, List<String>> sentences = ieJobs.getSentences(competenceCUs, innerSentenceSplitting);
//		Assert.assertTrue(sentences.size() > 0);
//		for (ClassifyUnit classifyUnit : sentences.keySet()) {
//			Assert.assertTrue(sentences.get(classifyUnit).size() > 0);
//		}
//
//	}

//	@Test
//	public void writeSentencesTest() throws IOException {
//		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
//		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
//		Map<ClassifyUnit, List<String>> sentences = ieJobs.getSentences(competenceCUs, innerSentenceSplitting);
//		ieJobs.writeSentencesFile(sentences, sentencesFile);
//	}

	@Test
	public void initializeCompetenceUnitsTest() throws IOException {
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		List<ExtractionUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
		Assert.assertTrue(compUnits.size() > competenceCUs.size());
		for (ExtractionUnit cu : compUnits) {
			Assert.assertTrue(cu.getSentence() != null);
			Assert.assertTrue(cu.getClassifyUnitID() != null);
			Assert.assertTrue(cu.getJobAdID() != 0);
		}
	}

	@Test
	public void setSentenceDataTest() throws IOException {
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		List<ExtractionUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
		ieJobs.setSentenceData(compUnits, sentenceDataFileName);
		for (ExtractionUnit compUnit : compUnits) {
			if (compUnit.getTokens().length > 1) {
				Assert.assertTrue(compUnit.getSentenceData().pheads.length >= 1);
			}
			Assert.assertTrue(compUnit.getLemmata().length > 0);
			Assert.assertTrue(compUnit.getMorphTags().length > 0);
			Assert.assertTrue(compUnit.getPosTags().length > 0);
			Assert.assertEquals(compUnit.getLemmata().length, compUnit.getMorphTags().length);
			Assert.assertEquals(compUnit.getLemmata().length, compUnit.getPosTags().length);
		}
	}

	@Test
	public void buildDependencyTreeTest() throws IOException {
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		List<ExtractionUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
		ieJobs.setSentenceData(compUnits, sentenceDataFileName);
		ieJobs.buildDependencyTrees(compUnits);
		for (ExtractionUnit competenceUnit : compUnits) {
			Assert.assertTrue(competenceUnit.getDependencyTree() != null);
			ieJobs.buildDependencyTree(competenceUnit);
			Assert.assertTrue(competenceUnit.getDependencyTree() != null);
			System.out.println(competenceUnit.getSentence());
			System.out.println(competenceUnit.getDependencyTree());
			System.out.println("__________________________");
		}
	}

	@Test
	public void extractionTest() throws IOException {
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		List<ExtractionUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
		ieJobs.setSentenceData(compUnits, sentenceDataFileName);
		ieJobs.buildDependencyTrees(compUnits);
		ieJobs.setCompetences(compUnits);
		for (ExtractionUnit cu : compUnits) {
			Assert.assertTrue(cu.getDependencyTree() != null);
			if(cu.getCompetences() != null){
				System.out.println(cu);
			}
			System.out.println("______________________________");
		}
	}

//	@Test
//	public void writeCompetenceFile() throws IOException {
//		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
//		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
//		competenceCUs = ieJobs.treatEncoding(competenceCUs);
//		List<ExtractionUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
//		ieJobs.setSentenceData(compUnits, sentenceDataFileName);
//		ieJobs.buildDependencyTrees(compUnits);
//		ieJobs.setCompetences(compUnits);
//		ieJobs.writeCompetenceData(compUnits, competenceDataOutputFile);
//	}
	
//	@Test
//	public void readCompetenceUnitsFromFile() throws IOException{
//		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
//		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
//		List<ExtractionUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
//		ieJobs.setSentenceData(compUnits, sentenceDataFileName);
//		ieJobs.buildDependencyTrees(compUnits);
//		ieJobs.setCompetences(compUnits);
//		ieJobs.writeCompetenceData(compUnits, competenceDataOutputFile);
//		List<ExtractionUnit> read = ieJobs.readCompetenceUnitsFromFile(competenceDataOutputFile);
//		Assert.assertTrue(read.size() > 0);
//		for (ExtractionUnit cu : read) {
//			
//			Assert.assertNotNull(cu.getSentence());
//			Assert.assertNotNull(cu.getClassifyUnitID());
//			Assert.assertNotNull(cu.getJobAdID());
//			Assert.assertNotNull(cu.getSecondJobAdID());
//		}
//	}

	 @AfterClass
	 public static void deleteOutput(){
	 File output = new
	 File("src/test/resources/information_extraction/output");
	 deleteFiles(output);
	 }
	

	private static void deleteFiles(File dir) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteFiles(file);
			}
			file.delete();
		}
	}

}
