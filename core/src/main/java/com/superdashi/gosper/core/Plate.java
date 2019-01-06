package com.superdashi.gosper.core;

import com.superdashi.gosper.core.Layout.Place;
import com.tomgibara.storage.Store;

public class Plate {

	public final Panel panel;
	public final Place place;
	public final Store<Content> content;

	Plate(Panel panel, Place place, Store<Content> content) {
		this.panel = panel;
		this.place = place;
		this.content = content;
	}

}
