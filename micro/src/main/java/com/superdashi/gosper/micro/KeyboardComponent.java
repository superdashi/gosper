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
import java.util.Optional;
import java.util.stream.Collectors;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.device.KeySet;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.micro.KeyboardDesign.Key;
import com.superdashi.gosper.micro.KeyboardDesign.ModelOp;
import com.superdashi.gosper.micro.KeyboardDesign.State;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.ClearPlane;
import com.superdashi.gosper.studio.Composer;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.IntFontMetrics;
import com.superdashi.gosper.studio.Shader;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.Typeface;
import com.superdashi.gosper.studio.TypefaceMetrics;
import com.superdashi.gosper.studio.Xor;
import com.superdashi.gosper.studio.Canvas.IntOps;
import com.superdashi.gosper.studio.Canvas.IntTextOps;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDir;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntRange;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntRectNavigator;
import com.tomgibara.intgeom.IntRectNavigator.Algorithm;
import com.tomgibara.intgeom.IntRectNavigator.TaggedRect;

//TODO keyboard should show 'key pressed'
//TODO does this component need to support keyboard based entry?
//TODO maybe have multiple keyboard component implementations?
class KeyboardComponent extends Component {

	private static final Composer xor = new Xor().asComposer();

	private static void drawKeyBox(Canvas canvas, IntRect rect, IntMargins margins) {
		IntRect inner = rect;
		IntRect outer = inner.plus(margins);
		IntRect pts = inner.plus(IntMargins.widths(0, -1, 0, -1));
		IntOps intOps = canvas.intOps();
		if (margins.minX < 0) intOps.fillRect(IntRect.bounded(outer.minX, inner.minY, inner.minX, inner.maxY)); // left
		if (margins.maxX > 0) intOps.fillRect(IntRect.bounded(inner.maxX, inner.minY, outer.maxX, inner.maxY)); // right
		if (margins.minY < 0) intOps.fillRect(IntRect.bounded(outer.minX, outer.minY, outer.maxX, inner.minY)); // top
		if (margins.maxY > 0) intOps.fillRect(IntRect.bounded(outer.minX, inner.maxY, outer.maxX, outer.maxY)); // bottom

		intOps.plotPixel(pts.minX, pts.minY); // tl
		intOps.plotPixel(pts.maxX, pts.minY); // tr
		intOps.plotPixel(pts.maxX, pts.maxY); // br
		intOps.plotPixel(pts.minX, pts.maxY); // bl
	}

	private static void drawEdgeBox(Canvas canvas, IntRect rect, IntMargins margins) {
		IntRect inner = rect.plus(margins);
		IntMargins forced = margins.largerOf(IntMargins.uniform(1));
		IntRect outer = inner.plus(forced);
		IntRect pts = inner.plus(IntMargins.widths(1, 0, 1, 0));
		IntOps intOps = canvas.intOps();
		intOps.fillRect(IntRect.bounded(outer.minX, inner.minY, inner.minX, inner.maxY)); // left
		intOps.fillRect(IntRect.bounded(inner.maxX, inner.minY, outer.maxX, inner.maxY)); // right
		intOps.fillRect(IntRect.bounded(inner.minX, outer.minY, inner.maxX, inner.minY)); // top
		intOps.fillRect(IntRect.bounded(inner.minX, inner.maxY, inner.maxX, outer.maxY)); // bottom

		intOps.plotPixel(pts.minX, pts.minY); // tl
		intOps.plotPixel(pts.maxX, pts.minY); // tr
		intOps.plotPixel(pts.maxX, pts.maxY); // br
		intOps.plotPixel(pts.minX, pts.maxY); // bl
	}

	private final Focusing focusing = new Focusing() {

		@Override
		public IntRect focusArea() {
			return bounds();
		}

	};

