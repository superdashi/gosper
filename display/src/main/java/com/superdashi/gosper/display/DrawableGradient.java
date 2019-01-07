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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;

import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.layout.Position;
import com.tomgibara.intgeom.IntRect;

public class DrawableGradient implements Drawable {

	private enum Type {
		FLAT,
		LINEAR,
		RADIAL,
		OTHER;
	}

	public enum Gradient {

		IN_ONLY(Type.FLAT),
		OUT_ONLY(Type.FLAT),

		LEFT_TO_RIGHT(Type.LINEAR),
		RIGHT_TO_LEFT(Type.LINEAR),
		TOP_TO_BOTTOM(Type.LINEAR),
		BOTTOM_TO_TOP(Type.LINEAR),

		CENTER_TO_OUT(Type.LINEAR),
		OUT_TO_CENTER(Type.LINEAR),
		MIDDLE_TO_OUT(Type.LINEAR),
		OUT_TO_MIDDLE(Type.LINEAR),

		IN_TO_OUT(Type.RADIAL),
		OUT_TO_IN(Type.RADIAL);

		final Type type;

		private Gradient(Type type) {
			this.type = type;
		}
	}

	public static DrawableGradient blackToWhite(Gradient gradient) {
		if (gradient == null) throw new IllegalArgumentException("null gradient");
		return new DrawableGradient(gradient, Color.BLACK, Color.WHITE, true);
	}

	public static DrawableGradient whiteToBlack(Gradient gradient) {
		if (gradient == null) throw new IllegalArgumentException("null gradient");
		return new DrawableGradient(gradient, Color.WHITE, Color.BLACK, true);
	}

	private final Gradient gradient;
	private final Color inColor;
	private final Color outColor;
	private final boolean singleChannel;

	private DrawableGradient(Gradient gradient, Color inColor, Color outColor, boolean singleChannel) {
		this.gradient = gradient;
		this.inColor = inColor;
		this.outColor = outColor;
		this.singleChannel = singleChannel;
	}

	public Gradient getGradient() {
		return gradient;
	}

	public int getInColor() {
		return inColor.getRGB();
	}

	public int getOutColor() {
		return outColor.getRGB();
	}

	@Override
	public void drawTo(Graphics2D g, IntRect rect, Position pos) {
		g.setPaint(paint(rect));
		g.fillRect(rect.minX, rect.minY, rect.width(), rect.height());
	}

	@Override
	public int getNumberOfChannels() {
		return singleChannel ? 1 : 4;
	}

	@Override
	public Resolution getResolution() {
		return Resolution.unlimited();
	}

	private Paint paint(IntRect rect) {
		switch(gradient) {

		case IN_ONLY:
			return inColor;
		case OUT_ONLY:
			return outColor;

		case LEFT_TO_RIGHT:
			return new GradientPaint(rect.minX, rect.minY, inColor, rect.minX + rect.width(), rect.minY, outColor, true);
		case RIGHT_TO_LEFT:
			return new GradientPaint(rect.minX + rect.width(), rect.minY, inColor, rect.minX, rect.minY, outColor, true);
		case TOP_TO_BOTTOM:
			return new GradientPaint(rect.minX, rect.minY, inColor, rect.minX, rect.minY + rect.height(), outColor, true);
		case BOTTOM_TO_TOP:
			return new GradientPaint(rect.minX, rect.minY + rect.height(), inColor, rect.minX, rect.minY, outColor, true);

		case CENTER_TO_OUT:
			return new GradientPaint(rect.minX, rect.minY, outColor, rect.minX + rect.width() * 0.5f, rect.minY, inColor, true);
		case OUT_TO_CENTER:
			return new GradientPaint(rect.minX, rect.minY, inColor, rect.minX + rect.width() * 0.5f, rect.minY, outColor, true);
		case MIDDLE_TO_OUT:
			return new GradientPaint(rect.minX, rect.minY, outColor, rect.minX, rect.minY + rect.height() * 0.5f, inColor, true);
		case OUT_TO_MIDDLE:
			return new GradientPaint(rect.minX, rect.minY, inColor, rect.minX, rect.minY + rect.height() * 0.5f, outColor, true);

		case IN_TO_OUT:
			return new RadialGradientPaint(rect.minX + rect.width() * 0.5f, rect.minY + rect.height() * 0.5f, Math.min(rect.width(), rect.height()) * 0.5f, new float[] {0f,1f}, new Color[] {inColor, outColor});
		case OUT_TO_IN:
			return new RadialGradientPaint(rect.minX + rect.width() * 0.5f, rect.minY + rect.height() * 0.5f, Math.min(rect.width(), rect.height()) * 0.5f, new float[] {0f,1f}, new Color[] {outColor, inColor});
		default:
			return null;
		}
	}
}
