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
