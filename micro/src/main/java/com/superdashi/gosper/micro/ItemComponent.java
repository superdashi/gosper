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

import static com.superdashi.gosper.util.Geometry.intRectToRect;
import static com.superdashi.gosper.util.Geometry.roundRectToIntRect;

import java.util.Optional;

import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.micro.Layout.Sizer;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Composer;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.Canvas.State;
import com.superdashi.gosper.studio.PorterDuff.Rule;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntRect;

abstract class ItemComponent extends Component {

	private static final Composer src = new PorterDuff(Rule.SRC).asComposer();
	private static final Composer srcOver = new PorterDuff(Rule.SRC_OVER).asComposer();

	// fields

	private CardDesign defaultDesign;
	private CardDesign design;
	private Places places = null;
	private long revision = -2L;
	private Constraints constraints = null; // computed when placed

	// accessors

	public CardDesign design() {
		return design;
	}

	public void design(CardDesign design) {
		if (design == null) {
			design = defaultDesign;
		} else {
			design = design.immutableCopy();
		}
		if (design == this.design) return;
		this.design = design;
		places = null;
		revision = -1L;
		situation.requestRedrawNow();
	}

	// component methods

	@Override
	void situate(Situation situation) {
		this.situation = situation;
		defaultDesign = defaultDesign();
		//TODO deal with possibility that default design does not fit
		design = defaultDesign;
	}

	@Override
	void place(Place place, int z) {
		constraints = new Constraints(designDimensions(place));
	}

	@Override
	Composition composition() {
		return layouter().isEntire() ? Composition.FILL : Composition.MASK;
	}

	@Override
	Changes changes() {
		return contentDirty() ? Changes.CONTENT : Changes.NONE;
	}

	@Override
	void render() {
		VisualSpec spec = situation.visualSpec();
		Place place = situation.place();
		IntRect bounds = designDimensions(place);
		ItemModel model = itemModel();
		//TODO not thread safe - we need a static copy of the model and the revision
		long revision = model.revision();

		//TODO currently we replace card contents logic
		// this is because if model changes, we can still reuse places
		// not a guarantee that the content system provides
		if (situation.dirty() || changes() != Changes.NONE) {
			Canvas canvas = situation.defaultPane().canvas();
			State state = canvas.recordState();
			canvas.color(place.style.colorBg()).composer(src).fill().composer(srcOver);
			Optional<Position> optPos = design.backgroundPosition();
			Optional<Frame> optPic = model.picture();
			if (optPos.isPresent() && optPic.isPresent()) {
				//Position pos = Position.from(Fit.FREE, Fit.FREE, Alignment2D.pair(place.style.alignmentX(), place.style.alignmentY()));
				Frame pic = optPic.get();
				IntRect picBounds = pic.dimensions().toRect();
				Rect src = intRectToRect(picBounds);
				Rect dst = intRectToRect(bounds);
				Transform transform = optPos.get().transform(src, dst);
				Rect rect = transform.transform(src);
				IntRect target = roundRectToIntRect(rect);
				//TODO support scaling
				canvas.intOps().drawFrame(pic, target.minimumCoords());
			}
			if (places == null) {
				places = layouter().computePlaces(constraints).orElse(Places.none);
			}
			places.stream().forEach(pl -> {
				canvas.color(pl.style.colorBg()).intOps().fillRect(pl.outerBounds);
				Optional<ItemContents> optional = design.contentsAtLocation(pl.location);
				optional.ifPresent(contents -> {
					contents.contentFrom(model)
						.sizeForWidth(spec, pl.style, pl.innerBounds.width())
						.render(canvas, pl.innerBounds);
				});
			});
			state.restoreStrictly();
			decorate(canvas);
			this.revision = revision;
		}
	}

	// package scoped methods

	void resetRevision() {
		revision = -2L;
	}

	CardDesign defaultDesign() {
		//TODO consider being able to cache component generated objects on visual spec?
		// cannot be static because it depends on visual spec
		//TODO is this a good choice?
		int textualColor = situation.visualSpec().theme.textualColor;
		CardDesign design = new CardDesign(Layout.single().withStyle(new Style().colorFg(textualColor)));
		return design.setContentsAtLocation(Location.center, ItemContents.label());
	}

	IntRect designDimensions(Place place) {
		return place.innerDimensions.toRect();
	}

	void decorate(Canvas canvas) {}

	// methods for implementation

	abstract ItemModel itemModel();

	// private helper methods

	private boolean contentDirty() {
		return revision != modelRevision();
	}

	private long modelRevision() {
		ItemModel model = itemModel();
		return model == null ? -1L : model.revision();
	}

	private Sizer layouter() {
		return design.layout().sizer(situation.visualSpec());
	}
}
