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

    Copyright (c) 2018,2019 Thomas Eickert
****************************************************************************************/

package gde.junit;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import gde.GDE;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;

/**
 *
 * @author Thomas Eickert (USER)
 */
public class ObjectKeyComplianceTest extends TestSuperClass {
	private final static String	$CLASS_NAME	= ObjectKeyComplianceTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	public void testObjectKeyCandidates() {
		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath(Paths.get("_ET_Exzerpt")).toString());
		// the next lines produce an exception without further consequences
		this.settings.setDataFoldersCsv("/testDir1");
		this.settings.setImportFoldersCsv("/testDir2");

		Set<String> objectKeyCandidates = ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations());
		log.log(Level.FINER, "", objectKeyCandidates);
		assertTrue("is valid object ", objectKeyCandidates.contains("TopSky"));
		assertFalse("is device ", objectKeyCandidates.contains("HoTTAdapter"));
		assertFalse("is _SupplementObjectDirs  ", objectKeyCandidates.contains("_SupplementObjectDirs"));
	}

	public void testObjectKeyNovelties() {
		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath(Paths.get("_ET_Exzerpt")).toString());

		Set<String> objectKeyKeyNovelties = ObjectKeyCompliance.defineObjectKeyNovelties(analyzer.getDeviceConfigurations().getAllConfigurations());
		log.log(Level.FINER, "", objectKeyKeyNovelties);
		assertTrue("is valid object ", objectKeyKeyNovelties.contains("Porsche"));
		assertFalse("is valid object ", objectKeyKeyNovelties.contains("TopSky"));
		assertFalse("is device ", objectKeyKeyNovelties.contains("HoTTAdapter"));
		assertFalse("is _SupplementObjectDirs  ", objectKeyKeyNovelties.contains("_SupplementObjectDirs"));
	}

	public void testRebuildObjectKeyAsync() {
		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath(Paths.get("_ET_Exzerpt")).toString());
		this.settings.setDataFoldersCsv("");
		this.settings.setImportFoldersCsv("");

		Set<String> objectKeyCandidates = ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations());
		objectKeyCandidates.add("TestEt");
		this.settings.setObjectList(objectKeyCandidates.toArray(new String[0]), 0);

		ObjectKeyCompliance.rebuildObjectKeys();

		assertFalse("is deleted object ", Arrays.asList(this.settings.getObjectList()).contains("TestEt"));
		assertTrue("is valid object  ", Arrays.asList(this.settings.getObjectList()).contains("TopSky"));
	}

	public void testRemoveObjectKey() {
		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath(Paths.get("_ET_Exzerpt")).toString());
		String deviceoriented = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		String[] objectKeys = new String[] { deviceoriented, "first", "2nd", "3rd", "" };

		ObjectKeyCompliance.removeObjectKey(objectKeys, 1);
		assertTrue("object key deleted ", Arrays.equals(this.settings.getObjectList(), new String[] { deviceoriented, "2nd", "3rd" }));

		ObjectKeyCompliance.removeObjectKey(objectKeys, 4);
		assertTrue("empty object key deleted ", Arrays.equals(this.settings.getObjectList(), new String[] { deviceoriented, "2nd", "3rd", "first" }));
	}

	public void testRenameObjectKey() {
		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath(Paths.get("_ET_Exzerpt")).toString());
		String deviceoriented = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		String[] objectKeys = new String[] { deviceoriented, "first", "2nd", "3rd" };
		FileUtils.checkDirectoryAndCreate(this.settings.getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + "first");

		ObjectKeyCompliance.renameObjectKey("first", "1st", objectKeys);
		assertTrue("object key renamed ", Arrays.equals(this.settings.getObjectList(), new String[] { deviceoriented, "1st", "2nd", "3rd" }));
		assertFalse("directory deleted ", FileUtils.checkFileExist(this.settings.getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + "first"));
	}

	public void testAddObjectKey() {
		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath(Paths.get("_ET_Exzerpt")).toString());
		String deviceoriented = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		String[] objectKeys = new String[] { deviceoriented, "first", "2nd", "3rd", "" };

		ObjectKeyCompliance.renameObjectKey("", "1st", objectKeys);
		assertTrue("object key added ", Arrays.equals(this.settings.getObjectList(), new String[] { deviceoriented, "1st", "2nd", "3rd", "first" }));
	}

	@Tag("performance")
	@Test
	public void testReadSourcePathsObjectKeysPerformance() {
		for (int i = 0; i < 7; i++) {
			long nanoTime = System.nanoTime();
			Set<String> objectKeyCandidates = ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations());

			System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " ms");
			System.out.println(objectKeyCandidates);
		}
	}

}
