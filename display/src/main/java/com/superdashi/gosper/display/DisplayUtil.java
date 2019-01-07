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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Optional;

import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.layout.Position;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntRect;

public final class DisplayUtil {

	private static final float[] ZEROS = new float[512];

	public static final boolean DEBUG = false;

	public static final Coloring testColoring = Coloring.corners(0x80ff0000, 0x800000ff, 0x80806600, 0x80000000);

	public static final Font defaultFont;
	static {
		try {
			defaultFont = Font.createFont(Font.TRUETYPE_FONT, DashiRendering.class.getClassLoader().getResourceAsStream("Roboto-Bold.ttf"));
		} catch (FontFormatException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <H> Optional<DynamicAtlas<H>.Updater> renderText(DynamicAtlas<H> atlas, Font font, float edge, String text, H hint) {
		Graphics2D graphics = atlas.getGraphics();
		FontMetrics metrics = graphics.getFontMetrics(font);
		Rectangle2D bounds = metrics.getStringBounds(text, graphics);
		int w = (int) Math.ceil((float) bounds.getWidth() + 2 * edge);
		int h = (int) Math.ceil((float) bounds.getHeight() + 2 * edge);
		Optional<DynamicAtlas<H>.Updater> updater = atlas.add(w, h, hint);
		if (updater.isPresent()) {
			Graphics2D g = updater.get().getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (DEBUG) {
				g.setColor(Color.GRAY);
				g.fillRect(0, 0, w, h);
				g.setColor(Color.DARK_GRAY);
				g.fillRect(2, 2, w-4, h-4);
			}

			g.setFont(font);
			GlyphVector glyph = font.createGlyphVector(g.getFontRenderContext(), text);
			g.translate(edge, edge + metrics.getAscent());
			Shape shape = glyph.getOutline();

			g.setStroke(new BasicStroke(edge * 2));
			g.setColor(Color.GRAY);
			g.draw(shape);

			g.setColor(Color.WHITE);
			g.fill(shape);
		}
		return updater;
	}

	//TODO honour position
	public static void renderText(Graphics2D g, Font font, Color inner, Color outer, float edge, String text, Position position) {
		if (font == null) {
			font = g.getFont();
		} else {
			g.setFont(font);
		}
		FontMetrics metrics = g.getFontMetrics();
		GlyphVector glyph = font.createGlyphVector(g.getFontRenderContext(), text);
		g.translate(edge, edge + metrics.getAscent());
		Shape shape = glyph.getOutline();

		g.setStroke(new BasicStroke(edge * 2));
		g.setColor(outer);
		g.draw(shape);

		g.setColor(inner);
		g.fill(shape);
	}

	public static float[] nCopies(int n, float... values) {
		int length = values.length;
		float[] copies = new float[length * n];
		for (int j = 0, i = 0; i < n; i++) {
			for (int k = 0; k < length; k++) {
				copies[j++] = values[k];
			}
		}
		return copies;
	}

	public static float[] projectToZ(float[] coords, float z) {
		float[] vertices = new float[coords.length / 2 * 3];
		projectToZ(coords, z, 0, vertices);
		return vertices;
	}

	public static int projectToZ(float[] coords, float z, int offset, float[] vertices) {
		for (int i = 0; i < coords.length;) {
			vertices[offset++] = coords[i++];
			vertices[offset++] = coords[i++];
			vertices[offset++] = z;
		}
		return offset;
	}

	public static void putZeros(FloatBuffer b, int length) {
		for (; length >= 512; length -= 512) b.put(ZEROS);
		if (length > 0) b.put(ZEROS, 0, length);
	}

	public static float[] transformedCoords(Transform t, float[] coords) {
		if (t.isIdentity()) return coords;
		coords = coords.clone();
		t.transform(coords);
		return coords;
	}

	public static Rect toRect(IntRect rect) {
		return Rect.atPoints(rect.minX, rect.minY, rect.maxX, rect.maxY);
	}
}
