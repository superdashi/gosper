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

public final class ActionModel extends Model {

	// statics

	// fields

	final Mutations mutations;
	private Action action;
	private ItemModel itemModel;
	private boolean enabled;

	// constructors

	// constructor for non-contextual snapshotted models
	ActionModel(Action action) {
		super((ActivityContext) null);
		mutations = null;
		this.action = action;
		itemModel = new ItemModel(action.item);
	}

	ActionModel(ActivityContext context, Action action, Mutations mutations) {
		super(context);
		this.mutations = mutations;
		this.action = action;
		this.itemModel = new ItemModel(context, action.item, mutations);
		enabled = true;
	}

	private ActionModel(ActionModel that, Mutations mutations) {
		super(that);
		this.mutations = mutations;
		this.action = that.action;
		this.itemModel = that.itemModel.copy(mutations);
		this.enabled = that.enabled;
	}

	// public accessors

	public Action action() {
		return action;
	}

	public void action(Action action) {
		if (action == null) throw new IllegalArgumentException("null action");
		if (action.equals(this.action)) return;
		this.action = action;
		Models models = models();
		this.itemModel = models == null ? new ItemModel(null, action.item, mutations) : models.itemModel(action.item, mutations);
		mutations.count ++;
		requestRedraw();
	}

	public boolean enabled() {
		return enabled;
	}

	public void enabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		mutations.count ++;
		requestRedraw();
	}

	// object methods

	//TODO is this correct?? perhaps hashCode/equals should be default implementations

	@Override
	public int hashCode() {
		return Boolean.hashCode(enabled) + action.hashCode() + 31 * itemModel.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ActionModel)) return false;
		ActionModel that = (ActionModel) obj;
		return this.action.equals(that.action) && this.itemModel.equals(that.itemModel) && this.enabled == that.enabled;
	}

	@Override
	public String toString() {
		return action.toString();
	}

	// package scoped methods

	long revision() {
		return mutations == null ? 0 : mutations.count;
	}

	// used to make detached snapshots of actions
	ActionModel copy(Mutations mutations) {
		return new ActionModel(this, mutations);
	}

	ItemModel itemModel() {
		return itemModel;
	}

}
