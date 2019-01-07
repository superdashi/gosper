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

final class LongType implements DataType {

	static final LongType instance = new LongType();

	private LongType() { }

	@Override
	public int compare(Object a, Object b) {
		long an = (long) a;
		long bn = (long) b;
		return Long.compare(an, bn);
	}

	@Override
	public int getMemory(Object obj) {
		return 8;
	}

	@Override
	public void write(WriteBuffer buff, Object obj) {
		buff.putLong((long) obj);
	}

	@Override
	public void write(WriteBuffer buff, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			buff.putLong((long) objs[i]);
		}
	}

	@Override
	public Object read(ByteBuffer buff) {
		return buff.getLong();
	}

	@Override
	public void read(ByteBuffer buff, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			objs[i] = buff.getLong();
		}
	}

}
