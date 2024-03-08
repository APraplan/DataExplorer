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

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.ui.datasources;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gde.Analyzer;
import gde.DataAccess;
import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.SourceDataSet;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.FileUtils;

/**
 * Folder for additional object directories in the working directory.
 * Supports caching external directory log files in this folder (mirroring).
 * @author Thomas Eickert (USER)
 */
public final class SupplementObjectFolder {
	private static final String	$CLASS_NAME					= SupplementObjectFolder.class.getName();
	private static final Logger	log									= Logger.getLogger($CLASS_NAME);

	private static final String	SUPPLEMENT_DIR_NAME	= "_SupplementObjectDirs";
	private static final String	README_NAME					= "README.TXT";
	private static final String	REPORT_NAME					= "report.txt";
	private static final int		PATHS_MESSAGE_LINES	= 22;
	private static final long		MIRROR_CYCLE_MS			= 60000;																				// min timespan before the mirror is updated again

	private static long					mirrorUpdate_ms			= System.currentTimeMillis() - MIRROR_CYCLE_MS;

	public static void checkAndCreate() {
		Path objectsPath = getSupplementObjectsPath();
		FileUtils.checkDirectoryAndCreate(objectsPath.toString());
		FileUtils.extract(SupplementObjectFolder.class, README_NAME, Settings.PATH_RESOURCE, objectsPath.toString(), Settings.PERMISSION_555);
	}

	/**
	 * @return the directory based on the data path defined by the user which holds the accessible log files object folders
	 */
	public static Path getSupplementObjectsPath() {
		return Paths.get(Settings.getInstance().getDataFilePath(), SUPPLEMENT_DIR_NAME); // ok
	}

