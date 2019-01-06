package com.superdashi.gosper.display;

import com.jogamp.opengl.GL;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Coloring.Corner;
import com.superdashi.gosper.display.ShaderParams.ColorParams;
import com.superdashi.gosper.display.ShaderParams.DiscParams;
import com.superdashi.gosper.model.Vector3;
import com.tomgibara.geom.core.Rect;

public class CircleElement extends RectElement {

	private static Rect square(Vector3 center, float r) {
		//TODO optimize?
		return center.projectionZ().vectorFromOrigin().asTranslation().transform(Rect.centerAtOrigin(r * 2, r * 2));
	}

	private final RenderState state;
	private final int[] colors;
	private final DiscParams[] params;
	private final float[] texCoords;
	private final boolean opaque;

	public CircleElement(Vector3 center, float r, Coloring fg, Coloring bg, boolean lit) {
		super(square(center, r), center.z);
		state = new RenderState();
		state.setMode(Mode.DISC.lit(lit));
		state.setBlend(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		colors = fg.asQuadInts();
		params = DiscParams.creator.create(4);
		ColorParams.quadColors(bg, params);
		texCoords = centralTexCoords(Corner.BL);
		opaque = fg.isOpaque() && bg.isOpaque();
	}

	@Override
	RenderState getRequiredState() {
		return state;
	}

	@Override
	RenderPhase getRenderPhase() {
		return RenderPhase.PANEL;
	}

	@Override
	boolean isOpaque() {
		return opaque;
	}

	@Override
	public void appendTo(ElementData data) {
		super.appendTo(data);
		data.colors.put(colors);
		data.texCoords.put(texCoords);
		for (int i = 0; i < 4; i++) {
			params[i].writeTo(data.shaders);
		}
	}
}
