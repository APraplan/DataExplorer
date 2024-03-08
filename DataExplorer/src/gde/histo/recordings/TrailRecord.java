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

package gde.histo.recordings;

import static java.util.logging.Level.FINER;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.stream.DoubleStream;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.data.CommonRecord;
import gde.data.Record;
import gde.data.Record.DataType;
import gde.device.IChannelItem;
import gde.device.MeasurementType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.config.HistoGraphicsTemplate;
import gde.histo.datasources.HistoSet;
import gde.histo.guard.Guardian;
import gde.histo.guard.Reminder;
import gde.histo.utils.ElementaryQuantile;
import gde.histo.utils.Spot;
import gde.log.Logger;
import gde.utils.ColorUtils;

/**
 * Hold histo data points of one measurement or settlement; score points are a third option.
 * A histo data point holds one aggregated value (e.g. max, avg, quantile).
 * Supports suites, i.e. records for multiple trails for combined curves.
 * @author Thomas Eickert
 */
public abstract class TrailRecord extends CommonRecord {
	private static final String	$CLASS_NAME				= TrailRecord.class.getName();
	private static final long		serialVersionUID	= 110124007964748556L;
	private static final Logger	log								= Logger.getLogger($CLASS_NAME);

	/**
	 * Provide the chart template properties.
	 */
	public static class ChartTemplate {

		boolean				isVisible					= true;
		boolean				isPositionLeft		= true;
		String				rgb								= "0,0,0";
		int						lineWidth					= 1;
		int						lineStyle					= SWT.LINE_SOLID;
		boolean				isRoundOut				= false;
		boolean				isStartpointZero	= false;
		boolean				isStartEndDefined	= false;
		DecimalFormat	df								= new DecimalFormat("0.0");
		int						numberFormat			= -1;																						// -1 = automatic, 0 = 0000, 1 = 000.0, 2 = 00.00
		double				maxScaleValue			= 0.;																						// overwrite calculated boundaries
		double				minScaleValue			= 0.;
		int						trailTextOrdinal	= -1;

		/**
		 * @param template holds all values
		 */
		void saveTemplate(HistoGraphicsTemplate template, TrailRecord record) {
			String recordName = record.getName();
			template.setRecordProperty(recordName, Record.IS_VISIBLE, String.valueOf(isVisible));
			template.setRecordProperty(recordName, Record.IS_POSITION_LEFT, String.valueOf(isPositionLeft));
			template.setRecordProperty(recordName, Record.COLOR, rgb);
			template.setRecordProperty(recordName, Record.LINE_WITH, String.valueOf(lineWidth));
			template.setRecordProperty(recordName, Record.LINE_STYLE, String.valueOf(lineStyle));
			template.setRecordProperty(recordName, Record.IS_ROUND_OUT, String.valueOf(isRoundOut));
			template.setRecordProperty(recordName, Record.IS_START_POINT_ZERO, String.valueOf(isStartpointZero));
			template.setRecordProperty(recordName, Record.NUMBER_FORMAT, String.valueOf(numberFormat));
			template.setRecordProperty(recordName, Record.IS_START_END_DEFINED, String.valueOf(isStartEndDefined));
			template.setRecordProperty(recordName, Record.DEFINED_MAX_VALUE, String.valueOf(maxScaleValue));
			template.setRecordProperty(recordName, Record.DEFINED_MIN_VALUE, String.valueOf(minScaleValue));

			template.setRecordProperty(recordName, Record.TRAIL_TEXT_ORDINAL, String.valueOf(trailTextOrdinal));
		}

