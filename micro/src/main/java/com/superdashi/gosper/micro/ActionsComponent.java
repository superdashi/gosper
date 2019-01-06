package com.superdashi.gosper.micro;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.micro.Display.Situation;
import com.tomgibara.bits.Bits;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntRect;

public abstract class ActionsComponent extends Component {

	static final ActionModel[] NO_ACTIONS = {};

	private final Focusing focusing = new Focusing() {

		@Override
		//TODO could memoize
		public List<IntRect> focusableAreas() {
			//TODO buttonCount won't be known until rendering
			int buttonCount = lastActions.length;
			if (buttonCount == 0) return Collections.emptyList();
			IntRect[] areas = areas(buttonCount);
			return Collections.unmodifiableList(Arrays.asList(areas));
		}

		@Override
		public void cedeFocus() {
			// remember the active index
			dirty(activeIndex);
		}

		@Override
		public void receiveFocus(int areaIndex) {
			if (areaIndex < 0) {
				// no op - leave active index unchanged
			} else {
				activeIndex = validIndex(areaIndex);
			}
			dirty(activeIndex);
		}

		@Override
		public IntRect focusArea() {
			return activeIndex < 0 ? bounds : area(activeIndex);
		}

	};

	private final Pointing pointing = new Pointing() {

		@Override
		public List<IntRect> clickableAreas() {
			//TODO share impl with focusing
			//TODO buttonCount won't be known until rendering
			int buttonCount = lastActions.length;
			if (buttonCount == 0) return Collections.emptyList();
			IntRect[] areas = areas(buttonCount);
			return Collections.unmodifiableList(Arrays.asList(areas));
		}

		@Override
		public boolean clicked(int areaIndex, IntCoords coords) {
			//TODO this is broken really: activeAction needs to be in sync
			situation.focus();
			int clickedIndex = validIndex(areaIndex);
			if (clickedIndex != activeIndex) {
				dirty(activeIndex);
				activeIndex = clickedIndex;
				activeAction(lastActions[activeIndex]);
				dirty(activeIndex);
			}
			activate();
			return true;
		}

	};

	// these are derived from the situation
	IntRect bounds;
	int maxDisplayCount; // must be calculated during construction, based on space available in display

	// these are calculated/updated during redraw
	// the number of actions displayed to the user
	private int displayCount = -1; // set to -1 to force redraw on initial zero sized actions
	private long lastMod = -1L;
	private Set<Integer> dirtyRowsSet = Bits.noBits().ones().asSet();
	private ActionModel activeAction = null;
	private ActionModel[] lastActions = NO_ACTIONS;

	// this can be mutated/exchanged by the application between redraws
	private ActionsModel model;
	// this is kept consistent with the model
	private int activeIndex = -1;

	// methods

	public boolean nextAction() {
		return activeIndex(activeIndex + 1);
	}

	public boolean previousAction() {
		return activeIndex(activeIndex - 1);
	}

	public Optional<ActionModel> activeAction() {
		return Optional.ofNullable(activeAction);
	}

	public int activeIndex() {
		return activeIndex;
	}

	public boolean activeIndex(int index) {
		//TODO how do we know correct button count before rendering?
		if (index < 0 || index >= displayCount || index == activeIndex) return false;
		dirty(activeIndex);
		activeIndex = index;
		dirty(activeIndex);
		situation.requestRedrawNow();
		return true;
	}

	public ActionsModel model() {
		return model;
	}

	public void model(ActionsModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		if (!this.model.equals(model)) {
			this.model = model;
			dirtyRowsSet = Bits.store(model.count()).ones().asSet();
			lastMod = -1L;
			if (activeAction != null) {
				int index = model.modelIndex(activeAction);
				if (index == -1) {
					activeAction(null);
				} else {
					activeIndex = index;
				}
			}
		}
	}

	// convenience method
	public ActionsModel actions(Action... actions) {
		if (actions == null) throw new IllegalArgumentException("null actions");
		ActionsModel model = situation.models().actionsModel(actions);
		model(model);
		return model;
	}

	// convenience method
	public Action[] actions() {
		return model.actions();
	}

	// private helper methods

	private void dirty(int index) {
		if (index >= 0 && index < lastActions.length) dirtyRowsSet.add(index);
	}

	private int validIndex(int index) {
		return validIndex(index, lastActions.length);
	}

	private int validIndex(int index, int numberOfActions) {
		int max = numberOfActions - 1;
		if (index < 0) return Math.min(0, max);
		if (index >= numberOfActions) return max;
		return index;
	}

