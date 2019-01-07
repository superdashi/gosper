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
package com.superdashi.gosper.core;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.config.ConfigTarget;

public class ClockConfig implements ConfigTarget {

	public static class HandShape {

		public final float width;
		public final float overhang;
		public final float reach;
		public final float point;

		HandShape(float width, float overhang, float reach, float point) {
			this.width = width;
			this.overhang = overhang;
			this.reach = reach;
			this.point = point;
		}

	}

	public static final boolean DEFAULT_RENDER_FACE = true;
	public static final boolean DEFAULT_RENDER_BACK = true;
	public static final Coloring DEFAULT_FACE_COLOR1 = Coloring.flat(0x80404040);
	public static final Coloring DEFAULT_FACE_COLOR2 = Coloring.flat(Argb.WHITE);
	public static final Coloring DEFAULT_BACK_COLOR1 = Coloring.flat(Argb.BLACK);
	public static final Coloring DEFAULT_BACK_COLOR2 = Coloring.NONE;
	public static final Coloring DEFAULT_TICK_COLOR1 = Coloring.flat(Argb.WHITE);
	public static final Coloring DEFAULT_TICK_COLOR2 = Coloring.NONE;
	public static final Coloring DEFAULT_SECOND_COLOR1 = Coloring.flat(0xffff0000);
	public static final Coloring DEFAULT_SECOND_COLOR2 = Coloring.NONE;
	public static final Coloring DEFAULT_MINUTE_COLOR1 = Coloring.flat(Argb.WHITE);
	public static final Coloring DEFAULT_MINUTE_COLOR2 = Coloring.NONE;
	public static final Coloring DEFAULT_HOUR_COLOR1 = Coloring.flat(Argb.WHITE);
	public static final Coloring DEFAULT_HOUR_COLOR2 = Coloring.NONE;
	public static final float DEFAULT_RADIUS = 0.5f;
	public static final float DEFAULT_CARD_SCALE = 1.0f;
	public static final float DEFAULT_BACK_SCALE = 1.2f;
	public static final float DEFAULT_HAND_GROW = 0.01f;
//	public static final float DEFAULT_TICK_WIDTH_TOP = 0.06f;
//	public static final float DEFAULT_TICK_WIDTH_BOTTOM = 0.04f;
//	public static final float DEFAULT_TICK_HEIGHT = 0.1f;
//	public static final float DEFAULT_TICK_WIDTH_TOP = 0.1f;
//	public static final float DEFAULT_TICK_WIDTH_BOTTOM = 0.00f;
//	public static final float DEFAULT_TICK_HEIGHT = 0.1f;
	public static final float DEFAULT_TICK_WIDTH_TOP = 0.07f;
	public static final float DEFAULT_TICK_WIDTH_BOTTOM = 0.07f;
	public static final float DEFAULT_TICK_HEIGHT = 0.12f;
	public static final float DEFAULT_HEIGHT = 0.001f;
	public static final HandShape DEFAULT_SECOND_SHAPE = new HandShape(0.01f, 0.20f, 0.95f, 0.00f);
	public static final HandShape DEFAULT_MINUTE_SHAPE = new HandShape(0.04f, 0.09f, 0.7f, 0.04f);
	public static final HandShape DEFAULT_HOUR_SHAPE = new HandShape(0.06f, 0.12f, 0.55f, 0.00f);
	public static final boolean DEFAULT_SECOND_SWEEP = true;

	public boolean renderBack = DEFAULT_RENDER_BACK;
	public boolean renderFace = DEFAULT_RENDER_FACE;
	public Coloring faceColor1 = DEFAULT_FACE_COLOR1;
	public Coloring faceColor2 = DEFAULT_FACE_COLOR2;
	public Coloring backColor1 = DEFAULT_BACK_COLOR1;
	public Coloring backColor2 = DEFAULT_BACK_COLOR2;
	public Coloring tickColor1 = DEFAULT_TICK_COLOR1;
	public Coloring tickColor2 = DEFAULT_TICK_COLOR2;
	public Coloring secondColor1 = DEFAULT_SECOND_COLOR1;
	public Coloring secondColor2 = DEFAULT_SECOND_COLOR2;
	public Coloring minuteColor1 = DEFAULT_MINUTE_COLOR1;
	public Coloring minuteColor2 = DEFAULT_MINUTE_COLOR2;
	public Coloring hourColor1 = DEFAULT_HOUR_COLOR1;
	public Coloring hourColor2 = DEFAULT_HOUR_COLOR2;
	public float tickWidthTop = DEFAULT_TICK_WIDTH_TOP;
	public float tickWidthBottom = DEFAULT_TICK_WIDTH_BOTTOM;
	public float tickHeight = DEFAULT_TICK_HEIGHT;
	public float radius = DEFAULT_RADIUS;
	public float cardinalScale = DEFAULT_CARD_SCALE;
	public float backScale = DEFAULT_BACK_SCALE;
	public float handGrow = DEFAULT_HAND_GROW;
	public float height = DEFAULT_HEIGHT;
	public HandShape secondShape = DEFAULT_SECOND_SHAPE;
	public HandShape minuteShape = DEFAULT_MINUTE_SHAPE;
	public HandShape hourShape = DEFAULT_HOUR_SHAPE;
	public boolean secondSweep = DEFAULT_SECOND_SWEEP;

	public void adopt(ClockConfig that) {
		this.renderBack = that.renderBack;
		this.renderFace = that.renderFace;
		this.faceColor1 = that.faceColor1;
		this.faceColor2 = that.faceColor2;
		this.backColor1 = that.backColor1;
		this.backColor2 = that.backColor2;
		this.tickColor1 = that.tickColor1;
		this.tickColor2 = that.tickColor2;
		this.secondColor1 = that.secondColor1;
		this.secondColor2 = that.secondColor2;
		this.minuteColor1 = that.minuteColor1;
		this.minuteColor2 = that.minuteColor2;
		this.hourColor1 = that.hourColor1;
		this.hourColor2 = that.hourColor2;
		this.tickWidthTop = that.tickWidthTop;
		this.tickWidthBottom = that.tickWidthBottom;
		this.tickHeight = that.tickHeight;
		this.radius = that.radius;
		this.cardinalScale = that.cardinalScale;
		this.backScale = that.backScale;
		this.handGrow = that.handGrow;
		this.height = that.height;
		this.secondShape = that.secondShape;
		this.minuteShape = that.minuteShape;
		this.hourShape = that.hourShape;
		this.secondSweep = that.secondSweep;
	}

}
