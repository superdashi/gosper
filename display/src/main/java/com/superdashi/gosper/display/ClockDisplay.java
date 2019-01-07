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

import static com.superdashi.gosper.color.Coloring.writeColor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Coloring.Corner;
import com.superdashi.gosper.core.Clock;
import com.superdashi.gosper.core.ClockConfig;
import com.superdashi.gosper.core.ClockConfig.HandShape;
import com.superdashi.gosper.display.ShaderParams.PlainParams;
import com.superdashi.gosper.model.Vector3;
import com.superdashi.gosper.model.Vertex;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.transform.Transform;

// Height list
// 8/8 Ring
// 7/8 Seconds
// 6/8 Minutes
// 5/8 Hours
// 4/8 Ticks
// 1/8 Backs
// 0/8 Face
class ClockDisplay implements ElementDisplay {

	private static final float PI_BY_30 = Angles.PI / 30;
	private static final float[] NORMAL = {0f, 0f, 1f};
	private static final float[] TICK_TEX_CORDS = new float[12 * 8];
	static {
		float[] texCoords = RectElement.defaultTexCoords(Corner.TR);
		for (int i = 0; i < TICK_TEX_CORDS.length; i += 8) {
			System.arraycopy(texCoords, 0, TICK_TEX_CORDS, i, 8);
		}
	}
	private static final short[] HAND_TRIANGLES = {
		0,3,1,  3,4,1,  4,2,1,
		0,1,5,  1,6,5,  1,2,6,
	};

	private static final short[] TICK_TRIANGLES = {
		 0, 1, 2,   2, 3, 0,
		 4, 5, 6,   6, 7, 4,
		 8, 9,10,  10,11, 8,
		12,13,14,  14,15,12,
		16,17,18,  18,19,16,
		20,21,22,  22,23,20,
		24,25,26,  26,27,24,
		28,29,30,  30,31,28,
		32,33,34,  34,35,32,
		36,37,38,  38,39,36,
		40,41,42,  42,43,40,
		44,45,46,  46,47,44,
	};

	private static final void transfer(float[] a, float[] b) {
		System.arraycopy(a, 0, b, 0, a.length);
	}

	private static final RenderState face = new RenderState();
	private static final RenderState tick = new RenderState();
	private static final RenderState hand = new RenderState();
	private static final RenderState back = new RenderState();

	static {
		face.setMode(Mode.PLAIN_LIT);
		tick.setMode(Mode.PLAIN_LIT);
		hand.setMode(Mode.PLAIN_LIT);
		back.setMode(Mode.PLAIN_LIT);
	}

	private final RenderPhase phase;
	private final float[] frontTickVertices =   new float[ 12 * 4 * 3 ];
	private final float[] backTickVertices =    new float[ 12 * 4 * 3 ];
	private final int  [] tickColors1 =         new int  [ 12 * 4     ];
	private final int  [] tickColors2 =         new int  [ 12 * 4     ];
	private final int  [] backColors1 =         new int  [ 12 * 4     ];
	private final int  [] backColors2 =         new int  [ 12 * 4     ];

	private final float[] secondFrontVertices = new float[  7 * 3     ];
	private final float[] secondBackVertices =  new float[  7 * 3     ];
	private final float[] secondFrontCoords =   new float[  7 * 2     ];
	private final float[] secondBackCoords =    new float[  7 * 2     ];
	private final float[] secondTexCoords =     new float[  7 * 2     ];
	private final int  [] secondColors1 =       new int  [  7         ];
	private final int  [] secondColors2 =       new int  [  7         ];

	private final float[] minuteFrontVertices = new float[  7 * 3     ];
	private final float[] minuteBackVertices =  new float[  7 * 3     ];
	private final float[] minuteFrontCoords =   new float[  7 * 2     ];
	private final float[] minuteBackCoords =    new float[  7 * 2     ];
	private final float[] minuteTexCoords =     new float[  7 * 2     ];
	private final int  [] minuteColors1 =       new int  [  7         ];
	private final int  [] minuteColors2 =       new int  [  7         ];

