package com.superdashi.gosper.display;

import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Coloring.Corner;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;

public class TileElement extends RectElement {

	private final RenderState required = new RenderState();
	private final Coloring coloring;
	private final float[] texCoords;
	private final float[] shader;

	public TileElement(Rect r, float z, Coloring coloring, Mode mode, Transform tex, float[] shader) {
		super(r, z);
		required.setMode(mode);
		this.coloring = coloring;
		texCoords = DisplayUtil.transformedCoords(tex, centralTexCoords(Corner.BL));
		this.shader = shader;
	}

	@Override
	RenderPhase getRenderPhase() {
		return RenderPhase.PANEL;
	}

	@Override
	RenderState getRequiredState() {
		return required;
	}

	@Override
	public void appendTo(ElementData data) {
		super.appendTo(data);
		data.colors.put(coloring.asQuadInts());
		data.texCoords.put(texCoords);
		if (shader != null) {
			for (int i = 0; i < 4; i++) {
				data.shaders.put(shader);
			}
		}
	}

}
