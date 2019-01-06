package com.superdashi.gosper.studio;

import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public interface Panel extends Destroyable {

	IntDimensions dimensions();

	boolean opaque();

	Pane createPane(IntRect bounds, IntCoords coords, int elevation);

	default Pane createEntirePane(IntCoords coords, int elevation) {
		return createPane(dimensions().toRect(), coords, elevation);
	}

	boolean invalid();

}
