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
