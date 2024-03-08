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

package gde.histo.cache;

import java.nio.file.Path;
import java.nio.file.Paths;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.histo.utils.SecureHash;
import gde.log.Logger;
import gde.utils.StringHelper;

/**
 * Add functions to the pure serializable histo vault object.
 * @author Thomas Eickert (USER)
 */
public final class ExtendedVault extends HistoVault implements Comparable<ExtendedVault> {
	private static final String	$CLASS_NAME			= ExtendedVault.class.getName();
	private static final Logger	log							= Logger.getLogger($CLASS_NAME);

	/**
	 * For hashing combined keys.
	 */
	private static final String	SHA1_DELIMITER	= ",";
	private static final String	timestampFormat	= "yyyy-MM-dd HH:mm:ss";

	/**
	 * @param vaultReaderSettings a non-empty string indicates that the file reader measurement values depend on device settings
	 * @return directory or zip file name as a unique identifier encoding the data explorer version, the device xml file contents(sha1) plus
	 *         channel number and some settings values
	 */
	public static String getVaultDirectoryName(IDevice device, Settings settings, int channelNumber, String vaultReaderSettings) {
		final String d = SHA1_DELIMITER;
		String tmpSubDirectoryLongKey = GDE.VERSION_NUMBER + d + device.getDeviceConfiguration().getFileSha1Hash() + d + channelNumber //
				+ d + settings.getDataFilePath() + d + settings.getSamplingTimespan_ms() + d + settings.getMinmaxQuantileDistance() + d + settings.getAbsoluteTransitionLevel() //
				+ d + settings.isCanonicalQuantiles() + d + settings.isSymmetricToleranceInterval() + d + settings.getOutlierToleranceSpread() //
				+ d + vaultReaderSettings;
		log.fine(() -> device.getName() + " " + SecureHash.sha1(tmpSubDirectoryLongKey) + " " + tmpSubDirectoryLongKey);
		return SecureHash.sha1(tmpSubDirectoryLongKey);
	}

	/**
	 * The vaults file name is determined without any log file contents and without file location properties (path).
	 * This supports creating a truss or accessing a vault without having read the original log file.
	 * @param newLogFileName file name + lastModified + newFileLength are a simple solution for getting a SHA-1 hash from the file contents
	 * @param newFileLastModified_ms
	 * @param newFileLength in bytes
	 * @param newLogRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @param vaultReaderSettings a non-empty string indicates that the file reader measurement values depend on device settings
	 * @return the file name as a unique identifier (sha1)
	 */
	public static String getVaultName(Path newLogFileName, long newFileLastModified_ms, long newFileLength, int newLogRecordSetOrdinal, //
			IDevice device, Settings settings, int channelNumber, String vaultReaderSettings) {
		final String d = SHA1_DELIMITER;
		String vaultDirectoryName = getVaultDirectoryName(device, settings, channelNumber, vaultReaderSettings);
		return SecureHash.sha1(vaultDirectoryName + d + newLogFileName.getFileName() + d + newFileLastModified_ms //
				+ d + newFileLength + d + newLogRecordSetOrdinal);
	}

	/**
	 * @param histoVault
	 * @param truss is the vault skeleton with the base data for the new recordset
	 * @return the extended vault created form the parameters
	 */
	public static ExtendedVault createExtendedVault(HistoVault histoVault, VaultCollector truss) {
		// todo check if the additional field loadLinkPath is required for file copies in multiple directories
		return new ExtendedVault(histoVault, truss.getVault().loadLinkPath, truss.getVault().loadFilePath, truss.getVault().loadObjectDirectory);
	}

	// parameters for the current loading activity
	private final Path		loadFilePath;					// source file
	private final String	loadObjectDirectory;	// differs from logObjectKey and vaultObjectKey

	private Path					loadLinkPath;					// link to the source file

