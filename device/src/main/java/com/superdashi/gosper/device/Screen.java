/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
