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

import static com.superdashi.gosper.display.DisplayUtil.transformedCoords;

import java.nio.IntBuffer;

import com.superdashi.gosper.color.Coloring;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.transform.Transform;

public class HexElement extends Element {


	static final float X = Angles.SIN_PI_BY_THREE;
	static final float Y = Angles.COS_PI_BY_THREE;
	static final float[] COORDS = {
		0, 0,    1,  0,    X,  Y,
		0, 0,    X,  Y,   -X,  Y,
		0, 0,   -X,  Y,   -1,  0,
		0, 0,   -1,  0,   -X, -Y,
		0, 0,   -X, -Y,    X, -Y,
		0, 0,    X, -Y,    1,  0,
	};
	static final float[] NORMAL = {0f, 0f, 1f};

	private final RenderState required = new RenderState();
	private final float[] vertices;
	private final float[] texCoords;
	private final int[] colors = new int[6 * 3];
	private final ShaderParams params;

	public HexElement(Transform vt, Transform tt, float z, Mode mode, int innerColor, int outerColor, ShaderParams params) {
		required.setMode(mode);
		vertices = DisplayUtil.projectToZ(transformedCoords(vt, COORDS), z);
		texCoords = transformedCoords(tt, COORDS);
		int ic = Coloring.argbToRGBA(innerColor);
		int oc = Coloring.argbToRGBA(outerColor);
		IntBuffer bgb = IntBuffer.wrap(colors);
		for (int i = 0; i < 6; i++) {
			bgb.put(ic).put(oc).put(oc);
		}

		this.params = params;
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
	int getVertexCount() {
		return 18;
	}

	@Override
	public void appendTo(ElementData data) {
		data.vertices.put(vertices);
		for (int i = 0; i < 18; i++) data.normals.put(NORMAL);
		data.colors.put(colors);
		data.texCoords.put(texCoords);
		params.writeTo(data.shaders, 18);
	}

}
