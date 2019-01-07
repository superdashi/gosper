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

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import com.superdashi.gosper.item.Value;

final class EmptyType implements DataType {

	static final EmptyType instance = new EmptyType();

	private EmptyType() {}

	@Override
	public int compare(Object a, Object b) { return 0; }

	@Override
	public int getMemory(Object obj) { return 0; }

	@Override
	public void write(WriteBuffer buff, Object obj) { }

	@Override
	public void write(WriteBuffer buff, Object[] obj, int len, boolean key) { }

	@Override
	public Object read(ByteBuffer buff) { return Value.empty(); }

	@Override
	public void read(ByteBuffer buff, Object[] obj, int len, boolean key) {
		Arrays.fill(obj, 0, len, Value.empty());
	}

}
