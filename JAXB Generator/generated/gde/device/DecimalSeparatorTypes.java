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
 * <p>Java class for decimal_separator_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="decimal_separator_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="dot"/>
 *     &lt;enumeration value="comma"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "decimal_separator_types")
@XmlEnum
public enum DecimalSeparatorTypes {

    @XmlEnumValue("dot")
    DOT("dot"),
    @XmlEnumValue("comma")
    COMMA("comma");
    private final String value;

    DecimalSeparatorTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DecimalSeparatorTypes fromValue(String v) {
        for (DecimalSeparatorTypes c: DecimalSeparatorTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}