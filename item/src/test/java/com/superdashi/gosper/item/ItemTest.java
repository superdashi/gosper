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

}
