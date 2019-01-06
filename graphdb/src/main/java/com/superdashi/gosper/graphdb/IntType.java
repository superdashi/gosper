package com.superdashi.gosper.graphdb;

import java.nio.ByteBuffer;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

public class IntType implements DataType {

	static final IntType instance = new IntType();

	private IntType() { }

	@Override
	public int compare(Object a, Object b) {
		int an = (int) a;
		int bn = (int) b;
		return Integer.compare(an, bn);
	}

	@Override
	public int getMemory(Object obj) {
		return 4;
	}

	@Override
	public void write(WriteBuffer buff, Object obj) {
		int n = (int) obj;
//System.out.format("WRITING %d%n", n);
		buff.putInt(n);
	}

	@Override
	public void write(WriteBuffer buff, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			int n = (int) objs[i];
//System.out.format("WRITING[%d] %d%n", i, n);
			buff.putInt(n);
		}
	}

	@Override
	public Object read(ByteBuffer buff) {
		int n = buff.getInt();
//System.out.format("READING %d%n", n);
		return n;
	}

	@Override
	public void read(ByteBuffer buff, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			int n = buff.getInt();
//System.out.format("READING[%d] %d%n", i, n);
			objs[i] = n;
		}
	}

}