	private final Eventing eventing = new Eventing() {

		@Override
		public EventMask eventMask() {
			return EventMask.anyKey();
		}

		@Override
		public boolean handleEvent(Event event) {
			boolean inTextArea = !model.activeKey().isPresent();
			if (event.isDown()) {
				switch (event.key) {
				case Event.KEY_CONFIRM :
					if (model.activeKey().isPresent()) {
						activating = true;
						situation.requestRedrawNow();
					}
					break;
				}
			} else {
				switch (event.key) {
				case Event.KEY_LEFT    :
					if (inTextArea && model.moveCaret(-1) != 0) return true;
					return activateInDirection(IntDir.LESS_X);
				case Event.KEY_RIGHT   :
					if (inTextArea && model.moveCaret(1) != 0) return true;
					return activateInDirection(IntDir.MORE_X);
				case Event.KEY_UP      : return activateInDirection(IntDir.LESS_Y);
				case Event.KEY_DOWN    : return activateInDirection(IntDir.MORE_Y);
				case Event.KEY_CONFIRM : //TODO should be on key down?
					if (model.activeKey().isPresent()) {
						activating = false;
						situation.requestRedrawNow();
					}
					return operateModel();
				}
			}
			return false;
		}

		private boolean activateInDirection(IntDir dir) {
			if (model == null) return false;
			IntRect start = model.activeKey().map(k -> k.face).orElse(caretRect);
			Algorithm algo = start == caretRect && dir.axis.vertical ? Algorithm.NATURAL : Algorithm.PREFER_STRICT;
			Optional<TaggedRect<Key>> result = navigators[model.state().ordinal].findFrom(start, dir, algo);
			if (!result.isPresent()) return false;
			Key key = result.get().tag;
			if (key == null) { // indicates textArea
				model.deactivateKey();
			} else {
				model.activateKey(key);
			}
			return true;
		}
	};

	private final Pointing pointing = new Pointing() {
		@Override
		public List<IntRect> clickableAreas() {
			if (navigators == null) return Collections.emptyList();
			return currentTaggedRects().stream().map(tr -> tr.rect).collect(Collectors.toList());
		}

		public boolean clicked(int areaIndex, IntCoords coords) {
			if (areaIndex == -1) return false;
			Key key = currentTaggedRects().get(areaIndex).tag;
			if (key == null) {
				// the user has pointed at the text
				IntCoords cs = coords.relativeTo(textArea);
				if (model.design.textLines == 1) {
					//TODO should choose nearest split, not leftmost
					model.caret( typeMetrics.accommodatedCharCount(TextStyle.regular(), model.text(), cs.x + textOffset, 0) );
				} else {
					//TODO this is a bit hacky, we have to count up to the caret, then count up to the point, then move the difference
					// check if point is beyond end
					int p = cs.y / lineHeight;
					if (p >= lines.length) {
						model.moveCaretToEnd();
					} else {
						// count up to caret
						int ct = model.caret() - caretOffset;
						for (int i = 0; i < caretLine; i++) {
							ct += lines[i].length();
						}
						// count up to point
						int pt = typeMetrics.accommodatedCharCount(TextStyle.regular(), lines[p], cs.x, 0);
						for (int i = 0; i < p; i++) {
							pt += lines[i].length();
						}
						model.moveCaret(pt - ct);
					}
				}
				model.deactivateKey();
			} else {
				model.activateKey(key);
				operateModel();
			}
			return true;
		}

		private List<TaggedRect<Key>> currentTaggedRects() {
			return navigators[model.state().ordinal].allTaggedRects();
		}
	};

	private final ItemModel info;
	private KeyboardModel model = null;
	private KeyboardModel visible = null;
	private Shader fallbackShader;
	private Frame background;
	private IntRectNavigator<Key>[] navigators;
	private IntRect textArea;
	private IntRect caretRect;
	private int textOffset = 0; // the horizontal offset in pixels (1 line) or the index of the first rendered line (2+ lines)
	private int caretLine = 0; // the line that contains the caret (relative to the top line) (2+ lines only)
	private int caretOffset = 0; // the number of characters from the start of the line (2+ lines only)
	private String[] lines;

