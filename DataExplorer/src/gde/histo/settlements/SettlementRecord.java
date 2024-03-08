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
package gde.histo.settlements;

import static java.util.logging.Level.FINE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import gde.GDE;
import gde.data.IRecord;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.IChannelItem;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.ObjectFactory;
import gde.device.PropertyType;
import gde.device.SettlementType;
import gde.histo.transitions.Transition;
import gde.histo.utils.ElementaryQuantile;
import gde.log.Logger;

/**
 * Hold settlement points of a line or curve calculated from measurements.
 * Applicable for settlements with an evaluation rule.
 * Do not support settlement display or table view.
 * Similar to record class except: clone, serialization, zoom / scope, triggers, syncWithRecords, setName, GPS-longitude, GPS-latitude.
 * @author Thomas Eickert
 */
public final class SettlementRecord extends Vector<Integer> implements IRecord {
	private static final String					$CLASS_NAME							= SettlementRecord.class.getName();
	private static final long						serialVersionUID				= 6130190003229390899L;
	private static final Logger					log											= Logger.getLogger($CLASS_NAME);

	private static final int						INITIAL_RECORD_CAPACITY	= 22;
	private static final String					BELOW_LIMIT							= MeasurementPropertyTypes.BELOW_LIMIT.value();
	private static final String					BEYOND_LIMIT						= MeasurementPropertyTypes.BEYOND_LIMIT.value();

	private final SettlementType				settlement;

	private final RecordSet							parent;
	private final int										logChannelNumber;
	private final String								name;																																		// measurement name Höhe

	/**
	 * Full translated value list.</br>
	 * Key is the timestamp index.</br>
	 * (Is persistent due to a small number of values compared to the record objects.)
	 */
	private final Map<Integer, Double>	translatedValues				= new LinkedHashMap<>();
	private final List<Transition>			transitions							= new ArrayList<>();

	private List<PropertyType>					properties							= new ArrayList<>();														// offset, factor, reduction, ...
	private ElementaryQuantile<Integer>	quantile								= null;

	/**
	 * Creates a vector to hold data points.
	 * @param newSettlement
	 * @param parent
	 * @param logChannelNumber
	 */
	public SettlementRecord(SettlementType newSettlement, RecordSet parent, int logChannelNumber) {
		super(INITIAL_RECORD_CAPACITY);
		this.settlement = newSettlement;
		this.name = newSettlement.getName();
		this.parent = parent;
		this.logChannelNumber = logChannelNumber;
		initializeProperties(this, newSettlement.getProperty());
		log.log(FINE, "newSettlement  ", this); //$NON-NLS-1$
	}

	/**
	 * Initialize properties, at least all records will have as default a factor, an offset and a reduction property
	 * @param recordRef
	 * @param newProperties
	 */
	private void initializeProperties(SettlementRecord recordRef, List<PropertyType> newProperties) {
		this.properties = this.properties != null ? this.properties : new ArrayList<PropertyType>(); // offset, factor, reduction, ...
		for (PropertyType property : newProperties) {
			log.finer(() -> String.format("%20s - %s = %s", recordRef.name, property.getName(), property.getValue())); //$NON-NLS-1$
			this.properties.add(property.clone());
		}
	}

	@Override
	public String toString() {
		String belowLimit = getBelowLimit() != -Double.MAX_VALUE ? String.format("%.1f", getBelowLimit()) : "none";
		String beyondLimit = getBeyondLimit() != Double.MAX_VALUE ? String.format("%.1f", getBeyondLimit()) : "none";
		return String.format("%s channel=%d  limits=%s/%s", this.name, this.logChannelNumber, belowLimit, beyondLimit);
	}

	@Override
	@Deprecated // use add with transition
	public synchronized boolean add(Integer e) {
		double translateValue = translateValue(e / 1000.0);
		if (translateValue > getBeyondLimit()) {
			log.warning(() -> String.format("discard beyond value=%f", translateValue) + this); //$NON-NLS-1$
			return false;
		} else if (translateValue < getBelowLimit()) {
			log.warning(() -> String.format("discard below value=%f", translateValue) + this); //$NON-NLS-1$
			return false;
		} else {
			return super.add(e);
		}
	}

	public synchronized boolean add(Integer e, Transition transition) {
		double translatedValue = translateValue(e / 1000.0);
		if (translatedValue > getBeyondLimit()) {
			log.warning(() -> String.format("discard beyond value=%f", translatedValue) + this); //$NON-NLS-1$
			return false;
		} else if (translatedValue < getBelowLimit()) {
			log.warning(() -> String.format("discard below value=%f", translatedValue) + this); //$NON-NLS-1$
			return false;
		} else {
			transitions.add(transition);
			translatedValues.put(transition.getThresholdStartIndex(), translatedValue);
			return super.add(e);
		}
	}

