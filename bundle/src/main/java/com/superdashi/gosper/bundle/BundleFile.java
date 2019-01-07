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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.tomgibara.streams.ReadStream;

public interface BundleFile {

	String name();

	ReadStream openAsReadStream() throws IOException;

	// assumes UTF8 encoding
	default Reader openAsReader() throws IOException {
		//TODO cache Charset somewhere
		return new InputStreamReader(openAsReadStream().asInputStream(), Charset.forName("UTF-8"));
	}
	// assumes UTF-8 encoding
	//TODO rename to openAsLines?
	default Iterator<String> openAsIterator() throws IOException {
		BufferedReader reader = new BufferedReader(openAsReader());
		return new Iterator<String>() {

			private String next;

			{ advance(); }

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public String next() {
				if (next == null) throw new NoSuchElementException();
				try {
					return next;
				} finally {
					advance();
				}
			}

			private void advance() {
				try {
					next = reader.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					next = null;
				}
				if (next == null) {
					try { reader.close(); } catch (IOException ex) { /* ignored for now */ }
				}
			}
		};
	}

	default Stream<String> openAsStream() throws IOException {
		Iterator<String> it = openAsIterator();
		return StreamSupport.stream(((Iterable<String>) () -> it).spliterator(), false);
	}

}
