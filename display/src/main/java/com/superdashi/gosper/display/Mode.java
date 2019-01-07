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

import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public enum Mode {

	GONE      (true,  false, false),
	PLAIN     (true,  false, false),
	PLAIN_LIT (true,  false, true ),
	MASK      (false, true,  false),
	MASK_LIT  (false, true,  true ),
	TEXT      (false, true,  false),
	TEXT_LIT  (false, true,  true ),
	DISC      (false, true,  false),
	DISC_LIT  (false, true,  true ),
	PLATE     (true,  true,  false),
	PLATE_LIT (true,  true,  true ),
	TRANS     (true,  false, false),
	TRANS_LIT (true,  false, true ),
	FADE      (true,  false, false),
	FADE_LIT  (true,  false, true ),
	BORDER    (true,  false, false),
	BORDER_LIT(true,  false, true ),
	PULSE     (false, true,  false),
	NOISE     (true,  false, false),
	PLASMA    (true,  false, false),
	CAUSTIC   (true,  true,  false),
	CONSOLE   (true,  true,  false);

	private static final Mode[] values = values();
	public static final Store<Mode> unlitModes;
	static {
		int count = 0;
		for (Mode mode : values) {
			if (!mode.lit) count++;
		}
		Mode[] modes = new Mode[count];
		count = 0;
		for (Mode mode : values) {
			if (!mode.lit) modes[count++] = mode;
		}
		unlitModes = StoreType.of(Mode.class).objectsAsStore(modes);
	}

	public final boolean opaque;
	public final boolean twoSided;
	public final boolean lit;

	private Mode(boolean opaque, boolean twoSided, boolean lit) {
		this.opaque = opaque;
		this.twoSided = twoSided;
		this.lit = lit;
	}

	public static Mode valueOf(int ordinal) {
		if (ordinal < 0) throw new IllegalArgumentException("negative ordinal");
		if (ordinal >= values.length) throw new IllegalArgumentException("negative ordinal");
		return values[ordinal];
	}

	public Mode lit() {
		if (lit) return this;
		switch (this) {
		case PLAIN: return PLAIN_LIT;
		case MASK : return MASK_LIT ;
		case TEXT : return TEXT_LIT ;
		case DISC : return DISC_LIT ;
		case PLATE: return PLATE_LIT;
		case TRANS: return TRANS_LIT;
		case FADE : return FADE_LIT;
		default: return this;
		}
	}

	public Mode unlit() {
		if (!lit) return this;
		switch (this) {
		case PLAIN_LIT: return PLAIN;
		case MASK_LIT : return MASK ;
		case TEXT_LIT : return TEXT ;
		case DISC_LIT : return DISC ;
		case PLATE_LIT: return PLATE;
		case TRANS_LIT: return TRANS;
		case FADE_LIT : return FADE;
		default: return this;
		}
	}

	public Mode lit(boolean lit) {
		return lit ? lit() : unlit();
	}
}