	@Override
	@Deprecated // use elaborated add methods for settlements
	public synchronized Integer set(int index, Integer point) {
		return super.set(index, point);
	}

	/**
	 * return the size
	 */
	@Override
	@Deprecated // pls use realSize() for code clarity
	public synchronized int size() {
		return super.size(); // zoom and scope not supported
	}

	/**
	 * @return the vector size
	 */
	public int realSize() {
		return super.size();
	}

	/**
	 * get property reference using given property type key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType
	 */
	private PropertyType getProperty(String propertyKey) {
		PropertyType property = null;
		for (PropertyType propertyType : this.properties) {
			if (propertyType.getName().equals(propertyKey)) {
				property = propertyType;
				break;
			}
		}
		return property;
	}

	/**
	 * Create a property and return the reference
	 * @param propertyKey
	 * @param type
	 * @return created property with associated propertyKey
	 */
	private PropertyType createProperty(String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(GDE.STRING_EMPTY + value);
		this.properties.add(newProperty);
		return newProperty;
	}

	public double getFactor() {
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			value = Double.parseDouble(property.getValue());
		else {
			// take default
		}
		return value;
	}

	public void setFactor(double newValue) {
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.FACTOR, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public double getOffset() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			value = Double.parseDouble(property.getValue());
		else {
			// take default
		}
		return value;
	}

	public void setOffset(double newValue) {
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.OFFSET, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public double getReduction() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			value = Double.parseDouble(property.getValue());
		else {
			// take default
		}
		return value;
	}

	public void setReduction(double newValue) {
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public boolean isBits() {
		boolean isBits = false;
		PropertyType tmpProperty = this.getProperty(IDevice.IS_BITS);
		if (tmpProperty != null) {
			isBits = Boolean.parseBoolean(tmpProperty.getValue());
		}
		return isBits;
	}

	/**
	 * @return all the translated settlement values which hold values only for the transition timestamps
	 */
	@Override
	public Collection<Double> getTranslatedValues() {
		return translatedValues.values();
	}

	@Override
	public Collection<Integer> getValues() {
		return this;
	}

	/**
	 * @param index
	 * @return the translated settlement value or null for timestamps without transition
	 */
	public Double getTranslatedValue(int index) {
		return translatedValues.get(index);
	}

	public RecordSet getParent() {
		return this.parent;
	}

	/**
	 * @return true if the record contained reasonable date which can be displayed
	 */
	public boolean hasReasonableData() {
		return this.realSize() > 0 && (getQuantile().getQuartile0() != getQuantile().getQuartile4() || translateValue(getQuantile().getQuartile4() / 1000.0) != 0.0);
	}

	/**
	 * Function to translate settlement values to values represented.
	 * Does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of device dependent value
	 */
	public double translateValue(double value) {
		double newValue = (value - this.getReduction()) * this.getFactor() + this.getOffset();
		log.finer(() -> "for " + this.name + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * Function to translate values represented into normalized settlement values.
	 * Does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of settlement dependent value
	 */
	public double reverseTranslateValue(double value) { // todo support settlements based on GPS-longitude or GPS-latitude with a base class common for
																											// Record, TrailRecord and Settlement
		double newValue = (value - this.getOffset()) / this.getFactor() + this.getReduction();
		log.finer(() -> "for " + this.name + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	public String getName() {
		return this.name;
	}

	public SettlementType getSettlement() {
		return this.settlement;
	}

	public IChannelItem getChannelItem() {
		return this.settlement;
	}

	public ElementaryQuantile<Integer> getQuantile() {
		if (this.quantile == null) {
			this.quantile = new ElementaryQuantile<>(this, true, parent.getAnalyzer().getSettings());
		}
		return this.quantile;
	}

	public int getLogChannelNumber() {
		return this.logChannelNumber;
	}

	private double getBeyondLimit() {
		double value = Double.MAX_VALUE;
		PropertyType property = this.getProperty(BEYOND_LIMIT);
		if (property != null)
			value = Double.parseDouble(property.getValue());
		else {
			// take default
		}
		return value;
	}

	private double getBelowLimit() {
		double value = -Double.MAX_VALUE;
		PropertyType property = this.getProperty(BELOW_LIMIT);
		if (property != null)
			value = Double.parseDouble(property.getValue());
		else {
			// take default
		}
		return value;
	}

}
