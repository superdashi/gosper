package com.superdashi.gosper.graphdb;

import java.nio.ByteBuffer;
import java.time.Instant;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;

import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Priority;
import com.superdashi.gosper.item.Value;

class GraphUtil {

	private static final boolean USE_MARKERS = false;
	private static final int MARKER = 0xdeadbeef;

	static final boolean _32_bit = true; //TODO
	static final int referenceSize = _32_bit ? 4 : 8;
	static final int objectHeaderSize = 8;

	static void writeMarker(WriteBuffer b) {
		if (USE_MARKERS) b.putInt(MARKER);
	}

	static void checkMarker(ByteBuffer b) {
		if (USE_MARKERS && b.getInt() != MARKER) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "expected marker");
	}

	static int arraySize(int[] ints) {
		return objectHeaderSize + 4 + 4 * ints.length;
	}

	static int arraySize(long[] longs) {
		return objectHeaderSize + 4 + 8 * longs.length;
	}

	static int arraySize(String[] strs) {
		if (strs == null) return 0;
		int size = objectHeaderSize + 4 + strs.length * referenceSize;
		for (String str : strs) {
			if (str != null) {
				size += objectHeaderSize + 4 + str.length() * 2;
			}
		}
		return size;
	}

	static int arraySize(Value[] values) {
		if (values == null) return 0;
		int size = objectHeaderSize + 4 + values.length * referenceSize;
		for (Value value : values) {
			if (value != null) {
				size += valueSize(value);
			}
		}
		return size;
	}

	static int valueSize(Value value) {
		switch (value.type()) {
		case EMPTY:    return 0;
		case IMAGE:    return 3 * objectHeaderSize + 3 + referenceSize * 2 + value.image().uri().toString().length() * 2;
		case INSTANT:  return 2 * objectHeaderSize + 8 + 4;
		case INTEGER:  return objectHeaderSize + 8;
		case NUMBER:   return objectHeaderSize + 8;
		case PRIORITY: return 0;
		case STRING:   return objectHeaderSize + 4 + value.string().length() * 2;
		default: return 0; // shouldn't happen
		}
	}

	static Value readValue(ByteBuffer b) {
		int ordinal = b.get() & 0xff;
		return readKnownTypeValue(b, Value.Type.valueOf(ordinal));
	}

	static Value readKnownTypeValue(ByteBuffer b, Value.Type type) {
		switch (type) {
		case EMPTY: return Value.empty();
		case IMAGE: {
			int len = b.getInt();
			String str = DataUtils.readString(b, len);
			return Value.ofImage(new Image(str));
		}
		case INSTANT: return Value.ofInstant(Instant.ofEpochMilli(b.getLong()));
		case INTEGER: return Value.ofInteger(b.getLong());
		case NUMBER: return Value.ofNumber(b.getDouble());
		case PRIORITY: return Value.ofPriority(Priority.valueOf(b.get() & 0xff));
		case STRING: {
			int len = b.getInt();
			String str = DataUtils.readString(b, len);
			return Value.ofString(str);
		}
		default: throw new IllegalArgumentException();
		}
	}

	static void writeValue(WriteBuffer b, Value value) {
		b.put((byte) value.type().ordinal());
		writeKnownTypeValue(b, value);
	}

	static void writeKnownTypeValue(WriteBuffer b, Value value) {
		switch (value.type()) {
		case EMPTY:
			return;
		case IMAGE:
			String uri = value.image().uri().toString();
			b.putInt(uri.length());
			b.putStringData(uri, uri.length());
			return;
		case INSTANT:
			b.putLong( value.instant().toEpochMilli() );
			return;
		case INTEGER:
			b.putLong( value.integer() );
			return;
		case NUMBER:
			b.putDouble( value.number() );
			return;
		case PRIORITY:
			b.put( (byte) value.priority().ordinal() );
			return;
		case STRING:
			String str = value.string();
			b.putInt(str.length());
			b.putStringData(str, str.length());
			return;
		default:
		}
	}

}