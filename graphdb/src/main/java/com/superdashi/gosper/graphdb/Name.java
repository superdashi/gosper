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

import com.superdashi.gosper.framework.Namespace;

abstract class Name {

	// combines namespace code with name code
	static final long nsnId(int nsc, int nmc) {
		return (long) nsc << 32 | (long) nmc & 0xffffffffL;
	}

	static final int nsCode(long nsnId) {
		return (int) (nsnId >> 32);
	}

	static final int nmCode(long nsnId) {
		return (int) nsnId;
	}

	public final Namespace namespace;
	public final String name;

	Name(Namespace namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

	// object methods

	@Override
	public int hashCode() {
		return name.hashCode() + namespace.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Name)) return false;
		Name that = (Name) obj;
		return
				this.getClass() == that.getClass() &&
				this.name.equals(that.name) &&
				this.namespace.equals(that.namespace);
	}

	@Override
	public String toString() {
		return namespace + ":" + name;
	}

}
