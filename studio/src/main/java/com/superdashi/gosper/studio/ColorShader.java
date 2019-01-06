package com.superdashi.gosper.studio;

import java.awt.Color;
import java.awt.Paint;

final class ColorShader extends Shader {

	final int argb;

	ColorShader(int argb) {
		this.argb = argb;
	}

	@Override
	Paint createPaint() {
		return new Color(argb, true);
	}

}
