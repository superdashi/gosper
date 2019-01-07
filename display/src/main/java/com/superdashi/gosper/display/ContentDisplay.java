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
package com.superdashi.gosper.display;

import java.awt.Graphics2D;

import com.superdashi.gosper.core.Cache;
import com.superdashi.gosper.core.Content;
import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.item.Info;
import com.tomgibara.intgeom.IntRect;

public class ContentDisplay {

	private final Content content;
	private final Graphics2D g;
	private final IntRect rect;
	private final Cache cache;
	private Info info = null;
	private boolean freshInfo = false;

	ContentDisplay(Content content, Graphics2D g, IntRect rect, DisplayContext context) {
		this.content = content;
		this.g = g;
		this.rect = rect;
		this.cache = context.getCache();
	}

	public boolean requiresInfo() {
		//TODO need to check for informational updates
		return info == null || content.acquirer.isNewInfoAvailable();
	}

	public void acquireInfo(Inspector inspector) {
		Info newInfo = content.acquirer.acquireInfo(inspector);
		if (!newInfo.equals(info)) {
			info = newInfo;
			freshInfo = true;
		}
	}

	public boolean requiresRender() {
		//TODO need to check for display updates
		return freshInfo;
	}

	public void renderInitial() {
		content.waitRenderer.renderInfo(content.waitInfo, g, rect, cache);
	}

	public void renderUpdate() {
		if (info != null) {
			content.infoRenderer.renderInfo(info, g, rect, cache);
			freshInfo = false;
		}
	}
}
