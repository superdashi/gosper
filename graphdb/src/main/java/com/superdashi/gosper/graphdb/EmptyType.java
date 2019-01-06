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
