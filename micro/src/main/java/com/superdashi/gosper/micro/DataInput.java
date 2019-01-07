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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import com.superdashi.gosper.item.Item;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamDeserializer;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;
import com.tomgibara.tries.IndexedTrie;

public final class DataInput extends Data {

	private static final IndexedTrie<String> emptyTrie = tries.newTrie();
	private static final int[] emptyRecords = new int[0];
	private static final byte[] emptyBytes = new byte[0];

	static final DataInput empty = new DataInput();
	static final DataInput NULL = new DataInput();

	public static DataInput deserialize(ReadStream r) {
		IndexedTrie<String> trie = tries.readTrie(r);
		int size = trie.size();
		if (size == 0) return empty; // nothing else recorded for empty data input
		int[] records = new int[size];
		for (int i = 0; i < records.length; i++) {
			records[i] = r.readInt();
		}
		int length = r.readInt();
		byte[] bytes = new byte[length];
		r.readBytes(bytes);
		return new DataInput(trie, records, bytes);
	}

	private final IndexedTrie<String> trie;
	private final int[] records;
	private final byte[] bytes;

	// constructors

	DataInput() {
		this.trie = emptyTrie;
		this.records = emptyRecords;
		this.bytes = emptyBytes;
	}

	DataInput(IndexedTrie<String> trie, int[] records, byte[] bytes) {
		this.trie = trie.immutable();
		this.records = records;
		this.bytes = bytes;
	}

	// accessors

	public boolean isEmpty() {
		return records.length == 0;
	}

	public Serializable get(String key) {
		if (key == null) throw new IllegalArgumentException("null key");
		return readGeneric(key, 0);
	}

	public <V> V get(String key, StreamDeserializer<V> deserializer) {
		if (key == null) throw new IllegalArgumentException("null key");
		if (deserializer == null) throw new IllegalArgumentException("null deserializer");
		Serializable value = readGeneric(key, 0);
		if (value instanceof Custom) return ((Custom) value).deserialize(deserializer);
		throw new IllegalArgumentException("not a custom type");
	}

	public Optional<Serializable> optionalGet(String key) {
		if (key == null) throw new IllegalArgumentException("null key");
		return Optional.ofNullable(readGenericOrNull(key, 0));
	}

	public <V> Optional<V> optionalGet(String key, StreamDeserializer<V> deserializer) {
		if (key == null) throw new IllegalArgumentException("null key");
		if (deserializer == null) throw new IllegalArgumentException("null deserializer");
		Serializable value = readGenericOrNull(key, 0);
		if (value == null) return Optional.empty();
		if (value instanceof Custom) return Optional.of(((Custom) value).deserialize(deserializer));
		throw new IllegalArgumentException("not a custom type");
	}

	public Set<String> keys() {
		return trie.asSet();
	}

	public boolean hasKey(String key) {
		return trie.contains(key);
	}

	public byte getByte(String key) {
		return read(key, TYPE_BYTE * 2).readByte();
	}

	public byte getByte(String key, byte defaultValue) {
		ReadStream r = readOrNull(key, TYPE_BYTE * 2);
		return r == null ? defaultValue : r.readByte();
	}

	public byte[] getBytes(String key) {
		return (byte[]) readGeneric(key, TYPE_BYTE * 2 + 1);
	}

