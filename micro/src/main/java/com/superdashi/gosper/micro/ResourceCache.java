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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.Surface;
import com.tomgibara.fundament.Producer;
import com.tomgibara.streams.ReadStream;

//TODO synchronization
final class ResourceCache {

	private static final boolean DELAY_LOADING = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.micro.ResourceCache.DELAY_LOADING"));
	private static final long DELAY = 1000L;

	private enum Type {
		FRAME;
	}

	private static final class Key {

		final Type type;
		final URI uri;
		final int hashCode;

		Key(Type type, URI uri) {
			this.type = type;
			this.uri = uri;
			hashCode = type.hashCode() + uri.hashCode();
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof ResourceCache.Key)) return false;
			ResourceCache.Key that = (ResourceCache.Key) obj;
			return
					this.hashCode == that.hashCode &&
					this.type == that.type &&
					this.uri.equals(that.uri);
		}

		@Override
		public String toString() {
			return type + " " + uri + "(" + "" + ")";
		}
	}

	private final Logger logger;
	private final Map<Key, Object> entries = new HashMap<>();

	ResourceCache(Logger logger) {
		this.logger = logger;
	}

	Frame frame(Environment env, URI uri) {
		Producer<ReadStream> source = env.requestReadStream(uri); // we have to do this first to ensure the caller has privileges
		Key key = new Key(Type.FRAME, uri);
		Frame frame = (Frame) entries.get(key);
		if (frame != null) return frame;
		try {
			frame = Surface.decode(source.produce()).immutableView();
		} catch (IOException e) {
			//TODO need to ensure no data leaks via e
			throw new ResourceException("failed to read image", e);
		}
		if (DELAY_LOADING) try {
			logger.debug().message("delaying loading for app {}").values(env.appInstance.instanceId).filePath(uri).log();
			Thread.sleep(DELAY);
		} catch (InterruptedException e) {
			/* ignored */
		}
		//TODO need to record date on entry?
		//TODO need to sort entries by qualification, so that they can be flushed on runtime changes
		entries.put(key, frame);
		return frame;
	}

}
