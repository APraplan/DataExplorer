//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.09.29 at 06:30:39 PM CEST 
//


package gde.device;

import gde.messages.MessageIds;
import gde.messages.Messages;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DesktopPropertyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DesktopPropertyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{}desktop_property_types" />
 *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="target_reference_ordinal" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DesktopPropertyType")
public class DesktopPropertyType {

    @XmlAttribute(required = true)
    protected DesktopPropertyTypes name;
    @XmlAttribute(required = true)
    protected boolean value;
    @XmlAttribute
    protected String description;
    @XmlAttribute(name = "target_reference_ordinal")
    protected Integer targetReferenceOrdinal;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link DesktopPropertyTypes }
     *     
     */
    public DesktopPropertyTypes getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link DesktopPropertyTypes }
     *     
     */
    public void setName(DesktopPropertyTypes value) {
        this.name = value;
    }

    /**
     * Gets the value of the value property.
     * 
     */
    public boolean isValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     */
    public void setValue(boolean value) {
        this.value = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the targetReferenceOrdinal property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getTargetReferenceOrdinal() {
        return targetReferenceOrdinal;
    }

    /**
     * Sets the value of the targetReferenceOrdinal property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTargetReferenceOrdinal(Integer value) {
        this.targetReferenceOrdinal = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link DesktopPropertyTypes }
     *     
     */
    public String getLocalizedName() {
    	switch (name) {
    	default:
    	case ANALOG_TAB:
        return Messages.getString(MessageIds.GDE_MSGT0178);
    	case DIGITAL_TAB:
        return Messages.getString(MessageIds.GDE_MSGT0180);
    	case TABLE_TAB:
        return Messages.getString(MessageIds.GDE_MSGT0182);
    	case VOLTAGE_PER_CELL_TAB:
        return Messages.getString(MessageIds.GDE_MSGT0184);
    	case UTILITY_DEVICE_TAB:
        return Messages.getString(MessageIds.GDE_MSGT0224);
    	case UTILITY_GRAPHICS_TAB:
        return Messages.getString(MessageIds.GDE_MSGT0226);
    	}
    }
}
