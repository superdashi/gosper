package com.superdashi.gosper.display;

import com.jogamp.opengl.GLES2;

public enum Wrap {

	CLAMP(GLES2.GL_CLAMP_TO_EDGE, GLES2.GL_CLAMP_TO_BORDER),
	MIRROR(GLES2.GL_MIRRORED_REPEAT, GLES2.GL_MIRRORED_REPEAT),
	REPEAT(GLES2.GL_REPEAT, GLES2.GL_REPEAT);

	private final int edgeConstant;
	private final int borderConstant;

	private static final Wraps[] wraps = {
		new Wraps(CLAMP,   CLAMP),
		new Wraps(CLAMP,  MIRROR),
		new Wraps(CLAMP,  REPEAT),
		new Wraps(MIRROR,  CLAMP),
		new Wraps(MIRROR, MIRROR),
		new Wraps(MIRROR, REPEAT),
		new Wraps(REPEAT,  CLAMP),
		new Wraps(REPEAT, MIRROR),
		new Wraps(REPEAT, REPEAT),
	};

	public static Wraps wraps(Wrap horizontal, Wrap vertical) {
		if (horizontal == null) throw new IllegalArgumentException("null horizontal");
		if (vertical == null) throw new IllegalArgumentException("null vertical");
		return wraps[horizontal.ordinal() * 3 + vertical.ordinal()];
	}

	private Wrap(int edgeConstant, int borderConstant) {
		this.edgeConstant = edgeConstant;
		this.borderConstant = borderConstant;
	}

	public int glConstant(boolean useBorder) {
		return useBorder ? borderConstant : edgeConstant;
	}

	public static final class Wraps {

		public final Wrap vertical;
		public final Wrap horizontal;

		private Wraps(Wrap horizontal, Wrap vertical) {
			this.horizontal = horizontal;
			this.vertical = vertical;
		}
	}
}
