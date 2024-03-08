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
package gde.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.exception.NotSupportedFileFormatException;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * This thread implementation goes through all sub folders of the data location and creates links to object related files
 * @author Winfried Brügmann
 */
public class ObjectKeyScanner extends Thread {
	final private static Logger		log									= Logger.getLogger(ObjectKeyScanner.class.getName());

	final private DataExplorer		application					= DataExplorer.getInstance();
	final private Settings				settings						= Settings.getInstance();
	String												objectKey						= GDE.STRING_EMPTY;

	final boolean									addToExistentKeys;

	private List<String>					obsoleteObjectKeys	= new ArrayList<>();

	/**
	 * constructor to create a object key scanner,
	 * starting this as thread all the sub files of the given data path are scanned for object key references
	 * and a file link will be created in a directory named with the object key.
	 * If newAddToExistentKeys is true: Provide obsolete object keys, i.e. without object directory or with empty directory.
	 * @param newAddToExistentKeys false will build a new object key list from scratch on without caring about any existing object keys
	 */
	public ObjectKeyScanner(boolean newAddToExistentKeys) {
		super("objectKeyScanner");
		this.addToExistentKeys = newAddToExistentKeys;
	}

	/**
	 * constructor to create a object key scanner,
	 * starting this as thread all the sub files of the given data path are scanned for object key references
	 * and a file link will be created in a directory named with the object key
	 * @param newObjectKey the object key to be used for scanning existing files
	 */
	public ObjectKeyScanner(String newObjectKey) {
		super("objectKeyScanner");
		this.objectKey = newObjectKey;
		this.addToExistentKeys = false;
	}

