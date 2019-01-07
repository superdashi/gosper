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

import com.superdashi.gosper.model.Model;
import com.superdashi.gosper.model.ModelMaterial;
import com.superdashi.gosper.model.Vector3;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;

public class ModelElement extends Element {

	private final Model model;
	private final BitStore render;
	private final RenderPhase phase;
	private final RenderState required;
	private final ShaderParams params;

	private final int vertexCount;
	private final int[] ranges;

	public ModelElement(Model model, RenderPhase phase, RenderState required, ShaderParams params) {
		this(model, Bits.oneBits(model.matCounts.length), phase, required, params);
	}

	public ModelElement(Model model, BitStore render, RenderPhase phase, RenderState required, ShaderParams params) {
		this.model = model;
		this.render = render;
		this.phase = phase;
		this.required = required;
		this.params = params;

		setHandle(new Vector3(model.handle).toVertex());

		//first count number of runs and count vertices
		int rc = 0;
		int[] cs = model.matCounts;
		int[] cm = new int[cs.length];
		{
			int fc = 0;
			boolean p = false;
			for (int i = 0; i < cs.length; i++) {
				boolean r = render.getBit(i);
				int c = cs[i];
				cm[i] = (i == 0 ? 0 : cm[i - 1]) + c;
				if (r) {
					fc += c;
					if (!p) {
						rc ++;
						p = true;
					}
				} else {
					p = false;
				}
			}
			vertexCount = fc * 3;
		}

		ranges = new int[rc * 2];
		{
			int ri = 0;
			boolean p = false;
			for (int i = 0; i < cs.length; i++) {
				boolean r = render.getBit(i);
				if (r) {
					if (!p) { // start of range
						p = true;
						ranges[ri++] = i == 0 ? 0 : cm[i - 1];
					}
				} else {
					if (p) { // end of range
						p = false;
						ranges[ri++] = cm[i - 1];
					}
				}
			}
			if (ri < ranges.length) ranges[ri++] = cm[cs.length - 1];
		}
	}

	@Override
	int getVertexCount() {
		return vertexCount;
	}

	@Override
	RenderPhase getRenderPhase() {
		return phase;
	}

	@Override
	RenderState getRequiredState() {
		return required;
	}

	@Override
	void appendTo(ElementData data) {
		for (int i = 0; i < ranges.length; i += 2) {
			int from = ranges[i * 2];
			int to = ranges[i * 2 + 1];
			int length = to - from;
			data.vertices.put(model.vertices, from * 9, length * 9);
			data.normals.put(model.normals, from * 9, length * 9);
			data.texCoords.put(model.texCoords, from * 6, length * 6);
		}

		ModelMaterial[] materials = model.materials;
		int[] matCounts = model.matCounts;
		for (int i = 0; i < matCounts.length; i++) {
			if (!render.getBit(i)) {
				index += matCounts[i] * 9;
				continue;
			}
			ModelMaterial mat = materials[i];
			int c = matCounts[i] * 3;
			for (int j = 0; j < c; j++) {
				data.colors.put(mat.color);
			}
		}
		params.writeTo(data.shaders, vertexCount);
	}

}
