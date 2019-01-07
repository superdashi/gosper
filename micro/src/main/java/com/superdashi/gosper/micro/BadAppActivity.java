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
import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.layout.Alignment2D;
import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Position.Fit;

//TODO needs to show details available
//TODO needs to take face from correct resources
class BadAppActivity implements Activity {

	private final String instanceId;
	private final String reason;
	private ActivityContext context;
	private ItemModel model;

	BadAppActivity(String instanceId, String reason) {
		this.instanceId = instanceId;
		this.reason = reason;
	}

	@Override
	public void init() {
		this.context = ActivityContext.current();
		//TODO add a convenience method for this
		model = context.models().itemModel(context.items().itemWithId("face_sad"));
	}

	@Override
	public void open(DataInput savedState) {
		Display display = context.configureDisplay().flavor(Flavor.ERROR).layoutDisplay(Layout.single());
		Bar bar = display.bar().get();
		bar.setPlainText("Application failed to launch");

		CardDesign design = new CardDesign(Position.from(Fit.FREE, Fit.FREE, Alignment2D.pair(Alignment.MID, Alignment.MIN)));
		Card card = display.addCard(Location.center);
		card.design(design);
		card.model(model);
	}

	@Override
	public void activate() {
		context.setEventListener(this::handleEvent);
	}

	private boolean handleEvent(Event event) {
		if (event.isDown() && event.key == Event.KEY_CONFIRM) {
			context.activities().requestActivity("activity_details").mode(ActivityMode.REPLACE_CURRENT).launch();
			return true;
		}
		return context.defaultEventHandler().handleEvent(event);
	}
}
