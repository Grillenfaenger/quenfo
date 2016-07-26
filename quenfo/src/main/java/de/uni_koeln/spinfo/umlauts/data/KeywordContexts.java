package de.uni_koeln.spinfo.umlauts.data;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;


public class KeywordContexts {
	
	private Map<String, List<List<String>>> keywordContextsMap;
	

	public KeywordContexts() {
		this.keywordContextsMap = new TreeMap<String, List<List<String>>>(Collator.getInstance(Locale.GERMAN));
	}

	public void addContexts(String keyword, List<List<String>> newContexts){
		if(keywordContextsMap.containsKey(keyword)){
			List<List<String>> contexts = keywordContextsMap.get(keyword);
			contexts.addAll(newContexts);
			keywordContextsMap.put(keyword, contexts);
		} else {
			keywordContextsMap.put(keyword, newContexts);
		}
		
	}
	
	public void addContext(String keyword, List<String> context) {
		if(keywordContextsMap.containsKey(keyword)){
			List<List<String>> contexts = keywordContextsMap.get(keyword);
			contexts.add(context);
			keywordContextsMap.put(keyword, contexts);
		} else {
			List<List<String>> contexts = new ArrayList<List<String>>();
			contexts.add(context);
			keywordContextsMap.put(keyword, contexts);
		}
		
	}
	
	public List<List<String>> getContext(String keyword){
		return keywordContextsMap.get(keyword);
	}
	
	public KeywordContexts loadKeywordContextsFromFile(String path) throws IOException{
		KeywordContexts keywordContexts = new KeywordContexts();
		
		String file = FileUtils.readFileToString(new File(path), "UTF-8");
		
		String[] keywords = file.split("\\$;\\n");
		
		for (int i = 0; i < keywords.length; i++) {
			System.out.println(keywords[i]+"\n");
			String[] contexts = keywords[i].split("\\n");
			System.out.println(contexts[0]);
			
			List<List<String>> newContexts = new ArrayList<List<String>>();
			for (int j = 1; j < contexts.length; j++) {
				System.out.println("contexts["+j+"]= "+ contexts[j]);
				String substring = contexts[j].substring(1,contexts[j].length()-1);
				System.out.println(substring);
				String[] split = substring.split(", ");
				List<String> context = Arrays.asList(split);
				System.out.println(Arrays.asList(split));
				newContexts.add(context);
			}
			keywordContexts.addContexts(contexts[0], newContexts);	
		}
		return keywordContexts;
	}
	
	public File printKeywordContexts(String destPath, String fileName) throws IOException {

		File file = new File(destPath + fileName /*+ getISO8601StringForCurrentDate() */+ ".txt");
		System.out.println(file.getAbsolutePath());
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

		for (Entry<String, List<List<String>>> entry : keywordContextsMap.entrySet()) {
			out.append(entry.getKey());
			System.out.println("Key: " + entry.getKey());
			out.append("\n");
			
			for(List<String> context : entry.getValue()){
				out.append(context.toString());
				System.out.println("Kontext: " + context.toString());
				out.append("\n");
			}
			out.append("$;\n");
		}

		out.flush();
		out.close();

		return file;
	}

	
	

	
	
	
	

}
