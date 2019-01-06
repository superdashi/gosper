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
