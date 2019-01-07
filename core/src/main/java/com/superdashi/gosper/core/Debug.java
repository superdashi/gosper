/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
