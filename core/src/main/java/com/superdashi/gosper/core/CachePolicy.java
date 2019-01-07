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

import java.nio.file.Path;

public interface CachePolicy {

	public static CachePolicy TRIVIAL = new CachePolicy() {
		@Override public boolean isMemoryUsed() { return false; }
		@Override public boolean isDiskUsed() { return false; }
		@Override public Path getDiskPath() { return null; }
	};

	default long getMaxDownloadSize() { return Long.MAX_VALUE; }

	default Path getDiskPath() { return null; }

	default String getFileSuffix() { return null; }

	default boolean isDiskUsed() { return true; }

	default boolean isMemoryUsed() { return true; }

	default long getMemoryLimit() { return 1024 * 1024; }

	default long getDiskLimit() { return 1024 * 1024; }

	default int getMaxDiskItems() { return 1000; }

	default int getMaxMemoryItems() { return 1000; }

	default int getMaxStreamSize() { return 1024 * 1024; }

	default int getMaxFileSize() { return 1024 * 1024; }


}
