/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.data;

import gde.GDE;
import gde.config.Settings;
import gde.io.ObjectDataReaderWriter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextPrintOptions;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.printing.Printer;

/**
 * @author Winfried Brügmann
 * Class to handle object relevant data, like object key, object description, object type, object image, ....
 */
public class ObjectData {
	public static final String	STRING_STYLED_TEXT_DEFAULT	= Messages.getString(MessageIds.GDE_MSGT0433);

	String							key;
	String							type;
	String							activationDate;
	String							status;
	Image								image;
	Properties					properties;
	int									imageWidth = 400;
	int									imageHeight = 300;
	String							styledText;
	StyleRange[]				styleRanges;
	Font								font;
	String							fullQualifiedObjectFilePath;

	/**
	 * Constructor for basic initialisation
	 * @param newKey
	 * @param newType
	 * @param newActivationDate
	 * @param newStatus
	 * @param newImage
	 * @param newStyledText
	 * @param newStyleRanges
	 */
	public ObjectData(String newKey, String newType, String newActivationDate, String newStatus, Image newImage, String newStyledText, StyleRange[] newStyleRanges, FontData fd) {
		this.key = newKey;
		this.type = newType;
		this.activationDate = newActivationDate;
		this.status = newStatus;
		this.image = newImage;
		this.styledText = newStyledText;
		this.styleRanges = newStyleRanges.clone();
		this.font = SWTResourceManager.getFont(fd.getName(), fd.getHeight(), fd.getStyle(), false, false);
	}