	@Override
	public void run() {
		try {
			String objectKeyDirPath = this.settings.getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + this.objectKey;

			if (this.objectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH) { // use exact defined object key
				FileUtils.checkDirectoryAndCreate(objectKeyDirPath);

				//scan all data files for object key
				List<File> files = FileUtils.getFileListing(new File(this.settings.getDataFilePath()), 1);
				final int progressDistance = 50;
				double progressStep = (99. - GDE.getUiNotification().getProgressPercentage()) / (files.size() + 1.) * progressDistance;
				int i = 0;
				for (File file : files) {
					try {
						String actualFilePath = file.getAbsolutePath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
						if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD) && actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) {
							log.log(Level.FINER, "working with " + file.getName()); //$NON-NLS-1$
							if (this.objectKey.equals(OsdReaderWriter.getHeader(file.getCanonicalPath()).get(GDE.OBJECT_KEY))) {
								log.log(Level.FINER, "found file with given object key " + file.getName()); //$NON-NLS-1$
								String newLinkFilePath = objectKeyDirPath + GDE.STRING_FILE_SEPARATOR_UNIX + file.getName();
								if (!new File(newLinkFilePath).exists()) {
									OperatingSystemHelper.createFileLink(file.getCanonicalPath(), newLinkFilePath);
								}
							}
						}
						if (i % progressDistance == 0) GDE.getUiNotification().setProgress((int) (i * progressStep));
						i++;
					}
					catch (IOException e) {
						log.log(Level.WARNING, file.getAbsolutePath(), e);
					}
					catch (NotSupportedFileFormatException e) {
						log.log(Level.WARNING, e.getLocalizedMessage(), e);
					}
					catch (Throwable t) {
						log.log(Level.WARNING, t.getLocalizedMessage(), t);
					}
				}
				GDE.getUiNotification().setProgress(100);
			}
			else { // search for all available keys
				log.log(Level.WARNING, "object key not set, actual object key = \"" + this.objectKey + "\" !"); //$NON-NLS-1$ //$NON-NLS-2$
				final int progressPercentageLimit = 80;

				final File rootDirectory = new File(this.settings.getDataFilePath());
				log.log(Level.FINE, "this.settings.getDataFilePath() = " + rootDirectory.toString());
				Vector<String> objectKeys = new Vector<>();
				HashMap<String, Vector<File>> objectFilesMap = new HashMap<String, Vector<File>>();
				{
					int fileCounter = 0;
					List<File> files = FileUtils.getFileListing(rootDirectory, 1);
					final int progressDistance = 50;
					double progressStep = (99. - GDE.getUiNotification().getProgressPercentage()) / (files.size() + 1.) * progressDistance;
					int i = 0;
					for (File file : files) {
						try {
							String actualFilePath = file.getAbsolutePath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
							if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD)) {
								fileCounter++;
								if (actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) { // this is not a link
									log.fine(() -> String.format("working with %s", file.getName())); //$NON-NLS-1$
									String foundObjectKey = OsdReaderWriter.getHeader(file.getCanonicalPath()).get(GDE.OBJECT_KEY);
									if (foundObjectKey != null && foundObjectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH) { // is a valid object key
										if (!objectKeys.contains(foundObjectKey)) {
											log.fine(() -> String.format("found new object key %s", foundObjectKey)); //$NON-NLS-1$
											objectKeys.add(foundObjectKey);
											Vector<File> tmpObjectFiles = new Vector<File>();
											tmpObjectFiles.add(file);
											objectFilesMap.put(foundObjectKey, tmpObjectFiles);
										}
										else {
											objectFilesMap.get(foundObjectKey).add(file);
										}
										log.fine(() -> String.format("add file %s to object key %s", file.getName(), foundObjectKey)); //$NON-NLS-1$
									}
								}
							}
							if (i % progressDistance == 0) GDE.getUiNotification().setProgress((int) (i * progressStep));
							i++;
						}
						catch (IOException e) {
							log.log(Level.WARNING, e.getLocalizedMessage(), e);
						}
						catch (NotSupportedFileFormatException e) {
							log.log(Level.WARNING, e.getLocalizedMessage(), e);
						}
						catch (Throwable t) {
							log.log(Level.WARNING, t.getLocalizedMessage(), t);
						}
					}
					if (log.isLoggable(Level.FINE)) 
						log.log(Level.FINE, String.format("scanned %d files for object key, foundKeysSize = %d", fileCounter, objectKeys.size())); //$NON-NLS-1$ //$NON-NLS-2$
				}
				GDE.getUiNotification().setProgress(progressPercentageLimit);
				{ // createFileLinks: Take the object key list and create file links for all files assigned to object keys.
					int progressPercentageStart = GDE.getUiNotification().getProgressPercentage();
					double progressStep = (100. - progressPercentageStart) / objectKeys.size();

					int j = 0;
					Iterator<String> iterator = objectKeys.iterator();
					//iterate all found object keys
					while (iterator.hasNext()) {
						String tmpObjKey = iterator.next();
						log.fine(() -> String.format("found object key in vector = %s", tmpObjKey)); //$NON-NLS-1$
						//iterate all files of temporary object key
						objectKeyDirPath = this.settings.getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpObjKey;
						FileUtils.checkDirectoryAndCreate(objectKeyDirPath);
						for (File file : objectFilesMap.get(tmpObjKey)) {
							try {
								String newLinkFilePath = objectKeyDirPath + GDE.STRING_FILE_SEPARATOR_UNIX + file.getName();
								if (!new File(newLinkFilePath).exists()) {
									OperatingSystemHelper.createFileLink(file.getCanonicalPath(), newLinkFilePath);
								}
							}
							catch (IOException e) {
								log.log(Level.WARNING, e.getLocalizedMessage(), e);
							}
						}
						j++;
						GDE.getUiNotification().setProgress(progressPercentageStart + (int) (j * progressStep));
					}
				}
				if (this.addToExistentKeys) {
					Set<String> newObjectList = Settings.getInstance().getRealObjectKeys().collect(Collectors.toSet());
					if (newObjectList.addAll(objectKeys) || !newObjectList.equals(new HashSet<>(objectKeys))) {
						this.application.setObjectList(newObjectList.toArray(new String[0]), this.settings.getActiveObject());
						if (log.isLoggable(Level.FINE))
							log.log(Level.FINE, "object list updated: ", newObjectList); //$NON-NLS-1$
					}

					this.obsoleteObjectKeys = getObsoleteObjectKeys(rootDirectory);
				}
				else {
					this.application.setObjectList(objectKeys.toArray(new String[0]), this.settings.getActiveObject());
				}
				GDE.getUiNotification().setProgress(100);
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Build list of object keys without a directory or an empty directory based on the current user selection.
	 * @param rootDirectory
	 * @throws FileNotFoundException
	 */
	private List<String> getObsoleteObjectKeys(File rootDirectory) throws FileNotFoundException {
		// get current object key list as a basis for determining the obsolete object keys
		List<String> resultObjectKeys = this.settings.getRealObjectKeys().collect(Collectors.toList());
		Map<String, DeviceConfiguration> devices = Analyzer.getInstance().getDeviceConfigurations().getAllConfigurations();
		resultObjectKeys.removeAll(ObjectKeyCompliance.defineObjectKeyNovelties(devices));

		for (File dir : FileUtils.getDirectories(rootDirectory)) {
			if (!FileUtils.getFileListing(dir, Integer.MAX_VALUE).isEmpty()) {
				if (GDE.IS_WINDOWS) {
					// iterate due to windows which allows only one directory for object keys with casing differences
					ListIterator<String> iterator = resultObjectKeys.listIterator();
					while (iterator.hasNext()) {
						if (iterator.next().toLowerCase().equals(dir.getName().toLowerCase())) {
							iterator.remove();
							// break; leave this statement out to eliminate multiple casing versions of the name
						}
					}
				}
				else {
					resultObjectKeys.remove(dir.getName());
				}
			}
		}
		log.log(Level.INFO, "obsoleteObjectKeys: ", resultObjectKeys); //$NON-NLS-1$
		return resultObjectKeys;
	}

	/**
	 * deletes all file links under standard data directory
	 * this is the reverse operation of the run() method creating such file links
	 */
	public static void cleanFileLinks() {
		try {
			List<File> files = FileUtils.getFileListing(new File(Settings.getInstance().getDataFilePath()), 1);
			for (File file : files) {
				try {
					String actualFilePath = file.getAbsolutePath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
					if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD) && !actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) {
						if (log.isLoggable(Level.FINE))
							log.log(Level.FINE, "working with " + file.getName()); //$NON-NLS-1$
						if (!file.delete()) {
							if (log.isLoggable(Level.FINE))
								log.log(Level.FINE, "could not delete " + file.getName()); //$NON-NLS-1$
						}
					}
				}
				catch (IOException e) {
					log.log(Level.WARNING, file.getAbsolutePath(), e);
				}
				catch (Throwable t) {
					log.log(Level.WARNING, t.getLocalizedMessage(), t);
				}
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	public List<String> getObsoleteObjectKeys() {
		return this.obsoleteObjectKeys;
	}
}
