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

package gde.utils;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;

import gde.Analyzer;
import gde.DataAccess;
import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.histo.ui.datasources.SupplementObjectFolder;
import gde.histo.utils.PathUtils;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Utilities to detect and maintain the object folders and object keys identity.
 * @author Thomas Eickert (USER)
 */
public class ObjectKeyCompliance {
	private static final String	$CLASS_NAME	= ObjectKeyCompliance.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Scan all OSD files in the data path for object names, create objects from them and merge them into the existing objects key list.
	 * Identify object keys with empty directories or without directories and query the user for deletion.
	 */
	public static void rebuildObjectKeys() {
		final ObjectKeyScanner objLnkSearch = new ObjectKeyScanner(true);
		objLnkSearch.start();
		try {
			objLnkSearch.join();
		} catch (InterruptedException e1) {
			log.log(Level.WARNING, e1.getMessage(), e1);
		}

		if (!GDE.isWithUi()) {
			removeObjectKeys(objLnkSearch.getObsoleteObjectKeys());
		} else {
			if (!objLnkSearch.getObsoleteObjectKeys().isEmpty()) {
				Collections.sort(objLnkSearch.getObsoleteObjectKeys(), String.CASE_INSENSITIVE_ORDER);
				String message = Messages.getString(MessageIds.GDE_MSGI0063, new Object[] { objLnkSearch.getObsoleteObjectKeys() });
				if (SWT.YES == DataExplorer.getInstance().openYesNoMessageDialogSync(message)) {
					removeObjectKeys(objLnkSearch.getObsoleteObjectKeys());
				}
			} else if (GDE.shell.isDisposed())
				DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0034));
			else
				DataExplorer.getInstance().openMessageDialogAsync(GDE.shell, Messages.getString(MessageIds.GDE_MSGI0034));
		}
	}

	/**
	 * Add the object directories and add the key to the object key list.
	 */
	public static void addObjectKeys(Collection<String> newObjectKeys) {
		// build a new object list consisting from existing objects and new objects
		Set<String> objectListClone = Settings.getInstance().getRealObjectKeys().collect(Collectors.toSet());
		if (objectListClone.addAll(newObjectKeys)) {
			// ensure that all objects owns a directory
			for (String tmpObjectKey : newObjectKeys) {
				Path objectKeyDirPath = Paths.get(Settings.getInstance().getDataFilePath()).resolve(tmpObjectKey);
				FileUtils.checkDirectoryAndCreate(objectKeyDirPath.toString()); //ok
			}
			Settings.getInstance().setObjectList(objectListClone.toArray(new String[0]), Settings.getInstance().getActiveObject());
			log.log(Level.FINE, "object list updated and directories created for object keys : ", newObjectKeys);
		}
	}

	/**
	 * @param newObjectKeys is the list of object keys for transfer into the settings
	 */
	private static void replaceObjectKey(String oldObjKey, String newObjKey, String[] newObjectKeys) {
		// reverse loop - the placeholder is at the end in case of add
		int selectionIndex = newObjectKeys.length - 1;
		for (; selectionIndex >= 0; selectionIndex--) {
			if (newObjectKeys[selectionIndex].equals(oldObjKey)) {
				newObjectKeys[selectionIndex] = newObjKey;
				break;
			}
		}
		Settings.getInstance().setObjectList(newObjectKeys, newObjKey);
	}

	/**
	 * @param objectKeys is the list of object keys to be extended by the new object key for transfer into the settings
	 */
	public static void addObjectKey(String newObjKey, String[] objectKeys) {
		String[] newObjectKeys = new String[objectKeys.length + 1];
		System.arraycopy(objectKeys, 0, newObjectKeys, 0, objectKeys.length);
		newObjectKeys[newObjectKeys.length - 1] = newObjKey;

		Settings.getInstance().setObjectList(newObjectKeys, newObjKey);
	}

	/**
	 * @param newObjectKeys is the list of object keys for transfer into the settings
	 */
	public static void renameObjectKey(String oldObjKey, String newObjKey, String[] newObjectKeys) {
		if (!oldObjKey.isEmpty()) {
			int answer = SWT.YES;
			if (GDE.isWithUi()) {
				// query if new object key should be used to modify all existing data files with the new corrected one
				answer = DataExplorer.getInstance().openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0048, new String[] { oldObjKey,
						newObjKey }));
			}
			if (answer == SWT.YES) {
				OsdReaderWriter.updateObjectKey(oldObjKey, newObjKey);
				DataExplorer.getInstance().updateCurrentObjectData(newObjKey);
				new ObjectKeyScanner(newObjKey).start();
			}

			if (FileUtils.checkDirectoryExist(Settings.getInstance().getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + oldObjKey)) { //ok
				// query for old directory deletion
				if (!GDE.isWithUi() || SWT.YES == DataExplorer.getInstance().openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW0031)))
					FileUtils.deleteDirectory(Settings.getInstance().getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + oldObjKey); //ok
			}
		}
		replaceObjectKey(oldObjKey, newObjKey, newObjectKeys);

		ObjectKeyCompliance.checkChannelForObjectKeyMissmatch(newObjKey);
	}

	/**
	 * Support deleting empty object keys.
	 * @param newObjectKeys the object key list for removal and transfer into the settings
	 * @param removeIndex points to the object key to be deleted
	 */
	public static void removeObjectKey(String[] newObjectKeys, int removeIndex) {
		List<String> tmpObjectKeys = new ArrayList<>(Arrays.asList(newObjectKeys));
		String delObjectKey = tmpObjectKeys.remove(removeIndex).trim();
		int index = removeIndex >= 2 && !delObjectKey.isEmpty() ? removeIndex - 1 : tmpObjectKeys.size() > 1 ? 1 : 0;
		Settings.getInstance().setObjectList(tmpObjectKeys.toArray(new String[1]), tmpObjectKeys.get(index));
		if (!delObjectKey.isEmpty()) {
			FileUtils.deleteDirectory(Settings.getInstance().getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + delObjectKey); //ok
		}
	}

	/**
	 * Remove from the object key list.
	 * Do not delete the object directories.
	 */
	public static void removeObjectKeys(Collection<String> obsoleteObjectKeys) {
		List<String> realObjectKeys = Settings.getInstance().getRealObjectKeys().collect(Collectors.toList());
		for (String tmpObjectKey : obsoleteObjectKeys) {
			// if (!tmpObjectKey.isEmpty()) {
			// Path objectKeyDirPath = Paths.get(Settings.getInstance().getDataFilePath()).resolve(tmpObjectKey);
			// FileUtils.deleteDirectory(objectKeyDirPath.toString());
			// }
			realObjectKeys.remove(tmpObjectKey);
		}
		Settings.getInstance().setObjectList(realObjectKeys.toArray(new String[0]), Settings.getInstance().getActiveObject());
		if (GDE.isWithUi()) //junit must skip
			DataExplorer.getInstance().setObjectListElements();
	}

	/**
	 * Do everything to make the new object key available.
	 */
	public static void createObjectKey(String newObjectKey) {
		ObjectKeyCompliance.addObjectKey(newObjectKey, Settings.getInstance().getObjectList());
		ObjectKeyCompliance.checkChannelForObjectKeyMissmatch(newObjectKey);

		DataExplorer.getInstance().setObjectListElements();
		DataExplorer.getInstance().setObjectDescriptionTabVisible(true);
		DataExplorer.getInstance().updateObjectDescriptionWindow();

		FileUtils.checkDirectoryAndCreate(Settings.getInstance().getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + newObjectKey); //ok
		new ObjectKeyScanner(newObjectKey).start();
	}

	/**
	 * @return an unknown object key candidate derived from the file path or an empty string
	 */
	public static String getUpcomingObjectKey(Path filePath) {
		String upcomingObjectKey = GDE.STRING_EMPTY;
		if (GDE.isWithUi()) {
			Path fileSubPath;
			try {
				fileSubPath = Paths.get(Settings.getInstance().getDataFilePath()).relativize(filePath);
			} catch (Exception e) {
				return upcomingObjectKey;
			}
			if (fileSubPath.getNameCount() < 1) return upcomingObjectKey;

			String directoryName = fileSubPath.subpath(0, 1).toString();
			if (directoryName.length() < GDE.MIN_OBJECT_KEY_LENGTH || GDE.STRING_PARENT_DIR.equals(directoryName)) return upcomingObjectKey;

			if (Settings.getInstance().isHistoActive() && Settings.getInstance().isObjectQueryActive() //
					&& !DataExplorer.getInstance().isObjectSelectorEditable() //
					&& !Analyzer.getInstance().getDeviceConfigurations().contains(directoryName) // ok
					&& !Settings.getInstance().getValidatedObjectKey(directoryName).isPresent()) { // ok
				if (SWT.NO != DataExplorer.getInstance().openYesNoMessageDialogSync(Messages.getString(MessageIds.GDE_MSGT0929, new Object[] {
						directoryName }))) upcomingObjectKey = directoryName;
			}
		}
		return upcomingObjectKey;
	}

	/**
	 * @return the valid paths for source files
	 */
	private static ArrayList<Path> getSourcePaths() {
		ArrayList<Path> dirPaths = new ArrayList<Path>();

		final String dataFilePath = Settings.getInstance().getDataFilePath();
		if (dataFilePath != null && !dataFilePath.trim().isEmpty() && !dataFilePath.equals(GDE.STRING_FILE_SEPARATOR_UNIX)) {
			dirPaths.add(Paths.get(dataFilePath));
			log.log(Level.FINE, "data path ", dataFilePath);
		}
		Arrays.stream(Settings.getInstance().getDataFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
				.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get).forEach(dirPaths::add);
		Arrays.stream(Settings.getInstance().getImportFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
				.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get).forEach(dirPaths::add);
		return dirPaths;
	}

	/**
	 * Scan the sub-directories in the source paths.
	 * @return the non-empty sub-directory names which do not represent devices
	 */
	public static Set<String> defineObjectKeyCandidates(Map<String, DeviceConfiguration> devices) {
		Set<String> excludedLowerCaseNames = new HashSet<>();
		excludedLowerCaseNames.add(SupplementObjectFolder.getSupplementObjectsPath().getFileName().toString().toLowerCase());

		devices.keySet().stream().map(String::toLowerCase).forEach(excludedLowerCaseNames::add);
		return readSourcePathsObjectKeys(excludedLowerCaseNames);
	}

	/**
	 * Scan the sub-directories in the source paths.
	 * @return the non-empty sub-directory names which neither represent devices nor object keys
	 */
	public static Set<String> defineObjectKeyNovelties(Map<String, DeviceConfiguration> devices) {
		Set<String> excludedLowerCaseNames = new HashSet<>();
		excludedLowerCaseNames.add(SupplementObjectFolder.getSupplementObjectsPath().getFileName().toString().toLowerCase());

		devices.keySet().stream().map(String::toLowerCase).forEach(excludedLowerCaseNames::add);
		Settings.getInstance().getRealObjectKeys().map(String::toLowerCase).forEach(excludedLowerCaseNames::add);
		return readSourcePathsObjectKeys(excludedLowerCaseNames);
	}

	/**
	 * @return the folder names of non-empty sub-directories not matching the {@code excludedLowerCaseNames}
	 */
	private static Set<String> readSourcePathsObjectKeys(Set<String> excludedLowerCaseNames) {
		Function<Path, Boolean> isSupplementMirrorRoot = directoryPath -> {
			// true for mirror copy roots just below the {@code supplementObjectsPath}
			boolean isRoot = false;
			try {
				Path supplementObjectsPath = SupplementObjectFolder.getSupplementObjectsPath();
				Path subPath = supplementObjectsPath.relativize(directoryPath);
				boolean isFirstLevelSupplement = directoryPath.startsWith(supplementObjectsPath) && subPath.getNameCount() == 1;
				isRoot = isFirstLevelSupplement && subPath.toString().length() > 2 && subPath.toString().substring(0, 2).equals(GDE.CHAR_UNDER_BAR + GDE.STRING_UNDER_BAR);
			} catch (IllegalArgumentException e) { // UNC windows path does not compare with standard windows path
				log.log(Level.FINE, e.getMessage(), SupplementObjectFolder.getSupplementObjectsPath() + " <> other: " + directoryPath);
			}
			return isRoot;
		};
		Function<Path, Boolean> isExcludedName = p -> excludedLowerCaseNames.contains(p.getFileName().toString().toLowerCase());
		Set<String> validLogExtentions = Analyzer.getInstance().getDeviceConfigurations().getValidLogExtentions(); // ok
		Function<Path, Boolean> isEmptyFolder = p -> !DataAccess.getInstance().getSourceFolderList(p) //
				.map(PathUtils:: getFileExtention).map(String::toLowerCase)
				.anyMatch(s -> validLogExtentions.contains(s)); // todo exclude folders???

		Set<String> directoryNames = new HashSet<>();
		for (Path dirPath : getSourcePaths()) {
			try (Stream<Path> stream = DataAccess.getInstance().getSourceFolders(dirPath)) {
				stream.filter(p -> !isSupplementMirrorRoot.apply(p)) // eliminate the roots but not their contents
						.filter(p -> !isExcludedName.apply(p)) //
						.filter(p -> !isEmptyFolder.apply(p)) //
						.peek(p -> log.log(Level.FINER, "sourcePath =", p)) //
						.map(Path::getFileName).map(Path::toString) //
						.distinct().forEach(directoryNames::add);
			} catch (NoSuchFileException e) { // folder does not exist
				log.log(Level.WARNING, "NoSuchFileException", dirPath);
			} catch (IOException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			} catch (Throwable e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
		return directoryNames;
	}

	/**
	 * Check the new key against an existing channel key and ask for replacement.
	 */
	public static void checkChannelForObjectKeyMissmatch(String newObjectKey) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			String channelObjKey = activeChannel.getObjectKey();

			// check if selected key matches the existing object key or is new for this channel
			if (!newObjectKey.equals(channelObjKey)) { // channel has a key
				int answer = SWT.YES;
				if (GDE.isWithUi()) {
					answer = DataExplorer.getInstance().getActiveRecordSet() == null ? SWT.YES
							: (DataExplorer.getInstance().openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGT0205, new Object[] { channelObjKey,
									newObjectKey })));
				}
				if (answer == SWT.YES) { // replace existing objectkey in channel
					activeChannel.setObjectKey(newObjectKey);
					String updateFileDescription = activeChannel.getFileDescription();
					if (channelObjKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH && updateFileDescription.contains(channelObjKey)) {
						if (newObjectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH) {
							updateFileDescription = updateFileDescription.substring(0, updateFileDescription.indexOf(channelObjKey)) + newObjectKey + updateFileDescription.substring(updateFileDescription.indexOf(channelObjKey) + channelObjKey.length());
						} else { // newObjectKey = ""
							updateFileDescription = updateFileDescription.substring(0, updateFileDescription.indexOf(channelObjKey) - 1) + updateFileDescription.substring(updateFileDescription.lastIndexOf(channelObjKey) + channelObjKey.length());
						}
					} else if (newObjectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH && !updateFileDescription.contains(newObjectKey)) {
						if (updateFileDescription.indexOf(GDE.STRING_BLANK) == -1)
							updateFileDescription = updateFileDescription + GDE.STRING_BLANK + newObjectKey;
						else
							updateFileDescription = updateFileDescription.substring(0, updateFileDescription.indexOf(GDE.STRING_BLANK) + 1) + newObjectKey + updateFileDescription.substring(updateFileDescription.indexOf(GDE.STRING_BLANK));
					}
					activeChannel.setFileDescription(updateFileDescription);
				}
				// do not exchange the object key in the channel/configuration, but keep the selector switch to enable new data load
				if (GDE.isWithUi()) 
					DataExplorer.getInstance().updateAllTabs(false);
			}
		}
	}

}