		/**
		 * @param template holds all values
		 */
		void applyTemplate(HistoGraphicsTemplate template, TrailRecord record) {
			String recordName = record.getName();
			isVisible = Boolean.parseBoolean(template.getRecordProperty(recordName, Record.IS_VISIBLE, "false"));
			isPositionLeft = Boolean.parseBoolean(template.getRecordProperty(recordName, Record.IS_POSITION_LEFT, "true"));
			rgb = template.getRecordProperty(recordName, Record.COLOR, record.getRGB());
			lineWidth = Integer.parseInt(template.getRecordProperty(recordName, Record.LINE_WITH, "1"));
			lineStyle = Integer.parseInt(template.getRecordProperty(recordName, Record.LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_SOLID));
			isRoundOut = Boolean.parseBoolean(template.getRecordProperty(recordName, Record.IS_ROUND_OUT, "false"));
			isStartpointZero = Boolean.parseBoolean(template.getRecordProperty(recordName, Record.IS_START_POINT_ZERO, "false"));
			record.setStartEndDefined(Boolean.parseBoolean(template.getRecordProperty(recordName, Record.IS_START_END_DEFINED, "false")), //
					Double.parseDouble(template.getRecordProperty(recordName, Record.DEFINED_MIN_VALUE, "0")), //
					Double.parseDouble(template.getRecordProperty(recordName, Record.DEFINED_MAX_VALUE, "0")));
			numberFormat = Integer.parseInt(template.getRecordProperty(recordName, Record.NUMBER_FORMAT, "-1"));
			df = new TrailRecordFormatter(record).getDecimalFormat(numberFormat);

			// this template property is required before applying the template
			// trailTextOrdinal = Integer.parseInt(template.getRecordProperty(record.recordName, Record.TRAIL_TEXT_ORDINAL, "-1"));
		}

		/**
		 * Method to initialize scale position defaults.
		 */
		public void setPositionLeft(int recordOrdinal) {
			this.isPositionLeft = recordOrdinal % 2 == 0;
		}

	}

	protected final ChartTemplate template = new ChartTemplate();

	/**
	 * Collect input data for the trail record and subordinate objects.
	 * Support initial collection and collections after user input (e.g. trail type selection).
	 * @author Thomas Eickert (USER)
	 */
	final class RecordCollector {
		@SuppressWarnings("hiding")
		private final Logger log = Logger.getLogger(RecordCollector.class.getName());

		/**
		 * Set the data points for one single trail record.
		 * The record takes the selected trail type / score data from the trail record vault and populates its data.
		 * Support trail suites and bits / tokens.
		 */
		synchronized void addVaults(TreeMap<Long, List<ExtendedVault>> histoVaults) {
			if (!trailSelector.isTrailSuite()) {
				histoVaults.values().stream().flatMap(Collection::stream).forEach(v -> {
					addElement(getVaultPoint(v, trailSelector.getTrailOrdinal()));
				});
			} else {
				setSuite(histoVaults.size());
				histoVaults.values().stream().flatMap(Collection::stream).forEach(v -> {
					addVaultToSuite(v); //
				});
			}
			log.finer(() -> " " + getTrailSelector());
		}

		/**
		 * Take those data points from the histo vault which are assigned to the selected trail type.
		 * Support trail suites and bits / tokens.
		 * @param pointFactor for multiplying points for bits or tokens
		 */
		private void addVaultToSuite(ExtendedVault histoVault) {
			List<TrailTypes> suiteMembers = trailSelector.getSuiteMembers();

			if (trailSelector.isBoxPlotSuite()) {
				for (int i = 0; i < trailSelector.getSuiteMembers().size(); i++) {
					suiteRecords.get(i).addElement(getVaultPoint(histoVault, suiteMembers.get(i).ordinal()));
				}
			} else {
				int tmpSummationFactor = 0;
				int masterPoint = 0; // this is the base value for adding or subtracting standard deviations

				for (int i = 0; i < suiteMembers.size(); i++) {
					Integer point = getVaultPoint(histoVault, suiteMembers.get(i).ordinal());
					if (point == null) {
						suiteRecords.get(i).addElement(null);
					} else {
						tmpSummationFactor = getSummationFactor(suiteMembers.get(i), tmpSummationFactor);
						if (tmpSummationFactor == 0)
							masterPoint = point; // use in the next iteration if summation is necessary, e.g. avg+2*sd
						else
							point = masterPoint + tmpSummationFactor * point * 2;

						suiteRecords.get(i).addElement(point);
					}
					if (log.isLoggable(FINER))
						log.log(FINER, String.format(" %s trail %3d  %s  %d minVal=%d maxVal=%d", getName(), trailSelector.getTrailOrdinal(), histoVault.getLoadFilePath(), point, suiteRecords.get(i).getMinRecordValue(), suiteRecords.get(i).getMaxRecordValue()));
				}
			}
			log.log(FINER, " ", trailSelector);
		}

