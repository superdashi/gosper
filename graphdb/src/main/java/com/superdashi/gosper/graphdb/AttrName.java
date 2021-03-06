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

import java.util.Optional;
import java.util.regex.Pattern;

import com.superdashi.gosper.framework.Namespace;

public final class AttrName implements Comparable<AttrName> {

	private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]*");

	static void checkAttrName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		if (name.isEmpty()) throw new IllegalArgumentException("empty name");
		if (!isValidAttrName(name)) throw new IllegalArgumentException("invalid name");
	}

	public static boolean isValidAttrName(String str) {
		if (str == null) throw new IllegalArgumentException("null str");
		return VALID_NAME.matcher(str).matches();
	}

	public static AttrName create(Namespace namespace, String name) {
		if (namespace == null) throw new IllegalArgumentException("null namespace");
		checkAttrName(name);
		return new AttrName(namespace, name);
	}

	public static Optional<AttrName> optionalCreate(Namespace namespace, String name) {
		return namespace == null || name == null || !isValidAttrName(name) ? Optional.empty() : Optional.of(new AttrName(namespace, name));
	}

	public final Namespace namespace;
	public final String name;

	AttrName(Namespace namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

	// comparable methods

	@Override
	public int compareTo(AttrName that) {
		int c = this.namespace.compareTo(that.namespace);
		return c == 0 ? this.name.compareTo(that.name) : c;
	}

	// object methods

	@Override
	public int hashCode() {
		return name.hashCode() + namespace.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof AttrName)) return false;
		AttrName that = (AttrName) obj;
		return this.name.equals(that.name) && this.namespace.equals(that.namespace);
	}

	@Override
	public String toString() {
		return namespace + ":" + name;
	}

}
