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
package gde.data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.Analyzer;
import gde.GDE;
import gde.config.GraphicsTemplate;
import gde.config.Settings;
import gde.device.ChannelTypes;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.RecordSetNameComparator;
import gde.utils.StringHelper;

/**
 * Channel class represents on channel (Ausgang 1, Ausgang 2, ...) where data record sets are accessible (1) laden, 2)Entladen, 1) Flugaufzeichnung, ..)
 * The behavior of this class depends on its type (ChannelTypes.TYPE_OUTLET or ChannelTypes.TYPE_CONFIG)
 * TYPE_OUTLET means that a channel represents exact one object with one view, like a battery
 * TPPE_CONFIG means one objects may have different views, so all channels represent one object
 * @author Winfried Brügmann
 */
public class Channel extends HashMap<String, RecordSet> {
	static final long							serialVersionUID	= 26031957;
	static final Logger						log								= Logger.getLogger(Channel.class.getName());

	private final Analyzer				analyzer;
	final int											number;							// 1
	final String									channelConfigName;	// Ausgang
	String												name;								// 1 : Ausgang
	final ChannelTypes						type;								// ChannelTypes.TYPE_OUTLET or ChannelTypes.TYPE_CONFIG
	private final GraphicsTemplate	template;						// graphics template holds view configuration
	private RecordSet							activeRecordSet;
	private RecordSet							lastActiveRecordSet;
	String												objectKey	= GDE.STRING_EMPTY;
	private String 								fileName;
	private String								fileDescription;
	private boolean								isSaved = false;
	private final Channels				parent;
	Comparator<String> 						comparator = new RecordSetNameComparator();

	public final static String		UNSAVED_REASON_ADD_OBJECT_KEY	= Messages.getString(MessageIds.GDE_MSGT0400);
	public final static String		UNSAVED_REASON_REMOVE_OBJECT_KEY	= Messages.getString(MessageIds.GDE_MSGT0401);
	public final static String		UNSAVED_REASON_CHANGED_OBJECT_DATA	= Messages.getString(MessageIds.GDE_MSGT0402);


	/**
	 * constructor where channel configuration name is used with the channels.ordinal+1 to construct the channel name.
	 * @param useChannelConfigName channelNumber 1 -> " 1 : Ausgang 1"
	 */
	public Channel(Analyzer analyzer, String useChannelConfigName, ChannelTypes channelType) {
		super(4);
		this.analyzer = analyzer;
		this.parent = analyzer.getChannels();
		this.number = this.parent.size() + 1;
		this.channelConfigName = useChannelConfigName;
		this.name = GDE.STRING_BLANK + this.number + GDE.STRING_BLANK_COLON_BLANK + this.channelConfigName;
		this.type = channelType;

		String templateFileName = this.analyzer.getActiveDevice().getName() + GDE.STRING_UNDER_BAR + this.name.split(GDE.STRING_COLON)[0].trim();
		this.template = new GraphicsTemplate(templateFileName);
		this.fileDescription = !this.analyzer.getSettings().getActiveObjectKey().isEmpty()
				? StringHelper.getDate() + GDE.STRING_BLANK + this.analyzer.getSettings().getActiveObjectKey() : StringHelper.getDate();
	}

	/**
	 * overwrites the size method to return faked size in case of channel type is ChannelTypes.TYPE_CONFIG
	 * TYPE_CONFIG means the all record sets depends to the object and the different (configuration) channels enable different views to it
	 */
	@Override
	public int size() {
		int size;
		if(this.type == ChannelTypes.TYPE_OUTLET) {
			size = super.size();
		}
		else { // ChannelTypes.TYPE_CONFIG
			size = 0;
			Channels channels = this.analyzer.getChannels();
			for (Integer channelNumber : this.parent.keySet()) {
				size += channels.get(channelNumber)._size();
			}
		}
		return size;
	}

	/**
	 * query the maximum size of a channel to return faked size in case of channel type is ChannelTypes.TYPE_CONFIG
	 * TYPE_CONFIG means the all record sets depends to the object and the different (configuration) channels enable different views to it
	 */
	public int maxSize() {
		int size;
		if(this.type == ChannelTypes.TYPE_OUTLET) {
			size = super.size();
		}
		else { // ChannelTypes.TYPE_CONFIG
			size = 0;
			Channels channels = this.analyzer.getChannels();
			for (Integer channelNumber : this.parent.keySet()) {
				size = channels.get(channelNumber)._size() > size ? channels.get(channelNumber)._size() : size;
			}
		}
		return size;
	}

