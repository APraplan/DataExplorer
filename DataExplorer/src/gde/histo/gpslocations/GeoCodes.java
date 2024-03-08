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
package gde.histo.gpslocations;

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import gde.DataAccess;
import gde.DataAccess.LocalAccess;
import gde.GDE;
import gde.config.Settings;
import gde.histo.utils.GpsCoordinate;
import gde.log.Level;
import gde.log.Logger;

/**
 * Gives access to geocode data based on reverse GPS geocode APIs.
 * @author Thomas Eickert
 */
public final class GeoCodes {
	static final String	$CLASS_NAME	= GeoCodes.class.getName();
	static final Logger	log					= Logger.getLogger($CLASS_NAME);

	public enum GeoCodeGoogle {
		STREET_ADDRESS, ROUTE, POLITICAL, ADMINISTRATIVE_AREA_LEVEL_3, ADMINISTRATIVE_AREA_LEVEL_2;
		/** use this instead of values() to avoid repeatedly cloning actions. */
		public static final GeoCodeGoogle VALUES[] = values();
	};

	/**
	 * Create, read and delete geofiles.
	 * Support different types of files originating from OSM, Google.
	 * @author Thomas Eickert (USER)
	 */
	private static final class GeoFiles {

		private final DataAccess	dataAccess;
		private final double			gpsLocationRadius;

		public GeoFiles(DataAccess dataAccess, double locationRadius) {
			this.dataAccess = dataAccess;
			this.gpsLocationRadius = locationRadius;
		}

		/**
		 * @return the formatted address of the GPS location, e.g. 73441 Bopfingen, Germany
		 */
		String getLocation(String fileName) {
			String location = GDE.STRING_EMPTY;
			try (InputStream inputStream = dataAccess.getGeoCodeInputStream(fileName)) {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
				GeoCodeProvider geoCodeProvider = GeoCodeProvider.get(doc);
				XPath xpath = XPathFactory.newInstance().newXPath();
				if (geoCodeProvider.isResponseOk(doc, xpath)) {
					location = geoCodeProvider.getLocation(doc, xpath);
				}
			} catch (Exception e) {
				log.log(WARNING, e.getMessage());
			}
			return location;
		}

		/**
		 * @return the file closest to the GPS coordinate or an empty string
		 */
		String getGeoFileName(GpsCoordinate gpsCoordinate) throws FileNotFoundException {
			List<String> fileNames = dataAccess.getGeoCodeFolderList();
			log.finer(() -> String.format("%04d files found in locationsDir %s", fileNames.size(), Settings.GPS_LOCATIONS_DIR_NAME));

			final Comparator<String> distanceComparator = (String f1, String f2) -> Double.compare(new GpsCoordinate(
					f1).getDistance(gpsCoordinate), (new GpsCoordinate(f2).getDistance(gpsCoordinate)));
			final Predicate<String> minDistanceFilter = f -> new GpsCoordinate(f).getDistance(gpsCoordinate) < gpsLocationRadius;
			String closestFile = fileNames.parallelStream().filter(minDistanceFilter).min(distanceComparator).orElse(GDE.STRING_EMPTY);
			log.log(Level.FINER, "closestFile", closestFile);
			return closestFile;
		}

		/**
		 * Access the geocode providers and return the first result which was validated.
		 * @return the file name corresponding to the GPS coordinate - it does not exist in the file system in any case
		 */
		private String acquireValidatedGeoData(GpsCoordinate gpsCoordinate) {
			String geoFileName = getGeoCodeFileName(gpsCoordinate);
			for (GeoCodeProvider geoCodeProvider : GeoCodeProvider.RANKED_VALUES) {
				loadGeoData(gpsCoordinate, geoCodeProvider);
				if (dataAccess.existsGeoCodeFile(geoFileName)) {
					try (InputStream inputStream = dataAccess.getGeoCodeInputStream(geoFileName)) {
						Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
						XPath xpath = XPathFactory.newInstance().newXPath();
						if (geoCodeProvider.isResponseOk(doc, xpath) || geoCodeProvider.isResponseZero(doc, xpath)) {
							break;
						} else {
							dataAccess.deleteGeoCodeFile(geoFileName);
							log.log(FINER, "Empty file deleted", geoFileName);
						}
					} catch (Exception e) {
						dataAccess.deleteGeoCodeFile(geoFileName);
						log.log(WARNING, "File deleted after IO / parser exception", e.getMessage());
					}
				}
			}
			return geoFileName;
		}

