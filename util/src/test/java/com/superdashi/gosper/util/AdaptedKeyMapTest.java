package com.superdashi.gosper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.superdashi.gosper.util.AdaptedKeyMap;
import com.tomgibara.fundament.Bijection;

public class AdaptedKeyMapTest {

	private static final Bijection<Integer, String> adapter = Bijection.fromFunctions(Integer.class, String.class, i -> Integer.toString(i), s -> Integer.parseInt(s));

	@Test
	public void testBasic() {
		Map<Integer, Color> map = new HashMap<>();
		Map<String, Color> a = new AdaptedKeyMap<>(map, adapter);
		assertTrue(a.isEmpty());
		map.put(1, Color.BLACK);
		assertFalse(a.isEmpty());
		assertEquals(Color.BLACK, a.get("1"));
		assertTrue(a.containsKey("1"));
		Set<Entry<String, Color>> es = a.entrySet();
		assertEquals(1, es.size());
		Iterator<Entry<String, Color>> i = es.iterator();
		Entry<String, Color> e = i.next();
		assertFalse(i.hasNext());
		assertEquals(e.getKey(), "1");
		assertEquals(e.getValue(), Color.BLACK);
		e.setValue(Color.RED);
		assertEquals(Color.RED, a.get("1"));
		a.put("2", Color.BLACK);
		assertEquals(Color.BLACK, map.get(2));
		assertEquals(2, a.size());
		assertEquals(2, map.size());
		Set<String> s = a.keySet();
		assertTrue(s.contains("1"));
		assertTrue(s.contains("2"));
		assertTrue(s.containsAll(Arrays.asList("1", "2")));
		assertFalse(s.containsAll(Arrays.asList("1", "2", "3")));
	}
}
