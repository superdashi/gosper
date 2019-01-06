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
