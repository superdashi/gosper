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
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import com.tomgibara.intgeom.IntDimensions;

final class LocalSurfacePool implements SurfacePool {

	static final LocalSurfacePool empty = new LocalSurfacePool();

	private final Set<ImageSurface> surfaces = new HashSet<>();

	void addSurface(ImageSurface surface) {
		assert surface != null;
		if (this == empty) return;
		surfaces.add(surface);
	}

	@Override
	public void disposeOfRemainingSurfaces() {
		surfaces.clear();
	}

	Optional<ImageSurface> obtainSurface(IntDimensions dimensions, boolean opaque) {
		if (surfaces.isEmpty()) return Optional.empty();
		for (Iterator<ImageSurface> i = surfaces.iterator(); i.hasNext(); ) {
			ImageSurface surface = i.next();
			if (surface.dimensions().equals(dimensions) && surface.opaque() == opaque) {
				i.remove();
				return Optional.of(surface);
			}
		}
		return Optional.empty();
	}

}
