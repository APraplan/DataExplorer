/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2018,2019 Thomas Eickert
****************************************************************************************/
package gde.junit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.config.ExportService;
import gde.histo.base.BasicTestCase;
import gde.tools.ExportServiceBuilder;
import gde.ui.DataExplorer;

public class TestTools extends BasicTestCase {
	static Logger log = Logger.getLogger(TestTools.class.getName());

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.FINER);
		log.setUseParentHandlers(true);
	}

	public void testPrepareManifest() {
		try {
			ExportServiceBuilder.main(new String[] { "../AkkuMaster" });
			System.out.println();
			ExportServiceBuilder.main(new String[] { "../SkyRC", "" });
			System.out.println();
			ExportServiceBuilder.main(new String[] { "../HoTTAdapter", "HoTTAdapterD" });
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void testServices4Projects() {
		log.log(Level.FINE, "start");
		String sourcePath = GDE.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		int index = sourcePath.charAt(2) == ':' ? 1 : 0; // get rid of leading / in windows url file:/C:/dataexplorer/DataExplorer/classes/
		Path basePath = Paths.get(sourcePath.substring(index, sourcePath.lastIndexOf(DataExplorer.class.getSimpleName())));
		ExportServiceBuilder builder = new ExportServiceBuilder(Analyzer.getInstance().getDataAccess());

		String[] projectFolders = basePath.toFile().list((dir, name) -> name.indexOf(GDE.CHAR_DOT) == -1);
		for (String projectName : projectFolders) {
			List<ExportService> exportServices = builder.getServices(basePath, projectName);
			log.log(Level.FINE, String.format("%-12.12s: %s", projectName, exportServices.stream().map(ExportService::getDisplayText).collect(Collectors.joining(", "))));

			Path jarPath = basePath.resolve("build/target/GNULinux_64/DataExplorer/devices").resolve(projectName + ".jar");
			try (JarFile jarFile = new JarFile(jarPath.toString())) {
				log.log(Level.FINE, String.format("%-12.12s: %s", projectName, jarFile.getManifest().getMainAttributes().getValue("Export-Service")));
			} catch (Exception e) {
				// no project jar file exists
			}
		}

	}

	public void testServiceFromDeviceXml() {
		log.log(Level.FINE, "start");
		String sourcePath = GDE.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		int index = sourcePath.charAt(2) == ':' ? 1 : 0; // get rid of leading / in windows url file:/C:/dataexplorer/DataExplorer/classes/
		Path basePath = Paths.get(sourcePath.substring(index, sourcePath.lastIndexOf(DataExplorer.class.getSimpleName())));

		ExportServiceBuilder builder = new ExportServiceBuilder(Analyzer.getInstance().getDataAccess());
		log.log(Level.FINE, "", builder.getService(basePath, "UltramatUDP", "UltraTrioPlus14.xml"));
		log.log(Level.FINE, "", builder.getService(basePath, "HoTTAdapter", "HoTTAdapter2.xml"));
		log.log(Level.FINE, "", builder.getService(basePath, "UltramatUDP", "UltraTrioPlus14.xml"));
		log.log(Level.FINE, "", builder.getService(basePath, "HoTTAdapter", "HoTTAdapter2.xml"));
	}
}
