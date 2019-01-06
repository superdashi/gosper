package com.superdashi.gosper.micro;

public class ResourceException extends RuntimeException {

	private static final long serialVersionUID = 8182230150410421368L;

	public ResourceException() {
		super();
	}

	public ResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ResourceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceException(String message) {
		super(message);
	}

	public ResourceException(Throwable cause) {
		super(cause);
	}

}