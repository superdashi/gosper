package com.superdashi.gosper.graphdb;

import java.nio.ByteBuffer;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

abstract class EdgeKeyType implements DataType {

	@Override
	public int getMemory(Object obj) {
		return GraphUtil.objectHeaderSize + 12;
	}

	@Override
	public void write(WriteBuffer b, Object obj) {
		EdgeKey key = (EdgeKey) obj;
		b.putInt(key.edgeId);
		b.putInt(key.sourceId);
		b.putInt(key.targetId);
	}

	@Override
	public void write(WriteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			write(b, objs[i]);
		}
	}

	@Override
	public EdgeKey read(ByteBuffer b) {
		return new EdgeKey(b.getInt(), b.getInt(), b.getInt());
	}

	@Override
	public void read(ByteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			objs[i] = read(b);
		}
	}

}
