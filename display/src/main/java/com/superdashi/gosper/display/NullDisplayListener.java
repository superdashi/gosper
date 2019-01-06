package com.superdashi.gosper.display;

class NullDisplayListener implements DisplayListener {

	private static final NullDisplayListener instance = new NullDisplayListener();
	static NullDisplayListener instance() { return instance; }

	private NullDisplayListener() {};

	@Override public void displayStarted() { }
	@Override public void displayStopped() { }
	@Override public void newContext(DisplayContext context) { }
	@Override public void update() { }

}
