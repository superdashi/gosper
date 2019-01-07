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
