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

import static com.superdashi.gosper.display.DisplayUtil.nCopies;
import static com.superdashi.gosper.display.HexElement.X;
import static com.superdashi.gosper.display.HexElement.Y;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.superdashi.gosper.anim.AnimEffects;
import com.superdashi.gosper.anim.AnimInterpolators;
import com.superdashi.gosper.anim.Animator;
import com.superdashi.gosper.anim.Animator.Terminal;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.display.ShaderParams.BorderParams;

public class HexBackgroundDisplay implements ElementDisplay {

	private static final int TRIANGLE_COUNT = 6 * 3;
	private static final int VERTEX_COUNT = 13;
	private static final float Z = 0f;
	private static final float BZ = -1f;

	static final float[] VERTICES = {
		 1,  0,  Z,
		 X,  Y,  Z,
		-X,  Y,  Z,
		-1,  0,  Z,
		-X, -Y,  Z,
		 X, -Y,  Z,

		 1,  0, BZ,
		 X,  Y, BZ,
		-X,  Y, BZ,
		-1,  0, BZ,
		-X, -Y, BZ,
		 X, -Y, BZ,

		 0,  0,  Z,
	};

	private static final float[] TEX_COORDS = {
		0f, 1f,  1f, 1f,  0f, 1f,  1f, 1f,  0f, 1f,  1f, 1f,
		0f, 0f,  1f, 0f,  0f, 0f,  1f, 0f,  0f, 0f,  1f, 0f,
		0f, 0f
	};

	private static final short[] TRIANGLES = {
		12,  0,  1,
		12,  1,  2,
		12,  2,  3,
		12,  3,  4,
		12,  4,  5,
		12,  5,  0,

		 0,  6,  7,
		 7,  1,  0,

		 1,  7,  8,
		 8,  2,  1,

		 2,  8,  9,
		 9,  3,  2,

		 3,  9, 10,
		10,  4,  3,

		 4, 10, 11,
		11,  5,  4,

		 5, 11,  6,
		 6,  0,  5,
	};

	private static final float[] NORMALS = nCopies(VERTEX_COUNT, HexElement.NORMAL);

	private static final RenderState REQUIRED = new RenderState();
	static {
		REQUIRED.setMode(Mode.BORDER);
	}

	private final int minHexes;
	private HexColumnElement[] els = null;

	public HexBackgroundDisplay(int minHexes) {
		this.minHexes = minHexes;
	}

	@Override
	public void init(RenderContext context) {
		ArtPlane plane = context.getDisplay().getBackPlane();
		float width = plane.width;
		float height = plane.height;
		float maxDim = Math.max(width, height);
		float s = maxDim/minHexes;

		int w = 1 + (int) ((width) / (s * Y * 2));
		int h = (int) ((height) / (s * X));
		float ox = /*2*/ -w * s * 1.5f + s * 0.75f;
		float oy = /*2*/ -h * s * 0.5f + s / Y;
		//s *= 0.125f;
		els = new HexColumnElement[w * h];
		ColOrd[] ords = new ColOrd[w * h];
		int i = 0;
		Animator scale = AnimEffects.scale(s,s).at(0L);
		Random r = new Random(0L);
		float cz = -plane.depth;
		BorderParams params = BorderParams.creator.create();
		params.setColor(0xff33aaff);
		params.setBorderWidth(0.15f);
		for (int y = 0; y < h; y++) {
			float d = (y & 1) * s * 1.5f;
			for (int x = 0; x < w; x++) {
				HexColumnElement el = new HexColumnElement(0xff333333, params);
				ElementAnim a = new ElementAnim();
				a.addAnimator(scale);
				float cx = ox + x * s * 3 + d;
				float cy = oy + y * Y * s;
				long period = 3000L + (long) (r.nextFloat() * 7000f);
				float hue = r.nextFloat() * 0f;
				float sat = r.nextFloat() * 0.25f + 0.75f;
				float bri = r.nextFloat() * 0.5f + 0.5f;
				int fgColor = Color.HSBtoRGB(hue, sat, bri);
				a.addAnimator(AnimEffects.translate(cx, cy, cz, cx, cy, cz).at(0L));
				a.addAnimator(AnimEffects.color(false, fgColor).at(0L));
				a.addAnimator(AnimEffects.translate(0f, 0f, -0.5f, 0f, 0f, 0.5f).interpolate(AnimInterpolators.accelDecel()).animator(0L, period, Terminal.BOUNCE, Terminal.BOUNCE));
				el.anim = a;
				ords[i++] = new ColOrd(cx * cx + cy * cy, el);
			}
		}

		Arrays.sort(ords);
		for (i = 0; i < els.length; i++) {
			els[i] = ords[i].e;
		}
	}

	@Override
	public void destroy(GL2ES2 gl) {
		els = null;
	}

	@Override
	public Collection<Element> getElements() {
		return Arrays.asList(els);
	}

	private final class HexColumnElement extends Element {

		private final BorderParams params;
		private final int[] colors;

		HexColumnElement(int color, BorderParams params) {
			this.params = params;
			colors = new int[VERTEX_COUNT];
			Arrays.fill(colors, Coloring.argbToRGBA(color));
		}

		@Override
		int getVertexCount() {
			return VERTEX_COUNT;
		}

		@Override
		int getTriangleCount() {
			return TRIANGLE_COUNT;
		}

		@Override
		RenderPhase getRenderPhase() {
			return RenderPhase.BACKGROUND;
		}

		@Override
		RenderState getRequiredState() {
			return REQUIRED;
		}

		@Override
		void appendTo(ElementData data) {
			data.putTriangles(TRIANGLES);
			data.vertices.put(VERTICES);
			data.normals.put(NORMALS);
			data.texCoords.put(TEX_COORDS);
			data.colors.put(colors);
			params.writeTo(data.shaders, VERTEX_COUNT);
		}
	}

	private static final class ColOrd implements Comparable<ColOrd> {

		final float d;
		final HexColumnElement e;

		public ColOrd(float d, HexColumnElement e) {
			this.d = d;
			this.e = e;
		}

		@Override
		public int compareTo(ColOrd that) {
			return Float.compare(this.d, that.d);
		}
	}
}
