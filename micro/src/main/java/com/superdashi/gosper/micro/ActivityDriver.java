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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import com.superdashi.gosper.bundle.ActivityDetails;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.device.EventState;
import com.superdashi.gosper.device.EventSwitch;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.micro.Activity.State;
import com.superdashi.gosper.micro.Display.FocusDir;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.scripting.ScriptSession;
import com.tomgibara.intgeom.IntCoords;

//TODO consider making this the context?
class ActivityDriver {

	private static final EventSwitch<Display.FocusDir> focusSwitch = EventSwitch.over(Display.FocusDir.class,
			EventMask.keyDown(Event.KEY_LEFT),
			EventMask.keyDown(Event.KEY_RIGHT),
			EventMask.keyDown(Event.KEY_UP),
			EventMask.keyDown(Event.KEY_DOWN),
			EventMask.never(),
			EventMask.never()
			);

	final ActivityManager manager;
	final int instanceId;
	final int ordinal;
	final ActivityDriver previous;
	ActivityLaunch launch; // cannot be final because of relaunching
	final Logger logger;

	private final EventState eventState = new EventState();
	private final List<ActivityDriver> successors = new ArrayList<>(1);
	private State state = State.CONSTRUCTED;
	// created on init
	private ActivityContext context;
	//private EventResponse eventResponse;
	// expected to be created on open
	private Display display = null;
	// created on close
	private DataInput savedState;
	// created on conclusion of subsequent activity
	final List<ActivityResponse> activityResponses = new ArrayList<>();
	Display.ComponentResponse componentResponse = null;
	// created when a delayed redraw is necessary
	private Future<?> futureRedraw;
	private long futureRedrawTime;

	ActivityDriver(ActivityManager manager, int instanceId, int ordinal, ActivityDriver previous, ActivityLaunch launch) {
		this.manager = manager;
		this.instanceId = instanceId;
		this.previous = previous;
		this.launch = launch;
		this.ordinal = ordinal;
		//TODO should use activity identity information in subname
		String activityName = launch.activityInstance.identity == null ? "system-activity" : launch.activityInstance.identity.name;
		logger = manager.logger.child("driver").descendant(activityName, Integer.toString(instanceId));
	}

	// must be called with lock via executor
	// callable on external thread
	//TODO would need to call impl methods to avoid checking for conclusions/launches mid transition
	void transitionTo(State newState) {
		transitionUpTo(newState);
		transitionDownTo(newState);
	}

	void transitionUpTo(State newState) {
		logger.debug().message("transitioning up to state {}").values(newState).log();
		int target = newState.ordinal();
		while (state.ordinal() < target) {
			switch (state) {
			case CONSTRUCTED: initActivity();      break;
			case INITIALIZED: openActivity();      break;
			case OPEN:        activateActivity();  break;
			default: throw new IllegalStateException();
			}
		}
	}

	void transitionDownTo(State newState) {
		logger.debug().message("transitioning down to state {}").values(newState).log();
		int target = newState.ordinal();
		while (state.ordinal() > target) {
			switch (state) {
			case INITIALIZED: destroyActivity();   break;
			case OPEN:        closeActivity();     break;
			case ACTIVE:      passivateActivity(); break;
			default: throw new IllegalStateException();
			}
		}
	}

	// must be called with lock via executor
	boolean deliver(Event event) {
		if (state != State.ACTIVE) return false;
		List<Event> events = eventState.apply(event);
		if (events.isEmpty()) return false;
		ActivityContext.setCurrent(context);
		try {
			events.forEach(context::deliver);
		} finally {
			ActivityContext.clearCurrent();
		}
		return true;
	}

	boolean handle(Event event) {
		// check if event can be used by focus
		if (display != null) {
			// first component gets to use event
			boolean handled = display.handleEvent(event);
			if (handled) return true;
			// then possibly treat it as a focus change
			if (event.isPoint()) { // maybe focus change by pointing
				for (Situation sit : display.situationsAt(IntCoords.at(event.x, event.y))) {
					if (sit.focus()) return true;
				}
			} else { // maybe focus change by direction
				Optional<FocusDir> optDir = focusSwitch.valueFor(event);
				if (optDir.isPresent()) {
					boolean focusChange = display.moveFocus(optDir.get());
					//TODO eliminate via calls within components
					if (focusChange) {
						return true;
					}
				}
			}
		}
		// finally it may be a standard activity function (at this time, just 'back')
		if (event.isKey() && event.isDown() && event.key == Event.KEY_CANCEL) {
			context.concludeActivity(); //TODO would be preferable not to use an API method here
			return true;
		}
		// nothing matched - ignore
		return false;
	}

