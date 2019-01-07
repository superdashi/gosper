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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Palette;
import com.superdashi.gosper.color.Palette.LogicalColor;

final class RenderPalettes implements RenderResource {

	public static final int MAX_PALETTES = 256;
	static final float UNIFORM_SCALE = 1f / (MAX_PALETTES - 1);

	private final int[] hashes = new int[MAX_PALETTES];
	private final RenderPalette[] palettes = new RenderPalette[MAX_PALETTES];
	private final IntBuffer buffer;
	private int count = 0;

	private int texId = 0;

	private int minDirty = 0;
	private int maxDirty = 0;

	RenderPalettes() {
		ByteBuffer b = ByteBuffer.allocateDirect(Palette.SIZE * 4 * MAX_PALETTES);
		b.order(ByteOrder.BIG_ENDIAN);
		buffer = b.asIntBuffer();
	}

	@Override
	public void init(GL gl) {
		int[] tmp = new int[1];
		gl.glGenTextures(1, tmp, 0);
		texId = tmp[0];
		if (texId == 0) throw new RuntimeException("Failed to create texture: " + Integer.toHexString(gl.glGetError()));
		gl.glBindTexture(GL.GL_TEXTURE_2D, texId);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, 16, MAX_PALETTES, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
	}

	@Override
	public void bind(GL gl) {
		if (texId == 0) return;
		gl.glActiveTexture(GL.GL_TEXTURE3);
		gl.glBindTexture(GL.GL_TEXTURE_2D, texId);
		clean(gl);
	}

	@Override
	public void destroy(GL gl) {
		if (texId == 0) return;
		int[] tmp = { texId };
		gl.glDeleteTextures(1, tmp, 0);
	}

	RenderPalette get(Palette p) {
		// check if we already have a matching palette
		int hc = p.hashCode();
		RenderPalette palette = null;
		for (int i = 0; i < count; i++) {
			if (hashes[i] == hc && palettes[i] != null) {
				palette = palettes[i];
				break;
			}
		}
		// no matching palette, try to allocate a new one
		if (palette == null) {
			if (count == MAX_PALETTES) { // try to compact
				int j = 0;
				for (int i = 0; i < MAX_PALETTES; i++) {
					RenderPalette cp = palettes[i];
					if (cp != null) {
						if (j != i) {
							palettes[j] = cp;
							hashes[j] = hashes[i];
							cp.index = j;
						}
						j++;
					}
				}
				count = j;
			}
			if (count < MAX_PALETTES) { // allocate from the top
				hashes[count] = hc;
				palettes[count] = palette = new RenderPalette(this, p);
				dirty(count);
				palette.index = count;
				// note: workaround for incompatible change in Java NIO APIs
				Buffer b = buffer;
				b.limit(Palette.SIZE * (count + 1));
				b.position(Palette.SIZE * count);
				palette.getPalette().writeTo(buffer);
				count ++;
			}
		}
		// look for close match
		//NOTE: could be optimized, but not worthwhile
		if (palette == null) {
			palette = palettes[0];
			int dist = distance(0, p);
			for (int i = 0; i < MAX_PALETTES; i++) {
				int d = distance(i, p);
				if (d < dist) {
					dist = d;
					palette = palettes[i];
				}
			}
		}
		palette.refCount ++;
		return palette;
	}

	void put(RenderPalette palette) {
		if (palette.refCount <= 0) throw new IllegalStateException("Ref count already zero (or less) on palette " + palette);
		if (--palette.refCount != 0) return;
		int i = palette.index;
		if (palettes[i] == null) throw new IllegalStateException("Palette alreadt removed? " + palette);
		palettes[i] = null;
	}

	private void dirty(int index) {
		if (minDirty == maxDirty) {
			minDirty = index;
			maxDirty = index + 1;
		} else {
			minDirty = Math.min(minDirty, index);
			maxDirty = Math.max(maxDirty, index + 1);
		}
	}

	private void clean(GL gl) {
		if (minDirty == maxDirty) return;
		buffer.limit(Palette.SIZE * maxDirty);
		buffer.position(Palette.SIZE * minDirty);
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, minDirty, Palette.SIZE, maxDirty - minDirty, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);
		minDirty = 0;
		maxDirty = 0;
	}

	private int distance(int i, Palette p) {
		i *= Palette.SIZE;
		int sum = 0;
		for (int j = 0; j < Palette.SIZE; j++) {
			int c1 = buffer.get(i + j);
			int c2 = p.color(LogicalColor.valueOf(j));
			sum += Coloring.opaqueDistSqr(c1, c2);
		}
		return sum;
	}
}
