package com.superdashi.gosper.model;

public final class Triangle {

	public final Vertex v1;
	public final Vertex v2;
	public final Vertex v3;

	public Triangle(Vertex v1, Vertex v2, Vertex v3) {
		if (v1 == null) throw new IllegalArgumentException("null v1");
		if (v2 == null) throw new IllegalArgumentException("null v2");
		if (v3 == null) throw new IllegalArgumentException("null v3");
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}

	@Override
	public String toString() {
		return "[" + v1.toString() + " " + v2.toString() + " " + v3.toString() + "]";
	}
}
