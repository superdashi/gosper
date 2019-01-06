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
