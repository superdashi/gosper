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
package com.superdashi.gosper.display;

import com.superdashi.gosper.color.Coloring.Corner;
import com.superdashi.gosper.model.Vertex;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;

public abstract class RectElement extends Element {

	private static float[] shift(float[] coords) {
		coords = coords.clone();
		for (int i = 0; i < coords.length; i++) {
			coords[i] -= 0.5f;
		}
		return coords;
	}

	private static final float[] TR_TEX_COORDS = {
		1f, 1f,
		1f, 0f,
		0f, 0f,
		0f, 1f,
		};
	private static final float[] TRC_TEX_COORDS = shift(TR_TEX_COORDS);

	//?
	private static final float[] BR_TEX_COORDS = {
		0f, 1f,
		0f, 0f,
		1f, 0f,
		1f, 1f,
	};
	private static final float[] BRC_TEX_COORDS = shift(BR_TEX_COORDS);

	private static final float[] BL_TEX_COORDS = {
		0f, 0f,
		1f, 0f,
		1f, 1f,
		0f, 1f,
	};
	private static final float[] BLC_TEX_COORDS = shift(BL_TEX_COORDS);

	private static final float[] TL_TEX_COORDS = {
		0f, 1f,
		1f, 1f,
		1f, 0f,
		0f, 0f,
	};
	private static final float[] TLC_TEX_COORDS = shift(TL_TEX_COORDS);

	public static float[] defaultTexCoords(Corner corner) {
		switch (corner) {
		case BL: return BL_TEX_COORDS;
		case BR: return BR_TEX_COORDS;
		case TL: return TL_TEX_COORDS;
		case TR: return TR_TEX_COORDS;
		default: throw new IllegalStateException();
		}
	}

	public static float[] centralTexCoords(Corner corner) {
		switch (corner) {
		case BL: return BLC_TEX_COORDS;
		case BR: return BRC_TEX_COORDS;
		case TL: return TLC_TEX_COORDS;
		case TR: return TRC_TEX_COORDS;
		default: throw new IllegalStateException();
		}
	}

	public static float[] rectTexCoords(Rect rect) {
		return new float[] {
				rect.minX, rect.maxY,
				rect.maxX, rect.maxY,
				rect.maxX, rect.minY,
				rect.minX, rect.minY,
		};
	}

	public static final short[] TRIANGLES = {
		0,1,2,
		2,3,0
	};

	private static final float[] FOUR_NORMALS = {0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f};

	private final float[] vertices;

	protected RectElement(Rect rect /* x,y extents */, float z /* distance from dash plane*/) {
		Point center = rect.getCenter();
		setHandle(new Vertex(center.x, center.y, z));
		vertices = new float[] {
				rect.minX, rect.maxY, z,
				rect.maxX, rect.maxY, z,
				rect.maxX, rect.minY, z,
				rect.minX, rect.minY, z,
		};
	}

	@Override
	public final int getVertexCount() {
		return 4;
	}

	@Override
	int getTriangleCount() {
		return 2;
	}

	@Override
	public void appendTo(ElementData data) {
		data.putTriangles(TRIANGLES);
		data.vertices.put(vertices);
		data.normals.put(FOUR_NORMALS);
	}
}
