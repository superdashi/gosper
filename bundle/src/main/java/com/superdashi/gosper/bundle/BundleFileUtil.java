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
package com.superdashi.gosper.bundle;

import static com.tomgibara.streams.Streams.streamInput;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.superdashi.gosper.util.ReaderInputStream;
import com.tomgibara.fundament.Producer;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;

public final class BundleFileUtil {

	//TODO should limit valid characters?
	static void checkValidName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		if (name.isEmpty()) throw new IllegalArgumentException("empty name");
		if (name.charAt(0) == '/') throw new IllegalArgumentException("absolute name");
		if (name.charAt(name.length() - 1) == '/') throw new IllegalArgumentException("invalid name");
	}


	public static BundleFile newEmptyFile(String name) {
		assert name != null;
		return new BundleFile() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public ReadStream openAsReadStream() throws IOException {
				return Streams.streamFromEmpty();
			}
		};
	}

	public static BundleFile newSystemFile(String name, Path path) {
		assert name != null;
		assert path != null;
		return new BundleFile() {
			@Override
			public String name() {
				return name;
			}
			@Override
			public ReadStream openAsReadStream() throws IOException {
				return streamInput( Files.newInputStream(path) );
			}
		};
	}

	public static BundleFile newResourceFile(String name, ClassLoader classLoader, String resourcePath) {
		assert classLoader != null;
		assert resourcePath != null;
		return new BundleFile() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public ReadStream openAsReadStream() throws IOException {
				InputStream stream = classLoader.getResourceAsStream(resourcePath);
				if (stream == null) throw new IOException("no such resource: " + resourcePath);
				return streamInput(stream);
			}
		};
	}

	public static BundleFile newBytesFile(String name, byte[] bytes) {
		assert name != null;
		assert bytes != null;
		return new BundleFile() {

			@Override
			public String name() {
				return name;
			}

			@Override
			public ReadStream openAsReadStream() throws IOException {
				return Streams.bytes(bytes).readStream();
			}
		};
	}

	public static BundleFile newTextFile(String name, String text, Charset charset) {
		assert name != null;
		assert text != null;
		assert charset != null;
		return new BundleFile() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public ReadStream openAsReadStream() throws IOException {
				return Streams.streamInput(new ReaderInputStream(new StringReader(text), charset));
			}

			@Override
			public Reader openAsReader() throws IOException {
				return new StringReader(text);
			}

			// TODO implement an efficient iterator
		};
	}

	public static BundleFile newTextFile(String name, String text) {
		return newTextFile(name, text, StandardCharsets.UTF_8);
	}

	//TODO could create a more efficient implementation
	public static BundleFile newLinesFile(String name, String... lines) {
		return newTextFile(name, String.join("\n", lines));
	}

	public static BundleFile newReadStream(String name, Producer<ReadStream> streamer) {
		assert name != null;
		assert streamer != null;
		return new BundleFile() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public ReadStream openAsReadStream() throws IOException {
				return streamer.produce();
			}
		};
	}

	private BundleFileUtil() {}
}
