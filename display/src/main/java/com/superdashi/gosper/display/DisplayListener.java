package com.superdashi.gosper.display;

public interface DisplayListener {

	void displayStarted();

	void displayStopped();

	void newContext(DisplayContext context);

	void update();

}