	public byte[] getBytes(String key, byte... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_BYTE * 2 + 1);
		return value == null ? defaultValue : (byte[]) value;
	}

	public short getShort(String key) {
		return read(key, TYPE_SHORT * 2).readShort();
	}

	public short getShort(String key, short defaultValue) {
		ReadStream r = readOrNull(key, TYPE_SHORT * 2);
		return r == null ? defaultValue : r.readShort();
	}

	public short[] getShorts(String key) {
		return (short[]) readGeneric(key, TYPE_SHORT * 2 + 1);
	}

	public short[] getShorts(String key, short... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_SHORT * 2 + 1);
		return value == null ? defaultValue : (short[]) value;
	}

	public int getInt(String key) {
		return read(key, TYPE_INT * 2).readInt();
	}

	public int getInt(String key, int defaultValue) {
		ReadStream r = readOrNull(key, TYPE_INT * 2);
		return r == null ? defaultValue : r.readInt();
	}

	public int[] getInts(String key) {
		return (int[]) readGeneric(key, TYPE_INT * 2 + 1);
	}

	public int[] getInts(String key, int... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_INT * 2 + 1);
		return value == null ? defaultValue : (int[]) value;
	}

	public long getLong(String key) {
		return read(key, TYPE_LONG * 2).readLong();
	}

	public long getLong(String key, long defaultValue) {
		ReadStream r = readOrNull(key, TYPE_LONG * 2);
		return r == null ? defaultValue : r.readLong();
	}

	public long[] getLongs(String key) {
		return (long[]) readGeneric(key, TYPE_LONG * 2 + 1);
	}

	public long[] getLongs(String key, long... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_LONG * 2 + 1);
		return value == null ? defaultValue : (long[]) value;
	}

	public boolean getBoolean(String key) {
		return read(key, TYPE_BOOLEAN * 2).readBoolean();
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		ReadStream r = readOrNull(key, TYPE_BOOLEAN * 2);
		return r == null ? defaultValue : r.readBoolean();
	}

	public boolean[] getBooleans(String key) {
		return (boolean[]) readGeneric(key, TYPE_BOOLEAN * 2 + 1);
	}

	public boolean[] getBooleans(String key, boolean... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_BOOLEAN * 2 + 1);
		return value == null ? defaultValue : (boolean[]) value;
	}

	public char getChar(String key) {
		return read(key, TYPE_CHAR * 2).readChar();
	}

	public char getChar(String key, char defaultValue) {
		ReadStream r = readOrNull(key, TYPE_CHAR * 2);
		return r == null ? defaultValue : r.readChar();
	}

	public char[] getChars(String key) {
		return (char[]) readGeneric(key, TYPE_CHAR * 2 + 1);
	}

	public char[] getChars(String key, char... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_CHAR * 2 + 1);
		return value == null ? defaultValue : (char[]) value;
	}

	public float getFloat(String key) {
		return read(key, TYPE_FLOAT * 2).readFloat();
	}

	public float getFloat(String key, float defaultValue) {
		ReadStream r = readOrNull(key, TYPE_FLOAT * 2);
		return r == null ? defaultValue : r.readFloat();
	}

	public float[] getFloats(String key) {
		return (float[]) readGeneric(key, TYPE_FLOAT * 2 + 1);
	}

	public float[] getFloats(String key, float... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_FLOAT * 2 + 1);
		return value == null ? defaultValue : (float[]) value;
	}

	public double getDouble(String key) {
		return read(key, TYPE_DOUBLE * 2).readDouble();
	}

	public double getDouble(String key, double defaultValue) {
		ReadStream r = readOrNull(key, TYPE_DOUBLE * 2);
		return r == null ? defaultValue : r.readDouble();
	}

	public double[] getDoubles(String key) {
		return (double[]) readGeneric(key, TYPE_DOUBLE * 2 + 1);
	}

	public double[] getDoubles(String key, double... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_DOUBLE * 2 + 1);
		return value == null ? defaultValue : (double[]) value;
	}

	public String getString(String key) {
		return read(key, TYPE_STRING * 2).readChars();
	}

	public String getString(String key, String defaultValue) {
		ReadStream r = readOrNull(key, TYPE_STRING * 2);
		return r == null ? defaultValue : r.readChars();
	}

	public String[] getStrings(String key) {
		return (String[]) readGeneric(key, TYPE_STRING * 2 + 1);
	}

	public String[] getStrings(String key, String... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_STRING * 2 + 1);
		return value == null ? defaultValue : (String[]) value;
	}

	public Item getItem(String key) {
		return read(key, TYPE_ITEM * 2).readWith(Item::deserialize).produce();
	}

	public Item getItem(String key, Item defaultValue) {
		ReadStream r = readOrNull(key, TYPE_ITEM * 2);
		return r == null ? defaultValue : r.readWith(Item::deserialize).produce();
	}

	public Item[] getItems(String key) {
		return (Item[]) readGeneric(key, TYPE_ITEM * 2 + 1);
	}

	public Item[] getItems(String key, Item... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_ITEM * 2 + 1);
		return value == null ? defaultValue : (Item[]) value;
	}

	public Action getAction(String key) {
		return read(key, TYPE_ACTION * 2).readWith(Action::deserialize).produce();
	}

	public Action getAction(String key, Action defaultValue) {
		ReadStream r = readOrNull(key, TYPE_ACTION * 2);
		return r == null ? defaultValue : r.readWith(Action::deserialize).produce();
	}

	public Action[] getActions(String key) {
		return (Action[]) readGeneric(key, TYPE_ACTION * 2 + 1);
	}

	public Action[] getActions(String key, Action... defaultValue) {
		Serializable value = readGenericOrNull(key, TYPE_ACTION * 2 + 1);
		return value == null ? defaultValue : (Action[]) value;
	}

	public <V> V getCustom(String key, StreamDeserializer<V> deserializer) {
		if (deserializer == null) throw new IllegalArgumentException("null deserializer");
		return read(key, TYPE_CUSTOM * 2).readWith(deserializer).produce();
	}

	public <V> V getCustom(String key, StreamDeserializer<V> deserializer, V defaultValue) {
		if (deserializer == null) throw new IllegalArgumentException("null deserializer");
		ReadStream r = readOrNull(key, TYPE_CUSTOM * 2);
		return r == null ? defaultValue : r.readWith(deserializer).produce();
	}

	// methods

	public void serialize(WriteStream w) {
		if (w == null) throw new IllegalArgumentException("null w");
		trie.writeTo(w);
		if (trie.isEmpty()) return; // don't record anything else
		for (int record : records) {
			w.writeInt(record);
		}
		w.writeInt(bytes.length);
		w.writeBytes(bytes);
	}

	// object methods

	@Override
	public int hashCode() {
		return trie.hashCode() + Arrays.hashCode(records) + Arrays.hashCode(bytes);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof DataInput)) return false;
		DataInput that = (DataInput) obj;
		return
				this.trie.asSet().equals(that.trie.asSet()) &&
				Arrays.equals(this.records, that.records) &&
				Arrays.equals(this.bytes, that.bytes);
	}

	@Override
	public String toString() {
		return toOutput().toString();
	}

	// package scoped methods

	DataOutput toOutput() {
		return new DataOutput(this);
	}

	// private helper methods

	private ReadStream read(String key, int type) {
		if (key == null) throw new IllegalArgumentException("null key");
		int index = trie.indexOf(key);
		if (index < 0) throw new IllegalArgumentException("key not present");
		int record = records[index];
		if ((record & 0xff) != type) throw new IllegalArgumentException("value type mismatch");
		int from = record >> 8;
		int to = (index == records.length - 1) ? bytes.length : records[index + 1] >> 8;
		//TODO this is very heavyweight
		return Streams.streamBuffer(ByteBuffer.wrap(bytes, from, to - from)).readStream();
	}

	private ReadStream readOrNull(String key, int type) {
		if (key == null) throw new IllegalArgumentException("null key");
		int index = trie.indexOf(key);
		if (index < 0) return null;
		int record = records[index];
		if ((record & 0xff) != type) return null;
		int from = record >> 8;
		int to = (index == records.length - 1) ? bytes.length : records[index + 1] >> 8;
		//TODO this is very heavyweight
		return Streams.streamBuffer(ByteBuffer.wrap(bytes, from, to - from)).readStream();
	}

	private Serializable readGeneric(String key, int requiredType) {
		int index = trie.indexOf(key);
		if (index < 0) throw new IllegalArgumentException("key not present");
		int record = records[index];
		int type = record & 0xff;
		if (requiredType != 0 && requiredType != type) throw new IllegalArgumentException("value type mismatch");
		int from = record >> 8;
		int to = (index == records.length - 1) ? bytes.length : records[index + 1] >> 8;
		int length = to - from;
		ReadStream r = Streams.streamBuffer(ByteBuffer.wrap(bytes, from, length)).readStream();
		return read(r, type, from, to);
	}

	private Serializable readGenericOrNull(String key, int requiredType) {
		int index = trie.indexOf(key);
		if (index < 0) return null;
		int record = records[index];
		int type = record & 0xff;
		if (requiredType != 0 && requiredType != type) return null;
		int from = record >> 8;
		int to = (index == records.length - 1) ? bytes.length : records[index + 1] >> 8;
		int length = to - from;
		ReadStream r = Streams.streamBuffer(ByteBuffer.wrap(bytes, from, length)).readStream();
		return read(r, type, from, to);
	}

	//TODO this is very heavyweight
	private Serializable read(ReadStream r, int type, int from, int to) {
		int length = to - from;
		switch (type) {

		case TYPE_BYTE    * 2     : return r.readByte();
		case TYPE_SHORT   * 2     : return r.readShort();
		case TYPE_INT     * 2     : return r.readInt();
		case TYPE_LONG    * 2     : return r.readLong();
		case TYPE_BOOLEAN * 2     : return r.readBoolean();
		case TYPE_CHAR    * 2     : return r.readChar();
		case TYPE_FLOAT   * 2     : return r.readFloat();
		case TYPE_DOUBLE  * 2     : return r.readDouble();
		case TYPE_STRING  * 2     : return r.readChars();
		case TYPE_ITEM    * 2     : return Item.deserialize(r);
		case TYPE_ACTION  * 2     : return Action.deserialize(r);
		case TYPE_CUSTOM  * 2     : return new Custom(Arrays.copyOfRange(bytes, from, to));

		case TYPE_BYTE    * 2 + 1 : return Arrays.copyOfRange(bytes, from, to);
		case TYPE_SHORT   * 2 + 1 : {
			short[] v = new short[length/2];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readShort();
			}
			return v;
		}
		case TYPE_INT     * 2 + 1 : {
			int[] v = new int[length/4];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readInt();
			}
			return v;
		}
		case TYPE_LONG    * 2 + 1 : {
			long[] v = new long[length/8];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readLong();
			}
			return v;
		}
		case TYPE_BOOLEAN * 2 + 1 : {
			boolean[] v = new boolean[length];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readBoolean();
			}
			return v;
		}
		case TYPE_CHAR    * 2 + 1 : {
			char[] v = new char[length/2];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readChar();
			}
			return v;
		}
		case TYPE_FLOAT   * 2 + 1 : {
			float[] v = new float[length/4];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readFloat();
			}
			return v;
		}
		case TYPE_DOUBLE  * 2 + 1 : {
			double[] v = new double[length/8];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readDouble();
			}
			return v;
		}
		case TYPE_STRING  * 2 + 1 : {
			String[] v = new String[r.readInt()];
			for (int i = 0; i < v.length; i++) {
				v[i] = r.readChars();
			}
			return v;
		}
		case TYPE_ITEM    * 2 + 1 : {
			Item[] v = new Item[r.readInt()];
			for (int i = 0; i < v.length; i++) {
				v[i] = Item.deserialize(r);
			}
			return v;
		}
		case TYPE_ACTION  * 2 + 1 : {
			Action[] v = new Action[r.readInt()];
			for (int i = 0; i < v.length; i++) {
				v[i] = Action.deserialize(r);
			}
			return v;
		}
		default: throw new IllegalStateException("unexpected type");
		}

	}
}
