package gde.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import gde.GDE;
import gde.config.Settings;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * Dates, times and durations in the format applicable for the user's locale and for the user's UTC setting.
 * Replaces StringHelper.getFormatedTime(...).
 * Use this class for all user output, e.g. display, print.
 * Do not use for file output or for log output.
 * May require more detailed localization for additional languages, e.g. '2j 11:34' ('j' stands for 'day' in French).
 * @author Thomas Eickert
 */
public final class LocalizedDateTime {

	public static void testUtc() {
		long ts = System.currentTimeMillis();
		Date localTime = new Date(ts);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); //$NON-NLS-1$
		SimpleDateFormat sdfUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$

		System.out.println(LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmssSSS, ts) + "  " + sdf.format(localTime)); //$NON-NLS-1$
		//		2017-01-06 13:00:25.791  2017/01/06 13:00:25
		System.out.println(LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmssSSS, ts) + "  " + sdfUTC.format(localTime)); //$NON-NLS-1$
		//		2017-01-06 13:00:25.791  2017-01-06T13:00:25Z

		// print UTC
		sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		System.out.println(LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmssSSS, ts) + "  " + sdfUTC.format(localTime) + "  " + sdfUTC.format(ts)); //$NON-NLS-1$//$NON-NLS-2$
		//		2017-01-06 13:00:25.791  2017-01-06T12:00:25Z  2017-01-06T12:00:25Z
	}

	public enum DateTimePattern {
		yyyyMMdd_HHmmssSSS(0, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		yyyyMMdd_HHmmss(1, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		yyyyMMdd_HHmm(2, "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		//		yyyyMMdd_HH(3, "yyyy-MM-dd HH", "yyyy-MM-dd'T'HH'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		yyyyMMdd(4, "yyyy-MM-dd", "yyyy-MM-dd'T'"), //$NON-NLS-1$ //$NON-NLS-2$
		yyyyMM(5, "yyyy-MM", "yyyy-MM'T'"), //$NON-NLS-1$ //$NON-NLS-2$
		yyMMdd(6, "yy-MM-dd", "yy-MM-dd'T'"), //$NON-NLS-1$ //$NON-NLS-2$
		//		MMdd_HHmm(7, "MM-dd HH:mm", "MM-dd'T'HH:mm'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		MMdd_HH(13, "MM-dd HH", "MM-dd'T'HH'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		MMdd(14, "MM-dd", "MM-dd'T'"), //$NON-NLS-1$ //$NON-NLS-2$
		dd_HHmm(16, "dd HH:mm", "dd'T'HH:mm'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		HHmmssSSS(17, "HH:mm:ss.SSS", "HH:mm:ss.SSS'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		HHmmss(18, "HH:mm:ss", "HH:mm:ss'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		HHmm(19, "HH:mm", "HH:mm'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		mmssSSS(20, "mm:ss.SSS", "mm:ss.SSS'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		ssSSS(28, "ss.SSS", "ss.SSS'Z'"); //$NON-NLS-1$ //$NON-NLS-2$

		private final String	userPattern;
		private final String	userUtcPattern;

		private DateTimePattern(@SuppressWarnings("unused") int displaySequence, String userPattern, String userUtcPattern) {
			this.userPattern = userPattern;
			this.userUtcPattern = userUtcPattern;
		}

		/**
		 * @return the standard pattern
		 */
		@Override
		public String toString() {
			return this.userPattern;
		}

		public SimpleDateFormat getFormatter() {
			final SimpleDateFormat sdf;
			if (Settings.getInstance().isDateTimeUtc()) {
				sdf = new SimpleDateFormat(this.userUtcPattern);
				sdf.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
			} else {
				sdf = new SimpleDateFormat(this.userPattern);
			}
			return sdf;
		}
	}

	public enum DurationPattern {
		yy_MM_dd_HH_mm_ss_SSS(0, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss.SSS"), //$NON-NLS-1$ //$NON-NLS-2$
		yy_MM_dd_HH_mm(2, "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm'Z'"), //$NON-NLS-1$ //$NON-NLS-2$
		yy_MM_dd(6, "yy-MM-dd", "yy-MM-dd"), //$NON-NLS-1$ //$NON-NLS-2$
		MM_dd_HH_mm_ss_SSS(9, "MM-dd HH:mm:ss.SSS", "MM-dd HH:mm:ss.SSSZ"), //$NON-NLS-1$ //$NON-NLS-2$
		MM_dd_HH(13, "MM-dd HH", "MM-dd HH"), //$NON-NLS-1$ //$NON-NLS-2$
		MM_dd(14, "MM-dd HH", "MM-dd HH"), //$NON-NLS-1$ //$NON-NLS-2$
		dd_HH_mm_ss_SSS(15, "dd HH:mm:ss.SSS", "dd HH:mm:ss.SSSZ"), //$NON-NLS-1$ //$NON-NLS-2$
		dd_HH_mm(16, "dd HH:mm", "dd HH:mm"), //$NON-NLS-1$ //$NON-NLS-2$
		HH_mm_ss_SSS(17, "HH:mm:ss.SSS", "HH:mm:ss.SSS"), //$NON-NLS-1$ //$NON-NLS-2$
		HH_mm_ss(18, "HH:mm:ss", "HH:mm:ss"), //$NON-NLS-1$ //$NON-NLS-2$
		HH_mm(19, "HH:mm", "HH:mm"), //$NON-NLS-1$ //$NON-NLS-2$
		mm_ss_SSS(24, "mm:ss.SSS", "mm:ss.SSS"), //$NON-NLS-1$ //$NON-NLS-2$
		ss_SSS(28, "ss.SSS", "ss.SSS"); //$NON-NLS-1$ //$NON-NLS-2$

		private final String								userPattern;
		private final String								userUtcPattern;

		/**
		 * use this to avoid repeatedly cloning actions instead of values()
		 */
		public static final DurationPattern	VALUES[]	= values();

		private DurationPattern(@SuppressWarnings("unused") int displaySequence, String userPattern, String userUtcPattern) {
			this.userPattern = userPattern;
			this.userUtcPattern = userUtcPattern;
		}

		/**
		 * @return the standard pattern
		 */
		@Override
		public String toString() {
			return this.userPattern;
		}

		public SimpleDateFormat getFormatter() {
			final SimpleDateFormat sdf;
			if (Settings.getInstance().isDateTimeUtc()) {
				sdf = new SimpleDateFormat(this.userUtcPattern);
			} else {
				sdf = new SimpleDateFormat(this.userPattern);
			}
			// avoid wrong number of hours due to a locale setting other than UTC/GMT
			sdf.getTimeZone().setRawOffset(0);
			return sdf;
		}

		/**
		 * @param dateTimePattern
		 * @return the duration pattern which fits best to the datetime pattern
		 */
		public static DurationPattern get(DateTimePattern dateTimePattern) {
			DurationPattern result = DurationPattern.dd_HH_mm;
			for (DurationPattern durationPattern : DurationPattern.VALUES) {
				if (durationPattern.toString().equals(dateTimePattern.toString())) {
					result = durationPattern;
					break;
				}
			}
			return result;
		}
	}

	/**
	 * @param formatPattern
	 * @param timeStamp_ms
	 * @return the localized or UTC formated date / time (e.g. "yyyy-MM-dd HH:mm:ss.SSS" or "yyyy-MM-ddTHH:mm:ss.SSSZ") or a duration up to "MM:dd:HH:mm:ss.SSS"
	 */
	public static String getFormatedTime(DateTimePattern formatPattern, long timeStamp_ms) {
		if (timeStamp_ms <= GDE.ONE_HOUR_MS * 24 * 365 * 11) {
			return LocalizedDateTime.getFormatedDuration(DurationPattern.get(formatPattern), timeStamp_ms);
		} else {
			return formatPattern.getFormatter().format(timeStamp_ms);
		}
	}

	public static String getFormatedTime(DateTimePattern formatPattern, double timeStamp_ms) {
		return LocalizedDateTime.getFormatedTime(formatPattern, (long) timeStamp_ms);
	}

	/**
	 * @param formatPattern up to a maximum of "yy-MM-dd HH:mm:ss.SSS"
	 * @param duration_ms is a non-negative 0-based time value based on the Java epoch of 1970-01-01T00:00:00Z.
	 * @return the formated duration with a maximum of 11 years (e.g. "yy-MM-dd HH:mm:ss.SSS")
	 */
	public static String getFormatedDuration(DurationPattern formatPattern, long duration_ms) {
		// 11 years duration should be sufficient
		if (duration_ms > GDE.ONE_HOUR_MS * 24 * 365 * 11) {
			throw new UnsupportedOperationException();
		}
		return formatPattern.getFormatter().format(duration_ms);
	}

	/**
	 * @param timestamp1_ms
	 * @param timestamp2_ms
	 * @return the duration between the epoch timestamps ('dd HH:mm' preceded by the number of months if applicable)
	 */
	public static String getFormatedDistance(long timestamp1_ms, long timestamp2_ms) {
		if (Math.abs(timestamp1_ms - timestamp2_ms) > GDE.ONE_HOUR_MS * 24) {
			Period y = Period.between(Instant.ofEpochMilli(timestamp2_ms).atZone(ZoneId.systemDefault()).toLocalDate(), Instant.ofEpochMilli(timestamp1_ms).atZone(ZoneId.systemDefault()).toLocalDate());
			if (Math.abs(y.toTotalMonths()) > 0)
				return String.format("%d%s %d%s", Math.abs(y.toTotalMonths()), Messages.getString(MessageIds.GDE_MSGT0868), Math.abs(y.getDays()), Messages.getString(MessageIds.GDE_MSGT0869)); //$NON-NLS-1$
			else
				return String.format("%d%s %s", Math.abs(y.getDays()), Messages.getString(MessageIds.GDE_MSGT0869), getFormatedDuration(DurationPattern.HH_mm, Math.abs(timestamp1_ms - timestamp2_ms))); //$NON-NLS-1$
		} else {
			return getFormatedDuration(DurationPattern.HH_mm, Math.abs(timestamp1_ms - timestamp2_ms));
		}
	}

}
