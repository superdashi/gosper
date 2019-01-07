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

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Item;
import com.tomgibara.intgeom.IntDimensions;

class KeyboardActivity implements Activity {

	static final String DATA_INFO = "info";
	static final String DATA_TEXT = "text";
	static final String DATA_REGEX = "regex";

	static DataOutput dataFor(Item info, String text, Regex regex) {
		DataOutput data = new DataOutput()
				.put(DATA_INFO, info)
				.put(DATA_TEXT, text);
		if (regex != null) data.put(DATA_REGEX, regex, Regex::serialize);
		return data;
	}

	private static final String ACTION_CANCEL = "create";

	private ActivityContext context;
	private ActionsModel actions;
	private ItemModel infoModel;
	private KeyboardModel kbModel;

	@Override
	public void init() {
		this.context = ActivityContext.current();
		context.setActionHandler(this::handleAction);

		// extract info and text
		DataInput in = context.launchData();
		Item info = in.hasKey(DATA_INFO) ? in.getItem(DATA_INFO) : defaultInfo();

		// extract model
		infoModel = context.models().itemModel(info);
		// action models
		actions = context.models().actionsModel(Action.create(ACTION_CANCEL, "Cancel", null, Event.KEY_CANCEL));
	}

	@Override
	public void open(DataInput savedState) {
		DataInput in = context.launchData();
		Regex regex = in.optionalGet(DATA_REGEX, Regex::deserialize).orElse(null);
		String text = (savedState == null ? in : savedState).getString(DATA_TEXT, "");
		Display display = context.configureDisplay().flavor(Flavor.INPUT).hasTopBar(false).layoutDisplay(Layout.single());
		//TODO need a more complete approach to this
		IntDimensions screenDim = display.visualSpec().qualifier.dimensions;
		String designName = String.format("keyboard.%dx%d.design", screenDim.width, screenDim.height);
		KeyboardDesign design = KeyboardDesign.fromResource(designName);
		kbModel = new KeyboardModel(context, design, new Mutations()); //TODO maybe move onto context
		kbModel.text(text);
		kbModel.regex(regex);
		display.actionsModel(actions);
		KeyboardComponent keyboard = display.addKeyboard(Location.center, infoModel);
		keyboard.model(kbModel);
	}

	@Override
	public void close(DataOutput state) {
		state.put(DATA_TEXT, kbModel.text());
		kbModel = null;
	}

	@Override
	public void destroy() {
		context = null;
	}

	private Item defaultInfo() {
		//TODO should base on lang etc.
		return Item.fromLabel("Text");
	}

	private void handleAction(Action action) {
		switch (action.id) {
		case Action.ID_CHANGE_VALUE:
			context.concludeActivity(context.launchData().toOutput().put(DATA_TEXT, kbModel.text()));
			return;
		case ACTION_CANCEL:
			context.concludeActivity();
			return;
		}
	}

}
