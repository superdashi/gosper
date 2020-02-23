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

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.Keyboard;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.micro.Component.Changes;
import com.superdashi.gosper.micro.Component.Eventing;
import com.superdashi.gosper.micro.Component.Pointing;
import com.superdashi.gosper.micro.Layout.Sizer;
import com.superdashi.gosper.scripting.ScriptSession;
import com.superdashi.gosper.studio.Composition;
import com.superdashi.gosper.studio.Pane;
import com.superdashi.gosper.studio.Panel;
import com.superdashi.gosper.studio.Studio;
import com.superdashi.gosper.studio.SurfacePool;
import com.tomgibara.fundament.Producer;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntDir;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntRectNavigator;
import com.tomgibara.intgeom.IntRectNavigator.Algorithm;
import com.tomgibara.intgeom.IntRectNavigator.TaggedRect;

//TODO rename to Components?
public final class Display {

	private static final boolean DEBUG_DISPLAY = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.micro.Display.DEBUG"));
	private static final int MAX_ACTIONS = 128;

	enum FocusDir {

		LEFT     (Optional.of(IntDir.LESS_X)),
		RIGHT    (Optional.of(IntDir.MORE_X)),
		UP       (Optional.of(IntDir.LESS_Y)),
		DOWN     (Optional.of(IntDir.MORE_Y)),
		FORWARD  (Optional.empty()          ),
		BACKWARD (Optional.empty()          );

		public final Optional<IntDir> intDir;

		private FocusDir(Optional<IntDir> intDir) {
			this.intDir = intDir;
		}

	}

	public static final int MAX_Z = 10;
	public static final int MIN_Z = 0;

	private static final long NO_TARGET_TIME = Long.MAX_VALUE;

	private static final int Z_BAR = MAX_Z;
	private static final int Z_BACKGROUND = MIN_Z;
	private static final int Z_SCROLLBAR = MIN_Z + 1;
	private static final int Z_DEFAULT = MIN_Z + 1;

	private final Studio studio;
	private final SurfacePool surfacePool;
	private final Background background;
	private final Composition composition;
	private final Pane backgroundPane;
	private final Panel sharedPanel;
	private final Keyboard keyboard; // needed to usefully default button visibility
	private final VisualSpec spec;
	private final ActivityContext activityContext;
	private final List<Situation> situations = new ArrayList<>();
	private final Map<Location, Situation> locations;
	private final ScriptSession scriptSession;

	//state only required until finished
	private Params params;

	// 'fixed' components
	private Situation bar;
	private Situation scrollbar;

	private ActionsModel actions = ActionsModel.none();
	// records the current action selected by the user
	private final ActionModel selectedAction;

	private boolean disordered = true;
	private Situation focused = null;
	private Situation pressed = null; // if MT supported, would need a keyed map of these
	private boolean redrawRequested = true; // initially, must require a redraw
	private long lastRenderCall = -1L;
	private boolean dirtySurfaces = false; // when compositor resources are captured, it's necessary to force rendering

	private final List<Action> componentActions = new ArrayList<>();

	// lazily created
	private CommonMark commonMark = null;

	//TODO need a way to defer the background construction so that a place can be supplied
	Display(Studio studio, SurfacePool surfacePool, Keyboard keyboard, VisualSpec spec, ActivityContext activityContext, DisplayConfiguration config, Layout layout, Background background, ScriptSession scriptSession) {
		this.studio = studio;
		this.surfacePool = surfacePool;
		this.background = background;
		composition = studio.createComposition(surfacePool);
		IntDimensions dimensions = spec.qualifier.dimensions;
		sharedPanel = composition.createPanel(dimensions, false);
		if (background.blank()) {
			backgroundPane = null;
		} else {
			backgroundPane = composition.createPanel(dimensions, background.opaque()).createEntirePane(IntCoords.ORIGIN, Z_BACKGROUND);

		}
		this.keyboard = keyboard;
		this.spec = spec;
		this.activityContext = activityContext;
		this.scriptSession = scriptSession;
		selectedAction = activityContext.models().actionModel(Action.noAction());

		IntRect bounds = spec.bounds;
		Style style = spec.styles.defaultPlaceStyle;

		// create the standard places and adjust the remaining bounds

		if (config.hasTopBar) {
			Bar component = new Bar(!keyboard.keySet.containsKey(Event.KEY_CANCEL));
			bar = new Situation(this, component, Location.topbar, style);
			updateBar();
			situations.add(bar);
			Place place = new Place(Location.topbar, bounds.resized(IntDir.MORE_Y, spec.metrics.barHeight), style);
			bar.place(place, Z_BAR);
			bounds = bounds.resized(IntDir.LESS_Y, bounds.height() - spec.metrics.barHeight);
		}

		if (config.hasScrollbar) {
			scrollbar = new Situation(this, new Scrollbar(), Location.scrollbar, style);
			situations.add(scrollbar);
			Place place = new Place(Location.scrollbar, bounds.resized(IntDir.LESS_X, spec.metrics.scrollbarWidth), style);
			scrollbar.place(place, Z_SCROLLBAR);
			bounds = bounds.resized(IntDir.MORE_X, bounds.width() - spec.metrics.scrollbarWidth);
		}

		// cache data for finishing
		this.params = new Params(spec, layout, new Constraints(bounds));
		locations = this.params.createLocations();
	}

