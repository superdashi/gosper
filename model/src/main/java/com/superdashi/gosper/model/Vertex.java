package com.superdashi.gosper.model;

public final class Vertex {

	public static Vertex ORIGIN = new Vertex(0f, 0f, 0f);

	public final float x;
	public final float y;
	public final float z;

	public Vertex(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vertex(float[] xyz) {
		this.x = xyz[0];
		this.y = xyz[1];
		this.z = xyz[2];
	}

	public Vector3 asVector() {
		return new Vector3(x, y, z);
	}

	@Override
	public int hashCode() {
		return asVector().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Vertex)) return false;
		return this.asVector().equals(((Vertex) obj).asVector());
	}

	@Override
	public String toString() {
		return asVector().toString();
	}
}
