package com.superdashi.gosper.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.StreamException;
import com.tomgibara.streams.Streams;

public class BundleFilesTxt implements BundleFiles {

	private static final String MAGIC_WORD = "dashi-bundle";
	private static final String BINARY_IND = ";base64";
	private static final int BINARY_LEN = BINARY_IND.length();

	private final URI uri;
	private final String text;
	private final Map<String, Entry> entries;

	public BundleFilesTxt(Path path) {
		this(path.toUri());
	}

	public BundleFilesTxt(URI uri) {
		if (uri == null) throw new IllegalArgumentException("null uri");
		this.uri = uri;
		byte[] bytes;
		if (uri.getScheme().equals("file")) {
			// optimization for common case of files
			Path path = Paths.get(uri);
			try {
				bytes = Files.readAllBytes(path);
			} catch (IOException e) {
				//TODO how do we report this?
				bytes = null;
			}
		} else try (InputStream in = uri.toURL().openStream()) {
			StreamBytes b = Streams.bytes();
			Streams.streamInput(in).to(b.writeStream()).transferFully();
			bytes = b.bytes();
		} catch (IOException | StreamException e) {
			//TODO how do we report this?
			bytes = null;
		}
		//TODO doesn't report decoding errors
		text = bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
		entries = new Extractor().entries; // does the work of extracting entries
	}

	public BundleFilesTxt(String text) {
		if (text == null) throw new IllegalArgumentException("null text");
		this.text = text;
		String base64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
		String url = "data:text/plain;charset=utf-8;base64," + base64;
		uri = URI.create(url);
		entries = new Extractor().entries; // does the work of extracting entries
	}

	@Override
	public boolean available() {
		return entries != null;
	}

	@Override
	public URI uri() {
		return uri;
	}

	@Override
	public Stream<String> names() {
		checkAvailable();
		return entries.keySet().stream();
	}

	@Override
	public boolean isName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		checkAvailable();
		return entries.containsKey(name);
	}

	@Override
	public BundleFile file(String name) {
		checkAvailable();
		Entry entry = entries.get(name);
		if (entry == null) throw new IllegalArgumentException("unknown name");
		String str = text.substring(entry.start, entry.finish);
		if (entry.binary) {
			//TODO wish for wrapper around reader
			byte[] bytes = Base64.getMimeDecoder().decode(str.getBytes(StandardCharsets.US_ASCII));
			return BundleFileUtil.newBytesFile(name, bytes);
		} else {
			//TODO wish for string slices
			return BundleFileUtil.newTextFile(name, str);
		}
	}

	// private helper methods

	private void checkAvailable() {
		if (!available()) throw new IllegalStateException("not available");
	}

	private static final class Entry {
		private final String name;
		private final int start;
		private final int finish;
		private boolean binary;

		Entry(String name, int start, int finish, boolean binary) {
			this.name = name;
			this.start = start;
			this.finish = finish;
			this.binary = binary;
		}

		void close(int finish, Map<String, Entry> entries) {
			assert this.finish == -1;
			assert finish >= 0;
			assert !entries.containsKey(name);
			Entry closed = new Entry(name, start, finish, binary);
			entries.put(name, closed);
		}
	}

	private final class Extractor {

		Map<String, Entry> entries = null;

		private boolean crlf;
		private String nl;
		private int nlen;

		Extractor() {
			if (text == null || text.length() < MAGIC_WORD.length()) return; // short/absent text - cannot be bundle
			// check magic word
			int prologEnd = text.indexOf('\n'); // end of the first line
			String prolog = prologEnd == -1 ? text : text.substring(0, prologEnd);
			if (!prolog.contains(MAGIC_WORD)) return; // not a bundle - no magic word
			//TODO in future extract version information from bundle

			// check if empty
			if (prologEnd == -1) { // no other lines - empty bundle
				entries = Collections.emptyMap();
				return;
			}

			// identify if file is CR+LF or LF
			crlf = text.charAt(prologEnd - 1) == '\r';
			nl = crlf ? "\r\n" : "\n";
			nlen = nl.length();

			// extract the second line
			int start = prologEnd + 1; // initially, start of the second line
			prologEnd -= nlen; // work back from the end of the newline
			int end = lineEnd(start); // initially, end of the second line
			String line = text.substring(start, end); // the second line

			// extract the separator
			int i = line.indexOf(' ');
			Entry entry;
			String separator;
			switch (i) {
			case 0:
				// invalid separator - have to bail;
				return;
			case -1:
				// invalid definition (no content specifier)
				// for now, we have to bail
				return;
			default:
				// extract separator
				separator = nl + line.substring(0, i) + " ";
			}
			int sepLen = separator.length();

			// use separator to parse line
			entry = parseEntry(start + i + 1);
			if (entry == null) {
				// for now, a bad entry means we have to bail
				return;
			}

			// process file
			int length = text.length();
			//TODO use an equivalence map?
			Map<String, Entry> map = new HashMap<>();
			while (true) {
				end = text.indexOf(separator, entry.start);
				if (end == -1) { // previous entry was last entry, end loop
					entry.close(length, map);
					break;
				}
				entry.close(end, map); // close previous entry
				entry = parseEntry(end + sepLen);
				if (entry == null) {
					// for now, a bad entry means we have to bail
					return;
				}
			}
			entries = Collections.unmodifiableMap(map);
		}

		// start points to end of separator, which include space
		private Entry parseEntry(int start) {
			int end = lineEnd(start);
			String name = text.substring(start, end).trim();
			boolean binary = name.endsWith(BINARY_IND);
			if (binary) {
				name = name.substring(0, name.length() - BINARY_LEN);
			}
			try {
				BundleFileUtil.checkValidName(name);
			} catch (IllegalArgumentException e) {
				return null; //TODO need to report
			}

			return new Entry(name, end + nlen, -1, binary);
		}

		private int lineEnd(int from) {
			int j = crlf ? text.indexOf("\r\n", from) : text.indexOf('\n', from);
			return j == -1 ? text.length() : j;
		}


	}
}
