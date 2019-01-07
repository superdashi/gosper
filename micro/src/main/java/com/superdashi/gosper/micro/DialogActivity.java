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

import static com.superdashi.gosper.item.Flavor.MODAL;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.layout.Style;

class DialogActivity implements Activity {

	private static final int MAX_OPTIONS = 3;
	private static final String[] locationNames = {"one", "two", "three", "four"};

	static final String DATA_INFO    = "info";
	static final String DATA_CANCEL  = "cancel";
	static final String DATA_OPTIONS = "options";
	static final String DATA_SELECTION = "selection";

	static DataOutput dataFor(Item info, Action cancel, Action... options) {
		DataOutput out = new DataOutput();
		if (info != null) out.put(DATA_INFO, info);
		if (cancel != null) out.put(DATA_CANCEL, cancel);
		if (options != null) out.put(DATA_OPTIONS, options);
		return out;
	}

	private ActivityContext context;
	private ItemModel info;
	private ActionModel cancel;
	private ActionModel[] options;

	@Override
	public void init() {
		this.context = ActivityContext.current();
		context.setEventListener(this::handleEvent);
		context.setActionHandler(this::handleAction);
		//TODO need to make optional
		DataInput data = context.launchData();
		Models models = context.models();
		info = models.itemModel( data.getItem(DATA_INFO) );
		if (data.hasKey(DATA_CANCEL)) cancel = models.actionModel(data.getAction(DATA_CANCEL));
		if (data.hasKey(DATA_OPTIONS)) options = models.actionModels(data.getActions(DATA_OPTIONS));
	}

	@Override
	public void open(DataInput savedState) {
		Style buttonStyle = new Style().colorBg(0xff000000).colorBg(0xffffffff).immutable();
		//Layout layout = Layout.single().withWeight(1f).addBelow().withMinimumHeight(15).withStyle(buttonStyle);
		int buttonCount = 0;
		if (cancel != null) buttonCount ++;
		if (options != null) buttonCount += Math.min(options.length, MAX_OPTIONS);

		Layout layout = Layout.single().withLocation(locationNames[0]);
		for (int i = 1; i < buttonCount; i++) {
			layout = layout.addRight().withLocation(locationNames[i]);
		}
		if (buttonCount > 1) layout = layout.group();
		layout = layout.withMinimumHeight(15).withStyle(buttonStyle).withMinimumHeight(15).withWeight(0f).addAbove().withWeight(1f);

		Display display = context.configureDisplay().flavor(MODAL).layoutDisplay(layout);
		Style labelStyle = new Style().colorFg(0xffffffff).textWeight(1);
		Style descStyle = new Style().colorFg(0xffffffff);
		//TODO could add as a document if info identified as having a resource
		//TODO not hardwire height
		CardDesign design = new CardDesign(Layout.single().withStyle(labelStyle).withMinimumHeight(15).addBelow().withStyle(descStyle).withWeight(1f))
				.setContentsAtLocation(Location.top, ItemContents.label())
				.setContentsAtLocation(Location.bottom, ItemContents.description())
				;
		Card card = display.addCard(Location.top);
		card.design(design);
		card.model(info);

		if (options != null) {
			for (int i = 0; i < options.length; i++) {
				PrimaryButton button = display.addPrimaryButton(Location.named(locationNames[i]));
				button.model(options[i]);
			}
		}
		if (cancel != null) {
			PrimaryButton button = display.addPrimaryButton(Location.named(locationNames[buttonCount - 1]));
			button.model(cancel);
		}
	}

	private void handleEvent(Event event) {
		if (cancel != null && event.isKey() && event.isDown() && event.key == Event.KEY_CANCEL) {
			// return the cancel item
			concludeWith(cancel.action());
			return;
		}
		// fall through to regular event handling
		context.defaultEventHandler().handleEvent(event);
	}

	private void handleAction(Action action) {
		concludeWith(action);
	}

	private void concludeWith(Action action) {
		context.concludeActivity(new DataOutput().put(DATA_SELECTION, action.item));
	}
}