	// config based on situation
	private boolean showActiveKey;

	// used to render text
	private Typeface typeface;
	private TextStyle textStyle = TextStyle.regular();
	private TypefaceMetrics typeMetrics;
	private IntFontMetrics fontMetrics;
	private int lineHeight;

	private boolean activating;
	private boolean visiblyActivating;


	KeyboardComponent(ItemModel info) {
		this.info = info;
	}

	void model(KeyboardModel model) {
		if (model == this.model) return;
		this.model = model;
		textArea = model.design.textArea;
		activating = false;
		visiblyActivating = false;
		buildNavigators();
		computeCaretRect();
		this.visible = null;
	}

	KeyboardModel model() {
		return model;
	}

	// component methods

	@Override
	void situate(Situation situation) {
		super.situate(situation);
		KeySet keySet = situation.deviceSpec().keyboard.keySet;
		showActiveKey = keySet.containsAllKeysInRange(Event.KEY_UP, Event.KEY_RIGHT) && keySet.containsKey(Event.KEY_CONFIRM);
		VisualSpec spec = situation.visualSpec();
		typeface = spec.typeface;
		typeMetrics = spec.typeMetrics;
		fontMetrics = typeMetrics.fontMetrics(textStyle);
		lineHeight = spec.metrics.lineHeight;
		//TODO should come from context?
		fallbackShader = Background.mono(2).asShader().get();
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
		return visible == null || model.differenceFrom(visible).any || activating != visiblyActivating ? Changes.CONTENT : Changes.NONE;
	}

