package com.superdashi.gosper.bundle;

public final class PrivilegeException extends RuntimeException {

	private static final long serialVersionUID = 7294252549644736933L;

	public PrivilegeException() {
		super();
	}

	public PrivilegeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PrivilegeException(String message, Throwable cause) {
		super(message, cause);
	}

	public PrivilegeException(String message) {
		super(message);
	}

	public PrivilegeException(Throwable cause) {
		super(cause);
	}

}
