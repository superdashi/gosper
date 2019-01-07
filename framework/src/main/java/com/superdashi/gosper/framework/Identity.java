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
package com.superdashi.gosper.framework;

import java.util.Optional;
import java.util.regex.Pattern;


public final class Identity {

	//TODO is . safe?
	private static final Pattern PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9._-]*");

	public static boolean isValidName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		return !name.isEmpty() && PATTERN.matcher(name).matches();
	}

	public static Optional<Identity> maybe(Namespace ns, String name) {
		if (ns == null) throw new IllegalArgumentException("null ns");
		if (name == null) throw new IllegalArgumentException("null name");
		return isValidName(name) ? Optional.of(new Identity(ns, name, true)) : Optional.empty();
	}

	public static Optional<Identity> maybeFromString(String str) {
		if (str == null) throw new IllegalArgumentException("null str");
		int i = str.indexOf(':');
		if (i == -1) return Optional.empty();
		String name = str.substring(i + 1);
		if (!isValidName(name)) return Optional.empty();
		Optional<Namespace> maybeNs = Namespace.maybe( str.substring(0, i) );
		if (!maybeNs.isPresent()) return Optional.empty();
		return Optional.of(new Identity(maybeNs.get(), name));
	}

	public static Identity fromString(String str) {
		return maybeFromString(str).orElseThrow(() -> new IllegalArgumentException("invalid str"));
	}

	public final Namespace ns;
	public final String name;

	public Identity(Namespace ns, String name) {
		if (ns == null) throw new IllegalArgumentException("null ns");
		if (name == null) throw new IllegalArgumentException("null name");
		if (!PATTERN.matcher(name).matches()) throw new IllegalArgumentException("invalid name");
		this.ns = ns;
		this.name = name;
	}

	//TODO hacky, maybe switch to static cons
	private Identity(Namespace ns, String name, boolean verified) {
		this.ns = ns;
		this.name = name;
	}

	// object methods

	@Override
	public int hashCode() {
		return ns.hashCode() + name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Identity)) return false;
		Identity that = (Identity) obj;
		if (!this.ns.equals(that.ns)) return false;
		if (!this.name.equals(that.name)) return false;
		return true;
	}

	@Override
	public String toString() {
		return ns.toString() + ':' + name;
	}
}