package com.superdashi.gosper.studio;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public class FontPlotter {

	private static final int ASCII_MIN = 32;
	private static final int ASCII_MAX = 127;

	private static final int MAX_WIDTH = 512;
	private static final int MAX_HEIGHT = 512;

	private static int leastPo2(int i) {
		return 1 << (32 - Integer.numberOfLeadingZeros(i));
	}

	private static boolean fits(int width, int height, int h, int[] ws) {
		int x = 0;
		int y = 0;
		int lim = height - h;
		for (int c = ASCII_MIN; c < ASCII_MAX; c++) {
			int w = ws[c];
			x += w + 1;
			if (x > width) {
				x = w + 1;
				y += h + 1;
				if (y > lim) return false;
			}
		}
		return true;
	}

	private static ImageMask[] plot(ImageMask mask, Font font, int h, int[] ws) {
		ImageMask[] masks = new ImageMask[ws.length];
		BufferedImage image = mask.image;
		Graphics2D g = image.createGraphics();
		g.setFont(font);
		g.setColor(Color.WHITE);
		int width = image.getWidth();
		int x = 0;
		int y = 0;
		for (int c = ASCII_MIN; c < ASCII_MAX; c++) {
			int w = ws[c];
			x += w + 1;
			if (x > width) {
				x = w + 1;
				y += h + 1;
			}
			masks[c] = mask.view(IntRect.rectangle(x, y, w, h));
			g.drawString(Character.toString((char) c), x, y);
		}
		g.dispose();
		return masks;
	}

	private final Font font;
	private final FontMetrics metrics;
	private final ImageMask masterMask;
	private final ImageMask asciiMasks[];

	public FontPlotter(Font font) {
		this.font = font;
		metrics = ImageUtil.fontMetrics(font);

		// precompute height
		int ascent = metrics.getMaxAscent();
		int descent = metrics.getMaxDescent();
		int h = ascent + descent;
		// precompute widths
		int[] ws = new int[ASCII_MAX];
		int w = 0;
		for (int c = ASCII_MIN; c < ASCII_MAX; c++) {
			int cw = metrics.charWidth(c);
			w = Math.max(cw, w);
			ws[c] = cw;
		}
		// identify minimal sizes
		int minHeight = leastPo2(h);
		int minWidth = leastPo2(w);
		int bestHeight = -1;
		int bestWidth = -1;
		int bestScore = -1;
		for (int height = minHeight; height < MAX_HEIGHT; height <<= 1) {
			for (int width = minWidth; width < MAX_HEIGHT; width <<= 1) {
				if (fits(width, height, h, ws)) {
					int score = width * height + width + height;
					if (bestScore == -1 || score < bestScore) {
						bestWidth = width;
						bestHeight = height;
						bestScore = score;
					}
				}
			}
		}
		if (bestScore == -1) throw new RuntimeException("Not yet supported");

		// produce atlas
		masterMask = new ImageMask(IntDimensions.of(bestWidth, bestHeight));
		asciiMasks = plot(masterMask, font, h, ws);
	}

}
