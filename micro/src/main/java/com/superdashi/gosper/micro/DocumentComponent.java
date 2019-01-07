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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.micro.DocumentModel.Blueprint;
import com.superdashi.gosper.micro.DocumentModel.Position;
import com.superdashi.gosper.micro.DocumentModel.Blueprint.Section;
import com.superdashi.gosper.micro.Selector.State;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Composer;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.PorterDuff.Rule;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntRange;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;

public class DocumentComponent extends Component {

	private static Composer src = new PorterDuff(Rule.SRC).asComposer();

	private final Focusing focusing = new Focusing() {

		@Override
		public IntRect focusArea() {
			return bounds();
		}

	};

	private final Eventing eventing = new Eventing() {

		@Override
		public EventMask eventMask() {
			return EventMask.anyKeyDown();
		}

		@Override
		public boolean handleEvent(Event event) {
			switch (event.key) {
			case Event.KEY_CONFIRM :
				if (position.block == null) return false;
				Optional<ActionModel> maybeAction = position.action();
				if (!maybeAction.isPresent()) return false;
				ActionModel actionModel = maybeAction.get();
				if (!actionModel.enabled()) return false;
				situation.instigate(actionModel.action());
				return true;
			case Event.KEY_UP :
				moveUp();
				return true;
			case Event.KEY_DOWN :
				moveDown();
				return true;
			}
			return false;
		}
	};

	private final Scrolling scrolling = new Scrolling() {
		@Override
		public ScrollbarModel scrollbarModel() {
			return scrollbarModel;
		}
	};

	private final Pointing pointing = new Pointing() {

		private List<Section> sections;
		private List<IntRect> areas;

		@Override
		public List<IntRect> clickableAreas() {
			computeAreas();
			return areas;
		}

		@Override
		public boolean clicked(int areaIndex, IntCoords coords) {
			if (areaIndex < 0) return false;
			situation.instigate(sections.get(areaIndex).action.action());
			return true;
		}

		private void computeAreas() {
			if (blueprint == null || position == null) {
				sections = Collections.emptyList();
				areas = Collections.emptyList();
				return;
			}
			areas = new ArrayList<>();
			sections = new ArrayList<>();
			IntDimensions viewSize = blueprint.viewSize;
			IntRange xRange = viewSize.rangeOfX();
			IntRange yRange = viewSize.rangeOfY();
			IntVector offset = bounds().vectorToMinimumCoords();
			for (Section section : blueprint.sections) {
				if (!section.actionable()) continue;
				int top = section.top - position.y;
				int bottom = top + section.height;
				IntRange.bounded(top, bottom).intersection(yRange).
				ifPresent(r -> {
					areas.add(r.projectAlongX(xRange).translatedBy(offset));
					sections.add(section);
				});
			}
		}
	};

	private boolean dirty = true;
	private DocumentModel model = null;
	private Blueprint blueprint = null;
	private Position position = null;
	private ScrollbarModel scrollbarModel = null;

	// accessors

	DocumentModel documentModel() {
		return model;
	}

	void documentModel(DocumentModel model) {
		if (Objects.equals(model, this.model)) return;
		this.model = model;
		blueprint = null;
		if (position != null) position = model.inheritPosition(position);
		updateAction();
		updateScrolling();
		dirty = true;
	}

//	Optional<ActionModel> activeAction() {
//		return position == null ? Optional.empty() : position.action();
//	}

	// methods

	// may only be called in active state
	public void moveDown() {
		move(false);
	}

	// may only be called in active state
	public void moveUp() {
		move(true);
	}

	// component methods

	@Override
	void situate(Situation situation) {
		this.situation = situation;
		scrollbarModel = situation.models().scrollbarModel();
	}

	@Override
	void place(Place place, int z) {
	}

	@Override
	Composition composition() {
		return Composition.FILL;
	}

	@Override
	Changes changes() {
		return dirty || model != null && model.revision() != blueprint.revision ? Changes.CONTENT : Changes.NONE;
	}

