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
package com.superdashi.gosper.item;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Value;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

public class ItemTest {

	@Test
	public void testSerial() throws Exception {
		testSerial(Item.nothing());
		testSerial(Item.fromLabel("some label"));
		testSerial(Item.fromPicture(new Image("http:www.example.com")));
		testSerial(Item.newBuilder().addExtra("some:field", Value.ofInstant(Instant.now())).build());
	}

	private void testSerial(Item orig) throws Exception {
		testSerialize(orig);
		testSerialization(orig);
	}

	private void testSerialize(Item orig) {
		StreamBytes bytes = Streams.bytes();
		orig.serialize(bytes.writeStream());
		Item copy = Item.deserialize(bytes.readStream());
		assertEquals(orig, copy);
	}

	private void testSerialization(Item orig) throws IOException, ClassNotFoundException {
		StreamBytes bytes = Streams.bytes();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes.writeStream().asOutputStream())) {
			out.writeObject(orig);
		}
		Item copy;
		try (ObjectInputStream in = new ObjectInputStream(bytes.readStream().asInputStream())) {
			copy = (Item) in.readObject();
		}
		assertEquals(orig, copy);
	}

	@Test
	public void testQualifier() {
		Assert.assertTrue( Item.nothing().qualifier().isUniversal() );
		Assert.assertFalse( Item.newBuilder().qualifyWithLang("en").build().qualifier().isUniversal() );
	}

	@Test
	public void testLabelInterpolate() {
		testLabelInterpolate("Hello {ex:noun}!", "ex:noun", "World", "Hello World!");
		testLabelInterpolate("{ex:noun}", "ex:noun", "Solo", "Solo");
		testLabelInterpolate("Suf{ex:noun}", "ex:noun", "fix", "Suffix");
		testLabelInterpolate("{ex:noun}fix", "ex:noun", "Pre", "Prefix");
		testLabelInterpolate("{ex:noun}", "ex:noun", "", "");
		testLabelInterpolate("A{ex:noun}", "ex:noun", "", "A");
		testLabelInterpolate("{ex:noun}B", "ex:noun", "", "B");
		testLabelInterpolate("A{ex:noun}B", "ex:noun", "", "AB");
		testLabelInterpolate("A{ex:missing}B", "ex:noun", "", "AB");
		testLabelInterpolate("{ex:noun}A{ex:noun}B{ex:noun}", "ex:noun", "", "AB");
		testLabelInterpolate("{ex:noun}A{ex:noun}B{ex:noun}", "ex:noun", "X", "XAXBX");
		testLabelInterpolate("{aa", "ex:noun", "", "{aa");
		testLabelInterpolate("a{a", "ex:noun", "", "a{a");
		testLabelInterpolate("aa{", "ex:noun", "", "aa{");

		Assert.assertEquals("X{}Y", Item.newBuilder().label("X{}Y").interpolate().build().label().get());
		Assert.assertEquals("XY", Item.newBuilder().label("X{gone}Y").interpolate().build().label().get());
		Assert.assertEquals("XY", Item.newBuilder().label("X{gone}Y").interpolate().build().label().get());
	}

	private void testLabelInterpolate(String label, String property, String value, String expected) {
		Assert.assertEquals(expected, Item.newBuilder().label(label).addExtra(property, Value.ofString(value)).interpolate().build().label().get());
	}
}
