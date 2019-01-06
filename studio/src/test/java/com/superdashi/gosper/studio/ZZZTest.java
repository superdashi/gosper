package com.superdashi.gosper.studio;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.studio.ImageMask;
import com.superdashi.gosper.studio.ImageUtil;
import com.tomgibara.intgeom.IntDimensions;

public class ZZZTest extends RenderTest {

	private static final double AREA_PROP = 0.2;
	private static final double DIFF_PROP = 40.0;

	private static int diff(int c1, int c2) {
		int a = Argb.alpha(c1) - Argb.alpha(c2);
		int r = Argb.red  (c1) - Argb.red  (c2);
		int g = Argb.green(c1) - Argb.green(c2);
		int b = Argb.blue (c1) - Argb.blue (c2);
		int d = a*a + r*r + g*g + b*b;
		int v = Math.round((float)Math.sqrt(d));
		return v < 256 ? v : 255;
	}

	private final ClassLoader cl = Thread.currentThread().getContextClassLoader();

	@Test
	public void zzzCheck() throws IOException {
		Error[] errors = Files.list(dir).map(this::check).filter(e -> e != null).toArray(Error[]::new);
		if (errors.length != 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("%d errors%n", errors.length));
			for (Error error : errors) {
				sb.append(String.format("%s: %s%n", error.filename, error.message));
			}
			Assert.fail(sb.toString());
		}
	}

	private Error check(Path path) {
		try {
			doCheck(path);
			return null;
		} catch (Throwable t) {
			return new Error(path.getFileName().toString(), t.getMessage());
		}
	}

	private void doCheck(Path path) throws Exception {
		InputStream in = cl.getResourceAsStream("expected-images/" + path.getFileName());
		if (in == null) fail("no expected result");
		BufferedImage expected = standardize(ImageIO.read(in));
		int w = expected.getWidth();
		int h = expected.getHeight();
		BufferedImage actual = standardize(ImageIO.read(path.toFile()));
		if (w != actual.getWidth() || h != actual.getHeight()) fail("mismatched dimensions");
		int size = w * h;
		int[] eps = new int[size];
		int[] aps = new int[size];
		expected.getRaster().getDataElements(0, 0, w, h, eps);
		actual.getRaster().getDataElements(0, 0, w, h, aps);
		if (!Arrays.equals(eps, aps)) {
			Files.createDirectories(out);
			byte[] areas = new byte[size];
			byte[] diffs = new byte[size];
			int count = 0;
			int sum = 0;
			for (int i = 0; i < size; i++) {
				byte area = (byte) (eps[i] == aps[i] ? 0 : 255);
				byte diff = (byte) diff(eps[i], aps[i]);
				areas[i] = area;
				diffs[i] = diff;
				if (area != 0) count++;
				sum += diff;
			}
			float areaProp = count / (float) size;
			float diffProp = sum / (float) count;
			BufferedImage area = new ImageMask(ImageUtil.imageOverByteGray(IntDimensions.of(w, h), areas)).image;
			BufferedImage diff = new ImageMask(ImageUtil.imageOverByteGray(IntDimensions.of(w, h), diffs)).image;
			ImageIO.write(area, "PNG", out.resolve(path.getFileName().toString().replace(".png", "_area.png")).toFile());
			ImageIO.write(diff, "PNG", out.resolve(path.getFileName().toString().replace(".png", "_diff.png")).toFile());
			if (areaProp > AREA_PROP || diffProp > DIFF_PROP) {
				fail("mismatched images");
			}
		}
	}

	private BufferedImage standardize(BufferedImage image) {
		if (image.getType() == BufferedImage.TYPE_INT_ARGB) return image;
		BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = copy.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return copy;
	}
	private void fail(String msg) {
		throw new RuntimeException(msg);
	}

	private static final class Error {

		final String filename;
		final String message;

		public Error(String filename, String message) {
			this.filename = filename;
			this.message = message;
		}

	}

}
