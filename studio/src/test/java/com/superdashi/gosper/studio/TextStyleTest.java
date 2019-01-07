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
package com.superdashi.gosper.studio;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.studio.TextStyle;

public class TextStyleTest {

	@Test
	public void testNoUnderline() {
		Assert.assertFalse(TextStyle.regular().underlined);
		Assert.assertFalse(TextStyle.bold().underlined);
		Assert.assertFalse(TextStyle.italic().underlined);
		Assert.assertFalse(TextStyle.boldItalic().underlined);
		Assert.assertTrue(TextStyle.regular().asStyle().textUnderline() == 0);
		Assert.assertTrue(TextStyle.bold().asStyle().textUnderline() == 0);
		Assert.assertTrue(TextStyle.italic().asStyle().textUnderline() == 0);
		Assert.assertTrue(TextStyle.boldItalic().asStyle().textUnderline() == 0);
	}
}