	boolean handle(Action action) {
		// currently a no-op
		return false;
	}

	Optional<Component> focus() {
		if (display == null) return Optional.empty();
		return Optional.ofNullable(display.focus());
	}

	boolean focus(Component component) {
		return display.focus(component);
	}

	void requestRedraw() {
		if (display != null) display.requestRedraw();
	}

	void assignDisplay(Display display) {
		if (state != State.INITIALIZED) throw new IllegalStateException("cannot assign display when " + state);
		this.display = display;
	}

	// must have lock and be called via executor
	boolean checkResponse() {
		List<Action> actions = display.takeComponentActions();
		if (actions.isEmpty()) return false;
		for (Action action : actions) {
			// assumes application exceptions will be trapped adjacent to application call
			// so exceptions should not occur here, and if they do, they're 'ours'
			context.deliver(action);
		}
		return true;
	}

	// must have lock and be called via executor
	boolean checkRedraw() {
		if (state != State.ACTIVE) return false; // we only draw in an active state
		return performRedraw();
	}

	boolean checkRedrawScheduled() {
		futureRedraw = null;
		futureRedrawTime = 0L;
		return checkRedraw();
	}

	// must have lock and be called via executor
	void checkConclusionAndOrLaunch() {
		if (manager.halting()) return; // manager already forcing conclusion and cannot launch activity while halting
		boolean mayConclude = state != State.CONSTRUCTED; // we can't conclude in a constructed state
		boolean mayLaunch = state.compareTo(State.OPEN) >= 0; // can only launch activities when open
		logger.debug().message("activity may conclude: {}, may launch: {}").values(mayConclude, mayLaunch).log();
		DataInput conclusion = mayConclude ? context.requestedConclusion() : null;
		ActivityLaunch launch = mayLaunch ? context.requestedLaunch() : null;
		logger.debug().message("activity conclusion: {}, launch: {}").values(conclusion, launch).log();
		if (launch != null) {
			// first we need to identify the launcher, since launches are relative to that
			ActivityDriver current = manager.locateDriver(launch.currentAncestorIds);
			ActivityMode mode = launch.mode;
			if (current == null) {
				// we can't find the activity that was current when activity requested (or it wasn't recorded)
				// this is okay if we're just launching detached
				if (mode == ActivityMode.DETATCH) {
					manager.launchActivityImpl(conclusion, manager.rootDriver, launch);
					return;
				} else {
					throw new UnsupportedOperationException("Unsupported launch mode without current activity: " + mode);
				}
			}
			// deal with special case of re-launching an activity
			if (launch.relaunch) {
				ActivityInstance sought = launch.activityInstance;
				ActivityDriver existing = current.ancestorMatching(d -> d.launch.activityInstance == sought);
				if (existing != null) {
					manager.concludeUpTo(current.top(), existing);
					State relaunchState = existing.launch.activityInstance.activity.relaunch(launch.input);
					if (relaunchState != null) { // relaunch not refused
						manager.setCurrentDriver(existing);
						manager.relaunchActivityImpl(relaunchState, launch);
						// EARLY RETURN - relaunch essentially blocks conclusion
						// either by concluding first or by replacing the same
						return;
					}
				}
				// if the existing activity refused the relaunch, or 'disappeared' since an activity was identified for relaunch
				// then we have to manufacture a new launch with the same parameters, except that the mode is not a relaunch.
				launch = launch.notRelaunch(context::instantiateActivity).orElse(null);
				if (launch == null) { // could not instantiate activity
					//TODO log
					// EARLY RETURN
					return;
				}
				// fall through
			}
			// identify the parent driver
			if (mode == ActivityMode.DETATCH) {
				manager.launchActivityImpl(conclusion, manager.rootDriver, launch);
			} else {
				ActivityDriver driver;
				if (mode.top()) {
					driver = this;
				} else if (mode.current()) {
					driver = current;
				} else {
					logger.warning().message("invalid launch state").log();
					driver = null;
				}
				// bring the parent to the top
				if (driver == null) {
					// the parent couldn't be found don't launch
					return;
				}
				manager.concludeUpTo(current.top(), driver);
				// prepare conclusion of (possibly new) top activity
				if (mode.replace()) {
					// even if there is a conclusion, don't use it, because we're replacing it
					conclusion = DataInput.NULL;
				} else if (driver != this) {
					// the supplied conclusion no longer applies
					conclusion = null;
				}
				// launch above parent - possibly concluding and replacing it
				manager.launchActivityImpl(conclusion, driver, launch);
			}
		} else if (conclusion != null) {
			manager.concludeActivityImpl(this, conclusion);
		}
	}

