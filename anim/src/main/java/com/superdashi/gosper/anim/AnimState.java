package com.superdashi.gosper.anim;


public class AnimState {

	private static final float[] DEFAULT_RGBA = {0f, 0f, 0f, 0f};
	private static final float[] DEFAULT_HSV = {0f, 0.5f, 0.5f};

	private static float combineScales(float s1, float s2) {
		float p = Math.abs(s1 - 0.5f) * 2f;
		return s1 * p + s2 * (1f - p);
	}

	private static boolean rgbPopulated(float[] rgb) {
		return rgb != null && rgb[3] != 0f;
	}

	private static boolean hsvPopulated(float[] hsv) {
		return hsv != null && (hsv[0] != 0f || hsv[1] != 0.5f || hsv[2] != 0.5f);
	}

	private static void copy(float[] as, float[] bs, float[] cs, int offset) {
		float[] xs = as == null ? bs : as;
		System.arraycopy(xs, 0, cs, offset, xs.length);
	}

	private static float[] combineRGB(float[] as, float[] bs) {
		if (bs == null) return as;
		if (as == null) return bs.clone();

		float a = as[3];
		float b = bs[3];
		float sum = a + b;
		if (sum == 0f) return bs.clone();

		as[0] = (as[0] * a + bs[0] * b) / sum;
		as[1] = (as[1] * a + bs[1] * b) / sum;
		as[2] = (as[2] * a + bs[2] * b) / sum;
		as[3] = sum - a * b;
		return as;
	}

	private float[] combineHSV(float[] as, float[] bs) {
		if (bs == null) return as;
		if (as == null) return bs.clone();
		as[0] += bs[0];
		as[1] = combineScales(as[1], bs[1]);
		as[2] = combineScales(as[2], bs[2]);
		return as;
	}

	public static AnimState neutral() {
		return new AnimState();
	}

	public static AnimState translate(float x, float y, float z) {
		return new AnimState(x,y,z);
	}

	public static AnimState scale(float sx, float sy, float sz) {
		return new AnimState(sx, sy, sz, true);
	}

	public static AnimState rotate(float rx, float ry, float rz, float ra) {
		return new AnimState(rx, ry, rz, ra);
	}

	public static AnimState color(boolean c2, float r, float g, float b, float a) {
		AnimState as = new AnimState();
		as.setRGBA(c2, new float[] {r, g, b, a});
		return as;
	}

	public static AnimState rotateHue(boolean c2, float h) {
		AnimState as = new AnimState();
		as.setHSV(c2, new float[] { h, 0.5f, 0.5f });
		return as;
	}

	public static AnimState scaleSaturation(boolean c2, float s) {
		AnimState as = new AnimState();
		as.setHSV(c2, new float[] { 0f, s, 0.5f });
		return as;
	}

	public static AnimState scaleValue(boolean c2, float v) {
		AnimState as = new AnimState();
		as.setHSV(c2, new float[] { 0f, 0.5f, v });
		return as;
	}

	private float x;
	private float y;
	private float z;

	private float sx;
	private float sy;
	private float sz;

	private float[] rot = null;

	private float[] rgba1 = null; // 4 els
	private float[] hsv1  = null; // 3 els
	private float[] rgba2 = null; // 4 els
	private float[] hsv2  = null; // 3 els

	private AnimState() {
		x =  0f;
		y =  0f;
		z =  0f;

		sx = 1f;
		sy = 1f;
		sz = 1f;
	}

	private AnimState(float x, float y, float z) {
		this();
		this.x = x;
		this.y = y;
		this.z = z;
	}

	private AnimState(float sx, float sy, float sz, boolean scaleDummy) {
		this();
		this.sx = sx;
		this.sy = sy;
		this.sz = sz;
	}

	private AnimState(float rx, float ry, float rz, float ra) {
		this();
		rot = new float[9];

		float c = (float) Math.cos(ra);
		float s = (float) Math.sin(ra);
		float t = 1 - c;

		rot[0] = rx * rx * t + c;
		rot[4] = ry * ry * t + c;
		rot[8] = rz * rz * t + c;

		{
			float a = rx * ry * t;
			float b = rz      * s;
			rot[1] = a - b;
			rot[3] = a + b;
		}

		{
			float a = rx * rz * t;
			float b = ry      * s;
			rot[2] = a + b;
			rot[6] = a - b;
		}

		{
			float a = ry * rz * t;
			float b = rx      * s;
			rot[5] = a - b;
			rot[7] = a + b;
		}
	}

