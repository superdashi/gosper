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