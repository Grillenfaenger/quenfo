package ja_prof.information_extraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mahout.cf.taste.hadoop.preparation.ToItemVectorsReducer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.information_extraction.applications.ReadAndMatchWorkingMaterialList;
import de.uni_koeln.spinfo.information_extraction.data.AMContext;
import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.data.Token;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;

public class ArbeitsmittelIETest {
	
	int maxNumberOfIterations = 5;

	static ZoneJobs jobs;
	static IEJobs ieJobs;
	static File classifyUnitsFile;
	static File filteredUnitsFile;
	static File amFile = new File("information_extraction/data/arbeitsmittel.txt");
	static File noAMFile = new File("information_extraction/data/kein_arbeitsmittel.txt");
	static File contextFile = new File("information_extraction/data/amContexts.txt");
	static File sectorsFiles = new File("information_extraction/data/Liste_konsolidiert.xls");
	// static File sentencesFile;
	static Integer[] relevantClasses = new Integer[] { 2, 3, 6 };
	// static String sentenceDataFileName;
	static File competenceDataOutputFile;
	static boolean innerSentenceSplitting = false;

	@BeforeClass
	public static void prepare() throws IOException {
		classifyUnitsFile = new File(
				//"classification/data/trainingDataScrambled.csv");				
				"classification/data/newTrainingData2016.csv");

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
		ieJobs = new IEJobs();

		StringBuffer sb = new StringBuffer();
		sb.append(classifyUnitsFile.getName().split("\\.")[0] + "_");
		for (int i = 0; i < relevantClasses.length; i++) {
			sb.append(relevantClasses[i]);
			if (i < relevantClasses.length - 1) {
				sb.append("_");
			}
		}

		filteredUnitsFile = new File(
				"src/test/resources/information_extraction/output/filtered_" + sb.toString() + ".txt");
		competenceDataOutputFile = new File(
				"src/test/resources/information_extraction/competenceData_" + sb.toString() + ".txt");
	}

	@Test
	public void workFlowTest() throws IOException {
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(classifyUnitsFile);
		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		competenceCUs = ieJobs.treatEncoding(competenceCUs);
		List<CompetenceUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
//		ieJobs.setSentenceData(compUnits, null);
//		ieJobs.readAMLists(amFile, noAMFile);
//		int iteration = 0;
//		boolean goOn = true;
//		while(goOn) {
//			ieJobs.matchWithAMList(compUnits);
//			Map<CompetenceUnit, Map<Integer, List<AMContext>>> matches = ieJobs.extractNewAMs(contextFile, compUnits);
//			if(matches.isEmpty()){
//				break;
//			}
//			goOn = ieJobs.annotateDetectedAMs(matches, amFile, noAMFile, iteration, maxNumberOfIterations);
//			iteration++;
//		};
//		ieJobs.countAMs(compUnits, new File("information_extraction/data/AM_counts.txt"));
		ieJobs.matchAMsWithSectors(compUnits, sectorsFiles);
	}
	


	@AfterClass
	public static void deleteOutput() {
		File output = new File("src/test/resources/information_extraction/output");
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
