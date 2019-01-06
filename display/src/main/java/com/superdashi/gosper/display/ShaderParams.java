package com.superdashi.gosper.display;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import com.jogamp.nativewindow.awt.DirectDataBufferInt;
import com.jogamp.nativewindow.awt.DirectDataBufferInt.BufferedImageInt;
import com.tomgibara.intgeom.IntRect;
import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.layout.Alignment2D;
import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Position.Fit;
import com.tomgibara.fundament.Producer;

public abstract class ShaderParams {

	private static final Position position = Position.from(Fit.MATCH, Fit.MATCH, Alignment2D.pair(Alignment.MIN, Alignment.MIN));

	private static float nonNeg(float v) { return Math.max(0f, v); }

	private static void renderToImageImpl(BufferedImageInt image, Drawable drawable, Drawable transition) {
		// create an image of the appropriate size that exposes a direct buffer
		IntBuffer buffer = image.getDataBuffer().getData();
		int capacity = buffer.capacity();
		Graphics2D gfx = image.createGraphics();
		gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		IntRect rect = IntRect.atOrigin(image.getWidth(), image.getHeight());
		// render the transition if we need one
		byte[] alpha = null;
		if (transition != null) {
			alpha = new byte[buffer.capacity()];
			transition.drawTo(gfx, rect, position);
			for (int i = 0; i < capacity; i++) {
				int v = buffer.get(i);
				int r = Argb.red(v);
				int g = Argb.green(v);
				int b = Argb.blue(v);
				int a = (r + g + b) / 3;
				alpha[i] = (byte) a;
				//buffer.put(i, Coloring.argb(a, 0, 0, 0));
			}
		}
		// render the drawable to the image
		drawable.drawTo(gfx, rect, position);
		gfx.dispose();
		// hack to fix up color
		for (int i = 0; i < capacity; i++) {
			int v = buffer.get(i);
			int r = Argb.red(v);
			int g = Argb.green(v);
			int b = Argb.blue(v);
			//int a = Coloring.alpha(v);
			int a = alpha == null ? 255 : alpha[i];
			buffer.put(i, Argb.argb(a, b, g, r));
		}
	}

	public static BufferedImageInt createImage(Resolution res) {
		return DirectDataBufferInt.createBufferedImage(res.h, res.v, BufferedImage.TYPE_INT_ARGB, null, null);
	}

	public static void writeParams(ShaderParams[] params, FloatBuffer buffer) {
		for (ShaderParams p : params) {
			p.writeTo(buffer);
		}
	}

	//TODO rename
	public static float[] toFloats(ShaderParams[] params) {
		float[] floats = new float[params.length * 4];
		writeParams(params, FloatBuffer.wrap(floats));
		return floats;
	}

	final float[] ps = new float[4];

	ShaderParams() { }

	public void writeTo(FloatBuffer buffer) {
		buffer.put(ps);
	}

	public void writeTo(FloatBuffer buffer, int copies) {
		for (int i = 0; i < copies; i++) {
			buffer.put(ps);
		}
	}

	public float[] toFloats(int copies) {
		float[] fs = new float[4 * copies];
		for (int j = 0; j < fs.length;) {
			fs[j++] = ps[0];
			fs[j++] = ps[1];
			fs[j++] = ps[2];
			fs[j++] = ps[3];
		}
		return fs;
	}

	public static final class EmptyParams extends ShaderParams {

		private static final EmptyParams instance = new EmptyParams();

		public static final EmptyParams instance() { return instance; }

		public static Creator<EmptyParams> creator = new Creator<>(EmptyParams.class, () -> instance);

		private EmptyParams() { }

		@Override
		public void writeTo(FloatBuffer buffer, int copies) {
			DisplayUtil.putZeros(buffer, 4 * copies);
		}

		public static EmptyParams[] create(int n) {
			EmptyParams[] params = new EmptyParams[n];
			Arrays.fill(params, instance);
			return params;
		}

}

	public static final class FreeParams extends ShaderParams {

		public static Creator<FreeParams> creator = new Creator<>(FreeParams.class, FreeParams::new);

		public static FreeParams instance(float p0, float p1, float p2, float p3) {
			FreeParams params = new FreeParams();
			params.setParams(p0, p1, p2, p3);
			return params;
		}