		/**
		 * Support adding / subtracting trail values from a preceding suite master record.
		 * @param trailType
		 * @param previousFactor the summation factor from the last iteration
		 * @return the alternating -1/+1 factor for summation trail types; 0 otherwise
		 */
		private int getSummationFactor(TrailTypes trailType, int previousFactor) {
			boolean isOddRange = TrailTypes.ODD_RANGE_TRAILS.contains(trailType);
			if (isOddRange) {
				return previousFactor == 0 ? -1 : previousFactor * -1;
			} else {
				return 0;
			}
		}

	}

	protected final DeviceXmlResource			xmlResource		= DeviceXmlResource.getInstance();

	protected final IChannelItem					channelItem;

	/**
	 * If a suite trail is chosen the values are added to suite records and the trail record does not get values.
	 */
	protected final SuiteRecords					suiteRecords	= new SuiteRecords();

	protected boolean											isDisplayable;

	protected TrailSelector								trailSelector;
	protected ElementaryQuantile<Double>	quantile;

	protected TrailRecord(IChannelItem channelItem, int newOrdinal, TrailRecordSet parentTrail, int initialCapacity) {
		super(parentTrail.getAnalyzer().getActiveDevice(), newOrdinal, channelItem.getName(), channelItem.getSymbol(), channelItem.getUnit(),
				channelItem.isActive(), null, initialCapacity);
		this.channelItem = channelItem;
		this.parent = parentTrail;

		log.fine(() -> channelItem.toString());
	}

	@Deprecated
	@Override
	public synchronized boolean add(Integer point) {
		throw new UnsupportedOperationException(" " + point); //$NON-NLS-1$
	}

