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

    Copyright (c) 2017, 2018 Thomas Eickert
    Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
****************************************************************************************/

/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gde.histo.datasources;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;

import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * Watch directories (or directory trees) for changes to files.
 * Decides for each event file if it is valid according to the provided predicate function.
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/notification.html">Watching a Directory for Changes</a>
 */
public final class WatchDir {
	private final static String				$CLASS_NAME					= WatchDir.class.getName();
	private final static Logger				log									= Logger.getLogger($CLASS_NAME);

	public final static long					DELAY								= 222;

	private final WatchService				watcher;
	private final Map<WatchKey, Path>	keys;
	private final boolean							recursive;

	private boolean										trace								= false;
	private boolean										hasChangedLogFiles	= false;
	private Predicate<Path>						logFileFilter;

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Show all added path filenames in the status bar and refresh any active histo window.
	 * Use a scheduled thread with a delay of {@value #DELAY} ms in order to get multiple paths.
	 */
	private final static class DirectoryNotification {
		@SuppressWarnings("hiding")
		private final static String						$CLASS_NAME	= DirectoryNotification.class.getName();
		@SuppressWarnings("hiding")
		private final static Logger						log					= Logger.getLogger($CLASS_NAME);

		private final static int							color				= SWT.COLOR_BLUE;

		private static DirectoryNotification	directoryNotification;

		/**
		 * @param logFilePath is a path to a log file or link file.
		 */
		public static synchronized void addAndShow(Path logFilePath) {
			if (refreshInstance()) {
				// we might wish to inform the user about new files
			}
			directoryNotification.paths.add(logFilePath);
			log.finest(() -> "" + directoryNotification + " " + logFilePath.toString());
		}

		/**
		 * @return true if the instance of the singleton DeferredNotification was created anew.
		 */
		private static synchronized boolean refreshInstance() {
			if (directoryNotification == null || directoryNotification.timerTask.scheduledExecutionTime() < System.currentTimeMillis() - 11) {
				// get new instance if already executed or too close to the scheduled execution time
				directoryNotification = new DirectoryNotification();
				log.log(Level.FINE, "new instance created " + directoryNotification);
				return true;
			}
			return false;
		}

		private TimerTask				timerTask;
		private final Set<Path>	paths	= new HashSet<>();

		private DirectoryNotification() {
			Timer timer = new Timer("timer");
			timerTask = new TimerTask() {
				@Override
				public void run() {
					showStatusMessage(timer);
				}
			};
			timer.schedule(timerTask, DELAY);
		}

		/**
		 * Compile and show message.
		 * Enforce cancel prior to the GC.
		 * @see <a href="https://stackoverflow.com/a/3499062">TimerTask keeps running</a>
		 */
		private void showStatusMessage(Timer timer) {
			String concatenatedNames = paths.stream().map(Path::getFileName).map(Path::toString).sorted().collect(Collectors.joining(" | "));
			DataExplorer.getInstance().getPresentHistoExplorer().setVolatileStatusMessage(Messages.getString(MessageIds.GDE_MSGT0800) + concatenatedNames, color);
			timer.cancel(); // enforce
		}
	}

	/**
	 * Register the given directory with the WatchService.
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the WatchService.
	 */
	private void registerAll(final Path start) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Creates a WatchService and registers the given directories.
	 * @param logFileFilter is a function to apply on the event paths
	 */
	WatchDir(List<Path> dirs, boolean recursive, Predicate<Path> logFileFilter) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		this.recursive = recursive;
		this.logFileFilter = logFileFilter;

		if (recursive) {
			for (Path dir : dirs) {
				if (FileUtils.checkDirectoryExist(dir.toString())) {
					log.log(Level.FINE, "Watching tree ", dir);
					registerAll(dir);
				} else {
					log.log(Level.WARNING, "Watching tree does not exist", dir);
				}
			}
		} else {
			for (Path dir : dirs) {
				if (FileUtils.checkDirectoryExist(dir.toString())) {
					log.log(Level.FINE, "Watching dir ", dir);
					register(dir);
				} else {
					log.log(Level.WARNING, "Watching dir does not exist", dir);
				}
			}
		}
		log.log(Level.FINE, "All paths registered.");

		// enable trace after initial registration
		// this.trace = true;
	}

	/**
	 * Start the watcher and provide the result in {@code hasChangedLogFiles()}.</br>
	 * In detail: Process the keys queued to the watcher.
	 */
	public void processEvents() {
		while (true) {

			// wait for key to be signalled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				try {
					watcher.close();
					log.log(Level.FINER, "terminated by InterruptedException");
				} catch (IOException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();

				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);
				log.finer(() -> String.format("%s: %s", event.kind().name(), child));

				// if directory is created, and watching recursively, then register it and its sub-directories
				if (recursive && (kind == ENTRY_CREATE)) {
					try {
						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
							registerAll(child);
						}
					} catch (IOException x) {
						// ignore to keep sample readable
					}
				}

				boolean isValidChange = Files.isDirectory(child, NOFOLLOW_LINKS) ? true : this.logFileFilter.test(child);
				this.hasChangedLogFiles |= isValidChange;
				if (isValidChange) DirectoryNotification.addAndShow(child);
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
	}

	/**
	 * @return true if there are new or changed log files since the last reset
	 */
	public synchronized boolean hasChangedLogFilesThenReset() {
		boolean result = this.hasChangedLogFiles;
		this.hasChangedLogFiles = false;
		return result;
	}

	/**
	 * @return true if there are new or changed log files since the last reset
	 */
	public boolean hasChangedLogFiles() {
		return this.hasChangedLogFiles;
	}

	/**
	 * @param hasChangedLogFiles false resets the indicator
	 */
	public void setChangedLogFiles(boolean hasChangedLogFiles) {
		this.hasChangedLogFiles = hasChangedLogFiles;
	}

	@Override
	public String toString() {
		return "WatchDir [keys=" + this.keys + ", recursive=" + this.recursive + ", hasChangedLogFiles=" + this.hasChangedLogFiles + "]";
	}
}
