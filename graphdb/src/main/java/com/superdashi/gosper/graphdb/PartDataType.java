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
