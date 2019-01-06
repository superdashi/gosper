package com.superdashi.gosper.util;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.intgeom.IntRect;

public class Geometry {

	public static IntRect roundRectToIntRect(Rect rect) {
		return IntRect.bounded(Math.round(rect.minX), Math.round(rect.minY), Math.round(rect.maxX), Math.round(rect.maxY));
	}

	public static Rect intRectToRect(IntRect rect) {
		return Rect.atPoints(rect.minX, rect.minY, rect.maxX, rect.maxY);
	}

	public static Point exactCenter(IntRect rect) {
		return new Point((rect.minX + rect.maxX)*0.5f, (rect.minY + rect.maxY)*0.5f);
	}

	private Geometry() { }
}
