package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Qualifier;
import com.tomgibara.intgeom.IntDimensions;

public final class VisualQualifier {

	// statics

	private static final VisualQualifier universal = new VisualQualifier(Qualifier.universal(), IntDimensions.NOTHING);

	public static VisualQualifier universal() { return universal; }

	// fields

	public final Qualifier qualifier;
	public final IntDimensions dimensions;

	// constructors

	public VisualQualifier(Qualifier qualifier, IntDimensions dimensions) {
		if (qualifier == null) throw new IllegalArgumentException("null qualifier");
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		this.qualifier = qualifier;
		this.dimensions = dimensions;
	}

	// public accessors

	public boolean isUniversal() {
		return qualifier.isUniversal() && dimensions.isNothing();
	}

	public boolean isFullySpecified() {
		return qualifier.isFullySpecified() && !dimensions.isDegenerate();
	}

	// public methods

	public boolean matches(VisualQualifier that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return qualifier.matches(that.qualifier) && this.dimensions.metBy(that.dimensions);
	}

	// object methods

	@Override
	public int hashCode() {
		return qualifier.hashCode() + dimensions.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof VisualQualifier)) return false;
		VisualQualifier that = (VisualQualifier) obj;
		return this.qualifier.equals(that.qualifier) && this.dimensions.equals(that.dimensions);
	}

	@Override
	public String toString() {
		return qualifier + " " + dimensions;
	}
}