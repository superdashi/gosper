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
package com.superdashi.gosper.scripting;

import java.util.function.Consumer;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.item.Item;

public class SysoutScriptEngine implements ScriptEngine {

	private static void sysout(String message) {
		System.out.println("> " + message);
	}

	private static void sysout(String faceId, String message) {
		sysout("> :" + faceId + ": " + message);
	}

	@Override
	public void runtimeStarted() {
		sysout("runtime started");
	}

	@Override
	public void runtimeStopped() {
		sysout("runtime stopped");
	}

	@Override
	public SysoutScriptSession openSession(String faceId) {
		return new SysoutScriptSession(faceId);
	}

	private static class SysoutScriptSession implements ScriptSession {

		private final String faceId;

		SysoutScriptSession(String faceId) {
			this.faceId = faceId;
		}

		@Override
		public void interfaceAttached() {
			sysout(faceId, "interface attached");
		}

		@Override
		public void interfaceDetached() {
			sysout(faceId, "interface detached");
		}

		@Override
		public void eventConsumer(Consumer<Event> eventConsumer) {
			// no-op
		}

		@Override
		public void activityActivated(int activityId, Details details) {
			sysout(faceId, "activity activated " + activityId + " " + details);
		}

		@Override
		public void actionAbsent() {
			sysout(faceId, "action absent");
		}

		@Override
		public void actionSelected(String id, Item item) {
			sysout(faceId, "action selected " + id + " " + item);
		}

	}

}
