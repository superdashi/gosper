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

import com.superdashi.gosper.studio.Mask;
import com.tomgibara.intgeom.IntDimensions;

public class MaskTest extends RenderTest {

	@Test
	public void testMaskOverBytes() {
		int w = 60;
		int h = 30;
		byte[] bytes = new byte[w*h];
		int i = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				bytes[i++] = (byte) (128 + 127 * Math.sin((x*x) / (y+30.0)));
			}
		}
		Mask mask = Mask.overByteGray(IntDimensions.of(w, h), bytes);
		recordResult(mask, "maskOverBytes");
	}
}
