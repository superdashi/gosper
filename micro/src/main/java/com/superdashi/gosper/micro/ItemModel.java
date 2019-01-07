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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.Surface;
import com.superdashi.gosper.studio.PorterDuff.Rule;

//TODO address synchronization
public final class ItemModel extends Model {

	final Item item;
	final Mutations mutations;
	private String label = null;
	private Frame picture = null;
	private Frame icon = null;
	private Frame disabledIcon = null;
	private Frame badge = null;
	private Frame symbol = null;

	private int outstanding = 0;
	private List<ItemModel> copies = null;

	ItemModel(ActivityContext context, Item item) {
		this(context, item, new Mutations());
	}

	// constructor for non-contextual snapshotted models
	ItemModel(Item item) {
		super((ActivityContext) null);
		this.item = item;
		mutations = null;
	}

	ItemModel(ActivityContext context, Item item, Mutations mutations) {
		super(context);
		this.item = item;
		this.mutations = mutations;
		if (!isSnapshot()) startLoading();
	}

	private ItemModel(ItemModel that, Mutations mutations) {
		super(that);
		this.item = that.item;
		this.mutations = mutations;
		this.badge = that.badge;
		this.symbol = that.symbol;
		this.icon = that.icon;
		this.picture = that.picture;
		if (!isSnapshot() && that.outstanding > 0) {
			if (that.copies == null) {
				that.copies = new ArrayList<>();
			}
			that.copies.add(this);
		}
	}

	public long revision() {
		return mutations.count;
	}

	public Item item() {
		return item;
	}

	String label() {
		if (label == null) {
			label = item.label().orElse("");
		}
		return label;
	}

	Optional<Frame> picture() {
		return Optional.ofNullable(picture);
	}

	Optional<Frame> enabledIcon() {
		return Optional.ofNullable(icon);
	}

	Optional<Frame> disabledIcon() {
		if (disabledIcon == null && icon != null) {
			// compute the disabled icon
			Surface surface = Surface.create(icon.dimensions(), false);
			//TODO needs access to visual context
			surface.createCanvas()
				.composer(new PorterDuff(Rule.SRC_ATOP).asComposer())
				//TODO this should come from context
				.shader(Background.mono(2).asShader().get())
				.drawFrame(icon)
				.destroy();
			disabledIcon = surface.immutableView();
		}
		return Optional.ofNullable(disabledIcon);
	}

	Optional<Frame> icon(boolean enabled) {
		return enabled ? enabledIcon() : disabledIcon();
	}

	Optional<Frame> badge() {
		return Optional.ofNullable(badge);
	}

	Optional<Frame> symbol() {
		return Optional.ofNullable(symbol);
	}

	// object methods

	@Override
	public int hashCode() {
		return item.hashCode() + Objects.hashCode(badge) + Objects.hashCode(symbol) + Objects.hashCode(icon) + Objects.hashCode(picture);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ItemModel)) return false;
		ItemModel that = (ItemModel) obj;
		return
				this.item.equals(that.item) &&
				Objects.equals(this.badge  , that.badge  ) &&
				Objects.equals(this.symbol , that.symbol ) &&
				Objects.equals(this.icon   , that.icon   ) &&
				Objects.equals(this.picture, that.picture)
				;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(item.toString());
		if (badge   != null) sb.append(" with badge"  );
		if (symbol  != null) sb.append(" with symbol" );
		if (icon    != null) sb.append(" with icon"   );
		if (picture != null) sb.append(" with picture");
		return sb.toString();
	}

	// package scoped methods

	ItemModel copy(Mutations mutations) {
		return new ItemModel(this, mutations);
	}

	// private utility methods

	//TODO - should loading be started lazily?
	private void startLoading() {
		if (picture == null) item.picture()                             .ifPresent(res -> { backgroundLoad(res, this::loadImage, this::picture); outstanding ++; } );
		if (icon    == null) item.icon   ()                             .ifPresent(res -> { backgroundLoad(res, this::loadImage, this::icon   ); outstanding ++; } );
		if (badge   == null) item.value("gosper:badge") .optionalImage().ifPresent(res -> { backgroundLoad(res, this::loadImage, this::badge  ); outstanding ++; } );
		if (symbol  == null) item.value("gosper:symbol").optionalImage().ifPresent(res -> { backgroundLoad(res, this::loadImage, this::symbol ); outstanding ++; } );
	}

	private void picture(Frame picture) {
		if (picture == this.picture) return;
		if (this.picture == null) reduceOutstanding();
		this.picture = picture;
		if (copies != null) {
			for (ItemModel copy : copies) {
				copy.picture = picture;
			}
		}
		mutations.count ++;
		requestRedraw();
	}

	private void icon(Frame icon) {
		if (icon == this.icon) return;
		if (this.icon == null) reduceOutstanding();
		this.icon = icon;
		this.disabledIcon = null;
		if (copies != null) {
			for (ItemModel copy : copies) {
				copy.icon = icon;
				copy.disabledIcon = null;
			}
		}
		mutations.count ++;
		requestRedraw();
	}

	private void badge(Frame badge) {
		if (badge == this.badge) return;
		if (this.badge == null) reduceOutstanding();
		this.badge = badge;
		if (copies != null) {
			for (ItemModel copy : copies) {
				copy.badge = badge;
			}
		}
		mutations.count ++;
		requestRedraw();
	}

	private void symbol(Frame symbol) {
		if (symbol == this.symbol) return;
		if (this.symbol == null) reduceOutstanding();
		this.symbol = symbol;
		if (copies != null) {
			for (ItemModel copy : copies) {
				copy.symbol = symbol;
			}
		}
		mutations.count ++;
		requestRedraw();
	}

	private void reduceOutstanding() {
		if (outstanding == 0) throw new IllegalStateException("outstanding already zero");
		if (--outstanding == 0) copies = null;
	}
}
