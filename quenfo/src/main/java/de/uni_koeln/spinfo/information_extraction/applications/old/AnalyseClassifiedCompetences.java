//package de.uni_koeln.spinfo.information_extraction.applications.old;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeMap;
//
//import de.uni_koeln.spinfo.information_extraction.data.TYPE;
//import de.uni_koeln.spinfo.information_extraction.data.competenceExtraction.Competence;
//import de.uni_koeln.spinfo.information_extraction.data.old.ClassifiedCompetencesTrainingDataGenerator;
//import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
//
//public class AnalyseClassifiedCompetences {
//	
//	private static File classifiedCompetencesFile = new File("information_extraction/data/classifiedCompetences_trainingDataScrambled_3_6.txt");
//	private static File K_statistics = new File("information_extraction/data/K_statistics_trainingDataScrambled_3_6.txt");
//	private static File A_statistics = new File("information_extraction/data/A_statistics_trainingDataScrambled_3_6.txt");
//	private static File KA_statistics = new File("information_extraction/data/KA_statistics_trainingDataScrambled_3_6.txt");
//	private static File stopwordFile = new File("classification/data/stopwords.txt");
//	
//	public static void main(String[] args) throws IOException {
//		Set<String> stopwords = new HashSet<String>();
//		BufferedReader in = new BufferedReader(new FileReader(stopwordFile));
//		String line = in.readLine();
//		while(line!=null){
//			stopwords.add(line.trim());
//			line = in.readLine();
//		}
//		in.close();
//		IETokenizer tokenizer = new IETokenizer();
//		
//		ClassifiedCompetencesTrainingDataGenerator tdg = new ClassifiedCompetencesTrainingDataGenerator(classifiedCompetencesFile);
//		List<Competence> classified = tdg.getclassifedCompetences();
//		Map<String, Integer> kStatistics = new HashMap<String, Integer>();
//		Map<String, Integer> aStatistics = new HashMap<String, Integer>();
//		Map<String, Integer> kaStatistics = new HashMap<String, Integer>();
//		for (Competence competence : classified) {
//			if(competence.getType() == TYPE.A){
//				List<String> tokens = Arrays.asList(tokenizer.tokenizeSentence(competence.getCompetence()));
//				for (String token : tokens) {
//					if(stopwords.contains(token)){
//						continue;
//					}
//					if(aStatistics.keySet().contains(token)){
//						int i = aStatistics.get(token);
//						i++;
//						aStatistics.put(token, i);
//					}
//					else{
//						aStatistics.put(token, 1);
//					}	
//				}
//			}
//			if(competence.getType() == TYPE.K){
//				List<String> tokens = Arrays.asList(tokenizer.tokenizeSentence(competence.getCompetence()));
//				for (String token : tokens) {
//					if(stopwords.contains(token)){
//						continue;
//					}
//					if(kStatistics.keySet().contains(token)){
//						int i = kStatistics.get(token);
//						i++;
//						kStatistics.put(token, i);
//					}
//					else{
//						kStatistics.put(token, 1);
//					}	
//				}
//			}
//			if(competence.getType() == TYPE.AK){
//				List<String> tokens = Arrays.asList(tokenizer.tokenizeSentence(competence.getCompetence()));
//				for (String token : tokens) {
//					if(stopwords.contains(token)){
//						continue;
//					}
//					if(kaStatistics.keySet().contains(token)){
//						int i = kaStatistics.get(token);
//						i++;
//						kaStatistics.put(token, i);
//					}
//					else{
//						kaStatistics.put(token, 1);
//					}	
//				}
//			}
//		}
//		Map<Integer, List<String>> kMap = new TreeMap<Integer,List<String>>();
//		for (String s : kStatistics.keySet()) {
//			int i = kStatistics.get(s);
//			List<String> list = kMap.get(i);
//			if(list == null) list = new ArrayList<String>();
//			list.add(s);
//			kMap.put(i++, list);
//		}
//		Map<Integer, List<String>> aMap = new TreeMap<Integer,List<String>>();
//		for (String s : aStatistics.keySet()) {
//			int i = aStatistics.get(s);
//			List<String> list = aMap.get(i);
//			if(list == null) list = new ArrayList<String>();
//			list.add(s);
//			aMap.put(i++, list);
//		}
//		Map<Integer, List<String>> kaMap = new TreeMap<Integer,List<String>>();
//		for (String s : kaStatistics.keySet()) {
//			int i = kaStatistics.get(s);
//			List<String> list = kaMap.get(i);
//			if(list == null) list = new ArrayList<String>();
//			list.add(s);
//			kaMap.put(i++, list);
//		}
//		
//		PrintWriter out = new PrintWriter(new FileWriter(A_statistics));
//		List<Integer> keysA = new ArrayList<Integer>(aMap.keySet());
//		for (int i = keysA.size()-1; i>=0; i--) {
//			int count = keysA.get(i);
//			out.write(count+"\n");
//			List<String> tokens = aMap.get(count);
//			for (String t : tokens) {
//				out.write(t+"\n");
//			}
//			out.write("______________________________________"+"\n");
//		}
//		out.close();
//		
//		out = new PrintWriter(new FileWriter(K_statistics));
//		List<Integer> keysK = new ArrayList<Integer>(kMap.keySet());
//		for (int i = keysK.size()-1; i>=0; i--) {
//			int count = keysK.get(i);
//			out.write(count+"\n");
//			List<String> tokens = kMap.get(count);
//			for (String t : tokens) {
//				out.write(t+"\n");
//			}
//			out.write("______________________________________"+"\n");
//		}
//		out.close();
//		
//		out = new PrintWriter(new FileWriter(KA_statistics));
//		List<Integer> keysKA = new ArrayList<Integer>(kaMap.keySet());
//		for (int i = keysKA.size()-1; i>=0; i--) {
//			int count = keysKA.get(i);
//			out.write(count+"\n");
//			List<String> tokens = kaMap.get(count);
//			for (String t : tokens) {
//				out.write(t+"\n");
//			}
//			out.write("______________________________________"+"\n");
//		}
//		out.close();
//	}
//
//}
