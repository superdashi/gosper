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
