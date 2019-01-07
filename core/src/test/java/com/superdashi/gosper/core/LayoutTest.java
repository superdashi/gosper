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
package com.superdashi.gosper.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import com.superdashi.gosper.core.Layout;
import com.superdashi.gosper.core.Layout.Merger;
import com.superdashi.gosper.core.Layout.Place;
import com.tomgibara.storage.Store;


public class LayoutTest {

	@Test
	public void testSimpleSplit() {
		Layout layout = Layout.entire(2, 2);
		checkEntire(layout);
		assertEquals(2, layout.splitters.size());

		Layout s0 = layout.splitters.get(0).split();
		assertEquals(2, s0.splitters.size());
		checkEntire(s0);

		Layout sa = s0.splitters.get(0).split().splitters.get(0).split();

		Layout s1 = layout.splitters.get(1).split();
		assertEquals(2, s1.splitters.size());
		checkEntire(s1);

		Layout sb = s1.splitters.get(0).split().splitters.get(0).split();

		assertEquals(sa, sb);
	}

	@Test
	public void testSimpleMerge() {
		Layout l = Layout.entire(2, 2);
		Layout s = l.splitters.get(0).split();
		assertEquals(1, s.mergers.size());
		Layout m = s.mergers.get(0).merge();
		assertEquals(l, m);
	}

	@Test
	public void testLargeMerging() {
		Layout l = Layout.grid(Layout.MAX_ROWS, Layout.MAX_COLS);
		Random r = new Random(0);
		while (true) {
			Store<Merger> ms = l.mergers;
			int s = ms.size();
			if (s == 0) break;
			l = ms.get(r.nextInt(s)).merge();
		}
		assertEquals(Layout.entire(Layout.MAX_ROWS, Layout.MAX_COLS), l);
	}

	private static void checkEntire(Layout ly) {
		for (int y = 0; y < ly.rows; y++) {
			for (int x = 0; x < ly.cols; x++) {
				Place p = ly.placeContaining(x, y);
				assertTrue(p.area.contains(x, y));
			}
		}
	}

}