	/**
	 * method to get size within channels instance to avoid stack overflow due to never ending recursion
	 */
	private int _size(){
		return super.size();
	}

	/**
	 * method to calculate next record set number, usually a record starts with a number followed by ")"
	 * this method is used to build a new record set name while gathering data "3") flight record
	 * @return next record set number
	 */
	public int getNextRecordSetNumber() {
		int recordNumber = 1;
		if (this.size() != 0) {
			String[] sortedRecordSetNames = this.getRecordSetNames();
			for (int i = sortedRecordSetNames.length - 1; i >= 0; --i) {
				try {
					recordNumber = Integer.parseInt(sortedRecordSetNames[i].split("[)]")[0]) + 1; //$NON-NLS-1$
					break;
				}
				catch (NumberFormatException e) {
					// is alpha no numeric or no ")"
				}
			}
		}
		else
			recordNumber = 1;

		return recordNumber;
	}

	/**
	 * method to calculate next record set number, usually a record starts with a number followed by ")"
	 * this method is used to build a new record set name while gathering data "3") flight record
	 * @return next record set number
	 */
	public int getNextRecordSetNumber(int availableNum) {
		Vector<Integer> sortedNumbers = new Vector<Integer>(this.size());
		if (this.size() != 0) {
			String[] sortedRecordSetNames = this.getRecordSetNames();
			for (String sortedRecordSetName : sortedRecordSetNames) {
				try {
					sortedNumbers.add(Integer.parseInt(sortedRecordSetName.split("[)]")[0])); //$NON-NLS-1$
				}
				catch (NumberFormatException e) {
					// is alpha no numeric or no ")"
				}
			}
		}

		return !sortedNumbers.contains(availableNum) ? availableNum : getNextRecordSetNumber();
	}

	/**
	 * @return the graphics template
	 */
	public GraphicsTemplate getTemplate() {
		return this.template;
	}

	/**
	 * method to get the record set names "1) Laden, 2) Entladen, ...".
	 * the behavior of this method depends on this.type (ChannelTypes.TYPE_OUTLET or ChannelTypes.TYPE_CONFIG)
	 * TYPE_OUTLET means that a channel represents exact one object, like a battery
	 * TPPE_CONFIG measn one objects has different views, so this method returns all record set names for all channels
	 * @return String[] containing the records names
	 */
	public synchronized String[] getRecordSetNames() {
		String[] keys;
		if(this.type == ChannelTypes.TYPE_OUTLET) {
			keys = this.keySet().toArray( new String[1]);
		}
		else { // ChannelTypes.TYPE_CONFIG
			Channels channels = this.analyzer.getChannels();
			Vector<String> namesVector = new Vector<String>();
			synchronized (channels) {
				for (int i = 1; i <= channels.size(); ++i) {
					String[] recordSetNames = channels.get(i).getUnsortedRecordSetNames();
					for (String recordSetName : recordSetNames) {
						if (recordSetName != null) namesVector.add(recordSetName);
					}
				}
			}
			keys = namesVector.toArray(new String[1]);
		}
		Arrays.sort(keys, this.comparator);
		return keys;
	}

	/**
	 * method to get unsorted recordNames within channels instance to avoid stack overflow due to never ending recursion
	 * @return String[] containing the records names
	 */
	public String[] getUnsortedRecordSetNames() {
		return this.keySet().toArray( new String[1]);
	}

	/**
	 * query the last displayed record set name, in case of ChannelTypes.TYPE_CONFIG the last used entry is returned
	 */
	public String getLastActiveRecordSetName() {
//		if (this.type == ChannelTypes.TYPE_CONFIG && this.keySet() != null)
//			return this.keySet().toArray(new String[1])[0];
//
		return this.lastActiveRecordSet == null ? this.getFirstRecordSetName() : this.lastActiveRecordSet.name;
	}

	/**
	 * set the last used record set
	 * @param lastRecordSet
	 */
	public void setLastActiveRecordSet(RecordSet lastRecordSet) {
		this.lastActiveRecordSet = lastRecordSet;
	}

	/**
	 * query the first record set name, in case of ChannelTypes.TYPE_CONFIG the first entry of keySet might returned
	 */
	public String getFirstRecordSetName() {
		if (this.type == ChannelTypes.TYPE_CONFIG && this.keySet() != null)
			return this.keySet().toArray(new String[1])[0];

		return this.getRecordSetNames()[0];
	}

