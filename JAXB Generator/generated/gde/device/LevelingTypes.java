//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.21 at 01:09:13 PM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for leveling_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="leveling_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="first"/>
 *     &lt;enumeration value="last"/>
 *     &lt;enumeration value="mid"/>
 *     &lt;enumeration value="avg"/>
 *     &lt;enumeration value="minmax"/>
 *     &lt;enumeration value="smooth_minmax"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "leveling_types")
@XmlEnum
public enum LevelingTypes {

    @XmlEnumValue("first")
    FIRST("first"),
    @XmlEnumValue("last")
    LAST("last"),
    @XmlEnumValue("mid")
    MID("mid"),
    @XmlEnumValue("avg")
    AVG("avg"),
    @XmlEnumValue("minmax")
    MINMAX("minmax"),
    @XmlEnumValue("smooth_minmax")
    SMOOTH_MINMAX("smooth_minmax");
    private final String value;

    LevelingTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LevelingTypes fromValue(String v) {
        for (LevelingTypes c: LevelingTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
