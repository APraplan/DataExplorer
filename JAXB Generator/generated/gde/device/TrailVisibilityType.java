//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.09.11 at 08:44:05 AM MESZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TrailVisibilityType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TrailVisibilityType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="trail" type="{}trail_types" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TrailVisibilityType")
public class TrailVisibilityType {

    @XmlAttribute
    protected TrailTypes trail;

    /**
     * Gets the value of the trail property.
     * 
     * @return
     *     possible object is
     *     {@link TrailTypes }
     *     
     */
    public TrailTypes getTrail() {
        return trail;
    }

    /**
     * Sets the value of the trail property.
     * 
     * @param value
     *     allowed object is
     *     {@link TrailTypes }
     *     
     */
    public void setTrail(TrailTypes value) {
        this.trail = value;
    }

}
