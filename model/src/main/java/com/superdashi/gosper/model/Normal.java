package com.superdashi.gosper.model;

public class Normal {

	public final float x;
	public final float y;
	public final float z;

	public Normal(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Normal(float[] xyz) {
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
		if (!(obj instanceof Vertex)) return false;
		return this.asVector().equals(((Vertex) obj).asVector());
	}

	@Override
	public String toString() {
		return asVector().toString();
	}

}
