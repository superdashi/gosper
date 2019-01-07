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

import com.superdashi.gosper.item.Item;

public final class Card extends ItemComponent {

	private ItemModel model;

	public ItemModel model() {
		return model;
	}

	public void model(ItemModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		if (model == this.model) return;
		this.model = model;
		resetRevision();
		situation.requestRedrawNow();
	}

	// convenience method
	public ItemModel item(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
		ItemModel model = situation.models().itemModel(item);
		model(model);
		return model;
	}

	// convenience method
	public Item item() {
		return model == null ? null : model.item;
	}

	// item component methods

	@Override
	ItemModel itemModel() {
		return model;
	}

}
