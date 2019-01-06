package com.superdashi.gosper.micro;

import java.util.Optional;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.studio.ClearPlane;
import com.superdashi.gosper.studio.ColorPlane;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.LinearGradientPlane;
import com.superdashi.gosper.studio.Plane;
import com.superdashi.gosper.studio.Shader;
import com.superdashi.gosper.studio.Surface;
import com.superdashi.gosper.studio.TilingPlane;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public abstract class Background {

	private static final IntDimensions TWO_X_TWO = IntDimensions.of(2, 2);

	private static final ColorPlane whiteColor = new ColorPlane(Argb.WHITE);
	private static final ColorPlane blackColor = new ColorPlane(Argb.BLACK);
	private static final ColorPlane grayColor = new ColorPlane(Argb.GRAY);

	private static final Plane blackTiling = blackColor; // optimization
	private static final Plane darkGrayTiling = monoTiling(1);
	private static final Plane grayTiling = monoTiling(2);
	private static final Plane lightGrayTiling = monoTiling(3);
	private static final Plane whiteTiling = whiteColor; // optimization

	private static final PlaneAdapter nonAdapter = (c,p) -> p;

	private static PlaneBackground[] monoBackgrounds = {
			new PlaneBackground(blackTiling, nonAdapter),
			new PlaneBackground(darkGrayTiling, nonAdapter),
			new PlaneBackground(grayTiling, nonAdapter),
			new PlaneBackground(lightGrayTiling, nonAdapter),
			new PlaneBackground(whiteTiling, nonAdapter)
	};

	// value from 0 to 4; X -- black, . -- white

	// 0  1  2  3  4
	// XX _X _X __ __
	// XX XX X_ X_ __

	private static int tilePixel(boolean passed) {
		return passed ? Argb.WHITE : Argb.BLACK;
	}
	private static TilingPlane monoTiling(int value) {
		Surface tile = Surface.create(TWO_X_TWO, true);
		tile.writePixel(0, 0, tilePixel(value >= 1));
		tile.writePixel(1, 0, tilePixel(value >= 3));
		tile.writePixel(0, 1, tilePixel(value >= 4));
		tile.writePixel(1, 1, tilePixel(value >= 2));
		return new TilingPlane(tile);
	}

	private static final Background none = new PlaneBackground(ClearPlane.instance(), (c,p) -> c.opaque ? blackColor : p);

	public static Background none() {
		return none;
	}

	public static Background color(int argb) {
		if (Argb.alpha(argb) == 0) return none();
		return new PlaneBackground(new ColorPlane(argb), (c,p) -> {
			int color = argb;
			if (c.opaque) color = Argb.opaque(color);
			switch (c.qualifier.qualifier.color) {
			case MONO:
				int i = Argb.intensity(color);
				if (i < 32) return blackTiling;
				if (i < 96) return darkGrayTiling;
				if (i < 160) return grayTiling;
				if (i < 224) return lightGrayTiling;
				return whiteTiling;
			case GRAY: color = Argb.gray(Argb.intensity(color)); break;
			default: /* no change */
			}
			return color == argb ? p : new ColorPlane(color);
		});
	}

	public static Background coloring(Coloring coloring) {
		if (coloring == null) throw new IllegalArgumentException("null coloring");
		if (coloring.isTransparent()) return none();
		if (coloring.isVertical() || coloring.isHorizontal()) return new GradientBackground(coloring.isVertical(), coloring.tl, coloring.bl);
		throw new UnsupportedOperationException("general colorings not yet supported");
	}

	static Background mono(int value) {
		if (value < 0 || value >= monoBackgrounds.length) throw new IllegalArgumentException("invalid value");
		return monoBackgrounds[value];
	}

	Background() {
	}

	public boolean blank() { return this == none(); }
	public abstract boolean opaque();

	final Frame generate(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		return generateImpl(bounds);
	}

	abstract Frame generateImpl(IntRect bounds);

	abstract Optional<Shader> asShader();

	abstract Background adaptedFor(VisualSpec spec);

	private static class PlaneBackground extends Background {

		private final Plane plane;
		private final PlaneAdapter adapter;

		PlaneBackground(Plane plane, PlaneAdapter adapter) {
			this.plane = plane;
			this.adapter = adapter;
		}

		@Override
		public boolean opaque() {
			return plane.opaque();
		}

		@Override
		Frame generateImpl(IntRect bounds) {
			return plane.frame(bounds);
		}

		@Override
		Background adaptedFor(VisualSpec spec) {
			Plane adapted = adapter.adapt(spec, plane);
			return adapted.equals(plane) ? this : new PlaneBackground(adapted, adapter);
		}

		@Override
		Optional<Shader> asShader() {
			return Optional.of(plane.asShader());
		}
	}

	@FunctionalInterface
	private interface PlaneAdapter {

		Plane adapt(VisualSpec spec, Plane current);

	}

	private static class GradientBackground extends Background {

		private final boolean vertical;
		private final int argb1;
		private final int argb2;

		GradientBackground(boolean vertical, int argb1, int argb2) {
			this.vertical = vertical;
			this.argb1 = argb1;
			this.argb2 = argb2;
		}

		@Override
		public boolean opaque() {
			return Argb.isOpaque(argb1) && Argb.isOpaque(argb2);
		}

		@Override
		Frame generateImpl(IntRect bounds) {
			Plane plane = vertical ?
					new LinearGradientPlane(IntCoords.atY(bounds.minY), argb1, IntCoords.atY(bounds.maxY), argb2) :
					new LinearGradientPlane(IntCoords.atX(bounds.minX), argb1, IntCoords.atX(bounds.maxX), argb2);
			return plane.frame(bounds);
		}

		@Override
		Optional<Shader> asShader() {
			return Optional.empty();
		}

		@Override
		Background adaptedFor(VisualSpec spec) {
			GradientBackground bg = this;
			if (spec.opaque && !bg.opaque()) bg = new GradientBackground(vertical, Argb.opaque(argb1), Argb.opaque(argb2));
			switch (spec.qualifier.qualifier.color) {
			case MONO: return color(Argb.mix(bg.argb1, bg.argb2, 0.5f)).adaptedFor(spec);
			case GRAY: return new GradientBackground(vertical, Argb.gray(Argb.intensity(bg.argb1)), Argb.gray(Argb.intensity(bg.argb2)));
			default: return bg;
			}
		}

	}
}
