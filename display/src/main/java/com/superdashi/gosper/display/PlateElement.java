/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
