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
