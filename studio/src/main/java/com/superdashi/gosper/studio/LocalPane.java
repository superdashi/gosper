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

import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

final class LocalPane implements Pane {

	final LocalPanel panel;
	final IntDimensions dimensions;
	private ImageSurface surface; // assigned when validated, nulled to indicate invalidated
	private LocalCanvas canvas; // created when needed, nulled when invalidated
	IntCoords coords;
	int elevation;
	int ordinal;

	LocalPane(LocalPanel panel, IntDimensions dimensions, IntCoords coords, int elevation) {
		this.panel = panel;
		this.dimensions = dimensions;
		this.coords = coords;
		//TODO need a direct accessor for this
		// should be able to directly get coordinates of corner
		// coords = bounds.vectorToMinimumCorner().translatedOrigin();
		this.elevation = elevation;
	}

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	public boolean opaque() {
		return panel.opaque;
	}

	@Override
	public IntCoords coords() {
		return coords;
	}

	@Override
	public IntRect bounds() {
		return IntRect.rectangle(coords, surface.dimensions());
	}

	@Override
	public int elevation() {
		return elevation;
	}

	@Override
	public void moveTo(IntCoords coords) {
		if (coords == null) throw new IllegalArgumentException("null coords");
		checkExists();
		this.coords = coords;
	}

	@Override
	public void elevateTo(int elevation) {
		checkExists();
		this.elevation = elevation;
	}

	@Override
	public LocalCanvas canvas() {
		checkExists();
		checkValid();
		return canvas == null ? canvas = surface.createCanvas() : canvas;
	}

	@Override
	public void attachCopyToGallery(String resourceId) {
		LocalGallery.checkResourceId(resourceId);
		checkExists();
		checkValid();
		panel.composition.studio.gallery.attach(resourceId, surface.immutableCopy());
	}

	@Override
	public boolean invalid() {
		return surface == null;
	}

	ImageSurface surface() {
		assert surface != null;
		return surface;
	}

	void validate(ImageSurface surface) {
		assert surface != null;
		this.surface = surface;
	}

	void invalidate() {
		surface = null;
		if (canvas != null) {
			canvas.destroy();
			canvas = null;
		}
	}

	private void checkExists() {
		if (panel.destroyed()) throw new IllegalStateException("destroyed");
	}

	private void checkValid() {
		if (surface == null) throw new IllegalStateException("invalid");
	}
}