	@Override
	void render() {
		if (situation.dirty() || changes() == Changes.CONTENT) {
			VisualSpec spec = situation.visualSpec();
			Canvas canvas = situation.defaultPane().canvas();
			if (model == null) {
				canvas.color(Argb.WHITE).fill();
			} else {
				// ensure we have a blueprint
				Place place = situation.place();
				updateBlueprint(place.innerDimensions);

				// collect state
				IntRect bounds = place.innerBounds;
				int viewWidth = bounds.width();
				int viewHeight = bounds.height();
				IntMargins selectorMargins = IntMargins.widths(blueprint.selectorGap, 0, 0, 0);
				boolean focused = situation.isFocused();

				// prepare background
				//TODO should support more backgrounds
				canvas.pushState();
				canvas.composer(src);
				canvas.color(blueprint.fundamentalStyle.colorBg()).fill();
				canvas.popState();

				// iterate over sections
				int sectionCount = blueprint.sectionCount;
				int top = - Math.min(position.y, Math.max(blueprint.docHeight - bounds.height(), 0));
				for (int i = 0; i < sectionCount; i++) {
					Section section = blueprint.sections[i];
					int blockHeight = section.height;
					Style blockStyle = section.style;
					int bottom = top + blockHeight;
					if (top <= viewHeight && bottom >= 0) { // visible
						Canvas.State canvasState = canvas.recordState();
						IntRect blockArea = IntRect.rectangle(0, top, viewWidth, blockHeight);
						canvas.intOps().clipRectAndTranslate(blockArea);
						canvas.color(blockStyle.colorBg()).fill();
						IntMargins blockMargins = blockStyle.margins();
						if (section.showAction) {
							//TODO need to be able to create selector based on color
							Selector selector = new Selector(spec, false);
							//TODO need to base active on document position
							boolean active = position.action().isPresent() && position.action().get().equals(section.action);
							boolean selectable = section.action.enabled();
							State state = Selector.State.of(selectable, active);
							//TODO should be based on line ascent - record in blueprint?
							selector.render(canvas, IntVector.to(0, -blockMargins.minY + 6 - selector.height()), state, focused);
						}
						//TODO need to check if rectangle 'disappears'
						IntRect contentArea = blockArea.translatedToOrigin().minus(blockMargins).minus(selectorMargins);
						section.content.render(canvas, contentArea);
						canvasState.restoreStrictly();
					}
					top = bottom;
				}
			}
			dirty = false;
		}
	}

	@Override
	Optional<Focusing> focusing() {
		return Optional.of(focusing);
	}

	@Override
	Optional<Eventing> eventing() {
		return Optional.of(eventing);
	}

	@Override
	Optional<Scrolling> scrolling() {
		return Optional.of(scrolling);
	}

	@Override
	Optional<Pointing> pointing() {
		return Optional.of(pointing);
	}

	// private helper methods

	// can only be called after placed
	private void move(boolean up) {
		if (model == null) return;
		if (!situation.isActive()) throw new IllegalStateException("not active");
		IntDimensions viewSize = situation.place().innerDimensions;
		updateBlueprint(viewSize);
		if (blueprint.sectionCount == 0) return; // trivial case, empty document
		int viewHeight = viewSize.height;
		int y = Math.min(position.y, blueprint.docHeight - viewHeight); // position may not be aligned to the top of the document
		int delta = up ?
				- Math.min(viewHeight, y) :
				  Math.min(viewHeight, blueprint.docHeight - y)
				;
		delta += y - position.y; // account for the adjustment above
		if (delta == 0) return; //can't move, we're already at the end of the document
		boolean moved = blueprint.movePosition(position, delta);
		if (moved) {
			dirty = true;
			situation.requestRedrawNow();
			updateAction();
			updateScrolling();
		}
	}

	private void updateBlueprint(IntDimensions viewSize) {
		if (model == null) {
			blueprint = null;
		} else if (blueprint == null || blueprint.revision != model.revision()) {
			blueprint = model.blueprint(situation.visualSpec(), viewSize);
			if (position == null) {
				position = blueprint.newPosition();
			} else {
				blueprint.refreshPosition(position, false);
			}
			updateAction();
			updateScrolling();
		}
	}

	private void updateAction() {
		situation.currentAction( Optional.ofNullable(position).flatMap(Position::action).map(ActionModel::action).orElse(null) );
	}

	private void updateScrolling() {
		if (blueprint == null) {
			scrollbarModel.update(0, 0, 0, 0);
		} else {
			int viewHeight = blueprint.viewSize.height;
			int y = Math.min(position.y, blueprint.docHeight - viewHeight);
			scrollbarModel.update(0, blueprint.docHeight, y, y + viewHeight);
		}
	}
}
