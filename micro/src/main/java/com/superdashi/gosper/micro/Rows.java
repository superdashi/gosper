package com.superdashi.gosper.micro;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.superdashi.gosper.graphdb.Inspect;
import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.graphdb.Node;
import com.superdashi.gosper.graphdb.Order;
import com.superdashi.gosper.graphdb.Selector;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.micro.Table.Row;

public interface Rows {

	public static Rows noRows() {
		//TODO want as a hidden constant
		return new Rows() {
			@Override public long revision() { return 0; }
			@Override public int size(long revision) { return 0; }
			@Override public boolean populateRows(long revision, int index, DisplayColumns columns, Row[] rows) { return true; }
		};
	}

	//TODO move onto models interface?
	//TODO call through to fixedActionRows
	public static Rows fixedItemRows(boolean selectable, boolean actionable, List<Item> items) {
		return new Rows() {

			@Override
			public int size(long revision) {
				return items.size();
			}

			@Override
			public long revision() {
				return 0;
			}

			@Override
			public boolean populateRows(long revision, int index, DisplayColumns columns, Row[] rows) {
				if (revision != 0) return false;
				List<Item> list = items.subList(index, index + rows.length);
				for (int i = 0; i < rows.length; i++) {
					Item item = list.get(i);
					Action action = actionable ? Action.create(Action.ID_SELECT_ROW, item) : null;
					rows[i] = new Row(item, selectable, false, action);
				};
				return true;
			}
		};
	}

	//TODO move onto models interface?
	public static Rows fixedActionRows(boolean selectable, boolean actionable, Action... actions) {
		//TODO check & clone actions in public interface
		return new Rows() {

			@Override
			public int size(long revision) {
				return actions.length;
			}

			@Override
			public long revision() {
				return 0;
			}

			@Override
			public boolean populateRows(long revision, int index, DisplayColumns columns, Row[] rows) {
				if (revision != 0) return false;
				for (int i = 0; i < rows.length; i++) {
					Action action = actions[i + index];
					rows[i] = new Row(action.item, selectable, false, action);
				};
				return true;
			}
		};
	}

	//TODO decide and clean up
	// possibly still a good implementation for larger result sets
	@Deprecated
	public static Rows oldDatabaseRows(Selector selector, Inspector inspector, Function<Node, Row> mapping) {
		if (selector == null) throw new IllegalArgumentException("null selector");
		if (inspector == null) throw new IllegalArgumentException("null inspector");
		if (mapping == null) throw new IllegalArgumentException("null mapping");
		return new Rows() {
			private List<Node> cached = null;
			private int skip = 0;
			private int length = 0; // this is somewhat arbitrary
			private int estSize = -1;
			private int knownSize = -1;
			private long revision = 0;

			@Override
			public long revision() {
				return revision;
			}

			@Override
			public int size(long revision) {
				if (cached == null || revision != this.revision) {
					update();
					return -1;
				}
				int size = knownSize == -1 ? estSize : knownSize;
				return size;
			}

			@Override
			public boolean populateRows(long revision, int index, DisplayColumns columns, Row[] rows) {
				if (cached == null || revision != this.revision || index != skip || length != rows.length) {
					skip = index;
					length = rows.length;
					update();
					return false;
				}
				for (int i = 0; i < rows.length; i++) {
					Node node = cached.get(i);
					rows[i] = mapping.apply(node);
				}
				return true;
			}

			private void update() {
				try (Inspect inspect = inspector.inspect()) {
					//cannot skip like this because the size cannot be known
					//cached = inspect.graph().nodes(selector).stream().skip(skip).limit(until - skip).collect(Collectors.toList());
					List<Node> nodes = new ArrayList<>();
					Iterator<Node> it = inspect.graph().nodes(selector).iterator();

					int from = skip;
					while (from > 0 && it.hasNext()) {
						it.next();
						from--;
					}

					// always request some records, otherwise can't recover from situation where previous length was zero
					int until = length + 10;
					while (nodes.size() < until && it.hasNext()) {
						nodes.add(it.next());
					}

					cached = nodes;
					int size = skip - from + nodes.size();
					if (size < until) {
						// we know that there are this many records
						knownSize = size;
					} else if (size > knownSize) {
						knownSize = -1;
						estSize = size + length;
					} else {
						estSize = size + length;
					}
					revision++;
				}
			}
		};
	}

	public static Rows databaseRows(Inspector inspector, Selector selector, Order order, Function<Node, Row> mapping) {
		if (inspector == null) throw new IllegalArgumentException("null inspector");
		if (selector == null) throw new IllegalArgumentException("null selector");
		if (order == null) throw new IllegalArgumentException("null order");
		if (mapping == null) throw new IllegalArgumentException("null mapping");
		return new Rows() {

			@Override
			public long revision() {
				try (Inspect inspect = inspector.inspect()) {
					return inspect.version();
				}
			}

			@Override
			public int size(long revision) {
				try (Inspect inspect = inspector.inspect()) {
					if (inspect.version() != revision) return -1;
					return inspect.graph().nodes(selector).count();
				}
			}

			@Override
			public boolean populateRows(long revision, int index, DisplayColumns columns, Row[] rows) {
				try (Inspect inspect = inspector.inspect()) {
					if (inspect.version() != revision) return false;
					inspect.graph().nodes(selector).order(order).stream().skip(index).limit(rows.length).map(mapping::apply).collect(Collectors.toList()).toArray(rows);
					return true;
				}
			}
		};
	}

	// non-negative monotonic
	long revision();

	// -1 if revision does not match
	int size(long revision);

	// false if revision does not match
	//TODO doesn't have visual context
	boolean populateRows(long revision, int index, DisplayColumns columns, Row[] rows);

}
