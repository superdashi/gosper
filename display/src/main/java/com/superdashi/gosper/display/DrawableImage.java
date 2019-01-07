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
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.regex.Pattern;

import com.tomgibara.intgeom.IntRect;
import com.superdashi.gosper.core.Cache;
import com.superdashi.gosper.core.CacheException;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.layout.Position;
import com.tomgibara.geom.awt.AWTUtil;

public abstract class DrawableImage implements Drawable {

	private static final Pattern COLOR_PATTERN = Pattern.compile("[0-9a-fA-F]{6}");

	private static Color color(String str) {
		if (!COLOR_PATTERN.matcher(str).matches()) throw new IllegalArgumentException("color did not match " + COLOR_PATTERN.pattern());
		return new Color(Integer.parseInt(str, 16));
	}

	public static DrawableImage of(Image image, Cache cache) {
		if (image == null) throw new IllegalArgumentException("null image");
		switch (image.type()) {
		case INTERNAL:
			URI uri = image.uri();
			String path = uri.getPath();
			if (path.startsWith("/color/")) {
				String colorStr = path.substring(7);
				//TODO make common with style parsing
				try {
					Color color = color(colorStr);
					return new ColorImage(image, color);
				} catch (IllegalArgumentException e) {
					DashiLog.warn("Invalid color definition: {0}", e, colorStr);
				}
			}
			DashiLog.debug("Unsupported dashi image definition: {0}", path);
			return new ErrorImage(image);
		case HTTP:
			try {
				return new HttpImage(image, cache);
			} catch (CacheException e) {
				DashiLog.debug("Failed to retrieve image: {0}", e, image.uri());
				return new ErrorImage(image);
			}
		default:
			DashiLog.debug("Unsupported image type: {0}", image.type());
			return new ErrorImage(image);
		}
	}

	private final Image image;

	private DrawableImage(Image image) {
		this.image = image;
	}

	public Image image() {
		return image;
	}

	private static class HttpImage extends DrawableImage {

		private final BufferedImage buffered;
		private final Resolution resolution;

		private HttpImage(Image image, Cache cache) {
			super(image);
			buffered = cache.cachedImage(image.uri());
			resolution = Resolution.sized(buffered.getWidth(), buffered.getHeight());
		}

		@Override
		public void drawTo(Graphics2D g, IntRect rect, Position pos) {
			AffineTransform t = g.getTransform();
			AffineTransform tx = AWTUtil.toAffineTransform( pos.transform(resolution.toRect().get(), DisplayUtil.toRect(rect)) );
			g.transform(tx);
			g.drawImage(buffered, 0, 0, null);
			g.setTransform(t);
		}

		@Override
		public int getNumberOfChannels() {
			return buffered.getColorModel().getNumComponents();
		}

		@Override
		public Resolution getResolution() {
			return resolution;
		}

	}

	private static class ErrorImage extends DrawableImage {

		private ErrorImage(Image image) {
			super(image);
		}

		@Override
		public void drawTo(Graphics2D g, IntRect rect, Position pos) {
			// TODO Auto-generated method stub
		}

		@Override
		public int getNumberOfChannels() {
			return 4;
		}

		@Override
		public Resolution getResolution() {
			return Resolution.unlimited();
		}

	}

	private static class ColorImage extends DrawableImage {

		private final Color color;

		private ColorImage(Image image, Color color) {
			super(image);
			this.color = color;
		}

		@Override
		public int getNumberOfChannels() {
			return 4;
		}

		@Override
		public Resolution getResolution() {
			return Resolution.unlimited();
		}

		@Override
		public void drawTo(Graphics2D g, IntRect rect, Position pos) {
			g.setColor(color);
			g.fillRect(rect.minX, rect.minY, rect.width(), rect.height());
		}

	}
}
