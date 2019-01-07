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
package com.superdashi.gosper.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.tomgibara.fundament.Mutability;

//TODO a simplify method would be useful
// merge either adjacent spans with the same style where neither has an id
// merge adjacent spans where both have the same style and share an id
// merge child styles without an id that are implied by their parent
public final class StyledText implements Mutability<StyledText> {

	// statics

	private static Comparator<Span> comp = (a, b) -> a.from - b.from;

	// fields

	private final boolean mutable;
	private final boolean view;
	private StringBuilder flexible;
	private String fixed;
	private Span root;

	// constructors

	public StyledText() {
		this(Style.noStyle(), "");
	}

	public StyledText(String text) {
		this(Style.noStyle(), text);
	}

	public StyledText(Style style, String text) {
		if (style == null) throw new IllegalArgumentException("null style");
		if (text == null) throw new IllegalArgumentException("null text");
		mutable = true;
		fixed = text;
		flexible = null;
		root = new Span(null, style.immutable(), null, 0, text.length());
		view = false;
	}

	// creates a view
	private StyledText(StyledText that, boolean mutable, boolean view) {
		this.view = view;
		this.mutable = mutable;
		if (view) {
			flexible = that.flexible;
			fixed = null;
			root = that.root;
		} else {
			flexible = null;
			fixed = that.fixed == null ? flexible.toString() : that.fixed;
			root = clone(null, that.root);
		}
	}

	// accessors

	public Span root() {
		return root;
	}

	public String text() {
		if (view) return flexible.toString(); // don't cache string, it may change underneath us
		return fixed == null ? fixed = flexible.toString() : fixed;
	}

	// convenience methods

	public int length() {
		return fixed == null ? flexible.length() : fixed.length();
	}

	public boolean isEmpty() {
		return length() == 0;
	}

	public void insertText(int index, String text) {
		root.insertText(index, text);
	}

	public Optional<Span> insertStyledText(int index, Style style, String text) {
		return root.insertStyledText(index, style, text);
	}

	public void appendText(String text) {
		root.insertText(root.to, text);
	}

	public Optional<Span> appendStyledText(Style style, String text) {
		return root.insertStyledText(root.to, style, text);
	}

	public void deleteText(int from, int to) {
		root.deleteText(from, to);
	}

	public void truncateText(int after) {
		root.deleteText(after, root.to);
	}

	// spans and segments

	public boolean isSingleSpan() {
		return root.children.isEmpty();
	}

	public List<Span> spansWithId(String id) {
		if (id == null) throw new IllegalArgumentException("null id");
		List<Span> spans = new ArrayList<>();
		root.spansWithId(id, spans);
		return spans;
	}

	public Span spanContaining(int index) {
		checkIndex(index);
		return findSpan(index, index);
	}

	public Style styleAt(int index) {
		if (index < 0 || index >= root.to) throw new IllegalArgumentException("invalid index");
		return root.buildStyle(index, root.style.mutableCopy()).immutable();
	}

	public Iterable<Segment> segments() {
		return segments(Style.noStyle());
	}

	public Iterable<Segment> segments(Style style) {
		if (style == null) throw new IllegalArgumentException("null style");
		return () -> new Flattener(root, style);
	}

	public Stream<Segment> segmentStream() {
		return segmentStream(Style.noStyle());
	}

	public Stream<Segment> segmentStream(Style style) {
		if (style == null) throw new IllegalArgumentException("null style");
		return StreamSupport.stream(segments().spliterator(), false);
	}

	public Iterable<Span> spans() {
		return () -> new SpanIterator(root);
	}

	// mutability methods

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public StyledText mutableCopy() {
		return new StyledText(this, true, false);
	}

	@Override
	public StyledText immutableCopy() {
		return new StyledText(this, false, false);
	}

	@Override
	public StyledText immutableView() {
		return new StyledText(this, false, true);
	}

