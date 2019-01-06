package com.superdashi.gosper.display;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.util.texture.Texture;
import com.superdashi.gosper.core.Panel;
import com.superdashi.gosper.core.Plate;
import com.superdashi.gosper.core.Layout.Place;
import com.superdashi.gosper.graphdb.Inspector;
import com.tomgibara.fundament.Mapping;
import com.tomgibara.geom.core.Offset;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public class PanelDisplay {

	private static final Storage<PlateDisplay> pdStorage = StoreType.of(PlateDisplay.class).storage().immutable();

	public final Panel panel;
	private final DynamicAtlas<Place> atlas;
	public final Store<PlateDisplay> plateDisplays;
	private boolean dirty;

	public PanelDisplay(Panel panel, DisplayContext context) {
		if (panel == null) throw new IllegalArgumentException("null panel");
		if (context == null) throw new IllegalArgumentException("null context");

		this.panel = panel;
		this.atlas = context.getPlaceAtlas();
		DashiCamera.Params params = context.getParams();
		float gutter = panel.style.gutter;
		float hGutter = gutter * params.unitW;
		float vGutter = gutter * params.unitH;
		Offset offset = Transform.scale(params.unitW, params.unitH).transform(panel.style.offset);
		Texture texture = atlas.getTexture();
		float texWidth = texture.getWidth();
		float texHeight = texture.getHeight();
		float atlasGutter = Math.min(texWidth, texHeight) * panel.style.gutter;
		float width = texWidth;
		float height = texHeight;
		plateDisplays = panel.getPlates().asTransformedBy(Mapping.fromFunction(
				Plate.class,
				PlateDisplay.class,
				plate -> {
			Place place = plate.place;
			Rect rect = place.getRect(offset, hGutter, vGutter);
			int w = (int) (width * rect.getWidth());
			int h = (int) (height * rect.getHeight());
			//TODO should guard against empty case
			DynamicAtlas<Place>.Updater updater = atlas.add(w, h, place).get();
			return new PlateDisplay(plate, rect, updater, context);
		})).copiedBy(pdStorage);
		//TODO should not be done in constructor?
		renderInitial();
	}

	public void clean(GL gl) {
		if (dirty) {
			atlas.update(gl);
			dirty = false;
		}
	}

//	// test only
//	public void blankPlates(GL gl) {
//		for (PlateDisplay pd : plateDisplays) {
//			pd.blankPlate(10);
//			pd.applyUpdate(gl);
//		}
//	}


	public boolean requiresInfo() {
		for (PlateDisplay display : plateDisplays) {
			if (display.requiresInfo()) return true;
		}
		return false;
	}

	public void acquireInfo(Inspector inspector) {
		for (PlateDisplay display : plateDisplays) {
			if (display.requiresInfo()) {
				display.acquireInfo(inspector);
			}
		}
	}

	public boolean requiresRender() {
		for (PlateDisplay display : plateDisplays) {
			if (display.requiresRender()) return true;
		}
		return false;
	}

	// called on worker thread
	public void renderInitial() {
		for (PlateDisplay display : plateDisplays) {
			display.renderInitial();
		}
	}

	// called on worker thread
	public void renderUpdate() {
		boolean dirtied = false;
		for (PlateDisplay display : plateDisplays) {
			if (display.requiresRender()) {
				display.renderUpdate();
				dirtied = true;
			}
		}
		dirty = dirty || dirtied;
	}

	// called on GL thread
	public Collection<Element> getElements(RenderContext context) {
		ArtPlane dash = context.getDisplay().getDashPlane();
		float inset = Math.min(dash.width, dash.height) * panel.style.gutter * 0.5f;
		List<Element> elements = new ArrayList<>();
		for (PlateDisplay display : plateDisplays) {
			elements.addAll(display.getElement(dash, inset));
		}
		return elements;
	}

}
