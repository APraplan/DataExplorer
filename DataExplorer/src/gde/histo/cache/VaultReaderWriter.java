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

		DataExplorer uses Guava 22.0, released May 22, 2017,
		which is available from https://code.google.com/p/guava-libraries/.
		Guava is subject to the Apache License v. 2.0:
		https://github.com/google/guava/blob/master/COPYING

		Copyright 2017 Google Inc.
    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.cache;

import static java.util.logging.Level.SEVERE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.histo.datasources.SourceDataSet;
import gde.histo.datasources.VaultPicker.ProgressManager;
import gde.histo.datasources.VaultPicker.TrussJobs;
import gde.histo.device.IHistoDevice;
import gde.histo.guard.ObjectVaultIndex.VaultKeyPair;
import gde.histo.innercache.Cache;
import gde.histo.innercache.CacheBuilder;
import gde.histo.innercache.CacheStats;
import gde.log.Level;
import gde.log.Logger;

/**
 * Data access for vaults and trusses.
 * @author Thomas Eickert (USER)
 */
public final class VaultReaderWriter {
	private static final String											$CLASS_NAME			= VaultReaderWriter.class.getName();
	private static final Logger											log							= Logger.getLogger($CLASS_NAME);

	private static final int												MIN_FILE_LENGTH	= 2048;

	private static final Cache<String, HistoVault>	memoryCache			=																		//
			CacheBuilder.newBuilder().maximumSize(4444).recordStats().build();															// key is the vaultName

	/**
	 * Prevents closing.
	 * Closing is implemented by the ZipInputStream after using an entry's ZipInputStream.
	 */
	private static class CloseIgnoringInputStream extends InputStream {
		private ZipInputStream stream;

		public CloseIgnoringInputStream(ZipInputStream inStream) {
			stream = inStream;
		}

		public ZipEntry getNextEntry() throws IOException {
			return stream.getNextEntry();
		}

		@Override
		public int read() throws IOException {
			return stream.read();
		}

		@Override
		public void close() {
			// ignore
		}

		public void reallyClose() throws IOException {
			stream.close();
		}
	}

	private final Analyzer									analyzer;
	private final Optional<ProgressManager>	progress;

	public VaultReaderWriter(Analyzer analyzer, Optional<ProgressManager> progress) {
		this.analyzer = analyzer;
		this.progress = progress;
	}

	/**
	 * Read file and populate the vault from the recordset.
	 */
	public void loadFromFile(Path filePath, List<VaultCollector> trusses) {
		SourceDataSet dataSet = SourceDataSet.createSourceDataSet(filePath, analyzer);
		if (dataSet != null) dataSet.readVaults4Ui(filePath, trusses);
	}

	/**
	 * Read cached vaults and reduce the trussJobs map.
	 * @param trussJobs lists all source files with a map of their vault skeletons (the key vaultFileName prevents double entries)
	 * @return the vaults and trusses loaded from the cache
	 * @throws IOException during opening or traversing the zip file
	 */
	public synchronized List<ExtendedVault> loadFromCaches(TrussJobs trussJobs) //
			throws IOException { // syn due to SAXException: FWK005 parse may not be called while parsing.
		String osdReaderSettings = GDE.STRING_EMPTY;
		List<ExtendedVault> vaults = loadFromCachePath(trussJobs, ExtendedVault.getVaultDirectoryName(analyzer.getActiveDevice(), analyzer.getSettings(), analyzer.getActiveChannel().getNumber(), osdReaderSettings));

		if (analyzer.getActiveDevice() instanceof IHistoDevice) {
			String nativeReaderSettings = ((IHistoDevice) analyzer.getActiveDevice()).getReaderSettingsCsv();
			if (!nativeReaderSettings.equals(osdReaderSettings)) {
				List<ExtendedVault> nativeVaults = loadFromCachePath(trussJobs, ExtendedVault.getVaultDirectoryName(analyzer.getActiveDevice(), analyzer.getSettings(), analyzer.getActiveChannel().getNumber(), nativeReaderSettings));
				vaults.addAll(nativeVaults);
			}
		}
		return vaults;
	}

