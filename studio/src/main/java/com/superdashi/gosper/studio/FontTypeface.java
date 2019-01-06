package com.superdashi.gosper.studio;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.layout.StyledText.Span;

final class FontTypeface extends Typeface {

	private static Map<Attribute, Object> addAttribute(Map<Attribute, Object>  map, Attribute attr, Object value) {
		switch (map.size()) {
		case 0 : return Collections.singletonMap(attr, value);
		case 1 : map = new HashMap<>(map); // fall through
		default:
			map.put(attr, value);
			return map;
		}
	}

	private static boolean hasFlag(int value) {
		return value != Style.NO_VALUE;
	}

	private static boolean flag(int value) {
		return value > 0;
	}

	private final Font regularFont;
	private final Font boldFont;
	private final Font italicFont;
	private final Font boldItalicFont;

	FontTypeface(Font font) {
		regularFont    = font;
		boldFont       = font;
		italicFont     = font;
		boldItalicFont = font;
	}

	FontTypeface(Font regularFont, Font boldFont, Font italicFont, Font boldItalicFont) {
		this.regularFont = regularFont;
		this.boldFont = boldFont;
		this.italicFont = italicFont;
		this.boldItalicFont = boldItalicFont;
	}

	@Override
	FontRenderer renderer(LocalCanvas canvas) {
		return new FontRenderer(canvas);
	}

	@Override
	FontMeasurer measurer() {
		return new FontMeasurer();
	}

	private Font font(TextStyle style) {
		switch (style.withUnderlined(false).index) {
		case 0 : return regularFont;
		case 1 : return boldFont;
		case 2 : return italicFont;
		case 3 : return boldItalicFont;
		default: return regularFont;
		}
	}

	private Map<Attribute, Object> attributes(Style style) {
		Map<Attribute, Object> map = Collections.emptyMap();
		if (hasFlag(style.textWeight())) {
			//map = addAttribute(map, TextAttribute.WEIGHT, flag(style.textWeight()) ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
			map = addAttribute(map, TextAttribute.FONT, flag(style.textWeight()) ? boldFont : regularFont);
		}
		if (hasFlag(style.textItalic())) {
			//map = addAttribute(map, TextAttribute.POSTURE, flag(style.textItalic()) ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
			map = addAttribute(map, TextAttribute.FONT, flag(style.textItalic()) ? italicFont : regularFont);
		}
		if (hasFlag(style.textUnderline())) {
			map = addAttribute(map, TextAttribute.UNDERLINE, flag(style.textUnderline()) ? TextAttribute.UNDERLINE_ON : -1);
		}
		//TODO need a way of identifying if color has been set
		if (style.colorFg() != 0) {
			map = addAttribute(map, TextAttribute.FOREGROUND, new Color(style.colorFg(), true));
		}
		return map;
	}

	//TODO look at possibility of caching these?
	private AttributedString convert(StyledText text) {
		AttributedString str = new AttributedString(text.root().text());
		str.addAttribute(TextAttribute.FONT, regularFont);
		for (Span span : text.spans()) {
			str.addAttributes(attributes(span.style()), span.from(), span.to());
		}
		return str;
	}

	private class FontRenderer implements TextRenderer {

		private final LocalCanvas canvas;
		private char[] singleChar = null;

		FontRenderer(LocalCanvas canvas) {
			this.canvas = canvas;
		}

		@Override
		public FontTypeface typeface() {
			return FontTypeface.this;
		}

		@Override
		public void renderChar(int x, int y, int c) {
			renderChar(x, y, TextStyle.regular(), c);
		}

		@Override
		public void renderChar(float x, float y, int c) {
			renderChar(x, y, TextStyle.regular(), c);
		}

		@Override
		public void renderChar(int x, int y, TextStyle style, int c) {
			// not in the BMP so fall back to string rendering
			if (Character.isBmpCodePoint(c) ) {
				// try something that should be a mite faster
				configure(style);
				canvas.doGraphics(g -> { g.drawChars(singleChar(c), 0, 1, x, y); });
			} else {
				// otherwise fall back to the render string method
				renderString(x, y, style, new String(new int[] {c}, 0, 1) );
			}
		}

