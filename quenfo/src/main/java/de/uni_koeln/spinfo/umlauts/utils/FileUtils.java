package de.uni_koeln.spinfo.umlauts.utils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

public class FileUtils {

	private FileUtils() {
		throw new AssertionError();
	}

	public static String outputPath = "../ang.data/output/";
	public static String inputPath = "../ang.data/input/";

	public static List<String> fileToList(String filePath) throws IOException {

		ArrayList<String> normalized = new ArrayList<String>();

		ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

		for (String s : lines) {

			s = Normalizer.normalize(s, Normalizer.Form.NFC);
			normalized.add(s);
		}
		return normalized;
	}
	
	public static List<String> fileToList(String filePath, String commentMarkup) throws IOException {

		ArrayList<String> normalized = new ArrayList<String>();

		ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

		for (String s : lines) {
			if(s.trim().startsWith(commentMarkup)) continue;
			
			s = Normalizer.normalize(s, Normalizer.Form.NFC);
			normalized.add(s);
		}
		return normalized;
	}
	
	public static List<List<String>> fileToListOfLists(String filePath) throws IOException {

		List<List<String>> toReturn = new ArrayList<List<String>>();
		ArrayList<String> normalized = new ArrayList<String>();

		ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

		for (String s : lines) {

			s = Normalizer.normalize(s, Normalizer.Form.NFC);
			normalized.add(s);
		}
		for (String string : normalized) {
			string = string.substring(1, string.length()-1);
			toReturn.add(Arrays.asList(string.split(", ")));
		}			
		return toReturn;
	}
	
	public static HashMap<String, String> fileToMap(String filePath) throws IOException {
		
		HashMap<String, String> map = new HashMap<String, String>();

		ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

		for (String s : lines) {
			s = Normalizer.normalize(s, Normalizer.Form.NFC);
			String[] split = s.split(" : ");
			map.put(split[0], split[1]);	
		}
		return map;
	}
	
	public static HashMap<String, HashSet<String>> fileToAmbiguities(String filePath) throws IOException{
		HashMap<String, HashSet<String>> ambiguities = new HashMap<String, HashSet<String>>();
		HashMap<String, String> map = fileToMap(filePath);
		
		for (Entry<String,String> entry : map.entrySet()) {
			String listString = entry.getValue();
			listString = listString.substring(1, listString.length()-1);
			HashSet<String> ambige = new HashSet<String>(Arrays.asList(listString.split(", ")));
			ambiguities.put(entry.getKey(), ambige);
			
		}
		return ambiguities;
		
	}

	public static <T> void serializeList(List<T> list, String fileName) throws IOException {

		ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outputPath + fileName));

		outputStream.writeObject(list);

		outputStream.close();

	}

	public static <T> void serializeList(List<T> list, String destPath, String fileName) throws IOException {

		fileName = fileName + ".ser";

		File file = new File(destPath + fileName);

		ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));

		outputStream.writeObject(list);

		outputStream.close();

	}

	public static <K, V> File printMap(Map<K, V> map, String destPath, String fileName) throws IOException {

		File file = new File(destPath + fileName /*+ getISO8601StringForCurrentDate() */+ ".txt");
		System.out.println(file.getAbsolutePath());
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

		for (Map.Entry<K, V> entry : map.entrySet()) {
			out.append(entry.getKey() + " : " + entry.getValue());
			out.append("\n");
		}

		out.flush();
		out.close();

		return file;
	}

	public static <T> File printSet(Set<T> set, String destPath, String filename) throws IOException {

		File file = new File(destPath + filename + ".txt");

		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

		for (Object o : set) {
			writer.append(o + "\n");
		}

		writer.flush();
		writer.close();

		return file;
	}

	public static <T> File listToTXT(List<T> list, String destPath, String filename) throws IOException {

		File file = new File(destPath + filename + ".txt");

		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

		for (Object o : list) {
			writer.append(o + "\n");
		}

		writer.flush();
		writer.close();

		return file;
	}
	
	public static <T> File printListOfList(List<List<T>> listOfLists, String destPath, String filename) throws IOException {

		File file = new File(destPath + filename + ".txt");

		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
		
		for (List<T> list : listOfLists) {
			writer.append(list + "\n");
		}

		writer.flush();
		writer.close();

		return file;
	}

	public static <T> File printList(List<T> list, String destPath, String filename, String fileFormat)
			throws IOException {

		File file = new File(destPath + filename + fileFormat);

		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

		for (Object o : list) {
			writer.append(o + "\n");
		}

		writer.flush();
		writer.close();

		return file;
	}
	
	public static File printString(String string, String destPath, String filename, String fileFormat)
			throws IOException {

		File file = new File(destPath + filename + fileFormat);

		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
		writer.append(string);

		writer.flush();
		writer.close();

		return file;
	}

	public static String getISO8601StringForCurrentDate() {
		Date now = new Date();
		return getISO8601StringForDate(now);
	}

	private static String getISO8601StringForDate(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMANY);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		return dateFormat.format(date);
	}
	
	public static List<String> snowballStopwordReader(String filePath) throws IOException{
		List<String> stopwords = new ArrayList<String>();
		List<String> lines = fileToList(filePath);
		for(String line : lines){
			if(line.contains("|")){
				String[] splits = line.split("\\|");
				line = splits[0];
			}
			line = line.trim();
			
			if (!line.isEmpty()){
				stopwords.add(line);
			}
		}
		return stopwords;
	}
	
	public static void processFamilyNamesFile(String inputFilePath) throws IOException{
		List<String> lines = fileToList(inputFilePath);
		Set<String> familynames = new HashSet<String>();
		
		for (String string : lines) {
			String[] splits = string.split("\\t");
			familynames.addAll(Arrays.asList(splits));
		}
		
		List<String> familynamesList = new ArrayList<String>();
		familynamesList.addAll(familynames);
		Collator coll = Collator.getInstance(Locale.GERMAN);
		Collections.sort(familynamesList, coll);
		System.out.println(familynamesList);
		System.out.println(familynamesList.size());
		
		printList(familynamesList, "output//stats//", "familynames", ".txt");
	}

	

}