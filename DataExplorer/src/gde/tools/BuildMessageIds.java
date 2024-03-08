/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * class to build message IDs from messages.properties to MessageIds.java
 * @author WInfried Brügmann
 */
public class BuildMessageIds {

	final static String fileHeader = "/************************************************************************************** \n"
		+ "  	This file is part of GNU DataExplorer.\n"
		+ "\n"
		+ "		GNU DataExplorer is free software: you can redistribute it and/or modify\n"
		+ "    it under the terms of the GNU General Public License as published by\n"
		+ "    the Free Software Foundation, either version 3 of the License, or\n"
		+ "    (at your option) any later version.\n"
		+ "\n"
		+ "    DataExplorer is distributed in the hope that it will be useful,\n"
		+ "    but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
		+ "    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
		+ "    GNU General Public License for more details.\n"
		+ "\n"
		+ "    You should have received a copy of the GNU General Public License\n"
		+ "    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.\n"
    + "\n"
    + "Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann \n"
		+ "****************************************************************************************/\n";
		
	final static String classHeader = "/** \n"
		+ " * @author Winfried Brügmann \n"
		+ " * Do not edit, MessageIds are generated from messages.properties \n"
		+ " */ \n"
		+ "public class MessageIds { \n"
		+ "\n";
	final static String fileClosing = "\n }\n";
	final static String lineHeader = "\tpublic final static String\t";
	final static String lineCenter = " = \"";
	final static String lineClosing = "\";\n";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String packageName = "package " + args[0] + ";\n\n"; // gde.messages
		String inFilePath = args[1]; //"src/gde/messages/messages.properties";
		String outFilePath = args[2]; //"src/gde/messages/MessageIds.java";
		BufferedReader reader; // to read the data
		BufferedWriter writer; // to write the data
		String line;
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFilePath), "ISO-8859-1"));
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFilePath), "UTF-8"));
		
		writer.write(fileHeader);
		writer.write(packageName);
		writer.write(classHeader);
		while ((line = reader.readLine()) != null){
			//System.out.println(line);
			if (line.trim().startsWith("#") || line.trim().length() <=1) 
				continue;
			
			System.out.println(line);
			
			String[] sections = line.trim().split("=");
			writer.write(lineHeader);
			writer.write(sections[0]);
			writer.write(lineCenter);
			writer.write(sections[0]);
			writer.write(lineClosing);
		}
		writer.write(fileClosing);
		
		writer.close();
		reader.close();
	}

}
