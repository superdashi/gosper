package com.superdashi.gosper.core;

import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.logging.Logger.Logging;

// this is for logging that's only of interest to gosper developers and not application developers
// used by gosper classes that do not have their own logger
//TODO needs to be thread-local based - this is a hack ftm
public final class Debug {

	private static Logger logger;

	public static void logger(Logger logger) {
		assert logger != null;
		assert Debug.logger == null;
		Debug.logger = logger;
	}

	public static Logger logger() {
		assert logger != null;
		return logger;
	}

	public static Logging logging() {
		return logger.debug();
	}

}
