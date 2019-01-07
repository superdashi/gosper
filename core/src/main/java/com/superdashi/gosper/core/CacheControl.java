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