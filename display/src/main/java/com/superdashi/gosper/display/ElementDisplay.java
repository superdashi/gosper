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

import java.util.Collection;

import com.jogamp.opengl.GL2ES2;

public interface ElementDisplay {

	default void init(RenderContext context) { }

	Collection<Element> getElements();

	default void update(RenderContext context) { }

	default void setAnimation(ElementAnim anim) {
		for (Element el : getElements()) {
			el.anim = anim;
		}
	}

	default void destroy(GL2ES2 gl) { }
}