	private final float[] hourFrontVertices =   new float[  7 * 3     ];
	private final float[] hourBackVertices =    new float[  7 * 3     ];
	private final float[] hourFrontCoords =     new float[  7 * 2     ];
	private final float[] hourBackCoords =      new float[  7 * 2     ];
	private final float[] hourTexCoords =       new float[  7 * 2     ];
	private final int  [] hourColors1 =         new int  [  7         ];
	private final int  [] hourColors2 =         new int  [  7         ];

	private final int  [] handColors1 =         new int  [  7         ];
	private final int  [] handColors2 =         new int  [  7         ];
	private final float[] scratchCoords =       new float[  7 * 2     ];

	//TODO needs to be possible configure its bounds
	private float z = 0.1f;
	private final Clock clock;
	private final ClockConfig style; // for efficiency

	private final ClockElement ticksFront;
	private final ClockElement ticksBack;
	private final ClockElement secondFront;
	private final ClockElement secondBack;
	private final ClockElement minuteFront;
	private final ClockElement minuteBack;
	private final ClockElement hourFront;
	private final ClockElement hourBack;
	private final CircleElement faceEl;
	private final CircleElement ring;

	private LocalTime time = LocalTime.now();
	private boolean secondDirty = true;
	private boolean minuteDirty = true;
	private boolean hourDirty   = true;

	ClockDisplay(Clock clock, RenderPhase phase) {
		this.clock = clock;
		this.style = clock.style;
		this.phase = phase;

		ticksFront  = new ClockElement(tick, TICK_TRIANGLES, frontTickVertices, TICK_TEX_CORDS, tickColors1, tickColors2, () -> {});
		ticksBack   = new ClockElement(back, TICK_TRIANGLES, backTickVertices, TICK_TEX_CORDS, backColors1, backColors2, () -> {});
		secondFront = new ClockElement(hand, HAND_TRIANGLES, secondFrontVertices, secondTexCoords, secondColors1, secondColors2, this::updateSecond);
		secondBack  = new ClockElement(back, HAND_TRIANGLES, secondBackVertices, secondTexCoords, handColors1, handColors2, this::updateSecond);
		minuteFront = new ClockElement(hand, HAND_TRIANGLES, minuteFrontVertices, minuteTexCoords, minuteColors1, minuteColors2, this::updateMinute);
		minuteBack  = new ClockElement(back, HAND_TRIANGLES, minuteBackVertices, minuteTexCoords, handColors1, handColors2, this::updateMinute);
		hourFront   = new ClockElement(hand, HAND_TRIANGLES, hourFrontVertices, hourTexCoords, hourColors1, hourColors2, this::updateHour);
		hourBack    = new ClockElement(back, HAND_TRIANGLES, hourBackVertices, hourTexCoords, handColors1, handColors2, this::updateHour);
		faceEl      = new CircleElement(new Vector3(0f, 0f, z), style.radius, style.faceColor1, Coloring.NONE, face.getMode().lit);
		ring        = new CircleElement(new Vector3(0f, 0f, z + style.height * 4), 0.08f * style.radius, style.secondColor1, Coloring.NONE, hand.getMode().lit);

		computeTicks();
		computeHands();
		computeColors();
	}

	@Override
	public void update(RenderContext context) {
		setTime(LocalTime.now());
	}

	@Override
	public Collection<Element> getElements() {
		List<Element> els = new ArrayList<>(10);
		// always shown
		{
			els.add(ticksFront);
			els.add(secondFront);
			els.add(minuteFront);
			els.add(hourFront);
		}
		// backs
		if (style.renderBack) {
			els.add(ticksBack);
			els.add(secondBack);
			els.add(minuteBack);
			els.add(hourBack);
		}
		if (style.renderFace) {
			els.add(faceEl);
		}
		els.add(ring);
		return els;
	}

	private void setTime(LocalTime time) {
		if (time.equals(this.time)) return;
		if (this.time.getHour() != time.getHour() || this.time.getMinute() != time.getMinute() || this.time.getSecond() != time.getSecond()) {
			secondDirty = true;
			minuteDirty = true;
			secondDirty = true;
		} else {
			secondDirty = style.secondSweep;
		}
		this.time = time;
	}

