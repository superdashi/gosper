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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.superdashi.gosper.bundle.ActivityDetails;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.micro.Activity.State;
import com.tomgibara.fundament.Producer;

class ActivityManager {

	private static final long EVENT_POLL_DELAY = 20L;

	final Object lock = new Object();
	final ScheduledExecutorService executor; // used to run code on the 'main ui thread'
	final ResourceCache resourceCache;

	final Interface face;
	final Screen screen;
	final Logger logger;
	final Logger uiLogger;
	private final Producer<List<Event>> eventSource;
	private final ScheduledFuture<?> eventPolling;
	private DeviceSpec deviceSpec;

	private int nextDriverInstanceId = 1;
	ActivityDriver rootDriver;
	private ActivityDriver activeDriver;
	private boolean halting = false;

	// used to interpolate the activity item for activity titling
	private String[] activityItemInterpolate;

	ActivityManager(Interface face) throws InterruptedException {
		assert face != null;
		// hold a reference to the face, we need the runtime
		this.face = face;
		// we need the screen to perform visual updates
		this.screen = face.screen();
		// default the spec based on the one defined by the runtime
		Device device = face.device;
		deviceSpec = device.getSpec();
		// assign ourselves a logger, using the runtime loggers
		logger = face.logger.child("act-man");
		uiLogger = logger.child("ui");
		// this is the application that we will launch implicitly
		AppInstance baseAppInstance = face.baseAppInstance;
		// now prep for execution
		screen.clear();
		screen.update();
		executor = new LoggingExecutor();
		resourceCache = new ResourceCache(logger.child("res-cache"));
		launchApplication(baseAppInstance);
		// ready for events
		eventSource = device.events().orElse(null);
		if (eventSource == null) {
			// events will be delivered by device
			device.setEventConsumer(this::deliverEvent);
			eventPolling = null;
		} else {
			// manager must poll device
			eventPolling = executor.scheduleWithFixedDelay(this::pollForEvents, 0L, EVENT_POLL_DELAY, TimeUnit.MILLISECONDS);
		}
		if (face.scriptSession != null) face.scriptSession.eventConsumer(this::deliverEvent);
	}

	public void configure(DeviceSpec deviceSpec) {
		if (deviceSpec == null) throw new IllegalArgumentException("null deviceSpec");
		synchronized (lock) {
			this.deviceSpec = deviceSpec;
		}
	}

	public Future<?> deliverEvent(Event event) {
		if (event == null) throw new IllegalArgumentException("null event");
		logger.debug().message("manager received event {}").values(event).log();
		if (halting) return null; // we're halting
		return perform(() -> {
			if (halting) return; // we're halting
			if (activeDriver == null) return; // we just shutdown the last activity?
			boolean eventSent = activeDriver.deliver(event);
			if (eventSent) {
				activeDriver.checkRedraw();
				activeDriver.checkConclusionAndOrLaunch();
			}
		});
	}

	public boolean launchApplication(AppInstance appInstance) throws InterruptedException {
		if (appInstance == null) throw new IllegalArgumentException("null appInstance");
		List<String> activityIds = appInstance.bundle.launchActivityIds();
		if (activityIds.isEmpty()) return false;
		for (String id : activityIds) {
			launchActivity(appInstance, id, null);
		}
		return true;
	}

	public void launchActivity(AppInstance appInstance, String activityId, DataOutput launchData) throws InterruptedException {
		if (appInstance == null) throw new IllegalArgumentException("null appInstance");
		if (activityId == null) throw new IllegalArgumentException("null activityId");
		Optional<ActivityDetails> optionalDetails = appInstance.bundle.optionalActivityDetails(activityId);
		if (!optionalDetails.isPresent()) throw new IllegalStateException("no activity details for activityId: " + activityId);
		Optional<ActivityInstance> activity = appInstance.instantiateActivity(activityId, logger);
		if (!activity.isPresent()) throw new IllegalStateException("application returned null launch activity");
		DeferredActivity deferred = new DeferredActivity(activityId, appInstance.bundle.appData().appDetails().identity(), optionalDetails.get().details.identity(), DeferredActivity.NO_ANCESTOR_IDS);
		deferred.launchData = launchData;
		deferred.mode = ActivityMode.DETATCH;
		ActivityLaunch launch = new ActivityLaunch(appInstance, activity.get(), false, deferred);
		performSync("launching activity", () -> launchActivityImpl(null, rootDriver, launch));
	}

