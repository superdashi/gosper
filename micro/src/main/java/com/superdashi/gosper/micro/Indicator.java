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