	// methods for activities to use

	public VisualSpec visualSpec() {
		return spec;
	}

	public List<Location> locations() {
		//TODO make efficient?
		return new ArrayList<>(locations.keySet());
	}

	public CommonMark commonMark() {
		return commonMark == null ? commonMark = CommonMark.defaultFor(spec.theme) : commonMark;
	}

	public void actionsModel(ActionsModel actions) {
		checkNotFinished();
		if (actions == null) throw new IllegalArgumentException("null actions");
		//TODO need to filter by device support
		this.actions = actions;
	}

	public ActionsModel actionsData(Action... actions) {
		checkNotFinished();
		//TODO need to filter by device support
		return this.actions = activityContext.models().actionsModel(actions);
	}

	public Optional<Bar> bar() {
		checkNotFinished();
		return bar == null ? Optional.empty() : Optional.of((Bar) bar.component);
	}

	public Optional<Scrollbar> scrollbar() {
		checkNotFinished();
		return scrollbar == null ? Optional.empty() : Optional.of((Scrollbar) scrollbar.component);
	}

	public TimeIndicator addTimeIndicator(Clock clock) {
		if (clock == null) throw new IllegalArgumentException("null clock");
		return addIndicator(new TimeIndicator(spec, clock));
	}

	public TimeIndicator addTimeIndicator() {
		Clock clock = activityContext.environment().minuteClock();
		return addIndicator(new TimeIndicator(spec, clock));
	}

	public Table addTable(Location location, DisplayColumns columns) {
		if (columns == null) throw new IllegalArgumentException("null columns");
		return addComponent(location, () -> new Table(columns));
	}

	public Table addTable(DisplayColumns columns) {
		if (columns == null) throw new IllegalArgumentException("null columns");
		return addComponent(() -> new Table(columns));
	}

	//TODO should provide ability to specify place in layout
	public Toggle addToggle(Location location) {
		return addComponent(location, Toggle::new);
	}

	public Toggle addToggle() {
		return addComponent(Toggle::new);
	}

	// defaults model with keyboard inaccessible actions
	public LabelButtons addLabelButtons(Location location) {
		return addActionsComponent(location, LabelButtons::new);
	}

	// defaults model with keyboard inaccessible actions
	public LabelButtons addLabelButtons() {
		return addActionsComponent(LabelButtons::new);
	}

	// defaults model with keyboard inaccessible actions
	public SymbolButtons addSymbolButtons(Location location) {
		return addActionsComponent(location, SymbolButtons::new);
	}
	// defaults model with keyboard inaccessible actions
	public SymbolButtons addSymbolButtons() {
		return addActionsComponent(SymbolButtons::new);
	}

	// defaults model with keyboard inaccessible actions
	public Icons addIcons(Location location) {
		return addActionsComponent(location, Icons::new);
	}

	// defaults model with keyboard inaccessible actions
	public Icons addIcons() {
		return addActionsComponent(Icons::new);
	}

	public Form addForm(Location location) {
		return addComponent(location, Form::new);
	}

	public Form addForm() {
		return addComponent(Form::new);
	}

