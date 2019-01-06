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