		public void set0(float v) { ps[0] = v; }
		public void set1(float v) { ps[1] = v; }
		public void set2(float v) { ps[2] = v; }
		public void set3(float v) { ps[3] = v; }

		public float get0() { return ps[0]; }
		public float get1() { return ps[1]; }
		public float get2() { return ps[2]; }
		public float get3() { return ps[3]; }

		public void setParams(float p0, float p1, float p2, float p3) {
			ps[0] = p0;
			ps[1] = p1;
			ps[2] = p2;
			ps[3] = p3;
		}

		public float[] getParams() {
			return ps.clone();
		}

		private FreeParams() { }
	}

	public abstract static class TimeParams extends ShaderParams {

		private TimeParams() {}

		public void setFadeInStartTime(float time) { setRange(time, ps[3]); }
		public void setFadeInFinishTime(float time) { setDurations(time, ps[2]); }
		public void setFadeOutStartTime(float time) { setDurations(ps[1], time); }
		public void setFadeOutFinishTime(float time) { setRange(ps[0], ps[3]); }

		public float getFadeInStartTime() { return ps[0]; }
		public float getFadeInFinishTime() { return ps[1]; }
		public float getFadeOutStartTime() { return ps[2]; }
		public float getFadeOutFinishTime() { return ps[3]; }

		public void setRangeAndDurations(float startTime, float inDuration, float outDuration, float finishTime) {
			// make parameters rational
			startTime = nonNeg(startTime);
			finishTime = nonNeg(finishTime);
			if (finishTime < startTime) finishTime = startTime;

			// apply parameters
			ps[0] = startTime;
			ps[3] = finishTime;

			// make consistent
			setDurations(inDuration, outDuration);
		}

		public void setRange(float startTime, float finishTime) {
			setRangeAndDurations(startTime, ps[1], ps[2], finishTime);
		}

		public void setDurations(float inDuration, float outDuration) {
			// make parameters rational
			inDuration = nonNeg(inDuration);
			outDuration = nonNeg(outDuration);
			float maxDuration = ps[3] - ps[0];
			float totalDuration = inDuration + outDuration;
			if (totalDuration > maxDuration) {
				float s = maxDuration / totalDuration;
				inDuration *= s;
				outDuration *= s;
			}

			// apply parameters
			ps[1] = ps[0] + inDuration;
			ps[2] = ps[3] - outDuration;
		}

	}

	public abstract static class ColorParams extends ShaderParams {

		public static void quadColors(Coloring coloring, ColorParams[] params) {
			params[0].setColor(coloring.tl);
			params[1].setColor(coloring.tr);
			params[2].setColor(coloring.br);
			params[3].setColor(coloring.bl);
		}

		public void setColor(int argb) {
			Coloring.writeColorAsFloats(argb, ps, 0);
		}

		public int getColor() {
			return Coloring.readColorAsFloats(ps, 0);
		}

		ColorParams() { }
	}

	public abstract static class OpaqueColorParams extends ShaderParams {

		public static void quadColors(Coloring coloring, OpaqueColorParams[] params) {
			params[0].setColor(coloring.tl);
			params[1].setColor(coloring.tr);
			params[2].setColor(coloring.br);
			params[3].setColor(coloring.bl);
		}

		public void setColor(int argb) {
			Coloring.writeOpaqueColorAsFloats(argb, ps, 0);
		}

		public int getColor() {
			return Coloring.readOpaqueColorAsFloats(ps, 0);
		}

		OpaqueColorParams() { }
	}

	// controls specular
	public static final class PlainParams extends ColorParams {

		public static Creator<PlainParams> creator = new Creator<>(PlainParams.class, PlainParams::new);

		public static PlainParams instance(int argb) {
			PlainParams params = new PlainParams();
			params.setColor(argb);
			return params;
		}

		private PlainParams() {}
	}

	// controls specular
	public static final class PlateParams extends ColorParams {

		public static Creator<PlateParams> creator = new Creator<>(PlateParams.class, PlateParams::new);

		private PlateParams() {}
	}

	// controls outline
	public static final class TextParams extends ColorParams {

		public static Creator<TextParams> creator = new Creator<>(TextParams.class, TextParams::new);

		private TextParams() {}
	}

	// controls exterior
	public static final class DiscParams extends ColorParams {

		public static Creator<DiscParams> creator = new Creator<>(DiscParams.class, DiscParams::new);

		private DiscParams() {}
	}

