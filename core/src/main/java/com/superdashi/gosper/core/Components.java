package com.superdashi.gosper.core;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.graphdb.Inspect;
import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.graphdb.Node;
import com.superdashi.gosper.graphdb.NodeCursor;
import com.superdashi.gosper.graphdb.Selector;
import com.superdashi.gosper.item.Info;
import com.superdashi.gosper.item.Item;

public final class Components {

	//TODO yuck!!!
	public interface InfoCheck {

		boolean isNewInfoAvailable();

	}

	public static InfoAcquirer createAcquirer(Details details, Selector selector, Function<NodeCursor, Item> metaFn, Function<NodeCursor, List<Item>> itemsFn, InfoCheck infoCheck) {
		return new InfoAcquirer() {

			@Override
			public Details details() { return details; }

			@Override
			public Info acquireInfo(Inspector inspector) {
				try (Inspect inspect = inspector.inspect()) {
					NodeCursor cursor = inspect.graph().nodes(selector);
					Item meta = metaFn.apply(cursor);
					List<Item> items = itemsFn.apply(cursor);
					return Info.from(meta, items);
				}
			}

			@Override
			public boolean isNewInfoAvailable() {
				return infoCheck.isNewInfoAvailable();
			}
		};
	}

	public static InfoAcquirer createOneToOneAcquirer(Details details, Selector selector, Function<NodeCursor, Item> metaFn, Function<Node, Item> itemFn, InfoCheck infoCheck) {
		return createAcquirer(details, selector, metaFn, c -> c.stream().map(itemFn).collect(Collectors.toList()), infoCheck);
	}
}
