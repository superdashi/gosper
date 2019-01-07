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

import java.util.HashSet;
import java.util.Set;

import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

final class LocalPanel implements Panel {

	final LocalComposition composition;
	final IntDimensions dimensions;
	final boolean opaque;
	private final Set<LocalPane> panes = new HashSet<>();
	private ImageSurface surface; // nulled to indicate invalidated
	boolean destroyed = false;

	LocalPanel(LocalComposition composition, IntDimensions dimensions, boolean opaque) {
		this.composition = composition;
		this.dimensions = dimensions;
		this.opaque = opaque;
	}

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	public boolean opaque() {
		return opaque;
	}

	@Override
	public LocalPane createPane(IntRect bounds, IntCoords coords, int elevation) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		if (!dimensions.toRect().containsRect(bounds)) throw new IllegalArgumentException("bounds exceed panel dimensions");
		if (coords == null) throw new IllegalArgumentException("null coords");
		LocalPane pane = new LocalPane(this, bounds.dimensions(), coords, elevation);
		if (surface != null) pane.validate(surface.view(bounds));
		composition.add(pane);
		return pane;
	}

	@Override
	public boolean invalid() {
		return surface == null;
	}

	@Override
	public boolean destroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (!destroyed) {
			// remove panes from composition, and invalidate them
			for (LocalPane pane : panes) {
				composition.remove(pane);
				pane.invalidate();
			}
			// clear lingering references to them
			panes.clear();
			// remove ourselves from the composition
			composition.remove(this);
			// eliminate reference to our surface
			surface = null;
			// and record ourselves as destroyed
			destroyed = true;
		}
	}

	void autoValidate(LocalSurfacePool pool) {
		if (surface == null) {
			surface = pool.obtainSurface(dimensions, opaque).orElseGet(() -> ImageSurface.sized(dimensions, opaque));
			//TODO can we defer this erase until we know we need it
			// specifically we don't need to erase if panes cover the whole surface
			surface.createCanvas().erase().destroy();
			validatePanes();
		}
	}

	void validate(ImageSurface surface) {
		assert this.surface == null;
		assert surface != null;
		assert surface.dimensions().equals(dimensions);
		assert surface.opaque() == opaque;
		this.surface = surface;
	}

	ImageSurface invalidate() {
		if (surface == null) return null; // already invalidated
		invalidatePanes();
		ImageSurface s = surface;
		surface = null;
		return s;
	}

	private void validatePanes() {
		for (LocalPane pane : panes) {
			pane.validate(surface.view(pane.bounds()));
		}
	}

	private void invalidatePanes() {
		for (LocalPane pane : panes) {
			pane.invalidate();
		}
	}
}
