//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.13 at 10:52:56 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for desktop_property_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="desktop_property_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="table_tab"/>
 *     &lt;enumeration value="digital_tab"/>
 *     &lt;enumeration value="analog_tab"/>
 *     &lt;enumeration value="voltage_per_cell_tab"/>
 *     &lt;enumeration value="utility_garphics_tab"/>
 *     &lt;enumeration value="utility_device_tab"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "desktop_property_types")
@XmlEnum
public enum DesktopPropertyTypes {

    @XmlEnumValue("table_tab")
    TABLE_TAB("table_tab"),
    @XmlEnumValue("digital_tab")
    DIGITAL_TAB("digital_tab"),
    @XmlEnumValue("analog_tab")
    ANALOG_TAB("analog_tab"),
    @XmlEnumValue("voltage_per_cell_tab")
    VOLTAGE_PER_CELL_TAB("voltage_per_cell_tab"),
    @XmlEnumValue("utility_garphics_tab")
    UTILITY_GRAPHICS_TAB("utility_garphics_tab"),
    @XmlEnumValue("utility_device_tab")
    UTILITY_DEVICE_TAB("utility_device_tab");
    private final String value;

    DesktopPropertyTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DesktopPropertyTypes fromValue(String v) {
        for (DesktopPropertyTypes c: DesktopPropertyTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}