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

import java.util.Optional;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Frame;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public final class Checkbox extends ItemComponent {

	// statics

	private static final int REGULAR  = 0;
	private static final int FOCUSED  = 1;
	private static final int SELECTED = 2;

	// fields

	private final Focusing focusing = new Focusing() {

		public void cedeFocus() {
			focusChanged = true;
			situation.requestRedrawNow();
		}

		@Override
		public void receiveFocus(int areaIndex) {
			focusChanged = true;
			situation.requestRedrawNow();
		}

		@Override
		public IntRect focusArea() {
			IntCoords coords = situation.place().innerBounds.minimumCoords();
			return IntRect.rectangle(coords, checkDims);
		}
	};

	private final Pointing pointing = new Pointing() {
		@Override
		public boolean clicked(int areaIndex, IntCoords coords) {
			situation.focus();
			toggle();
			return true;
		}
	};

	private final Eventing eventing = new Eventing() {
		@Override
		public boolean handleEvent(Event event) {
			if (event.isKey() && event.isDown() && event.key == Event.KEY_CONFIRM) {
				toggle();
			}
			return false;
		}
	};

	private Frame checkboxes;
	private int size;
	private IntDimensions checkDims;
	private CheckboxModel model = null;
	private boolean focusChanged = false;

	@Override
	void situate(Situation situation) {
		super.situate(situation);
		VisualSpec spec = situation.visualSpec();
		checkboxes = spec.checkbox();
		size = spec.metrics.badgeSize;
		checkDims = IntDimensions.square(size);
	}

	@Override
	Changes changes() {
		if (focusChanged) return Changes.CONTENT;
		return super.changes();
	}
	@Override
	Optional<Focusing> focusing() {
		return Optional.of(focusing);
	}

	@Override
	Optional<Pointing> pointing() {
		return Optional.of(pointing);
	}

	@Override
	Optional<Eventing> eventing() {
		return Optional.of(eventing);
	}

	@Override
	IntDimensions minimumSize() {
		return checkDims;
	}

	// public accessors

	public CheckboxModel model() {
		return model;
	}

	public void model(CheckboxModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		if (model == this.model) return;
		this.model = model;
		resetRevision();
		situation.requestRedrawNow();
	}

	// convenience method
	public CheckboxModel item(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
		CheckboxModel model = situation.models().checkboxModel(item);
		model(model);
		return model;
	}

	// convenience method
	public Item item() {
		return model == null ? null : model.item();
	}

	// package methods

	@Override
	ItemModel itemModel() {
		return model == null ? null : model.itemModel();
	}

	@Override
	IntRect designDimensions(Place place) {
		VisualSpec spec = situation.visualSpec();
		IntDimensions dims = place.innerDimensions;
		//TODO metrics needs badge gap
		//TODO need to cope with not fitting
		//return IntRect.bounded(bounds.minX + spec.metrics.badgeSize + 1, bounds.minY, bounds.maxX, bounds.maxY);
		return dims.toRect(spec.metrics.badgeSize + 1, 0);
	}

	@Override
	void decorate(Canvas canvas) {
		int state = situation.isFocused() ? FOCUSED : REGULAR;
		boolean disabled = false; //TODO how is this controlled?
		boolean checked = model == null ? false : model.checked();
		int x = state;
		int y = 0;
		if (disabled) y += 1;
		if (checked) y += 2;
		canvas.drawFrame(checkboxes.view(IntRect.rectangle(x,y,1,1).scaled(size)));
		focusChanged = false;
	}

	private void toggle() {
		if (model != null) {
			model.toggleChecked();
		}
		situation.requestRedrawNow();
	}

}
