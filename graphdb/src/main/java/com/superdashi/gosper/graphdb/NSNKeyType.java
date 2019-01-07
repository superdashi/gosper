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

final class NSNKeyType implements DataType {

	static final NSNKeyType instance = new NSNKeyType();

	private NSNKeyType() { }

	@Override
	public int compare(Object a, Object b) {
		NSNKey ka = (NSNKey) a;
		NSNKey kb = (NSNKey) b;
		int c;
		c = Integer.compare(ka.nsCode, kb.nsCode);
		if (c != 0) return c;
		c = Integer.compare(ka.nmCode, kb.nmCode);
		if (c != 0) return c;
		c = Integer.compare(ka.sourceId, kb.sourceId);
		if (c != 0) return c;
		c = Integer.compare(ka.edgeId, kb.edgeId);
		return c;
	}

	@Override
	public int getMemory(Object obj) {
		return GraphUtil.objectHeaderSize + 16;
	}

	@Override
	public void write(WriteBuffer b, Object obj) {
		NSNKey key = (NSNKey) obj;
		b.putInt(key.nsCode);
		b.putInt(key.nmCode);
		if (key.edgeId < 0) {
			b.putInt(-key.sourceId - 1);
		} else {
			b.putInt(key.sourceId);
			b.putInt(key.edgeId);
		}
	}

	@Override
	public void write(WriteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			write(b, objs[i]);
		}
	}

	@Override
	public Object read(ByteBuffer b) {
		int nsc = b.getInt();
		int nmc = b.getInt();
		int nodeId = b.getInt();
		return nodeId < 0 ?
				new NSNKey(nsc, nmc, -nodeId - 1) :
				new NSNKey(nsc, nmc, nodeId, b.getInt());
	}

	@Override
	public void read(ByteBuffer b, Object[] objs, int len, boolean key) {
		for (int i = 0; i < len; i++) {
			objs[i] = read(b);
		}
	}

}
