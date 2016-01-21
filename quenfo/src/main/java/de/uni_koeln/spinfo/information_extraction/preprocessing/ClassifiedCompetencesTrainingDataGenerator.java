package de.uni_koeln.spinfo.information_extraction.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.information_extraction.data.Competence;
import de.uni_koeln.spinfo.information_extraction.data.TYPE;

public class ClassifiedCompetencesTrainingDataGenerator {

	private File classifiedCompetencesFile;
	//private Map<String,List<Competence>> classifiedCompetences;
	private List<Competence> classifiedCompetences;
	private int deletions;
	private int size;

	public ClassifiedCompetencesTrainingDataGenerator(File classifiedCompetencesFile) throws IOException {
		this.classifiedCompetencesFile = classifiedCompetencesFile;
		if(!classifiedCompetencesFile.exists()){
			classifiedCompetencesFile.createNewFile();
		}
		//this.classifiedCompetences = new HashMap<String,List<Competence>>();
		this.classifiedCompetences = new ArrayList<Competence>();
	}

	public List<Competence> getclassifedCompetences() throws IOException {
		if(classifiedCompetences.isEmpty()){
			BufferedReader in = new BufferedReader(new FileReader(classifiedCompetencesFile));
			String line = in.readLine();
			
			if(line != null && line.startsWith("deletions:")){
				deletions = Integer.parseInt(line.split(":")[1].trim());
				line = in.readLine();
			}	
			while(line != null){
				
				String[] split = line.split("\t");
				String[] jobAdIDs = split[5].split("-");
				int jobAdID = Integer.parseInt(jobAdIDs[0].trim());
				int secondJobAdID = Integer.parseInt(jobAdIDs[1]);
				Competence c = new Competence(jobAdID, secondJobAdID);
				c.setCompetence(split[1]);
				c.setQuality(split[2]);
				c.setImportance(split[3]);
				String type = split[4];
				if(type.equals("A")){
					c.setType(TYPE.A);
				}
				if(type.equals("K")){
					c.setType(TYPE.K);
				}
				if(type.equals("AK")||type.equals("KA")){
					c.setType(TYPE.AK);
				}
//				List<Competence> list = classifiedCompetences.get(type);
//				
//				if(list == null){
//					list = new ArrayList<Competence>();
//				}
//				list.add(c);
//				classifiedCompetences.put(type, list);
				classifiedCompetences.add(c);
				line = in.readLine();
				size++;
			}
			in.close();
		}
		return classifiedCompetences;
	}

	public void annotate(List<Competence> comps) throws IOException {
		getclassifedCompetences();
		int start = size+deletions;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		for (int j = start; j < comps.size(); j++) {
			Competence competence = comps.get(j);
			System.out.println(competence);
			String answer = in.readLine();
			if (answer.equals("a")) {
//				List<Competence> a = classifiedCompetences.get("A");
//				if(a== null){
//					a = new ArrayList<Competence>();
//				}
//				a.add(competence);
//				classifiedCompetences.put("A", a);
				competence.setType(TYPE.A);
				classifiedCompetences.add(competence);
				continue;
			}
			if (answer.equals("k")) {
//				List<Competence> k = classifiedCompetences.get("K");
//				if(k== null){
//					k = new ArrayList<Competence>();
//				}
//				k.add(competence);
//				classifiedCompetences.put("K", k);
				competence.setType(TYPE.K);
				classifiedCompetences.add(competence);
				continue;
			}
			if (answer.equals("ka") || answer.equals("ak")) {
//				List<Competence> ak = classifiedCompetences.get("AK");
//				if(ak== null){
//					ak = new ArrayList<Competence>();
//				}
//				ak.add(competence);
//				classifiedCompetences.put("AK", ak);
				competence.setType(TYPE.AK);
				classifiedCompetences.add(competence);
				continue;
			}
			if(answer.equals("d")){
				deletions++;
				continue;
			}
			if(answer.equals("stop")){
				break;
			}
			else {
				System.out.println("Invalid answer. Try again");
				j--;
				continue;
			}
		}
		writeClassifiedCompetencesFile();
	}

	private void writeClassifiedCompetencesFile() throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(classifiedCompetencesFile));
		out.write("deletions: " + deletions+"\n");
//		for (String type : classifiedCompetences.keySet()) {
//			List<Competence> comps = classifiedCompetences.get(type);
//			for (Competence c : comps) {
//				out.write(c+"\t"+type+"\t"+c.getJobAdID()+"\n");
//			}
//		}
		for (Competence comp : classifiedCompetences) {
			out.write(comp+"\t"+comp.getType().toString()+"\t"+comp.getJobAdID()+"-"+comp.getSecondJobAdID()+"\n");
		}
		out.flush();out.close();
	}
	
	
}
