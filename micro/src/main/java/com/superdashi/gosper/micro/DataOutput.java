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
package com.superdashi.gosper.micro;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import com.superdashi.gosper.item.Item;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.StreamSerializer;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;
import com.tomgibara.tries.IndexedTrie;

// note: there is no notion of equality for outputs beyond reference equality
public final class DataOutput extends Data {

	// statics

	private static int typeOfClass(Class<?> clss) {
		switch (clss.getName()) {
		case "byte" :
		case "java.lang.Byte" : return TYPE_BYTE;
		case "short" :
		case "java.lang.Short" : return TYPE_SHORT;
		case "int" :
		case "java.lang.Integer" : return TYPE_INT;
		case "long" :
		case "java.lang.Long" : return TYPE_LONG;
		case "boolean" :
		case "java.lang.Boolean" : return TYPE_BOOLEAN;
		case "char" :
		case "java.lang.Character" : return TYPE_CHAR;
		case "float" :
		case "java.lang.Float" : return TYPE_FLOAT;
		case "double" :
		case "java.lang.Double" : return TYPE_DOUBLE;
		case "java.lang.String" : return TYPE_STRING;
		case "com.superdashi.gosper.item.Item" : return TYPE_ITEM;
		case "com.superdashi.gosper.micro.Action" : return TYPE_ACTION;
		case "com.superdashi.gosper.micro.Data$Custom" : return TYPE_CUSTOM;
		default:
			return -1;
		}
	}

	// 8 bit value, lsb is array flag
	static int typeOfObject(Object obj) {
		Class<?> clss = obj.getClass();
		boolean array = clss.isArray();
		if (array) clss = clss.getComponentType();
		int type = typeOfClass(clss);
		if (type == -1) throw new IllegalArgumentException("Unsupported type: " + clss.getName());
		return (type << 1) | (array ? 1 : 0);
	}

	private static Object cloneArray(Object array) {
		int length = Array.getLength(array);
		if (length == 0) return array; // nothing else needed
		if (length > MAX_ARRAY_LENGTH) throw new IllegalArgumentException("array length exceeds maximum");
		switch (array.getClass().getComponentType().getName()) {
		case "byte"    : return ((byte   []) array).clone();
		case "short"   : return ((short  []) array).clone();
		case "int"     : return ((int    []) array).clone();
		case "long"    : return ((long   []) array).clone();
		case "boolean" : return ((boolean[]) array).clone();
		case "char"    : return ((char   []) array).clone();
		case "float"   : return ((float  []) array).clone();
		case "double"  : return ((double []) array).clone();
		default:
			Object[] objs = (Object[]) array;
			for (Object obj : objs) {
				if (obj == null) throw new IllegalArgumentException("null array value");
			}
			return objs.clone();
		}
	}

	//TODO put more restrictions here?
	private static void checkKey(String key) {
		if (key == null) throw new IllegalArgumentException("null key");
		if (key.isEmpty()) throw new IllegalArgumentException("empty key");
	}

	// fields

	private final Map<String, Object> map = new HashMap<>();

	// constructors

	public DataOutput() { }

	DataOutput(DataInput in) {
		if (!in.isEmpty()) {
			in.keys().forEach(k -> {
				map.put(k, in.get(k));
			});
		}
	}

	// public methods

	// permitted types: java primitives, Items, and arrays of these
	public DataOutput put(String key, Serializable value) {
		checkKey(key);
		if (value == null) throw new IllegalArgumentException("null value");
		int type = typeOfObject(value); // check type
		if ((type & 1) == 1) cloneArray(value); // clone array and check for zeros
		map.put(key, value);
		return this;
	}

	public <V> DataOutput put(String key, V value, StreamSerializer<V> serializer) {
		checkKey(key);
		if (value == null) throw new IllegalArgumentException("null value");
		if (serializer == null) throw new IllegalArgumentException("null serializer");
		map.put(key, new Custom(value, serializer));
		return this;
	}

	public DataOutput remove(String key) {
		checkKey(key);
		map.remove(key);
		return this;
	}

	// object methods

	@Override
	public String toString() {
		return map.toString();
	}

	// package scoped methods

	DataInput toInput() {
		if (map.isEmpty()) return DataInput.empty;

		// create a trie
		IndexedTrie<String> trie = tries.newTrie();
		trie.addAll(map.keySet());
		trie.compactStorage();

		// accumulate value data and record its position+type
		StreamBytes bytes = Streams.bytes();
		int[] records = new int[trie.size()];
		int position = 0;
		WriteStream writer = bytes.writeStream();
		int index = 0;
		for (String key : trie) {
			position = (int) writer.position();
			if (position >= (1 << 24)) throw new IllegalStateException("output too large");
			Object value = map.get(key);
			int type = typeOfObject(value);
			switch (type) {
			case TYPE_BYTE    * 2     : writer.writeByte((Byte) value); break;
			case TYPE_SHORT   * 2     : writer.writeShort((Short) value); break;
			case TYPE_INT     * 2     : writer.writeInt((Integer) value); break;
			case TYPE_LONG    * 2     : writer.writeLong((Long) value); break;
			case TYPE_BOOLEAN * 2     : writer.writeBoolean((Boolean) value); break;
			case TYPE_CHAR    * 2     : writer.writeChar((Character) value); break;
			case TYPE_FLOAT   * 2     : writer.writeFloat((Float) value); break;
			case TYPE_DOUBLE  * 2     : writer.writeDouble((Double) value); break;
			case TYPE_STRING  * 2     : writer.writeChars((String) value); break;
			case TYPE_ITEM    * 2     : ((Item)   value).serialize(writer); break;
			case TYPE_ACTION  * 2     : ((Action) value).serialize(writer); break;
			case TYPE_CUSTOM  * 2     : writer.writeBytes(((Custom) value).bytes); break;

			case TYPE_BYTE    * 2 + 1 : writer.writeBytes((byte[]) value); break;
			case TYPE_SHORT   * 2 + 1 : for (short v : (short[]) value) writer.writeShort(v); break;
			case TYPE_INT     * 2 + 1 : for (int v : (int[]) value) writer.writeInt(v); break;
			case TYPE_LONG    * 2 + 1 : for (long v : (long[]) value) writer.writeLong(v); break;
			case TYPE_BOOLEAN * 2 + 1 : for (boolean v : (boolean[]) value) writer.writeBoolean(v); break; //TODO could optimize
			case TYPE_CHAR    * 2 + 1 : for (char v : (char[]) value) writer.writeChar(v); break;
			case TYPE_FLOAT   * 2 + 1 : for (float v : (float[]) value) writer.writeFloat(v); break;
			case TYPE_DOUBLE  * 2 + 1 : for (double v : (double[]) value) writer.writeDouble(v); break;
			case TYPE_STRING  * 2 + 1 :
				writer.writeInt(((String[]) value).length);
				for (String v : (String[]) value) writer.writeChars(v);
				break;
			case TYPE_ITEM    * 2 + 1 :
				writer.writeInt(((Item[]) value).length);
				for (Item v : (Item[]) value) v.serialize(writer);
				break;
			case TYPE_ACTION  * 2 + 1 :
				writer.writeInt(((Action[]) value).length);
				for (Action v : (Action[]) value) v.serialize(writer);
				break;
			//TODO support arrays of serialized?

			default: throw new IllegalStateException("unexpected type constant");
			}
			int record = (position << 8) | type;
			records[index++] = record;
		}

		// package into data input
		return new DataInput(trie, records, bytes.bytes());
	}

}
