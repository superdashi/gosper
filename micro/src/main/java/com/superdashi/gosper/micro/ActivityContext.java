package com.superdashi.gosper.micro;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import com.superdashi.gosper.bundle.ActivityDetails;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventHandler;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.Surface;
import com.tomgibara.streams.Streams;

// methods can be called from any thread
public class ActivityContext {

	// a delay introduced to prevent rapid flurries of redraws in response to background loading
	// at the expense of increasing update latency
	private static final long REDRAW_ENQUEUE_DELAY = 5L;

	private static ThreadLocal<ActivityContext> current = new ThreadLocal<>();

	static void setCurrent(ActivityContext context) {
		assert context != null;
		assert current.get() == null;
		current.set(context);
	}

	static void clearCurrent() {
		assert current.get() != null;
		current.set(null);
	}

	public static ActivityContext current() {
		return current.get();
	}

	private final ActivityManager manager;
	private final ActivityDriver driver;
	private final Logger logger;
	private final Thread thread; //TODO needed?
	private final DeviceSpec deviceSpec;
	private final Environment environment; // null for root application
	private Consumer<Event> eventListener;
	private ActionHandler actionHandler;
	private final Models models;
	private final Items items;
	private final Items metaItems;
	private final Activities activities;
	private final ActivityDetails activityDetails;

	private DataInput requestedConclusion = null;
	private ActivityLaunch requestedLaunch = null;

	// flag prevents multiple enquings of checkRedraw
	private boolean redrawEnqueued = false;
	// used to indicate to activity that it is active - this is set immediately prior to activation
	boolean active = false;

	ActivityContext(ActivityDriver driver) {
		this.manager = driver.manager;
		this.driver = driver;
		this.logger = driver.logger;
		this.thread = Thread.currentThread();
		this.deviceSpec = manager.deviceSpec();
		ActivityLaunch launch = driver.launch;
		AppInstance appInstance = launch.appInstance;
		Qualifier qualifier = manager.face.qualifier();

		//TODO should be a better way to do this
		String instanceId = appInstance.instanceId;
		if (instanceId.isEmpty()) {
			// system application, so no environment and no details
			environment = null;
			activityDetails = null;
		} else {
			environment = manager.face.directory().envForInstanceId(instanceId);
			Identity identity = launch.activityInstance.identity;
			if (identity == null) {
				// system activity, so no details
				activityDetails = null;
			} else {
				activityDetails = environment.appInstance.bundle.activityDetails(identity.name);
				qualifier = qualifier.withFlavor(activityDetails.flavor);
			}
		}
		logger.debug().message("activity qualifier is {}").values(qualifier).log();
		actionHandler = launch.activityInstance.defaultActionHandler;

		models = new Models(this);
		items = new Items(appInstance.bundle, qualifier, false);
		metaItems = new Items(appInstance.bundle, qualifier, true);
		activities = new Activities(this);
	}

	public DisplayConfiguration configureDisplay() {
		return new DisplayConfiguration(this);
	}

	// convenience method
	public Display layoutDisplay() {
		return new DisplayConfiguration(this).layoutDisplay();
	}

	// convenience method
	public Display layoutDisplay(Layout layout) {
		return new DisplayConfiguration(this).layoutDisplay(layout);
	}

	// the spec under which this activity is running
	// TODO should expose the current spec via the environment also
	// TODO should not be visible here - this is privileged
	public DeviceSpec deviceSpec() { return deviceSpec; }

	public ActivityDetails activityDetails() { return activityDetails; }

	public Item activityItem() {
		return activityDetails == null ? Item.nothing() : metaItems.itemWithId(activityDetails.details.identity().name);
	}

	public DataInput launchData() { return driver.launch.input; }

	public Items items() { return items; }

	public Items metaItems() { return metaItems; }

	public Environment environment() {
		if (environment == null) throw new IllegalStateException("no environment for base application");
		return environment;
	}

	public List<ActivityResponse> activityResponses() {
		synchronized (manager.lock) {
			List<ActivityResponse> copy = new ArrayList<>(driver.activityResponses);
			driver.activityResponses.clear();
			return copy;
		}
	}

