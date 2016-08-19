package de.uni_koeln.spinfo.umlauts.dewag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Splitter for large sDewac files.
 * Is able to split a file into files with a specified number of sentences 
 * or into files with sentences containing a specified string of interest.
 * 
 * @author jhermes
 *
 */
public class DewacSplitter {
	
	//private File[] inputFiles; 
	private File destinationDir;
		
	
	/**
	 * Initializes a new DewacSplitter. 
	 * @param destination Destination folder for the result files.
	 */
	public DewacSplitter(String destination){
		this.destinationDir = new File(destination);
		if(!destinationDir.exists())
			destinationDir.mkdirs();
	}
	
	
	/**
	 * Searches the input files for the specified strings of interest 
	 * and generates an output file for each string containing the specified 
	 * maximum count of sentences with this string
	 * @param inputFile The input file
	 * @param soi strings of interest  
	 * @param maxSentencesPerFile Max count of sentences per output file
	 * @throws IOException 
	 */
	public void createFilePerVerb(File inputFile, StringsOfInterest soi, int maxSentencesPerFile) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		
		List<PrintWriter> outs = new ArrayList<PrintWriter>();
		for (String string : soi.getStringsOfInterest()) {
			outs.add(new PrintWriter(new FileWriter(new File(destinationDir + "//"+ string + ".txt"))));
		}
		
		StringBuffer toWrite = new StringBuffer();
		
		int ofInterest = -1;
	
		List<Integer> found = new ArrayList<Integer>(outs.size()); 
		for (int i=0; i<outs.size(); i++) {
			found.add(0);
		}
		int notOfInterest = 0;
		while(nextLine!= null){
			if(nextLine.startsWith("<")){
				if(nextLine.trim().equals("<s>")){					
					toWrite.append(nextLine + '\n');					
				}
				else{
					if(nextLine.trim().equals("</s>")){
						toWrite.append(nextLine + "\n\n");
						if(ofInterest>=0){
							if(found.get(ofInterest) <= maxSentencesPerFile){
								outs.get(ofInterest).println(toWrite.toString());
								found.set(ofInterest, found.get(ofInterest)+1);
								System.out.println(notOfInterest + " " + ofInterest);
							}
						}
						else{
							notOfInterest++;
						}
						
						toWrite = new StringBuffer();
						ofInterest = -1;											
					}
				}				
			}
			else{
				String[] splits = nextLine.split("\\s");
				String lemma = splits[2].trim();
				if(lemma.equals("<unknown>")){
					lemma = splits[0].trim();
				}
				if(soi.isOfInterest(lemma)){
					ofInterest = soi.getId(lemma);
				}
				toWrite.append(nextLine + '\n');
			}
			
//			if(notOfInterest>20000000){
//				break;
//			}
			//System.out.println(nextLine);
			nextLine = in.readLine();		
			
		}	
		for (PrintWriter pw : outs) {
			pw.close();
		}
		
