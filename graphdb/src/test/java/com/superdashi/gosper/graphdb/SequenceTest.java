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
package com.superdashi.gosper.graphdb;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.graphdb.Sequence;

public class SequenceTest {

	@Test
	public void testFilter() {
		Sequence<Integer> toTen = () -> IntStream.range(0, 10).iterator();
		Predicate<Integer> isEven = x -> (x & 1) == 0;
		Iterator<Integer> it = toTen.filter(isEven).iterator();
		checkMatch(it, 0,2,4,6,8);
	}

	@Test
	public void testAnd() {
		Sequence<Integer> threesTo12 = () -> IntStream.range(0, 5).map(i -> i * 3).iterator();
		Sequence<Integer> twosTo12 = () -> IntStream.range(0, 7).map(i -> i * 2).iterator();
		Iterator<Integer> it = threesTo12.and(twosTo12).iterator();
		checkMatch(it, 0,6,12);
	}

	@Test
	public void testOr() {
		Sequence<Integer> threesTo12 = () -> IntStream.range(0, 5).map(i -> i * 3).iterator();
		Sequence<Integer> twosTo12 = () -> IntStream.range(0, 7).map(i -> i * 2).iterator();
		Iterator<Integer> it = threesTo12.or(twosTo12).iterator();
		checkMatch(it, 0,2,3,4,6,8,9,10,12);
	}

	private void checkMatch(Iterator<Integer> it, int... ints) {
		for (int i = 0; i < ints.length; i++) {
			Assert.assertTrue("element " + i + " present", it.hasNext());
			Assert.assertEquals(ints[i], (int) it.next());
		}
		Assert.assertFalse(it.hasNext());
	}
}
