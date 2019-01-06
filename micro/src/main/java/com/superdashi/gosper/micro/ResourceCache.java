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
