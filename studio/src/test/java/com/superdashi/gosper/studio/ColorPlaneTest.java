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
package com.superdashi.gosper.studio;

import org.junit.Test;

import com.superdashi.gosper.studio.ColorPlane;
import com.tomgibara.intgeom.IntRect;

public class ColorPlaneTest extends RenderTest {

	@Test
	public void testPrimaryColors() {
		IntRect bounds = IntRect.bounded(10, 10, 60, 40);

		recordResult(new ColorPlane(0xffff0000).frame(bounds), "red");
		recordResult(new ColorPlane(0xff00ff00).frame(bounds), "green");
		recordResult(new ColorPlane(0xff0000ff).frame(bounds), "blue");

		recordResult(new ColorPlane(0x80ff0000).frame(bounds), "red50");
		recordResult(new ColorPlane(0x8000ff00).frame(bounds), "green50");
		recordResult(new ColorPlane(0x800000ff).frame(bounds), "blue50");
	}
}
