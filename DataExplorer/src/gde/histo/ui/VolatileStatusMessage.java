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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.histo.ui;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.device.IDevice;
import gde.log.Logger;

/**
 * Status message text caching.
 * The text and color remain active for a defined number of histo window drawing actions.
 * @author Thomas Eickert (USER)
 */
public class VolatileStatusMessage {
	private static final String	$CLASS_NAME	= VolatileStatusMessage.class.getName();
	@SuppressWarnings("unused")
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	private final IDevice				device			= Analyzer.getInstance().getActiveDevice();
	private final Channel				channel			= Analyzer.getInstance().getActiveChannel();
	private final String				objectKey		= Settings.getInstance().getActiveObjectKey();

	private final String				textLine;
	private final int						color;
	private int									remainingAccessCounter;

	public VolatileStatusMessage(String textLine, int swtColor, int accessCounter) {
		this.textLine = textLine;
		this.color = swtColor;
		this.remainingAccessCounter = accessCounter;
	}

	/**
	 * @return true if the desired volatile message already exists
	 */
	public boolean isIdentical(String newTextLine, int swtColor) {
		return !textLine.equals(newTextLine) || color != swtColor;
	}

	/**
	 * @return the status line text or an empty string in case the message is not valid any more
	 */
	public String getTextLine() {
		if (--remainingAccessCounter < 0) throw new UnsupportedOperationException();
		return remainingAccessCounter >= 0 ? this.textLine : GDE.STRING_EMPTY;
	}

	/**
	 * @return the color
	 */
	public int getColor() {
		return this.color;
	}

	public boolean isExpired() {
		if (remainingAccessCounter > 0) {
			boolean isOtherChart = !device.equals(Analyzer.getInstance().getActiveDevice()) || !channel.equals(Analyzer.getInstance().getActiveChannel()) || !objectKey.equals(Settings.getInstance().getActiveObjectKey());
			return isOtherChart;
		} else
			return true;
	}

	@Override
	public String toString() {
		boolean isSameChart = device.equals(Analyzer.getInstance().getActiveDevice()) && channel.equals(Analyzer.getInstance().getActiveChannel()) && objectKey.equals(Settings.getInstance().getActiveObjectKey());
		return "VolatileStatusMessage [textLine=" + this.textLine + ", color=" + this.color + ", remainingAccessCounter=" + this.remainingAccessCounter + ", isSameChart=" + isSameChart + "]";
	}
}
