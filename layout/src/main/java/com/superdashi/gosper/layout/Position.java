/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.superdashi.gosper.layout;

import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

//TODO needs an equals implementation
//TODO should provide option of a direct result, skipping the intermediate transform
public final class Position {

	public enum Fit {
		FREE,
		MATCH,
		COVER;
	}

	private static final Position[] positions = new Position[81];

	static private int index(Fit fitH, Fit fitV, Alignment2D align) {
		int ao = align.ordinal;
		if (ao < 0) {
			int a = (int) (Integer.MAX_VALUE * align.horizontal.m);
			int b = (int) (Integer.MAX_VALUE * align.vertical.m);
			int i = a | b | fitH.ordinal() | (fitV.ordinal() << 16);
			return i == 0 ? Integer.MIN_VALUE : - i;
		}
		return fitH.ordinal() + 3 * (fitV.ordinal() + 3 * align.ordinal);
	}

	public static Position from(Fit fitH, Fit fitV, Alignment2D align) {
		int index = index(fitH, fitV, align);
		if (index < 0) return new Position(fitH, fitV, align);
		Position position = positions[index];
		if (position == null) {
			position = new Position(fitH, fitV, align);
			positions[index] = position;
		}
		return position;
	}

	private final int index;
	private final int fit;
	public final Fit fitH;
	public final Fit fitV;
	public final Alignment2D align;

	private Position(Fit fitH, Fit fitV, Alignment2D align) {
		index = index(fitH, fitV, align);
		fit = fitH.ordinal() + 3 * fitV.ordinal();

		this.fitH = fitH;
		this.fitV = fitV;
		this.align = align;
	}

//	public AffineTransform transform(Rectangle src, Rectangle dst) {
//		if (src == null) throw new IllegalArgumentException("null src");
//		if (dst == null) throw new IllegalArgumentException("null dst");
//
//		double sw = src.getWidth();
//		double sh = src.getHeight();
//		double dw = dst.getWidth();
//		double dh = dst.getHeight();
//		// the scale applied to the source, +ve is larger
//		final double sx;
//		final double sy;
//		// the differences in size of the actual transformed destination
//		// compared to the specified destination, +ve is larger
//		final double dx;
//		final double dy;
//		switch (fit) {
//		case 0: // free x,  free y
//			sy = sx = 1.0;
//			dx = dw - sw;
//			dy = dh - sh;
//			break;
//		case 1: // match x, free y
//		case 2: // cover x, free y
//			sy = sx = dw / sw;
//			dx = 0.0;
//			dy = dh - sy * sh;
//			break;
//		case 3: // free x,  match y
//		case 6: // free x,  cover y
//			sx = sy = dh / sh;
//			dx = dw - sx * sw;
//			dy = 0.0;
//			break;
//		case 4: // match x, match y
//			sx = dw / sw;
//			sy = dh / sh;
//			dx = 0.0;
//			dy = 0.0;
//			break;
//		case 5: // cover x, match y
//			sy = dh / sh;
//			sx = Math.max(sy, dw / sw);
//			dx = dw - sx * sw;
//			dy = 0.0;
//			break;
//		case 7: // match x, cover y
//			sx = dw / sw;
//			sy = Math.max(sx, dh / sh);
//			dx = 0.0;
//			dy = dh - sy * sh;
//			break;
//		case 8: // cover x, cover y;
//			double tx = dw / sw;
//			double ty = dh / sh;
//			if (tx > ty) {
//				sy = sx = tx;
//				dx = 0.0;
//				dy = dh - sy * sh;
//			} else {
//				sx = sy = ty;
//				dx = dw - sx * sw;
//				dy = 0.0;
//			}
//			break;
//			default: throw new IllegalStateException("fit case: " + fit);
//		}
//
//		final double tx = dst.getX() - src.getX() + dx * alignH.m;
//		final double ty = dst.getY() - src.getY() + dy * alignV.m;
//
//		return new AffineTransform(sx, 0.0, 0.0, sy, tx, ty);
//	}

