package com.superdashi.gosper.device.network;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.device.network.WifiUtil;

public class WifiUtilTest {

	@Test
	public void testWpaKey() {
		// test generated via wpa_passphrase
		Assert.assertEquals("ae87d8d85952fb776ad565aa32eb772476b4dfbf95693fbea16389f4f39ab41f", WifiUtil.wpaKey("topsekret", "SOMESSID"));
	}

	@Test
	public void testIsValidWpaPassphrase() {
		Assert.assertTrue(WifiUtil.isValidWpaPassphrase("topsekret"));
		Assert.assertFalse(WifiUtil.isValidWpaPassphrase("to"));
		Assert.assertTrue(WifiUtil.isValidWpaPassphrase("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"));
		Assert.assertFalse(WifiUtil.isValidWpaPassphrase("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_!"));
		Assert.assertFalse(WifiUtil.isValidWpaPassphrase("fenêtre"));
	}

	@Test
	public void testIsValidSSID() {
		Assert.assertTrue(WifiUtil.isValidSSID("SIMPLE"));
		Assert.assertTrue(WifiUtil.isValidSSID("Simple"));
		Assert.assertTrue(WifiUtil.isValidSSID("Spaces okay"));
		Assert.assertTrue(WifiUtil.isValidSSID("The characters !,# and ; are OK."));
		Assert.assertTrue(WifiUtil.isValidSSID("just right0123456789012345678901"));
		Assert.assertFalse(WifiUtil.isValidSSID("too long8901234567890123456789012"));
		Assert.assertFalse(WifiUtil.isValidSSID("")); // empty
		Assert.assertFalse(WifiUtil.isValidSSID(" ")); // trailing space
		Assert.assertFalse(WifiUtil.isValidSSID("No+."));
		Assert.assertFalse(WifiUtil.isValidSSID("No]."));
		Assert.assertFalse(WifiUtil.isValidSSID("No/."));
		Assert.assertFalse(WifiUtil.isValidSSID("No\"."));
		Assert.assertFalse(WifiUtil.isValidSSID("No\t."));
		Assert.assertFalse(WifiUtil.isValidSSID("! cannot lead"));
		Assert.assertFalse(WifiUtil.isValidSSID("# cannot lead"));
		Assert.assertFalse(WifiUtil.isValidSSID("; cannot lead"));
		Assert.assertFalse(WifiUtil.isValidSSID("No trailing space "));
	}

}
