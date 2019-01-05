package com.superdashi.gosper.color;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public final class Palette {

	private static final int OPAQUE = 0xff000000;

	public static final int SIZE = 16;

	public enum LogicalColor {
		DRK('_'),
		DRK_BCK('b'),
		DRK_FOR('f'),
		DRK_ACC('a'),
		DRK_SUC('s'),
		DRK_WRN('w'),
		DRK_ERR('e'),
		DRK_INF('i'),
		LGH('*'),
		LGH_BCK('B'),
		LGH_FOR('F'),
		LGH_ACC('A'),
		LGH_SUC('S'),
		LGH_WRN('W'),
		LGH_ERR('E'),
		LGH_INF('I'),
		;

		private static final LogicalColor[] values = values();
		private static final LogicalColor[] lookup = new LogicalColor[128];

		static {
			for (LogicalColor color : values) {
				lookup[color.c] = color;
			}
		}

		public static LogicalColor valueOf(int ordinal) {
			try {
				return values[ordinal];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("invalid ordinal: " + ordinal);
			}
		}

		public static LogicalColor valueOf(char c) {
			LogicalColor color = c >= 128 ? null : lookup[c];
			if (color == null) throw new IllegalArgumentException("invalid c");
			return color;
		}

		public final char c;
		public final boolean light;

		private LogicalColor(char c) {
			this.c = c;
			light = ordinal() >= 8;
		}

		public LogicalColor light() {
			return light ? this : values[ordinal() + 8];
		}

		public LogicalColor dark() {
			return light ? values[ordinal() - 8] : this;
		}
	}

	private static int[] newColors() {
		int[] colors = new int[16];
		Arrays.fill(colors, OPAQUE);
		return colors;
	}

	public static final class Builder {

		private int[] colors;

		private Builder() {
			colors = null;
		}

		private Builder(int[] colors) {
			this.colors = colors;
		}

		public Builder color(LogicalColor logical, int color) {
			if (logical == null) throw new IllegalArgumentException("null logical");
			colors()[logical.ordinal()] = color | OPAQUE;
			return this;
		}

		public Builder darken() {
			for (int i = 0; i < Palette.SIZE; i++) {
				colors[i] = Argb.darker(colors[i]);
			}
			return this;
		}

		public Builder lighten() {
			for (int i = 0; i < Palette.SIZE; i++) {
				colors[i] = Argb.lighter(colors[i]);
			}
			return this;
		}

		public Palette build() {
			return new Palette(this);
		}

		private int[] colors() {
			return colors == null ? colors = newColors() : colors;
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(int... colors) {
		if (colors == null) throw new IllegalArgumentException("null colors");
		if (colors.length != 16) throw new IllegalArgumentException("not 16 colors");
		int[] cs = new int[16];
		for (int i = 0; i < 16; i++) {
			cs[i] = colors[i] | OPAQUE;
		}
		return new Builder(cs);
	}

	private final int[] colors;
	private int hashCode = 0;

	private Palette(int[] colors) {
		this.colors = colors;
	}

	private Palette(Builder builder) {
		this(builder.colors());
		builder.colors = null;
	}

	public void writeTo(IntBuffer buffer) {
		// incorrect, needs to be rgba
//		buffer.put(colors);
		for (int i = 0; i < colors.length; i++) {
			buffer.put((colors[i] << 8) | 0xff);
		}
	}

	public int[] asOpaqueInts() {
		return colors.clone();
	}

	public float[] asOpaqueFloats() {
		float[] floats = new float[16 * 3];
		FloatBuffer b = FloatBuffer.wrap(floats);
		for (int i = 0; i < 16; i++) {
			Coloring.writeOpaqueColor(colors[i], b);
		}
		return floats;
	}

	public int color(LogicalColor logical) {
		if (logical == null) throw new IllegalArgumentException("null logical");
		return colors[logical.ordinal()];
	}

	public LogicalColor nearestLogicalColor(int color) {
		return LogicalColor.values[nearestColorIndex(color)];
	}

	public int nearestColor(int color) {
		return colors[nearestColorIndex(color)];
	}

	public Builder builder() {
		return new Builder(colors.clone());
	}

	@Override
	public int hashCode() {
		return hashCode == 0 ? hashCode = Arrays.hashCode(colors) : hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Palette)) return false;
		Palette that = (Palette) obj;
		return Arrays.equals(this.colors, that.colors);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LogicalColor logical: LogicalColor.values) {
			if (logical.ordinal() != 0) sb.append(',');
			sb.append(logical).append('=').append(Argb.toString(colors[logical.ordinal()]));
		}
		return sb.toString();
	}

	private int nearestColorIndex(int color) {
		int cr = Argb.red  (color);
		int cg = Argb.green(color);
		int cb = Argb.blue (color);

		int bi = -1;
		int bd = Integer.MAX_VALUE;
		for (int i = 1; i < 16; i++) {
			int c = colors[i];
			int dr = cr - Argb.red  (c);
			int dg = cg - Argb.green(c);
			int db = cb - Argb.blue (c);
			int d = dr*dr + dg*dg + db*db;
			if (d < bd) {
				bi = i;
				bd = d;
			}
		}

		return bi;
	}

}
