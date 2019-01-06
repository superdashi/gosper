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
