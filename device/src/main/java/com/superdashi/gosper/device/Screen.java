package com.superdashi.gosper.device;

import com.superdashi.gosper.studio.Composition;
import com.tomgibara.intgeom.IntDimensions;

//TODO try to hide system-level methods
public interface Screen {

	// characteristics
	IntDimensions dimensions();

	//TODO add invertible and contrastible to device spec? also resetable?
	// appearance
	boolean opaque();

	boolean inverted();
	void inverted(boolean inverted);

	float contrast();
	void contrast(float contrast);

	float brightness();
	void brightness(float brightness);

	int ambience();
	void ambience(int color);

	// lifecycle
	void begin() throws DeviceException;
	void end() throws DeviceException;
	void reset() throws DeviceException;

	// modification
	void clear(); // clears stored image data
	void composite(Composition composition);

	// visual update
	//TODO get rid of this?
	void blank(); // clears stored image data and transmits it to the screen
	void update(); // transmits stored image data to screen
}
