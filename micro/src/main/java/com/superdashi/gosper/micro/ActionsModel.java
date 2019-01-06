package com.superdashi.gosper.micro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.Keyboard;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.Bits;
import com.tomgibara.fundament.Mutability;

// two actions are equal if they are certain to exhibit the same state
//TODO there are problems with null mutations not being properly supported
public final class ActionsModel extends Model implements Mutability<ActionsModel> {

	// statics

	private static final ActionsModel none = new ActionsModel(null, new ActionModel[] {}, Bits.noBits(), new Mutations(), Bits.noBits(), false);

	private static ActionModel[] deepCopy(Mutations mutations, ActionModel[] models) {
		ActionModel[] copies = new ActionModel[models.length];
		for (int i = 0; i < copies.length; i++) {
			copies[i] = models[i].copy(mutations);
		}
		return copies;
	}

	static ActionsModel none() {
		return none;
	}

	// array cannot be empty
	// note: array is not cloned
	// assumes all mutations are the same
	static ActionsModel over(ActivityContext context, ActionModel... actions) {
		assert context != null;
		BitStore available = Bits.store(actions.length);
		available.fill();
		return new ActionsModel(context, actions, available, actions[0].itemModel().mutations, Bits.oneBits(actions.length), true);
	}

	// fields

	//TODO kludge - we hold on to the context, so that we can use it for constructing views
	private final ActivityContext context;

	private final ActionModel[] actions;
	private final BitStore available;
	private final Mutations mutations;
	private final BitStore visible;
	private final StatefulAction[] statefulActions;
	private final boolean mutable;

	private int last = 0;

	// constructors

	private ActionsModel(ActivityContext context, ActionModel[] actions, BitStore available, Mutations mutations, BitStore visible, boolean mutable) {
		super(context);
		this.context = context;
		this.actions = actions;
		this.available = available;
		this.mutations = mutations;
		this.visible = visible;
		this.mutable = mutable;

		BitMatches ones = visible.ones();
		statefulActions = new StatefulAction[ones.count()];
		Positions positions = ones.positions();
		for (int i = 0; i < statefulActions.length; i++) {
			statefulActions[i] = new StatefulAction(i, positions.nextPosition());
		}
	}

	// methods

	public int count() {
		return statefulActions.length;
	}

	public  Action[] actions() {
		Action[] actions = new Action[statefulActions.length];
		for (int i = 0; i < actions.length; i++) {
			actions[i] = statefulActions[i].action.action();
		}
		return actions;
	}

	public ActionModel[] allPossibleModels() {
		ActionModel[] models = new ActionModel[statefulActions.length];
		for (int i = 0; i < models.length; i++) {
			models[i] = statefulActions[i].action;
		}
		return models;
	}

	public int modelIndex(ActionModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		return ordinalOf(model);
	}

	public boolean modelAvailable(ActionModel model) {
		int index = modelIndex(model);
		return index != -1 && available.getBit(index);
	}

	public ActionModel modelWithId(String id) {
		if (id == null) throw new IllegalArgumentException("null id");
		return statefulActions[certainOrdinalOf(id)].action;
	}

	public Optional<ActionModel> possibleModelWithId(String id) {
		if (id == null) throw new IllegalArgumentException("null id");
		int index = ordinalOf(id);
		return index == -1 ? Optional.empty() : Optional.of(statefulActions[index].action);
	}

	public boolean modelAvailable(String id) {
		if (id == null) throw new IllegalArgumentException("null id");
		return available.getBit(certainOrdinalOf(id));
	}

	public void modelAvailable(String id, boolean available) {
		if (id == null) throw new IllegalArgumentException("null id");
		this.available.setBit(certainOrdinalOf(id), available);
		mutations.count++;
		requestRedraw();
	}

	public Optional<ActionModel> modelForKey(int key) {
		if (!Event.isValidKey(key)) throw new IllegalArgumentException("invalid key");
		for (StatefulAction action : statefulActions) {
			if (action.action.action().matchesKey(key)) return Optional.of(action.action);
		}
		return Optional.empty();
	}

	public ActionsModel keyboardAccessible(Keyboard keyboard) {
		if (keyboard == null) throw new IllegalArgumentException("null keyboard");
		return filteredActions(a -> a.action().isAccessibleFrom(keyboard));
	}

	public ActionsModel keyboardInaccessible(Keyboard keyboard) {
		if (keyboard == null) throw new IllegalArgumentException("null keyboard");
		return filteredActions(a -> !a.action().isAccessibleFrom(keyboard));
	}

	public List<ActionModel> actionsWithAvailability(boolean availability) {
		int count = actions.length;
		ArrayList<ActionModel> list = new ArrayList<>(count);
		synchronized (mutations) {
			for (int i = 0; i < count; i++) {
				if (visible.getBit(i) && available.getBit(i) == availability) list.add(actions[i]);
			}
		}
		return list;
	}

	public int count(boolean availability) {
		synchronized (mutations) {
			return new BitStore() {
				@Override public int size() { return actions.length; }
				@Override public boolean getBit(int index) { return visible.getBit(index) && (available.getBit(index) == availability); }
			}.ones().count();
		}
	}

