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
