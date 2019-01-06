package com.superdashi.gosper.display;

import com.jogamp.opengl.GL;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.display.ShaderParams.ColorParams;
import com.superdashi.gosper.display.ShaderParams.PlateParams;
import com.tomgibara.geom.core.Rect;

//TODO could pass in requirement to share objects
public class PlateElement extends RectElement {

	//TODO needs to use mode which is RGB texture
	private final RenderState required;
	private final int[] diffuse;
	private final PlateParams[] params;
	private final float[] texCoords;

	public PlateElement(Rect rect, float z, Coloring diffuse, Coloring specular, DynamicAtlas<?>.Updater updater) {
		super(rect, z);
		required = new RenderState(Mode.PLATE);
		required.setBlend(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		required.setTexture(updater.getTexture());
		this.diffuse = diffuse.asQuadInts();
		params = PlateParams.creator.create(4);
		ColorParams.quadColors(specular, params);
		Rect r = updater.getRect();
		texCoords = new float[] {
				r.minX, r.minY,
				r.maxX, r.minY,
				r.maxX, r.maxY,
				r.minX, r.maxY,
		};
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
		data.colors.put(diffuse);
		for (int i = 0; i < 4; i++) {
			params[i].writeTo(data.shaders);
		}
		data.texCoords.put(texCoords);
	}

}
