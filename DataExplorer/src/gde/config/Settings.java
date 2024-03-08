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
    							2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

// import org.eclipse.swt.graphics.Color;
// import org.eclipse.swt.graphics.Point;
// import org.eclipse.swt.graphics.Rectangle;
// import org.xml.sax.SAXException;

import gde.Analyzer;
import gde.DataAccess;
import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.FileUtils;
import gde.utils.RecordSetNameComparator;
import gde.utils.StringHelper;

/**
 * Settings class will read and write/update application settings, like window size and default path ....
 * Is a hybrid singleton supporting cloning.
 * @author Winfried Brügmann
 */
public final class Settings extends Properties {
	private static final long				serialVersionUID								= 26031957;
	private static final Logger			log															= Logger.getLogger(Settings.class.getName());
	private static final String			$CLASS_NAME											= Settings.class.getName();

	private static volatile Settings	instance											= null;																																														// singelton

	public static final String			EMPTY														= "---";																																													//$NON-NLS-1$
	public static final String			EMPTY_SIGNATURE									= Settings.EMPTY + GDE.STRING_SEMICOLON + Settings.EMPTY + GDE.STRING_SEMICOLON + Settings.EMPTY;
	static final String							UNIX_PORT_DEV_TTY								= "/dev/tty";																																											//$NON-NLS-1$
	static final String							WINDOWS_PORT_COM								= "COM";																																													//$NON-NLS-1$
	public static final String			PERMISSION_555									= "555";																																													//$NON-NLS-1$
	public static final String			PATH_RESOURCE										= "resource/";																																										//$NON-NLS-1$
	public static final String			PATH_RESOURCE_TEMPLATE					= "resource/template/";																																						//$NON-NLS-1$

	final static String							HEADER_TEXT											= "# -- DataExplorer Settings File -- ";																													//$NON-NLS-1$
	final static String							DEVICE_BLOCK										= "#[Actual-Device-Port-Settings]";																																// Picolario;Renschler;COM2 //$NON-NLS-1$
	final static String							WINDOW_BLOCK										= "#[Window-Settings]";																																						//$NON-NLS-1$
	final static String							WINDOW_MAXIMIZED								= "window_maximized";																																							//$NON-NLS-1$
	final static String							WINDOW_LEFT											= "window_left";																																									//$NON-NLS-1$
	final static String							WINDOW_TOP											= "window_top";																																										//$NON-NLS-1$
	final static String							WINDOW_WIDTH										= "window_width";																																									//$NON-NLS-1$
	final static String							WINDOW_HEIGHT										= "window_height";																																								//$NON-NLS-1$
	final static String							COOLBAR_ORDER										= "coolbar_order";																																								//$NON-NLS-1$
	final static String							COOLBAR_WRAPS										= "coolbar_wraps";																																								//$NON-NLS-1$
	final static String							COOLBAR_SIZES										= "coolbar_sizes";																																								//$NON-NLS-1$
	final static String							RECORD_COMMENT_VISIBLE					= "record_comment_visible";																																				//$NON-NLS-1$
	final static String							GRAPHICS_HEADER_VISIBLE					= "graphics_header_visible";																																			//$NON-NLS-1$
	final static String							GRAPHICS_AREA_BACKGROUND				= "graphics_area_background";																																			//$NON-NLS-1$
	final static String							GRAPHICS_SURROUND_BACKGRD				= "graphics_surround_backgrd";																																		//$NON-NLS-1$
	final static String							GRAPHICS_BORDER_COLOR						= "graphics_border_color";																																				//$NON-NLS-1$
	final static String							IS_GRAPHICS_SCALE_COLOR					= "is_graphics_scale_color";																																			//$NON-NLS-1$
	final static String							IS_GRAPHICS_NUMBERS_COLOR				= "is_graphics_number_color";																																			//$NON-NLS-1$
	final static String							IS_GRAPHICS_NAME_COLOR					= "is_graphics_text_color";																																				//$NON-NLS-1$
	final static String							IS_GRAPHICS_TICKS_10						= "is_graphics_ticks_bound_10";																																				//$NON-NLS-1$
	final static String							COMPARE_AREA_BACKGROUND					= "compare_area_background";																																			//$NON-NLS-1$
	final static String							COMPARE_SURROUND_BACKGRD				= "compare_surround_backgrd";																																			//$NON-NLS-1$
	final static String							COMPARE_BORDER_COLOR						= "compare_border_color";																																					//$NON-NLS-1$
	final static String							IS_COMPARE_CHANNELCONFIG				= "is_compare_channel_config_name";																																//$NON-NLS-1$
	final static String							UTILITY_AREA_BACKGROUND					= "utility_area_background";																																			//$NON-NLS-1$
	final static String							UTILITY_SURROUND_BACKGRD				= "utility_surround_backgrd";																																			//$NON-NLS-1$
	final static String							UTILITY_BORDER_COLOR						= "utility_border_color";																																					//$NON-NLS-1$
	final static String							STATISTICS_INNER_BACKGROUND			= "statistics_inner_background";																																	//$NON-NLS-1$
	final static String							STATISTICS_SURROUND_BACKGRD			= "statistics_surround_backgrd";																																	//$NON-NLS-1$
	final static String							ANALOG_INNER_BACKGROUND					= "analog_inner_background";																																			//$NON-NLS-1$
	final static String							ANALOG_SURROUND_BACKGRD					= "analog_surround_backgrd";																																			//$NON-NLS-1$
	final static String							DIGITAL_INNER_BACKGROUND				= "digital_inner_background";																																			//$NON-NLS-1$
	final static String							DIGITAL_SURROUND_BACKGRD				= "digital_surround_backgrd";																																			//$NON-NLS-1$
	final static String							CELL_VOLTAGE_INNER_BACKGROUND		= "cell_voltage_inner_background";																																//$NON-NLS-1$
	final static String							CELL_VOLTAGE_SURROUND_BACKGRD		= "cell_voltage_surround_backgrd";																																//$NON-NLS-1$
	final static String							FILE_COMMENT_INNER_BACKGROUND		= "file_comment_inner_background";																																//$NON-NLS-1$
	final static String							FILE_COMMENT_SURROUND_BACKGRD		= "file_comment_surround_backgrd";																																//$NON-NLS-1$
	final static String							OBJECT_DESC_INNER_BACKGROUND		= "object_desciption_inner_background";																														//$NON-NLS-1$
	final static String							OBJECT_DESC_SURROUND_BACKGRD		= "object_desciption_surround_backgrd";																														//$NON-NLS-1$
	final static String							DISPLAY_DENSITY_FONT_CORRECT		= "display_density_font_correction";
	final static String							KMZ_EXPORT_TIME_STEP_MS					= "kmz_export_time_step_ms";
	public final static String			SKIN_COLOR_SCHEMA								= "skin_color_schema";
	public final static String			COLOR_SCHEMA_SYSTEM							= "color_schema_system";
	public final static String			COLOR_SCHEMA_LIGHT							= "color_schema_light";
	public final static String			COLOR_SCHEMA_DARK								= "color_schema_dark";

	final static String							IS_HISTO_ACTIVE									= "is_histo_active";																																							//$NON-NLS-1$
	final static String							BOXPLOT_SCALE_ORDINAL						= "boxplot_scale_ordinal";																																				//$NON-NLS-1$
	final static String							BOXPLOT_SIZE_ADAPTATION_ORDINAL	= "boxplot_size_adaptation_ordinal";																															//$NON-NLS-1$
	final static String							X_SPREAD_GRADE_ORDINAL					= "x_spread_grade_ordinal";																																				//$NON-NLS-1$
	final static String							IS_X_LOGARITHMIC_DISTANCE				= "is_x_logarithmic_distance";																																		//$NON-NLS-1$
	final static String							IS_X_REVERSED										= "is_x_reversed";																																								//$NON-NLS-1$
	final static String							SEARCH_DATAPATH_IMPORTS					= "search_datapath_imports";																																			//$NON-NLS-1$
	final static String							IS_CHANNEL_MIX									= "is_channel_mix";																																								//$NON-NLS-1$
	final static String							SAMPLING_TIMESPAN_ORDINAL				= "sampling_timespan_ordinal";																																		//$NON-NLS-1$
	final static String							IGNORE_LOG_OBJECT_KEY						= "ignore_log_object_key";																																				//$NON-NLS-1$
	final static String							RETROSPECT_MONTHS								= "retrospect_months";																																						//$NON-NLS-1$
	final static String							IS_ZIPPED_CACHE									= "zipped_cache";																																									//$NON-NLS-1$
	final static String							IS_XML_CACHE										= "xml_cache";																																									//$NON-NLS-1$
	final static String							MINMAX_QUANTILE_DISTANCE				= "minmax_quantile_distance";																																			//$NON-NLS-1$
	final static String							ABSOLUTE_TRANSITION_LEVEL				= "absolute_transition_level";																																		//$NON-NLS-1$
	final static String							IS_DATETIME_UTC									= "is_datetime_utc";																																							//$NON-NLS-1$
	final static String							IS_DISPLAY_SETTLEMENTS					= "is_display_settlements";																																				//$NON-NLS-1$
	final static String							IS_DISPLAY_SCORES								= "is_display_scores";																																						//$NON-NLS-1$
	final static String							IS_DISPLAY_TAGS									= "is_display_tags";																																							//$NON-NLS-1$
	final static String							IS_SUPPRESS_MODE								= "is_suppress_mode";																																							//$NON-NLS-1$
	final static String							IS_CURVE_SURVEY									= "is_curve_survey";																																							//$NON-NLS-1$
	final static String							GPS_LOCATION_RADIUS							= "gps_location_radius";																																					//$NON-NLS-1$
	final static String							GPS_ADDRESS_TYPE								= "gps_address_type";																																							//$NON-NLS-1$
	final static String							SUBDIRECTORY_LEVEL_MAX					= "subdirectory_level_max";																																				//$NON-NLS-1$
	final static String							IS_DATA_TABLE_TRANSITIONS				= "is_data_table_transitions";																																		//$NON-NLS-1$
	final static String							IS_FIRST_RECORDSET_CHOICE				= "is_first_recordset_choice";																																		//$NON-NLS-1$
	final static String							REMINDER_COUNT_CSV							= "reminder_count_csv";																																						//$NON-NLS-1$
	final static String							REMINDER_COUNT_INDEX						= "reminder_count_index";																																					//$NON-NLS-1$
	final static String							REMINDER_LEVEL									= "reminder_level";																																								//$NON-NLS-1$
	final static String							IS_CANONICAL_QUANTILES					= "is_canonical_quantiles";																																				//$NON-NLS-1$
	final static String							IS_SYMMETRIC_TOLERANCE_INTERVAL	= "is_symmetric_tolerance_interval";																															//$NON-NLS-1$
	final static String							OUTLIER_TOLERANCE_SPREAD				= "outlier_tolerance_spread";																																			//$NON-NLS-1$
	final static String							SUMMARY_SCALE_SPREAD						= "summary_scale_spread";																																					//$NON-NLS-1$
	final static String							IS_SUMMARY_BOX_VISIBLE					= "is_summary_box_visible";																																				//$NON-NLS-1$
	final static String							IS_SUMMARY_SPREAD_VISIBLE				= "is_summary_spread_visible";																																		//$NON-NLS-1$
	final static String							IS_SUMMARY_SPOTS_VISIBLE				= "is_summary_spots_visible";																																			//$NON-NLS-1$
	final static String							DATA_FOLDERS_CSV								= "data_folders_csv";																																							//$NON-NLS-1$
	final static String							IMPORT_FOLDERS_CSV							= "import_folders_csv";																																						//$NON-NLS-1$
	final static String							MIRROR_SOURCE_FOLDERS_CSV				= "mirror_source_folders_csv";																																		//$NON-NLS-1$
	final static String							IS_SOURCE_FILE_LISTENER_ACTIVE	= "is_source_file_listener_active";																																//$NON-NLS-1$
	final static String							IS_OBJECT_QUERY_ACTIVE					= "is_object_query_active";																																				//$NON-NLS-1$

	final static String							FILE_HISTORY_BLOCK							= "#[File-History-List]";																																					//$NON-NLS-1$
	final static String							FILE_HISTORY_BEGIN							= "history_file_";																																								//$NON-NLS-1$
	List<String>										fileHistory											= new ArrayList<String>();

	final static String							APPL_BLOCK											= "#[Program-Settings]";																																					//$NON-NLS-1$
	final static String							TABLE_BLOCK											= "#[Table-Settings]";																																						//$NON-NLS-1$
	final static String							LOGGING_BLOCK										= "#[Logging-Settings]";																																					//$NON-NLS-1$
	final static String							HISTO_BLOCK											= "#[Histo-Settings]";																																						//$NON-NLS-1$
	public final static String			LOG_PATH												= "Logs";																																													//$NON-NLS-1$
	final static String							LOG_FILE												= "trace.log";																																										//$NON-NLS-1$
	final static String							SERIAL_LOG_FILE									= "serial.log";																																										//$NON-NLS-1$
	public final static String[]		LOGGING_LEVEL										= new String[] { "SEVERE", "WARNING", "TIME", "INFO", "FINE", "FINER", "FINEST" };								//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

	public final static String			MAPPINGS_DIR_NAME								= "Mapping";																																											//$NON-NLS-1$
	public final static String			MEASUREMENT_DISPLAY_FILE				= "MeasurementDisplayProperties.xml";																															//$NON-NLS-1$

	public final static String			ACTIVE_DEVICE										= "active_device";																																								//$NON-NLS-1$
	public final static String			DEVICE_USE											= "device_use";																																										//$NON-NLS-1$
	public final static String			OBJECT_LIST											= "object_list";																																									//$NON-NLS-1$
	public final static String			ACTIVE_OBJECT										= "active_object";																																								//$NON-NLS-1$

	public final static String			APPL_VERSION_NUMBER							= "appl_version_number";
	public final static String			DATA_FILE_PATH									= "data_file_path";																																								//$NON-NLS-1$
	public final static String			OBJECT_IMAGE_FILE_PATH					= "object_image_file_path";																																				//$NON-NLS-1$
	public final static String			LIST_SEPARATOR									= "list_separator";																																								//$NON-NLS-1$
	public final static String			DECIMAL_SEPARATOR								= "decimal_separator";																																						//$NON-NLS-1$
	public final static String			USE_DATA_FILE_NAME_LEADER				= "use_date_file_name_leader";																																		//$NON-NLS-1$
	public final static String			USE_OBJECT_KEY_IN_FILE_NAME			= "use_object_key_in_file_name";																																	//$NON-NLS-1$
	public final static String			WRITE_TMP_FILES									= "write_tmp_files";																																							//$NON-NLS-1$
	public static final String			ALPHA_BLENDING_VALUE						= "alpha_blending_value";																																					//$NON-NLS-1$
	public static final String			APLHA_BLENDING_ENABLED					= "aplha_blending_enabled";																																				//$NON-NLS-1$
	public static final String			KEEP_IMPORT_DIR_OBJECT_RELATED	= "keep_import_dir_object_related";																																//$NON-NLS-1$
	public final static String			SKIP_BLUETOOTH_DEVICES					= "skip_bluetooth_devices";																																				//$NON-NLS-1$
	public final static String			DO_PORT_AVAILABLE_TEST					= "do_port_available_test";																																				//$NON-NLS-1$
	public final static String			IS_PORT_BLACKLIST								= "is_port_black_list";																																						//$NON-NLS-1$
	public final static String			PORT_BLACKLIST									= "port_black_list";																																							//$NON-NLS-1$
	public final static String			IS_PORT_WHITELIST								= "is_port_white_list";																																						//$NON-NLS-1$
	public final static String			PORT_WHITELIST									= "port_white_list";																																							//$NON-NLS-1$
	public final static String			DEVICE_DIALOG_USE_MODAL					= "device_dialogs_modal";																																					//$NON-NLS-1$
	public static final String			DEVICE_DIALOG_ON_TOP						= "device_dialogs_on_top";																																				//$NON-NLS-1$
	public final static String			IS_GLOBAL_LOG_LEVEL							= "is_global_log_level";																																					//$NON-NLS-1$
	public static final String			IS_REDUCE_CHARGE_DISCHARGE			= "is_reduce_charge_discharge";																																		//$NON-NLS-1$
	public final static String			IS_ALL_IN_ONE_RECORDSET					= "is_all_in_one_record_set";																																			//$NON-NLS-1$
	public final static String			IS_PARTIAL_DATA_TABLE						= "is_partial_data_table";																																				//$NON-NLS-1$
	public final static String			IS_DATA_TABLE_EDITABLE					= "is_data_table_editable";																																				//$NON-NLS-1$
	public final static String			IS_RXTX_COMM_TO_BE_USED					= "is_rxtx_comm_to_be_used";																																				//$NON-NLS-1$
	public final static String			IS_START_DEV_COMM_AFTER_START		= "is_start_device_comm_after_startup";																																				//$NON-NLS-1$
	public final static String			IS_MAC_TOUCHBAR									= "is_mac_touchbar";																																				//$NON-NLS-1$
	public final static String			GLOBAL_LOG_LEVEL								= "global_log_level";																																							//$NON-NLS-1$
	public final static String			UI_LOG_LEVEL										= "ui_log_level";																																									//$NON-NLS-1$
	public final static String			DEVICE_LOG_LEVEL								= "device_log_level";																																							//$NON-NLS-1$
	public final static String			DATA_LOG_LEVEL									= "data_log_level";																																								//$NON-NLS-1$
	public final static String			CONFIG_LOG_LEVEL								= "config_log_level";																																							//$NON-NLS-1$
	public final static String			UTILS_LOG_LEVEL									= "utils_log_level";																																							//$NON-NLS-1$
	public final static String			FILE_IO_LOG_LEVEL								= "file_IO_log_level";																																						//$NON-NLS-1$
	public final static String			SERIAL_IO_LOG_LEVEL							= "serial_IO_log_level";																																					//$NON-NLS-1$
	public final static Properties	classbasedLogger								= new Properties();

