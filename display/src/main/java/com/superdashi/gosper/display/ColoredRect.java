package com.superdashi.gosper.display;

import com.jogamp.opengl.GL;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Coloring.Corner;
import com.tomgibara.geom.core.Rect;

final class ColoredRect extends RectElement {

	private final RenderState required = new RenderState();
	private final int[] colors;
	private final float[] texCoords;

	public ColoredRect(Rect rect, float z, Coloring color) {
		super(rect, z);
		required.setMode(Mode.PLAIN);
		if (!color.isOpaque()) {
			required.setBlend(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		}
		colors = color.asQuadInts();
		texCoords = defaultTexCoords(Corner.BL);
	}

	@Override
	RenderPhase getRenderPhase() {
		return RenderPhase.OVERLAY;
	}

	@Override
	RenderState getRequiredState() {
		return required;
	}

	@Override
	public void appendTo(ElementData data) {
		super.appendTo(data);
		data.colors.put(colors);
		data.texCoords.put(texCoords);
	}

}