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

import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Item;

class SelectActivity implements Activity {

	private static final Action[] NO_OPTIONS = new Action[0];

	static final String DATA_INFO      = "info";
	static final String DATA_OPTIONS   = "options";
	static final String DATA_SELECTED  = "selected";
	static final String DATA_SELECTION = "selection";

	static DataOutput dataFor(Item info, int selected, Action... options) {
		return new DataOutput()
				.put(DATA_INFO, info)
				.put(DATA_SELECTED, selected)
				.put(DATA_OPTIONS, options);
	}

	private ActivityContext context;
	private Item info;
	private int selected;
	private Action[] options;

	@Override
	public void init() {
		context = ActivityContext.current();
		context.setActionHandler(this::handleAction);
		DataInput in = context.launchData();
		//TODO need typed versions of hasKey, or optional accessors for everything
		info = in.hasKey(DATA_INFO) ? in.getItem(DATA_INFO) : defaultInfo();
		selected = in.hasKey(DATA_SELECTED) ? in.getInt(DATA_SELECTED) : -1;
		options = in.hasKey(DATA_OPTIONS) ? in.getActions(DATA_OPTIONS) : NO_OPTIONS;
	}

	@Override
	public void open(DataInput savedState) {
		Display display = context.configureDisplay().flavor(Flavor.INPUT).hasTopBar(true).hasScrollbar(true).layoutDisplay(Layout.single());
		// populate bar
		String barText = info.label().orElse("Select");
		//TODO should bar contents be set directly via display?
		display.bar().get().setPlainText(barText);
		// populate table
		Table table = display.addTable(Location.center, new DisplayColumns(true, true));
		TableModel model = context.models().tableModel(Rows.fixedActionRows(true, true, options));
		table.model(model);
		table.activeIndex(selected);
		table.emptyMessage("No options to select.");
	}

	private void handleAction(Action action) {
		//TODO use actions model for indexing?
		//TODO is strict equality brittle?
		DataOutput data = context.launchData().toOutput().put(DATA_SELECTION, action);
		for (int i = 0; i < options.length; i++) {
			if (options[i] == action) {
				data.put(DATA_SELECTED, i);
				break;
			}
		}
		context.concludeActivity(data);
	}

	private Item defaultInfo() {
		//TODO improve
		return Item.fromLabel("No selections available");
	}

}
