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
package com.superdashi.gosper.core;

import com.superdashi.gosper.core.Layout.Place;
import com.superdashi.gosper.item.Info;

//TODO consider making Info(Renderer|Acquirer) interfaces inner classes of Content
public final class Content {

	public final Place place;
	public final Info waitInfo;
	public final InfoRenderer waitRenderer;
	public final InfoRenderer infoRenderer;
	//TODO allow multiple acquirers?
	public final InfoAcquirer acquirer;

	public Content(Place place, Info waitInfo, InfoRenderer waitRenderer, InfoRenderer infoRenderer, InfoAcquirer acquirer) {
		if (place == null) throw new IllegalArgumentException("null place");
		if (waitInfo == null) throw new IllegalArgumentException("null waitInfo");
		if (waitRenderer == null) throw new IllegalArgumentException("null waitRenderer");
		if (infoRenderer == null) throw new IllegalArgumentException("null infoRenderer");
		if (acquirer == null) throw new IllegalArgumentException("null acquirer");

		this.place = place;
		this.waitInfo = waitInfo;
		this.waitRenderer = waitRenderer;
		this.infoRenderer = infoRenderer;
		this.acquirer = acquirer;
	}

}
