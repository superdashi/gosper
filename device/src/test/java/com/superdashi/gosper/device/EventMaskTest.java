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
package com.superdashi.gosper.device;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;

public class EventMaskTest {

	@Test
	public void testAnyKeyDown() {
		Assert.assertTrue(EventMask.anyKeyDown().test(Event.newKeyEvent(Event.KEY_A, true)));
		Assert.assertFalse(EventMask.anyKeyDown().test(Event.newKeyEvent(Event.KEY_A, false)));
	}
}
