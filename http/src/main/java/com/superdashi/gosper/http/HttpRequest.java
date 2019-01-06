package com.superdashi.gosper.http;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.StreamException;

// designed not to expose domain name
public interface HttpRequest {

	static long getLongHeader(HttpRequest request, String headerName) {
		Optional<String> header = request.getHeaderAsString(headerName);
		if (!header.isPresent()) return -1L;
		try {
			return Long.parseLong(header.get());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	// note: toString() returns original query string
	interface Query {

		//TODO should settle on list or set?
		Collection<String> keys();

		// empty list if value not present
		List<String> values(String key);

		// assumes a single value
		String value(String key);
	}

	interface Cookie {

		String name();

		String path();

		boolean secure();

		int maxAge();

	}

	String protocol();

	String method();

	String path();

	// access to query params

	Query query();

	// general access to headers

	Set<String> headerNames();

	Optional<String> getHeaderAsString(String headerName);

	default Optional<Number> getHeaderAsNumber(String headerName) {
		return getHeaderAsString(headerName).map(s -> {
			try {
				return new BigDecimal(s);
			} catch (NumberFormatException e) {
				return null;
			}
		});
	}

	default Optional<ZonedDateTime> getHeaderAsDateTime(String headerName) {
		return getHeaderAsString(headerName).map(s -> {
			try {
				return HttpUtil.parseHttpDate(s);
			} catch (DateTimeParseException e) {
				return null;
			}
		});
	}

	// convenient access to http headers

	default long contentLength() {
		return getLongHeader(this, "Content-Length");
	}

	default String contentType() {
		return getHeaderAsString("Content-Type").orElse(null);
	}

	default String mimeType() {
		return HttpUtil.mimeType(contentType());
	}

	default String characterEncoding() {
		return HttpUtil.mimeType(characterEncoding());
	}

	List<Cookie> getCookies();

	// content

	ReadStream contentAsStream();

	default Reader contentAsReader() throws StreamException {
		String encoding = characterEncoding();
		Charset cs;
		try {
			cs = Charset.forName(encoding);
		} catch (IllegalArgumentException e) {
			throw new StreamException("unsupported character encoding: " + encoding);
		}
		return new InputStreamReader(contentAsStream().asInputStream(), cs);
	}

	default String contentAsString() throws StreamException {
		return HttpUtil.contentAsString(this);
	}

	default byte[] contentAsBytes() throws StreamException {
		int initialCapacity = HttpUtil.initialCapacity(this);
		StreamBytes bytes = HttpUtil.requestBytes(initialCapacity);
		try (ReadStream in = contentAsStream()) {
			long transferred = bytes.writeStream().from(in).transferFully().bytesTransfered();
			return transferred == initialCapacity ? bytes.directBytes() : bytes.bytes();
		}
	}
}
