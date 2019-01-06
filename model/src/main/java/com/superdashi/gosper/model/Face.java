package com.superdashi.gosper.model;

public final class Face {

	public final Triangle triangle;
	public final Normal normal;

	public Face(Triangle triangle, Normal normal) {
		this.triangle = triangle;
		this.normal = normal;
	}
}
