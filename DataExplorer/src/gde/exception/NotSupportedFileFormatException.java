/**
 * 
 */
package gde.exception;

/**
 * @author brueg
 *
 */
public class NotSupportedFileFormatException extends Exception {
	static final long serialVersionUID = 26031957;
	/**
	 * 
	 */
	public NotSupportedFileFormatException() {
		//ignore
	}

	/**
	 * @param arg0
	 */
	public NotSupportedFileFormatException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public NotSupportedFileFormatException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public NotSupportedFileFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
