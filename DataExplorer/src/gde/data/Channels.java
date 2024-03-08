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
    							2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.data;

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.histo.ui.HistoExplorer;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Channels class is a map where all possible channels of a device are collected.
 * Is a hybrid singleton supporting cloning.
 * @author Winfried Brügmann
 */
public final class Channels extends HashMap<Integer, Channel> {
	final static long									serialVersionUID				= 26031957;
	final static Logger								log											= Logger.getLogger(Channels.class.getName());

	static final int									CHANNEL_NAME_MIN_LENGTH	= 3;																					// 'GPS'

	private static volatile Channels	channles								= null;

	private int												activeChannelNumber			= 1;																					// default at least one channel must exist
	private String[]									channelNames						= new String[1];

	private Analyzer									analyzer;

	/**
	 * Threadsafe usage.
	 * Be aware of the mandatory setupChannels call.
	 */
	public static Channels createChannels() {
		return new Channels(4);
	}

	/**
	 * getInstance returns the instance of this singleton
	 */
	public static Channels getInstance() {
		if (Channels.channles == null) {
			Channels.channles = new Channels(4);
			// synchronize now to avoid a performance penalty in case of frequent getInstance calls
			synchronized (Channels.class) {
			}
		}
		return Channels.channles;
	}

	/**
	 * singleton
	 * @param initialCapacity
	 */
	private Channels(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Hybrid singleton copy constructor.
	 * Support multiple threads with different Channels instances.
	 * Use if channels updates are not required or apply to the current thread only.
	 * Be aware of the cloning performance impact.
	 */
	public Channels(Channels that) {
		this(4);
		if (that.analyzer == null) throw new IllegalArgumentException("setup is missing");

		// non-UI fields
		this.analyzer  = that.analyzer;
		this.activeChannelNumber = that.activeChannelNumber;
		this.channelNames = that.channelNames.clone();
	}

	/**
	 * query the channel number by given string, if string not found channel number 1 is returned
	 * @param channelName
	 * @return channel number
	 */
	public int getChannelNumber(String channelName) {
		int searchedNumber = 1;

		if (channelName != null && channelName.length() >= CHANNEL_NAME_MIN_LENGTH) {
			// "2 : Outlet", use the first digit to calculate the channel number
			if (channelName.contains(GDE.STRING_COLON) && channelName.split(GDE.STRING_COLON).length >= 1 && Character.isDigit(channelName.split(GDE.STRING_COLON)[0].trim().charAt(0))) {
				return Integer.valueOf(channelName.split(GDE.STRING_COLON)[0].trim());
			} else // old file content "Outlet 2" use the last digit to calculate the channel number
			if (channelName.contains(GDE.STRING_BLANK) && channelName.split(GDE.STRING_BLANK).length > 1 && Character.isDigit(channelName.split(GDE.STRING_BLANK)[1].trim().charAt(0))) {
				try {
					return Integer.valueOf(channelName.split(GDE.STRING_BLANK)[1].trim());
				} catch (NumberFormatException e) {
					if (channelName.split(GDE.STRING_BLANK)[1].trim().contains("+")) {
						String tmpNum = channelName.split(GDE.STRING_BLANK)[1].trim();
						log.log(Level.WARNING, "channel name = " + channelName);
						return Integer.valueOf(tmpNum.substring(0, tmpNum.indexOf(GDE.CHAR_PLUS))) + Integer.valueOf(tmpNum.substring(tmpNum.indexOf(GDE.CHAR_PLUS) + 1));
					}
				}
			} else {
				for (String name : this.getChannelNames()) {
					// try name matching "Outlet"
					if (name.split(GDE.STRING_COLON)[1].trim().equals(channelName) || name.split(GDE.STRING_COLON)[1].trim().split(GDE.STRING_BLANK)[0].trim().equals(channelName)) {
						break;
					}
					++searchedNumber;
				}
			}
		}
		return searchedNumber;
	}

	/**
	 * @return array with channel names
	 */
	public String[] getChannelNames() {
		return this.channelNames;
	}

	/**
	 * @param newChannelNames the channel names to set
	 */
	public void setChannelNames(String[] newChannelNames) {
		this.channelNames = newChannelNames.clone();
	}

	/**
	 * @return array with channel names
	 */
	@Deprecated
	public String getChannelNamesToString() {
		StringBuilder sb = new StringBuilder();
		for (String channelName : DataExplorer.getInstance().getMenuToolBar().getChannelSelectCombo().getItems()) {
			sb.append(channelName.split(GDE.STRING_COLON)[1]).append(", "); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelName assuming p.e. " 1 : Ausgang"
	 */
	public void switchChannel(String channelName) {
		RecordSet recordSet = this.getActiveChannel().getActiveRecordSet();
		if (recordSet != null) recordSet.resetZoomAndMeasurement();

		this.switchChannel(Integer.valueOf(channelName.split(GDE.STRING_COLON)[0].trim()).intValue(), GDE.STRING_EMPTY);
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelNumber 1 -> " 1 : Ausgang"
	 * @param recordSetKey or empty string if switched to first record set
	 */
	public void switchChannel(int channelNumber, String recordSetKey) {
		if (!GDE.isWithUi()) throw new UnsupportedOperationException("for use with internal UI only");
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "switching to channel " + channelNumber); //$NON-NLS-1$
		DataExplorer application = DataExplorer.getInstance();
		application.checkUpdateFileComment();
		application.checkUpdateRecordSetComment();

		if (!(channelNumber > this.keySet().size())) {
			if (channelNumber != this.getActiveChannelNumber() || this.getActiveChannel().getActiveRecordSet() == null) {
				String currentObjectKey = Channels.this.getActiveChannel().getObjectKey();
				this.setActiveChannelNumber(channelNumber);
				if (this.getActiveChannel().getType() == ChannelTypes.TYPE_CONFIG) {
					// switching the channel may change the current object key
					Channels.this.getActiveChannel().setObjectKey(currentObjectKey);
				}
				application.getMenuToolBar().updateChannelToolItems();
				if (recordSetKey == null || recordSetKey.length() < 1)
					this.getActiveChannel().setActiveRecordSet(this.getActiveChannel().getLastActiveRecordSetName());
				else
					this.getActiveChannel().setActiveRecordSet(recordSetKey);

				if (this.getActiveChannel().type == ChannelTypes.TYPE_OUTLET) {
					application.updateTitleBar(application.getObjectKey(), application.getActiveDevice().getName(), application.getActiveDevice().getPort());
				}
				Settings.getInstance().addDeviceUse(application.getActiveDevice().getDeviceConfiguration().getName(), channelNumber); // ok
			} else {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "nothing to do selected channel == active channel"); //$NON-NLS-1$
			}
			application.cleanHeaderAndCommentInGraphicsWindow();
			// application.cleanHeaderAndCommentInHistoGraphicsWindow(); is done in setupHistoWindows
			Channel activeChannel = this.getActiveChannel();
			if (activeChannel != null) {
				RecordSet recordSet = activeChannel.getActiveRecordSet();
				if (recordSet != null) {
					if (!recordSet.hasDisplayableData) {
						recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), true);
					}
					recordSet.resetZoomAndMeasurement();
					if (recordSet.isRecalculation) recordSet.checkAllDisplayable(); // updates graphics window
					recordSet.updateVisibleAndDisplayableRecordsForTable();
				}
				application.resetGraphicsWindowZoomAndMeasurement();
				// update viewable
				// application.getMenuToolBar().updateObjectSelector();
				application.getMenuToolBar().updateChannelSelector();
				application.getMenuToolBar().updateRecordSetSelectCombo();
				application.updateMenusRegardingGPSData();
				application.updateAllTabs(true);

				application.getActiveDevice().setLastChannelNumber(channelNumber);
				if (application.getHistoExplorer().isPresent() != Settings.getInstance().isHistoActive()) { // ok
					// this case may exist during DE startup
				} else if (Settings.getInstance().isHistoActive()) { // ok
					application.getHistoExplorer().ifPresent(HistoExplorer::updateHistoTabs);
				}
			}
		} else
			application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW0006));
	}

	/**
	 * @return the activeChannelNumber
	 */
	public int getActiveChannelNumber() {
		return this.activeChannelNumber;
	}

	/**
	 * @param newActiveChannelNumber the activeChannelNumber to set
	 */
	public void setActiveChannelNumber(int newActiveChannelNumber) {
		this.activeChannelNumber = newActiveChannelNumber;
	}

	/**
	 * @return activeChannel
	 */
	public Channel getActiveChannel() {
		return this.get(this.activeChannelNumber);
	}

	/**
	 * method to cleanup all child and dependent
	 */
	public void cleanup() {
		this.activeChannelNumber = 1; // default at least one channel must exist
		this.channelNames = new String[1];
		try {
			Channel activeChannel = this.getActiveChannel();
			if (activeChannel != null) {
				activeChannel.objectKey = GDE.STRING_EMPTY;
				RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
				if (activeRecordSet != null) {
					activeRecordSet.parent.clear();
				}
			}
		} catch (RuntimeException e) {
			// ignore since this clear operations are not really required
			log.log(Level.WARNING, e.getMessage(), e);
		}
		// use super.size instead of this.size to enable only one channel for multiple channel configurations
		for (int i = 1; i <= super.size(); i++) {
			Channel channel = this.get(i);
			channel.setFileName(GDE.STRING_EMPTY);
			channel.setSaved(false);
			for (int j = 0; j < channel.size(); j++) {
				channel.getRecordSets().clear(); // clear records
			}
			channel.clear(); // clear record set
		}
		this.clear(); // clear channel
		log.log(Level.FINE, "visited"); //$NON-NLS-1$
	}

	/**
	 * method checking all channels has saved record set
	 * @return string array of record sets not saved, length == 0 for all record sets saved
	 */
	public String checkRecordSetsSaved() {
		StringBuffer sb = new StringBuffer();
		// use super.size instead of this.size to enable only one channel for multiple channel configurations
		for (int i = 1; i <= super.size(); i++) {
			Channel channel = this.get(i);
			for (String recordSetkey : channel.getRecordSetNames()) {
				if (channel.get(recordSetkey) != null && !channel.get(recordSetkey).isSaved()) {
					sb.append(System.getProperty("line.separator")).append(channel.getName()).append(" -> ").append(channel.get(recordSetkey).getName()); //$NON-NLS-1$ //$NON-NLS-2$
					if (channel.get(recordSetkey).getUnsaveReasons().size() > 0) {
						sb.append(" ("); //$NON-NLS-1$
						for (String reason : channel.get(recordSetkey).getUnsaveReasons()) {
							sb.append(reason).append(", "); //$NON-NLS-1$
						}
						sb.delete(sb.lastIndexOf(", "), sb.lastIndexOf(", ") + 2).append(")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Buildup new structure  - set up the channels with object key assignments.
	 * todo might be better to do the setup during instantiation
	 */
	public void setupChannels(Analyzer newAnalyzer) {
		this.cleanup();

		this.analyzer = newAnalyzer;

		IDevice device = newAnalyzer.getActiveDevice();
		int channelCount = device.getChannelCount();
		this.channelNames = new String[channelCount];
		for (int i = 1; i <= channelCount; i++) {
			log.log(Level.FINE, "setting up channels = " + i); //$NON-NLS-1$

			Channel newChannel = new Channel(newAnalyzer, device.getChannelNameReplacement(i), device.getChannelTypes(i));
			newChannel.setObjectKey(newAnalyzer.getSettings().getActiveObjectKey());
			// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
			put(Integer.valueOf(i), newChannel);
			// do not call channel.applyTemplate here, there are no record sets
			this.channelNames[i - 1] = i + " : " + device.getChannelNameReplacement(i);
		}
	}

	/**
	 * Use only if the outlet-channel/configuration in log dataset file does not match any existing.
	 * @return the new channel
	 */
	public Channel addChannel(String channelConfigName, ChannelTypes channelType, Analyzer newAnalyzer) {
		this.analyzer = newAnalyzer;

		Channel channel = new Channel(newAnalyzer, channelConfigName, channelType);
		// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
		this.put(channel.getNumber(), channel);
		Vector<String> newChannelNames = new Vector<String>();
		for (String channelConfigKey : this.getChannelNames()) {
			newChannelNames.add(channelConfigKey);
		}
		newChannelNames.add(channel.getNumber() + " : " + channelConfigName); //$NON-NLS-1$
		this.setChannelNames(newChannelNames.toArray(new String[1]));
		return channel;
	}

	/**
	 * synchronize file comment between channels
	 * @param syncChannel the channel which initialize the synchronization
	 * @param updateComment the comment text to synchronize
	 */
	public void syncFileDescription(Channel syncChannel, String updateComment) {
		for (Integer channelNumber : this.keySet()) {
			Channel channel = this.get(channelNumber);
			if (!channel.equals(syncChannel))
				channel.setFileDescription(updateComment);
		}		
	}
}