		/**
		 * Read the file from the internet and store in the geocodes cache.
		 */
		private void loadGeoData(GpsCoordinate gpsCoordinate, GeoCodeProvider geoCodeProvider) {
			if (GDE.EXECUTION_ENV != null) return;

			long milliTime = System.currentTimeMillis();
			String geoCodeFileName = getGeoCodeFileName(gpsCoordinate);
			((LocalAccess) dataAccess).checkAndCreateHistoLocations();
			try {
				URL requestUrl = geoCodeProvider.getRequestUrl(gpsCoordinate);
				try (InputStream inputStream = ((LocalAccess) dataAccess).getHttpsInputStream(requestUrl);
						ReadableByteChannel readableByteChannel = java.nio.channels.Channels.newChannel(inputStream);
						FileOutputStream fileOutputStream = ((LocalAccess) dataAccess).getGeoCodeOutputStream(geoCodeFileName.toString())) {
					fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, 1 << 20); // limit 1 MB
					log.time(() -> "http read in " + (System.currentTimeMillis() - milliTime) + " ms!  gpsCoordinate=" + gpsCoordinate.toCsvString());
				} catch (Exception e1) {
					log.log(WARNING, "saving geocodes from internet connection failed", e1.getMessage());
				}
			} catch (Exception e) {
				log.log(WARNING, "internet connection failed, check network capability if location data required", e.getMessage());
			}
		}

