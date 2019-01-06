package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRange;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

public class AtlasProducer {

	// statics

	private static final IntRange KERN_RANGE = IntRange.bounded(AtlasTypeface.MIN_KERN, AtlasTypeface.MAX_KERN);
	private static final Storage<Integer> storage = StoreType.of(int.class).settingNullToDefault().smallValueStorage(KERN_RANGE.pointSize());

	private static int computeKerning(int[] right, int[] left, float target) {
		int bestS = -1;
		float bestScore = Float.NaN;
		for (int s = KERN_RANGE.min; s <= KERN_RANGE.max; s++) {
			float distance = distance(right, left, s);
			if (Float.isNaN(distance)) continue; // invalid shift
			float score = Math.abs(distance - target);
			if (Float.isNaN(bestScore) || score < bestScore) {
				bestS = s;
				bestScore = score;
			}
		}
		return bestS;
	}

	private static float distance(int[] right, int[] left, int shift) {
		int sum = 0;
		int count = 0;
		int last = right.length - 1;
		for (int i = 0; i < right.length; i++) {
			int l = left[i];
			if (l == -1) continue;
			if (i > 0) {
				int r = right[i - 1];
				if (r >= 0) {
					int d = l + r + shift;
					if (d <= 0) return Float.NaN; // invalid - touching above
				}
			}
			if (i < last) {
				int r = right[i + 1];
				if (r >= 0) {
					int d = l + r + shift;
					if (d <= 0) return Float.NaN; // invalid - touching below
				}
			}
			int r = right[i];
			if (r == -1) continue;
			int d = r + l + shift;
			if (d <= 0) return Float.NaN; // invalid - touching horizontally
			sum += Math.min(d, 4);
			count ++;
		}
		return (float) sum / count;
	}

	static void serializeKerning(WriteStream s, Store<Integer> kerning) {
		if (kerning == null) {
			s.writeInt(0);
		} else {
			int size = kerning.size();
			s.writeInt(size);
			for (int i = 0; i < size; i++) {
				s.writeByte((byte) (int) kerning.get(i));
			}
		}
	}

	static Store<Integer> deserializeKerning(ReadStream s) {
		int size = s.readInt();
		if (size == 0) return null;
		Store<Integer> kerning = AtlasProducer.storage.newStore(size);
		for (int i = 0; i < size; i++) {
			kerning.set(i, (int) s.readByte());
		}
		return kerning;
	}

	static ImageMask atlas(String resourcePath) {
		BufferedImage img;
		InputStream in = AtlasProducer.class.getResourceAsStream(resourcePath);
		try {
			if (in == null) throw new RuntimeException("no atlas at resource path: " + resourcePath);
			img = ImageIO.read(in);
		} catch (IOException e) {
			throw new RuntimeException("could not find font atlas", e);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {
				//TODO
				/* ignored */
			}
		}
		img = ImageUtil.convertImage(img, BufferedImage.TYPE_BYTE_GRAY);
		return new ImageMask(img);
	}

