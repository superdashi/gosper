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
