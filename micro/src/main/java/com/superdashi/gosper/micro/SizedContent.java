package com.superdashi.gosper.micro;

import com.superdashi.gosper.studio.Canvas;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

interface SizedContent {

	static SizedContent noContent(IntDimensions dimensions) {
		return new SizedContent() {
			@Override public IntDimensions dimensions() { return dimensions; }
			@Override public void render(Canvas canvas, IntRect contentArea) { /* do nothing */ }
		};
	}

	IntDimensions dimensions();

	//TODO this should rely on translating canvas
	void render(Canvas canvas, IntRect contentArea);

}
