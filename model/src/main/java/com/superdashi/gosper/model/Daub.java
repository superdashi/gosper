package com.superdashi.gosper.model;

import com.tomgibara.geom.core.Point;

public final class Daub {

	public final int mat;
	public final Point p1;
	public final Point p2;
	public final Point p3;

	public Daub(int mat) {
		this.mat = mat;
		p3 = p2 = p1 = Point.ORIGIN;
	}

	public Daub(int mat, Point p1, Point p2, Point p3) {
		this.mat = mat;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}
}