	public static final String			LOCALE_IN_USE										= "locale_in_use";																																								//$NON-NLS-1$
	public static final String			LOCALE_CHANGED									= "locale_changed";																																								//$NON-NLS-1$
	public static final String			TIME_FORMAT_IN_USE							= "time_format_in_use";																																						//$NON-NLS-1$
	public static final String			IS_DESKTOP_SHORTCUT_CREATED			= "is_desktop_shotcut_created";																																		//$NON-NLS-1$
	public static final String			IS_APPL_REGISTERED							= "is_GDE_registered";																																						//$NON-NLS-1$
	public static final String			IS_LOCK_UUCP_HINTED							= "is_lock_uucp_hinted";																																					//$NON-NLS-1$
	public static final String			LAST_UPDATE_CHECK								= "last_update_check";																																						//$NON-NLS-1$
	public final static String			IS_OBJECT_TEMPLATES_ACTIVE			= "is_object_templates_active";																																		//$NON-NLS-1$

	public final static String			GRID_DASH_STYLE									= "grid_dash_style";																																							//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_HOR_TYPE		= "grid_compare_horizontal_type";																																	//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_HOR_COLOR		= "grid_compare_horizontal_color";																																//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_VER_TYPE		= "grid_compare_vertical_type";																																		//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_VER_COLOR		= "grid_compare_vertical_color";																																	//$NON-NLS-1$

	public final static String			DEVICE_PROPERTIES_DIR_NAME			= "Devices";																																											//$NON-NLS-1$
	public final static String			DEVICE_PROPERTIES_XSD_NAME			= "DeviceProperties" + GDE.DEVICE_PROPERTIES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD;								//$NON-NLS-1$
	public final static String			GRAPHICS_TEMPLATES_DIR_NAME			= "GraphicsTemplates";																																						//$NON-NLS-1$
	public final static String			GRAPHICS_TEMPLATES_XSD_NAME			= "GraphicsTemplates" + GDE.GRAPHICS_TEMPLATES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD;							//$NON-NLS-1$
	public final static String			GRAPHICS_TEMPLATES_EXTENSION		= GDE.FILE_ENDING_STAR_XML;

	public final static String			HISTO_CACHE_ENTRIES_DIR_NAME		= "Cache";																																												//$NON-NLS-1$
	public final static String			HISTO_CACHE_ENTRIES_XSD_NAME		= "HistoVault" + GDE.HISTO_CACHE_ENTRIES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD;										//$NON-NLS-1$
	public static final String			HISTO_EXCLUSIONS_FILE_NAME			= ".gdeignore";																																										//$NON-NLS-1$
	public static final String			HISTO_EXCLUSIONS_DIR_NAME				= ".gdeignore";																																										//$NON-NLS-1$
	public static final String			GPS_LOCATIONS_DIR_NAME					= "Locations";																																										//$NON-NLS-1$
	public static final String			GPS_API_URL											= "https://maps.googleapis.com/maps/api/geocode/xml?latlng=";																			//$NON-NLS-1$
	public static final String			HISTO_INCLUSIONS_FILE_NAME			= ".gdeinclude";																																									//$NON-NLS-1$
	public static final String			HISTO_INCLUSIONS_DIR_NAME				= ".gdeinclude";																																									//$NON-NLS-1$
	public final static String			HISTO_OBJECTS_DIR_NAME					= "Fleet";																																												//$NON-NLS-1$

	private static double[]					SAMPLING_TIMESPANS							= new double[] { 10., 5., 1., .5, .1, .05, .001 };
	private static String[]					REMINDER_COUNT_VALUES						= new String[] { "2", "3", "5", "8" };

	private final TreeMap<String, ExportService>				deviceServices;
	private final DeviceSerialization	deviceSerialization;
	private final Thread							xsdThread;

	private Thread										migrationThread;

	boolean													isDevicePropertiesUpdated				= false;
	// boolean isDevicePropertiesReplaced = false;
	boolean													isGraphicsTemplateUpdated				= false;
	boolean													isHistocacheTemplateUpdated			= false;

	Rectangle												window;
	boolean													isWindowMaximized								= false;
	String													cbOrder;
	private String									cbWraps;
	String													cbSizes;
	Comparator<String>							comparator											= new RecordSetNameComparator();																																	//used to sort object key list
	Properties											measurementProperties						= new Properties();


	public enum GeoCodeGoogle {
		STREET_ADDRESS, ROUTE, POLITICAL, ADMINISTRATIVE_AREA_LEVEL_3, ADMINISTRATIVE_AREA_LEVEL_2;
		/**
		 * use this instead of values() to avoid repeatedly cloning actions.
		 */
		public static final GeoCodeGoogle VALUES[] = values();
	};