	private void updateSecond() {
		if (!secondDirty) return;
		float s = time.getSecond();
		if (style.secondSweep) s += time.get(ChronoField.MILLI_OF_SECOND) / 1000f;
		float a = s * - PI_BY_30;
		Transform t = Transform.rotation(a);
		updateVertices(secondFrontCoords, secondFrontVertices, 7 * style.height / 8, t);
		updateVertices(secondBackCoords, secondBackVertices, style.height / 8, t);
		secondDirty = false;
	}

	private void updateMinute() {
		if (!minuteDirty) return;
		Transform t = Transform.rotation((time.getMinute() + time.getSecond() / 60f) * - PI_BY_30);
		updateVertices(minuteFrontCoords, minuteFrontVertices, 6 * style.height / 8, t);
		updateVertices(minuteBackCoords, minuteBackVertices, style.height / 8, t);
		minuteDirty = false;
	}

	private void updateHour() {
		if (!hourDirty) return;
		Transform t = Transform.rotation((time.getHour() + time.getMinute() / 60f) * - Angles.PI_BY_SIX);
		updateVertices(hourFrontCoords, hourFrontVertices, 5 * style.height / 8, t);
		updateVertices(hourBackCoords, hourBackVertices, style.height / 8, t);
		hourDirty = false;
	}

	private void updateVertices(float[] coords, float[] vertices, float z, Transform t) {
		z += this.z;
		t.transform(coords, scratchCoords);
		int j = 0;
		for (int i = 0; i < coords.length;) {
			vertices[j++] = scratchCoords[i++];
			vertices[j++] = scratchCoords[i++];
			vertices[j++] = z;
		}
	}

	private void computeColors() {
		computeTickColors(style.tickColor1, tickColors1);
		computeTickColors(style.tickColor2, tickColors2);
		computeTickColors(style.backColor1, backColors1);
		computeTickColors(style.backColor2, backColors2);
		computeHandColors(style.secondColor1, secondColors1);
		computeHandColors(style.secondColor2, secondColors2);
		computeHandColors(style.minuteColor1, minuteColors1);
		computeHandColors(style.minuteColor2, minuteColors2);
		computeHandColors(style.hourColor1, hourColors1);
		computeHandColors(style.hourColor2, hourColors2);
		computeHandColors(style.backColor1, handColors1);
		computeHandColors(style.backColor2, handColors2);
	}

	private void computeTickColors(Coloring color, int[] colors) {
		// color 1
		IntBuffer b = IntBuffer.wrap(colors);
		for (int i = 0; i < 12; i++) {
			float s = i == 0 ? 1f : (i-1) / 11f;
			int[] c = color.verticalSample(s).asQuadInts();
			b.put(c);
		}
	}

	private void computeHandColors(Coloring color, int[] colors) {
		IntBuffer b = IntBuffer.wrap(colors);
		// centre
		writeColor(color.getColor(Corner.TL), b);
		writeColor(color.getColor(Corner.BL), b);
		writeColor(color.getColor(Corner.BR), b);
		// right
		writeColor(color.getColor(Corner.TR), b);
		writeColor(color.getColor(Corner.BR), b);
		// left
		writeColor(color.getColor(Corner.TR), b);
		writeColor(color.getColor(Corner.BR), b);
	}

	private void computeTicks() {
		float[] tick = generateTickPoints();
		computeTickVertices(tick, false);
		computeTickVertices(tick, true);
	}

	private void computeHands() {
		computeHands(style.secondShape, 0f, secondFrontCoords, secondTexCoords);
		computeHands(style.secondShape, style.handGrow, secondBackCoords, secondTexCoords);
		computeHands(style.minuteShape, 0f, minuteFrontCoords, minuteTexCoords);
		computeHands(style.minuteShape, style.handGrow, minuteBackCoords, minuteTexCoords);
		computeHands(style.hourShape, 0f, hourFrontCoords, hourTexCoords);
		computeHands(style.hourShape, style.handGrow, hourBackCoords, hourTexCoords);
	}

