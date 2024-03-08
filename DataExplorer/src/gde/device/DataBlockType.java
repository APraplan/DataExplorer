//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.01.18 at 06:46:54 PM GMT+01:00 
//

package gde.device;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>Java class for DataBlockType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataBlockType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="format">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="type" use="required" type="{}format_types" />
 *                 &lt;attribute name="size" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *                 &lt;attribute name="source" type="{}input_types" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="checkSum" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="format" type="{}format_types" default="BINARY" />
 *                 &lt;attribute name="type" type="{}checkSum_types" default="XOR" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="timeUnit" type="{}time_unit_types" minOccurs="0"/>
 *         &lt;element name="separator" type="{}comma_separator_types" minOccurs="0"/>
 *         &lt;element name="leader" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="trailer" type="{http://www.w3.org/2001/XMLSchema}hexBinary" minOccurs="0"/>
 *         &lt;element name="preferredDataLocation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="preferredFileExtention" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataBlockType", propOrder = {
    "format",
    "checkSum",
    "timeUnit",
    "separator",
    "leader",
    "trailer",
    "preferredDataLocation",
    "preferredFileExtention"
})
public class DataBlockType {

	@XmlElement(required = true)
	protected List<DataBlockType.Format>	format;
	protected DataBlockType.CheckSum			checkSum;
	@XmlElement(defaultValue = "msec")
	protected TimeUnitTypes								timeUnit;
	@XmlElement(defaultValue = "semicolon")
	protected CommaSeparatorTypes					separator;
	@XmlElement(defaultValue = "$")
	protected String											leader;
	@XmlElement(type = String.class, defaultValue = "0D0A")
	@XmlJavaTypeAdapter(HexBinaryAdapter.class)
	@XmlSchemaType(name = "hexBinary")
	protected byte[]											trailer;
	protected String											preferredDataLocation;
	@XmlElement(defaultValue = "*.csv")
	protected String											preferredFileExtention;

	/**
	 * Gets the value of the format property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the format property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getFormat().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link DataBlockType.Format }
	 * 
	 * 
	 */
	public List<DataBlockType.Format> getFormat() {
		if (format == null) {
			format = new ArrayList<DataBlockType.Format>();
		}
		return this.format;
	}

	/**
	 * Gets the value of the checkSum property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link DataBlockType.CheckSum }
	 *     
	 */
	public DataBlockType.CheckSum getCheckSum() {
		return checkSum;
	}

	/**
	 * Sets the value of the checkSum property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link DataBlockType.CheckSum }
	 *     
	 */
	public void setCheckSum(DataBlockType.CheckSum value) {
		this.checkSum = value;
	}

	/**
	 * Gets the value of the timeUnit property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link TimeUnitTypes }
	 *     
	 */
	public TimeUnitTypes getTimeUnit() {
		if (timeUnit == null) {
			return TimeUnitTypes.MSEC;
		}
		return timeUnit;
	}

	/**
	 * Sets the value of the timeUnit property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link TimeUnitTypes }
	 *     
	 */
	public void setTimeUnit(TimeUnitTypes value) {
		this.timeUnit = value;
	}

	/**
	 * Gets the value of the separator property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link CommaSeparatorTypes }
	 *     
	 */
	public CommaSeparatorTypes getSeparator() {
		if (separator == null) {
			return CommaSeparatorTypes.SEMICOLON;
		}
		return separator;
	}

	/**
	 * Sets the value of the separator property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link CommaSeparatorTypes }
	 *     
	 */
	public void setSeparator(CommaSeparatorTypes value) {
		this.separator = value;
	}