	/**
	 * a singleton needs a static method to get the instance of this class.
	 * Is synchronized because during startup additional requests for an instance might occur during Settings initialization.
	 * The result would be multiple settings instances and data loss if any instance fields are modified.
	 * @return the instance
	 */
	public static Settings getInstance() {
		if (Settings.instance == null) {
			Settings.instance = new Settings();
			// synchronize now to avoid a performance penalty in case of frequent getInstance calls
			synchronized (Settings.instance) {
				try {
					Settings.instance.initialize();
					Settings.log.log(Level.TIME, "init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e) {
					Settings.log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return Settings.instance;
	}

	/**
	 * singleton private constructor
	 */
	private Settings(){
		if (GDE.APPL_HOME_PATH.isEmpty()) {
			Settings.log.log(Level.SEVERE, Messages.getString(MessageIds.GDE_MSGW0001));
		}
		this.deviceServices = new TreeMap<String, ExportService>(String.CASE_INSENSITIVE_ORDER);
		this.deviceSerialization = new DeviceSerialization();
		this.xsdThread = deviceSerialization.createXsdThread();
	}

	/**
	 * Singleton initialization loads the settings and the measurement display properties.
	 * For the standard environment it updates or migrates the device XMLs.
	 * @throws SAXException
	 * @throws JAXBException
	 */
	private void initialize() throws SAXException, JAXBException {
		this.load();

		if (Analyzer.isWithBuilders()) {
			this.readMeasurementDiplayProperties();
		} else {
			final String lang = this.getLocale().getLanguage().contains("de") || this.getLocale().getLanguage().contains("en") ? this.getLocale().getLanguage() : "en";
			// check existence of application home directory, check XSD version, copy all device XML+XSD and image files
			FileUtils.checkDirectoryAndCreate(GDE.APPL_HOME_PATH);
			String devicePropertiesTargetpath = Settings.getDevicesPath();
			if (!FileUtils.checkDirectoryAndCreate(devicePropertiesTargetpath, Settings.DEVICE_PROPERTIES_XSD_NAME)) {
				FileUtils.extract(this.getClass(), Settings.DEVICE_PROPERTIES_XSD_NAME, Settings.PATH_RESOURCE, devicePropertiesTargetpath, Settings.PERMISSION_555);
				checkDeviceProperties(String.format("%s%s", devicePropertiesTargetpath, GDE.STRING_FILE_SEPARATOR_UNIX), true);
				this.isDevicePropertiesUpdated = true;
				checkMeasurementDisplayMappings(true, lang);
			}
			else { // execute every time application starts to enable xsd exist and update from added plug-in
				if (!FileUtils.checkFileExist(String.format("%s%s", devicePropertiesTargetpath, Settings.DEVICE_PROPERTIES_XSD_NAME)))
					FileUtils.extract(this.getClass(), Settings.DEVICE_PROPERTIES_XSD_NAME, Settings.PATH_RESOURCE, devicePropertiesTargetpath, Settings.PERMISSION_555);
				checkDeviceProperties(String.format("%s%s", devicePropertiesTargetpath, GDE.STRING_FILE_SEPARATOR_UNIX), true);
				checkMeasurementDisplayMappings(true, lang);
			}
			
			// locale settings has been changed, replacement of device property files required
			if (this.getLocaleChanged()) {
				checkMeasurementDisplayMappings(false, lang);
			}
			//measurement display properties should be loaded after possible replacement due to language change
			this.readMeasurementDiplayProperties();

			if (this.isDevicePropertiesUpdated) { // check if previous devices exist and migrate device usage, default import directory, ....
				this.migrationThread = DeviceSerialization.createMigrationThread();
			}

			String templateDirectory = Settings.getGraphicsTemplatePath();
			if (!FileUtils.checkDirectoryAndCreate(templateDirectory, Settings.GRAPHICS_TEMPLATES_XSD_NAME)) {
				FileUtils.extract(this.getClass(), Settings.GRAPHICS_TEMPLATES_XSD_NAME, Settings.PATH_RESOURCE, templateDirectory, Settings.PERMISSION_555);
				this.isGraphicsTemplateUpdated = true;
			}

			checkDeviceTemplates( String.format("%s%s", templateDirectory, GDE.STRING_FILE_SEPARATOR_UNIX));

			String histoCacheDirectory =  String.format("%s%s%s", GDE.APPL_HOME_PATH, GDE.STRING_FILE_SEPARATOR_UNIX, Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
			if (!FileUtils.checkDirectoryAndCreate(histoCacheDirectory, Settings.HISTO_CACHE_ENTRIES_XSD_NAME)) {
				FileUtils.extract(Settings.class, Settings.HISTO_CACHE_ENTRIES_XSD_NAME, Settings.PATH_RESOURCE, histoCacheDirectory, Settings.PERMISSION_555);
				this.isHistocacheTemplateUpdated = true;
			}

			FileUtils.checkDirectoryAndCreate(String.format("%s%s%s", GDE.APPL_HOME_PATH, GDE.STRING_FILE_SEPARATOR_UNIX, Settings.LOG_PATH));

			this.isWindowMaximized = Boolean.parseBoolean(this.getProperty(Settings.WINDOW_MAXIMIZED, "false"));

			if (this.getProperty(Settings.WINDOW_LEFT) != null && this.getProperty(Settings.WINDOW_TOP) != null && this.getProperty(Settings.WINDOW_WIDTH) != null
					&& this.getProperty(Settings.WINDOW_HEIGHT) != null) {
				this.window = new Rectangle(Integer.valueOf(this.getProperty(Settings.WINDOW_LEFT).trim()).intValue(), Integer.parseInt(this.getProperty(Settings.WINDOW_TOP).trim()),
						Integer.parseInt(this.getProperty(Settings.WINDOW_WIDTH).trim()), Integer.parseInt(this.getProperty(Settings.WINDOW_HEIGHT).trim()));
			} else {
				this.window = new Rectangle(50, 50, 950, 600);
			}
		}
		this.setProperty(Settings.LOCALE_CHANGED, "false"); //$NON-NLS-1$

		this.xsdThread.start(); // wait to start the thread until the XSD checked if exist or updated
	}

	/**
	 * this map is used to enable device capability overview and selection of active devices in device selection dialog
	 * @return map of exported services by device plug-in jars
	 */
	public TreeMap<String, ExportService> getDeviceServices() {
		return this.deviceServices;
	}

	/**
	 * @param deviceName
	 * @return true if deviceName is in the list of active devices
	 */
	public boolean isOneOfActiveDevices(String deviceName) {
		return Arrays.stream(this.getDeviceUseCsv().split(GDE.STRING_CSV_SEPARATOR)).filter(s -> s.startsWith(deviceName + GDE.STRING_STAR)).count() > 0;
	}

	/**
	 * check existence of directory, create if required and update all
	 * @param devicePropertiesTargetpath
	 */
	private void checkDeviceProperties(String devicePropertiesTargetpath, boolean replaceDeviceXmlFiles) {
		final String $METHOD_NAME = "checkDeviceProperties"; //$NON-NLS-1$

		if (replaceDeviceXmlFiles) {
			String deviceJarBasePath = FileUtils.getDevicePluginJarBasePath();
			log.logp(java.util.logging.Level.CONFIG, Settings.$CLASS_NAME, $METHOD_NAME, String.format("deviceJarBasePath = %s", deviceJarBasePath)); //$NON-NLS-1$
			String[] files = new File(deviceJarBasePath).list();
			if (files == null) {
				log.log(Level.SEVERE, String.format("check if %s is an existing directory", deviceJarBasePath));
				throw new NullPointerException(String.format("directory %s does not exist", deviceJarBasePath));
			}
			for (String jarFileName : files) {
				if (!jarFileName.endsWith(GDE.FILE_ENDING_DOT_JAR))
					continue;
				JarFile jarFile = null;
				List<ExportService> services = new ArrayList<>();
				try {
					jarFile = new JarFile(String.format("%s%s%s", deviceJarBasePath, GDE.STRING_FILE_SEPARATOR_UNIX, jarFileName));
					services = FileUtils.getDeviceJarServices(jarFile);
					for (ExportService service : services) {
						final String serviceName = service.getName();
						if (this.getDeviceUseCsv().isEmpty() || this.isOneOfActiveDevices(serviceName)) {
							extractDeviceProperties(jarFile, serviceName, devicePropertiesTargetpath);
						}
						else {
							File obsoleteFile = new File(String.format("%s%s%s", devicePropertiesTargetpath, serviceName, GDE.FILE_ENDING_DOT_XML));
							if (obsoleteFile.exists())
								if (!obsoleteFile.delete())
									log.log(Level.WARNING, String.format("could not delete %s%s%s", devicePropertiesTargetpath, serviceName, GDE.FILE_ENDING_DOT_XML));
						}
					}
				}
				catch (Throwable e) {
					Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				}
				//add the services found in jar to the collection of device services
				for (ExportService service : services) {
					this.deviceServices.put(service.getName(), service);
				}
			}
		}
	}

	/**
	 * check for individual device XML list is available and need update
	 * @param devicePropertiesTargetpath
	 */
	public void checkUpdateDeviceProperties(String devicePropertiesTargetpath) {
		final String $METHOD_NAME = "checkUpdateDeviceProperties"; //$NON-NLS-1$
		
		log.log(Level.INFO, String.format("isDeviceXmlUpdateRequired %b", isDeviceXmlUpdateRequired()));
		if (isDeviceXmlUpdateRequired()) {
			List<String> serviceUpdateList = getServiceUpdateList();
			List<String> usedDeviceList = Arrays.asList(getDeviceUseCsv().split(GDE.STRING_CSV_SEPARATOR));
			List<String> usedServiceList = new ArrayList<>();
			for (String deviceName : usedDeviceList)
				if (!deviceName.isEmpty())
					usedServiceList.add(deviceName.substring(0, deviceName.indexOf(GDE.CHAR_STAR)));
			String deviceJarBasePath = FileUtils.getDevicePluginJarBasePath();
			log.logp(java.util.logging.Level.CONFIG, Settings.$CLASS_NAME, $METHOD_NAME, String.format("deviceJarBasePath = %s", deviceJarBasePath)); //$NON-NLS-1$
			String[] files = new File(deviceJarBasePath).list();
			if (files == null) {
				log.log(Level.SEVERE, String.format("check if %s is an existing directory", deviceJarBasePath));
				throw new RuntimeException(String.format("directory %s does not exist", deviceJarBasePath));
			}
			for (String jarFileName : files) {
				if (!jarFileName.endsWith(GDE.FILE_ENDING_DOT_JAR))
					continue;
				JarFile jarFile = null;
				List<ExportService> services = new ArrayList<>();
				try {
					jarFile = new JarFile(String.format("%s%s%s", deviceJarBasePath, GDE.STRING_FILE_SEPARATOR_UNIX, jarFileName));
					services = FileUtils.getDeviceJarServices(jarFile);
					for (ExportService service : services) {
						final String serviceName = service.getName();
						if (serviceUpdateList.contains(serviceName) && usedServiceList.contains(serviceName)) {
							DeviceConfigurations devConfigs = Analyzer.getInstance().getDeviceConfigurations();
							String deviceXML = devicePropertiesTargetpath + GDE.STRING_FILE_SEPARATOR_UNIX + serviceName + GDE.FILE_ENDING_DOT_XML;							
							if (devConfigs != null) {
								DeviceConfiguration oldDevConfig = devConfigs.get(serviceName);
								extractDeviceProperties(jarFile, serviceName, devicePropertiesTargetpath);
								DeviceConfiguration newDevConfig = new DeviceConfiguration(deviceXML);
								if (newDevConfig != null) {
									newDevConfig.setUsed(true);
									if (oldDevConfig.getPort().length() > 1 && !oldDevConfig.getPort().startsWith("USB")) newDevConfig.setPort(oldDevConfig.getPort());
									if (oldDevConfig.getDataBlockPreferredDataLocation().length() > 1) newDevConfig.setDataBlockPreferredDataLocation(oldDevConfig.getDataBlockPreferredDataLocation());
									newDevConfig.storeDeviceProperties();
									devConfigs.remove(serviceName);
									devConfigs.put(serviceName, newDevConfig);
									log.logp(java.util.logging.Level.OFF, Settings.$CLASS_NAME, $METHOD_NAME, String.format("device XML updated => %s.xml", serviceName)); //$NON-NLS-1$
								} 
							}
						}
					}
				}
				catch (Throwable e) {
					Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				}
				//add the services found in jar to the collection of device services
				for (ExportService service : services) {
					this.deviceServices.put(service.getName(), service);
				}
			}
		}
	}

	/**
	 * extract device properties xml if not exist extract from jar
	 * @param jarFile
	 * @param serviceName
	 * @param devicePropertiesTargetpath
	 */
	public void extractDeviceProperties(JarFile jarFile, final String serviceName, String devicePropertiesTargetpath) {
		if (!FileUtils.checkFileExist(String.format("%s%s%s", devicePropertiesTargetpath, serviceName, GDE.FILE_ENDING_DOT_XML))) {
			FileUtils.extract(jarFile, String.format("%s%s", serviceName, GDE.FILE_ENDING_DOT_XML), Settings.PATH_RESOURCE, devicePropertiesTargetpath, Settings.PERMISSION_555);
		}
	}

	/**
	 * update display mapping properties file
	 * @param existCheck false will force file replacement
	 * @param lang
	 */
	private void checkMeasurementDisplayMappings(boolean existCheck, final String lang) {
		File path = new File(String.format("%s%s%s",GDE.APPL_HOME_PATH, GDE.STRING_FILE_SEPARATOR_UNIX, Settings.MAPPINGS_DIR_NAME));
		String propertyFilePath = String.format("%s%s%s", path.toString(), GDE.STRING_FILE_SEPARATOR_UNIX, Settings.MEASUREMENT_DISPLAY_FILE);
		if (existCheck) {
			if (!FileUtils.checkFileExist(propertyFilePath))
				FileUtils.extract(this.getClass(), Settings.MEASUREMENT_DISPLAY_FILE, String.format("%s%s%s", Settings.PATH_RESOURCE, lang, GDE.STRING_FILE_SEPARATOR_UNIX), path.getAbsolutePath(), Settings.PERMISSION_555);
		}
		else {
			if (FileUtils.checkFileExist(propertyFilePath)) {
				new File(propertyFilePath).delete();
				try {
					Thread.sleep(5);
				}
				catch (InterruptedException e) {
					// ignore
				}
			}
			FileUtils.extract(this.getClass(), Settings.MEASUREMENT_DISPLAY_FILE, String.format("%s%s%s", Settings.PATH_RESOURCE, lang, GDE.STRING_FILE_SEPARATOR_UNIX), path.getAbsolutePath(), Settings.PERMISSION_555);
		}
	}

	/**
	 * Hybrid singleton copy constructor for a deep clone instance.
	 * Support multiple threads with different Settings instances.
	 * Use this if settings updates are not required or apply to the current thread only.
	 * Be aware of the cloning performance impact.
	 */
	public Settings(Settings that)  {
		for (Map.Entry<Object, Object> entry : that.entrySet()) {
			this.put(entry.getKey(), entry.getValue()); // Strings only - do not require cloning
		}

		// final fields
		this.xsdThread = that.xsdThread;
		this.deviceSerialization = that.deviceSerialization;
		this.deviceServices = that.deviceServices;

		// non-UI instance fields
		this.migrationThread = that.migrationThread;

		this.isDevicePropertiesUpdated = that.isDevicePropertiesUpdated;
		this.isGraphicsTemplateUpdated = that.isGraphicsTemplateUpdated;
		this.isHistocacheTemplateUpdated = that.isHistocacheTemplateUpdated;

		// UI related instance fields
		this.fileHistory = new ArrayList<>(that.fileHistory);

		this.window = that.window;
		this.isWindowMaximized = that.isWindowMaximized;
		this.cbOrder = that.cbOrder;
		this.cbWraps = that.cbWraps;
		this.cbSizes = that.cbSizes;
		this.comparator = that.comparator;
		this.measurementProperties = (Properties) that.measurementProperties.clone();
}

	/**
	 * check existence of device graphics default templates, extract if required
	 * if called after checkDeviceProperties and deviceServices contains all services exported
	 * additional access to device jar files not required
	 * @param templateDirectoryTargetPath
	 */
	private void checkDeviceTemplates(String templateDirectoryTargetPath) {
	//deviceServices contains all services exported by found device jar files
		if (this.getDeviceServices().size() == 0) {
			String deviceJarBasePath = FileUtils.getDevicePluginJarBasePath();
			log.config(() -> String.format("deviceJarBasePath = %s", deviceJarBasePath)); //$NON-NLS-1$
			for (String jarFileName : new File(deviceJarBasePath).list()) {
				if (!jarFileName.endsWith(GDE.FILE_ENDING_DOT_JAR)) continue;
				try {
					for (ExportService service : FileUtils.getDeviceJarServices(new JarFile(deviceJarBasePath + GDE.STRING_FILE_SEPARATOR_UNIX + jarFileName))) {
						final String serviceName = service.getName();
						if (this.isOneOfActiveDevices(serviceName)) {
							extractDeviceDefaultTemplates(service.getJarFile(), serviceName, templateDirectoryTargetPath);
						}
					}
				} catch (IOException e) {
					Settings.log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		} else {
			checkDeviceTemplates(templateDirectoryTargetPath, this.getDeviceServices().values());
		}
	}

	/**
	 * check for changes and extract or delete templates
	 * @param templateDirectoryTargetPath
	 * @param services
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void checkDeviceTemplates(String templateDirectoryTargetPath, Collection<ExportService> services) {
		Map<String, List<Path>> templateFileMap = readTemplateFiles(templateDirectoryTargetPath);
		for (ExportService service : services) {
			try {
				final String serviceName = service.getName();
				if (this.getDeviceUseCsv().isEmpty() || this.isOneOfActiveDevices(serviceName)) {
					extractDeviceDefaultTemplates(service.getJarFile(), serviceName, templateDirectoryTargetPath);
				} else if (templateFileMap.containsKey(serviceName)) {
					for (Path path : templateFileMap.get(serviceName)) {
						log.fine(() -> "delete " + path); //$NON-NLS-1$
						if (!path.toFile().delete())
							log.warning(() -> String.format("could not delete %s%s%s", templateDirectoryTargetPath, serviceName, GDE.FILE_ENDING_DOT_XML));
					}
				}
			} catch (Exception e) {
				Settings.log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	private Map<String, List<Path>> readTemplateFiles(String templateDirectoryTargetPath) {
		Map<String, List<Path>> filesMap = new HashMap<>();
		try (Stream<Path> files = Files.walk(Paths.get(templateDirectoryTargetPath), 2)) {
			Stream<Path> potentialFiles = files.filter(t -> t.getFileName().toString().contains(GDE.STRING_DOT));
			filesMap = potentialFiles.collect(Collectors.groupingBy(this::getTemplateFilePrefix));
		} catch (IOException e) {}
		return filesMap;
	}

	private String getTemplateFilePrefix(Path t) {
		String fileName = t.getFileName().toString();
		int index = fileName.lastIndexOf(GDE.CHAR_UNDER_BAR);
		return index < 0 ? "" : fileName.substring(0, index);
	}

	/**
	 * Extract device default templates if not exist at target path
	 * @param jarFile
	 * @param templateDirectoryTargetPath
	 * @param serviceName_
	 */
	public void extractDeviceDefaultTemplates(JarFile jarFile, final String serviceName, String templateDirectoryTargetPath) {
		String serviceName_ = String.format("%s%s", serviceName, GDE.STRING_UNDER_BAR);
		Enumeration<JarEntry> e = jarFile.entries();
		while (e.hasMoreElements()) {
			String entryName = e.nextElement().getName();
			if (entryName.startsWith(Settings.PATH_RESOURCE_TEMPLATE) && entryName.endsWith(GDE.FILE_ENDING_DOT_XML) && entryName.contains(serviceName_)) {
				String defaultTemplateName = entryName.substring(Settings.PATH_RESOURCE_TEMPLATE.length());
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("jarFile = %s ; defaultTemplateName = %s", jarFile.getName(), entryName)); //$NON-NLS-1$
				if (!FileUtils.checkFileExist(templateDirectoryTargetPath + defaultTemplateName)) {
					FileUtils.extract(jarFile, defaultTemplateName, Settings.PATH_RESOURCE_TEMPLATE, templateDirectoryTargetPath, Settings.PERMISSION_555);
				}
			}
		}
	}

	/**
	 * extract device properties XML and graphics templates
	 * @param jarFile where service is exported
	 * @param serviceName
	 */
	public void extractDevicePropertiesAndTemplates(JarFile jarFile, final String serviceName) {
		extractDeviceProperties(jarFile, serviceName, String.format("%s%s", Settings.getDevicesPath(), GDE.STRING_FILE_SEPARATOR_UNIX));
		extractDeviceDefaultTemplates(jarFile, serviceName, String.format("%s%s", Settings.getGraphicsTemplatePath(), GDE.STRING_FILE_SEPARATOR_UNIX));
	}

	/**
	 * read special properties to enable configuration to specific GPX extent values
	 */
	private void readMeasurementDiplayProperties() {
		final String $METHOD_NAME = "readMeasurementDiplayProperties"; //$NON-NLS-1$
		DataAccess.getInstance().checkMappingFileAndCreate(this.getClass(), Settings.MEASUREMENT_DISPLAY_FILE);
		try (InputStream stream = DataAccess.getInstance().getMappingInputStream(Settings.MEASUREMENT_DISPLAY_FILE)) {
			this.measurementProperties.loadFromXML(stream);
		} catch (Exception e) {
			Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage());
		}
	}

	/**
	 * @return the measurement display properties set
	 */
	public Properties getMeasurementDisplayProperties() {
		return this.measurementProperties;
	}

	/**
	 * read the application settings file
	 */
	void load() {
		final String $METHOD_NAME = "load"; //$NON-NLS-1$
		try (Reader reader = DataAccess.getInstance().getSettingsReader()) {
			this.load(reader);

			// update file history
			for (int i = 0; i < 9; i++) {
				String entry = this.getProperty(Settings.FILE_HISTORY_BEGIN + i);
				if (entry != null && entry.length() > 4) {
					if (!this.fileHistory.contains(entry)) this.fileHistory.add(entry);
				}
				else
					break;
			}
		}
		catch (Exception e) {
			Settings.log.logp(java.util.logging.Level.WARNING, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage());
		}

	}

	/**
	 * write the application settings file
	 */
	public void store() {
		final String $METHOS_NAME = "store()"; //$NON-NLS-1$
		try (Writer writer = DataAccess.getInstance().getSettingsWriter()) {
			writer.write(String.format("%s\n", Settings.HEADER_TEXT)); //$NON-NLS-1$

			writer.write(String.format("%s\n", Settings.DEVICE_BLOCK)); // [Gerät] //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.ACTIVE_DEVICE, this.getActiveDevice())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DEVICE_USE, this.getDeviceUseCsv())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_LIST, this.getObjectListAsString())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.ACTIVE_OBJECT, this.getActiveObject())); //$NON-NLS-1$

			writer.write(String.format("%s\n", Settings.WINDOW_BLOCK)); // [Fenster Einstellungen] //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_MAXIMIZED, this.isWindowMaximized)); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_LEFT, this.window.x)); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_TOP, this.window.y)); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_WIDTH, this.window.width)); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_HEIGHT, this.window.height)); //$NON-NLS-1$

			writer.write(String.format("%-40s \t=\t %s\n", Settings.COOLBAR_ORDER, this.cbOrder)); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.COOLBAR_WRAPS, this.cbWraps)); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.COOLBAR_SIZES, this.cbSizes)); //$NON-NLS-1$

			writer.write(String.format("%-40s \t=\t %s\n", Settings.RECORD_COMMENT_VISIBLE, isRecordCommentVisible())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_HEADER_VISIBLE, isGraphicsHeaderVisible())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DISPLAY_DENSITY_FONT_CORRECT, getFontDisplayDensityAdaptionFactor())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.KMZ_EXPORT_TIME_STEP_MS, getKmzExportTimeStep_ms())); //$NON-NLS-1$

			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_DASH_STYLE, getGridDashStyleAsString())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_HOR_TYPE, getGridCompareWindowHorizontalType())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_HOR_COLOR, getGridCompareWindowHorizontalColorStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_VER_TYPE, getGridCompareWindowVerticalType())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_VER_COLOR, getGridCompareWindowVerticalColorStr())); //$NON-NLS-1$

			writer.write(String.format("%-40s \t=\t %s\n", Settings.SKIN_COLOR_SCHEMA, this.getSkinColorSchema())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_AREA_BACKGROUND, getGraphicsCurveAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_SURROUND_BACKGRD, getGraphicsSurroundingBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_BORDER_COLOR, getGraphicsCurvesBorderColorStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GRAPHICS_SCALE_COLOR, isDrawScaleInRecordColor())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GRAPHICS_NAME_COLOR, isDrawNameInRecordColor())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GRAPHICS_TICKS_10, isDraw10TicksPerRecord())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GRAPHICS_NUMBERS_COLOR, isDrawNumbersInRecordColor())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.COMPARE_AREA_BACKGROUND, getCompareCurveAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.COMPARE_SURROUND_BACKGRD, getCompareSurroundingBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.COMPARE_BORDER_COLOR, getCurveCompareBorderColorStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_COMPARE_CHANNELCONFIG, isCurveCompareChannelConfigName())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILITY_AREA_BACKGROUND, getUtilityCurveAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILITY_SURROUND_BACKGRD, getUtilitySurroundingBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILITY_BORDER_COLOR, getUtilityCurvesBorderColorStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.STATISTICS_INNER_BACKGROUND, getStatisticsInnerAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.STATISTICS_SURROUND_BACKGRD, getStatisticsSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.ANALOG_INNER_BACKGROUND, getAnalogInnerAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.ANALOG_SURROUND_BACKGRD, getAnalogSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DIGITAL_INNER_BACKGROUND, getDigitalInnerAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DIGITAL_SURROUND_BACKGRD, getDigitalSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.CELL_VOLTAGE_INNER_BACKGROUND, getCellVoltageInnerAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.CELL_VOLTAGE_SURROUND_BACKGRD, getCellVoltageSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_COMMENT_INNER_BACKGROUND, getFileCommentInnerAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_COMMENT_SURROUND_BACKGRD, getFileCommentSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_DESC_INNER_BACKGROUND, getObjectDescriptionInnerAreaBackgroundStr())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_DESC_SURROUND_BACKGRD, getObjectDescriptionSurroundingAreaBackgroundStr())); //$NON-NLS-1$

			writer.write(String.format("%s\n", Settings.FILE_HISTORY_BLOCK)); // [Datei History Liste] //$NON-NLS-1$
			for (int i = 0; i < 9 && i < this.fileHistory.size(); i++) {
				if (this.fileHistory.get(i) == null) break;
				writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_HISTORY_BEGIN + i, this.fileHistory.get(i))); //$NON-NLS-1$
			}

			writer.write(String.format("%s\n", Settings.APPL_BLOCK)); // [Programmeinstellungen] //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.APPL_VERSION_NUMBER, getApplVersionNumber())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DATA_FILE_PATH, getDataFilePath())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_IMAGE_FILE_PATH, getObjectImageFilePath())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.USE_DATA_FILE_NAME_LEADER, getUsageDateAsFileNameLeader())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.USE_OBJECT_KEY_IN_FILE_NAME, getUsageObjectKeyInFileName())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.WRITE_TMP_FILES, getUsageWritingTmpFiles())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.ALPHA_BLENDING_VALUE, getDialogAlphaValue())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.APLHA_BLENDING_ENABLED, isDeviceDialogAlphaEnabled())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.KEEP_IMPORT_DIR_OBJECT_RELATED, isDeviceImportDirectoryObjectRelated())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.SKIP_BLUETOOTH_DEVICES, isSkipBluetoothDevices())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DO_PORT_AVAILABLE_TEST, doPortAvailabilityCheck())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_PORT_BLACKLIST, isSerialPortBlackListEnabled())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.PORT_BLACKLIST, getSerialPortBlackList())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_PORT_WHITELIST, isSerialPortWhiteListEnabled())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.PORT_WHITELIST, getSerialPortWhiteListString())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DEVICE_DIALOG_USE_MODAL, isDeviceDialogsModal())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DEVICE_DIALOG_ON_TOP, isDeviceDialogsOnTop())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.LOCALE_IN_USE, getLocale().getLanguage())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.LOCALE_CHANGED, getLocaleChanged())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.TIME_FORMAT_IN_USE, getTimeFormat())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DESKTOP_SHORTCUT_CREATED, this.isDesktopShortcutCreated())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_APPL_REGISTERED, this.isApplicationRegistered())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_LOCK_UUCP_HINTED, this.isLockUucpHinted())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.LAST_UPDATE_CHECK, StringHelper.getDate())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_OBJECT_TEMPLATES_ACTIVE, isObjectTemplatesActive())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_MAC_TOUCHBAR, isMacTouchbarEnabled())); //$NON-NLS-1$
			// charger specials
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_REDUCE_CHARGE_DISCHARGE, this.isReduceChargeDischarge())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_ALL_IN_ONE_RECORDSET, this.isContinuousRecordSet())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_PARTIAL_DATA_TABLE, this.isPartialDataTable())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_START_DEV_COMM_AFTER_START, this.isStartDeviceCommunicationAfterStartup())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_RXTX_COMM_TO_BE_USED, this.isRXTXcommToBeUsed())); //$NON-NLS-1$

			writer.write(String.format("%s\n", Settings.TABLE_BLOCK)); // [Tabellen Einstellungen] //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.LIST_SEPARATOR, getListSeparator())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DECIMAL_SEPARATOR, getDecimalSeparator())); //$NON-NLS-1$

			writer.write(String.format("%s\n", Settings.LOGGING_BLOCK)); // [Logging Einstellungen] //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GLOBAL_LOG_LEVEL, isGlobalLogLevel())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GLOBAL_LOG_LEVEL, getLogLevel(Settings.GLOBAL_LOG_LEVEL))); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.UI_LOG_LEVEL, getLogLevel(Settings.UI_LOG_LEVEL))); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DEVICE_LOG_LEVEL, getLogLevel(Settings.DEVICE_LOG_LEVEL))); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DATA_LOG_LEVEL, getLogLevel(Settings.DATA_LOG_LEVEL))); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.CONFIG_LOG_LEVEL, getLogLevel(Settings.CONFIG_LOG_LEVEL))); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILS_LOG_LEVEL, getLogLevel(Settings.UTILS_LOG_LEVEL))); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_IO_LOG_LEVEL, getLogLevel(Settings.FILE_IO_LOG_LEVEL))); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.SERIAL_IO_LOG_LEVEL, getLogLevel(Settings.SERIAL_IO_LOG_LEVEL))); //$NON-NLS-1$

			writer.write(String.format("%s\n", Settings.HISTO_BLOCK)); // [Histo Settings] //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_HISTO_ACTIVE, isHistoActive())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.BOXPLOT_SCALE_ORDINAL, getBoxplotScaleOrdinal())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.BOXPLOT_SIZE_ADAPTATION_ORDINAL, getBoxplotSizeAdaptationOrdinal())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.X_SPREAD_GRADE_ORDINAL, getXAxisSpreadOrdinal())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_X_LOGARITHMIC_DISTANCE, isXAxisLogarithmicDistance())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_X_REVERSED, isXAxisReversed())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.RETROSPECT_MONTHS, getRetrospectMonths())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.SEARCH_DATAPATH_IMPORTS, getSearchDataPathImports())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_CHANNEL_MIX, isChannelMix())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.SAMPLING_TIMESPAN_ORDINAL, getSamplingTimespanOrdinal())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IGNORE_LOG_OBJECT_KEY, getIgnoreLogObjectKey())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_ZIPPED_CACHE, isZippedCache())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_XML_CACHE, isXmlCache())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.MINMAX_QUANTILE_DISTANCE, getMinmaxQuantileDistance())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.ABSOLUTE_TRANSITION_LEVEL, getAbsoluteTransitionLevel())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DATETIME_UTC, isDateTimeUtc())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DISPLAY_SETTLEMENTS, isDisplaySettlements())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DISPLAY_SCORES, isDisplayScores())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DISPLAY_TAGS, isDisplayTags())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SUPPRESS_MODE, isSuppressMode())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_CURVE_SURVEY, isCurveSurvey())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.GPS_LOCATION_RADIUS, getGpsLocationRadius())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.SUBDIRECTORY_LEVEL_MAX, getSubDirectoryLevelMax())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DATA_TABLE_TRANSITIONS, isDataTableTransitions())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_FIRST_RECORDSET_CHOICE, isFirstRecordSetChoice())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.REMINDER_COUNT_CSV, getReminderCountCsv())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.REMINDER_COUNT_INDEX, getReminderCountIndex())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.REMINDER_LEVEL, getReminderLevel())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_CANONICAL_QUANTILES, isCanonicalQuantiles())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SYMMETRIC_TOLERANCE_INTERVAL, isSymmetricToleranceInterval())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.OUTLIER_TOLERANCE_SPREAD, getOutlierToleranceSpread())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.SUMMARY_SCALE_SPREAD, getSummaryScaleSpread())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SUMMARY_BOX_VISIBLE, isSummaryBoxVisible())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SUMMARY_SPREAD_VISIBLE, isSummarySpreadVisible())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SUMMARY_SPOTS_VISIBLE, isSummarySpotsVisible())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.DATA_FOLDERS_CSV, getDataFoldersCsv())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IMPORT_FOLDERS_CSV, getImportFoldersCsv())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.MIRROR_SOURCE_FOLDERS_CSV, getMirrorSourceFoldersCsv())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SOURCE_FILE_LISTENER_ACTIVE, isSourceFileListenerActive())); //$NON-NLS-1$
			writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_OBJECT_QUERY_ACTIVE, isObjectQueryActive())); //$NON-NLS-1$

			writer.flush();
			writer.close();
		}
		catch (IOException e) {
			Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOS_NAME, e.getMessage(), e);
		}

	}

	/*
	 * overload Properties method due to loading properties from file returns "null" instead of null
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	@Override
	public String getProperty(String key, String defaultValue) {
		String val = getProperty(key);
		if (val == null || val.equals(GDE.STRING_EMPTY) || val.equals("null")) val = defaultValue;
		return val;
	}

	public Rectangle getWindow() {
		return this.window;
	}

	public void setWindow(Point location, Point size) {
		this.window = new Rectangle(location.x, location.y, size.x, size.y);
	}

	public boolean isWindowMaximized() {
		return this.isWindowMaximized;
	}

	public void setWindowMaximized(boolean isMaximized) {
		this.isWindowMaximized = isMaximized;
	}

	public void setCoolBarStates(int[] order, int[] wraps, Point[] sizes) {
		this.cbOrder = StringHelper.intArrayToString(order);
		this.cbWraps = StringHelper.intArrayToString(wraps);
		this.cbSizes = StringHelper.pointArrayToString(sizes);
	}

	public int[] getCoolBarOrder(String defaultCoolBarSizes) {
		int[] intOrder = StringHelper.stringToIntArray(this.getProperty(Settings.COOLBAR_ORDER, "0;1;2;3;4").trim()); //$NON-NLS-1$
		int coolBarSize = this.getCoolBarSizes(defaultCoolBarSizes).length;
		if (intOrder.length != coolBarSize) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < coolBarSize; i++) {
				sb.append(i).append(";");
			}
			intOrder = StringHelper.stringToIntArray(sb.toString());
		}
		return intOrder;
	}

	public int[] getCoolBarWraps() {
		return StringHelper.stringToIntArray(this.getProperty(Settings.COOLBAR_WRAPS, "0;3").trim()); //$NON-NLS-1$
	}

	public Point[] getCoolBarSizes(String defaultCoolBarSizes) {
		return StringHelper.stringToPointArray(this.getProperty(Settings.COOLBAR_SIZES, defaultCoolBarSizes).trim());
	}

	public List<String> getFileHistory() {
		return this.fileHistory;
	}

	public String getActiveDevice() {
		return this.getProperty(Settings.ACTIVE_DEVICE, Settings.EMPTY_SIGNATURE).split(GDE.STRING_SEMICOLON)[0].trim();
	}

	public void setActiveDevice(String activeDeviceString) {
		this.setProperty(Settings.ACTIVE_DEVICE, activeDeviceString.trim());
	}

	/**
	 * @param objectKeyCandidate is supposed to be a valid object key
	 * @return empty or the validated object key in the correct case sensitive format
	 */
	public Optional<String> getValidatedObjectKey(String objectKeyCandidate) {
		String key = objectKeyCandidate.trim();
		return Arrays.stream(getObjectList()).filter(s -> s.equalsIgnoreCase(key)).findFirst();
	}

	/**
	 * @return the object keys without the deviceoriented entry
	 */
	public Stream<String> getRealObjectKeys() {
		Stream<String> objectKeys = Arrays.stream(this.getProperty(Settings.OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200)).split(GDE.STRING_SEMICOLON));
		String deviceOriented = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		// the first element is always the device oriented entry; remove also double entries which may appear after language change
		return objectKeys.distinct().skip(1).filter(s -> !s.equals(deviceOriented));
	}

	public String getObjectListAsString() {
		return this.getProperty(Settings.OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200));
	}

	/**
	 * @return the objects for UI purposes (the first entry is 'deviceoriented' or the translated equivalent)
	 */
	public String[] getObjectList() {
		String[] objectKeys = this.getProperty(Settings.OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200)).split(GDE.STRING_SEMICOLON);
		objectKeys[0] = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		return objectKeys;
	}

	/**
	 * @param activeObjectList are the objects for UI purposes (the first entry is 'deviceoriented' or the translated equivalent)
	 * @param newActiveObjectIndex related to {@code activeObjectList}
	 */
	public void setObjectList(String[] activeObjectList, int newActiveObjectIndex) {
		String activeObjectKey = activeObjectList[0];
		try {
			activeObjectKey = activeObjectList[newActiveObjectIndex];
		}
		catch (Exception e) {
			// IndexOutOfBounds may occur while object keys are renamed and not deleted
			Settings.log.log(Level.WARNING, e.getMessage(), e);
		}
		setObjectList(activeObjectList, activeObjectKey);
	}

	/**
	 * @param objectList are the unsorted object names with or without the device oriented entry
	 * @param activeObject is an object name (supports an empty string or any 'deviceoriented' string)
	 */
	public void setObjectList(String[] objectList, String activeObject) {
		// keep object oriented out of the sorting game
		boolean startsWithDeviceOriented = objectList[0].startsWith(Messages.getString(MessageIds.GDE_MSGT0200).substring(0, 10));
		if (startsWithDeviceOriented) {
			String[] tmpObjectKeys = new String[objectList.length - 1];
			System.arraycopy(objectList, 1, tmpObjectKeys, 0, objectList.length - 1);
			Arrays.sort(tmpObjectKeys, String.CASE_INSENSITIVE_ORDER);
			System.arraycopy(tmpObjectKeys, 0, objectList, 1, objectList.length - 1);
		}
		else {
			Arrays.sort(objectList, String.CASE_INSENSITIVE_ORDER);
			String[] tmpObjectKeys = new String[objectList.length + 1];
			tmpObjectKeys[0] = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
			System.arraycopy(objectList, 0, tmpObjectKeys, 1, objectList.length);
			objectList = tmpObjectKeys;
		}
		// check for invalid object key
		Vector<String> tmpObjectVector = new Vector<String>();
		for (String objectKey : objectList) {
			boolean isDuplicateKey = tmpObjectVector.stream().anyMatch(x -> x.equalsIgnoreCase(objectKey));
			if (!isDuplicateKey && objectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH) tmpObjectVector.add(objectKey.trim());
		}

		// find the active object index within sorted array
		StringBuffer sb = new StringBuffer();
		for (String element : tmpObjectVector) {
			sb.append(element).append(GDE.STRING_SEMICOLON);
		}
		this.setProperty(Settings.OBJECT_LIST, sb.toString());
		this.setProperty(Settings.ACTIVE_OBJECT, activeObject);
	}

	/**
	 * @return the index based on the object list (or 0 if no match)
	 */
	public int getActiveObjectIndex() {
		Vector<String> tmpObjectVector = new Vector<String>();
		for (String objectKey : this.getObjectList()) {
			if (objectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH) tmpObjectVector.add(objectKey);
		}
		int index = tmpObjectVector.indexOf(this.getProperty(Settings.ACTIVE_OBJECT, Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0]).trim());
		return index < 0 ? 0 : index;
	}

	/**
	 * @return the object for UI purposes (might be 'deviceoriented' or the translated equivalent)
	 */
	public String getActiveObject() {
		return getObjectList()[getActiveObjectIndex()];
	}

	/**
	 * @return the active object key or empty if deviceoriented
	 */
	public String getActiveObjectKey() {
		String[] objectKeys = this.getProperty(Settings.OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200)).split(GDE.STRING_SEMICOLON);
		objectKeys[0] = "";
		return objectKeys[getActiveObjectIndex()];
	}

	/**
	 * @param newObjectKey is an object name (supports an empty string or any 'deviceoriented' string) which is checked in the getter for conformity with the objects list
	 */
	public void setActiveObjectKey(String newObjectKey) {
		this.setProperty(Settings.ACTIVE_OBJECT, newObjectKey);
	}

	/**
	 * @return the settingsFilePath
	 */
	public static String getSettingsFilePath() {
		return GDE.SETTINGS_FILE_PATH;
	}

	/**
	 * @return the applHomePath
	 */
	public static String getApplHomePath() {
		return GDE.APPL_HOME_PATH;
	}

	/**
	 * @return the devicesFilePath
	 */
	public static String getDevicesPath() {
		return String.format("%s%s%s", GDE.APPL_HOME_PATH, GDE.STRING_FILE_SEPARATOR_UNIX, Settings.DEVICE_PROPERTIES_DIR_NAME).replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
	}

	/**
	 * @return the graphicsTemplatePath
	 */
	public static String getGraphicsTemplatePath() {
		return String.format("%s%s%s", GDE.APPL_HOME_PATH, GDE.STRING_FILE_SEPARATOR_UNIX, Settings.GRAPHICS_TEMPLATES_DIR_NAME).replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
	}

	/**
	 * @return the log file path
	 */
	public static String getLogFilePath() {
		final String $METHOD_NAME = "getLogFilePath"; //$NON-NLS-1$
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "applHomePath = " + GDE.APPL_HOME_PATH); //$NON-NLS-1$
		return GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.LOG_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.LOG_FILE;
	}

	/**
	 * @return the log file path for the serial trace logs
	 */
	public static String getSerialLogFilePath() {
		return GDE.APPL_HOME_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.LOG_PATH + GDE.STRING_FILE_SEPARATOR_UNIX + Settings.SERIAL_LOG_FILE;
	}

	/**
	 * @return the default dataFilePath
	 */
	public String getDataFilePath() {
		final String $METHOD_NAME = "getDataFilePath"; //$NON-NLS-1$
		String dataPath = this.getProperty(Settings.DATA_FILE_PATH, System.getProperty("user.home")).replace("\\\\", GDE.STRING_FILE_SEPARATOR_UNIX).replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX); //$NON-NLS-1$
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "dataFilePath = " + dataPath); //$NON-NLS-1$
		return dataPath.trim();
	}

	/**
	 * set the default dataFilePath
	 */
	public void setDataFilePath(String newDataFilePath) {
		final String $METHOD_NAME = "setDataFilePath"; //$NON-NLS-1$
		String filePath = newDataFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX).trim();
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "newDataFilePath = " + filePath); //$NON-NLS-1$
		this.setProperty(Settings.DATA_FILE_PATH, filePath);
	}

	/**
	 * @return the list separator
	 */
	public char getListSeparator() {
		if (this.getProperty(Settings.LIST_SEPARATOR) == null) this.setProperty(Settings.LIST_SEPARATOR, GDE.STRING_SEMICOLON);
		return this.getProperty(Settings.LIST_SEPARATOR).trim().charAt(0);
	}

	/**
	 * set the list separator
	 */
	public void setListSeparator(String newListSeparator) {
		this.setProperty(Settings.LIST_SEPARATOR, newListSeparator.trim());
	}

	/**
	 * @return the decimal separator, default value is '.'
	 */
	public char getDecimalSeparator() {
		if (this.getProperty(Settings.DECIMAL_SEPARATOR) == null) this.setProperty(Settings.DECIMAL_SEPARATOR, GDE.STRING_DOT);
		return this.getProperty(Settings.DECIMAL_SEPARATOR).trim().charAt(0);
	}

	/**
	 * set the decimal separator
	 */
	public void setDecimalSeparator(String newDecimalSeparator) {
		this.setProperty(Settings.DECIMAL_SEPARATOR, newDecimalSeparator.trim());
	}

	/**
	 * set the usage of suggest date as leader of the to be saved filename
	 */
	public void setUsageDateAsFileNameLeader(boolean usage) {
		this.setProperty(Settings.USE_DATA_FILE_NAME_LEADER, GDE.STRING_EMPTY + usage);
	}

	/**
	 * set usage of the object key within the file name
	 */
	public void setUsageObjectKeyInFileName(boolean usage) {
		this.setProperty(Settings.USE_OBJECT_KEY_IN_FILE_NAME, GDE.STRING_EMPTY + usage);
	}

	/**
	 * set usage if files should be written each 5 minutes
	 */
	public void setUsageWritingTmpFiles(boolean usage) {
		this.setProperty(Settings.WRITE_TMP_FILES, GDE.STRING_EMPTY + usage);
	}

	/**
	 * get the usage of suggest date as leader of the to be saved filename
	 */
	public boolean getUsageDateAsFileNameLeader() {
		return Boolean.valueOf(this.getProperty(Settings.USE_DATA_FILE_NAME_LEADER, "true")); //$NON-NLS-1$
	}

	/**
	 * get usage of the object key within the file name
	 */
	public boolean getUsageObjectKeyInFileName() {
		return Boolean.valueOf(this.getProperty(Settings.USE_OBJECT_KEY_IN_FILE_NAME, "false")); //$NON-NLS-1$
	}

	/**
	 * get usage if files should be written each 5 minutes
	 */
	public boolean getUsageWritingTmpFiles() {
		return Boolean.valueOf(this.getProperty(Settings.WRITE_TMP_FILES, "false")); //$NON-NLS-1$
	}

	/**
	 * @return boolean value of port black list enablement
	 */
	public boolean isSerialPortBlackListEnabled() {
		return Boolean.valueOf(this.getProperty(Settings.IS_PORT_BLACKLIST, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global serial port black list enabled
	 */
	public void setSerialPortBlackListEnabled(boolean enabled) {
		this.setProperty(Settings.IS_PORT_BLACKLIST, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return port black list
	 */
	public String getSerialPortBlackList() {
		StringBuffer blackList = new StringBuffer();
		for (String port : this.getProperty(Settings.PORT_BLACKLIST, GDE.STRING_EMPTY).trim().split(GDE.STRING_BLANK)) {
			if (port != null && port.length() > 3) blackList.append(port).append(GDE.STRING_BLANK);
		}
		return blackList.toString().trim();
	}

	/**
	 * set the serial port black list
	 */
	public void setSerialPortBlackList(String newPortBlackList) {
		StringBuilder blackList = new StringBuilder();
		for (String tmpPort : newPortBlackList.split(GDE.STRING_BLANK)) {
			if (GDE.IS_WINDOWS && tmpPort.toUpperCase().startsWith(Settings.WINDOWS_PORT_COM))
				blackList.append(tmpPort.toUpperCase()).append(GDE.STRING_BLANK);
			else if (tmpPort.startsWith(Settings.UNIX_PORT_DEV_TTY)) blackList.append(tmpPort).append(GDE.STRING_BLANK);
		}
		this.setProperty(Settings.PORT_BLACKLIST, blackList.toString());
	}

	/**
	 * @return boolean value of port white list enablement
	 */
	public boolean isSerialPortWhiteListEnabled() {
		return Boolean.valueOf(this.getProperty(Settings.IS_PORT_WHITELIST, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the serial port white list enabled
	 */
	public void setSerialPortWhiteListEnabled(boolean enabled) {
		this.setProperty(Settings.IS_PORT_WHITELIST, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return port white list as vector
	 */
	public Vector<String> getSerialPortWhiteList() {
		Vector<String> whiteList = new Vector<String>();
		for (String port : this.getProperty(Settings.PORT_WHITELIST, GDE.STRING_EMPTY).trim().split(";| ")) { //$NON-NLS-1$
			if (port != null && port.length() > 3) whiteList.add(port);
		}
		return whiteList;
	}

	/**
	 * @return port white list as String
	 */
	public String getSerialPortWhiteListString() {
		StringBuffer whiteList = new StringBuffer();
		for (String port : this.getProperty(Settings.PORT_WHITELIST, GDE.STRING_EMPTY).trim().split(";| ")) { //$NON-NLS-1$
			if (port != null && port.length() > 3) whiteList.append(port).append(GDE.STRING_BLANK);
		}
		return whiteList.toString().trim();
	}

	/**
	 * set the serial port white list
	 */
	public void setSerialPortWhiteList(String newPortWhiteList) {
		StringBuilder whiteList = new StringBuilder();
		for (String tmpPort : newPortWhiteList.split(GDE.STRING_BLANK)) {
			if (GDE.IS_WINDOWS && tmpPort.toUpperCase().startsWith(Settings.WINDOWS_PORT_COM))
				whiteList.append(tmpPort.toUpperCase()).append(GDE.STRING_SEMICOLON);
			else if (tmpPort.startsWith(Settings.UNIX_PORT_DEV_TTY)) whiteList.append(tmpPort).append(GDE.STRING_BLANK);
		}
		this.setProperty(Settings.PORT_WHITELIST, whiteList.toString());
	}

	/**
	 * @return boolean value if data gathering should only add charge and discharge data
	 */
	public boolean isReduceChargeDischarge() {
		return Boolean.valueOf(this.getProperty(Settings.IS_REDUCE_CHARGE_DISCHARGE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data gathering should only add charge and discharge data
	 */
	public void setReduceChargeDischarge(boolean enabled) {
		this.setProperty(Settings.IS_REDUCE_CHARGE_DISCHARGE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return boolean value if data gathering should result in a single recordSet
	 */
	public boolean isContinuousRecordSet() {
		return Boolean.valueOf(this.getProperty(Settings.IS_ALL_IN_ONE_RECORDSET, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data gathering should result in a single recordSet
	 */
	public void setContinuousRecordSet(boolean enabled) {
		this.setProperty(Settings.IS_ALL_IN_ONE_RECORDSET, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return boolean value if data table display selected records from graphics
	 */
	public boolean isPartialDataTable() {
		return Boolean.valueOf(this.getProperty(Settings.IS_PARTIAL_DATA_TABLE, "true").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data table should show only selected records from curve view
	 */
	public void setPartialDataTable(boolean enabled) {
		this.setProperty(Settings.IS_PARTIAL_DATA_TABLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return boolean value if data table displayed selected record entry is editable, this preference is not persistence
	 */
	public boolean isDataTableEditable() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DATA_TABLE_EDITABLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data table should enable editing selected record entry, this preference will not be made persistence
	 */
	public void setDataTableEditable(boolean enabled) {
		this.setProperty(Settings.IS_DATA_TABLE_EDITABLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return the global log level
	 */
	public boolean isGlobalLogLevel() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GLOBAL_LOG_LEVEL, "true").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global log level
	 */
	public void setIsGlobalLogLevel(String isGlobalLogLevel) {
		this.setProperty(Settings.IS_GLOBAL_LOG_LEVEL, isGlobalLogLevel.trim());
	}

	/**
	 * set property if during port scan disable detection of bluetooth devices
	 */
	public void setSkipBluetoothDevices(boolean enabled) {
		setProperty(Settings.SKIP_BLUETOOTH_DEVICES, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * get property if during port scan disable detection of bluetooth devices
	 */
	public boolean isSkipBluetoothDevices() {
		return Boolean.valueOf(getProperty(Settings.SKIP_BLUETOOTH_DEVICES, "true")); //$NON-NLS-1$
	}

	/**
	 * set property if during port scan a availability check should executed (disable for slow systems)
	 */
	public void setPortAvailabilityCheck(boolean enabled) {
		setProperty(Settings.DO_PORT_AVAILABLE_TEST, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * get property if during port scan a availability check should executed (disable for slow systems)
	 */
	public boolean doPortAvailabilityCheck() {
		return Boolean.valueOf(getProperty(Settings.DO_PORT_AVAILABLE_TEST, "false")); //$NON-NLS-1$
	}

	/**
	 * check if minimal settings available
	 */
	public boolean isOK() {
		boolean ok = false;
		if (getProperty(Settings.DATA_FILE_PATH) != null || getProperty(Settings.LIST_SEPARATOR) != null || getProperty(Settings.DECIMAL_SEPARATOR) != null) {
			ok = true;
		}

		return ok;
	}

	/**
	 * query if record set comment window is visible
	 */
	public boolean isRecordCommentVisible() {
		return Boolean.valueOf(this.getProperty(Settings.RECORD_COMMENT_VISIBLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set property if record set comment window is visible
	 */
	public void setRecordCommentVisible(boolean enabled) {
		this.setProperty(Settings.RECORD_COMMENT_VISIBLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query if record set comment window is visible
	 */
	public boolean isGraphicsHeaderVisible() {
		return Boolean.valueOf(this.getProperty(Settings.GRAPHICS_HEADER_VISIBLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set property if record set comment window is visible
	 */
	public void setGraphicsHeaderVisible(boolean enabled) {
		this.setProperty(Settings.GRAPHICS_HEADER_VISIBLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the grid line style
	 * @return actual grid line style as integer array
	 */
	public int[] getGridDashStyle() {
		String[] gridLineStyle = this.getProperty(Settings.GRID_DASH_STYLE, "10, 10").split(GDE.STRING_COMMA); //$NON-NLS-1$
		return new int[] { Integer.parseInt(gridLineStyle[0].trim()), Integer.parseInt(gridLineStyle[1].trim()) };
	}

	/**
	 * @return actual grid line style as string integer array
	 */
	private String getGridDashStyleAsString() {
		return this.getProperty(Settings.GRID_DASH_STYLE, "10, 10").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid line style in pixel length
	 * @param newGridDashStyle {drawn, blank}
	 */
	public void setGridDaschStyle(int[] newGridDashStyle) {
		this.setProperty(Settings.GRID_DASH_STYLE, GDE.STRING_EMPTY + newGridDashStyle[0] + ", " + newGridDashStyle[1]); //$NON-NLS-1$
	}

	/**
	 * @return the grid horizontal type of the compare window (0=none;1=each,2=eachSecond)
	 */
	public int getGridCompareWindowHorizontalType() {
		return Integer.parseInt(this.getProperty(Settings.GRID_COMPARE_WINDOW_HOR_TYPE, "0").trim()); //$NON-NLS-1$
	}

	/**
	 * set the grid horizontal type of the compare window
	 * @param newHorizontalGridType (0=none;1=each,2=eachSecond)
	 */
	public void setGridCompareWindowHorizontalType(int newHorizontalGridType) {
		this.setProperty(Settings.GRID_COMPARE_WINDOW_HOR_TYPE, GDE.STRING_EMPTY + newHorizontalGridType);
	}

	/**
	 * @return the grid horizontal color of the compare window (r,g,b)
	 */
	public Color getGridCompareWindowHorizontalColor() {
		return getColor(Settings.GRID_COMPARE_WINDOW_HOR_COLOR, "200,200,200"); //$NON-NLS-1$
	}

	/**
	 * @return the grid horizontal color of the compare window as string of (r,g,b)
	 */
	public String getGridCompareWindowHorizontalColorStr() {
		return this.getProperty(Settings.GRID_COMPARE_WINDOW_HOR_COLOR, "200,200,200").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid horizontal color of the compare window
	 * @param newColor (r,g,b)
	 */
	public void setGridCompareWindowHorizontalColor(Color newColor) {
		String rgb = newColor.getRGB().red + GDE.STRING_COMMA + newColor.getRGB().green + GDE.STRING_COMMA + newColor.getRGB().blue;
		this.setProperty(Settings.GRID_COMPARE_WINDOW_HOR_COLOR, rgb);
	}

	/**
	 * @return the grid vertical type of the compare window (0=none;1=each,2=mod60)
	 */
	public int getGridCompareWindowVerticalType() {
		return Integer.parseInt(this.getProperty(Settings.GRID_COMPARE_WINDOW_VER_TYPE, "0").trim()); //$NON-NLS-1$
	}

	/**
	 * set the grid vertical type of the compare window
	 * @param newVerticalGridType (0=none;1=each,2=eachSecond)
	 */
	public void setGridCompareWindowVerticalType(int newVerticalGridType) {
		this.setProperty(Settings.GRID_COMPARE_WINDOW_VER_TYPE, GDE.STRING_EMPTY + newVerticalGridType);
	}

	/**
	 * @return the grid vertical color of the compare window (r,g,b)
	 */
	public Color getGridCompareWindowVerticalColor() {
		return getColor(Settings.GRID_COMPARE_WINDOW_VER_COLOR, "200,200,200"); //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGridCompareWindowVerticalColorStr() {
		return this.getProperty(Settings.GRID_COMPARE_WINDOW_VER_COLOR, "200,200,200").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid vertical color of the compare window
	 * @param newColor (r,g,b)
	 */
	public void setGridCompareWindowVerticalColor(Color newColor) {
		String rgb = newColor.getRGB().red + GDE.STRING_COMMA + newColor.getRGB().green + GDE.STRING_COMMA + newColor.getRGB().blue;
		this.setProperty(Settings.GRID_COMPARE_WINDOW_VER_COLOR, rgb);
	}

	/**
	 * set global log level
	 */
	public void setGlobalLogLevel(java.util.logging.Level logLevel) {
		Logger logger = Logger.getLogger(GDE.STRING_EMPTY);
		logger.setLevel(logLevel);
		logger.setUseParentHandlers(true);
	}

	/**
	 * set individual log level
	 */
	public void setIndividualLogLevel(String packageName, java.util.logging.Level logLevel) {
		Logger logger = Logger.getLogger(packageName);
		logger.setLevel(logLevel);
		logger.setUseParentHandlers(true);
	}

	/**
	 * method to update the logging level
	 */
	public void updateLogLevel() {
		if (isGlobalLogLevel()) {
			java.util.logging.Level globalLogLevel = java.util.logging.Level.parse(getProperty(Settings.GLOBAL_LOG_LEVEL, "WARNING").trim()); //$NON-NLS-1$
			setIndividualLogLevel("gde.ui", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.data", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.config", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.device", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.utils", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.io", globalLogLevel); //$NON-NLS-1$
			setGlobalLogLevel(globalLogLevel);
			setLevelSerialIO(globalLogLevel);
			Enumeration<Object> e = Settings.classbasedLogger.keys();
			while (e.hasMoreElements()) {
				String loggerName = (String) e.nextElement();
				setIndividualLogLevel(loggerName, java.util.logging.Level.parse("SEVERE")); //$NON-NLS-1$
			}
			Settings.classbasedLogger.clear();
		}
		else {
			setGlobalLogLevel(java.util.logging.Level.parse(getProperty(Settings.GLOBAL_LOG_LEVEL, "WARNING").trim())); //$NON-NLS-1$
			setIndividualLogLevel("gde.ui", getLogLevel(Settings.UI_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.data", getLogLevel(Settings.DATA_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.config", getLogLevel(Settings.CONFIG_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.device", getLogLevel(Settings.DEVICE_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.utils", getLogLevel(Settings.UTILS_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.io", getLogLevel(Settings.FILE_IO_LOG_LEVEL)); //$NON-NLS-1$
			setLevelSerialIO(getLogLevel(Settings.SERIAL_IO_LOG_LEVEL));
			Enumeration<Object> e = Settings.classbasedLogger.keys();
			while (e.hasMoreElements()) {
				String loggerName = (String) e.nextElement();
				setIndividualLogLevel(loggerName, java.util.logging.Level.parse(Settings.classbasedLogger.getProperty(loggerName)));
			}
		}
	}

	/**
	 * @return log level from the given categorie, in case of parse error fall back to Level.WARNING
	 */
	java.util.logging.Level getLogLevel(String logCategorie) {
		java.util.logging.Level logLevel = java.util.logging.Level.WARNING;
		try {
			logLevel = java.util.logging.Level.parse(getProperty(logCategorie, "WARNING").trim()); //$NON-NLS-1$
		}
		catch (IllegalArgumentException e) {
			// ignore and fall back to WARNING
			setProperty(logCategorie, "WARNING"); //$NON-NLS-1$
		}
		return logLevel;
	}

	private void setLevelSerialIO(java.util.logging.Level logLevel) {
		final String $METHOD_NAME = "setLevelSerialIO"; //$NON-NLS-1$
		try {
			Logger logger = Logger.getLogger("gde.comm.DeviceSerialPortImpl"); //$NON-NLS-1$
			for (Handler handler : logger.getHandlers()) {
				logger.removeHandler(handler);
			}
			logger.setLevel(logLevel);
			if (logLevel.intValue() < java.util.logging.Level.parse("WARNING").intValue()) { //$NON-NLS-1$
				logger.setUseParentHandlers(false);
				Handler fh = new FileHandler(Settings.getSerialLogFilePath(), 15000000, 3);
				fh.setFormatter(new LogFormatter());
				logger.addHandler(new MemoryHandler(fh, 5000000, logLevel));
			}
			else
				logger.setUseParentHandlers(true); // enable global log settings

		}
		catch (Exception e) {
			Settings.log.logp(java.util.logging.Level.WARNING, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
	}

	/**
	 * set device dialog behavior, application modal or application equivalent
	 * @param enabled
	 */
	public void enabelModalDeviceDialogs(boolean enabled) {
		this.setProperty(Settings.DEVICE_DIALOG_USE_MODAL, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the the device dialogs behavior
	 * @return boolean value to signal the modality of the device dialog
	 */
	public boolean isDeviceDialogsModal() {
		return Boolean.valueOf(this.getProperty(Settings.DEVICE_DIALOG_USE_MODAL, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set device dialog behavior, always visible
	 * @param enabled
	 */
	public void enabelDeviceDialogsOnTop(boolean enabled) {
		this.setProperty(Settings.DEVICE_DIALOG_ON_TOP, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the the device dialogs behavior
	 * @return boolean value to signal the placement of the device dialog
	 */
	public boolean isDeviceDialogsOnTop() {
		return Boolean.valueOf(this.getProperty(Settings.DEVICE_DIALOG_ON_TOP, "false").trim()); //$NON-NLS-1$
	}

	public DeviceSerialization getDeviceSerialization() {
		return this.deviceSerialization;
	}

	/**
	 * query if locale has been changed, this will be used to copy new set of device property files to users application directory
	 * @return
	 */
	boolean getLocaleChanged() {
		return Boolean.valueOf(this.getProperty(Settings.LOCALE_CHANGED, "false")); //$NON-NLS-1$
	}

	/**
	 * set new locale language (de,en, ..), if language has been changed set locale changed to true to indicate copy device properties
	 * @param newLanguage
	 */
	public void setLocaleLanguage(String newLanguage) {
		if (!this.getLocale().getLanguage().equals(newLanguage)) {
			this.setProperty(Settings.LOCALE_CHANGED, "true"); //$NON-NLS-1$
			this.setProperty(Settings.LOCALE_IN_USE, newLanguage);
		}
	}

	/**
	 * query the locale language (en, de, ...) to be used to copy set of localized device property files
	 * if local is not supported, ENGLISH is used as default
	 * @return used locale
	 */
	public Locale getLocale() {
		// get the locale from loaded properties or system default
		Locale locale = new Locale(this.getProperty(Settings.LOCALE_IN_USE, Locale.getDefault().getLanguage()));
		if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage()) || locale.getLanguage().equals(Locale.GERMAN.getLanguage()) || locale.getLanguage().equals(Locale.ITALIAN.getLanguage())) {
			this.setProperty(Settings.LOCALE_IN_USE, locale.getLanguage());
		}
		else {
			this.setProperty(Settings.LOCALE_IN_USE, Locale.ENGLISH.getLanguage()); // fall back to 'en' as default for not supported localse
			locale = Locale.ENGLISH;
		}
		return locale;
	}

	/**
	 * set new time format to be used in table, while measurements and at start time while zooming
	 * @param newTimeFormat
	 */
	public void setTimeFormat(String newTimeFormat) {
		this.setProperty(Settings.TIME_FORMAT_IN_USE, newTimeFormat);
	}

	/**
	 * query the time format to be used in table, while measurements and at start time while zooming
	 * @return used time format as string
	 */
	public String getTimeFormat() {
		return this.getProperty(Settings.TIME_FORMAT_IN_USE, Messages.getString(MessageIds.GDE_MSGT0684));
	}

	/**
	 * query the time format is set to absolute
	 * @return used time format as string
	 */
	public boolean isTimeFormatAbsolute() {
		return this.getProperty(Settings.TIME_FORMAT_IN_USE, Messages.getString(MessageIds.GDE_MSGT0684)).trim().equals(Messages.getString(MessageIds.GDE_MSGT0359));
	}

	/**
	 * get the device dialog alpha value, 50 is a good transparency starting point
	 * @return the alphablending value
	 */
	public int getDialogAlphaValue() {
		return Integer.parseInt(this.getProperty(Settings.ALPHA_BLENDING_VALUE, "50").trim()); //$NON-NLS-1$
	}

	/**
	 * set a new alpha transparency value for the device dialog
	 * @param newAlphaValue
	 */
	public void setDialogAlphaValue(int newAlphaValue) {
		this.setProperty(Settings.ALPHA_BLENDING_VALUE, GDE.STRING_EMPTY + newAlphaValue);
	}

	/**
	 * set if alpha blending for device dialog should be used
	 * (supporting window manager is pre-req, covered by SWT)
	 * @param enable
	 */
	public void setDeviceDialogAlphaEnabled(boolean enable) {
		this.setProperty(Settings.APLHA_BLENDING_ENABLED, GDE.STRING_EMPTY + enable);
	}

	/**
	 * query usage of alpha blending for device dialog
	 * @return true if alphablending is enabled
	 */
	public boolean isDeviceDialogAlphaEnabled() {
		return Boolean.valueOf(this.getProperty(Settings.APLHA_BLENDING_ENABLED, "false")); //$NON-NLS-1$
	}

	/**
	 * set if the device import directory suggestion is independent of selected object key
	 * @param enable
	 */
	public void setDeviceImportDirectoryObjectRelated(boolean enable) {
		this.setProperty(Settings.KEEP_IMPORT_DIR_OBJECT_RELATED, GDE.STRING_EMPTY + enable);
	}

	/**
	 * query usage of device import directory
	 * @return true if directory suggestion follows object key
	 */
	public boolean isDeviceImportDirectoryObjectRelated() {
		return Boolean.valueOf(this.getProperty(Settings.KEEP_IMPORT_DIR_OBJECT_RELATED, "false")); //$NON-NLS-1$
	}

	/**
	 * @return the isDevicePropertiesUpdated
	 */
	public boolean isDevicePropertiesUpdated() {
		return this.isDevicePropertiesUpdated;
	}

	/**
	 * @return the isGraphicsTemplateUpdated
	 */
	public boolean isGraphicsTemplateUpdated() {
		return this.isGraphicsTemplateUpdated;
	}

	/**
	 * @return the isHistoCacheTemplateUpdated
	 */
	public boolean isHistoCacheTemplateUpdated() {
		return this.isHistocacheTemplateUpdated;
	}

	/**
	 * query value if desktop shortcut needs to be created
	 */
	public boolean isDesktopShortcutCreated() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DESKTOP_SHORTCUT_CREATED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if DataExplorer application is registered to operating system
	 */
	public boolean isApplicationRegistered() {
		return Boolean.valueOf(this.getProperty(Settings.IS_APPL_REGISTERED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if a hint was displayed to enalble uucp locking used on UNIX based systems with RXTXcomm
	 */
	public boolean isLockUucpHinted() {
		return Boolean.valueOf(this.getProperty(Settings.IS_LOCK_UUCP_HINTED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if a hint was displayed to enalble uucp locking used on UNIX based systems with RXTXcomm
	 */
	public boolean isUpdateChecked() {
		return this.getProperty(Settings.LAST_UPDATE_CHECK, "2000-01-01").equals(StringHelper.getDate()); //$NON-NLS-1$
	}

	/**
	 * set the background color of the main graphics curve area
	 * @param curveAreaBackground
	 */
	public void setGraphicsCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(Settings.GRAPHICS_AREA_BACKGROUND, curveAreaBackground.getRed() + GDE.STRING_COMMA + curveAreaBackground.getGreen() + GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * set the background color of the main graphics curve area
	 * @return requested color
	 */
	public Color getGraphicsCurveAreaBackground() {
		return getColor(Settings.GRAPHICS_AREA_BACKGROUND, "250,249,211"); // COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsCurveAreaBackgroundStr() {
		return this.getProperty(Settings.GRAPHICS_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve area
	 * @param curveAreaBackground
	 */
	public void setCompareCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(Settings.COMPARE_AREA_BACKGROUND, curveAreaBackground.getRed() + GDE.STRING_COMMA + curveAreaBackground.getGreen() + GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve area
	 * @return requested color
	 */
	public Color getCompareCurveAreaBackground() {
		return getColor(Settings.COMPARE_AREA_BACKGROUND, "250,249,211"); // COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCompareCurveAreaBackgroundStr() {
		return this.getProperty(Settings.COMPARE_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve area
	 * @param curveAreaBackground
	 */
	public void setUtilityCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(Settings.UTILITY_AREA_BACKGROUND, curveAreaBackground.getRed() + GDE.STRING_COMMA + curveAreaBackground.getGreen() + GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve area
	 * @return requested color
	 */
	public Color getUtilityCurveAreaBackground() {
		return getColor(Settings.UTILITY_AREA_BACKGROUND, "250,249,211"); // COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getUtilityCurveAreaBackgroundStr() {
		return this.getProperty(Settings.UTILITY_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the main graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setGraphicsSurroundingBackground(Color surroundingBackground) {
		this.setProperty(Settings.GRAPHICS_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the main graphics curve surrounding area
	 * @return requested color
	 */
	public Color getGraphicsSurroundingBackground() {
		return getColor(Settings.GRAPHICS_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsSurroundingBackgroundStr() {
		return this.getProperty(Settings.GRAPHICS_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setCompareSurroundingBackground(Color surroundingBackground) {
		this.setProperty(Settings.COMPARE_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve surrounding area
	 * @return requested color
	 */
	public Color getCompareSurroundingBackground() {
		return getColor(Settings.COMPARE_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCompareSurroundingBackgroundStr() {
		return this.getProperty(Settings.COMPARE_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setUtilitySurroundingBackground(Color surroundingBackground) {
		this.setProperty(Settings.UTILITY_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve surrounding area
	 * @return requested color
	 */
	public Color getUtilitySurroundingBackground() {
		return getColor(Settings.UTILITY_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getUtilitySurroundingBackgroundStr() {
		return this.getProperty(Settings.UTILITY_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the graphics curve area
	 * @param borderColor
	 */
	public void setCurveGraphicsBorderColor(Color borderColor) {
		this.setProperty(Settings.GRAPHICS_BORDER_COLOR, borderColor.getRed() + GDE.STRING_COMMA + borderColor.getGreen() + GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * get the border color of the graphics curve area
	 * @return requested color
	 */
	public Color getGraphicsCurvesBorderColor() {
		return getColor(Settings.GRAPHICS_BORDER_COLOR, "180,180,180"); // COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsCurvesBorderColorStr() {
		return this.getProperty(Settings.GRAPHICS_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the curve compare graphics area
	 * @param borderColor
	 */
	public void setCurveCompareBorderColor(Color borderColor) {
		this.setProperty(Settings.COMPARE_BORDER_COLOR, borderColor.getRed() + GDE.STRING_COMMA + borderColor.getGreen() + GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * set the background color of the curve compare graphics area
	 * @return requested color
	 */
	public Color getCurveCompareBorderColor() {
		return getColor(Settings.COMPARE_BORDER_COLOR, "180,180,180"); // COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCurveCompareBorderColorStr() {
		return this.getProperty(Settings.COMPARE_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the graphics curve area
	 * @param borderColor
	 */
	public void setUtilityCurvesBorderColor(Color borderColor) {
		this.setProperty(Settings.UTILITY_BORDER_COLOR, borderColor.getRed() + GDE.STRING_COMMA + borderColor.getGreen() + GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * get the border color of the graphics curve area
	 * @return requested color
	 */
	public Color getUtilityCurvesBorderColor() {
		return getColor(Settings.UTILITY_BORDER_COLOR, "180,180,180"); // COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the utility window as string of (r,g,b)
	 */
	public String getUtilityCurvesBorderColorStr() {
		return this.getProperty(Settings.UTILITY_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the statistics window surrounding area
	 * @param surroundingBackground
	 */
	public void setSatisticsSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.STATISTICS_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getStatisticsSurroundingAreaBackground() {
		return getColor(Settings.STATISTICS_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getStatisticsSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.STATISTICS_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the statistics window surrounding area
	 * @param innerAreaBackground
	 */
	public void setSatisticsInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.STATISTICS_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getStatisticsInnerAreaBackground() {
		return getColor(Settings.STATISTICS_INNER_BACKGROUND, "255,255,255"); // COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getStatisticsInnerAreaBackgroundStr() {
		return this.getProperty(Settings.STATISTICS_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the analog window surrounding area
	 * @param surroundingBackground
	 */
	public void setAnalogSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.ANALOG_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the analog window surrounding area
	 * @return requested color
	 */
	public Color getAnalogSurroundingAreaBackground() {
		return getColor(Settings.ANALOG_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getAnalogSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.ANALOG_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the analog window surrounding area
	 * @param surroundingBackground
	 */
	public void setAnalogInnerAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.ANALOG_INNER_BACKGROUND, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the analog window surrounding area
	 * @return requested color
	 */
	public Color getAnalogInnerAreaBackground() {
		return getColor(Settings.ANALOG_INNER_BACKGROUND, "250,249,211"); // COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getAnalogInnerAreaBackgroundStr() {
		return this.getProperty(Settings.ANALOG_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the digital window surrounding area
	 * @param surroundingBackground
	 */
	public void setDigitalSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.DIGITAL_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the digital window surrounding area
	 * @return requested color
	 */
	public Color getDigitalSurroundingAreaBackground() {
		return getColor(Settings.DIGITAL_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getDigitalSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.DIGITAL_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the digital window surrounding area
	 * @param innerAreaBackground
	 */
	public void setDigitalInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.DIGITAL_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getDigitalInnerAreaBackground() {
		return getColor(Settings.DIGITAL_INNER_BACKGROUND, "250,249,211"); // COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getDigitalInnerAreaBackgroundStr() {
		return this.getProperty(Settings.DIGITAL_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the cell voltage window surrounding area
	 * @param surroundingBackground
	 */
	public void setCellVoltageSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.CELL_VOLTAGE_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the cell voltage window surrounding area
	 * @return requested color
	 */
	public Color getCellVoltageSurroundingAreaBackground() {
		return getColor(Settings.CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCellVoltageSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the cell voltage window surrounding area
	 * @param innerAreaBackground
	 */
	public void setCellVoltageInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.CELL_VOLTAGE_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the cell voltage window surrounding area
	 * @return requested color
	 */
	public Color getCellVoltageInnerAreaBackground() {
		return getColor(Settings.CELL_VOLTAGE_INNER_BACKGROUND, "250,249,211"); // COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCellVoltageInnerAreaBackgroundStr() {
		return this.getProperty(Settings.CELL_VOLTAGE_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the file comment window surrounding area
	 * @param surroundingBackground
	 */
	public void setFileCommentSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.FILE_COMMENT_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the file comment window surrounding area
	 * @return requested color
	 */
	public Color getFileCommentSurroundingAreaBackground() {
		return getColor(Settings.FILE_COMMENT_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getFileCommentSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.FILE_COMMENT_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the file comment window surrounding area
	 * @param innerAreaBackground
	 */
	public void setFileCommentInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.FILE_COMMENT_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the file comment window surrounding area
	 * @return requested color
	 */
	public Color getFileCommentInnerAreaBackground() {
		return getColor(Settings.FILE_COMMENT_INNER_BACKGROUND, "255,255,255"); // COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getFileCommentInnerAreaBackgroundStr() {
		return this.getProperty(Settings.FILE_COMMENT_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the object description window surrounding area
	 * @param surroundingBackground
	 */
	public void setObjectDescriptionSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.OBJECT_DESC_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the object description window surrounding area
	 * @return requested color
	 */
	public Color getObjectDescriptionSurroundingAreaBackground() {
		return getColor(Settings.OBJECT_DESC_SURROUND_BACKGRD, "250,249,230"); // COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getObjectDescriptionSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.OBJECT_DESC_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the object description window surrounding area
	 * @param innerAreaBackground
	 */
	public void setObjectDescriptionInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.OBJECT_DESC_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the object description window surrounding area
	 * @return requested color
	 */
	public Color getObjectDescriptionInnerAreaBackground() {
		return getColor(Settings.OBJECT_DESC_INNER_BACKGROUND, "255,255,255"); // COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getObjectDescriptionInnerAreaBackgroundStr() {
		return this.getProperty(Settings.OBJECT_DESC_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * @return color according the given color key or the default value
	 */
	private Color getColor(String colorKey, String colorDefault) {
		String color = this.getProperty(colorKey, colorDefault); // CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230"
		int r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
		int g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
		int b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
		return SWTResourceManager.getColor(r, g, b);
	}

	public void joinXsdThread() {
		try {
			this.xsdThread.join();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void startMigationThread() {
		if (this.migrationThread != null) this.migrationThread.run();
	}

	public void joinMigationThread() {
		try {
			this.migrationThread.join();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @return the image data file path
	 */
	public String getObjectImageFilePath() {
		final String $METHOD_NAME = "getObjectImageFilePath"; //$NON-NLS-1$
		String objectImageFilePath = this.getProperty(Settings.OBJECT_IMAGE_FILE_PATH, getDataFilePath()).replace("\\\\", GDE.STRING_FILE_SEPARATOR_UNIX) //$NON-NLS-1$
				.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "objectImageFilePath = " + objectImageFilePath); //$NON-NLS-1$
		return objectImageFilePath.trim();
	}

	/**
	 * set the image data file path
	 */
	public void setObjectImageFilePath(String newImageFilePath) {
		final String $METHOD_NAME = "setObjectImageFilePath"; //$NON-NLS-1$
		String filePath = newImageFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX).trim();
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "newDataFilePath = " + filePath); //$NON-NLS-1$
		this.setProperty(Settings.OBJECT_IMAGE_FILE_PATH, filePath);
	}

	/**
	 * set boolean value if the scale of the records drawn in graphics should be in same color as the curve
	 * @param isUseRecordColor
	 */
	public void setDrawScaleInRecordColor(boolean isUseRecordColor) {
		this.setProperty(Settings.IS_GRAPHICS_SCALE_COLOR, GDE.STRING_EMPTY + isUseRecordColor);
	}

	/**
	 * @return boolean value of true if the scale of the records drawn in graphics should be in same color as the curve
	 */
	public boolean isDrawScaleInRecordColor() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GRAPHICS_SCALE_COLOR, "true")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if the name of the records drawn in graphics should be in same color as the curve
	 * @param isUseRecordColor
	 */
	public void setDrawNameInRecordColor(boolean isUseRecordColor) {
		this.setProperty(Settings.IS_GRAPHICS_NAME_COLOR, GDE.STRING_EMPTY + isUseRecordColor);
	}

	/**
	 * @return boolean value of true if the name at the scale of the records drawn in graphics should be in same color as the curve
	 */
	public boolean isDrawNameInRecordColor() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GRAPHICS_NAME_COLOR, "true")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if boolean value of true if the number of ticks of the records drawn in graphics should be 10
	 * @param isDraw10TicksPerRecord
	 */
	public void setDraw10TicksPerRecord(boolean isDraw10TicksPerRecord) {
		this.setProperty(Settings.IS_GRAPHICS_TICKS_10, GDE.STRING_EMPTY + isDraw10TicksPerRecord);
	}

	/**
	 * @return boolean value of true if the number of ticks of the records drawn in graphics should be 10
	 */
	public boolean isDraw10TicksPerRecord() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GRAPHICS_TICKS_10, "false")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if the scale of the records drawn in graphics should be in same color as the curve
	 * @param isUseRecordColor
	 */
	public void setDrawNumbersInRecordColor(boolean isUseRecordColor) {
		this.setProperty(Settings.IS_GRAPHICS_NUMBERS_COLOR, GDE.STRING_EMPTY + isUseRecordColor);
	}

	/**
	 * @return boolean value of true if the number at the scale of the records drawn in graphics should be in same color as the curve
	 */
	public boolean isDrawNumbersInRecordColor() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GRAPHICS_NUMBERS_COLOR, "false")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if the channel/configuration name should be used as leader of record name in curve compare
	 * @param isUseChannelConfigName
	 */
	public void setCurveCompareChannelConfigName(boolean isUseChannelConfigName) {
		this.setProperty(Settings.IS_COMPARE_CHANNELCONFIG, GDE.STRING_EMPTY + isUseChannelConfigName);
	}

	/**
	 * @return boolean value of true if the channel/configuration name should be used as leader of record name in curve compare
	 */
	public boolean isCurveCompareChannelConfigName() {
		return Boolean.valueOf(this.getProperty(Settings.IS_COMPARE_CHANNELCONFIG, "false")); //$NON-NLS-1$
	}

	/**
	 * set double value of display density font correction value
	 */
	public void setFontDisplayDensityAdaptionFactor(double newCorrectionValue) {
		this.setProperty(Settings.DISPLAY_DENSITY_FONT_CORRECT, GDE.STRING_EMPTY + newCorrectionValue);
	}

	/**
	 * @return double value of display density font correction value
	 */
	public double getFontDisplayDensityAdaptionFactor() {
		return Double.valueOf(this.getProperty(Settings.DISPLAY_DENSITY_FONT_CORRECT, "1.0")); //$NON-NLS-1$ ;
	}

	/**
	 * set the time step in milliseconds to be used for KMZ/KML export
	 */
	public void setKmzExportTimeStep_ms(int newTimeStepValue) {
		this.setProperty(Settings.KMZ_EXPORT_TIME_STEP_MS, GDE.STRING_EMPTY + newTimeStepValue);
	}

	/**
	 * @return time step in milliseconds to be used for KMZ/KML export
	 */
	public int getKmzExportTimeStep_ms() {
		return Integer.valueOf(this.getProperty(Settings.KMZ_EXPORT_TIME_STEP_MS, "500")); //$NON-NLS-1$ ;
	}

	/**
	 * set boolean value if the history analysis tabs should be visible
	 * @param isActive
	 */
	public void setHistoActive(boolean isActive) {
		this.setProperty(Settings.IS_HISTO_ACTIVE, String.valueOf(isActive));
	}

	/**
	 * @return boolean true if the history labels should be visible if the current device supports the history
	 */
	public boolean isHistoActive() {
		return Boolean.valueOf(this.getProperty(Settings.IS_HISTO_ACTIVE, "false")); //$NON-NLS-1$
	}

	/**
	 * @return three boxplot graphics sizes as localized texts
	 */
	public static String[] getBoxplotScaleNomenclatures() {
		return Messages.getString(MessageIds.GDE_MSGT0802).split(GDE.STRING_CSV_SEPARATOR);
	}

	/**
	 * set the boxplot size for the history
	 * @param scaleNomenclature
	 */
	public void setBoxplotScale(String scaleNomenclature) {
		this.setProperty(Settings.BOXPLOT_SCALE_ORDINAL, String.valueOf(Arrays.asList(getBoxplotScaleNomenclatures()).indexOf(scaleNomenclature)));
	}

	/**
	 * @return the boxplot size for the history (default is medium size)
	 */
	public String getBoxplotScale() {
		return getBoxplotScaleNomenclatures()[getBoxplotScaleOrdinal()];
	}

	/**
	 * @return the boxplot size ordinal for the history (default is medium size)
	 */
	public int getBoxplotScaleOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.BOXPLOT_SCALE_ORDINAL, String.valueOf(1)));
	}

	/**
	 * @return four boxplot size adaptation levels as localized texts ranging from none to large. the adaptation is based on the log duration.
	 */
	public static String[] getBoxplotSizeAdaptationNomenclatures() {
		return Messages.getString(MessageIds.GDE_MSGT0803).split(GDE.STRING_CSV_SEPARATOR);
	}

	/**
	 * set the boxplot size adaptation level for the history
	 * @param scaleNomenclature
	 */
	public void setBoxplotSizeAdaptation(String scaleNomenclature) {
		this.setProperty(Settings.BOXPLOT_SIZE_ADAPTATION_ORDINAL, String.valueOf(Arrays.asList(getBoxplotSizeAdaptationNomenclatures()).indexOf(scaleNomenclature)));
	}

	/**
	 * @return the boxplot size adaptation level for the history (default is medium adaptation)
	 */
	public String getBoxplotSizeAdaptation() {
		return getBoxplotSizeAdaptationNomenclatures()[getBoxplotSizeAdaptationOrdinal()];
	}

	/**
	 * @return the ordinal of the boxplot size adaptation level for the history (default is medium adaptation)
	 */
	public int getBoxplotSizeAdaptationOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.BOXPLOT_SIZE_ADAPTATION_ORDINAL, String.valueOf(2)));
	}

	/**
	 * @return six spreading labels starting with 0 to 5 for the history x axis
	 */
	public static String[] getXAxisSpreadGradeNomenclatures() {
		return Messages.getString(MessageIds.GDE_MSGT0823).split(GDE.STRING_CSV_SEPARATOR);
	}

	/**
	 * set the extent of logarithmic spreading of the x axis distances between trails
	 * @param gradeText
	 */
	public void setXAxisSpreadGrade(String gradeText) {
		this.setProperty(Settings.X_SPREAD_GRADE_ORDINAL, String.valueOf(Arrays.asList(getXAxisSpreadGradeNomenclatures()).indexOf(gradeText)));
	}

	/**
	 * @return the extent of logarithmic spreading of the x axis distances between timesteps (default is grade 2 which is just before the middle of six grades)
	 */
	public String getXAxisSpreadGrade() {
		return getXAxisSpreadGradeNomenclatures()[getXAxisSpreadOrdinal()];
	}

	/**
	 * @return the ordinal of the extent of logarithmic spreading of the x axis distances between trails (default is grade 2 which is just before the middle of six grades)
	 */
	public int getXAxisSpreadOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.X_SPREAD_GRADE_ORDINAL, String.valueOf(2)));
	}

	/**
	 * set true if the history x axis distances between the timesteps are based on logarithmic values
	 * @param isActive
	 */
	public void setXAxisLogarithmicDistance(boolean isActive) {
		this.setProperty(Settings.IS_X_LOGARITHMIC_DISTANCE, String.valueOf(isActive));
	}

	/**
	 * @return true if the history x axis distances between the timesteps are based on logarithmic values
	 */
	public boolean isXAxisLogarithmicDistance() {
		return Boolean.valueOf(this.getProperty(Settings.IS_X_LOGARITHMIC_DISTANCE, "false")); //$NON-NLS-1$
	}

	/**
	 * set true if the history x axis starts with the most recent timesteps
	 * @param isActive
	 */
	public void setXAxisReversed(boolean isActive) {
		this.setProperty(Settings.IS_X_REVERSED, String.valueOf(isActive));
	}

	/**
	 * @return true if the history x axis starts with the most recent timesteps
	 */
	public boolean isXAxisReversed() {
		return Boolean.valueOf(this.getProperty(Settings.IS_X_REVERSED, "true")); //$NON-NLS-1$
	}

	/**
	 * @param isActive true if import files from the data directory are read for the history
	 */
	public void setSearchDataPathImports(boolean isActive) {
		this.setProperty(Settings.SEARCH_DATAPATH_IMPORTS, String.valueOf(isActive));
	}

	/**
	 * @return true if import files from the data directory are read for the history
	 */
	public boolean getSearchDataPathImports() {
		return Boolean.valueOf(this.getProperty(Settings.SEARCH_DATAPATH_IMPORTS, String.valueOf(true)));
	}

	/**
	 * @param isActive true if channels with identical measurements are selected for the history
	 */
	public void setChannelMix(boolean isActive) {
		this.setProperty(Settings.IS_CHANNEL_MIX, String.valueOf(isActive));
	}

	/**
	 * @return true true if channels with identical measurements are selected for the history
	 */
	public boolean isChannelMix() {
		return Boolean.valueOf(this.getProperty(Settings.IS_CHANNEL_MIX, "true")); //$NON-NLS-1$
	}

	/**
	 * @return sampling timespan values in seconds with seven values ranging from 0.001 to 10.0 based on the current locale
	 */
	public static String[] getSamplingTimespanValues() {
		String[] textValues = new String[7];
		for (int i = 0; i < SAMPLING_TIMESPANS.length; i++) {
			textValues[i] = String.valueOf(SAMPLING_TIMESPANS[i]);
		}
		return textValues;
	}

	/**
	 * set the sampling time which defines the timespan for one single sample value for the history
	 * @param valueText
	 */
	public void setSamplingTimespan_ms(String valueText) {
		this.setProperty(Settings.SAMPLING_TIMESPAN_ORDINAL, String.valueOf(Arrays.asList(getSamplingTimespanValues()).indexOf(valueText)));
	}

	/**
	 * repairs the properties file setting if it holds an invalid index value.
	 * @return the sampling time which defines the timespan for one single sample value for the history (default is 1 sec)
	 */
	public int getSamplingTimespan_ms() {
		double timespan_sec;
		try {
			timespan_sec = SAMPLING_TIMESPANS[getSamplingTimespanOrdinal()];
		}
		catch (Exception e) {
			setSamplingTimespan_ms(Double.toString(1.)); // one second
			timespan_sec = SAMPLING_TIMESPANS[getSamplingTimespanOrdinal()];
		}
		return (int) (timespan_sec * 1000.);
	}

	/**
	 * @return the ordinal of the sampling time which defines the timespan for one single sample value for the history (default is 1 ms which is the value at index 4)
	 */
	public int getSamplingTimespanOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.SAMPLING_TIMESPAN_ORDINAL, String.valueOf(2)));
	}

	/**
	 * @param value true if the history should select files in the object directory which hold a different or empty object key internally
	 */
	public void setIgnoreLogObjectKey(boolean value) {
		this.setProperty(Settings.IGNORE_LOG_OBJECT_KEY, String.valueOf(value));
	}

	/**
	 * @return true if the history should select files in the object directory which hold a different or empty object key internally
	 */
	public boolean getIgnoreLogObjectKey() {
		return Boolean.valueOf(this.getProperty(Settings.IGNORE_LOG_OBJECT_KEY, "true")); //$NON-NLS-1$
	}

	/**
	 * @return the maximum number of full calendar months which is used for history log selection (default is 12)
	 */
	public int getRetrospectMonths() {
		return Integer.valueOf(this.getProperty(Settings.RETROSPECT_MONTHS, String.valueOf(12)));
	}

	/**
	 * @param uintValue the maximum number of full calendar months which is used for history log selection
	 */
	public void setRetrospectMonths(String uintValue) {
		try {
			int value = Integer.parseUnsignedInt(uintValue.trim());
			if (value < 1 || value > 240) value = 12;
			this.setProperty(Settings.RETROSPECT_MONTHS, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * @return true if the history cache directories are zip files (performs better for more than about 100 directory entries)
	 */
	public boolean isZippedCache() {
		return Boolean.valueOf(this.getProperty(Settings.IS_ZIPPED_CACHE, "false")); //$NON-NLS-1$
	}

	/**
	 * @param value true if the history cache directories are zip files (performs better for more than about 100 directory entries)
	 */
	public void setZippedCache(boolean value) {
		this.setProperty(Settings.IS_ZIPPED_CACHE, String.valueOf(value));
	}

	/**
	 * @return false if the history cache vault files are JSON files
	 */
	public boolean isXmlCache() {
		return Boolean.valueOf(this.getProperty(Settings.IS_XML_CACHE, "false")); //$NON-NLS-1$
	}

	/**
	 * @param value false if the history cache vault files are JSON files
	 */
	public void setXmlCache(boolean value) {
		this.setProperty(Settings.IS_XML_CACHE, String.valueOf(value));
	}

	/**
	 * @return the minmax distance value used in quantile calculations for settlements based on transitions
	 */
	public double getMinmaxQuantileDistance() {
		return Double.valueOf(this.getProperty(Settings.MINMAX_QUANTILE_DISTANCE, ".1")); //$NON-NLS-1$
	}

	/**
	 * @param doubleValue is the minmax distance value used in quantile calculations for settlements based on transitions
	 */
	public void setMinMaxQuantileDistance(String doubleValue) {
		try {
			double value = Double.parseDouble(doubleValue.trim());
			if (value > 1 || value < 0) value = .1;
			this.setProperty(Settings.MINMAX_QUANTILE_DISTANCE, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * small values select transitions with a smaller amplitude. values close to 1 only select peaks with an amplitude close to the min-max distance.
	 * @return the factor for the calculation of the minimum absolute transition level required for firing the trigger
	 */
	public double getAbsoluteTransitionLevel() {
		return Double.valueOf(this.getProperty(Settings.ABSOLUTE_TRANSITION_LEVEL, ".5")); //$NON-NLS-1$
	}

	/**
	 * small values select peaks with a smaller amplitude. values close to 1 only select peaks with an amplitude close to the min-max distance.
	 * @param doubleValue is the factor for the calculation of the minimum absolute transition level required for firing the trigger
	 */
	public void setAbsoluteTransitionLevel(String doubleValue) {
		try {
			double value = Double.parseDouble(doubleValue.trim());
			if (value >= 1 || value <= 0) value = .5;
			this.setProperty(Settings.ABSOLUTE_TRANSITION_LEVEL, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * @param isUtc true if the date and time values output are formatted in UTC
	 */
	public void setDateTimeUtc(boolean isUtc) {
		this.setProperty(Settings.IS_DATETIME_UTC, String.valueOf(isUtc));
	}

	/**
	 * @return true if the date and time values output are formatted in UTC
	 */
	public boolean isDateTimeUtc() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DATETIME_UTC, "false")); //$NON-NLS-1$
	}

	/**
	 * @param isDisplayActive true if the log properties are displayed in the histo table tab
	 */
	public void setDisplayTags(boolean isDisplayActive) {
		this.setProperty(Settings.IS_DISPLAY_TAGS, String.valueOf(isDisplayActive));
	}

	/**
	 * @return true if the log properties are displayed in the histo table tab
	 */
	public boolean isDisplayTags() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DISPLAY_TAGS, "true")); //$NON-NLS-1$
	}

	/**
	 * @param isDisplayActive true if the history scores are displayed in the histo tabs
	 */
	public void setDisplayScores(boolean isDisplayActive) {
		this.setProperty(Settings.IS_DISPLAY_SCORES, String.valueOf(isDisplayActive));
	}

	/**
	 * @return true if the history scores are displayed in the histo tabs
	 */
	public boolean isDisplayScores() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DISPLAY_SCORES, "true")); //$NON-NLS-1$
	}

	/**
	 * @param isDisplayActive true if the history settlements are displayed in the histo tabs
	 */
	public void setDisplaySettlements(boolean isDisplayActive) {
		this.setProperty(Settings.IS_DISPLAY_SETTLEMENTS, String.valueOf(isDisplayActive));
	}

	/**
	 * @return true if the history settlements are displayed in the histo tabs
	 */
	public boolean isDisplaySettlements() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DISPLAY_SETTLEMENTS, "false")); //$NON-NLS-1$
	}

	/**
	 * @param isSuppressMode true if ignoring recordsets in the history is active
	 */
	public void setSuppressMode(boolean isSuppressMode) {
		this.setProperty(Settings.IS_SUPPRESS_MODE, String.valueOf(isSuppressMode));
	}

	/**
	 * @return true if ignoring recordsets in the history is active
	 */
	public boolean isSuppressMode() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SUPPRESS_MODE, "false")); //$NON-NLS-1$
	}

	/**
	 * @param isCurveSurvey true extended curve delta measuring display
	 */
	public void setCurveSurvey(boolean isCurveSurvey) {
		this.setProperty(Settings.IS_CURVE_SURVEY, String.valueOf(isCurveSurvey));
	}

	/**
	 * @return true for extended curve delta measuring display
	 */
	public boolean isCurveSurvey() {
		return Boolean.valueOf(this.getProperty(Settings.IS_CURVE_SURVEY, "false")); //$NON-NLS-1$
	}

	/**
	 * @return the radius in km for GPS coordinates assignment to the same cluster (default 0.5 km)
	 */
	public double getGpsLocationRadius() {
		return Double.valueOf(this.getProperty(Settings.GPS_LOCATION_RADIUS, ".5")); //$NON-NLS-1$
	}

	/**
	 * @param doubleValue the radius in km for GPS coordinates assignment to the same cluster (default 0.5 km)
	 */
	public void setGpsLocationRadius(double doubleValue) {
		this.setProperty(Settings.GPS_LOCATION_RADIUS, String.valueOf(doubleValue));
	}

	/**
	 * @return the maximum number of subdirectories which is used for history log selection (default is 0)
	 */
	public int getSubDirectoryLevelMax() {
		return Integer.valueOf(this.getProperty(Settings.SUBDIRECTORY_LEVEL_MAX, String.valueOf(0)));
	}

	/**
	 * @param uintValue the maximum number of subdirectories which is used for history log selection
	 */
	public void setSubDirectoryLevelMax(String uintValue) {
		try {
			int value = Integer.parseUnsignedInt(uintValue.trim());
			if (value < 0 || value > 9) value = 0;
			this.setProperty(Settings.SUBDIRECTORY_LEVEL_MAX, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * @param enabled true adds a transition column to the data table
	 */
	public void setDataTableTransitions(boolean enabled) {
		this.setProperty(Settings.IS_DATA_TABLE_TRANSITIONS, String.valueOf(enabled));
	}

	/**
	 * @return true adds a transition column to the data table
	 */
	public boolean isDataTableTransitions() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DATA_TABLE_TRANSITIONS, "false")); //$NON-NLS-1$
	}

	/**
	 * @param enabled true if after loading a file the first recordset is shown
	 */
	public void setFirstRecordSetChoice(boolean enabled) {
		this.setProperty(Settings.IS_FIRST_RECORDSET_CHOICE, String.valueOf(enabled));
	}

	/**
	 * @return true if after loading a file the first recordset is shown
	 */
	public boolean isFirstRecordSetChoice() {
		return Boolean.valueOf(this.getProperty(Settings.IS_FIRST_RECORDSET_CHOICE, "true")); //$NON-NLS-1$
	}

	/**
	 * @param csvValues holds the list with the permitted numbers of logs for reminder analysis
	 */
	public void setReminderCountCsv(String csvValues) {
		this.setProperty(Settings.REMINDER_COUNT_CSV, String.valueOf(csvValues));
	}

	/**
	 * @return the list with the permitted numbers of logs for reminder analysis
	 */
	public String getReminderCountCsv() {
		String list = String.valueOf(this.getProperty(Settings.REMINDER_COUNT_CSV, String.join(GDE.STRING_CSV_SEPARATOR, REMINDER_COUNT_VALUES)));
		return list;
	}

	private int[] getReminderCountValues() {
		Stream<String> items = Arrays.stream(getReminderCountCsv().split(GDE.STRING_CSV_SEPARATOR));
		if (items.count() != REMINDER_COUNT_VALUES.length) {
			setReminderCountCsv(String.join(GDE.STRING_CSV_SEPARATOR, Arrays.asList(REMINDER_COUNT_VALUES)));
		}
		return Arrays.stream(getReminderCountCsv().split(GDE.STRING_CSV_SEPARATOR)).mapToInt(Integer::valueOf).toArray();
	}

	/**
	 * @return the index number set by the user which is used for history log selection (default is 0)
	 */
	public int getReminderCountIndex() {
		return Integer.valueOf(this.getProperty(Settings.REMINDER_COUNT_INDEX, String.valueOf(0)));
	}

	/**
	 * @return the number of logs which is analyzed for reminders
	 */
	public int getReminderCount() {
		return getReminderCount(getReminderCountIndex());
	}

	/**
	 * @return the number of most recent logs which is analyzed for reminders
	 */
	public int getReminderCount(int index) {
		int[] reminderCountValues = getReminderCountValues();
		int realIndex = index >= 0 && index < reminderCountValues.length ? index : 1;
		return Integer.valueOf(reminderCountValues[realIndex]);
	}

	/**
	 * @param uintValue the number of most recent logs which is analyzed for reminders
	 */
	public void setReminderCountIndex(String uintValue) {
		this.setProperty(Settings.REMINDER_COUNT_INDEX, String.valueOf(uintValue));
	}

	/**
	 * @return the reminder level set by the user (default 0 is far reminders only -> red; -1 is no reminder)
	 */
	public int getReminderLevel() {
		return Integer.valueOf(this.getProperty(Settings.REMINDER_LEVEL, String.valueOf(0)));
	}

	/**
	 * @param uintValue the reminder level set by the user (0 is far reminders only -> red; -1 is no reminder)
	 */
	public void setReminderLevel(String uintValue) {
		this.setProperty(Settings.REMINDER_LEVEL, String.valueOf(uintValue));
	}

	/**
	 * @param enabled true if all quantile calculations base on a uniform interquantile range without zero detection
	 */
	public void setCanonicalQuantiles(boolean enabled) {
		this.setProperty(Settings.IS_CANONICAL_QUANTILES, String.valueOf(enabled));
	}

	/**
	 * @return true if all quantile calculations base on a uniform interquantile range without zero detection
	 */
	public boolean isCanonicalQuantiles() {
		return Boolean.valueOf(this.getProperty(Settings.IS_CANONICAL_QUANTILES, "false")); //$NON-NLS-1$
	}

	/**
	 * @param enabled true if the lower and upper quantile tolerance is equal and forms a uniform interquantile range
	 */
	public void setSymmetricToleranceInterval(boolean enabled) {
		this.setProperty(Settings.IS_SYMMETRIC_TOLERANCE_INTERVAL, String.valueOf(enabled));
	}

	/**
	 * @return true if the lower and upper quantile tolerance is equal and forms a uniform interquantile range
	 */
	public boolean isSymmetricToleranceInterval() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SYMMETRIC_TOLERANCE_INTERVAL, "false")); //$NON-NLS-1$
	}

	/**
	 * @return the spread factor for vault outliers based on the tolerance interval (-> increase for less outliers)
	 */
	public int getOutlierToleranceSpread() {
		return Integer.valueOf(this.getProperty(Settings.OUTLIER_TOLERANCE_SPREAD, String.valueOf(99999)));
	}

	/**
	 * @param uintValue the spread factor for vault outliers based on the tolerance interval (default 9 -> increase for less outliers)
	 */
	public void setOutlierToleranceSpread(String uintValue) {
		this.setProperty(Settings.OUTLIER_TOLERANCE_SPREAD, String.valueOf(uintValue));
	}

	/**
	 * @return the spread factor based on the tolerance interval (default 9 -> increase for showing more outliers)
	 */
	public int getSummaryScaleSpread() {
		return Integer.valueOf(this.getProperty(Settings.SUMMARY_SCALE_SPREAD, String.valueOf(9)));
	}

	/**
	 * @param uintValue the spread factor based on the tolerance interval (default 9 -> increase for showing more outliers)
	 */
	public void setSummaryScaleSpread(String uintValue) {
		this.setProperty(Settings.SUMMARY_SCALE_SPREAD, String.valueOf(uintValue));
	}

	/**
	 * @param enabled true if the summary graphics shows the boxplot
	 */
	public void setSummaryBoxVisible(boolean enabled) {
		this.setProperty(Settings.IS_SUMMARY_BOX_VISIBLE, String.valueOf(enabled));
	}

	/**
	 * @return true if the summary graphics shows the boxplot
	 */
	public boolean isSummaryBoxVisible() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SUMMARY_BOX_VISIBLE, "true")); //$NON-NLS-1$
	}

	/**
	 * @param enabled true if the summary graphics shows the distribution ranges M +- 2 SD
	 */
	public void setSummarySpreadVisible(boolean enabled) {
		this.setProperty(Settings.IS_SUMMARY_SPREAD_VISIBLE, String.valueOf(enabled));
	}

	/**
	 * @return true if the summary graphics shows the distribution ranges M +- 2 SD
	 */
	public boolean isSummarySpreadVisible() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SUMMARY_SPREAD_VISIBLE, "false")); //$NON-NLS-1$
	}

	/**
	 * @param enabled true if the summary graphics show all spots; false shows the most recent spots
	 */
	public void setSummarySpotsVisible(boolean enabled) {
		this.setProperty(Settings.IS_SUMMARY_SPOTS_VISIBLE, String.valueOf(enabled));
	}

	/**
	 * @return true if the summary graphics shows all spots; false shows the most recent spots
	 */
	public boolean isSummarySpotsVisible() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SUMMARY_SPOTS_VISIBLE, "true")); //$NON-NLS-1$
	}

	/**
	 * @param enabled true if specific templates are available for objects
	 */
	public void setObjectTemplatesActive(boolean enabled) {
		this.setProperty(Settings.IS_OBJECT_TEMPLATES_ACTIVE, String.valueOf(enabled));
	}

	/**
	 * @return true if specific templates are available for objects
	 */
	public boolean isObjectTemplatesActive() {
		return Boolean.valueOf(this.getProperty(Settings.IS_OBJECT_TEMPLATES_ACTIVE, "true")); //$NON-NLS-1$
	}

	/**
	 * @param enabled true if MAC system support touchbar
	 */
	public void setMacTouchbarEnabled(boolean enabled) {
		this.setProperty(Settings.IS_MAC_TOUCHBAR, String.valueOf(enabled));
	}

	/**
	 * @return true if specific templates are available for objects
	 */
	public boolean isMacTouchbarEnabled() {
		return Boolean.valueOf(this.getProperty(Settings.IS_MAC_TOUCHBAR, "false")); //$NON-NLS-1$
	}

	/**
	 * @param csvValues holds the list of folder paths
	 */
	public void setDataFoldersCsv(String csvValues) {
		this.setProperty(Settings.DATA_FOLDERS_CSV, String.valueOf(csvValues));
	}

	/**
	 * @return the list of folder paths
	 */
	public String getDataFoldersCsv() {
		String list = String.valueOf(this.getProperty(Settings.DATA_FOLDERS_CSV, ""));
		return list;
	}

	/**
	 * @param csvValues holds the list of folder paths
	 */
	public void setImportFoldersCsv(String csvValues) {
		this.setProperty(Settings.IMPORT_FOLDERS_CSV, String.valueOf(csvValues));
	}

	/**
	 * @return the list of folder paths
	 */
	public String getImportFoldersCsv() {
		String list = String.valueOf(this.getProperty(Settings.IMPORT_FOLDERS_CSV, ""));
		return list;
	}

	/**
	 * @param csvValues holds the list of folder paths for object folder file mirroring
	 */
	public void setMirrorSourceFoldersCsv(String csvValues) {
		this.setProperty(Settings.MIRROR_SOURCE_FOLDERS_CSV, String.valueOf(csvValues));
	}

	/**
	 * @return the list of folder paths for object folder file mirroring
	 */
	public String getMirrorSourceFoldersCsv() {
		String list = String.valueOf(this.getProperty(Settings.MIRROR_SOURCE_FOLDERS_CSV, ""));
		return list;
	}

	/**
	 * @param enabled true if the histo source file paths are monitored by a file watcher
	 */
	public void setSourceFileListenerActive(boolean enabled) {
		this.setProperty(Settings.IS_SOURCE_FILE_LISTENER_ACTIVE, String.valueOf(enabled));
	}

	/**
	 * @return true if the histo source file paths are monitored by a file watcher
	 */
	public boolean isSourceFileListenerActive() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SOURCE_FILE_LISTENER_ACTIVE, "false"));
	}

	/**
	 * @param enabled true if a log file directory is checked for new object key candidate
	 */
	public void setObjectQueryActive(boolean enabled) {
		this.setProperty(Settings.IS_OBJECT_QUERY_ACTIVE, String.valueOf(enabled));
	}

	/**
	 * @return true if a log file directory is checked for new object key candidate
	 */
	public boolean isObjectQueryActive() {
		return Boolean.valueOf(this.getProperty(Settings.IS_OBJECT_QUERY_ACTIVE, "false"));
	}

	/**
	 * @return the list of deviceName*lastUsedChannelOrdinal from the user choices (the first entry is the most recently used entry)
	 */
	public String getDeviceUseCsv() {
		return String.valueOf(this.getProperty(Settings.DEVICE_USE, ""));
	}

	/**
	 * @param csvValues is the list of deviceName*lastUsedChannelOrdinal from the user choices (the first entry is the most recently used entry)
	 */
	public void setDeviceUseCsv(String csvValues) {
		log.log(Level.FINE, csvValues);
		this.setProperty(Settings.DEVICE_USE, String.valueOf(csvValues));
	}

	/**
	 * (Re-)define the device's used channel ordinal.
	 * @param deviceName
	 * @param channelNumber
	 */
	public void addDeviceUse(String deviceName, int channelNumber) {
		String entry = deviceName + GDE.STRING_STAR + (channelNumber - 1);
		Stream<String> remainingEntries = Arrays.stream(this.getDeviceUseCsv().split(GDE.STRING_CSV_SEPARATOR)) //
				.filter(s -> !s.startsWith(deviceName + GDE.STRING_STAR)).filter(s -> !s.isEmpty());
		this.setDeviceUseCsv(Stream.concat(Stream.of(entry), remainingEntries).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR)));
	}

	/**
	 * (Re-)define the device's used removing deviceName
	 * @param deviceName
	 */
	public void removeDeviceUse(String deviceName) {
		Stream<String> remainingEntries = Arrays.stream(this.getDeviceUseCsv().split(GDE.STRING_CSV_SEPARATOR)) //
				.filter(s -> !s.startsWith(deviceName + GDE.STRING_STAR));
		this.setDeviceUseCsv(remainingEntries.collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR)));
	}

	/**
	 * @return the last used channel number
	 */
	public int getLastUseChannelNumber(String deviceName) {
		Optional<String> lastUseEntry = Arrays.stream(this.getDeviceUseCsv().split(GDE.STRING_CSV_SEPARATOR)) //
				.filter(s -> s.startsWith(deviceName + GDE.STRING_STAR)).findFirst();
		return lastUseEntry.map(s -> s.substring((deviceName + GDE.STRING_STAR).length())).map(Integer::parseInt).orElse(0) + 1;
	}

	/**
	 * get color skin schema
	 */
	public String getSkinColorSchema() {
		return this.getProperty(Settings.SKIN_COLOR_SCHEMA, Settings.COLOR_SCHEMA_SYSTEM);
	}

	/**
	 * set color skin schema
	 */
	public void setSkinColorSchema(String colorSchema) {
		this.setProperty(Settings.SKIN_COLOR_SCHEMA, colorSchema);
		DataExplorer.getInstance().setColorSchemaColors(this.getSkinColorSchema());
	}

	/**
	 * @return the color schema type name, actually distinguished between 'light' and 'dark'
	 */
	public String getColorSchemaType() {
		return DataExplorer.getInstance().COLOR_BACKGROUND.getRed() + DataExplorer.getInstance().COLOR_BACKGROUND.getGreen() + DataExplorer.getInstance().COLOR_BACKGROUND.getBlue() > 500 ? "light/" : "dark/";
	}

	public boolean isRXTXcommToBeUsed() {
		return Boolean.valueOf(this.getProperty(Settings.IS_RXTX_COMM_TO_BE_USED, "false"));
	}
	
	public boolean isStartDeviceCommunicationAfterStartup() {
		return Boolean.valueOf(this.getProperty(Settings.IS_START_DEV_COMM_AFTER_START, "false"));
	}
	
	public void setStartDeviceCommunicationAfterStartup(boolean enabled) {
		this.setProperty(Settings.IS_START_DEV_COMM_AFTER_START, String.valueOf(enabled));
	}
	
	public int getApplVersionNumber() {
		return GDE.VERSION_NUMBER;	
	}
	
	/**
	 * check updated version number and entries in serviice update list
	 * @return true if possible device XML update required
	 */
	public boolean isDeviceXmlUpdateRequired() {
		return Integer.valueOf(this.getProperty(Settings.APPL_VERSION_NUMBER, "383").replace(".", "")) < GDE.VERSION_NUMBER && !getServiceUpdateList().isEmpty();
	}
	
	/**
	 * maintain this list while updating version number with device XML files changed without XSD update 
	 * @return list of potential to be updated device XML
	 */
	public List<String> getServiceUpdateList() {
		List<String> deviceList = new ArrayList<>();
		deviceList.add("HoTTAdapter");
		deviceList.add("HoTTAdapterM");
		deviceList.add("HoTTAdapter2");
		deviceList.add("HoTTAdapter2M");
		return deviceList;
	}
}
