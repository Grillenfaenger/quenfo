package de.uni_koeln.spinfo.information_extraction.applications;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import de.uni_koeln.spinfo.information_extraction.workflow.IEJobs_Tools;

public class writeMatchFileForToolsAndBIBBList {
	
	public static File toolsFile = new File("information_extraction/data/tools/tools.txt");
	public static File noToolsFile = new File("information_extraction/data/tools/no_Tools.txt");
	public static File sectorsFile = new File("information_extraction/data/tools/ToolsBySector_List_BIBB.xls");
	public static File outputFile = new File("information_extraction/data/tools/toolsAndBIBBListMatches.txt");
	
	public static void main(String[] args) throws IOException {
		writeMatchFile();
		writeToolCountsFile();
	}

	private static void writeToolCountsFile() {
		IEJobs_Tools jobs = new IEJobs_Tools();
	}

	private static void writeMatchFile() throws IOException {
		IEJobs_Tools jobs = new IEJobs_Tools();
		jobs.readToolLists(toolsFile, noToolsFile);
		Set<String> matches = jobs.matchToolsWithSectors(sectorsFile);

		BufferedWriter  out = new BufferedWriter(new FileWriter(outputFile));
		for (String string : matches) {
			out.write(string+"\n");
		}
		out.flush();
		out.close();
	}
	

	

}