	//TODO method needs to capture current driver, in case it changes before call
	public void concludeActivity(DataInput data) throws InterruptedException {
		performSync("concluding activity", () -> {
			// cannot conclude the base activity
			if (activeDriver.previous != null) {
				concludeCurrentActivity(data);
			}
		});
	}

	public void halt(long timeout) throws InterruptedException {
		if (executor.isShutdown()) return; // alternative, synchronous indicator of having halted
		performSync("halting", () -> {
			halting = true;
			while (activeDriver != null) {
				concludeCurrentActivity(null);
			}
		});
		executor.shutdown();
		executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
		screen.blank();
	}

	Future<?> perform(Runnable r) {
		int hash = System.identityHashCode(r);
		String name = r.getClass().getName();
		uiLogger.debug().message("perform: submitting runnable {} {}").values(name, hash).log();
		return executor.submit(() -> {
			uiLogger.debug().message("perform: locking runnable {} {}").values(name, hash).log();
			synchronized (lock) {
				uiLogger.debug().message("perform: locked runnable {} {}").values(name, hash).log();
				try {
					r.run();
				} catch (RuntimeException e) {
					logger.error().stacktrace(e).log();
				}
			}
		});
	}

	<T> Future<T> perform(Callable<T> t) {
		int hash = System.identityHashCode(t);
		String name = t.getClass().getName();
		uiLogger.debug().message("perform: submitting callable {} {}").values(name, hash).log();
		return executor.submit(() -> {
			uiLogger.debug().message("perform: locking callable {} {}").values(name, hash).log();
			synchronized (lock) {
				uiLogger.debug().message("perform: locked callable {} {}").values(name, hash).log();
				try {
					return t.call();
				} catch (RuntimeException e) {
					logger.error().message("perform: error {} {}").values(name, hash).stacktrace(e).log();
					throw e;
				} finally {
					uiLogger.debug().message("perform: completed {} {}").values(name, hash).log();
				}
			}
		});
	}

