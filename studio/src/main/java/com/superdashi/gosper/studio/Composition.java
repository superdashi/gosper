package com.superdashi.gosper.studio;

import com.tomgibara.intgeom.IntDimensions;

public interface Composition extends Destroyable {

	void compositeTo(Target target);

	Panel createPanel(IntDimensions dimensions, boolean opaque);

	void releaseResources();

	void captureResources();

}