	/**
	 * @param externalPath is a non empty path with subdirectories ('E:\Logs')
	 * @return the replacement path condensed to a single directory as a relative path ('E__Logs')
	 */
	static Path getTargetBasePath(Path externalPath) {
		if (externalPath.toString().isEmpty()) throw new IllegalArgumentException("empty mirror property string");

		String finalPath = externalPath.toString() //
				.replace(GDE.CHAR_FILE_SEPARATOR_UNIX, GDE.CHAR_UNDER_BAR) //
				.replaceAll(Matcher.quoteReplacement(GDE.STRING_FILE_SEPARATOR_WINDOWS), GDE.STRING_UNDER_BAR) //
				.replace(GDE.CHAR_COLON, GDE.CHAR_UNDER_BAR);
		// force path starting with "__"
		if (finalPath.substring(0, 1) != GDE.STRING_UNDER_BAR) finalPath = GDE.STRING_UNDER_BAR + finalPath;
		if (finalPath.substring(1, 2) != GDE.STRING_UNDER_BAR) finalPath = GDE.STRING_UNDER_BAR + finalPath;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "finalPath=" + finalPath + "  from " + externalPath.toString());
		return Paths.get(finalPath);
	}

	/**
	 * @return nonempty paths or an empty stream
	 */
	private static Stream<Path> getMirrorSourceFolders() {
		String sourceFoldersCsv = Settings.getInstance().getMirrorSourceFoldersCsv(); // ok
		if (sourceFoldersCsv.isEmpty()) return Stream.empty();

		try {
			return Arrays.stream(sourceFoldersCsv.split(GDE.STRING_CSV_SEPARATOR)) //
					.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return Stream.empty();
		}
	}

	/**
	 * Scan the mirror source folders asynchronously.
	 * Ensure latency period after last scan.
	 */
	public static void updateLogMirror() {
		if (mirrorUpdate_ms + MIRROR_CYCLE_MS > System.currentTimeMillis()) return;

		mirrorUpdate_ms = System.currentTimeMillis();
		SupplementObjectFolder instance = new SupplementObjectFolder(Analyzer.getInstance()); // ok
		Thread rebuildThread = new Thread(() -> instance.rebuildLogMirror(), "rebuildLogMirror");
		try {
			rebuildThread.start();
		} catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * Scan the mirror source folders.
	 */
	static void updateLogMirrorSync() {
		SupplementObjectFolder instance = new SupplementObjectFolder(Analyzer.getInstance()); //ok
		instance.rebuildLogMirror();
	}

	/**
	 * Delete all object folders.
	 * Do not delete associated vaults for simplicity reasons.
	 * @return a message for user notification with a maximum of {@value #PATHS_MESSAGE_LINES} folder paths
	 */
	public static String resetFolders() {
		String message = "";
		Path objectsPath = getSupplementObjectsPath();
		int initialSize_MiB = (int) (FileUtils.size(objectsPath) / 1024 / 1024); // ok

		Stream<String> realObjectKeys = Settings.getInstance().getRealObjectKeys(); // ok

		StringBuilder sb = new StringBuilder("  in ").append(objectsPath.toString()).append(GDE.STRING_BLANK_COLON_BLANK);
		try (Stream<Path> stream = DataAccess.getInstance().getSourceFolders(objectsPath, realObjectKeys)) { // ok
			int[] i = new int[] { 0 };
			stream.sorted(Comparator.reverseOrder()).forEach(p -> { // take the deeply nested folders first due to recursive delete
				FileUtils.deleteDirectory(p.toString());
				i[0]++;
				if (i[0] < PATHS_MESSAGE_LINES) {
					sb.append(GDE.STRING_NEW_LINE).append(GDE.STRING_BLANK).append(objectsPath.relativize(p));
				} else if (i[0] == PATHS_MESSAGE_LINES) {
					sb.append(GDE.STRING_NEW_LINE).append(GDE.STRING_BLANK).append(objectsPath.relativize(p)).append(GDE.STRING_NEW_LINE).append(GDE.STRING_ELLIPSIS);
				}
			});
			// avoid windows file access errors in the size method below
			Thread.sleep(11);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return "File access error";
		}
		int deletedSize_MiB = (int) (FileUtils.size(objectsPath) / 1024 / 1024);
		message = Messages.getString(MessageIds.GDE_MSGT0924, new Object[] { initialSize_MiB, deletedSize_MiB, sb.toString() });
		log.log(Level.FINE, message);
		return message;
	}

	/**
	 * @param sourceDir
	 * @param targetDir
	 * @param millis is the number of elapsed milliseconds
	 * @param fileCounterPriorCopy is the number of files in the target directory or -1 if the directory does not exist
	 * @param copyCounter is the number of files copied successfully
	 * @param freeSpacePriorCopy_MiB is the free space on the volume
	 */
	private static void writeReportFile(Path sourceDir, Path targetDir, long millis, long fileCounterPriorCopy, int copyCounter,
			long freeSpacePriorCopy_MiB) {
		// todo check if using a property file is a better solution
		long fileCounterPostCopy; // this figure might cost a lot of elapsed time: decide if better to remove entirely
		try (Stream<Path> stream = Files.list(targetDir)) {
			fileCounterPostCopy = stream.count();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return;
		}
		long freeSpaceAfterCopy_MiB = targetDir.toFile().getFreeSpace() / 1024 / 1024;

		String result = String.format("sourceDir=%s" + GDE.STRING_NEW_LINE //
				+ "targetDir=%s" + GDE.STRING_NEW_LINE //
				+ "numberOfFiles   : prior=%,d post=%,d copyAttempts=%,d" + GDE.STRING_NEW_LINE //
				+ "freeSpace [MiB] : prior=%,d post=%,d" + GDE.STRING_NEW_LINE //
				+ "duration [ms]   : elapsed=%,d" + GDE.STRING_NEW_LINE //
				+ "updated=%s", //
				sourceDir, targetDir, //
				fileCounterPriorCopy, fileCounterPostCopy, copyCounter, //
				freeSpacePriorCopy_MiB, freeSpaceAfterCopy_MiB, //
				millis, //
				Instant.now().toString());

		// no buffered writer as we invoke the write method only once
		try (Writer out = new OutputStreamWriter(new FileOutputStream(targetDir.resolve(REPORT_NAME).toFile()), "UTF-8")) {
			out.write(result);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * Extend the report file.
	 * Used after having checked the files in the directories without any copy activity.
	 */
	private static void appendReportFile(Path targetDir) {
		String result = String.format(GDE.STRING_NEW_LINE + "checked=%s", //
				Instant.now().toString());

		// no buffered writer as we invoke the write method only once
		try (Writer out = new OutputStreamWriter(new FileOutputStream(targetDir.resolve(REPORT_NAME).toFile(), true), "UTF-8")) {
			out.write(result);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private final Analyzer		analyzer;
	private final Path				supplementFolder	= getSupplementObjectsPath();
	private final Set<String>	logFileExtentions;

	/**
	 * Creates the base folder for additional logs which are created by other systems (computers, RC transmitters, etc).
	 * @param analyzer
	 */
	private SupplementObjectFolder(Analyzer analyzer) {
		this.analyzer = analyzer;
		this.logFileExtentions	= analyzer.getDeviceConfigurations().getValidLogExtentions();
		FileUtils.checkDirectoryAndCreate(this.supplementFolder.toString());
	}

	private void rebuildLogMirror() {
		log.log(Level.FINEST, "start rebuildLogMirror");
		long startNanoTime = System.nanoTime();

		getMirrorSourceFolders().forEach(f -> {
			log.log(Level.INFO, "externalBaseDir=", f);
			rebuildLogMirror(f);
		});
		log.time(() -> String.format("  %3d mirror paths scanned          time=%,6d [ms]", getMirrorSourceFolders().count(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime + 500000)));
	}

	private void rebuildLogMirror(Path externalBaseDir) {
		Path targetBaseDir = supplementFolder.resolve(getTargetBasePath(externalBaseDir));

		List<Path> result = new ArrayList<Path>();
		try (Stream<Path> objectPaths = analyzer.getDataAccess().getSourceFolders(externalBaseDir, Settings.getInstance().getRealObjectKeys())) { // ok
			result = objectPaths.collect(Collectors.toList());
			for (Path path : result) {
				log.log(Level.FINER, "sourcePath=", path);
				Path relativeSubPath = externalBaseDir.relativize(path);
				copyDirectoryFiles(path, targetBaseDir.resolve(relativeSubPath));
			}
		} catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), " is not accessible : " + e.getClass());
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
	}

	/**
	 * @param srcDir is the directory path holding the source files
	 * @param targetDir is the directory path dedicated for all log file types (*.osd, *.bin, ...)
	 * @throws IOException
	 */
	private void copyDirectoryFiles(Path srcDir, Path targetDir) throws IOException {
		long startNanoTime = System.nanoTime();

		int copyCounter = 0;
		final long fileCounterPriorCopy;
		final long freeSpacePriorCopy_MiB = targetDir.getRoot().toFile().getFreeSpace() / 1024 / 1024;
		if (targetDir.toFile().exists()) {
			try (Stream<Path> stream = Files.list(targetDir)) {
				fileCounterPriorCopy = stream.count();
			}
		} else {
			fileCounterPriorCopy = -1;
		}

		CopyOption[] options = new CopyOption[] { //
				// StandardCopyOption.REPLACE_EXISTING, //
				StandardCopyOption.COPY_ATTRIBUTES, //
				// LinkOption.NOFOLLOW_LINKS //
		};

		try (DirectoryStream<Path> files = Files.newDirectoryStream(srcDir)) {
			for (Path f : files) {
				if (f.toFile().isDirectory()) continue;

				boolean isLogFile = logFileExtentions.parallelStream().anyMatch(s -> f.getFileName().toString().toLowerCase().endsWith(s));
				if (!isLogFile) continue;

				Path targetPath = targetDir.resolve(f.getFileName());
				FileUtils.checkDirectoryAndCreate(targetDir.toString());
				if (!targetPath.toFile().exists()) {
					SourceDataSet sourceDataSet = SourceDataSet.createSourceDataSet(f, analyzer);
					if (sourceDataSet != null) { // check if supported by histo
						try {
							Files.copy(sourceDataSet.getActualFile(), targetPath, options);
							copyCounter++;
							log.log(Level.FINE, "copy done ", targetPath);
						} catch (AccessDeniedException e) {
							log.log(Level.WARNING, "File copy problem", e.getMessage() + " " + targetPath);
						} catch (IOException e) {
							log.log(Level.WARNING, "OSD link file problem", e.getMessage() + " " + targetPath);
						}
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		} finally {
			if (targetDir.toFile().exists()) {
				if (copyCounter > 0) {
					writeReportFile(srcDir, targetDir, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime + 500000), fileCounterPriorCopy, copyCounter, freeSpacePriorCopy_MiB);
				} else if (fileCounterPriorCopy > 0) { // this is folder with previously copied files
					try (Stream<Path> stream = Files.list(targetDir)) {
						boolean anyFiles = stream.anyMatch(p -> !p.toFile().isDirectory());
						if (anyFiles) appendReportFile(targetDir);
					}
				}
			}
		}
	}
}