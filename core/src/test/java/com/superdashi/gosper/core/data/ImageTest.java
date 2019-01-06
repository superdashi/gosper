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
