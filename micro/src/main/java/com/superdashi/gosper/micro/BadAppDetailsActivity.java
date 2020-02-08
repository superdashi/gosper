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
import com.superdashi.gosper.layout.Style;

class BadAppDetailsActivity implements Activity {

	private final String instanceId;
	private final String reason;

	BadAppDetailsActivity(String instanceId, String reason) {
		this.instanceId = instanceId;
		this.reason = reason;
	}

	@Override
	public void open(DataInput savedState) {
		ActivityContext context = ActivityContext.current();
		Display display = context.configureDisplay().flavor(Flavor.ERROR).layoutDisplay(Layout.single());
		display.bar().get().item(Item.fromLabel("Failed application launch"));
		Document document = display.addDocument(Location.center);

		DocumentModel docModel = context.models().documentModel();
		docModel.style(new Style().colorFg(0xffffffff).colorBg(0xff000000));
		docModel.attachedClass("title").style(new Style().textWeight(1).lineLimit(1));
		docModel.attachedClass("after").style(new Style().marginTop(2));
		docModel.appendNewBlock().textContent("Instance ID").classNames("title");
		docModel.appendNewBlock().textContent(instanceId);
		docModel.appendNewBlock().textContent("Reason").classNames("title", "after");
		docModel.appendNewBlock().textContent(reason);

		document.model(docModel);
	}

}
