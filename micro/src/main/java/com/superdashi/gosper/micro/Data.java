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
