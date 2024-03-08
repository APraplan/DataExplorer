/**
 * 
 */
package gde.exception;

/**
 * @author brueg
 *
 */
public class NotSupportedException extends Exception {
	static final long serialVersionUID = 26031957;
	/**
	 * 
	 */
	public NotSupportedException() {
		//ignore
	}

	/**
	 * @param arg0
	 */
	public NotSupportedException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public NotSupportedException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public NotSupportedException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
