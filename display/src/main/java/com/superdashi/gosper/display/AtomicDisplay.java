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
import java.util.Collections;
import java.util.Set;

final class AtomicDisplay implements ElementDisplay {

	private final Set<Element> els;

	AtomicDisplay(Element el) {
		if (el == null) throw new IllegalArgumentException("null el");
		els = Collections.singleton(el);
	}

	@Override
	public Collection<Element> getElements() {
		return els;
	}

}
