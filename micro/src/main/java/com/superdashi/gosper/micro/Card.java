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
