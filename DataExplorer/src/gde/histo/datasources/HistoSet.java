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

 Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.datasources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.sun.istack.Nullable;

import gde.data.Record.DataType;
import gde.device.IChannelItem;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.ExtendedVault;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Level;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * Facade of the history module.
 * Supports the selection of histo vaults and provides a trail recordset based on the vaults.
 * @author Thomas Eickert
 */
public final class HistoSet {
	private static final String	$CLASS_NAME										= HistoSet.class.getName();
	private static final Logger	log														= Logger.getLogger($CLASS_NAME);

	private static final double	TOLERANCE											= .000000001;

	private VaultPicker					vaultPicker;

	/**
	 * We allow 1 lower and 1 upper outlier for a log with 740 measurements
	 */
	public static final double	OUTLIER_SIGMA_DEFAULT					= 3.;
	/**
	 * Specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>).<br>
	 * Tolerance interval: <em>TI = &plusmn; z * &sigma; with z >= 0</em><br>
	 * Outliers are identified only if they lie beyond this limit.
	 */
	public static final double	OUTLIER_RANGE_FACTOR_DEFAULT	= 2.;
	/**
	 * Outlier detection for the summary graphics.
	 * We allow 1 outlier for 6 vaults.
	 */
	public static final double	SUMMARY_OUTLIER_SIGMA					= 1.36;
	/**
	 * Specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>).<br>
	 * Tolerance interval: <em>TI = &plusmn; z * &sigma; with z >= 0</em><br>
	 * Outliers are identified only if they lie beyond this limit.
	 */
	public static final double	SUMMARY_OUTLIER_RANGE_FACTOR	= 9.;

	/**
	 * Defines the first step during rebuilding the histoset data.
	 * A minimum of steps may be selected for performance reasons.
	 */
	public enum RebuildStep {
		/**
		 * starts from scratch on
		 */
		A_HISTOSET(6),
		/**
		 * starts building the histo vaults
		 */
		B_HISTOVAULTS(5),
		/**
		 * starts building the trail recordset from the histo vaults
		 */
		C_TRAILRECORDSET(4),
		/**
		 * starts refreshing the trail data from the histo vaults
		 */
		D_TRAIL_DATA(3),
		/**
		 * starts updating the graphics and table
		 */
		E_USER_INTERFACE(2),
		/**
		 * starts with a file check only which decides which update activity is required --- not implemented
		 */
		F_FILE_CHECK(1),
		/**
		 * starts with a file check only which decides which update activity is required --- not implemented
		 */
		G_SELECTOR_COMBO(0);

		/** zero is the lowest scopeOfWork. */
		public final int					scopeOfWork;

		/** use this to avoid repeatedly cloning actions instead of values() */
		public static RebuildStep	VALUES[]	= values();

		private RebuildStep(int scopeOfWork) {
			this.scopeOfWork = scopeOfWork;
		}

		/**
		 * @return true if {@code comparisonStep} is included in or equal to this rebuild step
		 */
		public boolean isEqualOrBiggerThan(RebuildStep comparisonStep) {
			return this.scopeOfWork >= comparisonStep.scopeOfWork;
		}
	};

	/**
	 * Translate a normalized histo vault value into to values represented. </br>
	 * Data types might require a special normalization (e.g. GPS coordinates).
	 * This is the equivalent of {@code device.translateValue} for data dedicated to the histo vault.
	 * @return double of device dependent value
	 */
	public static double decodeVaultValue(IChannelItem channelItem, double value) {
		final double newValue;
		switch (channelItem.getUnifiedDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			newValue = value / 1000.;
			break;

		default:
			double factor = channelItem.getFactor(); // != 1 if a unit translation is required
			double offset = channelItem.getOffset(); // != 0 if a unit translation is required
			double reduction = channelItem.getReduction(); // != 0 if a unit translation is required
			newValue = (value - reduction) * factor + offset;
			break;
		}
		return newValue;
	}

	/**
	 * This is the equivalent of {@code device.translateValue} for data dedicated to the histo vault.
	 * @return the translated value for a value which represents a difference
	 */
	public static double decodeDeltaValue(IChannelItem channelItem, double value) {
		double newValue = 0;
		switch (channelItem.getUnifiedDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			newValue = value / 1000.;
			break;

		default:
			newValue = value * channelItem.getFactor();
		}
		return newValue;
	}

