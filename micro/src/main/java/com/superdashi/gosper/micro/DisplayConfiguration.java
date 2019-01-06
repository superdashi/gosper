package com.superdashi.gosper.micro;

import java.util.Locale;

import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Qualifier;

//TODO consider changing has -> needs
public final class DisplayConfiguration {

	private final ActivityContext context;
	Qualifier qualifier;
	boolean hasTopBar;
	boolean hasScrollbar;

	DisplayConfiguration(ActivityContext context) {
		this.context = context;
		qualifier = context.items().qualifier();
		Flavor flavor = qualifier.flavor;
		hasTopBar = flavor != Flavor.INPUT && flavor != Flavor.MODAL;
		hasScrollbar = flavor == Flavor.LIST;
	}

	//TODO support as string - or use wrapper type?
	public Locale lang() {
		return qualifier.lang;
	}

	public DisplayConfiguration lang(Locale lang) {
		if (lang == null) throw new IllegalArgumentException("null lang");
		qualifier = qualifier.withLang(lang);
		return this;
	}

	public Flavor flavor() {
		return qualifier.flavor;
	}

	public DisplayConfiguration flavor(Flavor flavor) {
		if (flavor == null) throw new IllegalArgumentException("null flavor");
		qualifier = qualifier.withFlavor(flavor);
		return this;
	}

	public boolean hasTopBar() {
		return hasTopBar;
	}

	public DisplayConfiguration hasTopBar(boolean hasTopBar) {
		this.hasTopBar = hasTopBar;
		return this;
	}

	public boolean hasScrollbar() {
		return this.hasScrollbar;
	}

	public DisplayConfiguration hasScrollbar(boolean hasScrollbar) {
		this.hasScrollbar = hasScrollbar;
		return this;
	}

	public Display layoutDisplay() {
		return context.createDisplay(this, Layout.single(), null);
	}

	public Display layoutDisplay(Layout layout) {
		if (layout == null) throw new IllegalArgumentException("null layout");
		return context.createDisplay(this, layout, null);
	}

	public Display layoutDisplay(Layout layout, Background background) {
		if (layout == null) throw new IllegalArgumentException("null layout");
		if (background == null) throw new IllegalArgumentException("null background");
		return context.createDisplay(this, layout, background);
	}

}
