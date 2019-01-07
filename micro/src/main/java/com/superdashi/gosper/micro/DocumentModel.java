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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.logging.Logger;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;

//TODO allow nested blocks? floating?
//TODO how to share action models between blocks?
public final class DocumentModel extends Model {

	private static final boolean DEBUG_DOCUMENT_MODEL = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.micro.DocumentModel.DEBUG"));

	private static final int GONE = Integer.MIN_VALUE;

	private static void checkClassName(String className) {
		if (className == null) throw new IllegalArgumentException("null class name");
		if (className.isEmpty()) throw new IllegalArgumentException("empty class name");
		//TODO apply more checks
	}

	private final Mutations mutations;
	private final BlockModel root;
	private final Map<String, ClassModel> classes = new HashMap<>(); // use collect?

	DocumentModel(DocumentModel contextSource) {
		super(contextSource);
		this.mutations = contextSource.mutations;
		root = new BlockModel();
	}

	DocumentModel(ActivityContext context, Mutations mutations) {
		super(context);
		this.mutations = mutations;
		root = new BlockModel();
	}

	// public methods

	public long revision() {
		return mutations.count;
	}

	public Position inheritPosition(Position position) {
		if (position == null) throw new IllegalArgumentException("null position");
		if (position.document() == this) return position; // nothing to do
		Position copy = new Position();
		copy.id = position.id;
		copy.offset = position.offset;
		copy.y = position.y;
		return copy;
	}

	public List<BlockModel> topLevelBlocks() {
		return Collections.unmodifiableList(root.children);
	}

	public BlockModel newBlock() {
		return new BlockModel();
	}

	// public methods that mimic block

	public BlockModel appendNewBlock() {
		return root.appendNewBlock();
	}

	public void appendBlock(BlockModel block) {
		root.appendBlock(block);
	}

	public BlockModel prependNewBlock() {
		return root.prependNewBlock();
	}

	public void prependBlock(BlockModel block) {
		root.prependBlock(block);
	}

	public Style style() {
		return root.style;
	}

	public void style(Style style) {
		root.style(style);
	}

	// classes

	public ClassModel attachedClass(String name) {
		return classes.computeIfAbsent(name, n -> new ClassModel(n));
	}

	public Collection<ClassModel> attachedClasses() {
		return Collections.unmodifiableCollection(classes.values());
	}

	// package scoped methods

	Blueprint blueprint(VisualSpec spec, IntDimensions viewSize) {
		return new Blueprint(this, spec, viewSize);
	}

	// private methods

	private Position newPosition() {
		return new Position();
	}

	private List<BlockModel> blockList() {
		List<BlockModel> list = new ArrayList<>();
		root.growBlockList(list);
		return list;
	}

	// inner classes

	public final class BlockModel {

		// parent block, null if detached or root
		private BlockModel parent;
		// index within parent GONE indicates removed from document
		private int index;
		//TODO create lazily
		private List<BlockModel> children = new ArrayList<>();
		private String id = null;
		private Content content = Content.noContent;
		private ActionModel action = null;
		private Style style = Style.noStyle();
		private final List<String> classNames = new ArrayList<>();

		BlockModel() {
			parent = null;
			index = GONE;
			mutations.count ++;
			requestRedraw();
		}

		// document methods

		public DocumentModel document() {
			return DocumentModel.this;
		}

		public boolean isInDocument() {
			BlockModel model = this;
			while (model != null) {
				if (model == root) return true;
				model = model.parent;
			}
			return false;
		}

//		public int indexInDocument() {
//			if (index == GONE) throw new IllegalArgumentException("block not in document");
//			return index;
//		}

		// accessors

		public BlockModel id(String id) {
			if (id != null && id.isEmpty()) id = null;
			if (!Objects.equals(this.id, id)) {
				this.id = id;
				mutations.count ++;
				requestRedraw();
			}
			return this;
		}

		public Optional<String> id() {
			return Optional.ofNullable(id);
		}

		public Content content() {
			return content;
		}

		public Optional<ActionModel> action() {
			return Optional.ofNullable(action);
		}

		public BlockModel action(Action action) {
			if (this.action == null && action == null || this.action != null && this.action.action() == action) return this; // no change
			if (action == null) {
				this.action = null;
			} else {
				this.action = models().actionModel(action);
			}
			mutations.count ++;
			requestRedraw();
			return this;
		}

		public Style style() {
			return style;
		}

		public BlockModel style(Style style) {
			if (style == null) throw new IllegalArgumentException("null style");
			style = style.immutable();
			if (style != this.style) {
				this.style = style;
				mutations.count ++;
				requestRedraw();
			}
			return this;
		}