	//TODO should probably change to an event handler
	public void setEventListener(Consumer<Event> eventListener) {
		synchronized (manager.lock) {
			this.eventListener = eventListener;
		}
	}

	public void setActionHandler(ActionHandler actionListener) {
		synchronized (manager.lock) {
			this.actionHandler = actionListener;
		}
	}

	public EventHandler defaultEventHandler() {
		return event -> {
			if (event == null) throw new IllegalArgumentException("null event");
			boolean handled;
			if (isActivityThread()) {
				handled = driver.handle(event);
				driver.checkResponse();
			} else {
				handled = async(() -> driver.handle(event));
				perform(() -> driver.checkResponse());
			}
			return handled;
		};
	}

	public Optional<Component> focus() {
		return isActivityThread() ? driver.focus() : async(driver::focus);
	}

	public Future<?> perform(Runnable runnable) {
		if (runnable == null) throw new IllegalArgumentException("null runnable");
		return manager.perform(() -> {
			setCurrent(this);
			try {
				runnable.run();
			} finally {
				clearCurrent();
				driver.checkRedraw();
				driver.checkConclusionAndOrLaunch();
			}
		});
	}

	public <T> Future<T> perform(Callable<T> callable) {
		if (callable == null) throw new IllegalArgumentException("null callable");
		return manager.perform(() -> {
			setCurrent(this);
			try {
				return callable.call();
			} finally {
				clearCurrent();
				driver.checkRedraw();
				driver.checkConclusionAndOrLaunch();
			}
		});
	}

	public void requestRedraw() {
		synchronized (manager.lock) {
			driver.requestRedraw();
			enqueueRedraw();
		}
	}

	public void concludeActivity() {
		requestConclusion(DataInput.NULL);
	}

	public void concludeActivity(DataOutput returnData) {
		if (returnData == null) throw new IllegalArgumentException("null returnData");
		requestConclusion(returnData.toInput());
	}

	// activity request methods

	public Activities activities() {
		return activities;
	}

	// model creation methods

	public Models models() {
		return models;
	}

	// package scoped methods

	int instanceId() {
		return driver.instanceId;
	}

	// includes id of self
	int[] ancestorIds() {
		int i = driver.ordinal;
		int[] ids = new int[i + 1];
		for (ActivityDriver d = this.driver; d != null; d = d.previous) ids[i--] = d.instanceId;
		return ids;
	}

	Logger logger() {
		return logger;
	}

	DataInput requestedConclusion() {
		synchronized (manager.lock) {
			DataInput input = requestedConclusion;
			requestedConclusion = null;
			return input;
		}
	}

	//TODO is sync required?
	ActivityLaunch requestedLaunch() {
		synchronized (manager.lock) {
			ActivityLaunch launch = requestedLaunch;
			requestedLaunch = null;
			return launch;
		}
	}
	// must be called on activity thread with lock
	void deliver(Event event) {
		if (eventListener == null) {
			// automatically route the event to be handled by the framework
			driver.handle(event);
			driver.checkResponse();
		} else {
			// pass the event to the application,
			// it must pass it back via the default event handler
			eventListener.accept(event);
		}
	}
	// must be called on activity thread with lock
	void deliver(Action action) {
		// deferred activities are handled without passing the activity to the application
		if (action.deferredActivity.isPresent()) {
			launchActivity(action.deferredActivity.get());
		} else {
			if (actionHandler == null) {
				driver.handle(action);
			} else try {
				actionHandler.handleAction(action);
			} catch (RuntimeException e) {
				logger.error().message("activity raised exception to handling action {}").values(action).stacktrace(e).log();
			}
		}
	}

	Display createDisplay(DisplayConfiguration config, Layout layout, Background background) {
		Interface face = manager.face;
		// obtain a visual context
		//TODO should have an easier way to get visuals
		//TODO visuals should be able to fallback
		VisualSpec spec = face.runtime.visuals.contextFor(config.qualifier, manager.screen.dimensions(), manager.screen.opaque())
				.orElseThrow(() -> new RuntimeException("unable to attach: no available visual context"));
		// obtain a background
		background = background == null ? spec.background : background.adaptedFor(spec);
		// create a display
		Display display = new Display(face.studio, face.surfacePool, deviceSpec.keyboard, spec, this, config, layout, background, face.scriptSession);
		synchronized (manager.lock) {
			driver.assignDisplay(display);
		}
		return display;
	}

