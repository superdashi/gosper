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

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Priority;
import com.superdashi.gosper.item.Value;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

public class ValueTest {

	@Test
	public void testSerialize() {
		testSerialize(Value.empty());
		testSerialize(Value.ofString("")); // empty
		testSerialize(Value.ofString("TEST"));
		testSerialize(Value.ofInteger(0L));
		testSerialize(Value.ofInteger(100L));
		testSerialize(Value.ofNumber(0.0));
		testSerialize(Value.ofNumber(1.0));
		testSerialize(Value.ofNumber(Double.POSITIVE_INFINITY));
		testSerialize(Value.ofNumber(Double.NEGATIVE_INFINITY));
		testSerialize(Value.ofInstant(Instant.now()));
		testSerialize(Value.ofPriority(Priority.HIGH));
		testSerialize(Value.ofImage(new Image("http://www.example.com")));
	}

	private void testSerialize(Value orig) {
		StreamBytes bytes = Streams.bytes();
		orig.serialize(bytes.writeStream());
		Value copy = Value.deserialize(bytes.readStream());
		assertEquals(orig, copy);
	}
}
