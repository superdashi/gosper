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
package com.superdashi.gosper.util;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyIterator;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.util.Iterators;

public class IteratorsTest {

	private static <E> E[] arr(E...objects) {
		return objects;
	}

	private static <E> Iterator<E> it(E... objects) {
		return Arrays.stream(arr(objects)).iterator();
	}

	private static <E> Iterator<E> empty() {
		return emptyIterator();
	}

	private static <E> Iterator<E> single(E e) {
		return singleton(e).iterator();
	}

	private static <E> List<E> list(E...es) {
		return new ArrayList<>(Arrays.asList(es));
	}

	private static <E> Iterator<E> combine(List<E>... lists) {
		return Iterators.flatten( Iterators.map(Arrays.asList(lists).iterator(), l -> l.iterator()) );
	}

	@Test
	public void testEmpties() {
		Iterator<Object> i = emptyIterator();
		Iterator<Object> t = Iterators.flatten(it(i,i,i,i));
		Assert.assertFalse(t.hasNext());
	}

	@Test
	public void testEmpty() {
		Iterator<Object> i = emptyIterator();
		Iterator<Object> t = Iterators.flatten(it(i));
		Assert.assertFalse(t.hasNext());
	}

	@Test
	public void testSingle() {
		Object obj = new Object();
		Iterator<Object> i = single(obj);
		testSingleton(obj, i);
	}

	@Test
	public void testSingles() {
		Iterator<String> a = singleton("A").iterator();
		Iterator<String> b = singleton("B").iterator();
		Iterator<String> c = singleton("C").iterator();
		Iterator<String> t = Iterators.flatten(it(a,b,c));
		assertTrue(t.hasNext());
		assertSame("A", t.next());
		assertSame("B", t.next());
		assertSame("C", t.next());
		assertFalse(t.hasNext());
	}

	@Test
	public void testSoloSingle() {
		Object obj = new Object();
		Iterator<Object> e = emptyIterator();
		testSingleton(obj, e, single(obj));
		testSingleton(obj, single(obj), e);
		testSingleton(obj, e, e, single(obj));
		testSingleton(obj, e, single(obj), e);
		testSingleton(obj, single(obj), e, e);
	}

	@Test
	public void testRemove() {
		List<String> first = list("a", "b");
		List<String> second = list("c", "d");
		testEqual(combine(first, second), "a", "b", "c", "d");
		{
			Iterator<String> it = combine(first, second);
			it.next(); // a
			it.next(); // b
			it.remove();
			assertEquals(list("a"), first);
			assertEquals(list("c", "d"), second);
		}
		{
			Iterator<String> it = combine(first, second);
			it.next(); // a
			it.next(); // c
			it.remove();
			assertEquals(list("a"), first);
			assertEquals(list("d"), second);
		}
	}

	private <E> void testSingleton(E exp, Iterator<E>... its) {
		Iterator<E> t = Iterators.flatten(it(its));
		assertTrue(t.hasNext());
		assertSame(exp, t.next());
		assertFalse(t.hasNext());
	}

	private <E> void testEqual(Iterator<E> it, E... es) {
		List<E> list = new ArrayList<>();
		while (it.hasNext()) list.add(it.next());
		assertEquals(asList(es), list);
	}
}
