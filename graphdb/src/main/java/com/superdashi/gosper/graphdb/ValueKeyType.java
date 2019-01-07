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
import java.util.EnumMap;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import com.superdashi.gosper.item.Value;

public class ValueKeyType implements DataType {

	private static final EnumMap<Value.Type, ValueKeyType> dataTypes = new EnumMap<>(Value.Type.class);

	static {
		for (Value.Type type : Value.Type.values()) {
			dataTypes.put(type, new ValueKeyType(ValueType.instanceFor(type)));
		}
	}

	static ValueKeyType instanceFor(Value.Type type) {
		assert type != null;
		return dataTypes.get(type);
	}

	private final ValueType type;

	ValueKeyType(ValueType type) {
		this.type = type;
	}

	@Override
	public int compare(Object a, Object b) {
		ValueKey ka = (ValueKey) a;
		ValueKey kb = (ValueKey) b;
		int c = type.compare(ka.value, kb.value);
		if (c != 0) return c;
		c = Integer.compare(ka.sourceId, kb.sourceId);
		if (c != 0 || ka.edgeId < 0) return c; //TODO could assume edgeId is -1 when absent and fall through
		return Integer.compare(ka.edgeId, kb.edgeId);
	}

	@Override
	public int getMemory(Object obj) {
		return GraphUtil.objectHeaderSize + 8 + GraphUtil.valueSize(((ValueKey) obj).value);
	}

	@Override
	public void write(WriteBuffer b, Object obj) {
		ValueKey k = (ValueKey) obj;
		if (k.edgeId >= 0) b.putInt(k.edgeId);
		b.putInt(k.sourceId);
		type.write(b, k.value);
	}

	@Override
	public void write(WriteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			write(b, objs[i]);
		}
	}

	@Override
	public ValueKey read(ByteBuffer b) {
		int id = b.getInt();
		int edgeId;
		int sourceId;
		if (Space.isEdgeId(id)) {
			edgeId = id;
			sourceId = b.getInt();
		} else {
			edgeId = -1;
			sourceId = id;
		}
		return new ValueKey(type.read(b), sourceId, edgeId);
	}

	@Override
	public void read(ByteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			objs[i] = read(b);
		}
	}

}
