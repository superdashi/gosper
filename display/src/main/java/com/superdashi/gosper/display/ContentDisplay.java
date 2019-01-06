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
