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

import java.nio.FloatBuffer;
import java.util.Arrays;

import com.superdashi.gosper.model.Vertex;


abstract class Element {

	private static final int ARRAY_LIMIT = 64;
	private static final byte[][] ANIM_ARRAYS = new byte[ModeShaders.MAX_ANIMS + 1][ARRAY_LIMIT];

	static {
		for (int i = 0; i < ANIM_ARRAYS.length; i++) {
			Arrays.fill(ANIM_ARRAYS[i], (byte) (i - 1));
		}
	}

	int index = -1;

	ElementAnim anim = null;
	// the bucket to which the anim data was copied
	int animBucket;
	// the index of the anim data within the bucket
	int animIndex;
	// used to cache anim assignments for efficiency
	private byte[] animArray = null;

	// records handle of element
	private Vertex handle = Vertex.ORIGIN;
	private float[] handleArray = {0f, 0f, 0f};

	abstract int getVertexCount();

	abstract RenderPhase getRenderPhase();

	abstract RenderState getRequiredState();

	boolean isOpaque() {
		return getRequiredState().getMode().opaque;
	}

	abstract void appendTo(ElementData data);

	void setHandle(Vertex handle) {
		if (handle == null) throw new IllegalArgumentException("null handle");
		this.handle = handle;
		handleArray = handle.asVector().toArray();
	}

	Vertex getHandle() {
		return handle;
	}

	void appendAnimTo(ElementData data) {
		int count = getVertexCount();
		if (count == 0) return; // nothing to do
		{
			FloatBuffer handles = data.handles;
			for (int i = 0; i < count; i++) {
				handles.put(handleArray);
			}
		}
		if (animArray != null) {
			if (animArray[0] != animIndex) {
				// we do something special for our own array, just fill it
				if (count <= animArray.length && animArray.length > ARRAY_LIMIT) {
					Arrays.fill(animArray, (byte) animIndex);
				} else {
					animArray = null;
				}
			} else if (count > animArray.length) {
				animArray = null;
			}
		}
		if (animArray == null) {
			if (count <= ARRAY_LIMIT) {
				animArray = ANIM_ARRAYS[animIndex + 1];
			} else {
				animArray = new byte[count];
				Arrays.fill(animArray, (byte) animIndex);
			}
		}
		data.anims.put(animArray, 0, count);
	}

	int getTriangleCount() {
		int vertexCount = getVertexCount();
		int triangleCount = vertexCount / 3;
		if (triangleCount * 3 != vertexCount) throw new IllegalStateException();
		return triangleCount;
		//return getVertexCount() / 3;
	}
}
