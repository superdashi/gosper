package com.superdashi.gosper.framework;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.framework.Namespace;

public class NamespaceTest {

	@Test
	public void testBasic() {
		checkValid("example.com/path");
		checkValid("example.com/path/subpath");
		checkInvalid("example.com/");
		checkInvalid("example.com/path/subpath/");
		checkInvalid("example.com/path//subpath");
		checkInvalid("example.com//path/subpath");
		checkInvalid("/example.com/path/subpath");
	}

	@Test
	public void testCase() {
		Namespace ns1 = new Namespace("www.superdashi.com");
		Namespace ns2 = new Namespace("WWW.SUPERDASHI.COM");
		Assert.assertEquals(ns1, ns2);
	}

	@Test
	public void testValidHostname() {
		checkValid("a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z");
		checkValid("1.2.3.4.5");
		checkValid("1-2.3-4.5-6.7-8.9-0");
		checkValid("single");
	}

	@Test
	public void testInvalidHostname() {
		checkInvalid("");
		checkInvalid("-");
		checkInvalid("-example.com");
	}

	@Test
	public void testToString() {
		checkString("www.superdashi.com");
		checkString("example.com");
		checkString("example.com/sub/path");
	}

	@Test
	public void testReserved() {
		Assert.assertTrue(new Namespace("www.superdashi.com").isReserved());
		Assert.assertTrue(new Namespace("WWW.SUPERDASHI.COM").isReserved());
		Assert.assertTrue(new Namespace("www.SUPERDASHI.com").isReserved());
		Assert.assertTrue(new Namespace("www.superdashi.com/sub/path").isReserved());
		Assert.assertFalse(new Namespace("www.example.com").isReserved());
	}

	private void checkValid(String s) {
		new Namespace(s);
	}

	private void checkInvalid(String s) {
		try {
			new Namespace(s);
		} catch (IllegalArgumentException e) {
			/* expected */
			return;
		}
		throw new RuntimeException("Invalid string");
	}

	private void checkString(String str) {
		Assert.assertEquals(str, new Namespace(str).toString());
	}

}
