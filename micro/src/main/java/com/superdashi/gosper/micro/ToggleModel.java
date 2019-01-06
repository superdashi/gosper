package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Item;

//TODO consider adding support for gosper:active-label and gosper:inactive-label
public class ToggleModel extends Model {

	// statics

	public enum State {
		INACTIVE(Action.ID_DEACTIVATE_TOGGLE),
		NEUTRAL (Action.ID_NEUTRALIZE_TOGGLE),
		ACTIVE  (Action.ID_ACTIVATE_TOGGLE  );

		final String defaultActionId;

		private State(String defaultActionId) {
			this.defaultActionId = defaultActionId;
		}

		public static State forBoolean(Boolean bool) {
			if (bool == null) return State.NEUTRAL;
			return bool ? State.ACTIVE : INACTIVE;
		}
	}

	public enum Neutrality {
		DEFAULTS_TO_ACTIVE(false),
		DEFAULTS_TO_INACTIVE(false),
		ALWAYS_AVAILABLE(true),
		INTIAL_ONLY(false);

		final boolean allowsTransitionToNeutral;

		private Neutrality(boolean allowsTransitionToNeutral) {
			this.allowsTransitionToNeutral = allowsTransitionToNeutral;
		}
		@SuppressWarnings("incomplete-switch")
		State guard(State state) {
			if (state == State.NEUTRAL) {
				switch (this) {
				case DEFAULTS_TO_ACTIVE   : return State.ACTIVE;
				case DEFAULTS_TO_INACTIVE : return State.INACTIVE;
				}
			}
			return state;
		}

	}
	// fields

	private final Mutations mutations;

	private ItemModel info;
	private State state = State.NEUTRAL;
	private Neutrality neutrality = Neutrality.INTIAL_ONLY;

	// a snapshotted default model
	ToggleModel(Item item) {
		super((ActivityContext) null);
		info = new ItemModel(item);
		mutations = null;
	}

	ToggleModel(ActivityContext context, Mutations mutations, Item info) {
		super(context);
		this.mutations = mutations;
		this.info = context.models().itemModel(info);
	}

	// accessors

	public Item info() {
		return info.item;
	}

	public void info(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
		if (info.item.equals(item)) return;
		info = models().itemModel(item, mutations);
		mutations.count ++;
		requestRedraw();
	}

	public State state() {
		return state;
	}

	public void state(State state) {
		if (state == null) throw new IllegalArgumentException("null state");
		state = neutrality.guard(state);
		if (state == this.state) return;
		this.state = state;
		mutations.count ++;
		requestRedraw();
	}

	public Neutrality neutrality() {
		return neutrality;
	}

	public void neutrality(Neutrality neutrality) {
		if (neutrality == null) throw new IllegalArgumentException("null neutrality");
		if (neutrality == this.neutrality) return;
		this.neutrality = neutrality;
		state = neutrality.guard(state);
		mutations.count ++;
		requestRedraw();
	}

	// package scoped methods

	long revision() {
		return mutations.count;
	}

}
