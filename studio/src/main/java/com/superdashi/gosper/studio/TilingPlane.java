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
package com.superdashi.gosper.studio;

import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public class TilingPlane implements Plane {

	final Frame tile;
	final int width;
	final int height;

	public TilingPlane(Frame tile) {
		if (tile == null) throw new IllegalArgumentException("null tile");
		this.tile = tile;
		IntDimensions dimensions = tile.dimensions();
		this.width = dimensions.width;
		this.height = dimensions.height;
	}

	@Override
	public boolean opaque() {
		return tile.opaque();
	}

	@Override
	public int readPixel(int x, int y) {
		return tile.readPixel(x % width, y % height);
	}

	@Override
	public Frame frame(IntRect bounds) {
		return Plane.super.frame(bounds);
	}

	@Override
	public Shader asShader() {
		return new Shader() {
			@Override
			Paint createPaint() {
				//TODO problem here is that the paint gets a snapshot of tile, but is then cached
				//TODO need to avoid copying when frame is a truly immutable image
				//TODO this is ugly
				BufferedImage image;
				if (tile instanceof ImageSurface) {
					image = ((ImageSurface) tile).image;
				} else {
					image = tile.toImage();
				}
				return new TexturePaint(image, new Rectangle2D.Float(0, 0, width, height));
			}
		};
	}

	static final class TilingFrame extends PlaneFrame<TilingPlane> {

		private TilingFrame(TilingPlane plane, IntRect bounds) {
			super(plane, bounds);
		}

	}
}
