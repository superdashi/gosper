package com.superdashi.gosper.core;

import static java.lang.Math.round;

import com.tomgibara.geom.core.Rect;
import com.tomgibara.intgeom.IntRect;

public class Geometry {

	public static Rect asRect(IntRect rect) {
		return Rect.atPoints(rect.minX, rect.minY, rect.maxX, rect.maxY);
	}

	public static IntRect asIntRect(Rect rect) {
		return IntRect.bounded(round(rect.minX), round(rect.minY), round(rect.maxX), round(rect.maxY));
	}
}