		public BlockModel classNames(String... classNames) {
			if (classNames == null) throw new IllegalArgumentException("null classNames");
			for (String className : classNames) {
				checkClassName(className);
			}
			List<String> list = Arrays.asList(classNames);
			if (!list.equals(this.classNames)) {
				this.classNames.clear();
				this.classNames.addAll(list);
			}
			return this;
		}

		public List<String> classNames() {
			return Collections.unmodifiableList(classNames);
		}

		// structural methods

		public BlockModel appendNewBlock() {
			BlockModel child = new BlockModel();
			child.insert(this, children.size());
			mutations.count ++;
			return child;
		}

		public void appendBlock(BlockModel that) {
			if (that == null) throw new IllegalArgumentException("null that");
			checkSameDocument(that);
			if (that.parent != null) that.remove();
			that.insert(this, children.size());
			mutations.count ++;
		}

		public BlockModel prependNewBlock() {
			BlockModel child = new BlockModel();
			child.insert(this, 0);
			mutations.count ++;
			return child;
		}

		public void prependBlock(BlockModel that) {
			if (that == null) throw new IllegalArgumentException("null that");
			checkSameDocument(that);
			if (that.parent != null) that.remove();
			that.removeFromParent();
			that.insert(this, 0);
			mutations.count ++;
		}

		// note: shouldn't be called on root, root should not be visible to app
		public Optional<BlockModel> parent() {
			if (parent == null || parent == root) return Optional.empty();
			return Optional.of(parent);
		}

		public boolean removeFromParent() {
			if (parent == null) return false;
			remove();
			mutations.count ++;
			return true;
		}

		//TODO need to guard against inserting into self/child
		public boolean insertBefore(BlockModel that) {
			if (that == null) throw new IllegalArgumentException("null that");
			if (this == that) return false;
			checkSameDocument(that);
			// if we are already the direct predecessor return false
			if (this.parent == that.parent && this.index + 1 == that.index) return false;
			//TODO could optimize - if parent is same, just shuffle intermediate indices
			if (parent != null) remove();
			insert(that.parent, that.index);
			mutations.count ++;
			return true;
		}

		//TODO need to guard against inserting into self/child
		public boolean insertAfter(BlockModel that) {
			if (that == null) throw new IllegalArgumentException("null that");
			if (this == that) return false;
			checkSameDocument(that);
			// if we are already the direct successor return false
			if (this.parent == that.parent && this.index - 1 == that.index) return false;
			//TODO could optimize - if parent is same, just shuffle intermediate indices
			if (parent != null) remove();
			insert(that.parent, that.index + 1);
			mutations.count ++;
			return true;
		}

		// content methods
		public BlockModel textContent(String text) {
			return content(Content.textContent(text));
		}

		public BlockModel pictureContent(Item item) {
			return content(Content.pictureContent(models().itemModel(item)));
		}

		public BlockModel content(Content content) {
			if (content == null) throw new IllegalArgumentException("null content");
			if (!this.content.equals(content)) {
				this.content = content;
				mutations.count ++;
				requestRedraw();
			}
			return this;
		}

		// private helper methods

		private void checkSameDocument(BlockModel that) {
			if (that.document() != this.document()) throw new IllegalArgumentException("different document");
		}

		// assumes same document
		boolean isDescendantOf(BlockModel that) {
			BlockModel block = this;
			do {
				if (block == that) return true;
				block = block.parent;
			} while (block != null);
			return false;
		}

		//TODO replace with a call on parent
		private void remove() {
			parent.removeChild(index);
			parent = null;
			index = GONE;
		}

		private void insert(BlockModel parent, int index) {
			this.parent = parent;
			this.index = index;
			parent.insertChild(index, this);
		}

		private void insertChild(int index, BlockModel child) {
			children.add(index, child);
			reindexChildren(index, children.size());
		}
		private void removeChild(int index) {
			children.remove(index);
			reindexChildren(index, children.size());
		}

		private void reindexChildren(int from, int to) {
			for (int i = from; i < to; i++) {
				children.get(i).index = i;
			}
		}

		private void growBlockList(List<BlockModel> list) {
			if (this != root) list.add(this);
			for (BlockModel child : children) child.growBlockList(list);
		}
	}

	public final class ClassModel {

		private final String name;
		private Style style;

		ClassModel(String name) {
			checkClassName(name);
			this.name = name;
			style = Style.noStyle();
		}

		public String name() {
			return name;
		}

		public Style style() {
			return style;
		}

		public void style(Style style) {
			if (style == this.style) return;
			this.style = style.immutable();
			mutations.count ++;
			requestRedraw();
		}

