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
