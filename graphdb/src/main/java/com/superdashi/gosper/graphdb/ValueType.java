package com.superdashi.gosper.graphdb;

import java.nio.ByteBuffer;
import java.util.EnumMap;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.item.Value.Type;

final class ValueType implements DataType {

	private static final EnumMap<Value.Type, ValueType> dataTypes = new EnumMap<>(Value.Type.class);

	static {
		for (Value.Type type : Value.Type.values()) {
			dataTypes.put(type, new ValueType(type));
		}
	}

	static ValueType instanceFor(Value.Type type) {
		assert type != null;
		return dataTypes.get(type);
	}

	private final Type type;

	private ValueType(Type type) {
		this.type = type;
	}

	@Override
	public int compare(Object a, Object b) {
		return type.compareAscending.compare((Value) a, (Value) b);
	}

	@Override
	public int getMemory(Object obj) {
		return GraphUtil.valueSize((Value) obj);
	}

	@Override
	public void write(WriteBuffer b, Object obj) {
		Value value = (Value) obj;
		assert value.type() == type;
		GraphUtil.writeKnownTypeValue(b, (Value) obj);
	}

	@Override
	public void write(WriteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			write(b, objs[i]);
		}
	}

	@Override
	public Value read(ByteBuffer b) {
		return GraphUtil.readKnownTypeValue(b, type);
	}

	@Override
	public void read(ByteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			objs[i] = read(b);
		}
	}

}
