package com.superdashi.gosper.display;

import java.nio.IntBuffer;

import com.jogamp.nativewindow.awt.DirectDataBufferInt.BufferedImageInt;
import com.superdashi.gosper.color.Argb;

//TODO convert to integer arithmetic
public class GaussianBlur {

	// 3 std are applied over supplied radius
	public static GaussianBlur radius(float radius) {
		if (radius < 0) throw new IllegalArgumentException("negative radius");
		return new GaussianBlur(radius/3f, 3f);
	}

	// applies over 3 sigma
	public static GaussianBlur sigma(float sigma) {
		if (sigma <= 0f) throw new IllegalArgumentException("non-positive sigma");
		return new GaussianBlur(sigma, 3f);
	}

	// applies over n sigma
	public static GaussianBlur nSigma(float sigma, float n) {
		if (sigma <= 0f) throw new IllegalArgumentException("non-positive sigma");
		if (n <= 0f) throw new IllegalArgumentException("non-positive n");
		return new GaussianBlur(sigma, n);
	}

	// for clamping indices
	private static int clamp(int x, int max) {
		return Math.min(Math.max(0, x), max - 1);
	}

	// for clamping pixel values
	private static int clamp(float v) {
		if (v <= 0f) return 0;
		else if (v >= 255f) return 255;
		return Math.round(v);
	}

	// to generate kernel
	private static final float [] kernel(float sigma, float n) {
		// trivial case
		if (sigma == 0) return new float[]{ 1f };

		// the kernel is truncated at n sigmas from center.
		int d = (int) (2.0f * n * sigma + 1.0f);
		if ((d & 1) == 0) d++;  // size must be odd

		// build the kernel
		float [] kernel = new float[d];
		float sum = 0.0f;
		for(int i = 0; i < d; i++) {
			float x = i - d / 2;
			kernel[i] = (float) Math.exp( -x * x / (2.0 * sigma * sigma) );
			sum += kernel[i];
		}

		//normalise the area to 1
		for(int i = 0; i < d; i++) {
			kernel[i] /= sum;
		}

		return kernel;
	}

	// the kernel
	private final float[] k;

	// circular buffers of components from the image
	private final float[] as;
	private final float[] rs;
	private final float[] gs;
	private final float[] bs;

	private GaussianBlur(float sigma, float x) {
		k = kernel(sigma, x);
		as = new float[k.length];
		rs = new float[k.length];
		gs = new float[k.length];
		bs = new float[k.length];
	}

	public void blur2(BufferedImageInt image) {
		float[] k = this.k ;  // the kernel
		int d = k.length;     // diameter of the kernel (always odd)
		int e = d / 2;        // radius of the kernel
		if (e == 0) return;   // trivial case - nothing to do

		// image data
		IntBuffer buffer = image.getDataBuffer().getData();
		int width = image.getWidth();
		int height = image.getHeight();

		// first convolve horizontally
		for (int y = 0; y < height; y++) {
			int j = 0; // index into the component arrays
			for (int x = -e; x <= width + e; x++) {
				store(j, buffer.get(y * width + clamp(x, width)));
				if (++j == d) j = 0;
				if (x >= e & x < width + e) {
					buffer.put(y * width + x - e, convolve(j));
				}
			}
		}

		// then convolve vertically
		for (int x = 0; x < width; x++) {
			int j = 0; // index into the component arrays
			for (int y = -e; y < height + e; y++) {
				store(j, buffer.get(clamp(y, height) * width + x));
				if (++j == d) j = 0;
				if (y >= e & y < height + e) {
					buffer.put((y - e) * width + x, convolve(j));
				}
			}
		}
	}

	private void store(int j, int p) {
		as[j] = Argb.alpha(p);
		rs[j] = Argb.red  (p);
		gs[j] = Argb.green(p);
		bs[j] = Argb.blue (p);
	}

	private int convolve(int j) {
		float a = convolve(as, j);
		float r = convolve(rs, j);
		float g = convolve(gs, j);
		float b = convolve(bs, j);
		return Argb.argb(clamp(a), clamp(r), clamp(g), clamp(b));
	}

	private float convolve(float[] xs, int j) {
		float[] k = this.k;
		int d = k.length;

		float s = 0f;
		for (int i = 0; i < d; i++) {
			s += k[i] * xs[j];
			if (++j == d) j = 0;
		}
		return s;
	}

}
