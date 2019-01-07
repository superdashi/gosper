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

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.logging.Logger;

public class SubstitutionTest {

	@Test
	public void testBasic() {
		verify("nothing", "nothing");
		verify("nothing", "nothing", 1);
		verify("", "", 9);
		verify("{", "{", 9);
		verify("x{", "x{", 9);
		verify("{x", "{x", 9);
		verify("{1", "{1", 9);
		verify("{00}", "{00}", 9);
		verify("", "{63}");
		verify("{64}", "{64}");
		verify("9", "{0}", 9);
		verify("9zerostart", "{0}zerostart", 9);
		verify("zeroend9", "zeroend{0}", 9);
		verify("zero9middle", "zero{0}middle", 9);
		verify("9", "{}", 9);
		verify("9autostart", "{}autostart", 9);
		verify("autoend9", "autoend{}", 9);
		verify("auto9middle", "auto{}middle", 9);
		verify("A B", "{} {}", "A", "B");
		verify("AB", "{}{}", "A", "B");
		verify("012", "{0}{1}{2}", 0, 1, 2);
		verify("210", "{2}{1}{0}", 0, 1, 2);
		verify("212", "{2}{1}{2}", 0, 1, 2);
		verify("012", "{}{}{}", 0, 1, 2);
		verify("021", "{0}{}{1}", 0, 1, 2);
	}

	private void verify(String expected, String message, Object... values) {
		Assert.assertEquals(expected, Logger.substitute(message, values));
	}
}