	public Document addDocument(Location location) {
		return addComponent(location, Document::new);
	}

	public Document addDocument() {
		return addComponent(Document::new);
	}

	//TODO introduce an addPictureCard method
	public Card addCard(Location location) {
		return addComponent(location, Card::new);
	}

	//TODO introduce an addPictureCard method
	public Card addCard() {
		return addComponent(Card::new);
	}

	public Checkbox addCheckbox(Location location) {
		return addComponent(location, Checkbox::new);
	}

	public Checkbox addCheckbox() {
		return addComponent(Checkbox::new);
	}

	public Pursuit addPursuit(Location location) {
		return addComponent(location, Pursuit::new);
	}

	public Pursuit addPursuit() {
		return addComponent(Pursuit::new);
	}

	public Active addActive(Location location) {
		return addComponent(location, Active::new);
	}

	public Active addActive() {
		return addComponent(Active::new);
	}

	public PrimaryButton addPrimaryButton(Location location) {
		return addComponent(location, PrimaryButton::new);
	}

	public PrimaryButton addPrimaryButton() {
		return addComponent(PrimaryButton::new);
	}

	public Badges badges() {
		return new Badges(spec, activityContext.models());
	}

	// platform only components

	KeyboardComponent addKeyboard(Location location, ItemModel info) {
		checkNotFinished();
		KeyboardComponent keyboard = new KeyboardComponent(info);
		addComponent(keyboard, location);
		return keyboard;
	}

	// methods for framework to use

	void updateBar() {
		if (this.bar == null) return; // no bar to update
		Bar bar = (Bar) this.bar.component;
		bar.item( activityContext.titleItem() );
	}

	//TODO can this be eliminated? - still being used by driver
	void requestRedraw() {
		redrawRequested = true;
	}

//	// max value indicates none
//	long nextRedraw() {
//		if (redrawRequested) return 0L;
//		long earliest = NO_TARGET_TIME;
//		for (Situation situation : situations) {
//			earliest = Math.min(earliest, situation.targetTime);
//		}
//		return earliest;
//	}

	long nextRedrawDelay(long now) {
		if (redrawRequested) return 0L;
		long earliest = NO_TARGET_TIME;
		for (Situation situation : situations) {
			earliest = Math.min(earliest, situation.targetTime);
		}
		long delay = earliest == NO_TARGET_TIME ? NO_TARGET_TIME : Math.max(0L, earliest - now);
		if (DEBUG_DISPLAY) Debug.logging().message("computed next redraw delay to be {}ms").values(delay).log();
		return delay;
	}

	boolean isDirty() {
		//TODO need a better algorithm for this
		for (Situation sit : situations) {
			if (sit.component.changes() != Changes.NONE) {
				if (DEBUG_DISPLAY) Debug.logging().message("dirty state triggered at {} by {}").values(sit.location.name, sit.component.getClass().getName()).log();
				return true;
			}
		}
		return false;
	}

	void finish() {
		// gather finishing state
		Places places = params.computePlaces();
		params = null; // not needed once finished

		// place each component
		for (Situation sit : situations) {
			if (sit.isPlaced()) continue;
			Location location = sit.location;
			Place place = places.placeAtLocation(location);
			sit.place(place, Z_DEFAULT);
		}

		//TODO should allow activity to choose focus order
		if (focused == null) {
			Situation deferred = null;
			for (Situation sit : situations) {
				if (sit.supportsFocusing) {
					if (sit.component.focusing().get().focusableByDefault()) {
						setFocus(sit);
						deferred = null;
						break;
					}
					if (deferred == null) {
						deferred = sit;
					}
				}
			}
			if (deferred != null) setFocus(deferred);
		}

		// automatically link scrollbar to largest scrollable component
		if (scrollbar != null) {
			Scrollbar scr = (Scrollbar) scrollbar.component;
			if (scr.model() == null) {
				situations.stream()
					.map(s -> s.component)
					.filter(c -> c.scrolling().isPresent())
					.max((a,b) -> a.bounds().dimensions().area() - b.bounds().dimensions().area())
					.map(c -> c.scrolling().get().scrollbarModel())
					.ifPresent(m -> scr.model(m));
			}
		}

		// discard any un-reused surfaces
		surfacePool.disposeOfRemainingSurfaces();
	}

