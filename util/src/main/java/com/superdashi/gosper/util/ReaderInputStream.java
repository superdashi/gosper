package com.superdashi.gosper.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class ReaderInputStream extends InputStream {

	private static final int DEFAULT_CHARS_SIZE = 1024;
	private static final int DEFAULT_BYTES_SIZE = 128;

	private final Reader reader;
	private final CharsetEncoder encoder;
	private final CharBuffer chars;
	private final ByteBuffer bytes;
	private boolean underflow = true;
	private boolean eof = false;

	public ReaderInputStream(Reader reader, Charset charset, int bufferSize) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		if (charset == null) throw new IllegalArgumentException("null charset");
		if (bufferSize < 1) throw new IllegalArgumentException("non-positive buffer size");
		this.reader = reader;
		this.encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
		chars = CharBuffer.allocate(bufferSize);
		bytes = ByteBuffer.allocate(DEFAULT_BYTES_SIZE);
	}

	public ReaderInputStream(Reader reader, Charset charset) {
		this(reader, charset, DEFAULT_CHARS_SIZE);
	}

	@Override
	public int read() throws IOException {
		while(bytes.hasRemaining() || !eof) {
			if (eof) return bytes.get() & 0xff;
			fillBuffer();
		}
		return -1;
	}

	@Override
	public int read(final byte[] bs, int offset, int len) throws IOException {
		if (bs == null) throw new IllegalArgumentException("null bs");
		if (len < 0) throw new IndexOutOfBoundsException("negative len");
		if (offset < 0) throw new IndexOutOfBoundsException("negative offset");
		if ((offset + len) > bs.length) throw new IndexOutOfBoundsException("invalid offset and len");

		if (len == 0) return eof ? -1 : 0; // trivial case
		int read = 0;
		while (true) {
			if (bytes.hasRemaining()) {
				int c = Math.min(bytes.remaining(), len);
				bytes.get(bs, offset, c);
				offset += c;
				len -= c;
				read += c;
				if (len == 0) return read;
			} else if (!eof) {
				fillBuffer();
			} else {
				return -1;
			}
		}
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	private void fillBuffer() throws IOException {
		if (!eof && underflow) {
			chars.compact();
			int pos = chars.position();
			int c = reader.read(chars.array(), pos, chars.remaining());
			if (c == -1) {
				eof = true;
			} else {
				chars.position(pos + c);
			}
			chars.flip();
		}
		bytes.compact();
		CoderResult result = encoder.encode(chars, bytes, eof);
		if (result.isError()) throw new IOException(result.isUnmappable() ? "unmappable character" : "malformed");
		underflow = result.isUnderflow();
		bytes.flip();
	}
}
