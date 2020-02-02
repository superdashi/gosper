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
package com.superdashi.gosper.layout;

import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.layout.StyledText.Segment;
import com.superdashi.gosper.layout.StyledText.Span;

public class StyledTextTest {

	private static void expectISE(Runnable r) {
		try {
			r.run();
			Assert.fail("no ISE thrown");
		} catch (IllegalStateException e) {
			/* expected */
		}
	}

	@Test
	public void testManipulation() {
		Style root = new Style().immutable();
		String text = "this is styled text";
		StyledText st = new StyledText(root, text);
		st.root().insertText(7, " truly");
		Style bold = new Style().textWeight(1).immutable();
		Style italic = new Style().textItalic(1).immutable();
		Style under = new Style().textUnderline(1).immutable();
		Span s1 = st.root().applyStyle(italic, "italic", 8, 13).get(0);
		st.root().applyStyle(bold, "bold", 8, 9);
		s1.remove();
		st.root().applyStyle(italic, "italic", 14, 20);
		st.root().applyStyle(bold, "bold", 8, 25);
		st.root().insertText(7, "**");

		Assert.assertEquals(Style.NO_VALUE, st.styleAt(9).textItalic());
		Assert.assertEquals(Style.NO_VALUE, st.styleAt(9).textWeight());

		Assert.assertEquals(Style.NO_VALUE, st.styleAt(9).textItalic());
		Assert.assertEquals(1, st.styleAt(10).textWeight());

		Assert.assertEquals(1, st.styleAt(16).textItalic());
		Assert.assertEquals(1, st.styleAt(16).textWeight());

		Assert.assertEquals("this is** truly styled text", st.root().text());
		st.root().deleteText(5, 18);
		st.toString(); // serves as a check for valid indices

		Assert.assertEquals("this yled text", st.text());
		Span span = st.root().children().get(0).insertStyledText(2, under, "ooo");
		Assert.assertEquals("this yloooed text", st.text());
		Assert.assertEquals(1, st.styleAt(7).textUnderline());
		span.delete();
		Assert.assertEquals("this yled text", st.text());
		Assert.assertEquals(Style.NO_VALUE, st.styleAt(7).textUnderline());

		st.truncateText(4);
		Assert.assertEquals("this", st.text());
		Assert.assertTrue( st.isSingleSpan() );
	}

	@Test
	public void testEmpty() {
		Style root = new Style().immutable();
		StyledText st = new StyledText(root, "");
		Assert.assertEquals("", st.root().text().toString());
		st.root().insertText(0, "TEXT");
		Assert.assertEquals("TEXT", st.root().text().toString());
		st.root().deleteText(0, 4);
		Assert.assertEquals("", st.root().text().toString());
	}

	@Test
	public void testMutability() {
		Style italic = new Style().textItalic(1).immutable();
		Style root = new Style().immutable();
		String text1 = "Some test text";
		String text2 = "Some nice test text";
		StyledText st = new StyledText(root, text1);
		st.root().applyStyle(italic, "italic", 5, 9);

		StyledText iv = st.immutableView();
		StyledText ic = st.immutableCopy();
		StyledText mc = st.mutableCopy();

		Assert.assertTrue(st.isMutable());
		Assert.assertFalse(iv.isMutable());
		Assert.assertFalse(ic.isMutable());
		Assert.assertTrue(mc.isMutable());

		Assert.assertEquals(text1, st.root().text());
		st.root().insertText(5, "nice ");
		Assert.assertEquals(text2, st.root().text());
		Assert.assertEquals(text2, iv.root().text());
		Assert.assertEquals(text1, ic.root().text());
		Assert.assertEquals(text1, mc.root().text());

		st.root().applyStyle(italic, 0, 4);
		Assert.assertEquals(2, st.root().children().size());
		Assert.assertEquals(2, iv.root().children().size());
		Assert.assertEquals(1, ic.root().children().size());
		Assert.assertEquals(1, mc.root().children().size());
	}

