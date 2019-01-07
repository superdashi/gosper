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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.superdashi.gosper.item.Info;
import com.superdashi.gosper.item.Item;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.StreamCloser;
import com.tomgibara.streams.WriteStream;

//TODO reduce to just item and data?
public class ActivityData {

	// statics

	private static final int TYPE_NONE    =   0;
	private static final int TYPE_BYTE    =   1;
	private static final int TYPE_SHORT   =   2;
	private static final int TYPE_INT     =   3;
	private static final int TYPE_LONG    =   4;
	private static final int TYPE_BOOLEAN =   5;
	private static final int TYPE_CHAR    =   6;
	private static final int TYPE_FLOAT   =   7;
	private static final int TYPE_DOUBLE  =   8;

	private static final int TYPE_BYTES   =   9;
	private static final int TYPE_STRING  =  10;
	private static final int TYPE_ITEM    =  11;
	private static final int TYPE_SERIAL  = 255;

	// helper method
	public static void write(Serializable value, WriteStream w) {
		if (value == null) {
			w.writeByte((byte) TYPE_NONE);
			return;
		}
		switch (value.getClass().getName()) {
		case "java.lang.Byte":
			w.writeByte((byte) TYPE_BYTE);
			w.writeByte((byte) value);
			return;
		case "java.lang.Short":
			w.writeByte((byte) TYPE_SHORT);
			w.writeShort((short) value);
			return;
		case "java.lang.Integer":
			w.writeByte((byte) TYPE_INT);
			w.writeInt((int) value);
			return;
		case "java.lang.Long":
			w.writeByte((byte) TYPE_LONG);
			w.writeLong((long) value);
			return;
		case "java.lang.Boolean":
			w.writeByte((byte) TYPE_BOOLEAN);
			w.writeBoolean((boolean) value);
			return;
		case "java.lang.Character":
			w.writeByte((byte) TYPE_CHAR);
			w.writeChar((char) value);
			return;
		case "java.lang.Float":
			w.writeByte((byte) TYPE_FLOAT);
			w.writeFloat((float) value);
			return;
		case "java.lang.Double":
			w.writeByte((byte) TYPE_DOUBLE);
			w.writeDouble((double) value);
			return;
		case "java.lang.String":
			w.writeByte((byte) TYPE_STRING);
			w.writeChars((String) value);
			return;
		case "[B":
			w.writeByte((byte) TYPE_BYTES);
			w.writeInt(((byte[]) value).length);
			w.writeBytes((byte[]) value);
			return;
		case "com.superdashi.gosper.item.Item":
			w.writeByte((byte) TYPE_ITEM);
			((Item) value).serialize(w);
			return;
		default:
			w.writeByte((byte) TYPE_SERIAL);
			try (ObjectOutputStream out = new ObjectOutputStream(w.closedWith(StreamCloser.doNothing()).asOutputStream())) {
				out.writeObject(value);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			break;
		}
	}

	public static Serializable read(ReadStream r) {
		int type = r.readByte() & 0xff;
		switch (type) {
		case TYPE_NONE: return null;
		case TYPE_BYTE: return r.readByte();
		case TYPE_SHORT: return r.readShort();
		case TYPE_INT: return r.readInt();
		case TYPE_LONG: return r.readLong();
		case TYPE_BOOLEAN: return r.readBoolean();
		case TYPE_CHAR: return r.readChar();
		case TYPE_FLOAT: return r.readFloat();
		case TYPE_DOUBLE: return r.readDouble();
		case TYPE_STRING: return r.readChars();
		case TYPE_ITEM: return Item.deserialize(r);
		case TYPE_BYTES: {
			byte[] value = new byte[r.readInt()];
			r.readBytes(value);
			return value;
		}
		case TYPE_SERIAL:
			try (ObjectInputStream in = new ObjectInputStream(r.closedWith(StreamCloser.doNothing()).asInputStream())) {
				return (Serializable) in.readObject();
			} catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		default: throw new IllegalArgumentException("unrecognized stream value type");
		}
	}

	// fields

	boolean flag;
	long code;
	String text;
	double value;
	Info info;
	StreamBytes data;

	// used for null construction
	ActivityData() { }

	ActivityData(ActivityData that) {
		this.flag =  that.flag;
		this.code =  that.code;
		this.text =  that.text;
		this.value = that.value;
		this.info = that.info;
		this.data =  that.data;
	}

	// object methods

	@Override
	public String toString() {
		return "flag: " + flag + " code: " + code + " value: " + value + " text: " + text + " info: " + info + " data: " + (data == null ? "<none>" : data.length() + "bytes");
	}
}
