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
package com.superdashi.gosper.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.superdashi.gosper.logging.LogIdentity;

public class LogIdentityTest {

	@Test
	public void testOrdering() {
		LogIdentity idx = LogIdentity.create("x");
		LogIdentity idy = LogIdentity.create("y");
		LogIdentity idxa = idx.child("a");
		assertFalse(idx.equals(idy));
		assertFalse(idx.isSubIdentityOf(idy));
		assertFalse(idx.isSuperIdentityOf(idy));
		assertFalse(idx.equals(idxa));
		assertFalse(idx.isSubIdentityOf(idxa));
		assertTrue(idx.isSuperIdentityOf(idxa));
		assertTrue(idxa.isSubIdentityOf(idx));
		assertFalse(idxa.isSubIdentityOf(idy));
	}

	@Test
	public void testNames() {
		LogIdentity id = LogIdentity.create("a", "b", "c");
		assertEquals(Arrays.asList(new String[] {"a", "b", "c"}), id.names());
	}

	@Test
	public void testChild() {
		assertEquals(LogIdentity.create("a", "b"), LogIdentity.create("a").child("b"));
	}

	@Test
	public void testToString() {
		assertEquals("a/b", LogIdentity.create("a", "b").toString());
		assertEquals("a/b/c", LogIdentity.create("a", "b", "c").toString());
	}

	@Test
	public void testHashCode() {
		assertEquals(LogIdentity.create("a").hashCode(), LogIdentity.create("a").hashCode());
		assertEquals(LogIdentity.create("a", "b").hashCode(), LogIdentity.create("a").child("b").hashCode());
	}
}