	public long mutations() {
		return mutations == null ? 0L : mutations.count;
	}

	//TODO want this on the mutability interface
	public boolean invariable() {
		return mutations == null;
	}

	// mutability

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public ActionsModel mutableCopy() {
		//TODO make a constructor?
		Mutations mutations = this.mutations.copy();
		return new ActionsModel(context, deepCopy(mutations, actions), available.mutableCopy(), mutations, visible, true);
	}

	@Override
	public ActionsModel immutableCopy() {
		Mutations mutations = this.mutations.copy();
		return new ActionsModel(context, deepCopy(mutations, actions), available.immutableCopy(), mutations, visible, false);
	}

	@Override
	public ActionsModel immutableView() {
		return mutable ? new ActionsModel(context, actions, available.immutableView(), mutations, visible, false) : this;
	}

	// object methods

	@Override
	public int hashCode() {
		return mutations == null ? available.hashCode() : mutations.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ActionsModel)) return false;
		ActionsModel that = (ActionsModel) obj;
		return this.mutations != null && this.mutations == that.mutations;
	}

	@Override
	public String toString() {
		return "Actions [actions: " + Arrays.asList(statefulActions) + ", mutable: " + visible.isMutable() + "]";
	}

	// private helper methods

	private void checkMutable() {
		if (!mutable) throw new IllegalArgumentException("not mutable");
	}

	// assumption here is that patterns of access will often be either: same action repeatedly, sequential actions
	// so we always start our scan from the last successful location
	private int ordinalOf(ActionModel action) {
		int count = statefulActions.length;
		if (count == 0) return -1;
		int i = last;
		do {
			if (statefulActions[i].action.equals(action)) return last = i;
			if (++i == count) i = 0;
		} while (i != last);
		return -1;
	}

	private int ordinalOf(String id) {
		int count = statefulActions.length;
		if (count == 0) return -1;
		int i = last;
		do {
			if (statefulActions[i].action.action().id.equals(id)) return last = i;
			if (++i == count) i = 0;
		} while (i != last);
		return -1;
	}

	private int certainOrdinalOf(String id) {
		int index = ordinalOf(id);
		if (index == -1) throw new IllegalArgumentException("unrecognized id");
		return index;
	}
	// assumption here is that patterns of access will often be either: same action repeatedly, sequential actions
	// so we always start our scan from the last successful location
//	private int indexOf(Action action) {
//		int count = actions.length;
//		if (count == 0) return -1;
//		int i = last;
//		do {
//			if (actions[i].equals(action)) return last = i;
//			if (++i == last) i = 0;
//		} while (i != last);
//		return -1;
//	}
//
//	private int indexOf(int id) {
//		int count = actions.length;
//		if (count == 0) return -1;
//		int i = last;
//		do {
//			if (actions[i].id == id) return last = i;
//			if (++i == last) i = 0;
//		} while (i != last);
//		return -1;
//	}
//
//	private int checkedIndexOf(Action action) {
//		if (action == null) throw new IllegalArgumentException("null action");
//		int i = indexOf(action);
//		if (i == -1 || !visible.getBit(i)) throw new IllegalArgumentException("unknown action");
//		return i;
//	}
//
//	private void checkIndex(int i) {
//		if (i < 0 || i >= actions.length || !visible.getBit(i)) {
//			throw new IllegalArgumentException("invalid index");
//		}
//	}

	private ActionsModel subset(BitStore visible) {
		return new ActionsModel(context, actions, available, mutations, visible, mutable);
	}

	private ActionsModel filteredActions(Predicate<ActionModel> filter) {
		BitStore matches = visible.mutableCopy();
		Positions positions = matches.ones().positions();
		while (positions.hasNext()) {
			if (!filter.test(actions[positions.nextPosition()])) positions.remove();
		}
		return matches.equals(visible) ? this : subset(matches.immutableView());
	}

	// inner classes

	// equal if actions and states are equal
	private final class StatefulAction {

		private final int index;
		public final int ordinal;
		public final ActionModel action;

		private StatefulAction(int index, int ordinal) {
			this(index, ordinal, actions[index]);
		}

		private StatefulAction(int index, int ordinal, ActionModel action) {
			this.index = index;
			this.ordinal = ordinal;
			this.action = action;
		}

		public boolean available() {
			synchronized (mutations) {
				return available.getBit(index);
			}
		}

		public void available(boolean available) {
			checkMutable();
			synchronized (mutations) {
				ActionsModel.this.available.setBit(index, available);
				mutations.count ++;
			}
		}

		@Override
		public int hashCode() {
			return action.hashCode() + Boolean.hashCode(available());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof ActionsModel.StatefulAction)) return false;
			ActionsModel.StatefulAction that = (ActionsModel.StatefulAction) obj;
			return this.action.equals(that.action) && (this.available() == that.available());
		}

		@Override
		public String toString() {
			return action + " available: " + available();
		}

	}

}
