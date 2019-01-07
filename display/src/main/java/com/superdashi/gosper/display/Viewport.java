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

import com.jogamp.opengl.GL;
import com.superdashi.gosper.core.Resolution;
import com.tomgibara.intgeom.IntRect;

public final class Viewport {

	public static Viewport from(IntRect area) {
		if (area == null) throw new IllegalArgumentException("null area");
		return new Viewport(area);
	}

	public final IntRect area;
	public final Resolution resolution;

	private Viewport(IntRect area) {
		this.area = area;
		resolution = Resolution.ofRect(area);
	}

	public void shape(GL gl) {
		gl.glViewport(area.minX, area.minY, area.maxX - area.minX, area.maxY - area.minY);
	}


	// object methods

	@Override
	public int hashCode() {
		return area.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Viewport)) return false;
		Viewport that = (Viewport) obj;
		return this.area.equals(that.area);
	}

	@Override
	public String toString() {
		return area.toString();
	}
}
