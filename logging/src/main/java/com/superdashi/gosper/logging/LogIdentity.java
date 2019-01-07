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
package com.superdashi.gosper.logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class LogIdentity {

	// statics

	private static void checkName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		if (name.isEmpty()) throw new IllegalArgumentException("empty name");
		if (name.indexOf('/') != -1) throw new IllegalArgumentException("invalid name");
	}

	public static LogIdentity create(String firstName, String... moreNames) {
		checkName(firstName);
		return new LogIdentity(null, firstName).descendant(moreNames);
	}

	public static LogIdentity fromString(String str) {
		if (str == null) throw new IllegalArgumentException("null str");
		if (str.isEmpty()) throw new IllegalArgumentException("empty str");
		String[] names = str.split("/");
		LogIdentity identity = null;
		for (String name : names) {
			checkName(name);
			identity = new LogIdentity(identity, name);
		}
		return identity;
	}

	// fields

	private final LogIdentity parent;
	private final String name;
	private final int hashCode;
	private final int count;

	// constructors

	private LogIdentity(LogIdentity parent, String name) {
		this.parent = parent; // may be null
		this.name = name; // assumed to already be checked
		hashCode = parent == null ? name.hashCode() : parent.hashCode * 31 + name.hashCode();
		count = parent == null ? 1 : parent.count + 1;
	}

	// accessors

	public String name() {
		return name;
	}

	public Optional<LogIdentity> parent() {
		return Optional.ofNullable(parent);
	}

	public List<String> names() {
		List<String> list = new ArrayList<>();
		LogIdentity id = this;
		do {
			list.add(id.name);
			id = id.parent;
		} while (id != null);
		Collections.reverse(list);
		return Collections.unmodifiableList(list);
	}

	// public methods

	public boolean isSuperIdentityOf(LogIdentity that) {
		if (that == null) throw new IllegalArgumentException("null that");
		if (that.count < this.count) return false;
		while (that.count > this.count) that = that.parent;
		return this.equals(that);
	}

	public boolean isSubIdentityOf(LogIdentity that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return that.isSuperIdentityOf(this);
	}

	public <T> LogAppender<T> appendTo(LogAppender<T> a) throws IOException {
		if (parent != null) {
			parent.appendTo(a);
			a.append('/');
		}
		a.append(name);
		return a;
	}

	// package scoped methods

	LogIdentity child(String name) {
		checkName(name);
		return new LogIdentity(this, name);
	}

	LogIdentity descendant(String... names) {
		LogIdentity identity = this;
		for (String name : names) {
			identity = identity.child(name);
		}
		return identity;
	}

	// object methods

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof LogIdentity)) return false;
		LogIdentity that = (LogIdentity) obj;
		return this.hashCode == that.hashCode && this.name.equals(that.name) && Objects.equals(this.parent, that.parent);
	}

	private void toString(StringBuilder sb) {
		if (parent != null) {
			parent.toString(sb);
			sb.append('/');
		}
		sb.append(name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

}