	static AtlasProducer iotaInstance() {
		return new AtlasProducer(new String[] {"/iota_regular.png", "/iota_bold.png", "/iota_italic.png", "/iota_bold-italic.png"}, 20, 15, 4, 14, 5, 6, 3f, "1234567890", "abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
	}

	// fields

	private final String[] paths;
	private final int lineHeight;
	private final int baseline;
	private final int slantRatio;
	private final int ascent;
	private final int descent;
	private final int spaceWidth;
	private final float kernTarget;
	private final String[] rows;

	private final int digitKern;

	// constructors

	public AtlasProducer(
			String[] paths,
			int lineHeight,
			int baseline,
			int slantRatio,
			int ascent,
			int descent,
			int spaceWidth,
			float kernTarget,
			String... rows
			) {
		this.paths = paths;
		this.lineHeight = lineHeight;
		this.baseline = baseline;
		this.slantRatio = slantRatio;
		this.ascent = ascent;
		this.descent = descent;
		this.spaceWidth = spaceWidth;
		this.kernTarget = kernTarget;
		this.rows = rows;

		digitKern = KERN_RANGE.clampPoint(Math.round(kernTarget));
	}

	// public methods

	public void recordTypeface(Path path) throws IOException {
		createTypeface().serialize( Streams.streamOutput( Files.newOutputStream(path) ) );
	}

	public AtlasTypeface createTypeface() {
		AtlasFont[] fonts = Arrays.stream(paths).map(AtlasProducer::atlas).map(this::createFont).toArray(AtlasFont[]::new);
		return new AtlasTypeface(baseline, lineHeight, ascent, descent, fonts);
	}

	public static void main(String... args) throws IOException {
		Path path = Paths.get(args[0]);
		iotaInstance().recordTypeface(path);
	}

	// private methods

	private AtlasFont createFont(ImageMask mask) {
		//Mask dummy = Mask.entire(mask.dimensions());
		IntDimensions dimensions = mask.dimensions();
		AtlasGlyph[] glyphs = new AtlasGlyph[127];
		int step = lineHeight + 1; // 1 for indicator
		int top = lineHeight;
		int bottom = -1;
		boolean italic = (mask.readPixel(mask.dimensions().width - 1, 0) & 0xff) != 0;
		for (int i = 0; i < rows.length; i++) {
			String row = rows[i];
			int minY = i * step;
			int maxY = minY + lineHeight;
			int startX = 0;
			int[] cps = row.codePoints().toArray();
			for (int j = 0; j < cps.length; j++) {
				int cp = cps[j];
				int x = startX;
				int topY = maxY; // top of glyph
				int bottomY = minY; // bottom of glyph
				int leftX = Integer.MAX_VALUE;
				int rightX = Integer.MIN_VALUE;
				while (true) { // we break when gap is true
					boolean gap = true;
					// anayze pixels
					int y;
					for (y = minY; y < maxY; y++) {
						if ((mask.readPixel(x, y) & 0xff) != 0) {
							if (y < topY) topY = y;
							if (y+1 > bottomY) bottomY = y+1;
							gap = false;
						}
					}
					// analyze width
					if ((mask.readPixel(x, y) & 0xff) != 0) {
						gap = false;
						leftX = Math.min(leftX, x);
						rightX = Math.max(rightX, x);
					}
					x++;
					if (gap) break;
				}
				int offset = leftX - startX;
				int width = rightX - leftX + 1;
				AtlasGlyph glyph = new AtlasGlyph(cp, offset, width, topY - minY, maxY - bottomY, startX, topY, x - startX - 1, bottomY - topY);
				glyphs[cp] = glyph;
				top = Math.min(top, topY - minY);
				bottom = Math.max(bottom, bottomY - minY);
				startX = x;
			}
		}
		AtlasGlyph space = new AtlasGlyph(32, 0, spaceWidth, 0, 0, 0, 0, spaceWidth, lineHeight);
		//space.cutMask(blank);
		glyphs[32] = space;

		// cut masks and assemble kerners
		Kerner[] kerners = new Kerner[glyphs.length];
		for (int i = 0; i < kerners.length; i++) {
			AtlasGlyph glyph = glyphs[i];
			if (glyph == null) continue;
			glyph.cutMask(mask);
			kerners[i] = new Kerner(glyph, italic);
		}

		// compute kerning
		for (int i = 0; i < kerners.length; i++) {
			Kerner ki = kerners[i];
			if (ki == null) continue;
			int[] kerning = new int[kerners.length];
			AtlasGlyph g = ki.glyph;
			Arrays.fill(kerning, Integer.MIN_VALUE);
			for (int j = 0; j < kerners.length; j++) {
				Kerner kj = kerners[j];
				if (kj == null) continue;
				int k = ki.kern(kj, kernTarget);
				assert KERN_RANGE.containsPoint(k);
				kerning[j] = k;
			}
			// identify the modal value
			int[] freqs = new int[KERN_RANGE.pointSize()];
			for (int k = 0; k < kerning.length; k++) {
				int kern = kerning[k];
				if (kern == Integer.MIN_VALUE) continue;
				freqs[KERN_RANGE.pointOffsetInRange(kern)]++;
			}
			int bestK = 0;
			int bestF = freqs[0];
			int count = 0;
			for (int k = 1; k < freqs.length; k++) {
				int f = freqs[k];
				count += f;
				if (f > bestF) {
					bestK = k;
					bestF = f;
				}
			}
			if (bestF == count) {
				g.defaultKern = KERN_RANGE.translatePointOffset(bestK);
				g.kerning = null;
			} else if (bestF >= count / 2 && 1 == 0) {
				//TODO store compactly
			} else {
				g.defaultKern = Integer.MIN_VALUE;
				Store<Integer> store = storage.newStore(kerning.length);
				for (int k = 0; k < kerning.length; k++) {
					int kern = kerning[k];
					if (kern == Integer.MIN_VALUE) continue;
					store.set(k, KERN_RANGE.pointOffsetInRange(kerning[k]));
				}
				g.kerning = store.immutableView();
			}
		}

		int underlineThickness = 0;
		for (int y = dimensions.height - 1; y >= 0 && (mask.readPixel(dimensions.width - 1, y) & 0xff) != 0; y--, underlineThickness++);
		return new AtlasFont(mask, underlineThickness, glyphs, baseline - top, bottom - baseline);
	}

	// inner classes

	private class Kerner {

		private final AtlasGlyph glyph;
		private final int[] leftProfile;
		private final int[] rightProfile;
		private final boolean italic;

		Kerner(AtlasGlyph glyph, boolean italic) {
			this.glyph = glyph;
			this.italic = italic;
			leftProfile = profile(y -> {
				y -= glyph.offsetAbove;
				if (y < 0 || y >= glyph.maskHeight) return -1;
				Mask mask = glyph.mask();
				for (int x = 0; x < glyph.maskWidth; x++) {
					if ((mask.readPixel(x, y) & 0xff) != 0) {
						return x;
					}
				}
				return -1;
			}, true);
			rightProfile = profile(y -> {
				y -= glyph.offsetAbove;
				if (y < 0 || y >= glyph.maskHeight) return -1;
				Mask mask = glyph.mask();
				for (int x = glyph.maskWidth - 1; x >= 0; x--) {
					if ((mask.readPixel(x, y) & 0xff) != 0) {
						return glyph.maskWidth - 1 - x;
					}
				}
				return -1;
			}, false);
		}

		int kern(Kerner that, float target) {
			int c1 = this.glyph.codepoint;
			int c2 = that.glyph.codepoint;
			if (c1 == 32 || c2 == 32) return 0;
			if (c1 == '_' || c2 == '_') return 0;
			if (Character.isDigit(c1) && Character.isDigit(c2)) return digitKern;
			return computeKerning(this.rightProfile, that.leftProfile, target);
		}

		//HACK reusing this interface
		private int[] profile(Fitter fitter, boolean left) {
			int height = glyph.maskHeight;
			int above = glyph.offsetAbove;
			int below = glyph.offsetBelow;
			int[] profile = new int[height + above + below];
			Arrays.fill(profile, -1);
			for (int y = above; y < above + height; y++) {
				profile[y] = fitter.fit(y);
			}
			// fills in underhang
			int m = Integer.MAX_VALUE;
			for (int y = 0; y < baseline; y++) {
				if (italic && m != Integer.MAX_VALUE && (y % slantRatio) == 0) {
					m = left ? m - 1 : m + 1;
				}
				int oldY = profile[y];
				if (oldY == -1) {
					if (m != Integer.MAX_VALUE) profile[y] = m;
				} else {
					m = Math.min(m, oldY + 1);
					profile[y] = Math.min(oldY, m);
				}
			}
			return profile;
		}
	}

	private interface Fitter {

		int fit(int width);

	}

}
