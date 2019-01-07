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
package com.superdashi.gosper.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.tomgibara.collect.EquivalenceMap;
import com.superdashi.gosper.color.Palette.LogicalColor;
import com.superdashi.gosper.util.TextCanvas;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.storage.StoreType;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

/*
 * Data persisted on disk:
 *
 * URI (utf-8 string) filesize? (long), expiry timestamp (long)
 *
 */

//TODO needs syncing over files
public final class Cache {

	private static final Maps<URI, CacheEntry> maps = Collect.setsOf(URI.class).mappedTo(CacheEntry.class);

	private static final int DEFAULT_STREAM_SIZE = 65536;
	private static final String DIGEST = "SHA1";
	private static final Hasher<String> HASH;
	static {
		try {
			HASH = Hashing.digest(DIGEST).asHash().hasher((u, s) -> s.writeChars(u));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No hash algorithm: " + DIGEST);
		}
	}

	private static final ImageAdapter imageAdapter = new ImageAdapter();
	private static final CanvasAdapter canvasAdapter = new CanvasAdapter();

	private final CachePolicy policy;
	private final EquivalenceMap<URI, CacheEntry> entries = maps.newMap();
	private boolean closed;

	Cache(CachePolicy policy) {
		this.policy = policy;
	}

	public BufferedImage cachedImage(URI uri) throws CacheException {
		return cachedItem(uri, imageAdapter);
	}

	public TextCanvas cachedTextCanvas(URI uri) throws CacheException {
		return cachedItem(uri, canvasAdapter);
	}

	private <T> T cachedItem(URI uri, Adapter<T> adapter) throws CacheException {
		final URI key = !closed && policy.isMemoryUsed() ? adapter.keyFor(uri) : null;

		// first try memory
		if (key != null) {
			CacheEntry entry;
			synchronized (entries) {
				entry = entries.get(key);
			}
			//TODO check entry validity an nullify if necessary
			if (entry != null) {
				if (entry.expires < System.currentTimeMillis()) {
					entries.remove(key);
					/* FALL THROUGH */
				} else {
					DashiLog.trace("cache hit memory: {0}", key);
					//TODO update use timestamp
					return (T) entry.item;
				}
			} else {
				DashiLog.trace("cache miss memory: {0}", key);
				/* FALL THROUGH */
			}
		}

		// then fall back to disk
		final Path path;
		if (!closed && policy.isDiskUsed()) {
			Path dir = policy.getDiskPath();
			if (dir == null) {
				DashiLog.warn("disk caching requested without disk path");
				path = null;
			} else {
				path = dir.resolve( name(uri.toString()) );
			}
		} else {
			path = null;
		}

		// check file
		if (path != null) {
			try (ReadStream stream = Streams.streamInput(Files.newInputStream(path))) {
				String str = stream.readChars();
				if (!str.equals(uri.toString())) {
					DashiLog.warn("Hash collision {0} and {1} resolved to {2}", str, uri, path.getFileName());
					/* FALL THROUGH */
				} else {
					long expires = stream.readLong();
					if (expires < System.currentTimeMillis()) {
						DashiLog.debug("Deleting expired file {0} for URI {1}", path.getFileName(), uri);
						Files.delete(path);
						/* FALL THROUGH */
					} else {
						DashiLog.trace("cache hit disk: {0}", uri);
						//TODO should put back into entries?
						return adapter.fromStream(stream);
					}
				}
			} catch (IOException e) {
				DashiLog.warn("Error reading item {0} from disk at {1}", e, uri, path);
				/* FALL THROUGH */
			}
		}

		// adapt
		T item;
		long expires;
		URLConnection conn = null;
		StreamBytes bytes;
		try {
			conn = uri.toURL().openConnection();
			// sample UA string from Android: "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 5 Build/LMY48B; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/43.0.2357.65 Mobile Safari/537.36"
			conn.setRequestProperty("User-Agent", "Experimental/5.0 (Linux; Raspbian; Raspberry Pi)");
			expires = conn.getExpiration();
			long length = conn.getContentLengthLong();
			int maxStreamSize = policy.getMaxStreamSize();
			if (maxStreamSize < 0) maxStreamSize = Integer.MAX_VALUE;
			int maxFileSize = policy.getMaxFileSize();
			//TODO need to limit size
			//TODO need to use size if available
			//TODO need to do this only if caching enabled
			ReadStream stream = null;
			try {
				if (length > maxStreamSize) {
					bytes = null;
					stream = Streams.streamInput(conn.getInputStream());
				} else {
					int initialSize = Math.max((int) length, -1);
					if (initialSize == -1) {
						bytes = Streams.bytes(DEFAULT_STREAM_SIZE, maxStreamSize);
					} else {
						bytes = Streams.bytes(initialSize, maxStreamSize);
					}
					try (ReadStream in = Streams.streamInput(conn.getInputStream())) {
						//bytes.writeStream().from(in).transferFully();
						in.to(bytes.writeStream()).transferFully();
					}
					stream = bytes.readStream();
				}
				item = adapter.fromStream(stream);
			} finally {
				if (stream != null) stream.close();
			}
		} catch (IOException e) {
			throw new CacheException("failed to retrieve image from " + uri, e);
		} finally {
			if (conn instanceof HttpURLConnection) {
				try {
					((HttpURLConnection)conn).disconnect();
				} catch (RuntimeException e) {
					DashiLog.warn("Problem disconnecting from {0}", e, uri);
				}
			}
		}
		if (expires < System.currentTimeMillis()) {
			DashiLog.trace("Ignoring passed expiration date {0} from {1} ", expires, uri);
			expires = 0L;
		}

		// store in memory
		if (expires != 0 && key != null) {
			int size = adapter.sizeInBytes(item);
			synchronized (entries) {
				entries.put(key, new CacheEntry(key, size, expires, item, bytes));
			}
		}

		// store on disk (how? - cache original data on entry?);
		return item;
	}

