//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.02.05 at 09:30:28 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for channel_property_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="channel_property_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="enable_filter"/>
 *     &lt;enumeration value="text_mode"/>
 *     &lt;enumeration value="enable_channel"/>
 *     &lt;enumeration value="number_poles"/>
 *     &lt;enumeration value="pinion_gear"/>
 *     &lt;enumeration value="main_gear"/>
 *     &lt;enumeration value="status_fields"/>
 *     &lt;enumeration value="time_info"/>
 *     &lt;enumeration value="outlier_sigma"/>
 *     &lt;enumeration value="outlier_range_factor"/>
 *     &lt;enumeration value="none_specified"/>
 *     &lt;enumeration value="channel_percentage"/>
 *     &lt;enumeration value="sensor_alt_climb"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "channel_property_types")
@XmlEnum
public enum ChannelPropertyTypes {

	  @XmlEnumValue("enable_filter")
	  ENABLE_FILTER("enable_filter"),
	  @XmlEnumValue("text_mode")
	  TEXT_MODE("text_mode"),
	  @XmlEnumValue("enable_channel")
	  ENABLE_CHANNEL("enable_channel"),
    @XmlEnumValue("number_poles")
    NUMBER_POLES("number_poles"),
    @XmlEnumValue("pinion_gear")
    PINION_GEAR("pinion_gear"),
    @XmlEnumValue("main_gear")
    MAIN_GEAR("main_gear"),
    @XmlEnumValue("status_fields")
    STATUS_FIELDS("status_fields"),
    @XmlEnumValue("time_info")
    TIME_INFO("time_info"),
    @XmlEnumValue("outlier_sigma")
    OUTLIER_SIGMA("outlier_sigma"),
    @XmlEnumValue("outlier_range_factor")
    OUTLIER_RANGE_FACTOR("outlier_range_factor"),
    @XmlEnumValue("none_specified")
    NONE_SPECIFIED("none_specified"),
    @XmlEnumValue("channel_percentage")
    CHANNEL_PERCENTAGE("channel_percentage"),
    @XmlEnumValue("sensor_alt_climb")
    SENSOR_ALT_CLIMB("sensor_alt_climb");
    private final String value;

    ChannelPropertyTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ChannelPropertyTypes fromValue(String v) {
        for (ChannelPropertyTypes c: ChannelPropertyTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
