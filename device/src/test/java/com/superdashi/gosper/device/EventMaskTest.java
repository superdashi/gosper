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
