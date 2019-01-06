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
