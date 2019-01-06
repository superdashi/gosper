package com.superdashi.gosper.display;

import com.superdashi.gosper.core.Design;

public interface DisplayConduit {

	void setDisplayListener(DisplayListener listener);

	// OPTIONS
	// 1. method to 'runLater' on render thread
	// 2. use synchronization
	// 3. Lock object

	// pros/cons
	// 1. makes it difficult to handle side effects / least impact on render thread
	// 3. more heavyweight than sync / maximum freedom for controller
	// must be called with very quick tasks
	void sync(Runnable r);

	void setDesign(Design design);

	void addDisplay(ElementDisplay display);
}
