package ja_prof.information_extraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.toolExtraction.ToolContext;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs_Comps;
import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs_Tools;

public class CompetenceIETest {
	
	static ZoneJobs jobs;
	static IEJobs_Comps ieJobs;
	static List<File> trainingDataFiles = new ArrayList<File>();
	static File compsFile = new File("information_extraction/data/competences/competences.txt");
	static File noCompsFile = new File("information_extraction/data/competences/noCompetences.txt");
	static File toolsFile = new File("information_extraction/data/tools/tools.txt");
	static File contextFile = new File("information_extraction/data/competences/competence_contexts.txt");
	static Integer[] relevantClasses = new Integer[] {3};
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
		ieJobs = new IEJobs_Comps();

//		trainingDataFiles.add(new File("classification/data/trainingDataScrambled.csv"));
//		trainingDataFiles.add(new File("classification/data/newTrainingData2016.csv"));	
		trainingDataFiles.add(new File("classification/data/trainingSets/not_verified_TrainingData_March2016.csv"));
	}

	
	@Test
	public void workflowTest() throws IOException{
		List<ClassifyUnit> cus = new ArrayList<ClassifyUnit>();
		for (File file : trainingDataFiles) {
			cus.addAll(jobs.getCategorizedParagraphsFromFile(file));
		}
		List<ClassifyUnit> competenceCUs = ieJobs.filterClassifyUnits(cus, relevantClasses);
		competenceCUs = ieJobs.treatEncoding(competenceCUs);
		List<ExtractionUnit> compUnits = ieJobs.initializeCompetenceUnits(competenceCUs, innerSentenceSplitting);
		ieJobs.setSentenceData(compUnits, null);
		ieJobs.readCompLists(compsFile, noCompsFile, toolsFile);
		ieJobs.matchWithCompLists(compUnits);
		Map<ExtractionUnit, Map<Integer,List<ToolContext>>> detected = ieJobs.extractNewComps(contextFile, compUnits);
		ieJobs.annotatePotentialComps(detected, compsFile, noCompsFile, toolsFile);
	}

}
