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

import java.util.Arrays;

import com.superdashi.gosper.item.Item;

public final class Models {

	private final ActivityContext context;

	// cached instances
	private TableModel emptyTableModel = null;

	Models(ActivityContext context) {
		this.context = context;
	}

	//TODO rename these to new/create...Model
	public ItemModel itemModel(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
		return new ItemModel(context, item);
	}

	public ItemModel[] itemModels(Item... items) {
		if (items == null) throw new IllegalArgumentException("null items");
		ItemModel[] models = new ItemModel[items.length];
		for (int i = 0; i < models.length; i++) {
			Item item = items[i];
			if (item == null) throw new IllegalArgumentException("null item");
			models[i] = new ItemModel(context, item);
		}
		return models;
	}

	public CheckboxModel checkboxModel(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
		return new CheckboxModel(context, item, new Mutations());
	}

	public ActionModel actionModel(Action action) {
		if (action == null) throw new IllegalArgumentException("null action");
		return new ActionModel(context, action, new Mutations());
	}

	public ActionModel[] actionModels(Action... actions) {
		if (actions == null) throw new IllegalArgumentException("null actions");
		ActionModel[] models = new ActionModel[actions.length];
		for (int i = 0; i < models.length; i++) {
			Action action = actions[i];
			if (action == null) throw new IllegalArgumentException("null action");
			models[i] = new ActionModel(context, action, new Mutations());
		}
		return models;
	}

	public ActionsModel actionsModel(Action... actions) {
		if (actions == null) throw new IllegalArgumentException("null actions");
		if (actions.length == 0) return ActionsModel.none();
		Action.checkedKeys(false, Arrays.stream(actions).flatMapToInt(a -> Arrays.stream(a.keys())).toArray()); // check for keys mapped to more than one action
		for (int i = 0; i < actions.length; i++) if (actions[i] == null) throw new IllegalArgumentException("null action");
		Mutations mutations = new Mutations();
		ActionModel[] models = new ActionModel[actions.length];
		for (int i = 0; i < actions.length; i++) {
			Action action = actions[i];
			models[i] = new ActionModel(context, action, mutations);
		}

		return ActionsModel.over(context, models);
	}

	public FormModel formModel() {
		return new FormModel(context);
	}

	public DocumentModel documentModel() {
		return new DocumentModel(context, new Mutations());
	}

	public ScrollbarModel scrollbarModel() {
		return new ScrollbarModel(context, new Mutations());
	}

	public TableModel emptyTableModel() {
		return emptyTableModel == null ? emptyTableModel = new TableModel(context, Rows.noRows(), new Mutations()) : emptyTableModel;
	}

	public TableModel tableModel(Rows rows) {
		if (rows == null) throw new IllegalArgumentException("null rows");
		return new TableModel(context, rows, new Mutations());
	}

	public ActiveModel activeModel() {
		return new ActiveModel(context);
	}

	public ToggleModel toggleModel(Item info) {
		if (info == null) throw new IllegalArgumentException("null info");
		return new ToggleModel(context, new Mutations(), info);
	}

	//TODO consider how best to properly expose this
	ItemModel itemModel(Item item, Mutations mutations) {
		return new ItemModel(context, item, mutations);
	}

}
