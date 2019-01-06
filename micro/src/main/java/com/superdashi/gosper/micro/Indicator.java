package com.superdashi.gosper.micro;

import com.superdashi.gosper.studio.Canvas;
import com.tomgibara.intgeom.IntDimensions;

// indicators must have fixed dimensions
public abstract class Indicator {

	// prevent implementations outside this package
	Indicator() {
	}

	abstract IntDimensions dimensions();
	abstract int priority();
	abstract long period();
	abstract boolean needsRender();
	abstract void recordRender(Object state);
	abstract boolean needsClearBeforeRender();
	abstract Object render(Canvas canvas);

}
