//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.03.29 at 04:00:35 pm CEST 
//


package gde.device.estner;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}Dialog-Settings"/>
 *         &lt;element ref="{}Akku-Settings"/>
 *         &lt;element ref="{}Parameter-Settings"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dialogSettings",
    "akkuSettings",
    "parameterSettings"
})
@XmlRootElement(name = "AM_Settings")
public class AMSettings {

    @XmlElement(name = "Dialog-Settings", required = true)
    protected DialogSettings dialogSettings;
    @XmlElement(name = "Akku-Settings", required = true)
    protected AkkuSettings akkuSettings;
    @XmlElement(name = "Parameter-Settings", required = true)
    protected ParameterSettings parameterSettings;

    /**
     * Gets the value of the dialogSettings property.
     * 
     * @return
     *     possible object is
     *     {@link DialogSettings }
     *     
     */
    public DialogSettings getDialogSettings() {
        return dialogSettings;
    }

    /**
     * Sets the value of the dialogSettings property.
     * 
     * @param value
     *     allowed object is
     *     {@link DialogSettings }
     *     
     */
    public void setDialogSettings(DialogSettings value) {
        this.dialogSettings = value;
    }

    /**
     * Gets the value of the akkuSettings property.
     * 
     * @return
     *     possible object is
     *     {@link AkkuSettings }
     *     
     */
    public AkkuSettings getAkkuSettings() {
        return akkuSettings;
    }

    /**
     * Sets the value of the akkuSettings property.
     * 
     * @param value
     *     allowed object is
     *     {@link AkkuSettings }
     *     
     */
    public void setAkkuSettings(AkkuSettings value) {
        this.akkuSettings = value;
    }

    /**
     * Gets the value of the parameterSettings property.
     * 
     * @return
     *     possible object is
     *     {@link ParameterSettings }
     *     
     */
    public ParameterSettings getParameterSettings() {
        return parameterSettings;
    }

    /**
     * Sets the value of the parameterSettings property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParameterSettings }
     *     
     */
    public void setParameterSettings(ParameterSettings value) {
        this.parameterSettings = value;
    }

}
