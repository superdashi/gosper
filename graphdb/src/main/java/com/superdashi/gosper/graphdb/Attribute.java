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
package com.superdashi.gosper.graphdb;

import java.util.Comparator;

import com.superdashi.gosper.item.Value;

final public class Attribute {

	static final Comparator<Attribute> comparator = Comparator.comparing(a -> a.name);

	//TODO consider keeping name and namespace separate
	public final AttrName name;
	public final Value.Type type;
	public final Value value;
	public final boolean indexed;

	Attribute(AttrName name, Value.Type type, Value value, boolean indexed) {
		this.name = name;
		this.type = type;
		this.value = value;
		this.indexed = indexed;
	}
}