	/**
	 * Gets the value of the leader property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLeader() {
		if (leader == null) {
			return "$";
		}
		return leader;
	}

	/**
	 * Sets the value of the leader property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setLeader(String value) {
		this.leader = value;
	}

	/**
	 * Gets the value of the trailer property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public byte[] getTrailer() {
		if (trailer == null) {
			return new HexBinaryAdapter().unmarshal("0D0A");
		}
		return trailer;
	}

	/**
	 * Sets the value of the trailer property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setTrailer(byte[] value) {
		this.trailer = value;
	}

	/**
	 * Gets the value of the preferredDataLocation property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getPreferredDataLocation() {
		return preferredDataLocation;
	}

	/**
	 * Sets the value of the preferredDataLocation property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setPreferredDataLocation(String value) {
		this.preferredDataLocation = value;
	}

	/**
	 * Gets the value of the preferredFileExtention property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getPreferredFileExtention() {
		if (preferredFileExtention == null) {
			return "*.csv";
		}
		return preferredFileExtention;
	}

	/**
	 * Sets the value of the preferredFileExtention property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setPreferredFileExtention(String value) {
		this.preferredFileExtention = value;
	}

	/**
	 * <p>Java class for anonymous complex type.
	 * 
	 * <p>The following schema fragment specifies the expected content contained within this class.
	 * 
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;attribute name="format" type="{}format_types" default="BINARY" />
	 *       &lt;attribute name="type" type="{}checkSum_types" default="XOR" />
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "")
	public static class CheckSum {

		@XmlAttribute
		protected FormatTypes		format;
		@XmlAttribute
		protected CheckSumTypes	type;

		/**
		 * Gets the value of the format property.
		 * 
		 * @return
		 *     possible object is
		 *     {@link FormatTypes }
		 *     
		 */
		public FormatTypes getFormat() {
			if (format == null) {
				return FormatTypes.BINARY;
			}
			return format;
		}

		/**
		 * Sets the value of the format property.
		 * 
		 * @param value
		 *     allowed object is
		 *     {@link FormatTypes }
		 *     
		 */
		public void setFormat(FormatTypes value) {
			this.format = value;
		}

		/**
		 * Gets the value of the type property.
		 * 
		 * @return
		 *     possible object is
		 *     {@link CheckSumTypes }
		 *     
		 */
		public CheckSumTypes getType() {
			if (type == null) {
				return CheckSumTypes.XOR;
			}
			return type;
		}

		/**
		 * Sets the value of the type property.
		 * 
		 * @param value
		 *     allowed object is
		 *     {@link CheckSumTypes }
		 *     
		 */
		public void setType(CheckSumTypes value) {
			this.type = value;
		}

	}

	/**
	 * <p>Java class for anonymous complex type.
	 * 
	 * <p>The following schema fragment specifies the expected content contained within this class.
	 * 
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;attribute name="type" use="required" type="{}format_types" />
	 *       &lt;attribute name="size" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "")
	public static class Format {

		@XmlAttribute(required = true)
		protected InputTypes	inputType;
		@XmlAttribute(required = true)
		protected FormatTypes	formatType;
		@XmlAttribute(required = true)
		protected Integer			size;

		/**
		 * Gets the value of the source property.
		 * 
		 * @return
		 *     possible object is
		 *     {@link InputTypes }
		 *     
		 */
		public InputTypes getInputType() {
			if (inputType == null) {
				return InputTypes.FILE_IO;
			}
			return inputType;
		}

		/**
		 * Sets the value of the source property.
		 * 
		 * @param value
		 *     allowed object is
		 *     {@link InputTypes }
		 *     
		 */
		public void setInputType(InputTypes value) {
			this.inputType = value;
		}

		/**
		 * Gets the value of the type property.
		 * 
		 * @return
		 *     possible object is
		 *     {@link FormatTypes }
		 *     
		 */
		public FormatTypes getType() {
			if (formatType == null) {
				return FormatTypes.BYTE;
			}
			return this.formatType;
		}

		/**
		 * Sets the value of the type property.
		 * 
		 * @param value
		 *     allowed object is
		 *     {@link FormatTypes }
		 *     
		 */
		public void setFormatType(FormatTypes value) {
			this.formatType = value;
		}

		/**
		 * Gets the value of the size property.
		 * 
		 * @return
		 *     possible object is
		 *     {@link Integer }
		 *     
		 */
		public Integer getSize() {
			return size;
		}

		/**
		 * Sets the value of the size property.
		 * 
		 * @param value
		 *     allowed object is
		 *     {@link Integer }
		 *     
		 */
		public void setSize(Integer value) {
			this.size = value;
		}
	}
}
