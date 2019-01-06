package com.superdashi.gosper.micro;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Qualifier;

public final class Items {

	private static final Item[] NO_ITEMS = new Item[0];
	private static final Action[] NO_ACTIONS = new Action[0];

	private final Bundle bundle;
	private final Qualifier qualifier;
	private final boolean meta;

	Items(Bundle appData, Qualifier qualifier, boolean meta) {
		this.bundle = appData;
		this.qualifier = qualifier;
		this.meta = meta;
	}

	// the qualifier under which the items are being retrieved
	public Qualifier qualifier() {
		return qualifier;
	}

	public Item itemWithId(String id) {
		if (id == null) throw new IllegalArgumentException("null id");
		return bundle.item(id, qualifier, meta).orElseThrow(() -> new ResourceException("no item with id " + id));
	}

	public Optional<Item> possibleItemWithId(String id) {
		if (id == null) throw new IllegalArgumentException("null id");
		return bundle.item(id, qualifier, meta);
	}

	public Item[] itemsWithIds(String... ids) {
		if (ids == null) throw new IllegalArgumentException("null ids");
		switch (ids.length) {
		case 0 : return NO_ITEMS;
		case 1 : possibleItemWithId(ids[0]).map(i -> new Item[] {i}).orElse(NO_ITEMS);
		default:
			List<Item> list = new ArrayList<>(ids.length);
			for (String id : ids) {
				possibleItemWithId(id).ifPresent(i -> list.add(i));
			}
			return (Item[]) list.toArray(new Item[list.size()]);
		}
	}

	public Action actionWithId(String id) {
		return Action.create(id, itemWithId(id));
	}

	public Optional<Action> possibleActionWithId(String id) {
		return possibleItemWithId(id).map(item -> Action.create(id, item));
	}

	public Action[] actionsWithIds(String... ids) {
		if (ids == null) throw new IllegalArgumentException("null ids");
		switch (ids.length) {
		case 0 : return NO_ACTIONS;
		case 1 : possibleActionWithId(ids[0]).map(a -> new Action[] {a}).orElse(NO_ACTIONS);
		default:
			List<Action> list = new ArrayList<>(ids.length);
			for (String id : ids) {
				possibleActionWithId(id).ifPresent(i -> list.add(i));
			}
			return (Action[]) list.toArray(new Action[list.size()]);
		}
	}


}
