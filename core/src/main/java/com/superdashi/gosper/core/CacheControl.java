package com.superdashi.gosper.core;

public class CacheControl {

	public static CacheControl createCache(CachePolicy policy) {
		if (policy == null) throw new IllegalArgumentException("null policy");
		Cache cache = new Cache(policy);
		return new CacheControl(cache);
	}

	private final Cache cache;

	private CacheControl(Cache cache) {
		this.cache = cache;
	}

	public Cache getCache() {
		return cache;
	}

	public void closeCache() {
		cache.close();
	}

	public void openCache() {
		cache.open();
	}

	public void clearCacheMemory() {
		cache.clearMemory();
	}

	public void clearCacheDisk() throws CacheException {
		cache.clearDisk();
	}
}