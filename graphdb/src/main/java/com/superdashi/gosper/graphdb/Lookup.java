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
package com.superdashi.gosper.graphdb;

import java.util.Map;
import java.util.Random;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVMap.Builder;

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.tomgibara.collect.Collect.Sets;
import com.tomgibara.collect.EquivalenceMap;
import com.tomgibara.fundament.Bijection;

final class Lookup<T> {

	private static final Sets<String> strSets = Collect.setsOf(String.class);
	private static final Sets<Integer> intSets = Collect.setsOf(int.class);
	private static final Maps<Integer, String> strsByCodeMaps = intSets.mappedTo(String.class);
	private static final Maps<String, Integer> codesByStrMaps = strSets.mappedTo(int.class);

	private static final Builder<Integer, String> byIntBuilder = new MVMap.Builder<>();
	private static final Builder<String, Integer> byStrBuilder = new MVMap.Builder<>();

	interface CodeGenerator {
		int generateCode();
	}

	static final CodeGenerator positiveRandom(Random r) {
		if (r == null) throw new IllegalArgumentException("null r");
		return () -> {
			// avoids max int
			while (true) {
				int k = r.nextInt() & 0x7fffffff;
				if (k != Integer.MAX_VALUE) return k;
			}
		};
	}

	private final MVMap<Integer, String> stringsByCode;
	private final MVMap<String, Integer> codesByString;
	private final Bijection<String, T> bijection;
	private final CodeGenerator generator;

	private boolean modified = false;
	private boolean locked = false;

	private final EquivalenceMap<Integer, T> objsByCode;
	private final EquivalenceMap<T, Integer> codesByObj;
	private final Map<Integer, T> byCode;
	private final Map<T, Integer> byObj;

	Lookup(
			MVStore store,
			String byCodeMapName,
			String byStringMapName,
			Bijection<String, T> bijection,
			CodeGenerator generator) {
		this(
				store.openMap(byCodeMapName, byIntBuilder),
				store.openMap(byStringMapName, byStrBuilder),
				bijection,
				generator
				);
	}

	Lookup(
			MVMap<Integer, String> stringsByCode,
			MVMap<String, Integer> codesByString,
			Bijection<String, T> bijection,
			CodeGenerator generator) {
		this.stringsByCode = stringsByCode;
		this.codesByString = codesByString;
		this.bijection = bijection;
		this.generator = generator;

		Maps<Integer, T> byCodeMaps;
		Maps<T, Integer> byObjMaps;
		if (bijection.inverse() == bijection) { // ie. if identity
			byCodeMaps = (Maps<Integer, T>) strsByCodeMaps;
			byObjMaps = (Maps<T, Integer>) codesByStrMaps;
		} else {
			byCodeMaps = intSets.mappedTo(bijection.rangeType());
			byObjMaps = Collect.setsOf(bijection.rangeType()).mappedTo(int.class);
		}
		objsByCode = byCodeMaps.newMap();
		codesByObj = byObjMaps.newMap();
		byCode = objsByCode.immutableView();
		byObj = codesByObj.immutableView();
	}

	void record(T value) {
		assert value != null;
		checkNotLocked();
		String str = bijection.disapply(value);
		if (codesByString.containsKey(str)) return;
		int code;
		do {
			code = generator.generateCode();
		} while (stringsByCode.containsKey(code));
		stringsByCode.put(code, str);
		codesByString.put(str, code);
		modified = true;
	}

	boolean lock() {
		if (!locked) {
			stringsByCode.forEach((c,s) -> {
				T t = bijection.apply(s);
				objsByCode.put(c, t);
				codesByObj.put(t, c);
			});
			locked = true;
		}
		return modified;
	}

	void unlock() {
		if (locked) {
			objsByCode.clear();
			codesByObj.clear();
			locked = false;
		}
	}
	Map<Integer, T> getByCode() {
		return byCode;
	}

	Map<T, Integer> getByObj() {
		return byObj;
	}

	private void checkNotLocked() {
		if (locked) throw new ConstraintException(ConstraintException.Type.LOOKUP_STATE, "locked lookup");
	}

}
