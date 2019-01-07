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

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

final class PartDataType implements DataType {

	static final PartDataType instance = new PartDataType();

	private PartDataType() { }

	@Override
	public int compare(Object a, Object b) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMemory(Object obj) {
		return ((PartData) obj).estByteSize();
	}

	@Override
	public void write(WriteBuffer buff, Object obj) {
		((PartData) obj).write(buff);
	}

	@Override
	public void write(WriteBuffer buff, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			((PartData) objs[i]).write(buff);
		}
	}

	@Override
	public Object read(ByteBuffer buff) {
		return new PartData(buff);
	}

	@Override
	public void read(ByteBuffer buff, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			objs[i] = new PartData(buff);
		}
	}

}