		@Override
		public void renderChar(float x, float y, TextStyle style, int c) {
			renderString(x, y, style, new String(new int[] {c}, 0, 1) );
		}

		@Override
		public void renderString(int x, int y, String str) {
			configure(TextStyle.regular());
			canvas.doGraphics(g -> { g.drawString(str, x, y); } );
		}

		@Override
		public void renderString(float x, float y, String str) {
			configure(TextStyle.regular());
			canvas.doGraphics(g -> { g.drawString(str, x, y); } );
		}

		@Override
		public void renderString(int x, int y, TextStyle style, String str) {
			configure(style);
			canvas.doGraphics(g -> {
				g.drawString(str, x, y);
				if (style.underlined) underline(g, x, y, str);
			});
		}

		@Override
		public void renderString(float x, float y, TextStyle style, String str) {
			configure(style);
			canvas.doGraphics(g -> {
				g.drawString(str, x, y);
				if (style.underlined) underline(g, x, y, str);
			});
		}

		@Override
		public void renderText(int x, int y, StyledText text) {
			AttributedString as = convert(text);
			canvas.doGraphics(g -> {
				g.drawString(as.getIterator(), x, y);
			});
		}

		@Override
		public void renderText(float x, float y, StyledText text) {
			AttributedString as = convert(text);
			canvas.doGraphics(g -> {
				g.drawString(as.getIterator(), x, y);
			});
		}

		private void underline(Graphics2D g, float x, float y, String str) {
			FontMetrics metrics = g.getFontMetrics();
			LineMetrics lm = metrics.getLineMetrics(str, g);
			int width = metrics.stringWidth(str);
			g.fill(new Rectangle2D.Float(x, y + lm.getUnderlineOffset(), width, lm.getUnderlineThickness()));
		}

		private void configure(TextStyle style) {
			//assumes setting same font is cheap
			canvas.applyFont( font(style) );
		}

		private char[] singleChar(int c) {
			if (singleChar == null) singleChar = new char[1];
			singleChar[0] = (char) c;
			return singleChar;
		}

	}

	private class FontMeasurer implements TextMeasurer {

		private FontMetrics[] metrics = null;

		FontMeasurer() { }

		@Override
		public IntFontMetrics intMetrics(TextStyle style) {
			FontMetrics fm = fontMetrics(style);
			int leading = fm.getHeight() - fm.getAscent() - fm.getDescent();
			return new IntFontMetrics(fm.getAscent(), fm.getMaxAscent(), fm.getMaxDescent(), leading, fm.getAscent(), fm.getDescent());
		}

		@Override
		public int accommodatedCharCount(TextStyle style, String str, int width, int ellipsisWidth) {
			FontMetrics metrics = fontMetrics(style);
			FontRenderContext frc = metrics.getFontRenderContext();
			AttributedString text = new AttributedString(str);
			text.addAttribute(TextAttribute.FONT, metrics.getFont());
			return accommodatedCharCount(text, str.length(), width, ellipsisWidth, frc);
		}

		@Override
		public int accommodatedCharCount(StyledText text, int width, int ellipsisWidth) {
			AttributedString as = convert(text);
			FontRenderContext frc = fontMetrics(TextStyle.regular()).getFontRenderContext();
			return accommodatedCharCount(as, text.length(), width, ellipsisWidth, frc);
		}

		@Override
		public int intRenderedWidthOfString(TextStyle style, String str) {
			return fontMetrics(style).stringWidth(str);
		}

		private int accommodatedCharCount(AttributedString text, int length, int width, int ellipsisWidth, FontRenderContext frc) {
			LineBreakMeasurer lbm = new LineBreakMeasurer(text.getIterator(), BreakIterator.getCharacterInstance(), frc);
			int offset = lbm.nextOffset(width, length, true);
			if (offset == length) return offset;
			return lbm.nextOffset(width - ellipsisWidth, length, true);
		}

		private FontMetrics fontMetrics(TextStyle style) {
			if (metrics == null) metrics = new FontMetrics[4];
			int index = style.withUnderlined(false).index;
			FontMetrics fm = metrics[index];
			if (fm == null) {
				metrics[index] = fm = ImageUtil.fontMetrics(font(style));
			}
			return fm;
		}

	}

}