	private void setRGBA(boolean c2, float[] rgba) {
		if (c2) {
			rgba2 = rgba;
		} else {
			rgba1 = rgba;
		}
	}

	private void setHSV(boolean c2, float[] hsv) {
		if (c2) {
			hsv2 = hsv;
		} else {
			hsv1 = hsv;
		}
	}

	public void apply(AnimState state) {
		this.x  += state.x;
		this.y  += state.y;
		this.z  += state.z;
		this.sx *= state.sx;
		this.sy *= state.sy;
		this.sz *= state.sz;

		if (this.rot == null) {
			this.rot = state.rot;
		} else if (state.rot != null) {
			float[] tr = this.rot;   // (i)
			float[] sr = state.rot;  // (j)
			this.rot = new float[9]; // (k)
			for (int k = 0; k < 9; k++) {
				int i = k % 3;
				int j = k - i;
				rot[k] = sr[j] * tr[i] + sr[j+1] * tr[i+3] + sr[j+2] * tr[i+6];
			}
		}

		// note can only approximate application of both colors
		this.rgba1 = combineRGB(this.rgba1, state.rgba1);
		this.rgba2 = combineRGB(this.rgba2, state.rgba2);
		this.hsv1  = combineHSV(this.hsv1,  state.hsv1 );
		this.hsv2  = combineHSV(this.hsv2,  state.hsv2 );

	}

	public void populate(float[] trans, float[] color) {
		if (rot == null) { // scale
			trans[ 0] = sx;
			trans[ 1] = 0f;
			trans[ 2] = 0f;
			trans[ 4] = 0f;
			trans[ 5] = sy;
			trans[ 6] = 0f;
			trans[ 8] = 0f;
			trans[ 9] = 0f;
			trans[10] = sz;
		} else { // scale and rotate
			trans[ 0] = rot[0] * sx;
			trans[ 1] = rot[3] * sx;
			trans[ 2] = rot[6] * sx;
			trans[ 4] = rot[1] * sy;
			trans[ 5] = rot[4] * sy;
			trans[ 6] = rot[7] * sy;
			trans[ 8] = rot[2] * sz;
			trans[ 9] = rot[5] * sz;
			trans[10] = rot[8] * sz;
		}
		// translation
		trans[12] = x;
		trans[13] = y;
		trans[14] = z;
		// fixed
		trans[ 3] = 0f;
		trans[ 7] = 0f;
		trans[11] = 0f;
		trans[15] = 1f;

		// color
		copy(rgba1, DEFAULT_RGBA, color,  0);
		copy(hsv1,  DEFAULT_HSV,  color,  4);
		copy(rgba2, DEFAULT_RGBA, color,  8);
		copy(hsv2,  DEFAULT_HSV,  color, 12);
		color[ 7] = hsvPopulated(hsv1) ? 1f : 0f;
		color[15] = hsvPopulated(hsv2) ? 1f : 0f;

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (x != 0f || y != 0f || z != 0f) {
			sb.append("p (").append(x).append(",").append(y).append(",").append(z).append(")");
		}
		if (sx != 1f || sy != 1f || sz != 1f) {
			if (sb.length() != 0) sb.append(' ');
			if (sz == sy && sy == sz) {
				sb.append("s (").append(sz).append(")");
			} else {
				sb.append("s (").append(sx).append(",").append(sy).append(",").append(sz).append(")");
			}
		}
		if (rot != null) {
			if (sb.length() != 0) sb.append(' ');
			sb.append("r ")
			.append("[").append(rot[0]).append(",").append(rot[1]).append(",").append(rot[2]).append("]")
			.append("[").append(rot[3]).append(",").append(rot[4]).append(",").append(rot[5]).append("]")
			.append("[").append(rot[6]).append(",").append(rot[7]).append(",").append(rot[8]).append("]");
		}
		if (rgbPopulated(rgba1)) appendArray(sb, "rgba1", rgba1);
		if (rgbPopulated(rgba2)) appendArray(sb, "rgba2", rgba2);
		if (hsvPopulated(hsv1)) appendArray(sb, "hsv1", hsv1);
		if (hsvPopulated(hsv2)) appendArray(sb, "hsv2", hsv2);
		return sb.toString();
	}

	private static void appendArray(StringBuilder sb, String name, float[] fs) {
		if (sb.length() != 0) sb.append(' ');
		sb.append(name);
		for (int i = 0; i < fs.length; i++) {
			sb.append(i == 0 ? ':' : ',').append(fs[i]);
		}
	}
}
