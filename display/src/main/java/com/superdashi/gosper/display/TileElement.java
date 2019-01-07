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
