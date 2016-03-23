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
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.information_extraction.applications.ReadAndMatchWorkingMaterialList;
import de.uni_koeln.spinfo.information_extraction.data.ToolContext;
import de.uni_koeln.spinfo.information_extraction.data.CompetenceUnit;
import de.uni_koeln.spinfo.information_extraction.data.Token;
import de.uni_koeln.spinfo.information_extraction.data.Tool;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;

public class ArbeitsmittelIETest {

	int maxNumberOfIterations = 5;

	static ZoneJobs jobs;
	static IEJobs ieJobs;
	static List<File> trainingDataFiles = new ArrayList<File>();
	static File filteredUnitsFile;
	static File toolsFile = new File("information_extraction/data/tools.txt");
	static File noToolsFile = new File("information_extraction/data/no_tools.txt");
	static File contextFile = new File("information_extraction/data/toolContexts.txt");
	static File sectorsFiles = new File("information_extraction/data/Liste_konsolidiert.xls");
	static File toolCountsFile = new File("information_extraction/data/toolCounts.txt");
	static Integer[] relevantClasses = new Integer[] { 2, 3, 6 };
	static boolean innerSentenceSplitting = false;

	@BeforeClass
	public static void prepare() throws IOException {

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

//		trainingDataFiles.add(new File("classification/data/trainingDataScrambled.csv"));
//		trainingDataFiles.add(new File("classification/data/newTrainingData2016.csv"));	
		trainingDataFiles.add(new File("classification/data/notAnnotatedTrainingData_March2016.csv"));

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < relevantClasses.length; i++) {
			sb.append(relevantClasses[i]);
			if (i < relevantClasses.length - 1) {
				sb.append("_");
			}
		}
		
	}

	@Test
	public void workFlowTest() throws IOException {
		List<ClassifyUnit> cus = new ArrayList<ClassifyUnit>();
		for (File file : trainingDataFiles) {
			cus.addAll(jobs.getCategorizedParagraphsFromFile(file));
		}
		cus = cus.subList(4200, 4250);
		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		competenceCUs = ieJobs.treatEncoding(competenceCUs);
		List<CompetenceUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
		ieJobs.setSentenceData(compUnits, null);
		ieJobs.readToolLists(toolsFile, noToolsFile);
		int iteration = 0;
		boolean goOn = true;
		while (goOn) {
			ieJobs.matchWithToolLists(compUnits);
			Map<CompetenceUnit, Map<Integer, List<ToolContext>>> matches = ieJobs.extractNewTools(contextFile, compUnits);
			if (matches.isEmpty()) {
				break;
			}
			goOn = ieJobs.annotatePotentialTools(matches, toolsFile, noToolsFile, iteration, maxNumberOfIterations);
			iteration++;
		}
		;
		ieJobs.countTools(compUnits, toolCountsFile);
		// ieJobs.matchAMsWithSectors(compUnits, sectorsFiles);
//		Map<CompetenceUnit, List<Tool>> toolsByCU = ieJobs.getToolsByCU(compUnits);
//		for (CompetenceUnit cu : toolsByCU.keySet()) {
//			System.out.println(cu.getSentence());
//			for (Tool tool : toolsByCU.get(cu)) {
//				System.out.println("--> "+tool);
//			}
//		}
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
