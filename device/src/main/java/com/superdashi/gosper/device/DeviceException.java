package com.superdashi.gosper.device;

public class DeviceException extends RuntimeException {

	private static final long serialVersionUID = 5238965041760754190L;

	public DeviceException() { }

	public DeviceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DeviceException(String message, Throwable cause) {
		super(message, cause);
	}

	public DeviceException(String message) {
		super(message);
	}

	public DeviceException(Throwable cause) {
		super(cause);
	}

}