	void releaseResources() {
		composition.releaseResources();
	}

	void captureResources() {
		composition.captureResources();
		dirtySurfaces = true;
	}

	List<Situation> situationsAt(IntCoords coords) {
		List<Situation> list = Collections.emptyList();
		if (spec.bounds.containsUnit(coords)) {
			ensureSituationsOrdered();
			for (Situation sit : situations) {
				if (sit.component.bounds().containsUnit(coords)) {
					switch (list.size()) {
					case 0 :
						list = Collections.singletonList(sit);
						break;
					case 1 :
						List<Situation> tmp = new ArrayList<>(2); // expected to be rare to have more than two matches (1 back, 1 comp)
						tmp.add(list.get(0));
						tmp.add(sit);
						list = tmp; break;
						default:
							list.add(sit);
					}
				}
			}
		}
		Collections.reverse(list);
		return list;
	}

	Component focus() {
		return focused == null ? null : focused.component;
	}

	boolean focus(Component component) {
		if (!component.focusable()) return false;
		Situation sit = situation(component);
		return sit != null && setFocus(sit);
	}

	boolean moveFocus(FocusDir focusDir) {
		if (focused == null) return false;

		Situation newFocus;

		if (focusDir.intDir.isPresent()) {
			// case where focus is based on layout
			IntDir dir = focusDir.intDir.get();
			//TODO can precompute this navigator on finish
			//TODO inefficient way of filtering
			IntRectNavigator<Situation> navigator =new IntRectNavigator<>(
					situations
					.stream()
					.filter(c -> c.component.focusing().isPresent())
					.map(c -> new TaggedRect<>(c.component.bounds(), c)).toArray(TaggedRect[]::new)
					);
			//TODO can refine this using areas
			newFocus = navigator.findFrom(focused.component.focusing().get().focusArea(), dir, Algorithm.PREFER_STRICT).map(t -> t.tag).orElse(null);
		} else {
			// case where focus is based on order
			// TODO
			newFocus = null;
		}
		if (newFocus == null) return false;
		setFocus(newFocus);
		return true;
	}

	// note: components take priority
	boolean handleEvent(Event event) {
		if (handleEventWithDisplay(event)) return true;
		if (handleEventWithComponent(event)) return true;
		if (handleEventWithAction(event)) return true;
		return false;
	}

	List<Action> takeComponentActions() {
		List<Action> list = new ArrayList<>(componentActions);
		componentActions.clear();
		return list;
	}

	boolean render(Screen screen) {
		lastRenderCall = System.currentTimeMillis();
		redrawRequested = false;
		recomputeTargetTimes();
		boolean requireComposite = render(dirtySurfaces);
		if (requireComposite) {
			screen.composite(composition);
			screen.update();
		}
		dirtySurfaces = false;
		return requireComposite;
	}

	boolean deliver(ComponentResponse response) {
		//TODO would be strongly preferable for backgrounds to have a location
		Optional<Component> optional = situations.stream().filter(s -> s.place != null).filter(s -> s.location.name.equals(response.locationName)).map(s -> s.component).findAny();
		if (!optional.isPresent()) return false;
		optional.get().receiveResponse(response.activityResponse);
		return true;
	}

	void destroy() {
		composition.destroy();
	}

	// private helper methods

	private void checkNotFinished() {
		if (params == null) throw new IllegalStateException("display configuration finished");
	}

	//TODO must check if location is already occupied
	private void checkLocation(Location location) {
		if (!locations.containsKey(location)) throw new IllegalArgumentException("location not in layout");
	}

	private Location nextFreeLocation() {
		for (Entry<Location, Situation> entry : locations.entrySet()) {
			if (entry.getValue() == null) return entry.getKey();
		}
		throw new IllegalStateException("no free location");
	}

	private <I extends Indicator> I addIndicator(I indicator) {
		bar().ifPresent(b -> b.addIndicator(indicator));
		return indicator;
	}


	private <C extends Component> C addComponent(Producer<C> producer) {
		checkNotFinished();
		Location location = nextFreeLocation();
		C comp = producer.produce();
		addComponent(comp, location);
		return comp;
	}

