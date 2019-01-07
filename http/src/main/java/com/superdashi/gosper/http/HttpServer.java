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
package com.superdashi.gosper.http;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.tomgibara.fundament.Consumer;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.AsyncRunner;
import fi.iki.elonen.NanoHTTPD.ClientHandler;
import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;

//TODO what to do about non-standard header capitalization?
public final class HttpServer {

	private static final Consumer<ReqRes> passiveConsumer = (hrr) -> {};
	static final Status OK = new Status(200, "OK");
	static final Status NOT_FOUND = new Status(404, "Not found");

	private final NanoHTTPD httpd;
	private final Map<String, Consumer<ReqRes>> handlers = new HashMap<>();
	private ExecutorService executor = null;

	public HttpServer(int port) {
		httpd = new NanoHTTPD(port) {
			@Override
			public Response serve(IHTTPSession session) {
				String uri = session.getUri();
				// identify the consumer
				Consumer<ReqRes> consumer = null;
				int length = 0;
				//TODO use something more optimal - eg. a trie
				for (Entry<String,Consumer<ReqRes>> entry : handlers.entrySet()) {
					String prefix = entry.getKey();
					if (uri.startsWith(prefix) && prefix.length() > length) {
						consumer = entry.getValue();
						length = prefix.length();
					}
				}
				SessionReqRes reqres = new SessionReqRes(session);
				if (consumer != null) {
					reqres.trimPath(length - 1);
					consumer.consume(reqres);
				}
				if (reqres.response == null) {
					reqres.respondNotFound();
				}
				return reqres.response.response;
			}
		};
		httpd.setAsyncRunner(new Runner());
	}

	public void setHandler(String uriPrefix, Consumer<ReqRes> handler) {
		if (uriPrefix == null) throw new IllegalArgumentException("null uriPrefix");
		if (!uriPrefix.startsWith("/")) throw new IllegalArgumentException("uriPrefix does not begin with '/'");
		if (started()) throw new IllegalStateException("started");
		if (!uriPrefix.endsWith("/")) uriPrefix += "/";
		if (handler == null) handler = passiveConsumer;
		handlers.put(uriPrefix, handler);
	}

	public void clearHandler(String uriPrefix) {
		if (uriPrefix == null) throw new IllegalArgumentException("null uriPrefix");
		if (!uriPrefix.startsWith("/")) throw new IllegalArgumentException("uriPrefix does not begin with '/'");
		if (started()) throw new IllegalStateException("started");
		if (!uriPrefix.endsWith("/")) uriPrefix += "/";
		handlers.remove(uriPrefix);
	}

	public void start(ExecutorService executor) {
		if (executor == null) throw new IllegalArgumentException("null executor");
		if (started()) throw new IllegalStateException("started");
		this.executor = executor;
		try {
			httpd.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		} catch (RuntimeException | IOException e) {
			throw new RuntimeException("failed to start data server", e);
		}
	}

	public void stop() {
		if (!started()) throw new IllegalStateException("stopped");
		httpd.stop();
		this.executor = null;
	}

	private boolean started() {
		return executor != null;
	}

	static class Status implements IStatus {

		private final int code;
		private final String message;

		Status(int code) {
			this.code = HttpUtil.validStatusCode(code);
			this.message = HttpUtil.defaultStatusMessage(code);
		}

		Status(int code, String message) {
			this.code = HttpUtil.validStatusCode(code);
			this.message = message == null || message.isEmpty() ? HttpUtil.defaultStatusMessage(code) : message;
		}

		@Override
		public int getRequestStatus() {
			return code;
		}

		@Override
		public String getDescription() {
			return code + " " + message;
		}

	}

	public static interface ReqRes extends HttpReqRes {

		void trimPath(int chars);

	}

	private static final class SessionReqRes implements ReqRes {

		private final SessionRequest request;
		private SessionResponse response = null;

		SessionReqRes(IHTTPSession session) {
			this.request = new SessionRequest(session);
		}

		@Override
		public void trimPath(int chars) {
			if (chars > 0) request.trimPath(chars);
		}

		@Override
		public HttpRequest request() {
			return request;
		}

		@Override
		public void respondStatus(int statusCode, String statusMessage) {
			respond(new Status(statusCode, statusMessage));
		}

		@Override
		public void respondOkay() {
			respond(OK);
		}

		@Override
		public void respondNotFound() {
			respond(NOT_FOUND);
		}

		@Override
		public void respondText(int statusCode, String mimeType, String content) {
			if (mimeType == null) throw new IllegalArgumentException("null mimeType");
			if (content == null) throw new IllegalArgumentException("null content");
			response = new SessionResponse(NanoHTTPD.newFixedLengthResponse(new Status(statusCode, null), mimeType, content));
		}

		void respond(Status status) {
			response = new SessionResponse(NanoHTTPD.newFixedLengthResponse(status, "text/plain", status.message));
		}

	}

	private static final class SessionRequest implements HttpRequest {

		private final IHTTPSession session;
		private String path;

		SessionRequest(IHTTPSession session) {
			this.session = session;
			this.path = session.getUri();
		}

		@Override
		public String protocol() {
			//TODO is this exposed anywhere?
			throw new UnsupportedOperationException();
		}

		@Override
		public String method() {
			return session.getMethod().name();
		}

		@Override
		public String path() {
			return path;
		}

		@Override
		public Query query() {
			return new Query() {

				@Override
				public Collection<String> keys() {
					return session.getParameters().keySet();
				}

				@Override
				public List<String> values(String key) {
					List<String> list = session.getParameters().get(key);
					return list == null ? Collections.emptyList() : list;
				}

				@Override
				public String value(String key) {
					List<String> list = session.getParameters().get(key);
					return list == null || list.isEmpty() ? null : list.get(0);
				}

				@Override
				public String toString() {
					return session.getQueryParameterString();
				}
			};
		}

		@Override
		public Set<String> headerNames() {
			return session.getHeaders().keySet();
		}

		@Override
		public Optional<String> getHeaderAsString(String headerName) {
			return Optional.ofNullable( session.getHeaders().get(headerName.toLowerCase()) );
		}

		@Override
		public long contentLength() {
			return HttpRequest.getLongHeader(this, "content-length");
		}

		@Override
		public String contentType() {
			return session.getHeaders().get("content-type");
		}

		@Override
		public List<Cookie> getCookies() {
			CookieHandler cookies = session.getCookies();
			cookies.forEach(s -> {
				String c = cookies.read(s);
				//TODO parse and accumulate
			});
			throw new UnsupportedOperationException();
		}

		@Override
		public ReadStream contentAsStream() {
			return Streams.streamInput(session.getInputStream());
		}

		 void trimPath(int chars) {
			path = path.substring(Math.min(chars, path.length()));
		}

	}

	private static final class SessionResponse implements HttpResponse {

		private final Response response;

		SessionResponse(Response response) {
			this.response = response;
		}
	}

	private class Runner implements AsyncRunner {

		private final Map<ClientHandler, Future<?>> futures = new HashMap<>();

		@Override
		synchronized public void closeAll() {
			for (Future<?> future : futures.values()) {
				future.cancel(true);
			}
		}

		@Override
		synchronized public void closed(ClientHandler code) {
			Future<?> future = futures.remove(code);
			if (future != null) future.cancel(true);
		}

		@Override
		synchronized public void exec(ClientHandler code) {
			Future<?> future = executor.submit(code);
			futures.put(code, future);
		}

	}
}
