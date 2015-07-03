package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;

public class ReplaceLatLon {

	public static void main(String[] args) {
		String csvFilePath = args[0];
		String latFilePath = args[1];
		String lonFilePath = args[2];
		String newCSVFilePath = args[3];
		
		try {
			List<String> csvLines = Files.readAllLines(Paths.get(csvFilePath), Charset.defaultCharset());
			List<String> latLines = Files.readAllLines(Paths.get(latFilePath), Charset.defaultCharset());
			List<String> lonLines = Files.readAllLines(Paths.get(lonFilePath), Charset.defaultCharset());
			List<String> newCSVLines = new ArrayList<String>();
			
			for (int i = 0; i < csvLines.size(); i++) {
				String newLine = "";
				String csvLine = csvLines.get(i);
				StringTokenizer st = new StringTokenizer(csvLine, ",");
				newLine += st.nextToken() + "," + st.nextToken() + "," + latLines.get(i) + "," + lonLines.get(i);
				st.nextToken();
				st.nextToken();
				while (st.hasMoreTokens()) {
					newLine += ","+st.nextToken();
				}
				newCSVLines.add(newLine);				
			}
			
			File file = new File(newCSVFilePath);
			OutputStream outStream = new FileOutputStream(file);			
			IOUtils.writeLines(newCSVLines, "\n", outStream);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
