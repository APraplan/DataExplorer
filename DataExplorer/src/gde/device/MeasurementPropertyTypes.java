//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.09.10 at 12:50:42 PM MESZ 
//


package gde.device;

import gde.GDE;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for measurement_property_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="measurement_property_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="offset"/>
 *     &lt;enumeration value="factor"/>
 *     &lt;enumeration value="reduction"/>
 *     &lt;enumeration value="do_subtract_first"/>
 *     &lt;enumeration value="do_subtract_last"/>
 *     &lt;enumeration value="regression_type"/>
 *     &lt;enumeration value="regression_type_curve"/>
 *     &lt;enumeration value="regression_type_linear"/>
 *     &lt;enumeration value="regression_interval_sec"/>
 *     &lt;enumeration value="number_cells"/>
 *     &lt;enumeration value="prop_n100W"/>
 *     &lt;enumeration value="is_invert_current"/>
 *     &lt;enumeration value="number_motor"/>
 *     &lt;enumeration value="revolution_factor"/>
 *     &lt;enumeration value="scale_sync_ref_ordinal"/>
 *     &lt;enumeration value="google_earth_velocity_avg_limit_factor"/>
 *     &lt;enumeration value="google_earth_velocity_lower_limit"/>
 *     &lt;enumeration value="google_earth_velocity_upper_limit"/>
 *     &lt;enumeration value="google_earth_within_limits_color"/>
 *     &lt;enumeration value="google_earth_lower_limit_color"/>
 *     &lt;enumeration value="google_earth_upper_limit_color"/>
 *     &lt;enumeration value="google_earth_is_extrude"/>
 *     &lt;enumeration value="google_earth_is_random_color"/>
 *     &lt;enumeration value="filter_factor"/>
 *     &lt;enumeration value="beyond_limit"/>
 *     &lt;enumeration value="below_limit"/>
 *     &lt;enumeration value="is_bits"/>
 *     &lt;enumeration value="is_tokens"/>
 *     &lt;enumeration value="data_type"/>
 *     &lt;enumeration value="none_specified"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "measurement_property_types")
@XmlEnum
public enum MeasurementPropertyTypes {

    @XmlEnumValue("offset")
    OFFSET("offset"),
    @XmlEnumValue("factor")
    FACTOR("factor"),
    @XmlEnumValue("reduction")
    REDUCTION("reduction"),
    @XmlEnumValue("do_subtract_first")
    DO_SUBTRACT_FIRST("do_subtract_first"),
    @XmlEnumValue("do_subtract_last")
    DO_SUBTRACT_LAST("do_subtract_last"),
    @XmlEnumValue("regression_type")
    REGRESSION_TYPE("regression_type"),
    @XmlEnumValue("regression_type_curve")
    REGRESSION_TYPE_CURVE("regression_type_curve"),
    @XmlEnumValue("regression_type_linear")
    REGRESSION_TYPE_LINEAR("regression_type_linear"),
    @XmlEnumValue("regression_interval_sec")
    REGRESSION_INTERVAL_SEC("regression_interval_sec"),
    @XmlEnumValue("number_cells")
    NUMBER_CELLS("number_cells"),
    @XmlEnumValue("prop_n100W")
    PROP_N_100_W("prop_n100W"),
    @XmlEnumValue("is_invert_current")
    IS_INVERT_CURRENT("is_invert_current"),
    @XmlEnumValue("number_motor")
    NUMBER_MOTOR("number_motor"),
    @XmlEnumValue("revolution_factor")
    REVOLUTION_FACTOR("revolution_factor"),
    @XmlEnumValue("scale_sync_ref_ordinal")
    SCALE_SYNC_REF_ORDINAL("scale_sync_ref_ordinal"),
    @XmlEnumValue("google_earth_velocity_avg_limit_factor")
    GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR("google_earth_velocity_avg_limit_factor"),
    @XmlEnumValue("google_earth_velocity_lower_limit")
    GOOGLE_EARTH_VELOCITY_LOWER_LIMIT("google_earth_velocity_lower_limit"),
    @XmlEnumValue("google_earth_velocity_upper_limit")
    GOOGLE_EARTH_VELOCITY_UPPER_LIMIT("google_earth_velocity_upper_limit"),
    @XmlEnumValue("google_earth_within_limits_color")
    GOOGLE_EARTH_WITHIN_LIMITS_COLOR("google_earth_within_limits_color"),
    @XmlEnumValue("google_earth_lower_limit_color")
    GOOGLE_EARTH_LOWER_LIMIT_COLOR("google_earth_lower_limit_color"),
    @XmlEnumValue("google_earth_upper_limit_color")
    GOOGLE_EARTH_UPPER_LIMIT_COLOR("google_earth_upper_limit_color"),
    @XmlEnumValue("google_earth_is_extrude")
    GOOGLE_EARTH_IS_EXTRUDE("google_earth_is_extrude"),
    @XmlEnumValue("google_earth_is_random_color")
    GOOGLE_EARTH_IS_RANDOM_COLOR("google_earth_is_random_color"),
    @XmlEnumValue("filter_factor")
    FILTER_FACTOR("filter_factor"),
    @XmlEnumValue("beyond_limit")
    BEYOND_LIMIT("beyond_limit"),
    @XmlEnumValue("below_limit")
    BELOW_LIMIT("below_limit"),
    @XmlEnumValue("is_bits")
    IS_BITS("is_bits"),
    @XmlEnumValue("is_tokens")
    IS_TOKENS("is_tokens"),
    @XmlEnumValue("data_type")
    DATA_TYPE("data_type"),
    @XmlEnumValue("none_specified")
    NONE_SPECIFIED("none_specified");
    private final String value;

    MeasurementPropertyTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MeasurementPropertyTypes fromValue(String v) {
        for (MeasurementPropertyTypes c: MeasurementPropertyTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
    
  	public static String[] valuesAsStingArray() {
  		StringBuilder sb = new StringBuilder();
  		for (MeasurementPropertyTypes element : MeasurementPropertyTypes.values()) {
  			if(element.equals(NONE_SPECIFIED)) continue;
  			sb.append(element.value).append(GDE.STRING_SEMICOLON);
  		}
  		return sb.toString().split(GDE.STRING_SEMICOLON);
  	}

  	public static boolean isNoneSpecified(String checkName) {
  		boolean isNoneSpecified = true;
  		for (String element : MeasurementPropertyTypes.valuesAsStingArray()) {
  			if(element.equals(checkName)) {
  				isNoneSpecified = false;
  				break;
  			}
  		}
  		return isNoneSpecified;
  	}
}
