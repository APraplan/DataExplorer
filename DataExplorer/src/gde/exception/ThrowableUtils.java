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

package gde.exception;

/**
 * Helper for checked and unchecked exceptions.
 * @author Thomas Eickert (USER)
 */
public class ThrowableUtils {

	/**
	 * Cast a CheckedException as an unchecked one.
	 * @param throwable to cast
	 * @param <T> the type of the Throwable
	 * @return this method will never return a Throwable instance, it will just throw it.
	 * @throws T the throwable as an unchecked throwable
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> RuntimeException rethrow(Throwable throwable) throws T {
		throw (T) throwable; // rely on vacuous cast
	}

}
