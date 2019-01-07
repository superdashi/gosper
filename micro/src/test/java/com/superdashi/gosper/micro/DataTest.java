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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.micro.DataInput;
import com.superdashi.gosper.micro.DataOutput;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntPair;
import com.tomgibara.storage.Stores;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

public class DataTest {

	@Test
	public void testTypes() {
		DataOutput out = new DataOutput();

		out.put("byte", (byte) 1);
		out.put("short", (short) 1);
		out.put("int", 1);
		out.put("long", 1L);
		out.put("boolean", true);
		out.put("char", '1');
		out.put("float", 1.0f);
		out.put("double", 1.0);
		out.put("string", "1");
		out.put("item", Item.fromLabel("1"));
		out.put("custom", IntCoords.at(0, 1), (c,w) -> w.writeLong(IntPair.fromCoords(c)));

		out.put("bytes", new byte[1]);
		out.put("shorts", new short[1]);
		out.put("ints", new int[1]);
		out.put("longs", new long[1]);
		out.put("booleans", new boolean[1]);
		out.put("chars", new char[1]);
		out.put("floats", new float[1]);
		out.put("doubles", new double[1]);
		out.put("strings", new String[] {"1"});
		out.put("items", new Item[] {Item.nothing()});

		DataInput in = out.toInput();
		assertEquals((byte) 1, in.getByte("byte"));
		assertEquals((short) 1, in.getShort("short"));
		assertEquals(1, in.getInt("int"));
		assertEquals(1L, in.getLong("long"));
		assertEquals(true, in.getBoolean("boolean"));
		assertEquals('1', in.getChar("char"));
		assertEquals(1.0f, in.getFloat("float"), 0f);
		assertEquals(1.0, in.getDouble("double"), 0.0);
		assertEquals("1", in.getString("string"));
		assertEquals(Item.fromLabel("1"), in.getItem("item"));
		assertEquals(IntCoords.at(0, 1), in.getCustom("custom", r -> IntPair.toCoords(r.readLong())));

		assertEquals((byte) 1, in.getByte("byte", (byte) 2));
		assertEquals((short) 1, in.getShort("short", (short) 2));
		assertEquals(1, in.getInt("int", 2));
		assertEquals(1L, in.getLong("long", 2L));
		assertEquals(true, in.getBoolean("boolean", false));
		assertEquals('1', in.getChar("char", '2'));
		assertEquals(1.0f, in.getFloat("float", 2.0f), 0f);
		assertEquals(1.0, in.getDouble("double", 2.0), 0.0);
		assertEquals("1", in.getString("string", "2"));
		assertEquals(Item.fromLabel("1"), in.getItem("item", Item.fromLabel("2")));
		assertEquals(IntCoords.at(0, 1), in.getCustom("custom", r -> IntPair.toCoords(r.readLong()), IntCoords.at(0, 2)));

		assertEquals((byte) 2, in.getByte("nobyte", (byte) 2));
		assertEquals((short) 2, in.getShort("noshort", (short) 2));
		assertEquals(2, in.getInt("noint", 2));
		assertEquals(2L, in.getLong("nolong", 2L));
		assertEquals(false, in.getBoolean("noboolean", false));
		assertEquals('2', in.getChar("nochar", '2'));
		assertEquals(2.0f, in.getFloat("nofloat", 2.0f), 0f);
		assertEquals(2.0, in.getDouble("nodouble", 2.0), 0.0);
		assertEquals("2", in.getString("nostring", "2"));
		assertEquals(Item.fromLabel("2"), in.getItem("noitem", Item.fromLabel("2")));
		assertEquals(IntCoords.at(0, 2), in.getCustom("nocustom", r -> IntPair.toCoords(r.readLong()), IntCoords.at(0, 2)));

		assertEquals(Stores.bytes(new byte[1]), Stores.bytes(in.getBytes("bytes")));
		assertEquals(Stores.shorts(new short[1]), Stores.shorts(in.getShorts("shorts")));
		assertEquals(Stores.ints(new int[1]), Stores.ints(in.getInts("ints")));
		assertEquals(Stores.longs(new long[1]), Stores.longs(in.getLongs("longs")));
		assertEquals(Stores.booleans(new boolean[1]), Stores.booleans(in.getBooleans("booleans")));
		assertEquals(Stores.chars(new char[1]), Stores.chars(in.getChars("chars")));
		assertEquals(Stores.floats(new float[1]), Stores.floats(in.getFloats("floats")));
		assertEquals(Stores.doubles(new double[1]), Stores.doubles(in.getDoubles("doubles")));

		assertEquals(Stores.bytes(new byte[1]), Stores.bytes(in.getBytes("bytes", new byte[2])));
		assertEquals(Stores.shorts(new short[1]), Stores.shorts(in.getShorts("shorts", new short[2])));
		assertEquals(Stores.ints(new int[1]), Stores.ints(in.getInts("ints", new int[2])));
		assertEquals(Stores.longs(new long[1]), Stores.longs(in.getLongs("longs", new long[2])));
		assertEquals(Stores.booleans(new boolean[1]), Stores.booleans(in.getBooleans("booleans", new boolean[2])));
		assertEquals(Stores.chars(new char[1]), Stores.chars(in.getChars("chars", new char[2])));
		assertEquals(Stores.floats(new float[1]), Stores.floats(in.getFloats("floats", new float[2])));
		assertEquals(Stores.doubles(new double[1]), Stores.doubles(in.getDoubles("doubles", new double[2])));

		assertEquals(Stores.bytes(new byte[2]), Stores.bytes(in.getBytes("nobytes", new byte[2])));
		assertEquals(Stores.shorts(new short[2]), Stores.shorts(in.getShorts("noshorts", new short[2])));
		assertEquals(Stores.ints(new int[2]), Stores.ints(in.getInts("noints", new int[2])));
		assertEquals(Stores.longs(new long[2]), Stores.longs(in.getLongs("nolongs", new long[2])));
		assertEquals(Stores.booleans(new boolean[2]), Stores.booleans(in.getBooleans("nobooleans", new boolean[2])));
		assertEquals(Stores.chars(new char[2]), Stores.chars(in.getChars("nochars", new char[2])));
		assertEquals(Stores.floats(new float[2]), Stores.floats(in.getFloats("nofloats", new float[2])));
		assertEquals(Stores.doubles(new double[2]), Stores.doubles(in.getDoubles("nodoubles", new double[2])));

		assertEquals(1, in.get("int"));
		assertEquals(1, in.optionalGet("int").get());
		assertFalse(in.optionalGet("lint").isPresent());

		StreamBytes bytes = Streams.bytes();
		in.serialize(bytes.writeStream());
		assertTrue(bytes.length() > 0);
		DataInput copy = DataInput.deserialize(bytes.readStream());
		assertEquals(copy, in);

		DataOutput out2 = in.toOutput();
		DataInput in2 = out2.toInput();
		assertEquals(in, in2);
	}

}