		public boolean isAttached() {
			return classes.get(name) == this;
		}

		public boolean detachFromDocument() {
			return classes.remove(name, this);
		}
	}

	static class Blueprint {

		final DocumentModel document;
		final IntDimensions viewSize;
		final Style fundamentalStyle;
		final int sectionCount;
		final int actionCount;
		final long revision;
		final int selectorGap;
		final int docHeight;
		final Section[] sections;

		Blueprint(DocumentModel document, VisualSpec spec, IntDimensions viewSize) {
			this.document = document;
			this.viewSize = viewSize;
			this.revision = document.mutations.count;

			List<BlockModel> blocks = document.blockList();
			int blockCount = blocks.size();
			Style docStyle = document.root.style;
			IntMargins margins = docStyle.margins();
			docStyle = docStyle.noMargins();

			// first check whether any block has an associated action
			//TODO want a better approach to finding the selector dimensions
			Selector selector = blocks.stream().mapToInt(b -> b.action == null ? 0 : 1).sum() == 0 ? null : new Selector(spec, false);
			selectorGap = selector == null ? 0 : selector.width() + spec.metrics.buttonGap; //TODO need a dedicated selectorGap?
			int selectorHeight = selector == null ? 0 : selector.height();
			int docWidth = viewSize.width + margins.minX - margins.maxX;
			docWidth -= selectorGap;

			//TODO this is all very kludgy, should be done with an explict block stack to trace ancestry?
			List<Section> list = new ArrayList<>(blockCount);
			// for each block, the effective style is the base style (to ensure a value for all) + document style + its own style + any applicable classes
			fundamentalStyle = spec.styles.defaultDocumentStyle.mutable().apply(docStyle).immutable();
			int totalHeight = -margins.minY;
			int actionIndex = -1;
			ActionModel legacyAction = null;
			BlockModel legator = null;
			BlockModel firstLegatee = null;
			for (int i = 0; i < blockCount; i++) {
				BlockModel block = blocks.get(i);
				ActionModel action = block.action;
				// don't render blocks with children, but do 'inherit' their action indicator
				if (!block.children.isEmpty()) {
					if (action != null) {
						legacyAction = action;
						legator = block;
						firstLegatee = null;
					}
					continue;
				}
				// first compute style
				Style style = fundamentalStyle.mutable();
				style.apply(block.style);
				List<String> classNames = block.classNames;
				for (String className : classNames) {
					ClassModel clss = document.classes.get(className);
					if (clss != null) style.apply(clss.style);
				}
				// then compute height
				int blockWidth = docWidth - style.marginLeft() - style.marginRight();
				SizedContent content = block.content().sizeForWidth(spec, style, blockWidth);
				int contentHeight = content.dimensions().height;
				contentHeight = Math.max(contentHeight, selectorHeight);
				int blockHeight = contentHeight + style.marginTop() + style.marginBottom();
				// possibly inherit an action
				boolean showAction = action != null;
				if (legator != null) {
					if (block.isDescendantOf(legator)) {
						if (action == null) {
							action = legacyAction;
							if (firstLegatee == null) {
								firstLegatee = block;
								showAction = true;
							}
						}
					} else {
						legacyAction = null;
						legator = null;
						firstLegatee = null;
					}
				}
				// update action index
				if (showAction) {
					actionIndex++;
				}
				// record section
				list.add( new Section(i, totalHeight, blockHeight, style.immutable(), block, action, actionIndex, showAction, content) );
				// update top
				totalHeight += blockHeight;
			}
			sectionCount = list.size();
			sections = (Section[]) list.toArray(new Section[sectionCount]);
			docHeight = totalHeight + margins.maxY;
			actionCount = actionIndex + 1;

			if (DEBUG_DOCUMENT_MODEL) {
				Debug.logging().message("Blueprint with revision {}, selector gap {}, doc height {}").values(revision, selectorGap, docHeight).log();
				for (Section section : list) {
					section.debug(Debug.logger());
				}
			}
		}

		//TODO could accelerate lazily
		Optional<Section> sectionForBlock(BlockModel block) {
			if (block == null) throw new IllegalArgumentException("null block");
			//Arrays.stream(sections).filter(s -> s.block == block).findAny();
			for (Section section : sections) {
				if (section.block == block) return Optional.of(section);
			}
			return Optional.empty();
		}

		Optional<Section> sectionForId(String id) {
			if (id == null) throw new IllegalArgumentException("null id");
			for (Section section : sections) {
				if (section.block.id().isPresent() && section.block.id().get().equals(id)) return Optional.of(section);
			}
			return Optional.empty();
		}

