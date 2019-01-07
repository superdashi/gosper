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
