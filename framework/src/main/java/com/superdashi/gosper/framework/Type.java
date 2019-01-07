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

public final class Type {

	public final Identity identity;
	public final Kind kind;

	public Type(Identity identity, Kind kind) {
		if (identity == null) throw new IllegalArgumentException("null identity");
		if (kind == null) throw new IllegalArgumentException("null kind");
		this.identity = identity;
		this.kind = kind;
	}

	public Identity identity() {
		return identity;
	}

	@Override
	public int hashCode() {
		return identity.hashCode() + kind.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Type)) return false;
		Type that = (Type) obj;
		if (!this.identity.equals(that.identity)) return false;
		if (this.kind != that.kind) return false;
		return true;
	}

	@Override
	public String toString() {
		return identity.toString() + ';' + kind;
	}
}