	/**
	 * Reverse translate a measured value into a normalized histo vault value.</br>
	 * Data types might require a special normalization (e.g. GPS coordinates).
	 * This is the equivalent of {@code device.reverseTranslateValue} for data dedicated to the histo vault.
	 * @return the normalized histo vault value (as a multiplied int for fractional digits support)
	 */
	public static double encodeVaultValue(IChannelItem channelItem, double value) {
		final double newValue;
		switch (channelItem.getUnifiedDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			// this might be obsolete as isGPSCoordinates should do the job
			newValue = value * 1000.;
			break;

		default:
			double factor = channelItem.getFactor(); // != 1 if a unit translation is required
			double offset = channelItem.getOffset(); // != 0 if a unit translation is required
			double reduction = channelItem.getReduction(); // != 0 if a unit translation is required
			newValue = (value - offset) / factor + reduction;
			break;
		}
		return newValue;
	}

	/**
	 * Translate a normalized histo vault value into to values represented. </br>
	 * Data types might require a special normalization (e.g. GPS coordinates).
	 * This is the equivalent of {@code device.translateValue} for data dedicated to the histo vault.
	 * @return double of device dependent value
	 */
	@Deprecated // use method with IChannelItem parameter instead
	public static double decodeVaultValue(TrailRecord record, double value) {
		final double newValue;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			newValue = value / 1000.;
			break;

		default:
			double factor = record.getFactor(); // != 1 if a unit translation is required
			double offset = record.getOffset(); // != 0 if a unit translation is required
			double reduction = record.getReduction(); // != 0 if a unit translation is required

			newValue = (value - reduction) * factor + offset;
			break;
		}
		log.fine(() -> "for " + record.getName() + " in value = " + value + " out value = " + newValue);
		return newValue;
	}

	/**
	 * This is the equivalent of {@code device.translateValue} for data dedicated to the histo vault.
	 * @return the translated value for a value which represents a difference
	 */
	@Deprecated // use method with IChannelItem parameter instead
	public static double decodeDeltaValue(TrailRecord record, double value) {
		double newValue = 0;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			newValue = value / 1000.;
			break;

		default:
			newValue = value * record.getFactor();
		}
		return newValue;
	}

	/**
	 * Reverse translate a calculated trail value (e.g. a calculated scale end value) into a normalized histo vault value.
	 * @return the normalized histo vault value (as a multiplied int for fractional digits support)
	 */
	public static double encodeVaultValue(TrailRecord record, double value) {
		final double newValue;
		if (isGpsCoordinates(record)) {
			newValue = value * 1000.;
		} else {
			double factor = record.getFactor(); // != 1 if a unit translation is required
			double offset = record.getOffset(); // != 0 if a unit translation is required
			double reduction = record.getReduction(); // != 0 if a unit translation is required

			newValue = (value - offset) / factor + reduction;
		}
		log.fine(() -> "for " + record.getName() + " in value = " + value + " out value = " + newValue);
		return newValue;

	}

	public static boolean isGpsCoordinates(TrailRecord record) {
		return record.getDataType() == DataType.GPS_LATITUDE || record.getDataType() == DataType.GPS_LONGITUDE;
	}

	/**
	 * Returns {@code true} if {@code a} and {@code b} are within {@code tolerance} of each other.
	 *
	 * <p>
	 * Technically speaking, this is equivalent to {@code Math.abs(a - b) <= tolerance ||
	 * Double.valueOf(a).equals(Double.valueOf(b))}.
	 *
	 * <p>
	 * Notable special cases include:
	 *
	 * <ul>
	 * <li>All NaNs are fuzzily equal.
	 * <li>If {@code a == b}, then {@code a} and {@code b} are always fuzzily equal.
	 * <li>Positive and negative zero are always fuzzily equal.
	 * <li>If {@code tolerance} is zero, and neither {@code a} nor {@code b} is NaN, then {@code a}
	 * and {@code b} are fuzzily equal if and only if {@code a == b}.
	 * <li>With {@link Double#POSITIVE_INFINITY} tolerance, all non-NaN values are fuzzily equal.
	 * <li>With finite tolerance, {@code Double.POSITIVE_INFINITY} and {@code
	 *       Double.NEGATIVE_INFINITY} are fuzzily equal only to themselves.
	 * </ul>
	 *
	 * <p>
	 * This is reflexive and symmetric, but <em>not</em> transitive, so it is <em>not</em> an
	 * equivalence relation and <em>not</em> suitable for use in {@link Object#equals}
	 * implementations.
	 *
	 * ET removed throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
	 * @since 13.0
	 * @author GUAVA 22.0
	 */
	// todo move into gde.utils.MathUtils
	public static boolean fuzzyEquals(double a, double b) { // ET , double tolerance) {
		// ET MathPreconditions.checkNonNegative("tolerance", tolerance);
		return Math.copySign(a - b, 1.0) <= TOLERANCE
				// copySign(x, 1.0) is a branch-free version of abs(x), but with different NaN semantics
				|| (a == b) // needed to ensure that infinities equal themselves
				|| (Double.isNaN(a) && Double.isNaN(b));
	}

	/**
	 * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly-equal values.
	 *
	 * <p>
	 * This method is equivalent to
	 * {@code fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a, b)}. In particular, like
	 * {@link Double#compare(double, double)}, it treats all NaN values as equal and greater than all
	 * other values (including {@link Double#POSITIVE_INFINITY}).
	 *
	 * <p>
	 * This is <em>not</em> a total ordering and is <em>not</em> suitable for use in
	 * {@link Comparable#compareTo} implementations. In particular, it is not transitive.
	 *
	 * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
	 * @since 13.0
	 * @author GUAVA 22.0
	 */
	// todo move into gde.utils.MathUtils
	public static int fuzzyCompare(double a, double b) { // ET , double tolerance) {
		if (fuzzyEquals(a, b)) {
			return 0;
		} else if (a < b) {
			return -1;
		} else if (a > b) {
			return 1;
		} else {
			return Boolean.compare(Double.isNaN(a), Double.isNaN(b));
		}
	}

	/**
	 * collect the strongest rebuild action which was not performed (e.g. tab was not selected)
	 */
	private RebuildStep rebuildStepInvisibleTab = HistoSet.RebuildStep.E_USER_INTERFACE;

	public HistoSet() {
		initialize();
	}

	/**
	 *
	 */
	public void initialize() {
		this.vaultPicker = new VaultPicker();
		this.vaultPicker.initialize();
	}

	/**
	 * Determine histo files, build a recordset based job list and read from the log file or the cache for each job.
	 * Populate the trail recordset.
	 * Disregard rebuild steps if histo file paths have changed which may occur if new files have been added by the user
	 * or the device, channel or object was modified.
	 * @param rebuildStep
	 * @return true if the HistoSet was rebuilt
	 */
	public boolean rebuild4Screening(RebuildStep rebuildStep) //
			throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		return this.vaultPicker.rebuild4Screening(rebuildStep);
	}

	@Nullable // i.e. if rebuild thread is not finished
	public TrailRecordSet getTrailRecordSet() {
		return this.vaultPicker.getTrailRecordSet();
	}

	/**
	 * Determine the rebuild action for the invisible histo tabs or those which are not selected.
	 * @param isRebuilt
	 */
	public void setRebuildStepInvisibleTabs(RebuildStep rebuildStep, boolean isRebuilt) {
		RebuildStep performedRebuildStep = isRebuilt ? RebuildStep.B_HISTOVAULTS : rebuildStep;
		// determine the maximum rebuild priority from the past updates
		RebuildStep maximumRebuildStep = this.getRebuildStepInvisibleTab().scopeOfWork > performedRebuildStep.scopeOfWork
				? this.getRebuildStepInvisibleTab() : performedRebuildStep;
		// the invisible tabs need subscribe a redraw only if there was a rebuild with a higher priority than the standard file check request
		this.rebuildStepInvisibleTab = maximumRebuildStep.scopeOfWork > this.getRebuildStepInvisibleTab().scopeOfWork ? RebuildStep.E_USER_INTERFACE
				: RebuildStep.F_FILE_CHECK;
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, String.format("rebuildStep=%s  performedRebuildStep=%s  maximumRebuildStep=%s  rebuildStepInvisibleTab=%s", //$NON-NLS-1$
					rebuildStep, performedRebuildStep, maximumRebuildStep, this.getRebuildStepInvisibleTab()));
	}

	public RebuildStep getRebuildStepInvisibleTab() {
		return rebuildStepInvisibleTab;
	}

	public SourceFolders getSourceFolders() {
		return this.vaultPicker.getSourceFolders();
	}

	/**
	 * @return the paths which have been ignored on a file basis or suppressed on a recordset basis
	 */
	public List<Path> getExcludedPaths() {
		List<Path> result = this.vaultPicker.getExcludedFiles();
		for (ExtendedVault vault : this.vaultPicker.getSuppressedTrusses()) {
			result.add(vault.getLoadFileAsPath());
		}
		return result;
	}

	public String getDirectoryScanStatistics() {
		return Messages.getString(MessageIds.GDE_MSGI0064, //
				new Object[] { String.format("%,d", this.vaultPicker.getDirectoryFilesCount()), //
						String.format("%,d", this.vaultPicker.getMatchingFilesCount()), //
						String.format("%.2f", this.vaultPicker.getRecordSetBytesSum() / 1024 / 1024.), //
						String.format("%.2f", this.vaultPicker.getElapsedTime_ms() / 1000.), //
						String.format("%,d", this.vaultPicker.getTrussesCount() + this.vaultPicker.getSuppressedTrusses().size()), //
						String.format("%,d", this.vaultPicker.getTimeStepSize()) });

	}

}
