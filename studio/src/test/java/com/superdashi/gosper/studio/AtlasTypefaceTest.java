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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.layout.StyledText.Span;
import com.superdashi.gosper.studio.AtlasProducer;
import com.superdashi.gosper.studio.AtlasTypeface;
import com.superdashi.gosper.studio.ImageMask;
import com.superdashi.gosper.studio.ImageSurface;
import com.superdashi.gosper.studio.ImageUtil;
import com.superdashi.gosper.studio.IntFontMetrics;
import com.superdashi.gosper.studio.LocalCanvas;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.Typeface;
import com.superdashi.gosper.studio.Canvas.IntTextOps;
import com.superdashi.gosper.studio.Typeface.TextMeasurer;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

public class AtlasTypefaceTest extends RenderTest  {

	private static final List<TextStyle> basicStyles = Arrays.asList(TextStyle.regular(), TextStyle.bold(), TextStyle.italic(), TextStyle.boldItalic());

	static ImageMask atlas() {
		BufferedImage img;
		try {
			img = ImageIO.read(AtlasTypefaceTest.class.getResource("/iota_regular.png"));
		} catch (IOException e) {
			throw new RuntimeException("could not find font atlas", e);
		}
		img = ImageUtil.convertImage(img, BufferedImage.TYPE_BYTE_GRAY);
		return new ImageMask(img);
	}

	@Test
	public void testAtlas() {
		ImageMask atlas = atlas();
		ImageSurface surface = ImageSurface.sized(atlas.dimensions(), true);
		surface
		.createCanvas()
		.color(Argb.WHITE)
		.fill()
		.color(Argb.BLACK)
		.fillFrame(atlas)
		.destroy();
		recordResult(surface, "atlas");
	}

	@Test
	public void testSerialization() {
		AtlasTypeface original = AtlasProducer.iotaInstance().createTypeface();
		StreamBytes bytes = Streams.bytes();
		original.serialize(bytes.writeStream());
		AtlasTypeface copy = AtlasTypeface.deserialize(bytes.readStream());
	}

	@Test
	public void testBasicText() {
		Typeface typeface = Typeface.iota();
		IntFontMetrics metrics = typeface.measurer().intMetrics(TextStyle.regular());
		int lineHeight = metrics.lineHeight;
		int basline = metrics.baseline;
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(320, lineHeight * 5), true);
		surface
			.createCanvas()
			.color(Argb.WHITE)
			.fill()
			.color(Argb.BLACK)
			.intOps()
			.newText(typeface)
			.moveTo(0, lineHeight * 0 + basline)
			.renderString("This is some test (text). 2001")
			.moveTo(0, lineHeight * 1 + basline)
			.renderString("1234567890")
			.moveTo(0, lineHeight * 2 + basline)
			.renderString("abcdefghijklmnopqrstuvwxyz")
			.moveTo(0, lineHeight * 3 + basline)
			.renderString("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
			.moveTo(0, lineHeight * 4 + basline)
			.renderString("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~")
			.canvas()
			.destroy();
		recordResult(surface, "atlasBasicText");
	}

	@Test
	public void testAccommodatedCount() {
		basicStyles.forEach(this::testAccommodatedCount);
	}

	private void testAccommodatedCount(TextStyle style) {
		Typeface typeface = Typeface.iota();
		TextMeasurer measurer = typeface.measurer();
		IntFontMetrics metrics = measurer.intMetrics(style);
		int lineHeight = metrics.lineHeight;
		int baseline = metrics.baseline;
		String str = "This is a message for sizing test.";
		Function<Integer, String> trim = w -> {
			int c = measurer.accommodatedCharCount(style, str, w, 0);
			return str.substring(0, c);
		};
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(320, lineHeight * 5), true);
		surface
			.createCanvas()
			.color(Argb.WHITE)
			.fill()
			.color(0xff80ffff)
			.intOps()
			.fillRect(IntRect.bounded(0, lineHeight * 0, 70 * 1, lineHeight * 1))
			.fillRect(IntRect.bounded(0, lineHeight * 1, 70 * 2, lineHeight * 2))
			.fillRect(IntRect.bounded(0, lineHeight * 2, 70 * 3, lineHeight * 3))
			.fillRect(IntRect.bounded(0, lineHeight * 3, 70 * 4, lineHeight * 4))
			.canvas()
			.color(Argb.BLACK)
			.intOps()
			.newText(typeface)
			.moveTo(0, lineHeight * 0 + baseline)
			.renderString(style, trim.apply(70 * 1))
			.moveTo(0, lineHeight * 1 + baseline)
			.renderString(style, trim.apply(70 * 2))
			.moveTo(0, lineHeight * 2 + baseline)
			.renderString(style, trim.apply(70 * 3))
			.moveTo(0, lineHeight * 3 + baseline)
			.renderString(style, trim.apply(70 * 4))
			.canvas()
			.destroy();
		recordResult(surface, "atlasAccommodatedCount_" + style.toString());
	}

	@Test
	public void testStyledText() {
		Typeface typeface = Typeface.iota();
		TextMeasurer measurer = typeface.measurer();
		IntFontMetrics metrics = measurer.intMetrics(TextStyle.regular());
		int lineHeight = metrics.lineHeight;
		int baseline = metrics.baseline;
		String str = "This is a styled line of text.";
		StyledText text = new StyledText(str);
		Span root = text.root();
		root.applyStyle(TextStyle.italic().asStyle(), 5, 7);
		root.applyStyle(TextStyle.bold().asStyle(), 8, 16);
		root.applyStyle(TextStyle.bold().withUnderlined(true).asStyle(), 10, 15);
		root.applyStyle(new Style().colorFg(0xff0000ff), 17, 29);
		root.applyStyle(TextStyle.regular().withUnderlined(true).asStyle(), 17, 21);
		root.applyStyle(new Style().colorFg(0xffff0000), 22, 24);
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(320, lineHeight), true);
		surface
		.createCanvas()
		.color(Argb.WHITE)
		.fill()
		.color(Argb.BLACK)
		.intOps()
		.newText(typeface)
		.moveTo(0, baseline)
		.renderText(Style.noStyle(), text)
		.canvas()
		.destroy();
		recordResult(surface, "atlasStyledText");
	}

	@Test
	public void testKerning() {
		basicStyles.forEach(this::testKerning);
	}

	private void testKerning(TextStyle style) {
		Typeface typeface = Typeface.iota();
		TextMeasurer measurer = typeface.measurer();
		IntFontMetrics metrics = measurer.intMetrics(style);
		int baseline = metrics.baseline;
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(2048 + 640, 2048 - 160), true);
		LocalCanvas canvas = surface.createCanvas();
		IntTextOps text = canvas
			.color(Argb.WHITE)
			.fill()
			.color(Argb.BLACK)
			.intOps().newText(typeface);

		int dx = 28;
		int dy = 20;
		int count = 127 - 33;
		for (int y = 0; y < count; y++) {
			int cy = y + 33;
			for (int x = 0; x < count; x++) {
				int cx = x + 33;
				String str = new String(new char[] {(char) cy , (char) cx});
				text.moveTo(x * dx, y * dy + baseline).renderString(style, str);
			}
		}
		canvas.destroy();
		recordResult(surface, "atlasKerning_" + style.toString());
	}
}