	/**
	 * Add a data point to the record and set minimum and maximum.
	 * @param point
	 */
	@Override
	public synchronized void addElement(Integer point) {
		if (point == null) {
			if (this.isEmpty()) {
				this.minValue = Integer.MAX_VALUE;
				this.maxValue = Integer.MIN_VALUE;
			}
		} else {
			if (this.isEmpty())
				this.minValue = this.maxValue = point;
			else {
				if (point > this.maxValue) this.maxValue = point;
				if (point < this.minValue) this.minValue = point;
			}
		}
		super.addElement(point);
		log.finer(() -> this.name + " adding point = " + point); //$NON-NLS-1$
		log.finest(() -> this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	@Deprecated
	public synchronized Integer set(int index, Integer point) {
		throw new UnsupportedOperationException(" " + index + " " + point); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Clear the record subordinate objects.
	 */
	@Override
	public void clear() {
		this.suiteRecords.clear();
		this.quantile = null;
		super.clear();
	}

	/**
	 * @return the point size of a single curve or a suite
	 */
	@Override
	public synchronized int size() {
		return this.suiteRecords.getSuiteLength() > 0 && this.trailSelector.isTrailSuite() ? this.suiteRecords.realSize() : super.realSize();
	}

	/**
	 * @return the point size of a single curve or a suite element (calls realSize())
	 */
	@Override
	public int realSize() {
		return super.realSize();
	}

	/**
	 * Support Names replacement which is required for TrailRecords as they do not hold the replacement as a record name.
	 */
	@Override // reason is replacement for record names
	public String getSyncMasterName() {
		StringBuilder sb = new StringBuilder().append(this.getNameReplacement().split(GDE.STRING_BLANK)[0]);
		Vector<TrailRecord> syncedChildren = this.getParent().getScaleSyncedRecords(this.ordinal);
		if (syncedChildren == null) throw new UnsupportedOperationException();
		if (syncedChildren.firstElement().getNameReplacement().split(GDE.STRING_BLANK).length > 1) {
			sb.append(GDE.STRING_BLANK);
			String[] splitName = syncedChildren.firstElement().getNameReplacement().split(GDE.STRING_BLANK);
			sb.append(splitName.length > 1 ? splitName[1] : GDE.STRING_STAR);
			sb.append(GDE.STRING_DOT).append(GDE.STRING_DOT);
			String trailer = GDE.STRING_STAR;
			for (TrailRecord tmpRecord : syncedChildren) {
				if (tmpRecord.isDisplayable() && tmpRecord.size() > 1) trailer = tmpRecord.getNameReplacement();
			}
			sb.append(trailer.split(GDE.STRING_BLANK).length > 1 ? trailer.split(GDE.STRING_BLANK)[1] : GDE.STRING_STAR);
		} else {
			sb.append(GDE.STRING_MESSAGE_CONCAT).append(syncedChildren.lastElement().getNameReplacement());
		}
		return sb.toString();
	}

	/**
	 * Support triggered (calculated) statistics values.
	 * @return the graphics scale description including the symbol and the unit
	 */
	public String getScaleText() {
		String graphText;
		if (isScaleSyncMaster()) {
			graphText = getSyncMasterName();
			graphText += getSymbol() != null && getSymbol().length() > 0 ? "   " + getSymbol() : "";
			graphText += getUnit() != null && getUnit().length() > 0 ? "   [" + getUnit() + "]" : "   []";
		} else {
			String triggerScaleRawText = GDE.STRING_EMPTY;
			String triggerScaleUnit = GDE.STRING_EMPTY;
			if (this.trailSelector instanceof MeasurementTrailSelector && ((MeasurementTrailSelector) this.trailSelector).isTriggerTrail()) {
				triggerScaleRawText = ((MeasurementTrailSelector) this.trailSelector).getTriggerScaleRawText();
				triggerScaleUnit = ((MeasurementTrailSelector) this.trailSelector).getTriggerScaleUnit();
				if (!triggerScaleUnit.isEmpty() && triggerScaleUnit.startsWith(GDE.STRING_SLASH)) {
					TrailRecord referencedRatioRecord = getParent().get(((int) ((MeasurementType) getChannelItem()).getStatistics().getRatioRefOrdinal()));
					triggerScaleUnit = referencedRatioRecord.getUnit() + triggerScaleUnit;
				}
			}

			graphText = triggerScaleRawText.isEmpty() ? DeviceXmlResource.getInstance().getReplacement(getName())
					: DeviceXmlResource.getInstance().getReplacement(triggerScaleRawText);
			graphText += getSymbol() != null && getSymbol().length() > 0 && triggerScaleRawText.isEmpty() ? "   " + getSymbol() : "";
			graphText += getUnit() != null && getUnit().length() > 0 && triggerScaleRawText.isEmpty() ? "   [" + getUnit() + "]"
					: "   [" + triggerScaleUnit + "]";
		}
		return graphText;
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getFactor() {
		return this.channelItem.getFactor();
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getOffset() {
		return this.channelItem.getOffset();
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getReduction() {
		return this.channelItem.getReduction();
	}

	@Override
	public boolean isBits() {
		return this.channelItem.isBits();
	}

	@Override
	public boolean isTokens() {
		return this.channelItem.isTokens();
	}

	/**
	 * set instance value directly without logic
	 * @param newNumberFormat
	 */
	@Override
	public void setNumberFormatDirect(int newNumberFormat) {
		this.setNumberFormat(newNumberFormat);
	}

	@Override // reason: formatting of values <= 100 with decimal places; define category based on maxValueAbs AND minValueAbs
	public void setNumberFormat(int newNumberFormat) {
		log.log(Level.FINE, this.name, newNumberFormat);
		template.numberFormat = newNumberFormat;
		template.df = new TrailRecordFormatter(this).getDecimalFormat(newNumberFormat);

	}

	@Override
	public int getMaxValue() {
		return this.maxValue == this.minValue ? this.maxValue + 100 : this.maxValue;
	}

	@Override
	public int getMinValue() {
		return this.minValue == this.maxValue ? this.minValue - 100 : this.minValue;
	}

	@Override
	public double getMaxScaleValue() {
		return template.maxScaleValue;
	}

	@Override
	public double getMinScaleValue() {
		return template.minScaleValue;
	}

	public void setMinMaxScaleValue(double minScaleValue, double maxScaleValue) {
		template.minScaleValue = minScaleValue;
		template.maxScaleValue = maxScaleValue;
		log.fine(() -> getName() + " minScaleValue = " + minScaleValue + "; maxScaleValue = " + minScaleValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return true if the record or the suite contains reasonable data which can be displayed
	 */
	@Override
	public boolean hasReasonableData() {
		boolean hasReasonableData = false;
		if (this.size() > 0) {
			double[] extrema = Guardian.defineStandardExtrema(Arrays.asList(getParent().getIndexedVaults()), this.channelItem, getParent().getAnalyzer().getSettings());
			hasReasonableData = !HistoSet.fuzzyEquals(extrema[0], extrema[1]) || !HistoSet.fuzzyEquals(extrema[0], 0.);
			log.log(Level.FINE, name, hasReasonableData);
		}
		return hasReasonableData;
	}

	/**
	 * @return true if the record is active and has data values
	 */
	@Override // reason is size for suite records
	public boolean isActive() {
		return this.isActive == null || this.size() == 0 ? false : this.isActive;
	}

	/**
	 * @return true if the record is not suppressed by histo display settings which hide records
	 */
	protected abstract boolean isAllowedBySetting();

	/**
	 * @return true if the record is the scale sync master and if the record is for display according to histo display settings
	 */
	@Override // reason are the histo display settings which hide records
	public boolean isScaleVisible() {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, this.name + " isScaleSyncMaster=" + isScaleSyncMaster() + " isOneOfSyncableRecord=" + this.getAbstractParent().isOneOfSyncableRecord(getName()));
		return isScaleSyncMaster() ? this.getAbstractParent().isOneSyncableVisible(this.ordinal)
				: !this.getAbstractParent().isOneOfSyncableRecord(getName()) && template.isVisible && this.isDisplayable;
	}

	@Override
	@Deprecated
	public double getTime_ms(int index) {
		throw new UnsupportedOperationException();
		// return this.parent.timeStep_ms.getTime_ms(index);
	}

	/**
	 * Update the displayable record information in this record set.
	 */
	public void setDisplayable() {
		this.isDisplayable = isAllowedBySetting() && hasReasonableData();
	}

	@Override
	public boolean isDisplayable() {
		return this.isDisplayable;
	}

	@Override
	public boolean isVisible() {
		return this.template.isVisible;
	}

	@Override
	public void setVisible(boolean enabled) {
		this.template.isVisible = enabled;
	}

	@Override
	public boolean isPositionLeft() {
		return this.template.isPositionLeft;
	}

	@Override
	public void setPositionLeft(boolean enabled) {
		this.template.isPositionLeft = enabled;
	}

	/**
	 * @return the CSV value (e.g. 0,0,0 for black)
	 */
	@Override
	public String getRGB() {
		return this.template.rgb;
	}

	/**
	 * @param rgb is the CSV value (e.g. 0,0,0 for black)
	 */
	@Override
	public void setRGB(String rgb) {
		this.template.rgb = rgb;
	}

	/**
	 ** @return always false as trail records do not use this mechanism
	 ** @see gde.data.CommonRecord#isRoundOut()
	 */
	@Deprecated // not supported
	@Override
	public boolean isRoundOut() {
		return false;
	}

	/**
	 * @return the template value which is passed through
	 */
	public boolean isRealRoundOut() {
		return template.isRoundOut;
	}

	@Override
	public void setRoundOut(boolean enabled) {
		template.isRoundOut = enabled;
	}

	@Override
	public boolean isStartpointZero() {
		return template.isStartpointZero;
	}

	@Override
	public void setStartpointZero(boolean enabled) {
		template.isStartpointZero = enabled;
	}

	@Override // reason is missing zoom mode
	public boolean isStartEndDefined() {
		return template.isStartEndDefined;
	}

	@Override
	public void setStartEndDefined(boolean enabled) {
		template.isStartEndDefined = enabled;
	}

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param enabled
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	@Override // reason is unused channelConfigKey
	public void setStartEndDefined(boolean enabled, double newMinScaleValue, double newMaxScaleValue) {
		log.finer(() -> this.name + " enabled=" + enabled + " newMinScaleValue=" + newMinScaleValue + " newMaxScaleValue=" + newMaxScaleValue);
		template.isStartEndDefined = enabled;
		if (enabled) {
			template.maxScaleValue = newMaxScaleValue;
			template.minScaleValue = newMinScaleValue;
		} else {
			template.maxScaleValue = HistoSet.decodeVaultValue(this.channelItem, this.maxValue / 1000.0);
			template.minScaleValue = HistoSet.decodeVaultValue(this.channelItem, this.minValue / 1000.0);
		}
	}

	@Override
	public void setMinScaleValue(double newMinScaleValue) {
		template.minScaleValue = newMinScaleValue;
	}

	@Override
	public void setMaxScaleValue(double newMaxScaleValue) {
		template.maxScaleValue = newMaxScaleValue;
	}

	@Override
	public int getLineWidth() {
		return this.template.lineWidth;
	}

	@Override
	public void setLineWidth(int newLineWidth) {
		this.template.lineWidth = newLineWidth;
	}

	@Override
	public int getLineStyle() {
		return this.template.lineStyle;
	}

	@Override
	public void setLineStyle(int newLineStyle) {
		this.template.lineStyle = newLineStyle;
	}

	@Override
	public int getNumberFormat() {
		return this.template.numberFormat;
	}

	/**
	 * @return the parent
	 */
	public TrailRecordSet getParent() {
		return (TrailRecordSet) this.parent;
	}

	/**
	 * @param timeStamp1_ms is a timeStamp fitting exactly to an index
	 * @param timeStamp2_ms is a timeStamp fitting exactly to an index
	 * @return startIndex, inclusive, and endIndex, inclusive.<br/>
	 *         If the timestamps are equal, the returned values are the same.
	 */
	public int[] defineRangeIndices(long timeStamp1_ms, long timeStamp2_ms) {
		int index1 = this.getIndex(timeStamp1_ms);
		int index2 = this.getIndex(timeStamp2_ms);
		return new int[] { Math.min(index1, index2), Math.max(index1, index2) };
	}

	/**
	 * Supports suites.
	 * @param fromIndex inclusive
	 * @param toIndex exclusive
	 * @return the range of spots (timestamps_ms, aggregated translated value) in timestamp desc order.<br/>
	 *         If fromIndex and toIndex are equal, the returned list is empty.
	 */
	public List<Spot<Double>> getSubPoints(int fromIndex, int toIndex) {
		int recordSize = toIndex - fromIndex;
		List<Spot<Double>> result = new ArrayList<>(recordSize);

		Vector<Integer> points = this.getPoints();
		for (int i = fromIndex; i < toIndex; i++) {
			if (points.elementAt(i) != null) {
				result.add(new Spot<Double>(this.parent.getTime_ms(i), HistoSet.decodeVaultValue(this.channelItem, points.elementAt(i) / 1000.)));
			}
		}
		log.finer(() -> Arrays.toString(result.toArray()));
		return result;
	}

	/**
	 * @param timeStamp_ms
	 * @return the index fitting exactly to the timeStamp
	 */
	public int getIndex(long timeStamp_ms) {
		return this.getParent().getIndex(timeStamp_ms);
	}

	/**
	 * @return the uncloned point values of the record or suite master
	 */
	public Vector<Integer> getPoints() {
		final Vector<Integer> points;
		if (!this.trailSelector.isTrailSuite())
			points = this;
		else
			points = this.suiteRecords.get(this.trailSelector.getSuiteMasterIndex());

		return points;
	}

	/**
	 * Defines new suite records from the selected trail and the suite master record.
	 * @param initialCapacity
	 */
	public void setSuite(int initialCapacity) {
		this.suiteRecords.clear();

		List<TrailTypes> suiteMembers = this.trailSelector.getSuiteMembers();
		for (int i = 0; i < suiteMembers.size(); i++) {
			this.suiteRecords.put(i, new SuiteRecord(suiteMembers.get(i).ordinal(), initialCapacity));
		}
	}

	public String getNameReplacement() {
		return getDeviceXmlReplacement(this.name);
	}

	/**
	 * @return the localized value of the label property from the device channel entry or an empty string.
	 */
	public String getLabel() {
		return getDeviceXmlReplacement(this.channelItem.getLabel());
	}

	public String getTableRowHeader() {
		return getUnit().length() > 0 ? (getNameReplacement() + GDE.STRING_BLANK_LEFT_BRACKET + getUnit() + GDE.STRING_RIGHT_BRACKET).intern()
				: getNameReplacement().intern();
	}

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	private String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
	}

	public TrailSelector getTrailSelector() {
		return this.trailSelector;
	}

	public SuiteRecords getSuiteRecords() {
		return this.suiteRecords;
	}

	@Override
	public String getFormattedScaleValue(double finalValue) {
		return new TrailRecordFormatter(this).getScaleValue(finalValue);
	}

	@Override
	public DecimalFormat getRealDf() {
		return template.df;
	}

	@Override
	public void setRealDf(DecimalFormat realDf) {
		template.df = realDf;
	}

	/**
	 * @return the decimal format used by this record
	 */
	public DecimalFormat getDecimalFormat() {
		if (template.numberFormat == -1) this.setNumberFormat(-1); // update the number format to actual automatic formating
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, this.isScaleSynced() + " - " + this.getAbstractParent().getSyncMasterRecordOrdinal(getName()));
		return this.isScaleSynced() ? this.getAbstractParent().get(this.getAbstractParent().getSyncMasterRecordOrdinal(getName())).getRealDf()
				: template.df;
	}

	public void setColorDefaultsAndPosition(int recordOrdinal) {
		template.rgb = ColorUtils.getDefaultRgb(recordOrdinal);
		template.setPositionLeft(recordOrdinal);
	}

	public IChannelItem getChannelItem() {
		return this.channelItem;
	}

	/**
	 * Set the trail based on the record's chart template settings.
	 * Might finally set a different value if the smart statistics value has changed in the meantime.
	 */
	public void setSelectedTrail() {
		setSelectedTrail(template.trailTextOrdinal);
	}

	/**
	 * @param trailTextOrdinal is the ordinal of the applicable trail texts or a negative number (take the prioritized trail type from
	 *          applicable trails)
	 */
	public void setSelectedTrail(int trailTextOrdinal) {
		if (trailTextOrdinal >= 0 && trailTextOrdinal < trailSelector.getApplicableTrailsTexts().size()) {
			trailSelector.setTrailTextSelectedIndex(trailTextOrdinal);
		} else {
			trailSelector.setMostApplicableTrailTextOrdinal();
		}
		template.trailTextOrdinal = trailSelector.getTrailTextSelectedIndex();
		if (trailSelector.getTrailTextSelectedIndex() < 0) {
			log.info(() -> String.format("%s : no trail types identified" + name)); //$NON-NLS-1$
		}
		log.finer(() -> String.format("%s", trailSelector.toString()));
	}

	/**
	 * @param vault
	 * @param trailOrdinal is the requested trail ordinal number which may differ from the selected trail type (e.g. suites)
	 * @return the point value
	 */
	public abstract Integer getVaultPoint(ExtendedVault vault, int trailOrdinal);

	@Override
	public int getSyncMasterRecordOrdinal() {
		return channelItem.getSyncMasterRecordOrdinal();
	}

	@Override
	@Deprecated // histo relies on building the layout information from scratch on for each display cycle
	public void setSyncMinMax(int newMin, int newMax) {
		log.finer(() -> "  DEPRECATED    " + getName() + " syncMinValue=" + newMin + " syncMaxValue=" + newMax);
	}

	public double[] defineRecentMinMax(int limit) {
		int trailOrdinal = this.trailSelector.getTrailOrdinal();
		if (this.trailSelector.isOddRangeTrail()) {
			return Guardian.defineAlienMinMax(Arrays.stream(getParent().getIndexedVaults()).limit(limit), this.channelItem, trailOrdinal);
		} else {
			return Guardian.defineStandardMinMax(Arrays.stream(getParent().getIndexedVaults()).limit(limit), this.channelItem);
		}
	}

	/**
	 * @return the lower/upper values for all trails or for the selected trail in case of a different number range than the measurement values
	 *         (e.g. SD, counters)
	 */
	public double[] defineExtrema() { // todo consider caching this result
		int trailOrdinal = this.trailSelector.getTrailOrdinal();
		if (this.trailSelector.isOddRangeTrail()) {
			return Guardian.defineAlienExtrema(Arrays.asList(getParent().getIndexedVaults()), this.channelItem, trailOrdinal, getParent().getAnalyzer().getSettings());
		} else {
			return Guardian.defineStandardExtrema(Arrays.asList(getParent().getIndexedVaults()), this.channelItem, getParent().getAnalyzer().getSettings());
		}
	}

	/**
	 * Determine the outliers of the category which is the most significant.
	 * This means: The result holds far outliers OR close outliers in case no far outliers are present.
	 * @param logLimit is the maximum number of the most recent logs which is checked for warnings
	 * @return the array of outliers warning objects which may hold null values
	 */
	public Reminder[] defineMinMaxWarning(int logLimit) {
		return Guardian.defineMinMaxReminder(getParent().getIndexedVaults(), this.channelItem, this.trailSelector, logLimit, getParent().getAnalyzer().getSettings());
	}

	/**
	 * @return all the decoded record values including nulls
	 */
	protected List<Double> getDecodedNotNullValues() {
		List<Double> decodedValues = new ArrayList<>();

		final Vector<Integer> record;
		if (this.trailSelector.isTrailSuite()) {
			record = this.suiteRecords.get(this.trailSelector.getSuiteMasterIndex());
		} else {
			record = this;
		}

		for (Integer value : record) { // loops without calling the overridden getter
			if (value != null) decodedValues.add(HistoSet.decodeVaultValue(this.channelItem, value / 1000.));
		}
		return decodedValues;
	}

	protected void defineQuantile() {
		quantile = new ElementaryQuantile<>(getDecodedNotNullValues(), true, getParent().getAnalyzer().getSettings());
		log.finest(() -> name + " size=" + quantile.getSize() + " UpperWhisker=" + quantile.getQuantileUpperWhisker());
	}

	public ElementaryQuantile<Double> getQuantile() {
		if (quantile == null) defineQuantile();
		return quantile;
	}

	/**
	 * @return the points for the q0/q4 trails; for score groups w/o min/max scores take the first score
	 */
	protected Integer[] getExtremumTrailPoints(ExtendedVault vault) {
		int[] extremumOrdinals = this.trailSelector.getExtremumTrailsOrdinals();
		return new Integer[] { getVaultPoint(vault, extremumOrdinals[0]), getVaultPoint(vault, extremumOrdinals[1]) };
	}

	/**
	 * (Re)Build the data contents.
	 */
	public void initializeFromVaults(TreeMap<Long, List<ExtendedVault>> histoVaults) {
		RecordCollector collector = new RecordCollector();
		collector.addVaults(histoVaults);
	}

	/**
	 * @return false if the record does not exist or has no outliers
	 */
	public abstract boolean hasVaultOutliers(ExtendedVault vault);

	public abstract DoubleStream getVaultScraps(ExtendedVault vault);

	public abstract DoubleStream getVaultOutliers(ExtendedVault vault);

	public abstract boolean hasVaultScraps(ExtendedVault vault);

	public abstract DataType getVaultDataType(HistoVault vault);

	/**
	 * @return the dataType of this record
	 */
	@Override
	public Record.DataType getDataType() {
		return channelItem.getUnifiedDataType();
	}

	public void applyTemplate() {
		template.applyTemplate(getParent().getTemplate(), this);
	}

	public void saveTemplate() {
		template.saveTemplate(getParent().getTemplate(), this);
	}

	public abstract void setTrailSelector();

}
