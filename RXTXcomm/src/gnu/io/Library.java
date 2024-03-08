package gnu.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Library {

	static final String	SEPARATOR;

	static {
		SEPARATOR = System.getProperty("file.separator");
	}

	static boolean extract(String fileName, String mappedName) {
		FileOutputStream os = null;
		InputStream is = null;
		File file = new File(fileName);
		try {
			if (!file.exists()) {
				is = Library.class.getResourceAsStream("/src/" + mappedName); //$NON-NLS-1$
				if (is != null) {
					int read;
					byte[] buffer = new byte[4096];
					os = new FileOutputStream(fileName);
					while ((read = is.read(buffer)) != -1) {
						os.write(buffer, 0, read);
					}
					os.close();
					is.close();
					if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) { //$NON-NLS-1$
						try {
							Runtime.getRuntime().exec(new String[] { "chmod", "755", fileName }).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
						}
						catch (Throwable e) {
							//ignore
						}
					}
					if (load(fileName)) return true;
				}
			}
		}
		catch (Throwable e) {
			try {
				if (os != null) os.close();
			}
			catch (IOException e1) {
				//ignore
			}
			try {
				if (is != null) is.close();
			}
			catch (IOException e1) {
				//ignore
			}
		}
		if (file.exists()) file.delete();
		return false;
	}

	static boolean load(String libName) {
		try {
			if (libName.indexOf(SEPARATOR) != -1) {
				System.load(libName);
			}
			else {
				System.loadLibrary(libName);
			}
			return true;
		}
		catch (UnsatisfiedLinkError e) {
			//ignore
		}
		return false;
	}

	/**
	 * Loads the shared library that matches the version of the
	 * Java code which is currently running.  SWT shared libraries
	 * follow an encoding scheme where the major, minor and revision
	 * numbers are embedded in the library name and this along with
	 * <code>name</code> is used to load the library.  If this fails,
	 * <code>name</code> is used in another attempt to load the library,
	 * this time ignoring the SWT version encoding scheme.
	 *
	 * @param name the name of the library to load
	 */
	public static void loadLibrary(String name) {
		loadLibrary(name, true);
	}

	/**
	 * Loads the shared library that matches the version of the
	 * Java code which is currently running.  SWT shared libraries
	 * follow an encoding scheme where the major, minor and revision
	 * numbers are embedded in the library name and this along with
	 * <code>name</code> is used to load the library.  If this fails,
	 * <code>name</code> is used in another attempt to load the library,
	 * this time ignoring the SWT version encoding scheme.
	 *
	 * @param name the name of the library to load
	 * @param mapName true if the name should be mapped, false otherwise
	 */
	public static void loadLibrary(String name, boolean mapName) {
		String prop = System.getProperty("sun.arch.data.model"); //$NON-NLS-1$
		if (prop == null) prop = System.getProperty("com.ibm.vm.bitmode"); //$NON-NLS-1$

		/* Compute the library name and mapped name */
		String libName, mappedName;
		if (mapName) {
			libName = name;
			mappedName = System.mapLibraryName(libName);
		}
		else {
			libName = mappedName = name;
		}

		String osPath = System.getProperty("os.name").toLowerCase();
		if 			(osPath.startsWith("win")) osPath = "win" + prop + "/";
		else if (osPath.startsWith("lin")) osPath = "lnx" + prop + "/";
		else if (osPath.startsWith("mac")) osPath = "mac" + "/";

		/* Try loading library from java library path */
		//System.out.println("...loading library from library path : libname = " + libName);
		if (load(libName)) return;
		if (mapName && load(libName)) return;

		/* Try loading library from the user.dir directory which is the directory where application gets started */
		String path = System.getProperty("user.dir"); //$NON-NLS-1$
		path = new File(path).getAbsolutePath();
		//System.out.println("...loading library from java.io.tmpdir : libname = " + path + SEPARATOR + mappedName);
		if (load(path + SEPARATOR + mappedName)) return;
		
		/* Try loading library from the tmp directory */
		path = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
		path = new File(path).getAbsolutePath();
		//System.out.println("...loading library from java.io.tmpdir : libname = " + path + SEPARATOR + mappedName);
		if (load(path + SEPARATOR + mappedName)) return;

		/* Try extracting and loading library from jar */
		if (path != null) {
			//System.out.println("...extracting library to java.io.tmpdir : " + osPath + mappedName);
			if (extract(path + SEPARATOR + mappedName, osPath + mappedName)) return;
		}

		/* Failed to find the library */
		throw new UnsatisfiedLinkError("no " + libName + " in java.library.path or the jar file"); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