	//TODO failure should take the address
	<X,Y> void backgroundLoad(X address, Function<X, Y> loader, Consumer<Y> consumer, Consumer<RuntimeException> failure) {
		if (isActivityThread()) {
			// can't block activity thread, push this onto a different thread
			environment.executeInBackground(() ->{ backgroundLoad(address, loader, consumer, failure); });
		} else {
			Y result;
			String addrStr = address.toString();
			try {
				logger.debug().message("commencing background load").filePath(addrStr).log();
				result = loader.apply(address);
				logger.debug().message("completed background load").filePath(addrStr).log();
			} catch (RuntimeException e) {
				logger.debug().message("background load failed").filePath(addrStr).stacktrace(e).log();
				failure.accept(e);
				return;
			}
			consumer.accept(result);
		}
	}

	Frame loadEnvImage(Image res) {
		return manager.resourceCache.frame(environment, res.uri());
	}

	Frame loadResImage(String res) {
		InputStream stream = getClass().getClassLoader().getResourceAsStream(res);
		if (stream == null) throw new ResourceException("resource not found: " + res);
		try {
			return Surface.decode(Streams.streamInput(stream));
		} catch (IOException e) {
			throw new ResourceException("failed to load resource: " + res, e);
		}
	}

	void launchActivity(DeferredActivity deferred) {
		Optional<ActivityLaunch> optLaunch = createActivityLaunch(deferred);
		optLaunch.ifPresent(this::requestLaunch);
	}

	Identity appIdentity() {
		return environment == null ? null : environment.appInstance.details.identity();
	}

	Optional<ActivityInstance> instantiateActivity(Identity appIdentity, String activityId) {
		return environment.appInstanceForIdentity(appIdentity).flatMap(i -> environment.instantiateActivity(i, activityId));
	}

	private boolean isActivityThread() {
		return thread == Thread.currentThread();
	}

	// always called with manager lock
	// because of this redrawEnqueued does not need to be separately synchronized
	private void enqueueRedraw() {
		// don't need to enqueue redraws on the activity ui thread
		// and we only need one queued
		if (!isActivityThread() && !redrawEnqueued) {
			redrawEnqueued = true;
			Runnable r = () -> {
				redrawEnqueued = false;
				driver.checkRedraw();
			};
			if (REDRAW_ENQUEUE_DELAY == 0L) {
				manager.perform(r);
			} else {
				manager.schedule(r, REDRAW_ENQUEUE_DELAY);
			}
		}
	}

	private void requestConclusion(DataInput data) {
		synchronized (manager.lock) {
			if (driver.previous == null) return; // base activity cannot conclude itself
			requestedConclusion = data;
		}
		if (isActivityThread()) {
			manager.perform(driver::checkConclusionAndOrLaunch);
		}
	}

	private void requestLaunch(ActivityLaunch launch) {
		synchronized (manager.lock) {
			if (requestedLaunch != null) throw new IllegalStateException("activity launch already requested");
			requestedLaunch = launch;
		}
		if (isActivityThread()) {
			manager.perform(driver::checkConclusionAndOrLaunch);
		}
	}

	private <T> T async(Callable<T> t) {
		try {
			return manager.perform(t).get();
			//TODO this is an ugly hack!
		} catch (InterruptedException e) {
			throw new RuntimeException("", e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new RuntimeException(cause);
		}
	}

	private Optional<ActivityLaunch> createActivityLaunch(DeferredActivity deferred) {
		// note: try to reuse an activity if so requested
		return environment.createActivityLaunch(deferred, existingActivity(deferred));
	}

	private ActivityInstance existingActivity(DeferredActivity deferred) {
		return deferred.mode.relaunch() && deferred.activityIdentity != null ? driver.existingActivity(deferred.activityIdentity) : null;
	}
}
