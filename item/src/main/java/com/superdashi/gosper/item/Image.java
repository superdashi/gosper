package com.superdashi.gosper.item;

import java.net.URI;

public final class Image {

	public enum Type {

		HTTP,
		FILE,
		INTERNAL;

	}

	private static Type type(URI uri) {
		String scheme = uri.getScheme();
		if (scheme == null) throw new IllegalArgumentException("no URI scheme");
		switch (scheme) {
		case "internal": return Type.INTERNAL;
		case "file"    : return Type.FILE    ;
		case "http"    :
		case "https"   : return Type.HTTP    ;
		default:
			throw new IllegalArgumentException("unsupported URI scheme: " + scheme);
		}
	}

	private final URI uri;
	private final Type type;

	public Image(String uri) {
		if (uri == null) throw new IllegalArgumentException("null uri");
		this.uri = URI.create(uri);
		type = type(this.uri);
	}

	public Image(URI uri) {
		if (uri == null) throw new IllegalArgumentException("null uri");
		this.uri = uri;
		type = type(uri);
	}

	public URI uri() {
		return uri;
	}

	public Type type() {
		return type;
	}

	// object methods

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Image)) return false;
		Image that = (Image) obj;
		return this.uri.equals(that.uri);
	}

	@Override
	public String toString() {
		return uri.toString();
	}
}
