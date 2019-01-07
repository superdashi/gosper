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

import com.tomgibara.geom.core.Rect;

public class Situation {

	public final RenderPhase phase;
	public final Rect rect;
	public final float z;

	public Situation(RenderPhase phase, Rect rect, float z) {
		if (phase == null) throw new IllegalArgumentException("null phase");
		if (rect == null) throw new IllegalArgumentException("null rect");
		this.phase = phase;
		this.rect = rect;
		this.z = z;
	}
}
