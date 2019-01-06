package com.superdashi.gosper.studio;

import java.awt.Paint;

// future canvas implementations may defer rendering and compile command lists
// so we want shaders to be cheap to construct
// but when they are reused for immediate rendering
// we want to avoid rebuilding paint objects (which can be expensive)
// so we cache these
public abstract class Shader {

	private Paint paint = null;

	Shader() { }

	final Paint toPaint() {
		return paint == null ? paint = createPaint() : paint;
	}

	abstract Paint createPaint();

}
