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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.micro.Selector.State;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.Typeface;
import com.superdashi.gosper.studio.Canvas.IntOps;
import com.superdashi.gosper.studio.Canvas.IntTextOps;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntRange;
import com.tomgibara.intgeom.IntRect;

//TODO always reports a change when navigating, want to avoid this
public class Table extends Component {

	private static final boolean DEBUG_TABLE = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.micro.Table.DEBUG"));
	private static final int SAFETY_LIMIT = 50;

	private final Focusing focusing = new Focusing() {

		@Override
		public List<IntRect> focusableAreas() {
			// find out how many rows we have
			return rowAreaList();
		}

		@Override
		public IntRect focusArea() {
			if (activeIndex == -1) return bounds;
			int i = activeIndex - scrollbarStart();
			if (i >= fullyVisibleRows) return bounds;
			return rowAreas()[i];
		}

		@Override
		public void receiveFocus(int areaIndex) {
			if (areaIndex >= 0) {
				activeIndex(scrollbarStart() + areaIndex);
			}
			if (activeIndex == -1) {
				// we have to choose a row to have focus if there is one
				activeIndex(0);
			}
			if (activeIndex != -1) {
				// ensure active row gets repainted
				focusChange = activeIndex - scrollbarStart();
			}
		}

		@Override
		public void cedeFocus() {
			focusChange = activeIndex - scrollbarStart();
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
			case Event.KEY_UP: return previousActive();
			case Event.KEY_DOWN: return nextActive();
			case Event.KEY_CONFIRM :
				Action action = situation.currentAction();
				if (action == null) return false;
				situation.instigate(action);
				return true;
			}
			return false;
		}
	};

	private final Pointing pointing = new Pointing() {
		@Override
		public List<IntRect> clickableAreas() {
			return rowAreaList();
		}

		@Override
		public boolean clicked(int areaIndex, IntCoords coords) {
			situation.focus();
			activeIndex(scrollbarStart() + areaIndex);
			situation.instigateCurrentAction();
			return true;
		}
	};

	private final Scrolling scrolling = new Scrolling() {
		@Override
		public ScrollbarModel scrollbarModel() {
			return scrollbarModel;
		}
	};

	private final DisplayColumns columns;
	private ScrollbarModel scrollbarModel;
	//TODO could eliminate this reference to bounds
	private IntRect bounds;
	private int rowHeight;
	private int visibleRows;
	private int fullyVisibleRows;
	private Selector selector;
	private TableModel emptyModel;
	private TableModel model;
	private String emptyMessage = ""; //TODO should markdown this when support is available
	private long lastRevision = -1L;
	private Row[] lastRows;
	private String lastEmptyMessage = null;
	private int activeIndex = -1;
	private Object activeItem = null; //TODO can probably get rid of this?
	private IntRect[] rowAreas; // lazily computed
	private int focusChange = -1;

	Table(DisplayColumns columns) {
		if (columns == null) throw new IllegalArgumentException("null columns");
		this.columns = columns;
	}

	// accessors

	public int pageIndex() {
		syncScrollbarModel(currentPageIndex());
		return scrollbarStart();
	}

	// may only be called in active state
	public boolean pageIndex(int pageIndex) {
		return navigatePage(pageIndex);
	}

	// may only be called in active state
	public boolean nextPage() {
		return navigatePage(currentPageIndex() + fullyVisibleRows);
	}

	// may only be called in active state
	public boolean previousPage() {
		return navigatePage(currentPageIndex() - fullyVisibleRows);
	}

	public int activeIndex() {
		return activeIndex;
	}

	public boolean activeIndex(int activeIndex) {
		return navigateActive(activeIndex);
	}

	public boolean nextActive() {
		return navigateActive(activeIndex + 1);
	}

	public boolean previousActive() {
		return navigateActive(activeIndex - 1);
	}

	public Object activeData() {
		return activeItem;
	}

	public TableModel model() {
		return model == emptyModel ? null : model;
	}

	public void model(TableModel model) {
		if (model == null) model = emptyModel;
		if (model == this.model) return;
		this.model = model;
		desync();
	}

	public void emptyMessage(String emptyMessage) {
		this.emptyMessage = emptyMessage == null ? "" : emptyMessage;
	}

	public String emptyMessage() {
		return emptyMessage;
	}

	// component methods

	@Override
	void situate(Situation situation) {
		this.situation = situation;
		Models models = situation.models();
		emptyModel = models.emptyTableModel();
		scrollbarModel = models.scrollbarModel();
		model = emptyModel;
	}

	@Override
	void place(Place place, int z) {
		VisualSpec spec = situation.visualSpec();
		bounds = place.innerBounds;
		rowHeight = spec.metrics.tableRowHeight;
		visibleRows = (bounds.height() + rowHeight - 1) / rowHeight;
		fullyVisibleRows = bounds.height() / rowHeight;
		selector = new Selector(spec, false);
		lastRows = new Row[visibleRows];
	}

	@Override
	Composition composition() {
		return Composition.FILL;
	}

	@Override
	IntRect bounds() {
		return bounds;
	}

	@Override
	Changes changes() {
		return lastRevision == model.rows.revision() && focusChange == -1 ? Changes.NONE : Changes.CONTENT;
	}

	@Override
	//TODO could pre-compute widths, and badge counts, and supply relevant column subset
	void render() {
		VisualSpec spec = situation.visualSpec();
		boolean dirty = situation.dirty();
		if (dirty) {
			//TODO theoretically could be avoided by rendering only below row heights on first render
			situation.defaultPane().canvas().color(spec.theme.contentBgColor).fill();
		}

		// collect row data
		long renderedRevision;
		Row[] rows;
		int pageIndex = currentPageIndex();
		Rows modelRows = model.rows;
		if (DEBUG_TABLE) Debug.logging().message("Rendering empty table model? {}").values(model == emptyModel).log();
		{
			int count = 0;
			while (true) {
				if (count++ >= SAFETY_LIMIT) {
					if (DEBUG_TABLE) Debug.logging().message("bailing out after {} attempts").values(count).log();
					return;
				}
				renderedRevision = modelRows.revision();
				if (renderedRevision == lastRevision) {
					// no model change, but there could have been an external change (eg, focus)
					rows = null;
					break;
				}
				int size = modelRows.size(renderedRevision);
				if (size == -1) continue; // revision has changed
				updateScrollbarModel(pageIndex, size, false);
				int requiredLength = scrollbarModel.span().unitSize();
				rows = new Row[requiredLength];
				boolean okay = modelRows.populateRows(renderedRevision, scrollbarModel.span().min, columns, rows);
				if (!okay) continue; // revision has changed
				break; // we have the data we need
			}
		}
		int start = scrollbarModel.span().min;
		int finish = scrollbarModel.span().max;
		if (rows == null) {
			if (DEBUG_TABLE) Debug.logging().message("No change in table rows, has focus changed? {}").values(focusChange).log();
			if (!dirty && focusChange == -1) return; // there is nothing to do
			rows = lastRows;
		} else {
			if (DEBUG_TABLE) Debug.logging().message("{} table rows obtained").values(rows.length).log();
			if (activeIndex >= start && activeIndex < finish) {
				Row row = rows[activeIndex - start];
				if (DEBUG_TABLE) Debug.logging().message("Active table row is {}").values(row).log();
				row.active = true;
				activeItem = row.item;
				situation.currentAction(row.selectable ? row.action : null); //TODO get rid of selectable?
			} else {
				if (DEBUG_TABLE) Debug.logging().message("No active table row").log();
				activeItem = null;
				situation.currentAction(null);
			}
		}
		// check if rows need shifting to make last visible
		if (rows.length != 0 && rows.length == visibleRows && visibleRows != fullyVisibleRows) {
			Row row = rows[rows.length - 1];
			if (row != null && row.active) {
				for (int i = 0; i < rows.length - 1; i++) {
					rows[i] = rows[i + 1];
				}
				rows[rows.length - 1] = null;
			}
		}

		Canvas canvas = situation.defaultPane().canvas();
		Typeface typeface = spec.typeface;

		// deal with emptiness
		String newEmptyMessage;
		if (rows.length == 0 || rows[0] == null) { // null element check needed in case rows copies lastRows
			// dealing with an empty display
			newEmptyMessage = emptyMessage;
			if (!dirty && newEmptyMessage.equals(lastEmptyMessage)) {
				/* nothing to do - displaying same empty message */
				if (DEBUG_TABLE) Debug.logging().message("Empty message already visible, no change").log();
			} else {
				// special case, new empty message/state, render it
				if (DEBUG_TABLE) Debug.logging().message("Rendering empty message").log();
				TextStyle textStyle = TextStyle.regular();
				int lineHeight = spec.metrics.lineHeight;
				int maxLines = bounds.height() / lineHeight;
				int width = bounds.width();
				List<String> lines = spec.splitIntoLines(typeface, textStyle, emptyMessage.trim(), width, maxLines);
				IntTextOps text = canvas
						.color(spec.theme.contentBgColor).fill()
						.color(spec.theme.textualColor).intOps()
						.newText(typeface).moveTo(0, spec.typeMetrics.fontMetrics(textStyle).baseline);
				for (int i = 0; i < lines.size(); i++) {
					text.renderString(textStyle, lines.get(i)).moveBy(0, lineHeight);
				}
			}
		} else {
			if (DEBUG_TABLE) Debug.logging().message("Rendering table rows ({} rows)").values(rows.length).log();
			// dealing with a populated display
			newEmptyMessage = null;
			if (lastEmptyMessage != null) {
				// first time rendering rows since empty, clear the empty message first
				canvas.color(spec.theme.contentBgColor).fill();
			}
			renderTableContent(rows);
		}

		// update last info if we used new info
		if (lastRows != rows) {
			lastRevision = renderedRevision;
			for (int i = 0; i < lastRows.length; i++) {
				lastRows[i] = i>= rows.length ? null : rows[i];
			}
		}

		// record any displayed empty message
		lastEmptyMessage = newEmptyMessage;

		// clear the focusChange
		focusChange = -1;
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

	@Override
	Optional<Scrolling> scrolling() {
		return Optional.of(scrolling);
	}

	// private utility methods

	private void desync() {
		lastRevision = -1L;
	}

	private int currentPageIndex() {
		return scrollbarModel == null ? activeIndex : scrollbarStart();
	}

	// called after pageIndex update, so scrollbarModel is guaranteed
	private void nudgeActiveIndex() {
		if (scrollbarModel.span().unitSize() == 0) {
			activeIndex = -1;
			return;
		}
		IntRange span = scrollbarModel.span();
		if (activeIndex < span.min) {
			activeIndex = span.min;
		} else if (activeIndex >= span.max) {
			activeIndex = span.max - 1;
		}
	}

	private void nudgePageIndex() {
		syncScrollbarModel(activeIndex);
		if (scrollbarModel.range().unitSize() == 0) {
			activeIndex = -1;
			return;
		}
		IntRange span = scrollbarModel.span();
		if (activeIndex < span.min) {
			syncScrollbarModel(activeIndex);
			activeIndex = span.min;
		} else if (activeIndex >= span.max) {
			syncScrollbarModel(activeIndex - (fullyVisibleRows - 1));
			activeIndex = span.max - 1;
		}
	}

	private void syncScrollbarModel(int pageIndex) {
		long revision;
		int size;
		Rows rows = model.rows;
		while (true) {
			revision = rows.revision();
			size = rows.size(revision);
			if (size >= 0) break;
		}
		updateScrollbarModel(pageIndex, size, true);
	}

	private void updateScrollbarModel(int pageIndex, int size, boolean fullyVisible) {
		if (DEBUG_TABLE) Debug.logging().message("Updating table scrollbar based on page index {}, size {}, fullyVisible {}").values(pageIndex, size, fullyVisible).log();
		int length = fullyVisible ? fullyVisibleRows : visibleRows;
		int end = Math.min(size, pageIndex + length);
		int start = Math.max(0, end - length);
		if (DEBUG_TABLE) Debug.logging().message("Setting table scrollbar to range [{},{}] and span [{},{}]").values(0, size, start, end).log();
		scrollbarModel.update(0, size, start, end);
	}

	private IntRect[] rowAreas() {
		if (rowAreas == null) {
			int tableWidth = bounds.width();
			rowAreas = new IntRect[fullyVisibleRows];
			for (int i = 0; i < fullyVisibleRows; i++) {
				rowAreas[i] = IntRect.rectangle(bounds.minX, bounds.minY + i * rowHeight, tableWidth, rowHeight);
			}
		}
		return rowAreas;
	}

	private List<IntRect> rowAreaList() {
		int i = 0;
		while (i < fullyVisibleRows && lastRows[i] != null) i++;
		return i == 0 ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(rowAreas()).subList(0, i));
	}

	private boolean navigatePage(int index) {
		if (!situation.isPlaced()) return false;
		int previous = activeIndex;
		syncScrollbarModel(index);
		nudgeActiveIndex();
		boolean changed = previous != activeIndex;
		if (changed) {
			desync();
			situation.requestRedrawNow();
		}
		return changed;
	}

	private boolean navigateActive(int index) {
		if (!situation.isPlaced()) {
			if (index == activeIndex) return false;
			activeIndex = index;
			return true;
		}
		int previous = activeIndex;
		activeIndex = index;
		if (scrollbarModel.span().containsUnit(activeIndex) && lastRows != null) {
			Row row = lastRows[activeIndex - scrollbarStart()];
			situation.currentAction(row == null ? null : row.action);
		}
		nudgePageIndex();
		desync();
		situation.requestRedrawNow();
		boolean changed = previous != activeIndex;
		if (changed) {
			desync();
			situation.requestRedrawNow();
		}
		return changed;
	}

	//TODO could pre-compute widths, and badge counts, and supply relevant column subset
	private void renderTableContent(Row[] rows) {
		boolean dirty = situation.dirty();
		// compute widths
		VisualSpec spec = situation.visualSpec();
		int tableWidth = bounds.width();
		int labelStart = columns.navigation ? (rowHeight + spec.metrics.sideMargin) : 0; //TODO side margin here is a hack
		int maxBadgeWidth = tableWidth - labelStart - 1;
		int maxBadgeCount = columns.badges.size();
		int badgeWidth = spec.metrics.badgeSize;
		int badgeHeight = spec.metrics.badgeSize;
		int badgeCount = Math.min(maxBadgeCount, maxBadgeWidth / (badgeWidth + 1));
		int badgesWidth = 1 + badgeCount * (badgeWidth + 1);
		int labelEnd = Math.max(labelStart, tableWidth - badgesWidth);
		IntRect[] badgeBounds = new IntRect[badgeCount];
		for (int i = 0; i < badgeCount; i++) {
			badgeBounds[i] = IntRect.rectangle(labelEnd + 1 + (badgeWidth + 1) * i, 1, badgeWidth, badgeHeight);
		}
		int selectorHeight = selector.height();
		int selectorWidth = selector.width();
		Canvas canvas = situation.defaultPane().canvas();
		IntTextOps text = canvas.intOps().newText(spec.typeface);
		Canvas.State canvasState = canvas.recordState();

		// render content
		boolean focused = situation.isFocused();
		for (int i = 0; i < visibleRows; i++) {
			Row row = i >= rows.length ? null : rows[i];
			Row lastRow = lastRows[i];
			if (!dirty && i != focusChange && Objects.equals(row, lastRow)) {
				if (DEBUG_TABLE) Debug.logging().message("Skipping row {}, no change").values(i).log();
				continue; // no change
			}
			int y = i * rowHeight;
			IntRect rowBounds = IntRect.rectangle(0, y, tableWidth, rowHeight);
			IntOps intOps = canvas.color(spec.theme.contentBgColor).intOps();
			if (row == null) { // render blank row
				if (DEBUG_TABLE) Debug.logging().message("Rendering blank row {}").values(i).log();
				//TODO could be smarter here - first null indicates rest are null and we might be able to combine multiple rects
				intOps.fillRect(rowBounds);
			} else { // render row
				if (DEBUG_TABLE) Debug.logging().message("Rendering populated row {}").values(i).log();
				//TODO could render only row part that has changed
				int baseline = y + spec.typeMetrics.fontMetrics(TextStyle.regular()).baseline;
				intOps.fillRect(rowBounds);
				// render select
				if (columns.navigation) {
					State state = State.of(row.selectable, row.active);
					IntRect selectorBounds = IntRect.rectangle(
							(rowHeight - selectorWidth) / 2,
							baseline - selectorHeight,
							selectorWidth,
							selectorHeight
							);
					selector.render(canvas, selectorBounds, state, focused);
				} // render label
				if (columns.label) {
					int labelWidth = labelEnd - labelStart;
					canvas.color(spec.theme.textualColor);
					String label = row.item.label().orElse("");
					TextStyle textStyle = TextStyle.with(false, row.placeholderLabel,false);
					String line = spec.accommodatedString(textStyle, label, labelWidth);
					text.moveTo(labelStart, baseline).renderString(textStyle, line);
				}
				if (badgeCount > 0) { // render badges
					List<Badge> badges = columns.badges.subList(maxBadgeCount - badgeCount, maxBadgeCount);
					for (int j = 0; j < badgeCount; j++) {
						canvas.pushState();
						try {
							canvas.intOps().clipRectAndTranslate(badgeBounds[j].translatedBy(0, y));
							badges.get(j).render(row.item, canvas);
						} finally {
							canvas.popState();
						}
					}
				}
			}
		}

		canvasState.restoreStrictly();
	}

	private int scrollbarStart() {
		return scrollbarModel.span().min;
	}

	// inner classes

	public static final class Row {

		private final Item item;
		private final boolean selectable;
		private final boolean placeholderLabel;
		private final Action action;
		private boolean active;

		public Row(Item item, boolean selectable, boolean placeholderLabel, Action action) {
			if (item == null) throw new IllegalArgumentException("null item");
			this.item = item;
			this.selectable = selectable;
			this.placeholderLabel = placeholderLabel;
			this.action = action;
		}

		@Override
		public int hashCode() {
			return item.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Table.Row)) return false;
			Table.Row that = (Table.Row) obj;
			return
					this.active == that.active &&
					this.item.equals(that.item) &&
					Objects.equals(this.action, that.action);
		}

		public String toString() {
			return item.label().orElse("");
		}
	}

}