	/**
	 * @param objectDirectory validated object key
	 * @param filePath file name + lastModified + file length are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileLastModified_ms
	 * @param fileLength in bytes
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @param logRecordSetBaseName the base name without recordset number
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, validated parent path for bin files)
	 * @param vaultReaderSettings a non-empty string indicates that the file reader measurement values depend on device settings
	 */
	public ExtendedVault(String objectDirectory, Path filePath, long fileLastModified_ms, long fileLength, int fileVersion, int logRecordSetSize,
			int logRecordSetOrdinal, String logRecordSetBaseName, String logDeviceName, Analyzer analyzer, long logStartTimestamp_ms, int logChannelNumber,
			String logObjectKey, String vaultReaderSettings) {
		this.loadFilePath = filePath;
		this.loadObjectDirectory = objectDirectory;
		this.loadLinkPath = Paths.get("");

		this.vaultDataExplorerVersion = "" + GDE.VERSION_NUMBER;
		this.vaultDeviceName = analyzer.getActiveDevice().getName();
		this.vaultDeviceKey = analyzer.getActiveDevice().getDeviceConfiguration().getFileSha1Hash();
		this.vaultChannelNumber = analyzer.getActiveChannel().getNumber();
		this.vaultObjectKey = analyzer.getSettings().getActiveObjectKey();
		this.vaultSamplingTimespan_ms = analyzer.getSettings().getSamplingTimespan_ms();

		this.logLinkPath = GDE.STRING_EMPTY;
		this.logFilePath = filePath.toString().replace(GDE.FILE_SEPARATOR, GDE.STRING_FILE_SEPARATOR_UNIX); // toString to avoid 'Object' during marshalling
		this.logFileLastModified = fileLastModified_ms;
		this.logFileLength = fileLength;
		this.logFileVersion = fileVersion;

		this.logObjectDirectory = objectDirectory;
		this.logRecordSetSize = logRecordSetSize;
		this.logRecordSetOrdinal = logRecordSetOrdinal;
		this.logRecordsetBaseName = logRecordSetBaseName;
		this.logDeviceName = logDeviceName;
		this.logChannelNumber = logChannelNumber;
		this.logObjectKey = logObjectKey;
		this.logStartTimestamp_ms = logStartTimestamp_ms;

		this.vaultDirectory = getVaultDirectoryName(analyzer.getActiveDevice(), analyzer.getSettings(), analyzer.getActiveChannel().getNumber(), vaultReaderSettings);
		this.vaultName = getVaultName(filePath, fileLastModified_ms, fileLength, logRecordSetOrdinal, analyzer.getActiveDevice(), analyzer.getSettings(), analyzer.getActiveChannel().getNumber(), vaultReaderSettings);
		this.vaultCreated_ms = System.currentTimeMillis();

		this.vaultReaderSettings = vaultReaderSettings;

		log.finer(() -> String.format("HistoVault.ctor  objectDirectory=%s  path=%s  lastModified=%s  logRecordSetOrdinal=%d  logRecordSetBaseName=%s  startTimestamp_ms=%d   channelConfigNumber=%d   objectKey=%s", //
				objectDirectory, filePath.getFileName().toString(), logRecordSetBaseName, StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", this.logFileLastModified), //
				StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", this.logStartTimestamp_ms), logChannelNumber, logObjectKey)); //
		log.finer(() -> String.format("vaultDirectory=%s  vaultName=%s", this.vaultDirectory, this.vaultName)); //
	}

	/**
	 * Convert into extended vault via shallow copy.
	 * @param histoVault
	 */
	private ExtendedVault(HistoVault histoVault, Path loadLinkPath, Path loadFilePath, String loadObjectDirectory) {
		this.loadFilePath = loadFilePath;
		this.loadObjectDirectory = loadObjectDirectory;

		this.loadLinkPath = loadLinkPath;

		this.vaultName = histoVault.vaultName;
		this.vaultDirectory = histoVault.vaultDirectory;
		this.vaultCreated_ms = histoVault.vaultCreated_ms;

		this.vaultReaderSettings = histoVault.vaultReaderSettings;

		this.vaultDataExplorerVersion = histoVault.vaultDataExplorerVersion;
		this.vaultDeviceKey = histoVault.vaultDeviceKey;
		this.vaultDeviceName = histoVault.vaultDeviceName;
		this.vaultChannelNumber = histoVault.vaultChannelNumber;
		this.vaultObjectKey = histoVault.vaultObjectKey;
		this.vaultSamplingTimespan_ms = histoVault.vaultSamplingTimespan_ms;

		this.logLinkPath = histoVault.logLinkPath;
		this.logFilePath = histoVault.logFilePath;
		this.logFileLastModified = histoVault.logFileLastModified;
		this.logFileLength = histoVault.logFileLength;
		this.logFileVersion = histoVault.logFileVersion;

		this.logObjectDirectory = histoVault.logObjectDirectory;
		this.logRecordSetSize = histoVault.logRecordSetSize;
		this.logRecordSetOrdinal = histoVault.logRecordSetOrdinal;
		this.logRecordsetBaseName = histoVault.logRecordsetBaseName;
		this.logDeviceName = histoVault.logDeviceName;
		this.logChannelNumber = histoVault.logChannelNumber;
		this.logObjectKey = histoVault.logObjectKey;
		this.logStartTimestamp_ms = histoVault.logStartTimestamp_ms;

		this.measurements = histoVault.measurements;
		this.settlements = histoVault.settlements;
		this.scores = histoVault.scores;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		final String d = GDE.STRING_COMMA_BLANK;
		sb.append(this.loadLinkPath).append(d);
		sb.append(this.loadFilePath).append(d);
		return sb.toString();
	}

	/**
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public String getStartTimeStampFormatted() {
		return StringHelper.getFormatedTime(timestampFormat, this.logStartTimestamp_ms); //
	}

	public String getLoadFileExtension() {
		String extension = "";
		int i = this.loadFilePath.toString().lastIndexOf('.');
		if (i > 0) {
			extension = this.loadFilePath.toString().substring(i + 1);
		}
		return extension;
	}

	public String getLoadFileName() {
		return this.loadFilePath.getFileName().toString();
	}

	public Path getLoadFileAsPath() {
		return this.loadFilePath;
	}

	/**
	 * @return relative path (directory name)
	 */
	public Path getVaultDirectoryPath() {
		return Paths.get(this.vaultDirectory);
	}

	public Path getVaultFileName() {
		return Paths.get(this.vaultName);
	}

	/**
	 * @return the non-validated object key or alternatively (if empty) the non-validated object directory
	 */
	public String getRectifiedObjectKey() {
		return this.logObjectKey.isEmpty() ? this.loadObjectDirectory : this.logObjectKey;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int hashCode = 1;
		hashCode = prime * hashCode + this.logChannelNumber;
		hashCode = prime * hashCode + ((this.logDeviceName == null) ? 0 : this.logDeviceName.hashCode());
		hashCode = prime * hashCode + (int) (this.logStartTimestamp_ms ^ (this.logStartTimestamp_ms >>> 32));
		return hashCode;
	}

	/**
	 * Identify duplicates originating from copied log files or from file conversions (e.g. bin to osd).
	 * @param obj
	 * @return true if a truss or a fully populated vault have the same origin log file
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		HistoVault other = (HistoVault) obj;
		if (this.logChannelNumber != other.logChannelNumber) return false;
		if (this.logDeviceName == null) {
			if (other.logDeviceName != null) return false;
		} else if (!this.logDeviceName.equals(other.logDeviceName)) return false;
		if (this.logStartTimestamp_ms != other.logStartTimestamp_ms) return false;
		return true;
	}

	/**
	 * The newest vault is the smallest one.
	 */
	@Override
	public int compareTo(ExtendedVault o) {
		return Long.compare(o.logStartTimestamp_ms, this.logStartTimestamp_ms);
	}

	public static String getSha1Delimiter() {
		return SHA1_DELIMITER;
	}

	public String getLoadFilePath() {
		return this.loadFilePath.toString();
	}

	public Path getLoadLinkPath() {
		return this.loadLinkPath;
	}

	public void setLoadLinkPath(Path loadLinkPath) {
		this.loadLinkPath = loadLinkPath;
	}

	public String getLoadObjectDirectory() {
		return this.loadObjectDirectory;
	}

	/**
	 * @return true if this is a vault skeleton only
	 */
	public boolean isTruss() {
		return this.getMeasurements().isEmpty();
	}

}
