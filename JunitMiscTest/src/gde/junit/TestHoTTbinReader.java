package gde.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import gde.GDE;
import gde.device.graupner.HoTTbinReader.SdLogFormat;
import gde.device.graupner.HoTTbinReader.SdLogInputStream;

public class TestHoTTbinReader {
	static Logger						log				= Logger.getLogger(TestHoTTbinReader.class.getName());

	final String						tmpDir		= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR) ? System.getProperty("java.io.tmpdir")
			: System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR;
	@Rule
	public TemporaryFolder	tmpFolder	= new TemporaryFolder();

	/**
	 * Compare the 'read + stripped write' sd log file approach with the SdLogInputStream approach.
	 */
	@Test
	public void testSdLogInputStream() {
		Path sdLogFilePath = getDataPath(Paths.get("HoTTAdapterX").resolve("0002.bin"));
		Path outputFile = Paths.get(tmpFolder.getRoot().getPath()).resolve("0002.bin");
		log.log(Level.OFF, "", sdLogFilePath);
		System.out.println("" + sdLogFilePath);

		long fileSize = sdLogFilePath.toFile().length() - 27 - 323; // - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize;

		try (InputStream data_in = new FileInputStream(sdLogFilePath.toString()); //
				OutputStream data_out = new FileOutputStream(outputFile.toFile());) {
			byte[] buffer = new byte[27];
			data_in.read(buffer);
			buffer = new byte[23];
			for (int i = 0; i < fileSize / buffer.length; i++) {
				if (buffer.length == data_in.read(buffer)) data_out.write(buffer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		SdLogFormat sdLogFormat = new SdLogFormat(27, 323, 23);
		try (SdLogInputStream data_in = new SdLogInputStream(new BufferedInputStream(new FileInputStream(sdLogFilePath.toString())),
				sdLogFilePath.toFile().length(), sdLogFormat); //
				InputStream data_out = new FileInputStream(outputFile.toFile());) {
			assertEquals("net file size (payload) differs", fileSize, data_in.getPayloadSize());
			int readOk = 0;
			byte[] buffer = new byte[23];
			byte[] bufferOut = new byte[23];
			for (int i = 0; i < data_in.getPayloadSize() / buffer.length; i++) {
				if (i == data_in.getPayloadSize() / buffer.length - 1) {
					readOk = data_in.read(buffer);
					data_out.read(bufferOut);
					assertTrue("last read buffer differs", Arrays.equals(buffer, bufferOut));
				} else {
					readOk = data_in.read(buffer);
					data_out.read(bufferOut);
					assertTrue("buffer differs @" + i, Arrays.equals(buffer, bufferOut));
				}
			}
			System.out.println("last read " + readOk + "   " + Arrays.toString(buffer));
			readOk = data_in.read(buffer);
			System.out.println("fail read " + readOk + "   " + Arrays.toString(buffer));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Path getDataPath(Path subPath) {
		String srcDataPath = getLoaderPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
		if (srcDataPath.endsWith("bin/")) { // running inside eclipse
			srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
		} else if (srcDataPath.indexOf("classes") > -1) { // ET running inside eclipse in Debug mode
			srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
		} else {
			srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf("build")) + "DataFilesTestSamples/" + GDE.NAME_LONG;
		}
		// return Paths.get(srcDataPath).resolve(subPath); Error because of leading slash:
		// /C:/Users/USER/git/dataexplorer/DataFilesTestSamples/DataExplorer // this.dataPath = Paths.get(srcDataPath).resolve(subPath).toFile();
		return (new File(srcDataPath)).toPath().resolve(subPath);
	}

	/**
	 * get the path where the class GDE gets loaded
	 *
	 * @return
	 */
	protected static String getLoaderPath() {
		return GDE.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

}
