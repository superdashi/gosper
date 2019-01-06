package com.superdashi.gosper.studio;

import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public interface Pane {

	IntDimensions dimensions();

	boolean opaque();

	IntCoords coords();

	int elevation();

	IntRect bounds();

	void moveTo(IntCoords coords);

	void elevateTo(int height);

	Canvas canvas();

	void attachCopyToGallery(String resourceId);

	boolean invalid();

}