	// control methods
	void close() {
		closed = true;
	}

	void open() {
		closed = false;
	}

	void clearMemory() {
		synchronized (entries) {
			entries.clear();
		}
	}

	void clearDisk() throws CacheException {
		Path path = policy.getDiskPath();
		if (path == null) {
			DashiLog.warn("Cannot clear disk cache: no path configured");
			return;
		}
		String suffix = policy.getFileSuffix();
		try {
			Files.newDirectoryStream(path, p -> Files.isRegularFile(path) && (suffix == null || p.getFileName().toString().endsWith(suffix))).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					throw new CacheException("Failed to delete cache file: " + p, e);
				}
			});
		} catch (IOException e) {
			throw new CacheException("Error occurred listing files to clear disk cache", e);
		}
	}

	// private helper methods

	private String name(String uriStr) {
		BigInteger value = HASH.hash(uriStr).bigValue();
		String hash = String.format("%040x", value);
		String suffix = policy.getFileSuffix();
		return suffix == null ? hash : hash + suffix;
	}

	private static interface Adapter<T> {

		URI keyFor(URI uri);

		T fromStream(ReadStream stream) throws IOException;

		int sizeInBytes(T item);
	}

	private static class ImageAdapter implements Adapter<BufferedImage> {

		@Override
		public URI keyFor(URI uri) {
			return URI.create("image:" + uri.toString());
		}

		@Override
		public BufferedImage fromStream(ReadStream stream) throws IOException {
			BufferedImage image = ImageIO.read(stream.asInputStream());
			if (image == null) throw new IOException("unable to read image");
			return image;
		}

		@Override
		public int sizeInBytes(BufferedImage item) {
			DataBuffer buffer = item.getRaster().getDataBuffer();
			return buffer.getSize() * DataBuffer.getDataTypeSize(buffer.getDataType());
		}
	}

	private static class CanvasAdapter implements Adapter<TextCanvas> {

		private static final Charset UTF_8 = Charset.forName("UTF-8");
		private static final Pattern DEF = Pattern.compile("#\\s*([1-9][0-9]*)x([1-9][0-9]*)\\s+(interleaved|striped|separate)");

		@Override
		public URI keyFor(URI uri) {
			return URI.create("canvas:" + uri.toString());
		}

		@Override
		public TextCanvas fromStream(ReadStream stream) throws IOException {
			TextCanvas canvas;
			try (BufferedReader r = new BufferedReader(new InputStreamReader(stream.asInputStream(), UTF_8))) {
				String first = r.readLine();
				if (first == null) throw new IOException("empty stream");
				Matcher matcher = DEF.matcher(first);
				if (!matcher.matches()) throw new IOException("invalid definition");
				int cols = Integer.parseInt(matcher.group(1));
				int rows = Integer.parseInt(matcher.group(2));
				canvas = TextCanvas.blank(cols, rows);
				String type = matcher.group(3);
				switch (type) {
				case "interleaved" :
					for (int row = 0; row < rows; row++) {
						int lineNo = row + 2;
						String line = r.readLine();
						if (line == null) break;
						int[] arr = line.codePoints().limit(3 * cols).toArray();
						int len = arr.length / 3;
						if (len * 3 != arr.length) throw new IOException("invalid row length (line " + lineNo + ")");
						for (int col = 0; col < len; col++) {
							LogicalColor bg;
							LogicalColor fg;
							try {
								bg = LogicalColor.valueOf((char) arr[col * 3 + 0]);
								fg = LogicalColor.valueOf((char) arr[col * 3 + 1]);
							} catch (IllegalArgumentException e) {
								throw new IOException("invalid color (line + " + lineNo + ")", e);
							}
							int c = arr[col * 3 + 2];
							canvas.put(col, row, c, false, bg, fg);
						}
					}
					break;
				case "striped" :
				case "separate" :
					throw new UnsupportedOperationException("not currently supported: " + type);
					default:
						throw new IOException("invalid type");
				}
			}
			return canvas.immutable();
		}

		@Override
		public int sizeInBytes(TextCanvas canvas) {
			return canvas.width() * canvas.height() * (4 + 1 + 1);
		}

	}

	private static final class CacheEntry {

		private final URI uri;
		private final long expires;
		private final int size;
		private Object item;
		private StreamBytes bytes;

		private CacheEntry(URI uri, int size, long expires, Object item, StreamBytes bytes) {
			this.uri = uri;
			this.size = size;
			this.expires = expires;
			this.item = item;
			this.bytes = bytes;
		}
	}
}