	@Override
	void render() {
		VisualTheme theme = situation.visualSpec().theme;
		Canvas canvas = situation.defaultPane().canvas();

		if (model == null) { // bail out
			canvas.shader(fallbackShader).fill();
			return;
		}
		KeyboardModel visible = model.snapshot();

		Optional<Frame> image = model.backdrop();
		if (!image.isPresent()) { // bail out
			canvas.shader(fallbackShader).fill();
			return;
		}

		ensureBackground();
		canvas.drawFrame(background);

		canvas.drawFrame(image.get());

		if (showActiveKey) {
			visible.activeKey().ifPresent( k -> {
					IntRect rect = k.face;
//					if (activating) rect = rect.plus(k.border);
//					canvas.pushState();
//					//TODO should be controllable
//					canvas.color(Argb.BLACK);
//					canvas.composer(xor);
//					canvas.intOps().fillRect(rect);
//					canvas.popState();
					drawKeyBox(canvas.color(Argb.WHITE), rect, k.border);
					drawEdgeBox(canvas.color(theme.keyActiveColor), rect, k.border);
				});
		}

		int textLines = model.design.textLines;
		if (textLines == 1) {
			caretLine = 0;
			caretOffset = 0;
			computeCaretRect();
			if (caretRect.minX > textArea.maxX) {
				textOffset = caretRect.centerX() - textArea.centerX();
				computeCaretRect();
			} else if (caretRect.maxX < textArea.minX) {
				textOffset = Math.max(caretRect.centerX() - textArea.centerX(), 0);
				computeCaretRect();
			}
			lines = new String[] { model.text() };
			caretLine = 0;
		} else {
			caretLine = -1;
			caretOffset = 0;
			int caret = model.caret();
			String text = model.text();
			int width = model.design.textArea.width();
			List<String> list = new ArrayList<>();
			while (true) {
				int count = typeMetrics.accommodatedCharCount(TextStyle.regular(), text, width, 0);
				if (caretLine == -1 && caret - caretOffset <= count) { // record the line that contains the caret
					caretLine = list.size();
				}
				boolean fits = count == text.length();
				String line = fits ? text : text.substring(0, count);
				list.add(line);
				if (caretLine == -1) { // we're still seeking the line that contains the caret
					caretOffset += count;
				}
				if (fits) { // the whole line fits - we're done
					assert caretLine != -1;
					break;
				}
				text = text.substring(count);
			}
			// choose valid textOffset closest to previous value that leaves caret line visible
			IntRange full = IntRange.bounded(0, list.size());
			IntRange range = IntRange.atMaximum(caretLine + 1, textLines).intersection(full).get(); // exclusive
			textOffset = range.clampUnit(textOffset);
			caretLine -= textOffset;
			int limit = Math.min(textOffset + textLines, list.size());
			lines = list.subList(textOffset, limit).toArray(new String[limit - textOffset]);
			computeCaretRect();
		}
		int baseline = fontMetrics.baseline + 1;
		canvas.pushState()
			.color(model.activeKey().isPresent() ? theme.caretPassiveColor : theme.caretActiveColor)
			.intOps()
			.clipRect(textArea)
			.fillRect(caretRect);
		IntTextOps text = canvas
			.color(Argb.BLACK) //TODO needs to come from keyboard design (?)
			.intOps()
			.newText(typeface);
		int caret = model.caret() - caretOffset;
		for (int i = 0; i < lines.length; i++) {
			text.moveTo(1 - textOffset, baseline);
			String line = lines[i];
			if (i == caretLine) {
				text
					.renderString(textStyle, line.substring(0, caret))
					.moveTo(caretRect.maxX + 1, baseline)
					.renderString(textStyle, line.substring(caret));
			} else {
				text.renderString(textStyle, line);
			}
			baseline += lineHeight;
		}
		canvas.popState();

		this.visible = visible;
		visiblyActivating = activating;
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
	Optional<Pointing> pointing() {
		return Optional.of(pointing);
	}

	private void ensureBackground() {
		if (background != null) return;
		if (model.design.opaqueBackdrops) {
			background = ClearPlane.instance().view(bounds());
		} else {
			VisualSpec spec = situation.visualSpec();
			background = spec.theme.keyboardBackground.adaptedFor(spec).generate(bounds());
		}
	}

	// TODO hacky?

	private void buildNavigators() {
		if (model == null) {
			navigators = new IntRectNavigator[0];
		} else {
			List<State> states = model.design.states;
			int stateCount = states.size();
			navigators = new IntRectNavigator[stateCount];
			for (State state : states) {
				List<TaggedRect<Key>> rects = model.design.keysForState(state).map(k -> new TaggedRect<>(k.face, k)).collect(Collectors.toList());
				rects.add(new TaggedRect<KeyboardDesign.Key>(textArea, null));
				navigators[state.ordinal] = new IntRectNavigator<>(rects);
			}
		}
	}

	private void computeCaretRect() {
		//TODO make 1 a constant in visual context
		String left = caretOffset == 0 ? model.textBeforeCaret() : model.text().substring(caretOffset, model.caret());
		int x = -textOffset + 1 + textArea.minX + typeMetrics.intRenderedWidthOfString(textStyle, left) + 1;
		int baseline = textArea.minY + fontMetrics.baseline + 1;
		if (caretLine != 0) baseline += lineHeight * caretLine;
		int width = model.activeKey().isPresent() ? 1 : 2;
		caretRect = IntRect.rectangle(x, baseline - fontMetrics.top, width, fontMetrics.top + fontMetrics.bottom);
	}

	private boolean operateModel() {
		ModelOp op = model.activeOperation();
		if (op.isConfirm()) {
			if (model.valid()) {
				Action action = Action.create(Action.ID_CHANGE_VALUE, info.item);
				situation.instigate(action);
				return true;
			} else {
				//TODO show info screen if available
				return true;
			}
		}
		if (op.isCancel()) {
			//TODO need to formulate an action
			//response.instigate(action);
			return true;
		}
		return op.operateOn(model);
	}

}