		/**
		 * @return the geocode cache file path
		 */
		private static String getGeoCodeFileName(GpsCoordinate gpsCoordinate) {
			return gpsCoordinate.toAngularCoordinate();
		}

	}

	/**
	 * @return an empty string or the formatted address of the GPS location, e.g. 73441 Bopfingen, Germany
	 */
	public static String getOrAcquireLocation(GpsCoordinate gpsCoordinate, double locationRadius, DataAccess dataAccess) {
		if (GDE.EXECUTION_ENV != null) return GDE.STRING_EMPTY;

		String location = GDE.STRING_EMPTY;
		((LocalAccess) dataAccess).checkAndCreateHistoLocations();
		GeoFiles geoFiles = new GeoFiles(dataAccess, locationRadius);
		try {
			String geoFileName = geoFiles.getGeoFileName(gpsCoordinate);
			if (dataAccess.existsGeoCodeFile(geoFileName)) {
				location = geoFiles.getLocation(geoFileName);
			} else {
				geoFileName = geoFiles.acquireValidatedGeoData(gpsCoordinate);
				if (dataAccess.existsGeoCodeFile(geoFileName)) {
					location = geoFiles.getLocation(geoFileName);
				} else {
					log.log(WARNING, "geoCode file not found");
				}
			}
		} catch (FileNotFoundException e) {
			log.log(WARNING, e.getMessage(), e);
		}
		return location;
	}

	public enum GeoCodeProvider {
		GOOGLE_API(1, "GeocodeResponse") {
			@Override
			URL getRequestUrl(GpsCoordinate gpsCoordinate) {
				try {
					String url = String.format(Locale.US, "https://maps.googleapis.com/maps/api/geocode/xml?latlng=%f,%f", //
							gpsCoordinate.getLatitude(), gpsCoordinate.getLongitude());
					return new URL(url);
				} catch (Exception e) {
					throw new RuntimeException("Google malformed URL", e);
				}
			}

			@Override
			boolean isResponseOk(Document doc, XPath xpath) throws XPathExpressionException {
				String status = xpath.evaluate("/GeocodeResponse/status", doc, XPathConstants.STRING).toString();
				// "OVER_QUERY_LIMIT","REQUEST_DENIED","INVALID_REQUEST","UNKNOWN_ERROR"
				return "OK".equals(status);
			}

			@Override
			boolean isResponseZero(Document doc, XPath xpath) throws XPathExpressionException {
				String status = xpath.evaluate("/GeocodeResponse/status", doc, XPathConstants.STRING).toString();
				return "ZERO_RESULTS".equals(status);
			}

			@Override
			String getLocation(Document doc, XPath xpath) throws XPathExpressionException {
				String location;
				// XPathExpression expr = xpath.compile("/GeocodeResponse/result[3]/formatted_address"); // 1-based
				XPathExpression expr = xpath.compile("/GeocodeResponse/result[type/text()='" + GeoCodeGoogle.STREET_ADDRESS.name().toLowerCase() + "']/formatted_address"); //$NON-NLS-1$ //$NON-NLS-2$
				location = expr.evaluate(doc, XPathConstants.STRING).toString();
				log.log(FINER, "1st try formatted_address=", location);

				if (location.isEmpty()) {
					NodeList nodes = (NodeList) xpath.evaluate("/GeocodeResponse/result/formatted_address", doc, XPathConstants.NODESET);
					for (int i = 0; i < nodes.getLength(); i++) {
						location = nodes.item(i).getTextContent();
						if (!location.isEmpty()) {
							if (log.isLoggable(FINER)) log.log(FINER, "loop formatted_address=", nodes.item(i).getTextContent());
							break;
						}
					}
				}
				return location.replace("Unnamed Road, ", "");
			}
		},

		OSM_NOMINATIM(0, "reversegeocode") {
			@Override
			URL getRequestUrl(GpsCoordinate gpsCoordinate) {
				try {
					String url = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=xml&lat=%f&lon=%f&zoom=18&addressdetails=1", //
							gpsCoordinate.getLatitude(), gpsCoordinate.getLongitude());
					log.log(Level.FINER, "OSM", url);
					return new URL(url);
				} catch (Exception e) {
					throw new RuntimeException("OSM malformed URL", e);
				}
			}

			@Override
			boolean isResponseOk(Document doc, XPath xpath) throws XPathExpressionException {
				String location = xpath.evaluate("/reversegeocode/result", doc, XPathConstants.STRING).toString();
				return !location.isEmpty();
			}

			@Override
			boolean isResponseZero(Document doc, XPath xpath) throws XPathExpressionException {
				String status = xpath.evaluate("/reversegeocode/error", doc, XPathConstants.STRING).toString();
				return "Unable to geocode".equals(status);
			}

			@Override
			String getLocation(Document doc, XPath xpath) throws XPathExpressionException {
				String location = xpath.evaluate("/reversegeocode/result", doc, XPathConstants.STRING).toString();
				log.log(FINER, "unmodified address=", location);
				return location;
			}
		};

		static GeoCodeProvider get(Document doc) {
			String nodeName = doc.getDocumentElement().getNodeName();
			if (nodeName.equals(GeoCodeProvider.OSM_NOMINATIM.rootName)) {
				return GeoCodeProvider.OSM_NOMINATIM;
			} else if (nodeName.equals(GeoCodeProvider.GOOGLE_API.rootName)) {
				return GeoCodeProvider.GOOGLE_API;
			} else {
				throw new UnsupportedOperationException();
			}
		}

		static final GeoCodeProvider	VALUES[]				= values();
		static final GeoCodeProvider	RANKED_VALUES[]	= Arrays.stream(values())											//
				.sorted((p1, p2) -> Integer.compare(p1.rank, p2.rank)).toArray(GeoCodeProvider[]::new);

		/**
		 * Search priority (zero is the highest rank)
		 */
		private int										rank;
		private String								rootName;

		private GeoCodeProvider(int rank, String rootName) {
			this.rank = rank;
			this.rootName = rootName;
		}

		abstract URL getRequestUrl(GpsCoordinate gpsCoordinate);

		abstract boolean isResponseOk(Document doc, XPath xpath) throws XPathExpressionException;

		abstract boolean isResponseZero(Document doc, XPath xpath) throws XPathExpressionException;

		abstract String getLocation(Document doc, XPath xpath) throws XPathExpressionException;
	}
}