	private <C extends Component> C addComponent(Location location, Producer<C> producer) {
		checkNotFinished();
		checkLocation(location);
		C comp = producer.produce();
		addComponent(comp, location);
		return comp;
	}

	private void addComponent(Component c, Location location) {
		Situation situation = new Situation(this, c, location, null);
		situations.add(situation);
		locations.put(location, situation);
	}

	private <C extends ActionsComponent> C addActionsComponent(Location location, Producer<C> producer) {
		C comp = addComponent(location, producer);
		comp.model(actions.keyboardInaccessible(keyboard));
		return comp;
	}

	private <C extends ActionsComponent> C addActionsComponent(Producer<C> producer) {
		C comp = addComponent(producer);
		comp.model(actions.keyboardInaccessible(keyboard));
		return comp;
	}

	private Situation situation(Component c) {
		for (Situation sit : situations) {
			if (sit.component == c) return sit;
		}
		return null;
	}

	private void ensureSituationsOrdered() {
		if (disordered) {
			situations.sort(null);
			disordered = false;
		}
	}

	private boolean recordAction(Action action) {
		if (componentActions.size() >= MAX_ACTIONS) return false;
		componentActions.add(action);
		return true;
	}

	private boolean setFocus(Situation newFocus) {
		if (focused == newFocus) return false; // nothing to do
		if (focused != null) {
			focused.cedeFocus();
		}
		focused = newFocus;
		if (focused != null) {
			focused.receiveFocus();
		}
		return true;
	}

	private boolean setPressed(Situation newPress) {
		if (pressed == newPress) return false; // nothing to do
		if (pressed != null) {
			pressed.component.pointing().get().released();
		}
		pressed = newPress;
		if (pressed != null) {
			pressed.component.pointing().get().pressed();
		}
		return true;
	}

	private void selectedAction(Action selectedAction) {
		this.selectedAction.action(selectedAction == null ? Action.noAction() : selectedAction);
		if (scriptSession != null) {
			if (selectedAction == null) {
				scriptSession.actionAbsent();
			} else {
				scriptSession.actionSelected(selectedAction.id, selectedAction.item);
			}
		}
	}

	private void recomputeTargetTimes() {
		for (Situation sit : situations) {
			sit.advanceTargetTime(lastRenderCall);
		}
	}

	private boolean handleEventWithDisplay(Event event) {
		if (!event.isPoint() && !event.isMove()) return false;
		if (event.isMove() && !event.isDown() && !event.isRepeat()) {
			setPressed(null);
		}
		IntCoords coords = IntCoords.at(event.x, event.y);
		for (Situation sit : situationsAt(coords)) {
			//TODO will have other ways for components to handle these events that may take priority
			if (event.isMove() && event.isDown() && !event.isRepeat()) {
				boolean pressed = sit.press();
				if (pressed) return true;
			}
			if (event.isPoint()) {
				boolean clicked = sit.click(coords);
				if (clicked) return true;
			}
		}
		return false;
	}

	private boolean handleEventWithComponent(Event event) {
		if (focused == null) return false; // no component to receive event
		Optional<Eventing> opt = focused.component.eventing();
		if (!opt.isPresent()) return false; // component doesn't handle events
		Eventing eventing = opt.get();
		if (!eventing.eventMask().test(event)) return false; // component not wanting this event
		return eventing.handleEvent(event);
	}

	private boolean handleEventWithAction(Event event) {
		if (!event.isDown()) return false; // actions mapped to key-down
		Optional<ActionModel> opt = actions.modelForKey(event.key);
		if (!opt.isPresent()) return false; // no matching action
		return recordAction(opt.get().action());
	}

	private boolean render(boolean force) {
		if (DEBUG_DISPLAY) Debug.logging().message("performing render, forced: {}").values(force).log();
		ensureSituationsOrdered();
		boolean requireComposite = force;
		for (Situation sit : situations) {
			Component component = sit.component;
			if (!force && component.changes() == Changes.NONE) continue;
			if (DEBUG_DISPLAY) Debug.logging().message("rendering {} at {}").values(component.getClass().getName(), sit.location.name).log();
			component.render();
			requireComposite = true;
		}
		if (force) {
			//TODO this needs to be made possible without a panel when compositing
			backgroundPane.canvas().drawFrame(background.generate(backgroundPane.bounds()));
		}
		return requireComposite;
	}

