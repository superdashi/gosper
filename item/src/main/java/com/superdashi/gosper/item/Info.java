package com.superdashi.gosper.item;

import java.util.List;

import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public final class Info {

	private static final Storage<Item> storage = StoreType.of(Item.class).settingNullDisallowed().storage().immutable();

	public static Info from(Item metaOnly) {
		if (metaOnly == null) throw new IllegalArgumentException("null meta");
		return new Info(metaOnly, storage.newStore(0));
	}

	public static Info from(Item meta, List<Item> items) {
		if (meta == null) throw new IllegalArgumentException("null meta");
		if (items == null) throw new IllegalArgumentException("null items");
		//TODO create a new storage method for this
		return new Info(meta, storage.newStoreOf(items.toArray(new Item[items.size()])));
	}

	public final Item meta;
	public final Store<Item> items;

	Info(Item meta) {
		this.meta = meta;
		this.items = storage.newStore(0);
	}

	Info(Item meta, Item... items) {
		this.meta = meta;
		this.items = storage.newStoreOf(items);
	}

	Info(Item meta, Store<Item> items) {
		this.meta = meta;
		this.items = items;
	}

	//TODO possible method
//	public Info withItems(List<Item> items) {
//
//	}

	@Override
	public String toString() {
		return meta + " " + items;
	}
}
