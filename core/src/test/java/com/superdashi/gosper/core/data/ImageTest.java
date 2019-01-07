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
package com.superdashi.gosper.core.data;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Image.Type;

public class ImageTest {

	@Test
	public void testSchemes() throws URISyntaxException {
		Image h = new Image(new URI("http://www.example.com"));
		Assert.assertEquals(Type.HTTP, h.type());
		Image s = new Image(new URI("https://www.example.com"));
		Assert.assertEquals(Type.HTTP, s.type());
		Image d = new Image(new URI("internal://color/ff4400"));
		Assert.assertEquals(Type.INTERNAL, d.type());
	}
}