	Future<?> schedule(Runnable r, long delay) {
		int hash = System.identityHashCode(r);
		String name = r.getClass().getName();
		uiLogger.debug().message("schedule: submitting runnable {} {}").values(name, hash).log();
		return executor.schedule(() -> {
			uiLogger.debug().message("schedule: locking runnable {} {}").values(name, hash).log();
			synchronized (lock) {
				uiLogger.debug().message("schedule: locked runnable {} {}").values(name, hash).log();
				try {
					r.run();
				} catch (RuntimeException e) {
					logger.error().message("schedule: error {} {}").values(name, hash).stacktrace(e).log();
				} finally {
					uiLogger.debug().message("schedule: completed {}").values(name, hash).log();
				}
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

	void performSync(String message, Runnable r) throws InterruptedException {
		int hash = System.identityHashCode(r);
		uiLogger.debug().message("peformSync: submitting {} {}").values(message, hash).log();
		try {
			Future<?> future = executor.submit(() -> {
				uiLogger.debug().message("peformSync: locking {} {}").values(message, hash).log();
				synchronized (lock) {
					uiLogger.debug().message("peformSync: locked {} {}").values(message, hash).log();
					if (isHalted()) throw new IllegalStateException("halted");
					try {
						r.run();
					} finally {
						uiLogger.debug().message("peformSync: completed {} {}").values(message, hash).log();
					}
				}
			});
			future.get();
		} catch (ExecutionException e) {
			throw new RuntimeException(message, e);
		}
	}

	// must be called on executor and with lock
	DeviceSpec deviceSpec() {
		return deviceSpec;
	}

	boolean halting() {
		return halting;
	}

	// must have lock and be called via executor
	void recordException(String message, Throwable e) {
		logger.error().message(message).stacktrace(e).log();
	}

	// must be called on executor and with lock
	void launchActivityImpl(DataInput conclusion, ActivityDriver previous, ActivityLaunch launch) {
		if (activeDriver != null) {
			activeDriver.transitionDownTo(State.OPEN);
		}
		if (conclusion != null) { // we have to swap
			previous.transitionDownTo(State.CONSTRUCTED);
			previous = previous.previous;
		}
		int instanceId = nextDriverInstanceId();
		if (previous == null) {
			rootDriver = activeDriver = new ActivityDriver(this, instanceId, 0, null, launch);
		} else {
			activeDriver = new ActivityDriver(this, instanceId, previous.ordinal + 1, previous, launch);
		}
		activateCurrentDriver();
	}

	void setCurrentDriver(ActivityDriver driver) {
		assert !driver.hasSuccessors();
		if (driver == activeDriver) return; // no change
		// passivate the currently active driver
		activeDriver.transitionDownTo(State.OPEN);
		// reorganize the hierarchy to ensure new active is top
		driver.makeTop();
		// set the current active driver
		activeDriver = driver;
	}

	void concludeCurrentActivity(DataInput data) {
		assert activeDriver != null;
		concludeActivityImpl(activeDriver, data);
	}

	// must be called on executor and with lock
	void concludeActivityImpl(ActivityDriver driver, DataInput data) {
		//TODO inefficiency here - driver doesn't know to avoid collecting state on close
		driver.transitionDownTo(State.CONSTRUCTED);
		ActivityLaunch launch = driver.launch;
		ActivityDriver previous = driver.previous;

		if (previous != null) {
			if (launch.mode.respond() && data != DataInput.NULL) {
				// the requester wanted a response, and we had one to give
				previous.returnData(launch, data);
			}
		}
		if (activeDriver == driver) {
			activeDriver = previous == null ? null : previous.top();
			if (activeDriver != null) activateCurrentDriver();
		}
	}

	// must be called on executor and with lock
	void relaunchActivityImpl(State relaunchState, ActivityLaunch launch) {
		activeDriver.transitionDownTo(relaunchState);
		activeDriver.launch = launch;
		activateCurrentDriver();
	}

//	void concludeStack() {
//		int ordinal = -1;
//		while (activeDriver != null && ordinal != 0) {
//			ordinal = activeDriver.ordinal;
//			concludeActivityImpl(null);
//		}
//	}

	// preserved must be before top
	void concludeUpTo(ActivityDriver top, ActivityDriver preserved) {
		while (top != preserved) {
			ActivityDriver below = top.below();
			concludeActivityImpl(top, null);
			top = below;
		}
	}

	ActivityDriver locateDriver(int[] instanceIds) {
		if (instanceIds.length == 0) return null;
		ActivityDriver driver = rootDriver;
		if (driver == null || driver.instanceId != instanceIds[0]) return null;
		for (int i = 1; i < instanceIds.length; i++) {
			driver = driver.successorById(instanceIds[i]);
			if (driver == null) break;
		}
		return driver;
	}

	//because this may trigger further lifecycle changes, this must be the last method called in other lifecycle methods
	private void activateCurrentDriver() {
		ActivityDriver current = activeDriver; // being careful: some methods (eg. conclusion) may change driver
		current.transitionUpTo(State.ACTIVE);
		current.checkConclusionAndOrLaunch();
	}

	private boolean isHalted() {
		return halting && activeDriver == null;
	}

	private int nextDriverInstanceId() {
		return nextDriverInstanceId++;
	}

	private void pollForEvents() {
		uiLogger.debug().message("polling for events").log();
		synchronized (lock) {
			if (halting || activeDriver == null) return; // we're halting or we just shutdown the last activity?
			List<Event> events = eventSource.produce();
			uiLogger.debug().message("received {} events").values(events.size()).log();
			if (events.isEmpty()) return; // no events
			boolean eventsSent = false;
			for (Event event : events) {
				eventsSent = activeDriver.deliver(event) | eventsSent;
			}
			if (eventsSent) {
				activeDriver.checkRedraw();
				activeDriver.checkConclusionAndOrLaunch();
			}
		}
	}
	// inner classes

	private class LoggingExecutor extends ScheduledThreadPoolExecutor {

		public LoggingExecutor() {
			super(1, r -> new Thread(r, "gosper-UI"));
		}

		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			if (t == null && r instanceof Future && !(r instanceof ScheduledFuture<?>)) {
				Future<?> f = ((Future<?>) r);
				if (!f.isCancelled()) {
					try {
						f.get();
	//				} catch (CancellationException e) {
	//					t = e;
					} catch (ExecutionException e) {
						t = e.getCause();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); // ignore/reset
					}
				}
			}
			if (t != null) logger.error().message("exception on gosper UI thread").stacktrace(t).log();
		}
	}

}