		System.out.println("Sentences of interest: " + found);
		System.out.println("Verbs: " + soi.getStringsOfInterest()) ;
		System.out.println("Sentences not of interest: " + notOfInterest);
		System.out.println();
		soi.printMap();
	}
	
	/**
	 * Searches the input files for the specified strings of interest 
	 * and generates an output file for all string containing the specified 
	 * maximum count of sentences with this string
	 * @param inputFile The input file
	 * @param soi strings of interest  
	 * @param maxSentencesPerFile Max count of sentences per output file
	 * @throws IOException 
	 */
	public void sentencesOfInterestToFile(File inputFile, StringsOfInterest soi, int maxSentences) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		StringBuffer toWrite = new StringBuffer();
		File outputFile = new File(destinationDir + "//byWord//ofInterest.txt");
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		boolean ofInterest = false;
		int found = 0;
		int notOfInterest = 0;
		while(nextLine!= null){
//			System.out.println(inputFile.getAbsolutePath());
//			System.out.println(nextLine);
			if(nextLine.startsWith("<")){
				if(nextLine.trim().equals("<s>")){					
					toWrite.append(nextLine + '\n');					
				}
				else{
					if(nextLine.trim().equals("</s>")){
						toWrite.append(nextLine + "\n\n");
						if(ofInterest){
							out.println(toWrite.toString());
							found++;
						}
						else{
							notOfInterest++;
						}
						
						toWrite = new StringBuffer();
						ofInterest = false;											
					}
				}				
			}	
			else if(nextLine.trim().equals("")){
				nextLine = in.readLine();
				continue;
			}
			else{
				String[] splits = nextLine.split("\\s");
				String lemma = splits[2].trim();
				if(soi.isOfInterest(lemma)){
					ofInterest = true;
				}
				toWrite.append(nextLine + '\n');
			}
			//System.out.println(nextLine);
			nextLine = in.readLine();		
//			if(found>maxSentences){
//				break;
//			}
			if(found%10000 == 0){
				System.out.println(found);
			}
		}	
		System.out.println("Sentences of interest: " + found);
		System.out.println("Sentences not of interest: " + notOfInterest);
		System.out.println();
		soi.printMap();
		
		in.close();
		out.close();
		
		if(found == 0){
			Files.deleteIfExists(outputFile.toPath());
		}
	}
	
	/**
	 * Searches the input files for the specified strings of interest 
	 * and generates an output file for all string containing the specified 
	 * maximum count of sentences with this string
	 * @param inputFile The input file
	 * @param soi strings of interest  
	 * @param maxSentencesPerFile Max count of sentences per output file
	 * @throws IOException 
	 */
	public void sentencesOfInterestToFile2(File inputFile, StringsOfInterest soi, int maxSentences) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		StringBuffer toWrite = new StringBuffer();
		File outputFile = new File(destinationDir + "//byWord//ofInterest.txt");
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		boolean ofInterest = false;
		int found = 0;
		int notOfInterest = 0;
		while(nextLine!= null){
//			System.out.println(inputFile.getAbsolutePath());
//			System.out.println(nextLine);
			if(nextLine.startsWith("<")){
				if(nextLine.trim().equals("<s>")){					
					toWrite.append(nextLine + '\n');					
				}
				else{
					if(nextLine.trim().equals("</s>")){
						toWrite.append(nextLine + "\n\n");
						if(ofInterest){
							out.println(toWrite.toString());
							found++;
						}
						else{
							notOfInterest++;
						}
						
						toWrite = new StringBuffer();
						ofInterest = false;											
					}
				}				
			}	
			else if(nextLine.trim().equals("")){
				nextLine = in.readLine();
				continue;
			}
			else{
				String[] splits = nextLine.split("\\s");
				String lemma = splits[2].trim();
				if(soi.isOfInterest(lemma)){
					ofInterest = true;
					if(soi.getStringsFoundOf(lemma)<maxSentences){
						soi.removeStringOfInterest(lemma);
					}
				}
				toWrite.append(nextLine + '\n');
			}
			//System.out.println(nextLine);
			nextLine = in.readLine();		
//			if(found>maxSentences){
//				break;
//			}
			if(found%100 == 0){
				System.out.println(found);
				System.out.println(soi.getStringsOfInterest().size());
			}
		}	
		System.out.println("Sentences of interest: " + found);
		System.out.println("Sentences not of interest: " + notOfInterest);
		System.out.println();
		soi.printMap();
		
		
		in.close();
		out.close();
		
		if(found == 0){
			Files.deleteIfExists(outputFile.toPath());
		}
	}
	
	public List<List<String>> getTokenizedSentences(File inputFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		
		List<List<String>> toReturn = new ArrayList<List<String>>();
		List<String> sentence = null;
		
		System.out.println(inputFile.getName());
		int i = 0;
		while(nextLine!= null){
			if(nextLine.startsWith("<")){
				if(nextLine.trim().equals("<s>")){					
					sentence = new ArrayList<String>();	
				}
				else if(nextLine.trim().equals("</s>")){
					toReturn.add(sentence);
					nextLine = in.readLine();
					continue;
				}				
			} 
			else if(nextLine.trim().equals("")){
				nextLine = in.readLine();
				continue;
			}
			else{
				String[] splits = nextLine.split("\\s");
				String token = splits[0].trim();
				sentence.add(token);
			}
			nextLine = in.readLine();		
		}
		
		in.close();
		return toReturn;
	}
	
	public void sentencesWithUmlautsToFile(File inputFile, int maxSentences, int fileNr) throws IOException{
		String regexPattern = ".*([ÄäÖöÜü]).*";
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		StringBuffer toWrite = new StringBuffer();
		PrintWriter out = new PrintWriter(new FileWriter(new File(destinationDir + "//ofInterest"+fileNr+".txt")));
		boolean ofInterest = false;
		int found = 0;
		int notOfInterest = 0;
		while(nextLine!= null){
			if(nextLine.startsWith("<")){
				if(nextLine.trim().equals("<s>")){					
					toWrite.append(nextLine + '\n');					
				}
				else{
					if(nextLine.trim().equals("</s>")){
						toWrite.append(nextLine + "\n\n");
						if(ofInterest){
							out.println(toWrite.toString());
							found++;
						}
						else{
							notOfInterest++;
						}
						
						toWrite = new StringBuffer();
						ofInterest = false;											
					}
				}				
			}
			else{
				String[] splits = nextLine.split("\\s");
				String token = splits[0].trim();
				if(token.matches(regexPattern)){
					ofInterest = true;
				}
				toWrite.append(nextLine + '\n');
			}
			//System.out.println(nextLine);
			nextLine = in.readLine();		
			if(found>maxSentences){
				break;
			}
		}	
		System.out.println("Sentences of interest: " + found);
		System.out.println("Sentences not of interest: " + notOfInterest);
		System.out.println();
		
		in.close();
		out.close();

	}
	
	/** 
	 * Counts the sentences lengthes of the input file
	 * @param inputFile File to analyze
	 * @return A map with sentences lenghtes as keys, their count as values
	 * @throws IOException
	 */
	public Map<Integer, Integer> analyseSentencesLengths(File inputFile) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		int sentenceCounter = 0;
		Map<Integer, Integer> lengthes = new HashMap<Integer, Integer>();
		int length = 0;
		
		while(nextLine!= null){
			
			if(nextLine.startsWith("<")){				
				if(nextLine.trim().equals("<s>")){					
					System.out.println(sentenceCounter + " length =" + length);
					sentenceCounter++;
					length=0;
				}
				else{
					if(nextLine.trim().equals("</s>")){
						Integer put = lengthes.put(length, 1);
						if(put!=null){
							lengthes.put(length, (Integer)(1+put));
						}				
					}
					
				}	
				
			}
			else{
				length++;
			}
			nextLine = in.readLine();
					
		}		
		in.close();
		return lengthes;
	}
	
	/** Filters the sentences of the input file by the specified lenghtes. 
	 * Generates an output file with max count of sentences with the specified lengthes.
	 * @param inputFile file to analyze
	 * @param lengthes Sentence lengthes of interest
	 * @param max Max sentences to export
	 * @throws IOException
	 */
	public void getSentencesWithMaxLength(File inputFile, Set<Integer> lengthes, int max) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		int sentenceCounter = 0;
		//Map<Integer, Integer> lengthes = new HashMap<Integer, Integer>();
		PrintWriter out = new PrintWriter(new FileWriter(new File(destinationDir + "//shortTexts.txt")));
		StringBuffer toWrite = new StringBuffer();
		int length = 0;
		while(nextLine!= null){
			
			if(nextLine.startsWith("<")){				
				if(nextLine.trim().equals("<s>")){					
					//System.out.println(sentenceCounter + " length =" + length);
					sentenceCounter++;
					length=0;
				}
				else{
					if(nextLine.trim().equals("</s>")){
						if(lengthes.contains(length)){
							System.out.println(length);
							System.out.println(toWrite);
							out.println(toWrite);
							
							if(max==0){
								break;
							}
						}	
						toWrite = new StringBuffer();
						max--;
						
					}					
				}	
				
			}
			else{
				length++;
				String[] split = nextLine.split("\t");
				String word = split[0];
//				for(int i=0; i<word.length(); i++){
//					if(Characterword[i])
//				}
				toWrite.append(word + " ");
				
			}
			nextLine = in.readLine();
					
		}		
		in.close();
		out.flush();
		out.close();
		
	}
	
	/**
	 * Splits the specified file into nFiles subfiles containing sentencesPerFile sentences
	 * @param inputFile The file to split
	 * @param sentencesPerFile The count of sentences per result file
	 * @param nFiles The count of result files
	 * @throws IOException 
	 */
	public void splitFile(File inputFile, int sentencesPerFile, int nFiles) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String nextLine = in.readLine();
		
		StringBuffer toWrite = new StringBuffer();
		int sentenceCounter = 0;
		int fileCounter = nFiles;
		PrintWriter out = null;
		
		while(nextLine!= null){
			if(nextLine.startsWith("<")){
				if(nextLine.trim().equals("<s>")){
					sentenceCounter++;;
					toWrite.append(nextLine + '\n');
					//System.out.println(sentenceCounter);
				}
				else{
					if(nextLine.trim().equals("</s>")){
						toWrite.append(nextLine + "\n\n");
						if(sentenceCounter>=sentencesPerFile){
							//toWrite.append(nextLine + "\n\n");
							System.out.println("File: " + fileCounter);
							System.out.println("sentences: " + sentenceCounter);
							System.out.println("Buffer: " + toWrite.length());
							System.out.println();
							File destFile = new File(destinationDir +"//"+ inputFile.getName() + "_" + fileCounter++);
							out = new PrintWriter(new FileWriter(destFile));
							out.println(toWrite.toString());
							out.close();
							toWrite = new StringBuffer();
							sentenceCounter = 0;
							if(fileCounter>(nFiles*2)){
								break;
							}
						}						
					}
				}				
			}
			else{
				toWrite.append(nextLine + '\n');
			}
			nextLine = in.readLine();			
		}		
		in.close();
	}


	/**
	 * Creates a subsection of each file in the input folder 
	 * @param inputFolder
	 * @param start
	 * @param end
	 * @throws IOException
	 */
	public void splitFilesinFolder(String inputFolder, int start, int end) throws IOException {
		File folder = new File(inputFolder);
		File[] listFiles = folder.listFiles();
		for (File inputFile : listFiles) {
			BufferedReader in = new BufferedReader(new FileReader(inputFile));
			String nextLine = in.readLine();
			
			StringBuffer toWrite = new StringBuffer();
			int sentenceCounter = 0;
			PrintWriter out;
			
			while(nextLine!= null){
				if(nextLine.startsWith("<")){
					if(nextLine.trim().equals("<s>")){
						sentenceCounter++;
						if(sentenceCounter>start)
							toWrite.append(nextLine + '\n');
						//System.out.println(sentenceCounter);
					}
					else{
						if(sentenceCounter>start){
							if(nextLine.trim().equals("</s>")){
								toWrite.append(nextLine + "\n\n");
								if(sentenceCounter>=end){
									//toWrite.append(nextLine + "\n\n");
									//System.out.println("File: " + fileCounter);
									System.out.println("sentences: " + sentenceCounter);
									System.out.println("Buffer: " + toWrite.length());
									System.out.println();
									File destFile = new File(destinationDir +"/"+ inputFile.getName());
									out = new PrintWriter(new FileWriter(destFile));
									out.println(toWrite.toString());
									out.close();
									toWrite = new StringBuffer();
									sentenceCounter = 0;
									
								}						
							}
						}	
					}
				}
				else{
					if(sentenceCounter>start)
						toWrite.append(nextLine + '\n');
				}
				nextLine = in.readLine();			
			}		
			in.close();	
		}			
	}
}