	/**
	 * get the name of the channel " 1: Ausgang"
	 * @return String
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * get the name of the channel to be used as configuration key " 1: Ausgang" -> "Ausgang"
	 * @return String
	 */
	public String getChannelConfigKey() {
		return this.channelConfigName;
	}

	/**
	 * method to get all the record sets of this channel
	 * @return HashMap<Integer, Records>
	 */
	public synchronized HashMap<String, RecordSet> getRecordSets() {
		HashMap<String, RecordSet> content = new HashMap<String, RecordSet>(this.size());
		if(this.type == ChannelTypes.TYPE_OUTLET) {
			for (String key : this.getRecordSetNames()) {
				content.put(key, this.get(key));
			}
		}
		else { // ChannelTypes.TYPE_CONFIG
			Channels channels = this.analyzer.getChannels();
			synchronized (channels) {
				for (int i = 1; i <= channels.size(); ++i) {
					for (String key : channels.get(i).getUnsortedRecordSetNames()) {
						if (key != null && key.length() > 1) content.put(key, channels.get(i).get(key));
					}
				}
			}
		}
		return content;
	}

	/**
	 * method to save the graphics definition into template file
	 */
	public void saveTemplate() {
		final RecordSet recordSet = this.getActiveRecordSet();

		if (recordSet != null) {
			for (int i=0; i<recordSet.size(); ++i) {
				Record record = recordSet.get(i);
				this.template.setProperty(i + Record.IS_VISIBLE, Boolean.valueOf(record.isVisible()).toString());
				this.template.setProperty(i + Record.IS_POSITION_LEFT, Boolean.valueOf(record.isPositionLeft()).toString());
				this.template.setProperty(i + Record.COLOR, record.getRGB());
				this.template.setProperty(i + Record.LINE_WITH, Integer.valueOf(record.getLineWidth()).toString());
				this.template.setProperty(i + Record.LINE_STYLE, Integer.valueOf(record.getLineStyle()).toString());
				this.template.setProperty(i + Record.IS_ROUND_OUT, Boolean.valueOf(record.isRoundOut()).toString());
				this.template.setProperty(i + Record.IS_START_POINT_ZERO, Boolean.valueOf(record.isStartpointZero()).toString());
				this.template.setProperty(i + Record.NUMBER_FORMAT, Integer.valueOf(record.getNumberFormat()).toString());
				this.template.setProperty(i + Record.IS_START_END_DEFINED, Boolean.valueOf(record.isStartEndDefined()).toString());
				this.template.setProperty(i + Record.DEFINED_MAX_VALUE, Double.valueOf(record.getMaxScaleValue()).toString());
				this.template.setProperty(i + Record.DEFINED_MIN_VALUE, Double.valueOf(record.getMinScaleValue()).toString());
			}
			//smooth current drop
			this.template.setProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, Boolean.toString(recordSet.isSmoothAtCurrentDrop()));
			recordSet.setSmoothAtCurrentDrop(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
			//smooth voltage curve
			this.template.setProperty(RecordSet.SMOOTH_VOLTAGE_CURVE, Boolean.toString(recordSet.isSmoothVoltageCurve()));
			recordSet.setSmoothVoltageCurve(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_VOLTAGE_CURVE, "false"))); //$NON-NLS-1$
			// time grid
			Color color = recordSet.getColorTimeGrid();
			String rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
			this.template.setProperty(RecordSet.TIME_GRID_COLOR, rgb);
			this.template.setProperty(RecordSet.TIME_GRID_LINE_STYLE, Integer.valueOf(recordSet.getLineStyleTimeGrid()).toString());
			this.template.setProperty(RecordSet.TIME_GRID_TYPE, Integer.valueOf(recordSet.getTimeGridType()).toString());
			// curve grid
			color = recordSet.getValueGridColor();
			rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
			this.template.setProperty(RecordSet.VALUE_GRID_COLOR, rgb);
			this.template.setProperty(RecordSet.VALUE_GRID_LINE_STYLE, Integer.valueOf(recordSet.getValueGridLineStyle()).toString());
			this.template.setProperty(RecordSet.VALUE_GRID_TYPE, Integer.valueOf(recordSet.getValueGridType()).toString());
			if (recordSet.get(recordSet.getValueGridRecordOrdinal()) != null) {
				this.template.setProperty(RecordSet.VALUE_GRID_RECORD_ORDINAL, GDE.STRING_EMPTY + recordSet.getValueGridRecordOrdinal());
			}
			this.template.store();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "creating graphics template file " + Settings.getApplHomePath() + GDE.STRING_FILE_SEPARATOR_UNIX + this.getActiveRecordSet().getName() + this.name); //$NON-NLS-1$
		}
	}

	/**
	 * method to apply the graphics template definition colors to an record set.
	 * Not applicable for TrailRecordSets!
	 */
	public void applyTemplateBasics(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);
		this.activeRecordSet = this.analyzer.getActiveChannel() != null ? this.analyzer.getActiveChannel().getActiveRecordSet() : null;
		if (recordSet != null) {
			if (log.isLoggable(Level.FINE) && this.get(this.getLastActiveRecordSetName()) != null && this.get(recordSetKey) != null)
				log.log(Level.FINE, "this.size() > 1 " + (this.size() > 1) + "; this.lastActiveRecordSet = " + this.getLastActiveRecordSetName() +  " - " + this.get(this.getLastActiveRecordSetName()).getChannelConfigNumber() + "!=" +  this.get(recordSetKey).getChannelConfigNumber());
			if (this.size() <= 1 || (this.get(this.getLastActiveRecordSetName()) != null && this.get(recordSetKey) != null && this.type == ChannelTypes.TYPE_CONFIG && this.get(this.getLastActiveRecordSetName()).getChannelConfigNumber() != this.get(recordSetKey).getChannelConfigNumber())) { //apply values from template
				if (this.template != null) this.template.load();
				if (this.template != null && this.template.isAvailable()) {
					log.log(Level.FINER, "name = " + this.template.getDefaultFileName());
					int r, g, b;
					for (int i = 0; i < recordSet.realSize(); ++i) {
						Record record = recordSet.get(i);
						record.setVisible(Boolean.valueOf(this.template.getProperty(i + Record.IS_VISIBLE, "true"))); //$NON-NLS-1$
						record.setPositionLeft(Boolean.valueOf(this.template.getProperty(i + Record.IS_POSITION_LEFT, "true"))); //$NON-NLS-1$
						String color = this.template.getProperty(i + Record.COLOR, record.getRGB());
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						record.setColor(SWTResourceManager.getColor(r, g, b));
						record.setLineWidth(Integer.valueOf(this.template.getProperty(i + Record.LINE_WITH, "1")).intValue()); //$NON-NLS-1$
						record.setLineStyle(Integer.valueOf(this.template.getProperty(i + Record.LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_SOLID)).intValue());
						record.setRoundOut(Boolean.valueOf(this.template.getProperty(i + Record.IS_ROUND_OUT, "false"))); //$NON-NLS-1$
						record.setStartpointZero(Boolean.valueOf(this.template.getProperty(i + Record.IS_START_POINT_ZERO, "false"))); //$NON-NLS-1$
						record.setStartEndDefined(Boolean.valueOf(this.template.getProperty(i + Record.IS_START_END_DEFINED, "false")), //$NON-NLS-1$ 
								Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MIN_VALUE, "0")), //$NON-NLS-1$ 
								Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MAX_VALUE, "100"))); //$NON-NLS-1$
						record.setNumberFormat(Integer.valueOf(this.template.getProperty(i + Record.NUMBER_FORMAT, "-1")).intValue()); //$NON-NLS-1$
					}
					//smooth current drop
					recordSet.setSmoothAtCurrentDrop(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
					//smooth voltage curve
					recordSet.setSmoothVoltageCurve(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_VOLTAGE_CURVE, "false"))); //$NON-NLS-1$
					// time grid
					String color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
					recordSet.setTimeGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
					recordSet.setTimeGridType(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
					// curve grid
					color = this.template.getProperty(RecordSet.VALUE_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					recordSet.setValueGridColor(SWTResourceManager.getColor(r, g, b));
					recordSet.setValueGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.VALUE_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
					recordSet.setValueGridType(Integer.valueOf(this.template.getProperty(RecordSet.VALUE_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
					recordSet.setValueGridRecordOrdinal(Integer.valueOf(this.template.getProperty(RecordSet.VALUE_GRID_RECORD_ORDINAL, "-1")).intValue()); //$NON-NLS-1$
				}
			}
			else { //take over values from last active record set
				RecordSet _lastActiveRecordSet = this.get(this.getLastActiveRecordSetName());
				int _lastActiveRecordSetSize = _lastActiveRecordSet.realSize();
				for (int i = 0; i < recordSet.realSize(); ++i) {
					Record record = recordSet.get(i);
					if (i < _lastActiveRecordSetSize) {
					Record lastActiveRecord = _lastActiveRecordSet.get(i);
					if(log.isLoggable(Level.FINER)) log.log(Level.FINER, "lastActiveRecord = " + lastActiveRecord.name + " isVisible=" + lastActiveRecord.isVisible + " isPositionLeft=" + lastActiveRecord.isPositionLeft + " isStartpointZero=" + lastActiveRecord.isStartpointZero);
						record.setVisible(lastActiveRecord.isVisible);
						record.setPositionLeft(lastActiveRecord.isPositionLeft);
						record.setRGB(lastActiveRecord.getRGB());
						record.setLineWidth(lastActiveRecord.lineWidth);
						record.setLineStyle(lastActiveRecord.lineStyle);
						record.setRoundOut(lastActiveRecord.isRoundOut);
						record.setStartpointZero(lastActiveRecord.isStartpointZero);
						record.setStartEndDefined(lastActiveRecord.isStartEndDefined, lastActiveRecord.getMinScaleValue(), lastActiveRecord.getMaxScaleValue());
						record.setNumberFormat(lastActiveRecord.numberFormat);
					}
					RecordSet lastActiveParent = _lastActiveRecordSet;
					//smooth current drop
					recordSet.setSmoothAtCurrentDrop(lastActiveParent.isSmoothAtCurrentDrop);
					//smooth voltage curve
					recordSet.setSmoothVoltageCurve(lastActiveParent.isSmoothVoltageCurve);
					// time grid
					recordSet.setTimeGridColor(lastActiveParent.timeGridColor);
					recordSet.setTimeGridLineStyle(lastActiveParent.timeGridLineStyle);
					recordSet.setTimeGridType(lastActiveParent.timeGridType);
					// curve grid
					recordSet.setValueGridColor(lastActiveParent.valueGridColor);
					recordSet.setValueGridLineStyle(lastActiveParent.valueGridLineStyle);
					recordSet.setValueGridType(lastActiveParent.valueGridType);
					recordSet.setValueGridRecordOrdinal(lastActiveParent.valueGridRecordOrdinal >= 0 ? lastActiveParent.valueGridRecordOrdinal : 0);
					if(log.isLoggable(Level.FINER)) log.log(Level.FINER, "record = " + record.name + " isVisible=" + record.isVisible + " isPositionLeft=" + record.isPositionLeft + " isStartpointZero=" + record.isStartpointZero);
				}
			}

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "applied graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (this.activeRecordSet != null && recordSet.getName().equals(this.activeRecordSet.name) && GDE.isWithUi()) {
				DataExplorer.getInstance().updateGraphicsWindow();
			}
		}
	}

	/**
	 * method to apply the graphics template definition to an record set
	 * @param recordSetKey
	 * @param doUpdateVisibilityStatus
	 */
	public void applyTemplate(String recordSetKey, boolean doUpdateVisibilityStatus) {
		RecordSet recordSet = this.get(recordSetKey);
		this.activeRecordSet = this.analyzer.getActiveChannel() != null ? this.analyzer.getActiveChannel().getActiveRecordSet() : null;
		if (recordSet != null) {
			if (this.template != null) this.template.load();
			int r, g, b;
			if (this.template != null && this.template.isAvailable()) {
				for (int i = 0; i < recordSet.size(); ++i) {
					Record record = recordSet.get(i);
					record.setVisible(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_VISIBLE, "true"))); //$NON-NLS-1$
					record.setPositionLeft(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_POSITION_LEFT, "true"))); //$NON-NLS-1$
					String color = this.template.getProperty(i + Record.COLOR, record.getRGB());
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					record.setColor(SWTResourceManager.getColor(r, g, b));
					record.setLineWidth(Integer.parseInt(this.template.getProperty(i + Record.LINE_WITH, "1"))); //$NON-NLS-1$
					record.setLineStyle(Integer.parseInt(this.template.getProperty(i + Record.LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_SOLID)));
					record.setRoundOut(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_ROUND_OUT, "false"))); //$NON-NLS-1$
					record.setStartpointZero(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_START_POINT_ZERO, "false"))); //$NON-NLS-1$
					record.setStartEndDefined(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_START_END_DEFINED, "false")), //$NON-NLS-1$  
							Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MIN_VALUE, "0")), //$NON-NLS-1$ 
							Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MAX_VALUE, "100"))); //$NON-NLS-1$
					record.setNumberFormat(Integer.parseInt(this.template.getProperty(i + Record.NUMBER_FORMAT, "-1"))); //$NON-NLS-1$
				}
				//smooth current drop
				recordSet.setSmoothAtCurrentDrop(Boolean.parseBoolean(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
				//smooth voltage curve
				recordSet.setSmoothVoltageCurve(Boolean.parseBoolean(this.template.getProperty(RecordSet.SMOOTH_VOLTAGE_CURVE, "false"))); //$NON-NLS-1$
				// time grid
				String color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
				g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
				b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
				recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setTimeGridLineStyle(Integer.parseInt(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)));
				recordSet.setTimeGridType(Integer.parseInt(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0"))); //$NON-NLS-1$
				// curve grid
				color = this.template.getProperty(RecordSet.VALUE_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
				g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
				b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
				recordSet.setValueGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setValueGridLineStyle(Integer.parseInt(this.template.getProperty(RecordSet.VALUE_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)));
				recordSet.setValueGridType(Integer.parseInt(this.template.getProperty(RecordSet.VALUE_GRID_TYPE, "0"))); //$NON-NLS-1$
				int gridRecordOrdinal = Integer.parseInt(this.template.getProperty(RecordSet.VALUE_GRID_RECORD_ORDINAL, "-1"));
				if (gridRecordOrdinal >= 0 && gridRecordOrdinal < recordSet.realSize() && recordSet.get(gridRecordOrdinal).isVisible) {
					recordSet.setValueGridRecordOrdinal(gridRecordOrdinal);
				}
				else {
					recordSet.setValueGridRecordOrdinal(findFirstVisibleRecord(recordSet));
				}
				recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "applied graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
				if (doUpdateVisibilityStatus) {
					recordSet.device.updateVisibilityStatus(recordSet, false);
				}
				if (this.activeRecordSet != null && recordSet.getName().equals(this.activeRecordSet.name) && GDE.isWithUi()) {
					DataExplorer.getInstance().updateGraphicsWindow();
				}
			}
		}
	}

	/**
	 * @param recordSet
	 * @return the first visible recordSet record ordinal if horizontal grid is not defined
	 */
	protected int findFirstVisibleRecord(RecordSet recordSet) {
		int j = 0;
		for (; j < recordSet.size(); j++) {
			if (recordSet.get(j).isVisible)
				break;
		}
		return j;
	}

	/**
	 * method to reset scale end point definition to an record set switching back from 10 scale ticks
	 * @param recordSetKey
	 */
	public void applyTemplateScaleEndpoints(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);
		this.activeRecordSet = this.analyzer.getActiveChannel() != null ? this.analyzer.getActiveChannel().getActiveRecordSet() : null;
		if (recordSet != null) {
			if (this.template != null && this.template.isAvailable()) {
				for (int i = 0; i < recordSet.size(); ++i) {
					Record record = recordSet.get(i);
					record.setRoundOut(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_ROUND_OUT, "false"))); //$NON-NLS-1$
				}

				int gridRecordOrdinal = Integer.parseInt(this.template.getProperty(RecordSet.VALUE_GRID_RECORD_ORDINAL, "-1"));
				if (gridRecordOrdinal >= 0 && gridRecordOrdinal < recordSet.realSize() && recordSet.get(gridRecordOrdinal).isVisible) {
					recordSet.setValueGridRecordOrdinal(gridRecordOrdinal);
				}
				else {
					recordSet.setValueGridRecordOrdinal(findFirstVisibleRecord(recordSet));
				}
			}
			if (this.activeRecordSet != null && recordSet.getName().equals(this.activeRecordSet.name) && GDE.isWithUi()) {
				DataExplorer.getInstance().updateGraphicsWindow();
			}
		}
	}
	
	/**
	 * remove active record set and records
	 * @param deleteRecordSetName
	 */
	public void remove(String deleteRecordSetName) {
		super.remove(deleteRecordSetName);
		if (this.size() == 0) this.activeRecordSet = null;
		else this.activeRecordSet = this.get(this.getRecordSetNames()[0]);
	}

	/**
	 * @return the activeRecordSet
	 */
	public RecordSet getActiveRecordSet() {
		return this.activeRecordSet;
	}

	/**
	 * @param recordSetKey of the activeRecordSet to set
	 */
	public void setActiveRecordSet(String recordSetKey) {
		DataExplorer.getInstance().checkUpdateFileComment();
		DataExplorer.getInstance().checkUpdateRecordSetComment();

		RecordSet newActiveRecordSet = this.get(recordSetKey);
		if (newActiveRecordSet != null) {
			this.activeRecordSet = newActiveRecordSet;
		}
	}

	/**
	 * @param newActiveRecordSet to set
	 */
	public void setActiveRecordSet(RecordSet newActiveRecordSet) {
		this.activeRecordSet = newActiveRecordSet;
	}

	/**
	 * switch the record set according selection and set applications active channel
	 * @param recordSetName p.e. "1) Laden"
	 */
	public synchronized void switchRecordSet(String recordSetName) {
		if (!GDE.isWithUi()) throw new UnsupportedOperationException("for use with internal UI only");
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("switching to record set threadId = %06d", Thread.currentThread().getId())); //$NON-NLS-1$
		DataExplorer application = DataExplorer.getInstance();
		int percentage = application.getProgressPercentage();
		if (percentage > 99 || percentage == 0)
			application.setProgress(0, null);

		final String recordSetKey = recordSetName;
		this.lastActiveRecordSet = this.get(recordSetKey);
		if (Thread.currentThread().getId() == application.getThreadId()) {
			updateForSwitchRecordSet(application, recordSetKey);
		}
		else { // execute asynchronous
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					updateForSwitchRecordSet(application, recordSetKey);
				}
			});
		}
	}

	/**
	 * @param recordSetKey
	 */
	private void updateForSwitchRecordSet(DataExplorer application, String recordSetKey) {
		//reset old record set before switching
		RecordSet oldRecordSet = this.getActiveRecordSet();
		if (oldRecordSet != null) oldRecordSet.resetZoomAndMeasurement();

		RecordSet recordSet = this.get(recordSetKey);
		if (recordSet == null) { //activeChannel do not have this record set, try to switch
			int channelNumber = this.findChannelOfRecordSet(recordSetKey);
			if (channelNumber > 0) {
				this.analyzer.getChannels().switchChannel(channelNumber, recordSetKey);
				recordSet = this.get(recordSetKey);
				if (recordSet != null && recordSet.isRecalculation)
					recordSet.checkAllDisplayable();
			}
		}
		else { // record  set exist
			this.setActiveRecordSet(recordSetKey);
			if (!recordSet.hasDisplayableData)
				recordSet.loadFileData(this.getFullQualifiedFileName(), true);
			recordSet.resetZoomAndMeasurement();
			application.resetGraphicsWindowZoomAndMeasurement();
			if (recordSet.isRecalculation)
				recordSet.checkAllDisplayable(); // updates graphics window

			application.getMenuToolBar().updateRecordSetSelectCombo();
			application.updateMenusRegardingGPSData();
			application.cleanHeaderAndCommentInGraphicsWindow();
			application.updateAllTabs(true);
		}
	}

	/**
	 * search through all channels/configurations for the channel which owns a record set with the given key
	 * @param recordSetKey
	 * @return 0 if record set does not exist
	 */
	public int findChannelOfRecordSet(String recordSetKey) {
		int channelNumber = 0;
		Channels channels = this.analyzer.getChannels();
		for (Integer tmpNumber : channels.keySet()) {
			Channel channel = channels.get(tmpNumber);
			if (channel.get(recordSetKey) != null) {
				channelNumber = tmpNumber.intValue();
			}
		}
		return channelNumber;
	}

	/**
	 * @return the type as ordinal
	 */
	public ChannelTypes getType() {
		return this.type;
	}

	/**
	 * @param newName the name to set
	 */
	public void setName(String newName) {
		this.name = newName;
	}

	public String getFileName() {
		return this.fileName!= null ? this.fileName.substring(this.fileName.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX)+1) : null;
	}

	public String getFullQualifiedFileName() {
		return this.fileName;
	}

	public void setFileName(String newFileName) {
		if(this.type == ChannelTypes.TYPE_CONFIG) {
			Channels channels = this.analyzer.getChannels();
			for (int i = 1; i<= channels.getChannelNames().length; ++i) {
				channels.get(i).fileName = newFileName;
			}
		}
		else {
			this.fileName = newFileName;
		}
		if (this.fileName != null && this.analyzer.getActiveDevice() != null) DataExplorer.getInstance().updateTitleBar(this.analyzer.getSettings().getActiveObjectKey(), this.analyzer.getActiveDevice().getName(), this.analyzer.getActiveDevice().getPort());
	}


	public String getFileDescription() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("Channel %s get description %s", this.getName(), this.fileDescription));
		return this.fileDescription;
	}

	public void setFileDescription(String newFileDescription) {
		this.setUnsaved(RecordSet.UNSAVED_REASON_COMMENT);
		this.fileDescription = newFileDescription;
		if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("Channel %s set description %s", this.getName(), newFileDescription));
		DataExplorer.getInstance().updateGraphicsCaptions();
	}
	
	/**
	 * synchronize file comment between channels
	 * @param syncChannel the channel which initialize the synchronization
	 * @param updateComment the comment text to synchronize
	 */
	public void syncFileDescription(Channel syncChannel, String updateComment) {
		this.parent.syncFileDescription(syncChannel, updateComment);
	}


	public boolean isSaved() {
		return this.isSaved;
	}

	public void setSaved(boolean is_saved) {
		if(this.type == ChannelTypes.TYPE_CONFIG) {
			Channels channels = this.analyzer.getChannels();
			for (int i = 1; i<= channels.getChannelNames().length; ++i) {
				channels.get(i).isSaved = is_saved;
			}
		}
		else {
			this.isSaved = is_saved;
		}
	}

	/**
	 * set a unsaved reason marker to enable unsaved data warning
	 * valid arguments are UNSAVED_REASON_ADD_OBJECT_KEY, UNSAVED_REASON_REMOVE_OBJECT_KEY, UNSAVED_REASON_CHANGED_OBJECT_DATA
	 * @param unsavedReason
	 */
	public void setUnsaved(String unsavedReason) {
		this.activeRecordSet = this.getActiveRecordSet();
		if (this.activeRecordSet != null) {
			this.activeRecordSet.setUnsaved(unsavedReason);
		}
	}


	/**
	 * check if all record sets have its data loaded, if required load data from file
	 * this method can be used to check prior to save modified data
	 * the behavior which record set data is checked and loaded depends on the method this.getRecordSetNames()
	 */
	public synchronized void checkAndLoadData() {
		String fullQualifiedFileName = this.getFullQualifiedFileName();
		for (String tmpRecordSetName : this.getRecordSetNames()) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "tmpRecordSetName = " + tmpRecordSetName); //$NON-NLS-1$
			Channel activeChannel = this.analyzer.getActiveChannel();
			if (activeChannel != null) {
				//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
				//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
				Channel selectedChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : this.analyzer.getChannels().get(this.findChannelOfRecordSet(tmpRecordSetName));
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "selectedChannel = " + (selectedChannel != null ? selectedChannel.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
				if (selectedChannel != null) {
					RecordSet tmpRecordSet = selectedChannel.get(tmpRecordSetName);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "tmpRecordSet = " + (tmpRecordSet != null ? tmpRecordSet.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
					if (tmpRecordSet != null && !tmpRecordSet.hasDisplayableData()) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "tmpRecordSetName needs data to loaded"); //$NON-NLS-1$
						if (tmpRecordSet.fileDataSize != 0 && tmpRecordSet.fileDataPointer != 0) {
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "loading data ..."); //$NON-NLS-1$
							tmpRecordSet.loadFileData(fullQualifiedFileName, GDE.isWithUi());
						}
					}
				}
			}
		}
	}

	/**
	 * @return the channel/config number
	 */
	public int getNumber() {
		return this.number;
	}

	public String getObjectKey() {
		return this.objectKey;
	}

	public void setObjectKey(String newObjectKey) {
		if (!this.objectKey.equals(newObjectKey)) {
			this.objectKey = newObjectKey;
			if (this.activeRecordSet != null) {
				if (newObjectKey.equals(GDE.STRING_EMPTY))
					this.activeRecordSet.setUnsaved(Channel.UNSAVED_REASON_REMOVE_OBJECT_KEY);
				else
					this.activeRecordSet.setUnsaved(Channel.UNSAVED_REASON_ADD_OBJECT_KEY);
			}
		}
	}

	/**
	 * overloaded clear method to enable implementation specific clear actions
	 */
	@Override
	public void clear() {
		for (String recordSetKey : this.getRecordSetNames()) {
			if (recordSetKey != null && recordSetKey.length() > 3) this.remove(recordSetKey);
		}

		super.clear();
		this.objectKey = GDE.STRING_EMPTY;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int hashCode = super.hashCode();
		hashCode = prime * hashCode + ((this.name == null) ? 0 : this.name.hashCode());
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		Channel other = (Channel) obj;
		if (this.name == null) {
			if (other.name != null) return false;
		} else if (!this.name.equals(other.name)) {
			return false;
		} else {
			super.equals(other);
		}
		return true;
	}
}

