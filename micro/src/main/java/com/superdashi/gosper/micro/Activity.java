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

@FunctionalInterface
public interface Activity {

	//TODO defining State here makes imports messy, move
	enum State {
		CONSTRUCTED, INITIALIZED, OPEN, ACTIVE;
	}

	//TODO binding device to context prevents device changes during activity lifetime
	default void init() {}

	//TODO should separate out opening from layout so that re-layout can be requested
	void open(DataInput savedState);

	default void activate() {}

	default void passivate() {}

	default void close(DataOutput state) {}

	default void destroy() {}

	// may return null to refuse relaunch, but should this be explicitly supported?
	default State relaunch(DataInput launchData) {
		return State.CONSTRUCTED;
	}

}
