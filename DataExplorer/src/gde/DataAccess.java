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

package gde;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;

import com.sun.istack.Nullable;

import gde.config.ExportService;
import gde.config.Settings;
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultProxy;
import gde.log.Level;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

/**
 * Support the roaming data sources.
 * Is a hybrid singleton supporting cloning.
 * @author Thomas Eickert (USER)
 */
public abstract class DataAccess implements Cloneable {

	public final static class LocalAccess extends DataAccess {
		private static final String	$CLASS_NAME	= LocalAccess.class.getName();
		private static final Logger	log					= Logger.getLogger($CLASS_NAME);

		private LocalAccess() {
		}

		private LocalAccess(LocalAccess that) {
			// nothing to clone
		}

		@Override
		@Nullable
		public Reader getSettingsReader() {
			Reader reader = null;
			try {
				File file = new File(GDE.SETTINGS_FILE_PATH);
				if (!file.exists()) {
					if (!file.createNewFile()) log.log(Level.WARNING, "failed creating ", file);
				}
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")); //$NON-NLS-1$
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return reader;
		}

		@Override
		@Nullable
		public Writer getSettingsWriter() {
			Writer writer = null;
			try {
				File file = new File(GDE.SETTINGS_FILE_PATH);
				if (!file.exists()) {
					if (!file.createNewFile()) log.log(Level.WARNING, "failed creating ", file);
				}
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return writer;
		}

		@Override
		public void verifyRoaming() {
			FileUtils.checkDirectoryAndCreate(GDE.APPL_HOME_PATH);
		}

		@Override
		public void verifyLogging() {
			FileUtils.checkDirectoryAndCreate(String.join(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.APPL_HOME_PATH, Settings.LOG_PATH));
		}

		@Override
		public boolean verifyAndBackupTemplates() {
			String templatePath = String.join(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME);
			if (!FileUtils.checkDirectoryAndCreate(templatePath, Settings.GRAPHICS_TEMPLATES_XSD_NAME)) {
				FileUtils.extract(Settings.class, Settings.GRAPHICS_TEMPLATES_XSD_NAME, Settings.PATH_RESOURCE, templatePath, Settings.PERMISSION_555);
				return false;
			} else {
				return true;
			}
		}

		@Override
		public void checkGraphicsTemplates(Collection<ExportService> services, Predicate<String> serviceValidator) {
			Map<String, List<Path>> templateFileMap = readTemplateFiles();
			for (ExportService service : services) {
				try {
					final String serviceName = service.getName();
					if (serviceValidator.test(serviceName)) {
						extractDeviceDefaultTemplates(service.getJarPath(), serviceName);
					} else if (templateFileMap.containsKey(serviceName)) {
						for (Path path : templateFileMap.get(serviceName)) {
							log.log(Level.FINE, "delete ", path);
							if (!path.toFile().delete()) log.log(Level.WARNING, "could not delete ", path);
						}
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}

		private Map<String, List<Path>> readTemplateFiles() {
			Map<String, List<Path>> filesMap = new HashMap<>();
			Path templatePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME);
			try (Stream<Path> files = Files.walk(templatePath, 2)) {
				Stream<Path> potentialFiles = files.filter(t -> t.getFileName().toString().contains(GDE.STRING_DOT));
				filesMap = potentialFiles.collect(Collectors.groupingBy(this::getTemplateFilePrefix));
			} catch (IOException e) {}
			return filesMap;
		}

		private String getTemplateFilePrefix(Path t) {
			String fileName = t.getFileName().toString();
			int index = fileName.lastIndexOf(GDE.CHAR_UNDER_BAR);
			return index < 0 ? "" : fileName.substring(0, index);
		}

		@Override
		public long getGraphicsTemplateLastModified(Path fileSubPath) {
			return Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath).toFile().lastModified();
		}

		/**
		 * Support standard file systems only.
		 * @param file is a file in a standard file system
		 * @return the inputStream for a standard template or an object template
		 */
		@SuppressWarnings("static-method")
		@Nullable
		public InputStream getAlienTemplateInputStream(File file) throws FileNotFoundException {
			return new FileInputStream(file);
		}

		@Override
		public boolean existsGraphicsTemplate(Path fileSubPath) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath);
			return targetFilePath.toFile().exists();
		}

		@Override
		@Nullable
		public InputStream getGraphicsTemplateInputStream(Path fileSubPath) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		@Nullable
		public OutputStream getGraphicsTemplateOutputStream(Path fileSubPath) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath);
			File folder = targetFilePath.getParent().toFile();
			if (!folder.exists()) {
				if (!folder.mkdir()) {
					log.log(WARNING, "failed to create ", folder);
				}
			}
			return new FileOutputStream(targetFilePath.toFile());
		}

