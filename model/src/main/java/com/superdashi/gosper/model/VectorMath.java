package com.superdashi.gosper.model;

public final class VectorMath {

	private float x;
	private float y;
	private float z;

	public VectorMath() {
	}

	public VectorMath(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public VectorMath add(Vector3 v) {
		x += v.x;
		y += v.y;
		z += v.z;
		return this;
	}

	public Vector3 toVector3() {
		return new Vector3(x, y, z);
	}

	public Normal toNormal() {
		float s = (float) Math.sqrt(x*x + y*y + z*z);
		return new Normal(x/s, y/s, z/s);
	}

	public Vertex toVertex() {
		return new Vertex(x, y, z);
	}
}