	// controls exterior
	public static final class MaskParams extends ColorParams {

		public static Creator<MaskParams> creator = new Creator<>(MaskParams.class, MaskParams::new);

		private MaskParams() {}
	}

	public static final class TransParams extends TimeParams {

		public static Creator<TransParams> creator = new Creator<>(TransParams.class, TransParams::new);

		public static void renderToImage(BufferedImageInt image, Drawable drawable, Drawable transition) {
			renderToImageImpl(image, drawable, transition);
		}

		private TransParams() { }

	}

	public static final class FadeParams extends TimeParams {

		public static Creator<FadeParams> creator = new Creator<>(FadeParams.class, FadeParams::new);

		public static void renderToImage(BufferedImageInt image, Drawable drawable) {
			renderToImageImpl(image, drawable, null);
		}

		public static void renderToImage(BufferedImageInt image, Drawable drawable, Drawable transition) {
			renderToImageImpl(image, drawable, transition);
		}

		private FadeParams() { }

	}

	public static final class PlasmaParams extends ShaderParams {

		public static Creator<PlasmaParams> creator = new Creator<>(PlasmaParams.class, PlasmaParams::new);

		private PlasmaParams() {}

		public void setScale(float scale) {
			ps[0] = scale;
			ps[1] = scale;
		}

		public void setScale(float scaleX, float scaleY) {
			ps[0] = scaleX;
			ps[1] = scaleY;
		}

		public void setOffset(float offsetX, float offsetY) {
			ps[2] = offsetX;
			ps[3] = offsetY;
		}

		public void setScaleAndOffset(float scaleX, float scaleY, float offsetX, float offsetY) {
			setScale(scaleX, scaleY);
			setOffset(offsetX, offsetY);
		}
	}

	public static final class NoiseParams extends OpaqueColorParams {

		public static Creator<NoiseParams> creator = new Creator<>(NoiseParams.class, NoiseParams::new);

		private NoiseParams() {}
	}

	public static final class BorderParams extends OpaqueColorParams {

		public static Creator<BorderParams> creator = new Creator<>(BorderParams.class, BorderParams::new);

		private BorderParams() {}

		public void setBorderWidth(float width) {
			ps[3] = width;
		}

		public float getBorderWidth() {
			return ps[3];
		}
	}

	public static final class PulseParams extends ShaderParams {

		public static Creator<PulseParams> creator = new Creator<>(PulseParams.class, PulseParams::new);

		public static PulseParams instance(float startTime, float period, float bandWidth, float bandProportion) {
			PulseParams params = new PulseParams();
			params.setTimings(startTime, period);
			params.setBanding(bandWidth, bandProportion);
			return params;
		}

		private PulseParams() {}

		public void setTimings(float startTime, float period) {
			ps[0] = startTime;
			ps[1] = period;
		}

		public void setBanding(float bandWidth, float bandProportion) {
			ps[2] = bandWidth;
			ps[3] = bandProportion;
		}

		public float getStartTime()      { return ps[0]; }
		public float getPeriod()         { return ps[1]; }
		public float getBandWidth()      { return ps[2]; }
		public float getBandProportion() { return ps[3]; }

	}

	public static class ConsoleParams extends ShaderParams {

		public static Creator<ConsoleParams> creator = new Creator<>(ConsoleParams.class, ConsoleParams::new);

		public static ConsoleParams instance(int cols, int rows) {
			ConsoleParams p = new ConsoleParams();
			p.setDimensions(cols,rows);
			return p;
		}

		private ConsoleParams() {}

		public void setDimensions(int cols, int rows) {
			if (cols < 1 || cols > 65535) throw new IllegalArgumentException("invalid cols");
			if (rows < 1 || rows > 65535) throw new IllegalArgumentException("invalid rows");
			ps[0] = cols;
			ps[1] = rows;
		}

	}

	public static final class Creator<P> {

		private final Producer<P> producer;
		private final P[] empty;

		Creator(Class<P> clss, Producer<P> producer) {
			this.producer = producer;
			empty = (P[]) Array.newInstance(clss, 0);
		}

		public Producer<P> producer() {
			return producer;
		}

		public P create() {
			return producer.produce();
		}

		public P[] create(int n) {
			P[] array = Arrays.copyOf(empty, n);
			for (int i = 0; i < n; i++) {
				array[i] = producer.produce();
			}
			return array;
		}

	}
}
