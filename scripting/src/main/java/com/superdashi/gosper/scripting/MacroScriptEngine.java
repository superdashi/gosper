package com.superdashi.gosper.scripting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.item.Item;

public class MacroScriptEngine implements ScriptEngine {

	public static abstract class Update {

		abstract void deliverTo(Macro macro);
	}

	public static class ActivityUpdate extends Update {

		public final int id;
		public final Details details;
		//convenience
		public final String identityName;

		ActivityUpdate(int id, Details details) {
			this.id = id;
			this.details = details;
			identityName = details == null ? "" : details.identity().name;
		}

		@Override
		void deliverTo(Macro macro) {
			macro.activityUpdate(this);
		}

		@Override
		public String toString() {
			return id + " " + details;
		}
	}

	public static class ActionUpdate extends Update {

		public final String id;
		public final Item item;

		ActionUpdate(String id, Item item) {
			this.id = id;
			this.item = item;
		}

		@Override
		void deliverTo(Macro macro) {
			macro.actionUpdate(this);
		}

		@Override
		public String toString() {
			return id + " " + item;
		}
	}

	public static class NoActionUpdate extends Update {

		NoActionUpdate() {
		}

		@Override
		void deliverTo(Macro macro) {
			macro.noActionUpdate(this);
		}

		@Override
		public String toString() {
			return "<no action>";
		}
	}

	static public final class MacroEvents {

		public final Consumer<Event> consumer;

		MacroEvents(Consumer<Event> consumer) {
			this.consumer = consumer;
		}

		public void pressKey(int key) {
			consumer.accept(Event.newKeyEvent(key, true));
			consumer.accept(Event.newKeyEvent(key, false));
		}

		public void pressCancel() {
			pressKey(Event.KEY_CANCEL);
		}

		public void pressConfirm() {
			pressKey(Event.KEY_CONFIRM);
		}
	}

	public interface Macro {

		void events(MacroEvents events);

		void activityUpdate(ActivityUpdate update);

		void actionUpdate(ActionUpdate update);

		void noActionUpdate(NoActionUpdate update);

		default void delay(long delay) {
			if (delay < 0L) throw new IllegalArgumentException("negative delay");
			if (delay == 0L) return;
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// ignored
			}
		}
	}

	@Override
	public void runtimeStarted() {
	}

	@Override
	public void runtimeStopped() {
	}

	private final Function<String, Macro> macroSource;

	public MacroScriptEngine(Function<String, Macro> macroSource) {
		if (macroSource == null) throw new IllegalArgumentException("null macroSource");
		this.macroSource = macroSource;
	}

	@Override
	public ScriptSession openSession(String faceId) {
		Macro macro = macroSource.apply(faceId);
		return macro == null ? null : new MacroScriptSession(macro);
	}

	private class MacroScriptSession implements ScriptSession {

		private final Macro macro;
		private final Thread thread;
		private volatile boolean running = true;
		private final List<Update> updates = new ArrayList<>();

		public MacroScriptSession(Macro macro) {
			this.macro = macro;
			thread = new Thread(this::execute);
		}

		@Override
		public void interfaceAttached() {
			thread.start();
		}

		@Override
		public void interfaceDetached() {
			running = false;
			synchronized (updates) {
				updates.notifyAll();
			}
		}

		@Override
		public void eventConsumer(Consumer<Event> eventConsumer) {
			macro.events(new MacroEvents(eventConsumer));
		}

		@Override
		public void activityActivated(int activityId, Details details) {
			deliver(new ActivityUpdate(activityId, details));
		}

		@Override
		public void actionAbsent() {
			deliver(new NoActionUpdate());
		}

		@Override
		public void actionSelected(String id, Item item) {
			deliver(new ActionUpdate(id, item));
		}

		private void deliver(Update update) {
			synchronized (updates) {
				updates.add(update);
				updates.notifyAll();
			}
		}

		private void execute() {
			while (running) {
				List<Update> list;
				synchronized (updates) {
					while (updates.isEmpty() && running) {
						try {
							updates.wait();
						} catch (InterruptedException e) {
							// ignored
						}
					}
					list = new ArrayList<>(updates);
					updates.clear();
				}
				for (Update update : list) {
					update.deliverTo(macro);
				}
			}
		}
	}
}
