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
package com.superdashi.gosper.item;

import java.util.Optional;

public enum ScreenColor {

	OTHER, // custom or not applicable
	MONO,  // black and white
	GRAY,  // greyscale
	COLOR; // typically, but not necessarily full color

	private static final ScreenColor[] values = values();

	public static ScreenColor valueOf(int ordinal) {
		if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid ordinal");
		return values[ordinal];
	}

	public Optional<ScreenColor> fallback() {
		switch (this) {
		case COLOR: return Optional.of(GRAY);
		case GRAY : return Optional.of(MONO);
		case MONO : return Optional.empty();
		case OTHER: return Optional.of(COLOR);
		default   : return Optional.empty();
		}
	}
}