	// object methods

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		root.toString(sb, 0);
		return sb.toString();
	}

	// private helper methods

	private String substring(int from, int to) {
		return fixed == null ? flexible.substring(from, to) : fixed.substring(from, to);
	}

	private void checkMutable() {
		if (!mutable) throw new IllegalArgumentException("immutable");
	}

	private void checkIndex(int index) {
		if (index < 0 || index > root.to) throw new IllegalArgumentException("invalid index");
	}

	private Span findSpan(int from, int to) {
		return root.findSpan(from, to);
	}

	private Span clone(Span parent, Span span) {
		Span clone = new Span(parent, span.style, span.id, span.from, span.to);
		for (Span child : span.children) {
			clone.children.add(clone(clone, child));
		}
		return clone;
	}

	private void insertTextImpl(int index, String text) {
		int delta = text.length();
		if (delta == 0) return;
		if (flexible == null) flexible = new StringBuilder(fixed);
		flexible.insert(index, text);
		fixed = null;
		adjust(index, delta);
	}

	private void adjust(int index, int delta) {
		root.adjustSelfAndDescendants(index, delta);
		// special case - growing from empty
		// theoretically, the delta is appended beyond the end of the empty root
		// we don't want this and need to deal with it, we do it here
		if (delta > 0 && root.to == index) root.to += delta;
		assert root.from == 0 && root.to == length();
	}

	public final class Span {

		private Style style;
		private final String id;

		private Span parent;
		private final List<Span> children = new ArrayList<>(0);
		private int from;
		private int to;

		Span(Span parent, Style style, String id, int from, int to) {
			this.parent = parent;
			this.style = style;
			this.id = id;
			this.from = from;
			this.to = to;
			assert from >= 0;
			assert parent == null || to > from;
			assert to <= StyledText.this.length();
		}

		public int from() {
			return from;
		}

		public int to() {
			return to;
		}

		public Optional<Span> parent() {
			checkValid();
			return Optional.of(parent);
		}

		public Optional<String> id() {
			return Optional.of(id);
		}

		public void style(Style style) {
			if (style == null) throw new IllegalArgumentException("null style");
			checkValid();
			checkMutable();
			this.style = style.immutable();
		}

		public Style style() {
			checkValid();
			return style;
		}

		public String text() {
			checkValid();
			if (parent == null) {
				return StyledText.this.text();
			} else if (flexible == null) {
				return fixed.substring(from, to);
			} else {
				return flexible.substring(from, to);
			}
		}

		public int length() {
			checkValid();
			return to - from;
		}

		public List<Span> children() {
			checkValid();
			return Collections.unmodifiableList(children);
		}

		public List<Span> applyStyle(Style style, String id, int from, int to) {
			if (id == null) throw new IllegalArgumentException("null id");
			return applyStyleImpl(style, id, from, to);
		}

		public List<Span> applyStyle(Style style, int from, int to) {
			return applyStyleImpl(style, null, from, to);
		}

		public void insertText(int index, String text) {
			index = checkedIndex(index);
			if (text == null) throw new IllegalArgumentException("null text");
			checkValid();
			checkMutable();
			insertTextImpl(index, text);
		}

		//TODO add method to insert actual StyledText too
		public Optional<Span> insertStyledText(int index, Style style, String text) {
			index = checkedIndex(index);
			if (style == null) throw new IllegalArgumentException("null style");
			if (text == null) throw new IllegalArgumentException("null text");
			if (text.isEmpty()) return Optional.empty();
			insertTextImpl(index, text);
			Span parent = findSpan(index, index + text.length());
			Span child = new Span(parent, style.immutable(), text, index, index + text.length());
			parent.children.add(child);
			return Optional.of(child);
		}

		public void deleteText(int from, int to) {
			if (from > to) throw new IllegalArgumentException("from exceeds to");
			from = checkedIndex(from);
			to = checkedIndex(to);
			checkValid();
			checkMutable();
			deleteTextImpl(from, to);
		}

		public void delete() {
			checkValid();
			checkMutable();
			if (parent == null) throw new IllegalStateException("cannot remove root");
			deleteTextImpl(from, to);
		}

		public void remove() {
			checkValid();
			checkMutable();
			if (parent == null) throw new IllegalStateException("cannot remove root");

			boolean gone = parent.children.remove(this);
			assert gone;
			for (Span child : children) {
				child.parent = this.parent;
			}
			parent.children.sort(comp);

			parent = null;
			children.clear();
			from = to = -1;
		}

		public boolean invalid() {
			return to == from;
		}

		private void spansWithId(String id, List<Span> spans) {
			if (this.id != null && this.id.equals(id)) spans.add(this);
			for (Span child : children) {
				child.spansWithId(id, spans);
			}
		}

		private void checkValid() {
			if (from == -1) throw new IllegalStateException("invalid");
		}
		private int checkedIndex(int index) {
			index += from;
			if (index < from || index > to) throw new IllegalArgumentException("invalid index: " + index);
			return index;
		}

		private Span findSpan(int from, int to) {
			for (Span child : children) {
				if (child.from <= from && to <= child.to) return child.findSpan(from, to);
			}
			return this;
		}

		private void adjustSelfAndDescendants(int index, int delta) {
			if (index < to) to += delta;
			if (index < from) from += delta;
			assert parent == null || to > from;
			for (Span child : children) {
				child.adjustSelfAndDescendants(index, delta);
			}
		}

		private List<Span> applyStyleImpl(Style style, String id, int from, int to) {
			if (style == null) throw new IllegalArgumentException("null style");
			if (from == to) throw new IllegalArgumentException("empty span");
			if (from > to) throw new IllegalArgumentException("from exceeds to");
			from = checkedIndex(from);
			to = checkedIndex(to);
			style = style.immutable();
			Span span = findSpan(from, to);
			List<Slice> slices = span.slice(from, to);
			List<Span> spans = new ArrayList<>(slices.size());
			for (Slice slice : slices) {
				spans.add(slice.createSpan(style, id));
			}
			return spans;
		}

		private List<Slice> slice(int from, int to) {
			ArrayList<Slice> slices = new ArrayList<>();
			slice(new Slice(this, from, to), slices);
			return slices;
		}

		private void slice(Slice slice, List<Slice> result) {
			// first, split over our children
			List<Slice> splits = new ArrayList<>(2);
			slice.slice(splits);

			// then recurse into children
			for (Slice split : splits) {
				if (split.span == this) {
					// this slice is confirmed good
					result.add(split);
				} else {
					// we have to recurse to check it's good for our child
					split.recurse(result);
				}
			}
		}

		// assumes that everything is good
		private Span createSpan(Style style, String id, int from, int to) {
			Span child = new Span(this, style, id, from, to);
			//TODO make more efficient
			children.add(child);
			children.sort(comp);
			return child;
		}

		private void deleteTextImpl(int from, int to) {
			if (from == to) return;
			Span span = findSpan(from, to);
			List<Slice> slices = span.slice(from, to);
			int delta = 0;
			for (Slice slice : slices) {
				slice.span.deleteSafe(slice.from + delta, slice.to + delta);
				delta += slice.from - slice.to;
			}
		}

		// call when guaranteed that from-to is fully contained and doesn't intersect with any children
		private void deleteSafe(int from, int to) {
			if (from == this.from && to == this.to) { // this leaves us empty
				if (parent == null) { // special case empty root permitted
					if (flexible != null) flexible.setLength(0);
					fixed = "";
					from = 0;
					to = 0;
				} else { // remove ourselves
					Span target = parent;
					remove();
					target.deleteSafe(from, to); // note, parent may do the same
				}
				return;
			}
			if (flexible == null) flexible = new StringBuilder(fixed);
			flexible.delete(from, to);
			fixed = null;
			adjust(from, from - to);
		}

		private Style buildStyle(int index, Style style) {
			for (Span child : children) {
				if (child.from <= index && index < child.to) {
					style.apply(child.style);
					return child.buildStyle(index, style);
				}
			}
			return style;
		}

		//TODO avoid having this method, or make more efficient
		private Span nextSibling() {
			if (parent == null) return null;
			List<Span> children = parent.children;
			int index = children.indexOf(this);
			return index == children.size() - 1 ? null : children.get(index + 1);
		}

		private String name() {
			return this.id == null ? Integer.toHexString(hashCode()) : this.id;
		}

//		private boolean intersectsChild(int from, int to) {
//			for (Span child : children) {
//				if (from < child.to && to > child.from) return true;
//			}
//			return false;
//		}

		private void toString(StringBuilder sb, int count) {
			String name;
			if (parent == null) {
				name = null;
			} else if (id == null) {
				name = Integer.toString(count++);
			} else {
				name = id;
			}

			if (name != null) {
				sb.append("[START ").append(name).append("]");
			}
			int last = from;
			for (Span child : children) {
				sb.append(substring(last, child.from));
				child.toString(sb, count);
				last = child.to;
			}
			sb.append(substring(last, to));
			if (name != null) {
				sb.append("[FINISH ").append(name).append("]");
			}
		}
	}

	private final class TextSlice implements CharSequence {

		private final int from;
		private final int to;

		private TextSlice(int from, int to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			if (start < 0) throw new IllegalArgumentException("negative start");
			if (start > end) throw new IllegalArgumentException("start exceeds end");
			if (end > to - from) throw new IllegalArgumentException("end too large");
			return new TextSlice(from + start, from + end);
		}

		@Override
		public int length() {
			return to - from;
		}

		@Override
		public char charAt(int index) {
			index += from;
			if (index < from) throw new IllegalArgumentException("negative index");
			if (index >= to) throw new IllegalArgumentException("index too great");
			return fixed == null ? flexible.charAt(index) : fixed.charAt(index);
		}

		@Override
		public String toString() {
			return fixed == null ? flexible.substring(from, to) : fixed.substring(from, to);
		}

	}

	private static class Slice {
		final Span span;
		final int from;
		final int to;

		Slice(Span span, int from, int to) {
			this.span = span;
			this.from = from;
			this.to = to;
			assert from >= span.from;
			assert to <= span.to;
			assert from < to;
		}

		@Override
		public String toString() {
			return span.name() + "[" + from + "-" + to + "]";
		}

		private void slice(List<Slice> splits) {
			Slice remaining = this;
			for (Span child : span.children) {
				remaining = remaining.recordSplit(child, splits);
				if (remaining == null) return;
			}
			splits.add(remaining);
		}

		private void recurse(List<Slice> result) {
			span.slice(this, result);
		}

		private Slice recordSplit(Span child, List<Slice> splits) {
			if (to <= child.from) { // no intersection - going order, so we're known good
				splits.add(this);
				return null;
			}
			if (from >= child.to) { // no intersection - we're unchanged
				return this;
			}
			if (child.from <= from && to <= child.to) { // contained in child
				splits.add(new Slice(child, from, to));
				return null;
			}
			if (from < child.from) splits.add(new Slice(span, from, child.from)); // any left part is known good
			splits.add( new Slice(child, Math.max(from, child.from), Math.min(to, child.to)) ); // any centre part is within child
			if (to > child.to) { // any right part must be checked
				return new Slice(span, child.to, to);
			}
			// nothing left
			return null;
		}

		Span createSpan(Style style, String id) {
			return span.createSpan(style, id, from, to);
		}

	}

	private final class Flattener implements Iterator<Segment> {

		private final List<Frame> stack = new ArrayList<>();
		private int index = 0;
		private Segment next;

		private Flattener(Span root, Style style) {
			stack.add(new Frame(root, style.mutableCopy().apply(root.style).immutable()));
			advance();
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public Segment next() {
			Segment ret = next;
			advance();
			return ret;
		}

		private void advance() {
			while (true) {
				// have we exhausted the spans?
				if (stack.isEmpty()) {
					next = null;
					return;
				}
				int frameIndex = stack.size() - 1;
				// have we reached the end of all spans?
				Frame frame = stack.get(frameIndex);
				Span span = frame.span;
				if (index == span.to) {
					stack.remove(frameIndex);
					continue;
				}
				// are we past all children?
				Style style = frame.style;
				if (frame.childIndex == span.children.size()) {
					next = new Segment(new TextSlice(index, span.to), style);
					index = next.to;
					return;
				}
				// are we at the start of the next child?
				Span child = span.children.get(frame.childIndex);
				if (index == child.from) {
					stack.add(new Frame(child, style.mutable().apply(child.style).immutable()));
					frame.childIndex++;
					continue;
				}
				//  we must be before next child
				next = new Segment(new TextSlice(index, child.from), style);
				index = next.to;
				return;
			}
		}

	}

	private final class SpanIterator implements Iterator<Span> {

		private final List<Span> stack = new ArrayList<>();

		public SpanIterator(Span root) {
			stack.add(root);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Span next() {
			if (stack.isEmpty()) throw new NoSuchElementException();
			Span next = stack.get(stack.size() - 1);
			advance();
			return next;
		}

		private void advance() {
			Span span = stack.get(stack.size() - 1);
			// if we have a child move there
			if (!span.children.isEmpty()) {
				stack.add(span.children.get(0));
				return;
			}
			// keep recursing up until we have a sibling
			Span sibling;
			while (true) {
				sibling = span.nextSibling();
				if (sibling != null) break;
				stack.remove(stack.size() - 1);
				if (stack.isEmpty()) return; // end of the line
				span = stack.get(stack.size() - 1);
			}
			// move to our sibling
			stack.set(stack.size() - 1, sibling);
		}
	}

	public static class Segment {

		public final CharSequence text;
		public final Style style;
		public final int from;
		public final int to;

		private Segment(TextSlice textSlice, Style style) {
			this.style = style;
			this.text = textSlice;
			from = textSlice.from;
			to = textSlice.to;
		}

	}

	private static class Frame {
		Span span;
		Style style;
		int childIndex;

		private Frame(Span span, Style style) {
			this.span = span;
			this.style = style;
			childIndex = 0;
		}
	}
}
