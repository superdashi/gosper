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
package com.superdashi.gosper.config;

import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public class ConfigUtil {

	public static final StoreType<Configurable> storeType = StoreType.of(Configurable.class);

	public static final Store<Configurable> none() {
		return storeType.emptyStore();
	}

	public static final Store<Configurable> singleton(Configurable s) {
		if (s == null) throw new IllegalArgumentException("null s");
		return storeType.constantStore(s, 1);
	}

	public static final Store<Configurable> array(Configurable... configurables) {
		return storeType.arrayAsStore(configurables);
	}

	public static final Store<Configurable> size(int size) {
		return storeType.storage().newStore(size);
	}

}
