package com.superdashi.gosper.studio;

import java.awt.Color;
import java.awt.Paint;

final class ClearShader extends Shader {

	private static final ClearShader instance = new ClearShader();
	private static final Paint paint = new Color(0x00000000, true);

	static ClearShader instance() { return instance; }

	private ClearShader() { }

	@Override Paint createPaint() { return paint; }

}
