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

    Copyright (c) 2018,2019 Thomas Eickert
****************************************************************************************/

package gde;

import gde.data.Channels;
import gde.device.IDevice;

/**
 * Kernel for analyzing logging data.
 * Use this for unit test without integrated UI.
 * @author Thomas Eickert (USER)
 */
public class TestAnalyzer extends Analyzer {

	TestAnalyzer() {
		super();
	}

	@SuppressWarnings("unused")
	private TestAnalyzer(TestAnalyzer analyzer) {
		super(analyzer);
	}

	/**
	 * Use this for non-histo junit tests only.
	 */
	@Override
	public void setActiveDevice(IDevice device) {
		if (device != null) {
			this.activeDevice = device;
		}
	}

	/**
	 * Use this for non-histo junit tests only.
	 */
	public void setChannels(Channels channels) {
		this.channels = channels;
	}

	@Override
	public TestAnalyzer getReplica() {
		return this;
	}

}