	/**
	 * Constructor for default initialization
	 * @param objectFilePath
	 */
	public ObjectData(String objectFilePath) {
		this.fullQualifiedObjectFilePath = objectFilePath;
		this.key = objectFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX).substring(objectFilePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1, objectFilePath.lastIndexOf(GDE.CHAR_DOT));
		this.type = Messages.getString(MessageIds.GDE_MSGT0279);
		this.activationDate = Messages.getString(MessageIds.GDE_MSGT0279);
		this.status = Messages.getString(MessageIds.GDE_MSGT0279);
		this.image = null;
		this.styledText = ObjectData.STRING_STYLED_TEXT_DEFAULT;
		this.styleRanges = new StyleRange[] { new StyleRange(0, this.styledText.length(), null, null, SWT.BOLD) };
		this.font = GDE.IS_WINDOWS ? SWTResourceManager.getFont("Microsoft Sans Serif", 10, SWT.NORMAL, false, false) : SWTResourceManager.getFont("Sans Serif", 10, SWT.NORMAL, false, false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * copy constructor
	 */
	public ObjectData(ObjectData objectData) {
		this.fullQualifiedObjectFilePath = Settings.getInstance().getDataFilePath() + objectData.key + GDE.FILE_SEPARATOR + objectData.key + GDE.FILE_ENDING_DOT_ZIP;
		this.key = objectData.key;
		this.type = objectData.type;
		this.activationDate = objectData.activationDate;
		this.status = objectData.status;
		this.image = SWTResourceManager.getImage(objectData.image.getImageData(), this.key, imageWidth, imageHeight, false);
		this.styledText = objectData.styledText;
		Vector<StyleRange> tmpRanges = new Vector<StyleRange>();
		for (StyleRange range : objectData.styleRanges) {
			Color foreground = range.foreground != null ? SWTResourceManager.getColor(range.foreground.getRed(), range.foreground.getGreen(), range.foreground.getBlue()) : null;
			Color background = range.background != null ? SWTResourceManager.getColor(range.background.getRed(), range.background.getGreen(), range.background.getBlue()) : null;
			tmpRanges.add(new StyleRange(range.start, range.length, foreground, background, range.fontStyle));
		}
		this.styleRanges = tmpRanges.toArray(new StyleRange[0]);
	}

	/**
	 * copy constructor
	 */
	public ObjectData(ObjectData objectData, String updateObjectKey) {
		this.fullQualifiedObjectFilePath = Settings.getInstance().getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + updateObjectKey + GDE.STRING_FILE_SEPARATOR_UNIX + updateObjectKey + GDE.FILE_ENDING_DOT_ZIP;
		this.key = objectData.key;
		this.type = objectData.type;
		this.activationDate = objectData.activationDate;
		this.status = objectData.status;
		this.image = objectData.image != null ? SWTResourceManager.getImage(objectData.image.getImageData(), this.key, imageWidth, imageHeight, false) : null;
		this.styledText = objectData.styledText;
		Vector<StyleRange> tmpRanges = new Vector<StyleRange>();
		for (StyleRange range : objectData.styleRanges) {
			Color foreground = range.foreground != null ? SWTResourceManager.getColor(range.foreground.getRed(), range.foreground.getGreen(), range.foreground.getBlue()) : null;
			Color background = range.background != null ? SWTResourceManager.getColor(range.background.getRed(), range.background.getGreen(), range.background.getBlue()) : null;
			tmpRanges.add(new StyleRange(range.start, range.length, foreground, background, range.fontStyle));
		}
		this.styleRanges = tmpRanges.toArray(new StyleRange[0]);
		this.key = updateObjectKey;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * @param newKey the key to set
	 */
	public void setKey(String newKey) {
		this.key = newKey;
	}

	/**
	 * @return the activationDate
	 */
	public String getActivationDate() {
		return this.activationDate;
	}

	/**
	 * @param newActivationDate the activationDate to set
	 */
	public void setActivationDate(String newActivationDate) {
		this.activationDate = newActivationDate;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return this.status;
	}

	/**
	 * @param newStatus the status to set
	 */
	public void setStatus(String newStatus) {
		this.status = newStatus;
	}

	/**
	 * @return the image
	 */
	public Image getImage() {
		return this.image;
	}

	/**
	 * @param newImage the image to set
	 */
	public void setImage(Image newImage) {
		this.image = newImage;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @param newType the type to set
	 */
	public void setType(String newType) {
		this.type = newType;
	}

	/**
	 * @return the styledText
	 */
	public String getStyledText() {
		return this.styledText;
	}

	/**
	 * @param newStyledText the styledText to set
	 */
	public void setStyledText(String newStyledText) {
		this.styledText = newStyledText;
	}

	/**
	 * @return the style ranges
	 */
	public StyleRange[] getStyleRanges() {
		return this.styleRanges;
	}

	/**
	 * @param newStyleRanges the style ranges to set
	 */
	public void setStyleRanges(StyleRange[] newStyleRanges) {
		this.styleRanges = newStyleRanges;
	}

	public synchronized void save() {
		ObjectDataReaderWriter objWriter = new ObjectDataReaderWriter(this);
		objWriter.write();
	}

	public synchronized void load() {
		ObjectDataReaderWriter objReader = new ObjectDataReaderWriter(this);
		objReader.read();
	}

	public void print() {
		StyledTextPrintOptions options = new StyledTextPrintOptions();
		options.header = StyledTextPrintOptions.SEPARATOR + this.fullQualifiedObjectFilePath + StyledTextPrintOptions.SEPARATOR;
		options.footer = StyledTextPrintOptions.SEPARATOR + StyledTextPrintOptions.PAGE_TAG + StyledTextPrintOptions.SEPARATOR + GDE.NAME_LONG;
		options.printLineBackground = true;
		options.printTextBackground = true;
		options.printTextFontStyle = true;
		options.printTextForeground = true;
		options.lineLabels = new String[] {this.key, this.type, this.activationDate, this.status};

		StyledText tmpStyledText = new StyledText(DataExplorer.getInstance().getParent(), SWT.NONE);
		tmpStyledText.setFont(this.font);
		tmpStyledText.setText(this.styledText);
		tmpStyledText.setStyleRanges(this.styleRanges);
		tmpStyledText.print(new Printer()).run();
	}

	/**
	 * @return the imageWidth
	 */
	public int getImageWidth() {
		return imageWidth;
	}

	/**
	 * @param newWidth the imageWidth to set
	 */
	public void setImageWidth(int newWidth) {
		imageWidth = newWidth;
	}

	/**
	 * @return the imageHeight
	 */
	public int getImageHeight() {
		return imageHeight;
	}

	/**
	 * @param newHeight the imageHeight to set
	 */
	public void setImageHeight(int newHeight) {
		imageHeight = newHeight;
	}

	/**
	 * @return the fontData
	 */
	public FontData getFontData() {
		return this.font != null ? this.font.getFontData()[0] : SWTResourceManager.getFont("Sans Serif", 10, SWT.NORMAL, false, false).getFontData()[0]; //$NON-NLS-1$
	}

	/**
	 * @param newFont the fontData to set
	 */
	public void setFont(Font newFont) {
		this.font = newFont;
	}

	/**
	 * @return the fullQualifiedObjectFilePath
	 */
	public String getFullQualifiedObjectFilePath() {
		return this.fullQualifiedObjectFilePath;
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * @param newProperties the properties to set
	 */
	public void setProperties(Properties newProperties) {
		this.properties = (Properties) newProperties.clone();
	}
}
