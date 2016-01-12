package ja_prof.classification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.LogLikeliHoodFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.RelativeFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.TFIDFFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.RegexClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneKNNClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneNaiveBayesClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneRocchioClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.svm.SVMClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ExperimentResult;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.evaluation.EvaluationValue;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.workflow.ZoneJobs;



public class WorkflowTest {
	
	static ZoneJobs jobs;
	static File dataFile;
	static ExperimentConfiguration testConfig; 
	static List<Integer> evaluationCategories;

	@BeforeClass
	public static void initialize() throws IOException{
		
		dataFile = new File("src/test/resources/classification/cuTestData.csv");

		//Translation- & Evaluation-config.
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
		evaluationCategories = new ArrayList<Integer>();
		evaluationCategories.add(1);
		evaluationCategories.add(2);
		evaluationCategories.add(3);

		//Experimentconfig.
		testConfig = getRandomExperimentConfig();
	}
	
	/**
	 * creates a random ExperimentConfiguration-Object
	 * @return randomConfig
	 */
	private static ExperimentConfiguration getRandomExperimentConfig() {
		Random r = new Random();
		int n = r.nextInt(4);
		int[] nGrams = null;
		if(n > 1){
			nGrams = new int[]{n};
		}
		int mi = r.nextInt(2);
		if(mi > 0){
			mi = 80;
		}
		FeatureUnitConfiguration testFuc = new FeatureUnitConfiguration(r.nextBoolean(), r.nextBoolean(), r.nextBoolean(), nGrams, r.nextBoolean(), mi, r.nextBoolean());
		
		Map<Integer, AbstractFeatureQuantifier> fqs = new HashMap<Integer,AbstractFeatureQuantifier>();
		fqs.put(0,new TFIDFFeatureQuantifier());
		fqs.put(1,new LogLikeliHoodFeatureQuantifier()); 
		fqs.put(2,new RelativeFrequencyFeatureQuantifier()); 
		fqs.put(3,new AbsoluteFrequencyFeatureQuantifier());
		AbstractFeatureQuantifier fq = fqs.get(r.nextInt(4));
		
		Map<Integer, AbstractClassifier> classifiers = new HashMap<Integer,AbstractClassifier>();
		classifiers.put(0,new ZoneKNNClassifier(false, 3, Distance.COSINUS));
		classifiers.put(1,new ZoneKNNClassifier(false, 4, Distance.EUKLID)); 
		classifiers.put(2,new ZoneNaiveBayesClassifier()); 
		classifiers.put(3,new ZoneRocchioClassifier(false, Distance.COSINUS));
		classifiers.put(4, new SVMClassifier());
		//classifiers.put(5, new WekaClassifier(new NaiveBayesMultinomial()));
		AbstractClassifier classifier = classifiers.get(r.nextInt(5));
		
		testConfig = new ExperimentConfiguration(testFuc, fq, classifier, dataFile, "src/test/resources/classification/output");
		return testConfig;
	}
	

	@Test
	public void readParagraphsFromCSVFileTest() throws IOException {
		List<ClassifyUnit> cus = jobs.getCategorizedASCIIParagraphsFromFile(dataFile);	
		Assert.assertEquals(17, cus.size());
		cus = jobs.getCategorizedParagraphsFromFile(dataFile);
		Assert.assertEquals(17, cus.size());
		
	}
	
	@Test
	public void initializeCUsTest() throws IOException{
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(dataFile);
		cus = jobs.initializeClassifyUnits(cus);
		Assert.assertEquals(17, cus.size());
		Assert.assertNotNull(cus.get(4).getFeatureUnits());
		Assert.assertNotNull(cus.get(2).getContent());
		Assert.assertNotNull(cus.get(1).getID());
		Assert.assertNotNull(((ZoneClassifyUnit) cus.get(3)).getActualClassID());
		Assert.assertNotNull(((ZoneClassifyUnit) cus.get(0)).getClassIDs());
	}
	
	@Test
	public void setFeaturesTest() throws IOException{
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(dataFile);
		cus = jobs.initializeClassifyUnits(cus);
		cus = jobs.setFeatures(cus, testConfig.getFeatureConfiguration(), true);
	}
	
