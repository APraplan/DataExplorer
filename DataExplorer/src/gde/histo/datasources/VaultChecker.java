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

package gde.histo.datasources;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.device.IHistoDevice;
import gde.histo.utils.PathUtils;
import gde.log.Logger;

/**
 * Check vaults without Ui, i.e. active device, channel, object.
 * @author Thomas Eickert (USER)
 */
public final class VaultChecker {
	private static final String	$CLASS_NAME	= VaultChecker.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Holds the selection criteria for trusses (vault skeletons) or vaults.
	 */
	public static final class TrussCriteria {
		final List<Integer>	channelConfigNumbers;
		final long					minStartTimeStamp_ms;
		final String				activeObjectKey;
		final boolean				ignoreLogObjectKey;
		final Set<String>		realObjectKeys;

		public static TrussCriteria createTrussCriteria(Analyzer analyzer) {
			return new TrussCriteria(analyzer);
		}

		private TrussCriteria(Analyzer analyzer) {
			channelConfigNumbers = analyzer.getSettings().isChannelMix() //
					? analyzer.getActiveDevice().getDeviceConfiguration().getChannelMixConfigNumbers(analyzer.getActiveChannel().getNumber()) //
					: Collections.singletonList(analyzer.getActiveChannel().getNumber());
			minStartTimeStamp_ms = LocalDate.now().minusMonths(analyzer.getSettings().getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
			activeObjectKey = analyzer.getSettings().getActiveObjectKey();
			ignoreLogObjectKey = analyzer.getSettings().getIgnoreLogObjectKey();
			realObjectKeys = analyzer.getSettings().getRealObjectKeys().collect(Collectors.toSet());
		}

		@Override
		public String toString() {
			return "TrussCriteria [channelConfigNumbers=" + this.channelConfigNumbers + ", minStartTimeStamp_ms=" + this.minStartTimeStamp_ms + ", filesWithOtherObject=" + this.ignoreLogObjectKey + ", realObjectKeys=" + this.realObjectKeys + "]";
		}
	}

	private final Analyzer	analyzer;
	/**
	 * Use the getter instead - caching.
	 */
	private TrussCriteria		trussCriteria	= null;

	/**
	 */
	public VaultChecker(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	/**
	 * @return true if the current device supports both directory type and file extension
	 */
	public boolean isWorkableDataSet(Path logFilePath, Set<DirectoryType> directoryTypes, SourceFolders sourceFolders) {
		if (!sourceFolders.isMatchingPath(logFilePath)) {
			log.log(Level.INFO, "not a matching file path      ", logFilePath);
			return false;
		}
		for (DirectoryType directoryType : directoryTypes) {
			List<String> dataSetExtentions = directoryType.getDataSetExtentions(analyzer.getActiveDevice(), analyzer.getSettings());
			if (dataSetExtentions.contains(PathUtils.getFileExtention(logFilePath.getFileName().toString()))) {
				return true;
			}
		}
		log.log(Level.INFO, "not a valid extension         ", logFilePath);
		return false;
	}

	public boolean matchObjectAndStart(HistoVault vault) {
		if (vault.getLogStartTimestamp_ms() < getTrussCriteria().minStartTimeStamp_ms) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match startTime%,7d kiB %s", analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match startTime  %s", vault.getLogFilePath()));
			return false;
		}
		if (!getTrussCriteria().activeObjectKey.isEmpty()) {
			boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(getTrussCriteria().activeObjectKey);
			if (!consistentObjectKey) {
				if (getTrussCriteria().ignoreLogObjectKey) return true;
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match objectKey=%8s%,7d kiB %s", ((ExtendedVault) vault).getRectifiedObjectKey(), analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match objectKey  %s", vault.getLogFilePath()));
				return false;
			}
		}
		return true;
	}

	public boolean matchDeviceChannelObjectAndStart(HistoVault vault) {
		if (analyzer.getActiveDevice() != null && !vault.getLogDeviceName().equals(analyzer.getActiveDevice().getName()) //
		// HoTTViewer V3 -> HoTTViewerAdapter
				&& !(vault.getLogDeviceName().startsWith("HoTTViewer") && analyzer.getActiveDevice().getName().equals("HoTTViewer"))) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match device   %,7d kiB %s", analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match device  %s", vault.getLogFilePath()));
			return false;
		}
		if (!getTrussCriteria().channelConfigNumbers.contains(vault.getLogChannelNumber())) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match channel%2d%,7d kiB %s", vault.getLogChannelNumber(), analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match channel  %s", vault.getLogFilePath()));
			return false;
		}
		if (vault.getLogStartTimestamp_ms() < getTrussCriteria().minStartTimeStamp_ms) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match startTime%,7d kiB %s", analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match startTime  %s", vault.getLogFilePath()));
			return false;
		}

		if (getTrussCriteria().activeObjectKey.isEmpty()) {
			if (!vault.getLogObjectKey().isEmpty()) return true;
			// no object in the osd file is a undesirable case but the log is accepted in order to show all logs
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match objectKey=%8s%,7d kiB %s", "empty", analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match objectKey  %s", vault.getLogFilePath()));
		} else {
			if (vault.getLogObjectKey().isEmpty()) {
				if (getTrussCriteria().ignoreLogObjectKey) return true;
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match objectKey=%8s%,7d kiB %s", "empty", analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match objectKey  %s", vault.getLogFilePath()));
				return false;
			}
			boolean matchingObjectKey = getTrussCriteria().realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(vault.getLogObjectKey().trim()));
			if (!matchingObjectKey) {
				if (getTrussCriteria().ignoreLogObjectKey) return true;
				String objectDirectory = vault instanceof ExtendedVault ? ((ExtendedVault) vault).getLoadObjectDirectory() : vault.getLogObjectDirectory();
				boolean matchingObjectDirectory = getTrussCriteria().realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(objectDirectory));
				if (matchingObjectDirectory) return true;
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match objectKey=%8s%,7d kiB %s", ((ExtendedVault) vault).getRectifiedObjectKey(), analyzer.getDataAccess().getSourceLength(((ExtendedVault) vault).getLoadFileAsPath()) / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match objectKey  %s", vault.getLogFilePath()));
				return false;
			}
			boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(getTrussCriteria().activeObjectKey);
			if (!consistentObjectKey) {
				// finally we found a log with a validated object key which does not correspond to the desired one
				return false;
			}
		}
		return true;
	}

	/**
	 * @param validDirectoryTypes
	 * @param sourceFolders
	 * @return true if the current device supports both directory type and file extension
	 */
	public boolean isValidVault(HistoVault vault, EnumSet<DirectoryType> validDirectoryTypes, SourceFolders sourceFolders) {
		Path logFilePath = Paths.get(vault.getLogFilePath());
		if (isWorkableDataSet(logFilePath, validDirectoryTypes, sourceFolders)) {
			String extention = PathUtils.getFileExtention(Paths.get(vault.getLogFilePath()));
			if (extention.equals(GDE.FILE_ENDING_DOT_OSD))
				return matchDeviceChannelObjectAndStart(vault);
			else if (analyzer.getActiveDevice() instanceof IHistoDevice) {
				return matchObjectAndStart(vault);
			} else {
				return false;
			}
		} else {
			return false;
		}

	}

	private TrussCriteria getTrussCriteria() {
		if (this.trussCriteria == null) setTrussCriteria();
		return this.trussCriteria;
	}

	private void setTrussCriteria() {
		this.trussCriteria = TrussCriteria.createTrussCriteria(analyzer);
	}

}