		Optional<Section> sectionForY(int y) {
			if (sectionCount == 0) return Optional.empty();
			if (y < 0) return Optional.of(sections[0]);
			for (Section section : sections) {
				if (section.top <= y && y < section.top + section.height) return Optional.of(section);
			}
			return Optional.of(sections[sectionCount - 1]);
		}

		Optional<Section> sectionForActionIndex(int actionIndex) {
			if (sectionCount == 0) return Optional.empty();
			if (actionIndex < 0) return Optional.of(sections[0]);
			for (Section section : sections) {
				if (section.actionIndex == actionIndex) return Optional.of(section);
			}
			return Optional.of(sections[sectionCount - 1]);
		}

		Position newPosition() {
			Position position = document.newPosition();
			if (sectionCount > 0) {
				Section section = sections[0];
				position.y = section.top;
				position.block = section.block;
				position.id = position.block.id().orElse(null);
				position.offset = 0;
				position.actionIndex = section.actionIndex;
			}
			return position;
		}

		void refreshPosition(Position position, boolean positionMoved) {
			if (position == null) throw new IllegalArgumentException("null position");
			if (position.document() != document) throw new IllegalArgumentException("position from different document");
			if (sectionCount == 0) {
				// no point, empty document
				position.reset();
				return;
			}

			// first try to identify relevant section via block
			Optional<Section> section = Optional.empty();
			if (!section.isPresent() && position.block != null) {
				section = sectionForBlock(position.block);
			}

			// if that fails, try the id
			if (!section.isPresent() && position.id != null) {
				section = sectionForId(position.id);
			}

			// if we have one, check that we're still pointing inside it
			if (section.isPresent()) {
				Section s = section.get();
				if (s.top <= position.y && position.y < s.top + s.height) {
					// we're still good, just adjust offset
					position.offset = position.y - s.top;
				} else {
					// we're not good, try again using an offseted position
					if (!positionMoved) {
						position.y = s.top + position.offset;
					}
					section = Optional.empty();
				}
			}

			// if we're still not there, fall back to the y position
			if (!section.isPresent()) {
				// rely on old y position
				section = sectionForY(position.y);
				// section cannot be null because document is not empty
				position.offset = position.y - section.get().top;
			}

			// make position consistent with chose section
			positionRelative(section.get(), position);
		}

		boolean movePosition(Position position, int delta) {
			if (delta == 0) return false;
			int oldActionIndex = position.actionIndex;
			int oldY = position.y;
			position.y += delta;
			position.offset += delta;
			refreshPosition(position, true);
			if (position.y == oldY) return false; // couldn't move
			// don't let movement skip more than one action
			if (Math.abs(position.actionIndex - oldActionIndex) > 1) {
				position.offset = 0;
				// section must exist, because position changed, so document not empty
				positionRelative(sectionForActionIndex(oldActionIndex + Integer.signum(delta)).get(), position);
			}
			return true;
		}

		private void positionRelative(Section section, Position position) {
			position.block = section.block;
			position.id = section.block.id().orElse(null);
			position.y = section.top + position.offset;
			position.actionIndex = section.actionIndex;
		}

		static class Section {

			final int index;
			final int top;
			final int height;
			final Style style;
			final BlockModel block;
			final ActionModel action;
			final int actionIndex;
			final boolean showAction;
			final SizedContent content;

			Section(int index, int top, int height, Style style, BlockModel block, ActionModel action, int actionIndex, boolean showAction, SizedContent content) {
				this.index = index;
				this.top = top;
				this.height = height;
				this.style = style;
				this.block = block;
				this.action = action;
				this.actionIndex = actionIndex;
				this.showAction = showAction;
				this.content = content;
			}

			boolean actionable() {
				return action != null && action.enabled();
			}

			void debug(Logger logger) {
				logger.debug().message("Section #{} top: {}, height: {}, actionIndex: {}").values(index, top, height, actionIndex).log();
				logger.debug().message("Content class: {}").values(content.getClass().getName()).log();
				style.debug(logger);
			}

		}
	}


	class Position implements Cloneable {

		BlockModel block;
		String id;
		int y;
		int offset;
		int actionIndex;

		private Position() {
			reset();
		}

		void reset() {
			block = null;
			id = null;
			y = 0;
			offset = 0;
			actionIndex = -1;
		}

		DocumentModel document() {
			return DocumentModel.this;
		}

		Optional<ActionModel> action() {
			BlockModel block = this.block;
			while (block != null) {
				Optional<ActionModel> action = block.action();
				if (action.isPresent()) return action;
				block = block.parent;
			}
			return Optional.empty();
		}
	}
}
