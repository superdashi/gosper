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