	// static classes

	static class ComponentResponse {

		final String locationName;
		final ActivityResponse activityResponse;

		ComponentResponse(String location, ActivityResponse response) {
			this.locationName = location;
			this.activityResponse = response;
		}

	}

	private static class Params {

		private final Sizer sizer;
		private final Constraints constraints;

		private Params(VisualSpec spec, Layout layout, Constraints constraints) {
			this.constraints = constraints;
			sizer = layout.sizer(spec);
		}

		Map<Location, Situation> createLocations() {
			Location[] locations = sizer.locations();
			Map<Location, Situation> map = new LinkedHashMap<>(locations.length);
			for (Location location : locations) {
				map.put(location, null);
			}
			return map;
		}

		Places computePlaces() {
			// compute the layout
			//TODO must avoid throwing
			if (DEBUG_DISPLAY) Debug.logging().message("computing places based on {}").values(constraints).log();
			return sizer.computePlaces(constraints).orElseThrow(() -> new IllegalArgumentException("insufficient space for layout"));
		}
	}

	//TODO make top level?
	//TODO consider 'denormalizing' some fields, like the visual context, that are heavily referenced, place.innerBounds too perhaps
	static final class Situation implements Comparable<Situation>{

		private final Display display;
		final Component component;
		final Location location;
		final Style style;

		final boolean supportsFocusing;
		final boolean supportsPointing;

		// set during placement
		private Place place;
		private int z;

		// time in the future, if any, that a component wants to be rendered
		private long targetTime = NO_TARGET_TIME;
		private long renderPeriod = 0L;

		private Pane defaultPane = null;
		private Set<Panel> panels = null;

		// records the component's current action
		// this is sent to the context if the component has focus
		private Action currentAction;

		Situation(Display display, Component component, Location location, Style style) {
			this.display = display;
			this.component = component;
			this.location = location;
			this.style = style == null ? style = display.params.sizer.styleAtLocation(location).noMargins() : style;

			component.situate(this);

			supportsFocusing = component.focusing().isPresent();
			supportsPointing = component.pointing().isPresent();
		}

		// std java methods

		//TODO ideally should hide this behind a dedicated comparator
		@Override
		public int compareTo(Situation that) {
			return this.z - that.z;
		}

		@Override
		public String toString() {
			return component + "@location " + location.name;
		}

		// place methods

		boolean isPlaced() {
			return place != null;
		}

		private void place(Place place, int z) {
			this.place = place;
			this.z = z;
			component.place(place, z);
		}

		Place place() {
			if (place == null) throw new IllegalStateException("not placed");
			return place;
		}

		int z() {
			if (place == null) throw new IllegalStateException("not placed");
			return z;
		}

		void constrain(IntDimensions minimumSize) {
			display.params.constraints.setMinimumContentSize(location, minimumSize);
		}

		// contextual methods

		VisualSpec visualSpec() {
			return display.spec;
		}

		DeviceSpec deviceSpec() {
			return display.activityContext.deviceSpec();
		}

		// exposed so that components can create their own empty models
		Models models() {
			return display.activityContext.models();
		}

		// exposed so that components can create their own activity requests
		Activities activities() {
			return display.activityContext.activities();
		}

		// used for restricting some component methods to the active state
		boolean isActive() {
			return display.activityContext.active;
		}

		// way for components to style text via markdown
		CommonMark commonMark() {
			return display.commonMark();
		}

		// render resource methods

		boolean dirty() {
			return display.dirtySurfaces;
		}

		Pane defaultPane() {
			if (defaultPane == null) {
				IntRect bounds = component.bounds();
				defaultPane = display.sharedPanel.createPane(bounds, bounds.minimumCoords(), z);
				//defaultPane = display.composition.createPanel(bounds.dimensions(), false).createEntirePane(bounds.minimumCoords(), z);
			}
			return defaultPane;
		}

		Pane createPane(IntCoords coords, IntDimensions dimensions, boolean opaque) {
			Panel panel = display.composition.createPanel(dimensions, opaque);
			if (panels == null) panels = new HashSet<>();
			panels.add(panel);
			return panel.createPane(dimensions.toRect(), coords, z);
		}

		// action methods

		ActionModel selectedAction() {
			return display.selectedAction;
		}

