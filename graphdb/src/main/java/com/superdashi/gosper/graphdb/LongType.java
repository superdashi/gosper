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