		@Override
		public boolean ensureExclusionDirectory() {
			Path directoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME);
			return FileUtils.checkDirectoryAndCreate(directoryPath.toString());
		}

		@Override
		public boolean existsExclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			return targetFilePath.toFile().exists();
		}

		/**
		 * @param fileName of the inclusions file in the standard directory
		 */
		@Override
		public void deleteExclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			FileUtils.deleteFile(targetFilePath.toString());
		}

		@Override
		@Nullable
		public InputStream getExclusionsInputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		@Nullable
		public OutputStream getExclusionsOutputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			File folder = targetFilePath.getParent().toFile();
			if (!folder.exists()) {
				if (!folder.mkdir()) {
					log.log(WARNING, "failed to create ", folder);
				}
			}
			return new FileOutputStream(targetFilePath.toFile());
		}

		@Override
		public boolean ensureInclusionDirectory() {
			Path directoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME);
			return FileUtils.checkDirectoryAndCreate(directoryPath.toString());
		}

		@Override
		public boolean existsInclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			return targetFilePath.toFile().exists();
		}

		@Override
		public void deleteInclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			FileUtils.deleteFile(targetFilePath.toString());
		}

		@Override
		@Nullable
		public InputStream getInclusionsInputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		@Nullable
		public OutputStream getInclusionsOutputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			File folder = targetFilePath.getParent().toFile();
			if (!folder.exists()) {
				if (!folder.mkdir()) {
					log.log(WARNING, "failed to create ", folder);
				}
			}
			return new FileOutputStream(targetFilePath.toFile());
		}

		@Override
		public String[] getCacheFolderList(String vaultDirectory, int minFileLength) {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, vaultDirectory);
			if (!cachePath.toFile().exists()) return new String[0];

			if (minFileLength <= 0) {
				return cachePath.toFile().list();
			} else {
				List<String> vaultNames = new ArrayList<>();
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(cachePath)) {
					for (Path path : stream) {
						if (path.toFile().length() >= minFileLength) vaultNames.add(path.getFileName().toString());
					}
				} catch (Exception e) {
					log.log(SEVERE, e.getMessage(), e);
				}
				return vaultNames.toArray(new String[vaultNames.size()]);
			}
		}

		@Override
		public String[] getCacheZipFolderList(String vaultDirectory, int minFileLength) {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, vaultDirectory);
			if (!cachePath.toFile().exists()) return new String[0];

			List<String> vaultNames = new ArrayList<>();
			try (ZipInputStream stream = dataAccess.getCacheZipInputStream(vaultDirectory)) {
				ZipEntry entry;
				while ((entry = stream.getNextEntry()) != null) {
					if (entry.getSize() >= minFileLength) vaultNames.add(entry.getName());
				}
			} catch (Exception e) {
				log.log(SEVERE, e.getMessage(), e);
			}
			return vaultNames.toArray(new String[vaultNames.size()]);
		}

		@Override
		@Nullable
		public HistoVault getCacheVault(String vaultDirectory, String vaultName, int minVaultLength, boolean xmlFormat) {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, vaultDirectory);
			HistoVault histoVault = null;
			File file = cachePath.resolve(vaultName).toFile();
			if (file.length() > minVaultLength) {
				try (InputStream stream = getCacheInputStream(vaultDirectory, file.getName())) {
					histoVault = xmlFormat ? VaultProxy.load(stream) : VaultProxy.loadJson(stream);
				} catch (Exception e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
			return histoVault;
		}

		@Override
		@Nullable
		public HistoVault getCacheZipVault(String vaultDirectory, String vaultName, int minVaultLength, boolean xmlFormat) {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, vaultDirectory);
			HistoVault histoVault = null;
			try (ZipFile zf = new ZipFile(cachePath.toFile())) {
				if (zf.getEntry(vaultName) != null && zf.getEntry(vaultName).getSize() > minVaultLength) {
					try (InputStream stream = zf.getInputStream(zf.getEntry(vaultName))) {
						histoVault = xmlFormat ? VaultProxy.load(stream) : VaultProxy.loadJson(stream);
					}
				}
			} catch (Exception e) {
				log.log(SEVERE, e.getMessage(), e);
			}
			return histoVault;
		}

		@Override
		public boolean verifyAndBackupDevices() {
			String devicePath = String.join(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.APPL_HOME_PATH, Settings.DEVICE_PROPERTIES_DIR_NAME);
			if (!FileUtils.checkDirectoryAndCreate(devicePath, Settings.DEVICE_PROPERTIES_XSD_NAME)) {
				FileUtils.extract(Settings.class, Settings.DEVICE_PROPERTIES_XSD_NAME, Settings.PATH_RESOURCE, devicePath, Settings.PERMISSION_555);
				return false;
			} else { // execute every time application starts to enable xsd exist and update from added plug-in
				if (!FileUtils.checkFileExist(String.join(GDE.STRING_FILE_SEPARATOR_UNIX, devicePath, Settings.DEVICE_PROPERTIES_XSD_NAME)))
					FileUtils.extract(Settings.class, Settings.DEVICE_PROPERTIES_XSD_NAME, Settings.PATH_RESOURCE, devicePath, Settings.PERMISSION_555);
				return true;
			}
		}

		@Override
		public void checkDeviceProperties(List<ExportService> services, Predicate<String> serviceValidator) {
			for (ExportService service : services) {
				String serviceName = service.getName();
				try {
					if (serviceValidator.test(serviceName)) {
						extractDeviceProperties(service.getJarPath(), serviceName);
					} else {
						Path path = Paths.get(GDE.APPL_HOME_PATH, Settings.DEVICE_PROPERTIES_DIR_NAME, serviceName + GDE.FILE_ENDING_DOT_XML);
						if (path.toFile().exists()) {
							log.log(Level.FINE, "delete ", path);
							if (!path.toFile().delete()) {
								log.log(Level.WARNING, "could not delete", path);
							}
						}
					}
				} catch (IOException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}

		@Override
		public String[] getDeviceFolderList() {
			String xmlBasePath = GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + GDE.STRING_FILE_SEPARATOR_UNIX;
			File deviceFolder = new File(xmlBasePath);
			if (!deviceFolder.exists()) {
				return new String[0];
			} else {
				return deviceFolder.list();
			}
		}

		@Override
		public boolean existsDeviceXml(Path fileSubPath) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH).resolve(fileSubPath);
			return targetFilePath.toFile().exists();
		}

		@SuppressWarnings("static-method")
		public boolean existsDeviceXml(String xmlFilePath) {
			return new File(xmlFilePath).exists();
		}

		@Override
		public InputStream getDeviceXsdInputStream() throws FileNotFoundException {
			String xmlBasePath = GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + GDE.STRING_FILE_SEPARATOR_UNIX;
			Path targetFilePath = Paths.get(xmlBasePath, Settings.DEVICE_PROPERTIES_XSD_NAME);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		public InputStream getDeviceXmlInputStream(Path fileSubPath) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH).resolve(fileSubPath);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		public InputStream getDeviceXmlInputStream(String xmlFilePath) throws FileNotFoundException {
			return new FileInputStream(xmlFilePath);
		}

		@Override
		public OutputStream getDeviceXmlOutputStream(Path fileSubPath) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH).resolve(fileSubPath);
			return new FileOutputStream(targetFilePath.toFile());
		}

		@SuppressWarnings("static-method")
		@Nullable
		public FileOutputStream getDeviceXmlOutputStream(String xmlFilePath) throws FileNotFoundException {
			return new FileOutputStream(xmlFilePath);
		}

		@Override
		@SuppressWarnings("static-method")
		public InputStream getDeviceXsdMigrationStream(int versionNumber) throws FileNotFoundException {
			Path migratePropertyPath = Paths.get(GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + "_V" + versionNumber);
			Path targetFilePath = migratePropertyPath.resolve("DeviceProperties_V" + versionNumber + GDE.FILE_ENDING_DOT_XSD);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		@SuppressWarnings("static-method")
		public boolean existsDeviceMigrationFolder(int versionNumber) {
			Path migratePropertyPath = Paths.get(GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + "_V" + versionNumber);
			return migratePropertyPath.toFile().exists();
		}

		@Override
		public String[] getDevicePropertyFileNames(Path devicePath) {
			return devicePath.toFile().list((dir, name) -> name.endsWith(GDE.FILE_ENDING_DOT_XML));

		}

		@Override
		public List<Path> getDeviceXmlSubPaths(int versionNumber) throws FileNotFoundException {
			String folderName = Settings.DEVICE_PROPERTIES_DIR_NAME + "_V" + versionNumber;
			Path migratePropertyPath = Paths.get(GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + folderName);
			List<File> fileList = FileUtils.getFileListing(migratePropertyPath.toFile(), 1, GDE.FILE_ENDING_DOT_XML);
			return fileList.stream().map(File::getName).map(s -> Paths.get(folderName, s)).collect(Collectors.toList());
		}

		@Override
		public void deleteDeviceHistoTemplates(String deviceName) throws IOException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH).resolve(Settings.GRAPHICS_TEMPLATES_DIR_NAME);
			Files.walkFileTree(targetFilePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String histoTemplateEnding = "H" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(1);
					String fileName = file.getFileName().toString();
					if (fileName.endsWith(histoTemplateEnding) && fileName.startsWith(deviceName)) {
						Files.delete(file);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					// do not delete the directory
					return FileVisitResult.CONTINUE;
				}
			});
		}

		@Override
		public InputStream getGeoCodeInputStream(String geoFileName) throws FileNotFoundException {
			Path filePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			return new FileInputStream(filePath.toFile());
		}

		@SuppressWarnings("static-method")
		public InputStream getHttpsInputStream(URL requestUrl) throws IOException {
			HttpsURLConnection conn = (HttpsURLConnection) requestUrl.openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			InputStream httpInputStream = conn.getInputStream();
			return httpInputStream;
		}

		@SuppressWarnings("static-method")
		public FileOutputStream getGeoCodeOutputStream(String geoFileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			return new FileOutputStream(targetFilePath.toString());
		}

		@Override
		public boolean existsGeoCodeFile(String geoFileName) {
			Path filePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			return FileUtils.checkFileExist(filePath.toString());
		}

		@Override
		public void deleteGeoCodeFile(String geoFileName) {
			Path filePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			FileUtils.deleteFile(filePath.toString());
		}

		@Override
		public List<String> getGeoCodeFolderList() throws FileNotFoundException {
			Path locationsPath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME);
			List<File> fileListing = FileUtils.getFileListing(locationsPath.toFile(), 0);

			List<String> fileNames = new ArrayList<>();
			for (File file : fileListing) {
				fileNames.add(file.getName());
			}
			return fileNames;
		}

		/**
		 * @return true if files were actually deleted
		 */
		@SuppressWarnings("static-method")
		public boolean resetHistolocations() {
			Path locationsPath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME);
			if (FileUtils.checkDirectoryExist(locationsPath.toString())) {
				FileUtils.deleteDirectory(locationsPath.toString());
				log.log(Level.CONFIG, "histo geo locations deleted"); //$NON-NLS-1$
				return true;
			} else
				return false;
		}

		/**
		 * @return true if the folder already exists
		 */
		@SuppressWarnings("static-method")
		public boolean checkAndCreateHistoLocations() {
			return FileUtils.checkDirectoryAndCreate(GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.GPS_LOCATIONS_DIR_NAME);
		}

		public String resetHistoCache() {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
			int initialSize_KiB = (int) FileUtils.size(cachePath) / 1024;
			FileUtils.cleanDirectory(cachePath.toFile());
			FileUtils.checkDirectoryAndCreate(cachePath.toString());
			int deletedSize_KiB = (int) FileUtils.size(cachePath) / 1024;
			FileUtils.extract(this.getClass(), Settings.HISTO_CACHE_ENTRIES_XSD_NAME, Settings.PATH_RESOURCE, cachePath.toString(), Settings.PERMISSION_555);
			String message = Messages.getString(MessageIds.GDE_MSGT0831, new Object[] { initialSize_KiB, deletedSize_KiB, cachePath });
			log.log(Level.CONFIG, message);
			return message;
		}

		@Override
		public boolean verifyAndBackupCache() {
			String cachePath = GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.HISTO_CACHE_ENTRIES_DIR_NAME;
			if (!FileUtils.checkDirectoryAndCreate(cachePath, Settings.HISTO_CACHE_ENTRIES_XSD_NAME)) {
				FileUtils.extract(Settings.class, Settings.HISTO_CACHE_ENTRIES_XSD_NAME, Settings.PATH_RESOURCE, cachePath, Settings.PERMISSION_555);
				return false;
			} else {
				return true;
			}
		}

		@Override
		public boolean existsCacheDirectory(String directoryName) {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			return cacheDirectoryPath.toFile().exists();
		}

		@Override
		public boolean ensureCacheDirectory(String directoryName) {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			return FileUtils.checkDirectoryAndCreate(cacheDirectoryPath.toString());
		}

		@Override
		public boolean existsCacheVault(String directoryName, String fileName) {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			return FileUtils.checkFileExist(cacheDirectoryPath.resolve(fileName).toString());
		}

		@Override
		public long getCacheSize() {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
			return FileUtils.size(cachePath);
		}

		@Override
		public InputStream getCacheXsdInputStream() throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		public InputStream getCacheInputStream(String directoryName, String fileName) throws IOException {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			return Files.newInputStream(cacheDirectoryPath.resolve(fileName), StandardOpenOption.CREATE_NEW);
		}

		@Override
		public ZipInputStream getCacheZipInputStream(String directoryName) throws ZipException, IOException {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			return new ZipInputStream(new FileInputStream(cacheDirectoryPath.toFile()));
		}

		@Override
		public FileSystem getCacheZipFileSystem(String directoryName) throws IOException {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true");
			return FileSystems.newFileSystem(URI.create("jar:" + cacheDirectoryPath.toUri()), env);
		}

		@Override
		public OutputStream getCacheZipOutputStream(FileSystem fileSystem, String fileName) throws IOException {
			return Files.newOutputStream(fileSystem.getPath(fileName), StandardOpenOption.CREATE_NEW);
		}

		@Override
		public OutputStream getCacheOutputStream(String directoryName, String fileName) throws IOException {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			return Files.newOutputStream(cacheDirectoryPath.resolve(fileName), StandardOpenOption.CREATE_NEW);
		}

		@Override
		public InputStream getMappingInputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.MAPPINGS_DIR_NAME, fileName);
			return new BufferedInputStream(new FileInputStream(targetFilePath.toFile()));
		}

		@Override
		public void checkMappingFileAndCreate(Class<?> sourceClass, String fileName) {
			File path = new File(GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.MAPPINGS_DIR_NAME);
			Path targetFilePath = Paths.get(path.toString() + GDE.STRING_FILE_SEPARATOR_UNIX + fileName);
			if (!targetFilePath.toFile().exists()) {
				if (!path.exists() && !path.isDirectory()) path.mkdir();
				// extract initial property files
				FileUtils.extract(sourceClass, fileName, Locale.getDefault().equals(Locale.ENGLISH) ? "resource/en" //$NON-NLS-1$
						: "resource/de", path.getAbsolutePath(), Settings.PERMISSION_555); //$NON-NLS-1$
			}
		}

		@Override
		public long getSourceLastModified(Path fittedFilePath) {
			return fittedFilePath.toFile().lastModified();
		}

		@Override
		public long getSourceLength(Path fittedFilePath) {
			return fittedFilePath.toFile().length();
		}

		@Override
		@Nullable
		public InputStream getSourceInputStream(Path fittedFilePath) {
			try {
				return new FileInputStream(fittedFilePath.toFile());
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException("invalid path " + fittedFilePath);
			}
		}

		@Override
		public Stream<String> getSourceFolderList(Path fittedFolderPath) {
			if (existsSourceFolder(fittedFolderPath)) {
				try {
					return Files.list(fittedFolderPath).map(Path::getFileName).map(Path::toString);
				} catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
// return fittedFolderPath.toFile().list();
			}
			return Stream.empty();
		}

		@Override
		public boolean existsSourceFolder(Path fittedFolderPath) {
			File sourceFolder = fittedFolderPath.toFile();
			return sourceFolder.exists() && sourceFolder.isDirectory() && sourceFolder.canRead();
		}

		@Override
		public boolean existsSourceFile(Path fittedFilePath) {
			return FileUtils.checkFileExist(fittedFilePath.toString());
		}

		@Override
		public Stream<Path> getSourceFolders(Path fittedFolderPath) throws IOException {
			Stream<Path> folders = Files.walk(fittedFolderPath) //
					.filter(p -> !p.equals(fittedFolderPath)) // eliminate root
					.filter(Files::isDirectory);
			return folders;
		}

		@Override
		public Stream<Path> getSourceFolders(Path fittedFolderPath, Stream<String> objectKeys) throws IOException {
			Set<String> lowerCaseKeys = objectKeys.map(String::toLowerCase).collect(Collectors.toSet());
			Stream<Path> folders = Files.walk(fittedFolderPath) //
					.filter(Files::isDirectory) //
					.filter(p -> lowerCaseKeys.contains(p.getFileName().toString().toLowerCase()));
			return folders;
		}

		@Override
		@Nullable
		public Path getActualSourceFile(Path fittedFilePath) {
			long startMillis = System.currentTimeMillis();
			File tmpActualFile = null;
			try {
				tmpActualFile = new File(OperatingSystemHelper.getLinkContainedFilePath(fittedFilePath.toString()));
				// getLinkContainedFilePath may have long response times in case of an unavailable network resources
				// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
				log.log(Level.FINER, "time_ms=", System.currentTimeMillis() - startMillis);
				if (!fittedFilePath.toFile().exists()) throw new IllegalArgumentException("source file does not exist");

				if (Files.isSameFile(fittedFilePath, tmpActualFile.toPath()) && (System.currentTimeMillis() - startMillis > 555)) {
					log.log(Level.WARNING, "Dead OSD link " + fittedFilePath + " pointing to ", tmpActualFile);
					if (!fittedFilePath.toFile().delete()) {
						log.log(Level.WARNING, "could not delete link file ", fittedFilePath);
					}
					tmpActualFile = null;
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return tmpActualFile != null ? tmpActualFile.toPath() : null;
		}

		@Override
		public boolean deleteFleetObjects() {
			Path targetDirPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_OBJECTS_DIR_NAME);
			return FileUtils.cleanDirectory(targetDirPath.toFile());
		}

		@Override
		public Set<String> getFleetFileNames(Function<String, Boolean> objectKeyfilter) {
			Path targetDirPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_OBJECTS_DIR_NAME);
			FileUtils.checkDirectoryAndCreate(targetDirPath.toString());
			Set<String> objectNames;
			try (Stream<String> stream = Files.list(targetDirPath).map(p -> targetDirPath.relativize(p)).map(Path::toString)) {
				objectNames = stream.filter(objectKeyfilter::apply).collect(Collectors.toSet());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return objectNames;
		}

		@Override
		public FileInputStream getFleetInputStream(Path fileSubPath) {
			Path targetDirPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_OBJECTS_DIR_NAME);
			FileUtils.checkDirectoryAndCreate(targetDirPath.toString());
			try {
				return new FileInputStream(targetDirPath.resolve(fileSubPath).toFile());
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException("invalid path " + fileSubPath);
			}
		}

		@Override
		public FileOutputStream getFleetOutputStream(Path fileSubPath) {
			Path targetDirPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_OBJECTS_DIR_NAME);
			FileUtils.checkDirectoryAndCreate(targetDirPath.toString());
			try {
				Files.createDirectories(targetDirPath);
				return new FileOutputStream(targetDirPath.resolve(fileSubPath).toFile());
			} catch (Exception e) {
				throw new IllegalArgumentException("invalid path " + fileSubPath);
			}
		}

		@Override
		public void extractDeviceProperties(Path jarPath, String serviceName) throws IOException {
			if (!FileUtils.checkFileExist(Paths.get(Settings.getDevicesPath(), serviceName + GDE.FILE_ENDING_DOT_XML).toString())) {
				FileUtils.extract(new JarFile(
						jarPath.toFile()), serviceName + GDE.FILE_ENDING_DOT_XML, Settings.PATH_RESOURCE, Settings.getDevicesPath(), Settings.PERMISSION_555);
			}
		}

		@Override
		public void extractDeviceDefaultTemplates(Path jarPath, String serviceName) throws IOException {
			JarFile jarFile = new JarFile(jarPath.toFile());
			String serviceName_ = serviceName + GDE.STRING_UNDER_BAR;
			Enumeration<JarEntry> e = jarFile.entries();
			while (e.hasMoreElements()) {
				String entryName = e.nextElement().getName();
				if (entryName.startsWith(Settings.PATH_RESOURCE_TEMPLATE) && entryName.endsWith(GDE.FILE_ENDING_DOT_XML) && entryName.contains(serviceName_)) {
					String defaultTemplateName = entryName.substring(Settings.PATH_RESOURCE_TEMPLATE.length());
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("jarFile = %s ; defaultTemplateName = %s", jarFile.getName(), entryName)); //$NON-NLS-1$
					if (!FileUtils.checkFileExist(Paths.get(Settings.getGraphicsTemplatePath(), defaultTemplateName).toString())) {
						FileUtils.extract(jarFile, defaultTemplateName, Settings.PATH_RESOURCE_TEMPLATE, Settings.getGraphicsTemplatePath(), Settings.PERMISSION_555);
					}
				}
			}
		}

		@Override
		public void checkMeasurementDisplayProperties(boolean localeChanged, String lang) {
			Path path = Paths.get(GDE.APPL_HOME_PATH, Settings.MAPPINGS_DIR_NAME);
			Path propertyFilePath = path.resolve(Settings.MEASUREMENT_DISPLAY_FILE);
			if (!localeChanged) {
				if (!FileUtils.checkFileExist(propertyFilePath.toString()))
					FileUtils.extract(this.getClass(), Settings.MEASUREMENT_DISPLAY_FILE, String.format("%s%s%s", Settings.PATH_RESOURCE, lang, GDE.STRING_FILE_SEPARATOR_UNIX), path.toString(), Settings.PERMISSION_555);
			} else {
				if (FileUtils.checkFileExist(propertyFilePath.toString())) {
					propertyFilePath.toFile().delete();
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				FileUtils.extract(this.getClass(), Settings.MEASUREMENT_DISPLAY_FILE, String.format("%s%s%s", Settings.PATH_RESOURCE, lang, GDE.STRING_FILE_SEPARATOR_UNIX), path.toString(), Settings.PERMISSION_555);
			}
		}

		@Override
		public synchronized LocalAccess clone() {
			LocalAccess clone = null;
			try {
				clone = new LocalAccess(this);
			} catch (Exception e) {
				LocalAccess.log.log(Level.SEVERE, e.getMessage(), e);
			}
			return clone;
		}

	}

	public static DataAccess getInstance() {
		if (DataAccess.dataAccess == null) {
			if (!Analyzer.isWithBuilders()) {
				DataAccess.dataAccess = new LocalAccess();
			} else {
				DataAccess.dataAccess = null;
			}
		}
		return DataAccess.dataAccess;
	}

	protected static DataAccess dataAccess;

	/**
	 * @param fileSubPath to retrieve the object template from the standard directory
	 * @return the outputStream for a standard template or an object template
	 */
	public abstract OutputStream getGraphicsTemplateOutputStream(Path fileSubPath) throws FileNotFoundException;

	/**
	 * @param fileSubPath to retrieve the object template from the standard directory
	 * @return the inputStream for a standard template or an object template
	 */
	public abstract InputStream getGraphicsTemplateInputStream(Path fileSubPath) throws FileNotFoundException;

	/**
	 * @param fileSubPath to retrieve the object template from the standard directory
	 * @return true if the file exists
	 */
	public abstract boolean existsGraphicsTemplate(Path fileSubPath);

	public abstract long getGraphicsTemplateLastModified(Path fileSubPath);

	public abstract Writer getSettingsWriter();

	public abstract Reader getSettingsReader();

	/**
	 * Check if folder exists and create if required.
	 * @return false if the directory did not exist
	 */
	public abstract boolean ensureInclusionDirectory();

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the outputStream for an exclusions file
	 */
	public abstract OutputStream getInclusionsOutputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the inputStream for an exclusions file
	 */
	public abstract InputStream getInclusionsInputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the inclusions from the standard directory
	 * @return true if the file exists
	 */
	public abstract boolean existsInclusionFile(String fileName);

	/**
	 * @param fileName of the inclusions file in the standard directory
	 */
	public abstract void deleteInclusionFile(String fileName);

	/**
	 * Check if folder exists and create if required.
	 * @return false if the directory did not exist
	 */
	public abstract boolean ensureExclusionDirectory();

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the outputStream for an inclusions file
	 */
	public abstract OutputStream getExclusionsOutputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the inputStream for an inclusions file
	 */
	public abstract InputStream getExclusionsInputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the exclusions from the standard directory
	 * @return true if the file exists
	 */
	public abstract boolean existsExclusionFile(String fileName);

	/**
	 * @param fileName of the exclusions file in the standard directory
	 */
	public abstract void deleteExclusionFile(String fileName);

	/**
	 * @return an array with all device roaming folder entries
	 */
	public abstract String[] getDeviceFolderList();

	/**
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return true if the file exists
	 */
	public abstract boolean existsDeviceXml(Path fileSubPath);

	public abstract InputStream getDeviceXsdInputStream() throws FileNotFoundException;

	/**
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the inputStream for a device properties file
	 */
	public abstract InputStream getDeviceXmlInputStream(Path fileSubPath) throws FileNotFoundException;

	/**
	 * @param devicePropertiesFilePath is the full path
	 * @return the inputStream for a device properties file
	 */
	public abstract InputStream getDeviceXmlInputStream(String devicePropertiesFilePath) throws FileNotFoundException;

	/**
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the inputStream for a device properties file
	 */
	public abstract OutputStream getDeviceXmlOutputStream(Path fileSubPath) throws FileNotFoundException;

	public abstract InputStream getGeoCodeInputStream(String geoFileName) throws FileNotFoundException;

	public abstract boolean existsGeoCodeFile(String geoFileName);

	public abstract void deleteGeoCodeFile(String geoFileName);

	public abstract List<String> getGeoCodeFolderList() throws FileNotFoundException;

	public abstract boolean existsCacheDirectory(String directoryName);

	/**
	 * Check if folder exists and create if required.
	 * @param directoryName is the folder name
	 * @return false if the directory did not exist
	 */
	public abstract boolean ensureCacheDirectory(String directoryName);

	public abstract boolean existsCacheVault(String directoryName, String fileName);

	/**
	 * @return the size of the cache in bytes
	 */
	public abstract long getCacheSize();

	public abstract InputStream getCacheXsdInputStream() throws FileNotFoundException;

	public abstract ZipInputStream getCacheZipInputStream(String directoryName) throws ZipException, IOException;

	/**
	 * @param directoryName is the zip file name (i.e. the file holding the extension zip)
	 * @return the zip file system
	 */
	public abstract FileSystem getCacheZipFileSystem(String directoryName) throws IOException;

	/**
	 * @param fileSystem is the zip file system based on the zip file
	 * @param fileName is the name of the file to be created in the zip file system
	 * @return an output stream for writing the file into the zip file
	 */
	public abstract OutputStream getCacheZipOutputStream(FileSystem fileSystem, String fileName) throws IOException;

	public abstract OutputStream getCacheOutputStream(String directoryName, String fileName) throws IOException;

	public abstract InputStream getCacheInputStream(String directoryName, String fileName) throws IOException;

	/**
	 * @param vaultDirectory is the folderName
	 * @param minVaultLength is the lower limit of bytes
	 * @return the file names without any order
	 */
	public abstract String[] getCacheFolderList(String vaultDirectory, int minVaultLength);

	/**
	 * @param vaultDirectory is the zipFile name
	 * @param minVaultLength is the lower limit of uncompressed bytes
	 * @return the file names without any order
	 */
	public abstract String[] getCacheZipFolderList(String vaultDirectory, int minVaultLength);

	/**
	 * @param vaultDirectory is the folderName
	 * @param minVaultLength is the lower limit of uncompressed bytes
	 * @param xmlFormat false uses a json format instead
	 * @return the vault retrieved from the file system if it exceeds the minimum file length
	 */
	@Nullable
	public abstract HistoVault getCacheVault(String vaultDirectory, String vaultName, int minVaultLength, boolean xmlFormat);

	/**
	 * @param vaultDirectory is the zipFile name
	 * @param minVaultLength is the lower limit of uncompressed bytes
	 * @param xmlFormat false uses a json format instead
	 * @return the vault retrieved from the file system if it exceeds the minimum file length
	 */
	@Nullable
	public abstract HistoVault getCacheZipVault(String vaultDirectory, String vaultName, int minVaultLength, boolean xmlFormat);

	public abstract InputStream getMappingInputStream(String fileName) throws FileNotFoundException;

	public abstract void checkMappingFileAndCreate(Class<?> sourceClass, String fileName);

	/**
	 * @param fittedFilePath is a customized path (full path for local file system access or relative path for other data sources)
	 * @return the lastModified timestamp
	 */
	public abstract long getSourceLastModified(Path fittedFilePath);

	/**
	 * @param fittedFilePath is a customized path (full path for local file system access or relative path for other data sources)
	 * @return the uncompressed length in bytes
	 */
	public abstract long getSourceLength(Path fittedFilePath);

	/**
	 * @param fittedFilePath is a customized path (full path for local file system access or relative path for other data sources)
	 * @return the input stream for a logging source file (osd, bin , ...)
	 */
	public abstract InputStream getSourceInputStream(Path fittedFilePath);

	/**
	 * @param fittedFolderPath is a customized path (full path for local file system access or relative path for other data sources)
	 * @return true if the folder exists and is accessible
	 */
	public abstract boolean existsSourceFolder(Path fittedFolderPath);

	/**
	 * @param fittedFilePath is a customized path (full path for local file system access or relative path for other data sources)
	 * @return true if the folder exists and is accessible
	 */
	public abstract boolean existsSourceFile(Path fittedFilePath);

	/**
	 * @param fittedFolderPath is a customized path (full path for local file system access or relative path for other data sources)
	 * @return the files and directory entries for the folder
	 */
	public abstract Stream<String> getSourceFolderList(Path fittedFolderPath);

	/**
	 * Use the {@code try-with-resources} construct.
	 * @see java.nio.file.Files#walk(Path, java.nio.file.FileVisitOption...)
	 * @param fittedFolderPath is any path
	 */
	public abstract Stream<Path> getSourceFolders(Path fittedFolderPath) throws IOException;

	/**
	 * Use the {@code try-with-resources} construct.
	 * @see java.nio.file.Files#walk(Path, java.nio.file.FileVisitOption...)
	 * @param fittedFolderPath is any path
	 * @return the paths to all folders corresponding to the object keys
	 */
	public abstract Stream<Path> getSourceFolders(Path fittedFolderPath, Stream<String> objectKeys) throws IOException;

	/**
	 * @param fittedFilePath is a customized path (full path for local file system access or relative path for other data sources).
	 *          Points to a source log file or a link file.
	 * @return the path pointing to the link target or the same path in case of a source log file or null if the {@code sourcePath} link is dead
	 */
	public abstract Path getActualSourceFile(Path fittedFilePath);

	/**
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the stream for a fleet index file after creating required directories
	 */
	public abstract FileOutputStream getFleetOutputStream(Path fileSubPath);

	/**
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the stream for a fleet index file after creating required directories
	 */
	public abstract FileInputStream getFleetInputStream(Path fileSubPath);

	/**
	 * @return true if all fleet files are deleted
	 */
	public abstract boolean deleteFleetObjects();

	/**
	 * Search the fleet index entries.
	 * @return the objectKeys corresponding to the objectKeyFilter
	 */
	public abstract Set<String> getFleetFileNames(Function<String, Boolean> objectKeyFilter);

	/**
	 * Support multiple threads with different instances.
	 * Use this if settings updates are not required or apply to the current thread only.
	 * Be aware of the cloning performance impact.
	 * @return the deep clone instance
	 */
	@Override
	public abstract DataAccess clone();

	/**
	 * Extract device properties xml if not exist extract from jar.
	 */
	public abstract void extractDeviceProperties(Path jarPath, String serviceName) throws IOException;

	/**
	 * Extract device default templates if not exist at target path.
	 */
	public abstract void extractDeviceDefaultTemplates(Path jarPath, String serviceName) throws IOException;

	/**
	 * Check and create path.
	 */
	public abstract void verifyRoaming();

	/**
	 * Check and create path.
	 */
	public abstract void verifyLogging();

	/**
	 * Check XSD version and extract XSD if required.
	 * @return false if the current version's directory did not exist and was created
	 */
	public abstract boolean verifyAndBackupCache();

	/**
	 * Check XSD version and extract XSD / XMLs if required.
	 * @return false if the current version's directory did not exist and was created
	 */
	public abstract boolean verifyAndBackupDevices();

	/**
	 * Check XSD version and extract XSD / XMLs if required.
	 * @return false if the current version's directory did not exist and was created
	 */
	public abstract boolean verifyAndBackupTemplates();

	/**
	 * Extract or delete templates.
	 * @param serviceValidator true if the the service name identifies a used service
	 */
	public abstract void checkGraphicsTemplates(Collection<ExportService> services, Predicate<String> serviceValidator);

	/**
	 * Check existence of directory, create if required and update all.
	 * @param services all device jar services
	 * @param serviceValidator true if the the service name identifies a used service
	 */
	public abstract void checkDeviceProperties(List<ExportService> services, Predicate<String> serviceValidator);

	/**
	 * Update display mapping properties file.
	 * @param localeChanged true will force a file replacement
	 * @param lang two-char language identifier (e.g. de, en)
	 */
	public abstract void checkMeasurementDisplayProperties(boolean localeChanged, String lang);

	/**
	 * File system garbage cleaner (rf. to commit 'Fix creating new object graphics templates for criteria never utilized').
	 */
	public abstract void deleteDeviceHistoTemplates(String deviceName) throws IOException;

	public abstract List<Path> getDeviceXmlSubPaths(int versionNumber) throws FileNotFoundException;

	/**
	 * @param devicePath folder with device property files
	 * @return the device property file names
	 */
	public abstract String[] getDevicePropertyFileNames(Path devicePath);

	public abstract boolean existsDeviceMigrationFolder(int versionNumber);

	public abstract InputStream getDeviceXsdMigrationStream(int versionNumber) throws FileNotFoundException;

}