	private void computeHands(HandShape shape, float grow, float[] coords, float[] texCoords) {
		transfer(generateHandCoords(shape, grow), coords);
		transfer(generateHandTexCoords(shape), texCoords);
	}

	private void computeTickVertices(float[] tick, boolean back) {
		float z = this.z + (back ? style.height / 8 : style.height / 2);
		float[] vertices = back ? backTickVertices : frontTickVertices;
		float baseScale = back ? style.backScale : 1f;

		float[] trans = new float[8];
		int i = 0;
		for (int hour = 0; hour < 12; hour++) {
			float angle = hour * Angles.PI_BY_SIX;
			float scale = baseScale;
			if ((hour % 3) == 0) scale *= style.cardinalScale;
			Transform t = Transform.identity()
					.apply(Transform.scaleAbout(new Point(0f, 1f - style.tickHeight / 2), scale, scale))
					.apply(Transform.scale(style.radius))
					.apply(Transform.rotation(-angle));
			t.transform(tick, trans);
			for (int j = 0; j < 8;) {
				vertices[i++] = trans[j++];
				vertices[i++] = trans[j++];
				vertices[i++] = z;
			}
		}
	}

	private float[] generateTickPoints() {
		return new float[] {
			 style.tickWidthTop    / 2, 1f              ,
			-style.tickWidthTop    / 2, 1f              ,
			-style.tickWidthBottom / 2, 1f - style.tickHeight,
			 style.tickWidthBottom / 2, 1f - style.tickHeight,
			};
	}

	private float[] generateHandCoords(HandShape shape, float grow) {
		float w = shape.width / 2;
		float y0 = - style.radius * shape.overhang;
		float y2 =   style.radius * shape.reach;
		if (grow != 0f) {
			float d = style.radius * grow;
			w += d;
			y0 -= d;
			y2 += d;
		}
		float y1 =   y2 + (y0 - y2) * shape.point;

		return new float[] {
				 0, y0,     0, y1,     0, y2,
				 w, y0,     w, y1,
				-w, y0,    -w, y1,
		};
	}

	private float[] generateHandTexCoords(HandShape shape) {
		float p = shape.point;
		return new float[] {
				0,0,  0,p,  1,1,
				1,0,  1,p,
				1,0,  1,p,
		};
	}

	private final class ClockElement extends Element {

		private final RenderState required;
		private final short[] triangles;
		private final float[] vertices;
		private final float[] texCoords;
		private final int[] fgColors;
		private final PlainParams[] params;
		private final Runnable validator;
		private final int vertexCount;
		private final int triangleCount;

		public ClockElement(RenderState required, short[] triangles, float[] vertices, float[] texCoords, int[] fgColors, int[] bgColors, Runnable validator) {
			this.required = required;
			this.triangles = triangles;
			this.vertices = vertices;
			this.texCoords = texCoords;
			this.fgColors = fgColors;
			this.validator = validator;
			//TODO no guarantee these params match render state mode
			params = PlainParams.creator.create(bgColors.length);
			for (int i = 0; i < bgColors.length; i++) {
				params[i].setColor(bgColors[i]);
			}
			setHandle(new Vertex(0f, 0f, z));
			vertexCount = vertices.length / 3;
			triangleCount = triangles == null ? vertexCount / 3 : triangles.length / 3;
		}

		@Override
		int getVertexCount() {
			return vertexCount;
		}

		@Override
		int getTriangleCount() {
			return triangleCount;
		}

		@Override
		RenderPhase getRenderPhase() {
			return phase;
		}

		@Override
		RenderState getRequiredState() {
			return required;
		}

		@Override
		void appendTo(ElementData data) {
			validator.run();
			// triangles
			if (triangles != null) data.putTriangles(triangles);
			// vertices
			data.vertices.put(vertices);
			// normals & shaders
			FloatBuffer normals = data.normals;
			FloatBuffer shaders = data.shaders;
			for (int i = 0; i < vertexCount; i++) {
				normals.put(NORMAL);
				params[i].writeTo(shaders);
			}
			// texture coords
			data.texCoords.put(texCoords);
			// colors
			data.colors.put(fgColors);
		}

	}

}