	void returnData(ActivityLaunch requestedLaunch, DataInput returnedData) {
		ActivityResponse response = new ActivityResponse(requestedLaunch.requestId, returnedData);
		if (requestedLaunch.respondToComponent != null) {
			componentResponse = new Display.ComponentResponse(requestedLaunch.respondToComponent, response);
		} else {
			activityResponses.add(response);
		}
	}

	ActivityInstance existingActivity(Identity activityIdentity) {
		ActivityDriver driver = ancestorMatching(d -> activityIdentity.equals(d.launch.activityInstance.identity));
		return driver == null ? null : driver.launch.activityInstance;
	}

	// must have lock and be called via executor
	ActivityDriver successorById(int instanceId) {
		for (ActivityDriver s : successors) {
			if (s.instanceId == instanceId) return s;
		}
		return null;
	}

	// must have lock and be called via executor
	boolean hasSuccessors() {
		return !successors.isEmpty();
	}

	// must have lock and be called via executor
	ActivityDriver top() {
		ActivityDriver driver = this;
		while (!driver.successors.isEmpty()) {
			driver = driver.successors.get(successors.size() - 1);
		}
		return driver;
	}

	ActivityDriver below() {
		if (previous == null) return null;
		List<ActivityDriver> list = previous.successors;
		int index = list.indexOf(this);
		if (index < 0) throw new IllegalStateException("activity driver " + instanceId + " not found in sucessors of " + previous.instanceId);
		index ++;
		return index == list.size() ? previous : list.get(index).top();
	}

	void makeTop() {
		ActivityDriver driver = this;
		do {
			ActivityDriver previous = driver.previous;
			List<ActivityDriver> list = previous.successors;
			boolean removed = list.remove(driver);
			assert removed;
			list.add(driver);
			driver = previous;
		} while (driver != null);
	}

	private void initActivity() {
		checkState(State.CONSTRUCTED);
		if (previous != null) previous.attach(this);
		context = new ActivityContext(this);
		ActivityContext.setCurrent(context);
		try {
			launch.activityInstance.activity.init();
		} catch (RuntimeException e) {
			recordActivityException(e);
		} finally {
			ActivityContext.clearCurrent();
			setState(State.INITIALIZED);
		}
	}

	private void openActivity() {
		checkState(State.INITIALIZED);
		ActivityContext.setCurrent(context);
		boolean actConcluded = false;
		try {
			launch.activityInstance.activity.open(savedState);
			actConcluded = true;
		} catch (RuntimeException e) {
			recordActivityException(e);
		} finally {
			ActivityContext.clearCurrent();
			// activity may have failed to initialize, or may not have defined a display
			// so provide a default display for something to render
			if (display == null) display = context.configureDisplay().flavor(actConcluded ? Flavor.GENERIC : Flavor.ERROR).layoutDisplay(Layout.single());
			display.finish();
			savedState = null;
			setState(State.OPEN);
		}
	}

	private void activateActivity() {
		checkState(State.OPEN);
		display.captureResources();
		if ( context.deviceSpec().screenSupportsAmbience() ) {
			manager.screen.ambience( display.visualSpec().theme.ambientColor );
		}
		if (componentResponse != null) {
			display.deliver(componentResponse);
			componentResponse = null;
		}
		ActivityContext.setCurrent(context);
		context.active = true;
		try {
			launch.activityInstance.activity.activate();
		} catch (RuntimeException e) {
			recordActivityException(e);
		} finally {
			ActivityContext.clearCurrent();
			setState(State.ACTIVE);
			eventState.resuming();
		}
		performRedraw();
	}