	@Test
	public void setFeatureVectorsTest() throws IOException{
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(dataFile);
		cus = jobs.initializeClassifyUnits(cus);
		cus = jobs.setFeatures(cus, testConfig.getFeatureConfiguration(), true);
		
		cus = jobs.setFeatureVectors(cus, new TFIDFFeatureQuantifier(), null);
		Assert.assertNotNull(cus.get(5).getFeatureVector());
		Assert.assertEquals(cus.get(0).getFeatureVector().length, cus.get(8).getFeatureVector().length);
		
		cus = jobs.setFeatureVectors(cus, new LogLikeliHoodFeatureQuantifier(), null);
		Assert.assertNotNull(cus.get(5).getFeatureVector());
		Assert.assertEquals(cus.get(0).getFeatureVector().length, cus.get(8).getFeatureVector().length);
		
		cus = jobs.setFeatureVectors(cus, new RelativeFrequencyFeatureQuantifier(), null);
		Assert.assertNotNull(cus.get(5).getFeatureVector());
		Assert.assertEquals(cus.get(0).getFeatureVector().length, cus.get(8).getFeatureVector().length);
		
		cus = jobs.setFeatureVectors(cus, new AbsoluteFrequencyFeatureQuantifier(), null);
		Assert.assertNotNull(cus.get(5).getFeatureVector());
		Assert.assertEquals(cus.get(0).getFeatureVector().length, cus.get(8).getFeatureVector().length);
		
	}
	
	@Test
	public void preClassifyTest() throws IOException{
		List<ClassifyUnit> cus = prepareCUs();
		Map<ClassifyUnit, boolean[]> preClassified = preClassify(cus);
		Assert.assertEquals(17, preClassified.size());
		Assert.assertNotNull(preClassified.values().iterator().next());
	}
	
	@Test
	public void crossvalidateTest() throws IOException{
		List<ClassifyUnit> cus = prepareCUs();
		List<AbstractClassifier> classifiers = new ArrayList<AbstractClassifier>();
		//classifiers.add(new WekaClassifier(new NaiveBayesMultinomial()));	
		classifiers.add(new ZoneKNNClassifier(false, 4, Distance.COSINUS));
		classifiers.add(new ZoneRocchioClassifier(false, Distance.COSINUS));
		classifiers.add(new ZoneNaiveBayesClassifier());
		classifiers.add(new SVMClassifier());
		
		Map<ClassifyUnit, boolean[]> classified;
		for (AbstractClassifier classifier : classifiers) {
			ExperimentConfiguration config = new ExperimentConfiguration(testConfig.getFeatureConfiguration(), testConfig.getFeatureQuantifier(), classifier, dataFile, testConfig.getOutputFolder());
			classified = jobs.crossvalidate(cus, config);
			Assert.assertNotNull(classified.values().iterator().next());
			Assert.assertEquals(10, classified.size());
		}
		
	}
	
	@Test
	public void mergeResultsTest() throws IOException{
		List<ClassifyUnit> cus = prepareCUs();
		Map<ClassifyUnit, boolean[]> preClassified = preClassify(cus);
		Map<ClassifyUnit, boolean[]> classified = crossvalidate(cus);
		Map<ClassifyUnit, boolean[]> merged = jobs.mergeResults(classified, preClassified);	
		for (ClassifyUnit cu : classified.keySet()) {
			boolean[] classes = classified.get(cu);
			for(int i = 0; i < classes.length; i++){
				if(classes[i]){
					Assert.assertTrue(merged.get(cu)[i]);
				}
			}
		}	
	}
	
	@Test
	public void translateClassesTest() throws IOException{
		List<ClassifyUnit> cus = prepareCUs();
		Map<ClassifyUnit, boolean[]> classified = jobs.crossvalidate(cus, testConfig);
		Map<ClassifyUnit, boolean[]> translated = jobs.translateClasses(classified);
		int i = jobs.getStmc().getNumberOfCategories();
		int j = ((ZoneClassifyUnit) translated.keySet().iterator().next()).getClassIDs().length;
		Assert.assertEquals(i,j);
	}
	
