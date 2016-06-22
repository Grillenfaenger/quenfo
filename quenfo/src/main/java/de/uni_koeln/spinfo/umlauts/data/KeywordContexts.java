package de.uni_koeln.spinfo.umlauts.data;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


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
	
	public List<List<String>> getContext(String keyword){
		return keywordContextsMap.get(keyword);
	}
	
	public File printKeywordContexts(String destPath, String fileName) throws IOException {

		File file = new File(destPath + fileName /*+ getISO8601StringForCurrentDate() */+ ".txt");
		System.out.println(file.getAbsolutePath());
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

		for (Entry<String, List<List<String>>> entry: keywordContextsMap.entrySet()) {
			out.append("\n===========\n"+entry.getKey()+":\n"+entry.getValue().size()+" contexts=\n");
			for(List<String> context : entry.getValue()){
				out.append(context.toString()+"\n");
			}
			out.append("============\n");
		}

		out.flush();
		out.close();

		return file;
	}
	
	
	
	

}