	private void passivateActivity() {
		checkState(State.ACTIVE);
		ActivityContext.setCurrent(context);
		try {
			launch.activityInstance.activity.passivate();
		} catch (RuntimeException e) {
			recordActivityException(e);
		} finally {
			ActivityContext.clearCurrent();
			setState(State.OPEN);
			if (futureRedraw != null) {
				futureRedraw.cancel(true);
				futureRedraw = null;
				futureRedrawTime = 0L;
			}
			display.releaseResources();
			context.active = false;
		}
	}

	private void closeActivity() {
		checkState(State.OPEN);
		DataOutput output = new DataOutput();
		ActivityContext.setCurrent(context);
		try {
			launch.activityInstance.activity.close(output);
		} catch (RuntimeException e) {
			recordActivityException(e);
		} finally {
			ActivityContext.clearCurrent();
			savedState = output.toInput();
			display.destroy();
			display = null;
			setState(State.INITIALIZED);
		}
	}

	private void destroyActivity() {
		checkState(State.INITIALIZED);
		assert successors.isEmpty();
		if (previous != null) previous.detach(this);
		ActivityContext.setCurrent(context);
		try {
			launch.activityInstance.activity.destroy();
		} catch (RuntimeException e) {
			recordActivityException(e);
		} finally {
			ActivityContext.clearCurrent();
			context = null;
			setState(State.CONSTRUCTED);
		}
	}

	// must have lock and be called via executor
	private void checkState(State expected) {
		if (state != expected) throw new IllegalStateException("expected state " + expected + " but state was " + state);
	}

	// must have lock and be called via executor
	private void setState(State state) {
		if (Math.abs(this.state.ordinal() - state.ordinal()) != 1) throw new IllegalStateException();
		if (this.state == State.ACTIVE) { // we need to close-up events
			eventState.cleanup().forEach(context::deliver);
		}
		this.state = state;
		if (state == State.ACTIVE) { // check if we need to inform script session
			ScriptSession scriptSession = manager.face.scriptSession;
			if (scriptSession != null) {
				ActivityDetails activityDetails = context.activityDetails(); // may be null for system activities
				Details details = activityDetails == null ? null : activityDetails.details;
				scriptSession.activityActivated(instanceId, details);
			}
		}
	}

	// must have lock and be called via executor
	private void attach(ActivityDriver successor) {
		assert successor != null;
		successors.add(successor);
		logSuccessors();
	}

	// must have lock and be called via executor
	private void detach(ActivityDriver successor) {
		assert successor != null;
		boolean removed = successors.remove(successor);
		assert removed;
		logSuccessors();
	}

	private void logSuccessors() {
		if (!logger.isDebugLogged()) return;
		int[] successorIds = successors.stream().mapToInt(ad -> ad.instanceId).toArray();
		logger.debug().message("activity driver {} has successors {}").values(instanceId, successorIds).log();
	}

	// must have lock and be called via executor
	private void recordActivityException(RuntimeException e) {
		manager.recordException("Exception on activity: " + launch.activityInstance.activity + " during " + state, e);
	}

	private boolean performRedraw() {
		if (manager.halting()) return false;
		boolean result = display.render(manager.screen);
		scheduleDelayedRedraw();
		return result;
	}

	private void scheduleDelayedRedraw() {
		if (state == State.ACTIVE) {
			long now = System.currentTimeMillis();
			long delay = display.nextRedrawDelay(now);
			if (delay == 0L) {
				manager.perform(this::checkRedraw);
			} else if (delay < Long.MAX_VALUE) {
				long then = now + delay;
				if (futureRedraw != null && futureRedrawTime > then) {
					futureRedraw.cancel(true);
					futureRedraw = null;
				}
				if (futureRedraw == null) {
					futureRedraw = manager.schedule(this::checkRedrawScheduled, delay);
					futureRedrawTime = then;
				}
			} else {
				/* nothing to do - nothing animating */
			}
		}
	}

	private ActivityDriver ancestorWithActivityIdentity(Identity actIdentity) {
		ActivityDriver driver = this;
		do {
			if (actIdentity.equals(driver.launch.activityInstance.identity)) break;
			driver = driver.previous;
		} while (driver != null);
		return driver;
	}

	private ActivityDriver ancestorMatching(Predicate<ActivityDriver> matcher) {
		ActivityDriver driver = this;
		do {
			if (matcher.test(driver)) break;
			driver = driver.previous;
		} while (driver != null);
		return driver;
	}
}
