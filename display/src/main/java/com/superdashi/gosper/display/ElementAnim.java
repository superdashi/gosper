package com.superdashi.gosper.display;

import com.jogamp.opengl.math.Matrix4;
import com.superdashi.gosper.anim.AnimSequence;
import com.superdashi.gosper.anim.AnimState;

final class ElementAnim extends AnimSequence {

	public final Matrix4 mat4;
	public final Matrix4 color;

	ElementAnim() {
		mat4 = new Matrix4();
		color = new Matrix4();
	}

	@Override
	protected void apply(AnimState state) {
		state.populate(mat4.getMatrix(), color.getMatrix());
	}
}
