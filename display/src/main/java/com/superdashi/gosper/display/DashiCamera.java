package com.superdashi.gosper.display;

import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.PMVMatrix;
import com.superdashi.gosper.core.DashiLog;

// design: camera sits at origin
// dashboard plane is at 1

public class DashiCamera {

	public static final float DASH_DEPTH = 2f;
	public static final float BACK_DEPTH = 3f; // distance in addition to the DASH_DEPTH
	public static final float OVER_DEPTH = 1f; // distance from the DASH_DEPTH

	public static final class Params {

		// width over height
		public final float aspect;
		public final float angle;
		// the width when the dimensions are scaled so that the smallest side is scaled to have unit length
		public final float unitW;
		// ditto for the height
		public final float unitH;

		Params(float aspect, float angle) {
			this.aspect = aspect;
			this.angle = angle;
			if (aspect == 1f) {
				unitW = 1f;
				unitH = 1f;
			} else if (aspect < 1f) {
				unitW = 1f;
				unitH = 1f / aspect;
			} else {
				unitW = 1f / aspect;
				unitH = 1f;
			}
		}
	}

	public final Params params;
	public final ArtPlane dash;
	public final ArtPlane back;
	public final ArtPlane over;

	//TODO maybe pass in width & height - could make unit calculation more accurate
	public DashiCamera(float aspect, float angle) {
		params = new Params(aspect, angle);

		double a = angle * Math.PI / 360; // half angle
		float t = (float) Math.tan(a); // tan of half angle
		float s = t * 2; // scale
		float hScale = aspect * s; // horizontal scale
		float vScale = s; // vertical scale
		dash = new ArtPlane(DASH_DEPTH, hScale, vScale);
		back = new ArtPlane(DASH_DEPTH + BACK_DEPTH, hScale, vScale);
		over = new ArtPlane(DASH_DEPTH - OVER_DEPTH, hScale, vScale);

		DashiLog.debug("CAMERA ASPECT " + aspect);
		DashiLog.debug("CAMERA HALF ANGLE " + a/Math.PI + "pi");
		DashiLog.debug("CAMERA TAN " + t);
	}

	public void configure(PMVMatrix m) {
		m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		m.glLoadIdentity();
		m.gluPerspective(params.angle, params.aspect, 0.1f, 10f);
		m.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		m.glLoadIdentity();
		m.glTranslatef(0f, 0f, -DashiCamera.DASH_DEPTH);
	}

}
