package com.superdashi.gosper.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Configurable;
import com.superdashi.gosper.core.Layout.Place;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public final class Panel extends DesignChild {

	private static final StoreType<Content> contentType = StoreType.of(Content.class).settingNullAllowed();
	private static final Store<Content> noContent = contentType.emptyStore();
	private static final StoreType<Plate> plateType = StoreType.of(Plate.class).settingNullDisallowed();
	private static final PanelConfig DEFAULT_STYLE = new PanelConfig();
	private static final Layout DEFAULT_LAYOUT = Layout.grid(1, 1);
	private static final Store<Plate> DEFAULT_PLATES = plateType.emptyStore();

	private static Store<Content> join(Store<Content> a, Store<Content> b) {
		//TODO add method to store to make this more efficient?
		int as = a.size();
		int bs = b.size();
		a = a.resizedCopy(as + bs);
		for (int i = 0; i < bs; i++) {
			a.set(as + i, b.get(i));
		}
		return a;
	}


	private PanelConfig givenStyle = DEFAULT_STYLE;
	private Layout layout = DEFAULT_LAYOUT;
	private Store<Plate> plates = DEFAULT_PLATES;

	public final PanelConfig style = new PanelConfig();

	public Panel() {
	}

	// accessors

	public void setStyle(PanelConfig style) {
		this.givenStyle = style;
	}

	public void setContent(Layout layout, Content... contents) {
		if (layout == null) throw new IllegalArgumentException("null layout");
		this.layout = layout;
		Store<Place> places = layout.places;
		int count = places.size();
		Plate[] plates = new Plate[count];
		HashMap<Place, Store<Content>> map = Arrays.stream(contents).collect(Collectors.toMap(c -> c.place, c -> contentType.objectsAsStore(c), Panel::join, HashMap::new));
		for (int i = 0; i < count; i++) {
			Place place = places.get(i);
			plates[i] = new Plate(this, place, map.getOrDefault(place, noContent));
		}
		this.plates =  plateType.objectsAsStore(plates);
	}

	public Layout getLayout() {
		return layout;
	}

	public Store<Plate> getPlates() {
		return plates;
	}

	// styleable

	public Configurable.Type getStyleType() {
		return Configurable.Type.PANEL;
	}

	@Override
	public ConfigTarget openTarget() {
		style.adopt(givenStyle);
		return style;
	}

	@Override
	public Optional<String> getRole() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public String getId() {
		//TODO
		return null;
	}

}
