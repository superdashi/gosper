package com.superdashi.gosper.core;

public class CacheException extends RuntimeException {

	private static final long serialVersionUID = 3362693882395696407L;

	CacheException() {
		super();
	}

	CacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	CacheException(String message, Throwable cause) {
		super(message, cause);
	}

	CacheException(String message) {
		super(message);
	}

	CacheException(Throwable cause) {
		super(cause);
	}

}