	// component methods

	@Override
	void situate(Situation situation) {
		this.situation = situation;
		model = situation.models().actionsModel();
	}

	@Override
	void place(Place place, int z) {
		bounds = place.innerBounds;
		maxDisplayCount = computeMaxActionCount();
	}

	@Override
	Changes changes() {
		if (maxDisplayCount == 0) return Changes.NONE; // no space to render
		if (lastMod != model.mutations()) {
			if (composition() != Composition.MASK) return Changes.CONTENT; // we only care about shape/content distinction if masked
			int newButtonCount = Math.min(maxDisplayCount, model.count(true));
			if (newButtonCount != displayCount) return Changes.SHAPE; // more/fewer button outlines
			return Changes.CONTENT; // visible buttons may have changed
		}
		if (!dirtyRowsSet.isEmpty()) return Changes.CONTENT; // buttons have changed
		return Changes.NONE;
	}

	// inner classes

	class RenderData {

		// set on construction
		final int oldDisplayCount = ActionsComponent.this.displayCount;

		// set before rendering
 		final int displayCount;
 		final long lastMod;
 		final ActionModel[] actions;
 		final int activeIndex;

 		// updated during rendering
 		int index;
 		ActionModel action;
 		boolean redraw;
 		boolean active;
 		boolean enabled;

 		// available after rendering
 		ActionModel activeAction = null;

 		RenderData() {
			ActionsModel mas = model.immutableCopy();
			//TODO tidy up
			actions = mas.actionsWithAvailability(true).toArray(new ActionModel[mas.count(true)]);
			lastMod = mas.mutations();
			displayCount = Math.min(maxDisplayCount, mas.count(true));
			activeIndex = validIndex(ActionsComponent.this.activeIndex, actions.length);
		}

		void render(Runnable r) {
			// render loop
			ActionModel activeAction = null;
			for (index = 0; index < actions.length; index++) {
				action = actions[index];
				ActionModel lastAction = index>= lastActions.length ? null : lastActions[index];
				redraw = dirtyRowsSet.contains(index) || !action.equals(lastAction);
				active = index == activeIndex;
				enabled = action.enabled();
				if (active) activeAction = action;
				r.run();
			}
			// post render vars
			this.activeAction = activeAction;
			// update parent
			activeAction(activeAction);
			ActionsComponent.this.activeIndex = activeIndex;
			ActionsComponent.this.lastMod = lastMod;
			ActionsComponent.this.displayCount = displayCount;
			dirtyRowsSet.clear();
			lastActions = actions;
		}

	}

	@Override
	public Optional<Focusing> focusing() {
		return Optional.of(focusing);
	}

	@Override
	Optional<Pointing> pointing() {
		return Optional.of(pointing);
	}

	// package scoped

	// guaranteed to be called from constructor, can assume display and bounds have been set
	abstract int computeMaxActionCount();

	abstract IntRect[] areas(int displayCount);

	abstract IntRect area(int index);

	boolean activate() {
		if (
				activeAction == null ||
				!activeAction.enabled() ||
				!model.modelAvailable(activeAction)
			) return false;
		situation.instigate(activeAction.action());
		return true;
	}

	// may be called prior to construction
	Eventing verticalEventing() {
		return new Eventing() {

			@Override
			public EventMask eventMask() {
				return EventMask.anyKeyDown();
			}

			@Override
			public boolean handleEvent(Event event) {
				switch (event.key) {
				case Event.KEY_UP:
					return previousAction();
				case Event.KEY_DOWN:
					return nextAction();
				case Event.KEY_CONFIRM:
					return activate();
					default:
						return false;
				}
			}
		};
	}

	// may be called prior to construction
	Eventing horizontalEventing() {
		return new Eventing() {

			@Override
			public EventMask eventMask() {
				return EventMask.anyKeyDown();
			}

			@Override
			public boolean handleEvent(Event event) {
				switch (event.key) {
				case Event.KEY_LEFT:
					return previousAction();
				case Event.KEY_RIGHT:
					return nextAction();
				case Event.KEY_CONFIRM:
					return activate();
					default:
						return false;
				}
			}
		};
	}

	// private methods

	private void activeAction(ActionModel activeAction) {
		if (activeAction == this.activeAction) return;
		this.activeAction = activeAction;
		situation.currentAction(activeAction == null ? null : activeAction.action());
	}
}