	@Test
	public void evaluateTest() throws IOException{
		List<ClassifyUnit> cus = prepareCUs();
		Map<ClassifyUnit,boolean[]> classified = crossvalidate(cus);
		ExperimentResult result = jobs.evaluate(classified, evaluationCategories, testConfig);	
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getExperimentConfiguration());
		Assert.assertNotNull(result.getCategoryEvaluations());
		Assert.assertNotNull(result.getEvaluationValue(EvaluationValue.RECALL));
		Assert.assertNotNull(result.getFP());
		Assert.assertEquals(result.getNumberOfClasses(),4);
	}
	
	@Test 
	public void storeAndRankResultsTest() throws IOException, ClassNotFoundException{
		List<ClassifyUnit> cus = prepareCUs();
		List<AbstractClassifier> classifiers = new ArrayList<AbstractClassifier>();
		classifiers.add(new ZoneRocchioClassifier(false, Distance.COSINUS));
		classifiers.add(new ZoneNaiveBayesClassifier());
		classifiers.add(new ZoneKNNClassifier(false, 4, Distance.COSINUS));
		classifiers.add(new SVMClassifier());	
		Map<ClassifyUnit, boolean[]> classified;
		List<ExperimentResult> results = new ArrayList<ExperimentResult>();
		for (AbstractClassifier classifier : classifiers) {
			ExperimentConfiguration config = new ExperimentConfiguration(testConfig.getFeatureConfiguration(), testConfig.getFeatureQuantifier(), classifier, dataFile, testConfig.getOutputFolder());
			classified = jobs.crossvalidate(cus, config );
			results.add(jobs.evaluate(classified, evaluationCategories, config));
		}
		File resultFile = new File("src/test/resources/classification/output/results");
		jobs.persistExperimentResults(results, resultFile);
		Assert.assertTrue(resultFile.listFiles().length == 1);
		File rankingFile = new File("src/test/resources/classification/output/rankings");
		jobs.rankResults(resultFile, rankingFile);
		Assert.assertTrue(rankingFile.listFiles().length == 20);
		
		Random r = new Random();
		int fileNumber = r.nextInt(20);
		File testFile =  rankingFile.listFiles()[fileNumber];
		BufferedReader in = new BufferedReader(new FileReader(testFile));
		
		String line = in.readLine();
		line = in.readLine();
		double before = 1.0;
		while(line != null){
			String s = line.split("\t")[0].replace(",",".");
			double current;
			try{
				 current = Double.parseDouble(s);
			}
			catch(NumberFormatException e){
				 current = Double.NaN;
			}
			if(!(current != Double.NaN && before != Double.NaN)){
				Assert.assertTrue(current <= before);
			}
			before = current;
			line = in.readLine();
		}
		in.close();
	}
	
	

	private List<ClassifyUnit> prepareCUs() throws IOException{
		List<ClassifyUnit> cus = jobs.getCategorizedParagraphsFromFile(dataFile);
		cus = jobs.initializeClassifyUnits(cus);
		cus = jobs.setFeatures(cus, testConfig.getFeatureConfiguration(),true);
		cus = jobs.setFeatureVectors(cus, testConfig.getFeatureQuantifier(), null);
		return cus;
	}
	
	private Map<ClassifyUnit,boolean[]> crossvalidate(List<ClassifyUnit> cus) throws IOException{
		Map<ClassifyUnit, boolean[]> classified = new HashMap<ClassifyUnit, boolean[]>();
		classified = jobs.crossvalidate(cus, testConfig);
		return classified;
	}
	
	private Map<ClassifyUnit,boolean[]> preClassify(List<ClassifyUnit> cus) throws IOException{
		Map<ClassifyUnit, boolean[]> preClassified = new HashMap<ClassifyUnit, boolean[]>();
		RegexClassifier rc = new RegexClassifier("src/test/resources/classification/regex.txt");			
		for (ClassifyUnit cu : cus) {
			boolean[] classIDs = rc.classify(cu, null);
			preClassified.put(cu, classIDs);
		}
		return preClassified;
	}
	
	@AfterClass
	public static void deleteOutput(){
		File output = new File("src/test/resources/classification/output");
		deleteFiles(output);
		File svm = new File("classification/svm");
		deleteFiles(svm);
	}
	

	private static void deleteFiles(File dir) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				deleteFiles(file);
			}
			file.delete();
		}	
	}
}