	public Transform transform(Rect src, Rect dst) {
		if (src == null) throw new IllegalArgumentException("null src");
		if (dst == null) throw new IllegalArgumentException("null dst");

		float sw = src.getWidth();
		float sh = src.getHeight();
		float dw = dst.getWidth();
		float dh = dst.getHeight();
		// the scale applied to the source, +ve is larger
		final float sx;
		final float sy;
		// the differences in size of the actual transformed destination
		// compared to the specified destination, +ve is larger
		final float dx;
		final float dy;
		switch (fit) {
		case 0: // free x,  free y
			sy = sx = 1.0f;
			dx = dw - sw;
			dy = dh - sh;
			break;
		case 1: // match x, free y
		case 2: // cover x, free y
			sy = sx = dw / sw;
			dx = 0.0f;
			dy = dh - sy * sh;
			break;
		case 3: // free x,  match y
		case 6: // free x,  cover y
			sx = sy = dh / sh;
			dx = dw - sx * sw;
			dy = 0.0f;
			break;
		case 4: // match x, match y
			sx = dw / sw;
			sy = dh / sh;
			dx = 0.0f;
			dy = 0.0f;
			break;
		case 5: // cover x, match y
			sy = dh / sh;
			sx = Math.max(sy, dw / sw);
			dx = dw - sx * sw;
			dy = 0.0f;
			break;
		case 7: // match x, cover y
			sx = dw / sw;
			sy = Math.max(sx, dh / sh);
			dx = 0.0f;
			dy = dh - sy * sh;
			break;
		case 8: // cover x, cover y;
			float tx = dw / sw;
			float ty = dh / sh;
			if (tx > ty) {
				sy = sx = tx;
				dx = 0.0f;
				dy = dh - sy * sh;
			} else {
				sx = sy = ty;
				dx = dw - sx * sw;
				dy = 0.0f;
			}
			break;
			default: throw new IllegalStateException("fit case: " + fit);
		}

		final float tx = dst.minX - src.minX + align.horizontal.adjustDelta(dx);
		final float ty = dst.minY - src.minY + align.vertical  .adjustDelta(dy);

		return Transform.components(sx, 0.0f, 0.0f, sy, tx, ty);
	}

	public IntRect position(IntDimensions dimensions, IntRect bounds) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		if (bounds == null) throw new IllegalArgumentException("null bounds");

		int sw = dimensions.width;
		int sh = dimensions.height;
		int dw = bounds.width();
		int dh = bounds.height();

		int w;
		int h;

		int dx;
		int dy;

		switch (fit) {
		case 0: // free x,  free y
			w = sw;
			h = sh;
			dx = dw - sw;
			dy = dh - sh;
			break;
		case 1: // match x, free y
		case 2: // cover x, free y
			w = dw;
			h = sh * dw / sw;
			dx = 0;
			dy = dh - h;
			break;
		case 3: // free x,  match y
		case 6: // free x,  cover y
			w = sw * dh / sh;
			h = dh;
			dx = dw - w;
			dy = 0;
			break;
		case 4: // match x, match y
			w = dw;
			h = dh;
			dx = 0;
			dy = 0;
			break;
		case 5: // cover x, match y
			h = dh;
			w = Math.max(dw, sw * dh / sh);
			dx = dw - w;
			dy = 0;
			break;
		case 7: // match x, cover y
			w = dw;
			h = Math.max(dh, sh * dw / sw);
			dx = 0;
			dy = dh - h;
			break;
		case 8: // cover x, cover y;
			float tx = dw / sw;
			float ty = dh / sh;
			if (tx > ty) {
				w = dw;
				h = sh * dw / sw;
				dx = 0;
				dy = dh - h;
			} else {
				w = sw * dh / sh;
				h = dh;
				dx = dw - w;
				dy = 0;
			}
			break;
			default: throw new IllegalStateException("fit case: " + fit);
		}

		int x = bounds.minX + align.horizontal.adjustDelta(dx);
		int y = bounds.minY + align.vertical  .adjustDelta(dy);

		return IntRect.rectangle(x, y, w, h);
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public String toString() {
		return String.format("%sx%s @ %sx%s", fitH, fitV, align.horizontal, align.vertical);
	}

}