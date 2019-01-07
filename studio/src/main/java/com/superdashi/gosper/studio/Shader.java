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
package com.superdashi.gosper.studio;

import java.awt.Paint;

// future canvas implementations may defer rendering and compile command lists
// so we want shaders to be cheap to construct
// but when they are reused for immediate rendering
// we want to avoid rebuilding paint objects (which can be expensive)
// so we cache these
public abstract class Shader {

	private Paint paint = null;

	Shader() { }

	final Paint toPaint() {
		return paint == null ? paint = createPaint() : paint;
	}

	abstract Paint createPaint();

}
