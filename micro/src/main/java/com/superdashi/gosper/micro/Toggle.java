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
import com.superdashi.gosper.micro.ToggleModel.Neutrality;
import com.superdashi.gosper.micro.ToggleModel.State;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Composer;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.Xor;
import com.tomgibara.intgeom.IntRect;

public class Toggle extends Component {

	// statics

	private static final ToggleModel DEFAULT_MODEL = new ToggleModel(Item.nothing());
	private static final Composer xor = new Xor().asComposer();

	// fields

	private final Focusing focusing = new Focusing() {

		@Override
		public IntRect focusArea() {
			return bounds;
		}
	};

	private final Eventing eventing = new Eventing() {

		@Override
		public boolean handleEvent(Event event) {
			if (event.isDown()) {
				switch (event.key) {
				case Event.KEY_LEFT:
					switch (model.state()) {
					case ACTIVE:
						/* nothing to do */
						return false;
					case NEUTRAL:
						model.state(State.ACTIVE);
						instigateAction();
						return true;
					case INACTIVE:
						model.state(model.neutrality().allowsTransitionToNeutral ? State.NEUTRAL : State.ACTIVE);
						instigateAction();
						return true;
					}
					return false;
				case Event.KEY_RIGHT:
					switch (model.state()) {
					case ACTIVE:
						model.state(model.neutrality().allowsTransitionToNeutral ? State.NEUTRAL : State.INACTIVE);
						instigateAction();
						return true;
					case NEUTRAL:
						model.state(State.INACTIVE);
						instigateAction();
						return true;
					case INACTIVE:
						/* nothing to do */
						return false;
					}
					return false;
				}
			}
			return false;
		}

		private void instigateAction() {
			situation.instigate(Action.create(model.state().defaultActionId, model.info()));
		}
	};

	private Frame twoState;
	private Frame triState;
	private IntRect bounds;
	private boolean fitted;
	private ToggleModel model = DEFAULT_MODEL;
	private Frame active = null;
	private boolean sizeChange = false;
	private long revision = -1L;

	// constructors

	// accessors

	public ToggleModel model() {
		return model;
	}

	public void model(ToggleModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		if (model == this.model) return;
		this.model = model;
		revision = -1L;
	}

	// convenience method
	public ToggleModel item(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
		ToggleModel model = situation.models().toggleModel(item);
		model(model);
		return model;
	}

	// convenience method
	public Item item() {
		return model == null ? null : model.info();
	}

	// component

	@Override
	void situate(Situation situation) {
		this.situation = situation;
		//TODO need to load asynchonously
		VisualSpec spec = situation.visualSpec();
		twoState = spec.toggle2();
		triState = spec.toggle3();
	}

	@Override
	void place(Place place, int z) {
		updateActive();
	}

	@Override
	Composition composition() {
		return Composition.MASK;
	}

	@Override
	IntRect bounds() {
		return bounds;
	}

	@Override
	Changes changes() {
		if (sizeChange) return Changes.SHAPE;
		if (!fitted) return Changes.NONE; //TODO how to render when it doesn't fit
		return revision != model.revision() ? Changes.CONTENT : Changes.NONE;
	}

	@Override
	void render() {
		updateActive();
		Canvas canvas = situation.defaultPane().canvas();
		if (fitted) {
			canvas.drawFrame(active);
			final IntRect area;
			if (active == twoState) {
				switch (model.state()) {
				case ACTIVE   : area = IntRect.rectangle(2, 2, 20, 20); break;
				case INACTIVE : area = IntRect.rectangle(24, 2, 20, 20); break;
				default: area = null;
				}
			} else {
				switch (model.state()) {
				case ACTIVE   : area = IntRect.rectangle(2, 2, 20, 20); break;
				case NEUTRAL  : area = IntRect.rectangle(24, 2, 20, 20); break;
				case INACTIVE : area = IntRect.rectangle(46, 2, 20, 20); break;
				default: area = null;
				}
			}
			if (area != null) {
				canvas.pushState();
				//TODO why not rendering?
				canvas.composer(xor).intOps().fillRect(area);
				canvas.popState();
			}
		} else {
			canvas.erase();
		}
		revision = model.revision();
		sizeChange = true;
	}

	@Override
	Optional<Focusing> focusing() {
		return Optional.of(focusing);
	}

	@Override
	Optional<Eventing> eventing() {
		return Optional.of(eventing);
	}

	// private utility methods

	private void updateActive() {
		boolean neutral = model.neutrality() == Neutrality.ALWAYS_AVAILABLE;
		Frame active = neutral ? triState : twoState;
		if (active == this.active) return;
		//TODO this is no longer possible without creating a dedicated pane - probably not worth it
		//if (this.active == null || !active.dimensions().equals(this.active.dimensions())) {
		if (this.active == null) {
			//TODO should not assume centre
			Place place = situation.place();
			IntRect outerBounds = place.innerBounds;
			bounds = active.dimensions().toRect().centeredIn(outerBounds);
			fitted = place.outerBounds.containsRect(bounds);
			sizeChange = true;
		}
		this.active = active;
	}

}
