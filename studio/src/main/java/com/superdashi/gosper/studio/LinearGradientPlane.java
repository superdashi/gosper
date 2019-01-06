package com.superdashi.gosper.studio;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;

import com.superdashi.gosper.color.Argb;
import com.tomgibara.geom.awt.AWTUtil;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.path.Parameterization.ByIntrinsic;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntRect;

public class LinearGradientPlane implements Plane {

	private final Point point1;
	private final int argb1;
	private final Point point2;
	private final int argb2;

	private ByIntrinsic intrinsic = null;

	public LinearGradientPlane(IntCoords coord1, int argb1, IntCoords coord2, int argb2) {
		if (coord1 == null) throw new IllegalArgumentException("null coord1");
		if (coord2 == null) throw new IllegalArgumentException("null coord2");
		this.point1 = new Point(coord1.x, coord1.y);
		this.argb1 = argb1;
		this.point2 = new Point(coord2.x, coord2.y);
		this.argb2 = argb2;
	}

	public LinearGradientPlane(Point point1, int argb1, Point point2, int argb2) {
		if (point1 == null) throw new IllegalArgumentException("null point1");
		if (point2 == null) throw new IllegalArgumentException("null point2");
		this.point1 = point1;
		this.argb1 = argb1;
		this.point2 = point2;
		this.argb2 = argb2;
	}

	@Override
	public boolean opaque() {
		return Argb.isOpaque(argb1 & argb2);
	}

	@Override
	public int readPixel(int x, int y) {
		if (intrinsic == null) {
			intrinsic = LineSegment.fromPoints(point1, point2).getPath().byIntrinsic();
		}
		float p = intrinsic.parameterNearest(new Point(x,y));
		return Argb.mix(argb1, argb2, p);
	}

	@Override
	public Shader asShader() {
		return new Shader() {
			@Override
			Paint createPaint() {
				return new GradientPaint(AWTUtil.toPoint(point1), new Color(argb1, true), AWTUtil.toPoint(point2), new Color(argb2, true));
			}
		};
	}

	static final class LinearGradientFrame extends PlaneFrame<LinearGradientPlane> {

		private LinearGradientFrame(LinearGradientPlane plane, IntRect bounds) {
			super(plane, bounds);
		}

	}
}