	private List<ExtendedVault> loadFromCachePath(TrussJobs trussJobs, String vaultDirectoryName) throws IOException {
		List<ExtendedVault> vaults = new ArrayList<>();
		if (!analyzer.getDataAccess().existsCacheDirectory(vaultDirectoryName)) return vaults;

		Function<InputStream, HistoVault> storeKeeper = analyzer.getSettings().isXmlCache() //
				? (stream) -> VaultProxy.load(stream) //
				: (stream) -> VaultProxy.loadJson(stream);
		if (analyzer.getSettings().isZippedCache()) {
			HashMap<String, VaultCollector> vaultNameMap = trussJobs.getAsVaultNameMap();
			try (CloseIgnoringInputStream stream = new CloseIgnoringInputStream(analyzer.getDataAccess().getCacheZipInputStream(vaultDirectoryName))) {
				ZipEntry entry;
				while ((entry = stream.getNextEntry()) != null) {
					String vaultName = entry.getName();
					if (vaultNameMap.containsKey(vaultName)) {
						progress.ifPresent((p) -> p.countInLoop(1));
						try {
							HistoVault histoVault = memoryCache.get(vaultName, () -> storeKeeper.apply(stream));
							VaultCollector truss = vaultNameMap.get(vaultName);
							vaults.add(ExtendedVault.createExtendedVault(histoVault, truss));
							trussJobs.remove(truss);
						} catch (Exception e) {
							log.log(SEVERE, e.getMessage(), e);
						}
					}
				}
				stream.reallyClose();
			}
		} else {
			Iterator<Map.Entry<Path, List<VaultCollector>>> trussJobsIterator = trussJobs.entrySet().iterator();
			while (trussJobsIterator.hasNext()) {
				final List<VaultCollector> map = trussJobsIterator.next().getValue();
				final Iterator<VaultCollector> trussesIterator = map.iterator();
				while (trussesIterator.hasNext()) {
					progress.ifPresent((p) -> p.countInLoop(1));
					VaultCollector truss = trussesIterator.next();
					String vaultName = truss.getVault().vaultName;
					if (analyzer.getDataAccess().existsCacheVault(vaultDirectoryName, vaultName)) {
						HistoVault histoVault = null;
						try (InputStream stream = analyzer.getDataAccess().getCacheInputStream(vaultDirectoryName, vaultName)) {
							histoVault = memoryCache.get(vaultName, () -> storeKeeper.apply(stream));
							vaults.add(ExtendedVault.createExtendedVault(histoVault, truss));
							trussesIterator.remove();
						} catch (Exception e) {
							log.log(SEVERE, e.getMessage(), e);
						}
					}
				}
				if (map.isEmpty()) trussJobsIterator.remove();
			}
		}
		log.fine(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		return vaults;
	}

	/**
	 * Get the zip file name from the history vault class and add all histoset vaults to this file.
	 */
	public void storeInCaches(TrussJobs trussJobs) throws IOException {
		Map<SourceDataSet, List<VaultCollector>> groupedJobs = trussJobs.values().parallelStream().flatMap(Collection::parallelStream).collect(Collectors.groupingBy(VaultCollector::getSourceDataSet));
		groupedJobs.entrySet().stream().forEach(e -> {
			boolean providesReaderSettings = e.getKey().providesReaderSettings();
			String readerSettings = providesReaderSettings && analyzer.getActiveDevice() instanceof IHistoDevice
					? ((IHistoDevice) analyzer.getActiveDevice()).getReaderSettingsCsv() : GDE.STRING_EMPTY;
			String vaultsDirectoryName = ExtendedVault.getVaultDirectoryName(analyzer.getActiveDevice(), analyzer.getSettings(), analyzer.getActiveChannel().getNumber(), readerSettings);
			try {
				storeInCachePath(e.getValue(), vaultsDirectoryName);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	/**
	 * Get the zip file name from the history vault class and add all histoset vaults to this file.
	 * @throws IOException
	 */
	private void storeInCachePath(List<VaultCollector> newVaults, String vaultDirectoryName) throws IOException {
		BiConsumer<HistoVault, OutputStream> storeKeeper = analyzer.getSettings().isXmlCache() //
				? (vault, stream) -> VaultProxy.store(vault, stream) //
				: (vault, stream) -> VaultProxy.storeJson(vault, stream);
		if (analyzer.getSettings().isZippedCache()) {
			// use a zip file system because it supports adding files in contrast to the standard procedure using a ZipOutputStream
			try (FileSystem zipFileSystem = analyzer.getDataAccess().getCacheZipFileSystem(vaultDirectoryName)) {
				for (VaultCollector vaultCollector : newVaults) {
					ExtendedVault vault = vaultCollector.getVault();
					try (OutputStream cacheOutputStream = analyzer.getDataAccess().getCacheZipOutputStream(zipFileSystem, vault.vaultName);
							OutputStream outputStream = new BufferedOutputStream(cacheOutputStream)) {
						storeKeeper.accept(vault, outputStream);
					} catch (Exception e) {
						log.log(SEVERE, e.getMessage(), e);
					}
					memoryCache.put(vault.vaultName, vault);
				}
			}
		} else {
			analyzer.getDataAccess().ensureCacheDirectory(vaultDirectoryName);
			for (VaultCollector vaultCollector : newVaults) {
				ExtendedVault vault = vaultCollector.getVault();
				try (OutputStream cacheOutputStream = analyzer.getDataAccess().getCacheOutputStream(vaultDirectoryName, vault.vaultName);
						OutputStream outputStream = new BufferedOutputStream(cacheOutputStream)) {
					storeKeeper.accept(vault, outputStream);
				} catch (Exception e) {
					log.log(SEVERE, e.getMessage(), e);
				}
				memoryCache.put(vault.vaultName, vault);
			}
		}
	}

	public static long getInnerCacheHitCount() {
		return memoryCache.stats().hitCount();
	}

	/**
	 * @param keyPairs holds the access keys for the cache vaults
	 * @return the extracted vaults
	 */
	@Nullable
	public List<HistoVault> loadFromCaches(List<VaultKeyPair> keyPairs) {
		List<HistoVault> vaults = new ArrayList<>();
		for (VaultKeyPair p : keyPairs) {
			if (p == null) continue;
			String folderName = p.getKey();
			String fileName = p.getValue();
			HistoVault histoVault = memoryCache.getIfPresent(fileName);
			if (histoVault == null) {
				histoVault = analyzer.getSettings().isZippedCache() //
						? analyzer.getDataAccess().getCacheZipVault(folderName, fileName, MIN_FILE_LENGTH, analyzer.getSettings().isXmlCache()) //
						: analyzer.getDataAccess().getCacheVault(folderName, fileName, MIN_FILE_LENGTH, analyzer.getSettings().isXmlCache());
			}
			vaults.add(histoVault);
		}
		log.fine(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		log.log(Level.FINE, "vaults size=", vaults.size());
		return vaults;
	}

	/**
	 * @param directoryName defines the zip file holding the vaults or the vaults folder
	 * @return the extracted vault indices (object key, index data) after eliminating small vault files
	 * @throws IOException during opening or traversing the zip file
	 */
	public List<Entry<String, String>> readVaultsIndices(String directoryName) throws IOException {
		List<Entry<String, String>> vaultExtract = new ArrayList<>();

		Function<InputStream, HistoVault> storeKeeper = analyzer.getSettings().isXmlCache() //
				? (stream) -> VaultProxy.load(stream) //
				: (stream) -> VaultProxy.loadJson(stream);
		if (analyzer.getSettings().isZippedCache()) {
			try (CloseIgnoringInputStream stream = new CloseIgnoringInputStream(analyzer.getDataAccess().getCacheZipInputStream(directoryName))) {
				ZipEntry entry;
				while ((entry = stream.getNextEntry()) != null) {
					if (entry.getSize() <= MIN_FILE_LENGTH) continue;

					try {
						HistoVault histoVault = memoryCache.get(entry.getName(), () -> storeKeeper.apply(stream));
						vaultExtract.add(new AbstractMap.SimpleImmutableEntry<String, String>(histoVault.getVaultObjectKey(), toIndexEntry(histoVault)));
					} catch (Exception e) {
						log.log(SEVERE, e.getMessage(), e);
					}
				}
				stream.reallyClose();
			}
		} else {
			for (String vaultName : analyzer.getDataAccess().getCacheFolderList(directoryName, MIN_FILE_LENGTH)) {
				try (InputStream stream = analyzer.getDataAccess().getCacheInputStream(directoryName, vaultName)) {
					HistoVault histoVault = memoryCache.get(vaultName, () -> storeKeeper.apply(stream));
					vaultExtract.add(new AbstractMap.SimpleImmutableEntry<String, String>(histoVault.getVaultObjectKey(), toIndexEntry(histoVault)));
				} catch (Exception e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		}
		log.fine(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		if (log.isLoggable(Level.FINER)) {
			for (Entry<String, String> entry : vaultExtract) {
				log.log(Level.FINER, entry.getKey(), entry.getValue());
			}
		}
		log.fine(() -> String.format("%s : %s %d", //
				vaultExtract.isEmpty() ? directoryName : vaultExtract.get(0).getKey(), directoryName, vaultExtract.size()));
		return vaultExtract;
	}

	private static String toIndexEntry(HistoVault h) {
		final String d = GDE.STRING_CSV_SEPARATOR;
		String readerSettings = h.vaultReaderSettings.replace(GDE.CHAR_CSV_SEPARATOR, GDE.CHAR_UNDER_BAR);
		String attributes = h.vaultName + d + h.vaultDirectory + d + readerSettings + d + h.vaultCreated_ms //
				+ d + h.vaultDataExplorerVersion + d + h.vaultDeviceKey + d + h.vaultDeviceName + d + h.vaultChannelNumber + d + h.vaultSamplingTimespan_ms //
				+ d + h.logFileLastModified + d + h.logFileLength + d + h.logRecordSetOrdinal + d + h.logRecordsetBaseName //
				+ d + h.logChannelNumber + d + h.logStartTimestamp_ms + d + h.logFilePath;
		return attributes;
	}

}