	@Test
	public void testSegment() {
		Style italic = new Style().textItalic(1).immutable();
		Style bold = new Style().textWeight(1).immutable();
		Style under = new Style().textUnderline(1).immutable();

		StyledText st = new StyledText("Styling some text is lots of effort.");
		st.root().applyStyle(italic, "i", 8, 28);
		st.root().applyStyle(bold, "b", 13, 17);
		st.root().applyStyle(under, "u", 26, 28);

		Function<Style, String> styleString = s -> (s.textItalic() == 1 ? "italic" : "") + "," +  (s.textWeight() == 1 ? "bold" : "") + "," + (s.textUnderline() == 1 ? "underline" : "");
		for (Segment s : st.segments()) {
			System.out.println(s.from + " " + s.to + " " + styleString.apply(s.style) + " \"" + s.text + "\"");
		}
		System.out.println(st.root().text());
		System.out.println(st);
	}

	@Test
	public void testOverlapping() {
		Style italic = new Style().textItalic(1).immutable();
		Style bold = new Style().textWeight(1).immutable();
		Style under = new Style().textUnderline(1).immutable();
		Style blue = new Style().colorFg(0xff0000ff).immutable();
		Style red = new Style().colorFg(0xffff0000).immutable();

		String str = "This is a styled line of text.";
		StyledText text = new StyledText(str);
		Span root = text.root();

		root.applyStyle(italic, 5, 7);
		root.applyStyle(bold, 8, 16);
		root.applyStyle(blue, 17, 29);
		root.applyStyle(under, 17, 21);
		root.applyStyle(red, 22, 24);

		String chars = StreamSupport.stream(text.segments().spliterator(), false).map(s -> s.text).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
		Assert.assertEquals(str, chars);
	}

	@Test
	public void testSpans() {
		Style italic = new Style().textItalic(1).immutable();
		Style bold = new Style().textWeight(1).immutable();
		Style under = new Style().textUnderline(1).immutable();

		StyledText st = new StyledText("Styling some text is lots of effort.");
		st.root().applyStyle(italic, "i", 8, 28);
		st.root().applyStyle(bold, "b", 13, 17);
		st.root().applyStyle(under, "u", 26, 28);

		st.spansStream().forEach(span -> {
				System.out.println(span.text());
		});
	}

	@Test
	public void testAppendSpan() {
		Style boldStyle = new Style().textWeight(1).immutable();
		StyledText text = new StyledText();
		text.appendStyledText(boldStyle, "Display Brightness");
		text.appendText("Yeah!");
	}

	@Test
	public void testEmptySpan() {
		Style bold = new Style().textWeight(1).immutable();
		StyledText text = new StyledText("This is a  word");
		Style base = text.styleAt(0);
		Span span = text.insertStyledText(10, bold, "");
		Assert.assertTrue(span.isEmpty());
		Assert.assertEquals("This is a  word", text.text());
		span.insertText(0,"bold");
		Assert.assertEquals("This is a bold word", text.text());
		Assert.assertEquals(base, text.styleAt(9));
		Assert.assertEquals(bold, text.styleAt(10));
		Assert.assertEquals(bold, text.styleAt(13));
		Assert.assertEquals(base, text.styleAt(14));
		Assert.assertEquals(span, text.spanContaining(12));
		span.deleteText(0,4);
		Assert.assertEquals("This is a  word", text.text());
		Assert.assertTrue(span.isEmpty());
		Assert.assertEquals(text.root(), text.spanContaining(12));
	}

	@Test
	public void testImmutability() {
		StyledText text = new StyledText("no changes").immutable();
		Style style = new Style();

		expectISE(() -> text.truncateText(2));
		expectISE(() -> text.appendText(" thank you"));
		expectISE(() -> text.insertText(3, "more "));
		expectISE(() -> text.deleteText(0, 3));
		expectISE(() -> text.insertStyledText(3, style, "more "));
		expectISE(() -> text.appendStyledText(style, " thank you"));

		expectISE(() -> text.root().applyStyle(style, 0, 2));
		expectISE(() -> text.root().insertStyledText(0, style, "absolutely"));
		expectISE(() -> text.root().appendStyledText(style, " today"));
	}

}
