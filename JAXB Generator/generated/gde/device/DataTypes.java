//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.20 at 02:08:46 PM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for data_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="data_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Integer"/>
 *     &lt;enumeration value="Double"/>
 *     &lt;enumeration value="Boolean"/>
 *     &lt;enumeration value="String"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "data_types")
@XmlEnum
public enum DataTypes {

    @XmlEnumValue("Integer")
    INTEGER("Integer"),
    @XmlEnumValue("Double")
    DOUBLE("Double"),
    @XmlEnumValue("Boolean")
    BOOLEAN("Boolean"),
    @XmlEnumValue("String")
    STRING("String");
    private final String value;

    DataTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DataTypes fromValue(String v) {
        for (DataTypes c: DataTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}