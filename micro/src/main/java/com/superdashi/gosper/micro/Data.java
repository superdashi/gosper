package com.superdashi.gosper.micro;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import com.tomgibara.streams.EndOfStreamException;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.StreamDeserializer;
import com.tomgibara.streams.StreamSerializer;
import com.tomgibara.streams.Streams;
import com.tomgibara.tries.IndexedTries;
import com.tomgibara.tries.Tries;

abstract class Data {

	// prevent really big arrays
	public static final int MAX_ARRAY_LENGTH = 65536;

	static final IndexedTries<String> tries = Tries.serialStrings(StandardCharsets.UTF_8).nodeSource(Tries.sourceForCompactLookups()).indexed();

	static final int TYPE_BYTE    =   1;
	static final int TYPE_SHORT   =   2;
	static final int TYPE_INT     =   3;
	static final int TYPE_LONG    =   4;
	static final int TYPE_BOOLEAN =   5;
	static final int TYPE_CHAR    =   6;
	static final int TYPE_FLOAT   =   7;
	static final int TYPE_DOUBLE  =   8;

	static final int TYPE_STRING  =   9;
	static final int TYPE_ITEM    =  10;
	static final int TYPE_ACTION  =  11;
	static final int TYPE_CUSTOM  = 127;

	public final class Custom implements Serializable {

		private static final long serialVersionUID = 1L;

		final byte[] bytes;

		Custom(byte[] bytes) {
			this.bytes = bytes;
		}

		<V> Custom(V value, StreamSerializer<V> serializer) {
			StreamBytes bytes = Streams.bytes(0, MAX_ARRAY_LENGTH);
			try {
				serializer.serialize(value, bytes.writeStream());
			} catch (EndOfStreamException e) {
				throw new IllegalArgumentException("serialization too large");
			}
			this.bytes = bytes.bytes();
		}

		public <V> V deserialize(StreamDeserializer<V> deserializer) {
			return Streams.bytes(bytes).readStream().readWith(deserializer).produce();
		}
	}

}