		//TODO consider making an optional
		Action currentAction() {
			return currentAction;
		}

		void currentAction(Action action) {
			if (action == currentAction) return;
			this.currentAction = action;
			if (isFocused()) display.selectedAction(action);
		}

		void instigateCurrentAction() {
			if (currentAction != null) instigate(currentAction);
		}

		void instigate(Action action) {
			if (action == null) throw new IllegalArgumentException("null action");
			boolean okay = display.recordAction(action);
			//TODO would like something better
			if (!okay) throw new RuntimeException("actions flooded");
		}

		void concludeActivity() {
			display.activityContext.concludeActivity();
		}

		// status methods

		// called by component to request focus
		boolean focus() {
			return supportsFocusing && display.setFocus(this);
		}

		boolean isFocused() {
			return display.focused == this;
		}

		// called by display to give focus to component
		private void receiveFocus() {
			component.focusing().get().receiveFocus(-1);
			display.selectedAction(currentAction);
		}

		// called by display to remove focus from component
		private void cedeFocus() {
			component.focusing().get().cedeFocus();
			display.selectedAction(null);
		}

		private boolean press() {
			return supportsPointing && display.setPressed(this);
		}

		private boolean click(IntCoords coords) {
			if (!supportsPointing) return false;
			Pointing pointing = component.pointing().get();
			List<IntRect> areas = pointing.clickableAreas();
			if (areas.isEmpty()) {
				return pointing.clicked(-1, coords);
			}
			int index = 0;
			for (IntRect area : areas) {
				if (area.containsUnit(coords)) {
					return pointing.clicked(index, coords.relativeTo(area));
				}
				index ++;
			}
			return false;
		}

		private boolean release() {
			return display.pressed == this && display.setPressed(null);
		}

		// redraw methods

		void requestRedrawNow() {
			display.redrawRequested = true;
		}

		void requestRedrawAt(long time) {
			long now = System.currentTimeMillis();
			if (time <= now) {
				requestRedrawNow();
				targetTime = NO_TARGET_TIME;
			} else {
				targetTime = time;
			}
			if (DEBUG_DISPLAY) logRenderTimeChanges();
		}

		void requestRedrawPeriodicallyAt(long time, long period) {
			if (period <= 0L) throw new IllegalArgumentException("period not positive");
			targetTime = time;
			renderPeriod = period;
			advanceTargetTime(System.currentTimeMillis());
			if (DEBUG_DISPLAY) logRenderTimeChanges();
		}

		void requestRedrawDelayed(long delay) {
			if (delay < 0) throw new IllegalArgumentException("negative delay");
			if (delay == 0) {
				requestRedrawNow();
				targetTime = NO_TARGET_TIME;
			} else {
				targetTime = System.currentTimeMillis() + delay;
			}
			if (DEBUG_DISPLAY) logRenderTimeChanges();
		}

		void requestRedrawPeriodicallyDelayed(long delay, long period) {
			if (delay < 0) throw new IllegalArgumentException("negative delay");
			if (period <= 0L) throw new IllegalArgumentException("period not positive");
			targetTime = System.currentTimeMillis() + (delay == 0 ? period : delay);
			renderPeriod = period;
			if (DEBUG_DISPLAY) logRenderTimeChanges();
		}

		void cancelRequestedRedraw() {
			targetTime = NO_TARGET_TIME;
			renderPeriod = 0L;
			if (DEBUG_DISPLAY) logRenderTimeChanges();
		}

		// private helper methods

		private void advanceTargetTime(long now) {
			if (targetTime >= now) return; // target time not exceeded (or none)
			if (renderPeriod == 0L) {
				targetTime = NO_TARGET_TIME;
			} else {
				targetTime = now + renderPeriod - (now % renderPeriod);
			}
			if (DEBUG_DISPLAY) logRenderTimeChanges();
		}

		private void logRenderTimeChanges() {
			if (place == null) return; // not placed yet
			String locationName = place.location.name;
			if (targetTime == NO_TARGET_TIME) {
				Debug.logging().message("no target time at {}").values(locationName).log();
			} else {
				Debug.logging().message("target time at {} is {} with period {}").values(locationName, targetTime, renderPeriod).log();
			}
		}

	}

}
