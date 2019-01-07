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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.graphdb.Node;
import com.superdashi.gosper.graphdb.Order;
import com.superdashi.gosper.graphdb.Selector;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.micro.Table.Row;
import com.superdashi.gosper.util.DashiUtil;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

public interface Factory {

	// enums

	default Map<String, Regex.Flag> regexFlags() {
		//TODO needs to be cached
		return DashiUtil.enumToMap(Regex.Flag.class);
	}

	//TODO naming?
	default Map<String, Layout.Direction> layoutDirs() {
		//TODO needs to be cached
		return DashiUtil.enumToMap(Layout.Direction.class);
	}

	// serialization

	default DeferredActivity deserializeDeferredActivity(ReadStream r) {
		return DeferredActivity.deserialize(r);
	}

	default DataInput deserializeDataInput(ReadStream r) {
		return DataInput.deserialize(r);
	}

	default void writeDataValue(Serializable value, WriteStream w) {
		ActivityData.write(value, w);
	}

	default Serializable readDataValue(ReadStream r) {
		return ActivityData.read(r);
	}

	default Regex deserializeRegex(ReadStream r) {
		return Regex.deserialize(r);
	}

	// rows

	default Row newRow(Item item, boolean selectable, boolean placeholderLabel, Action action) {
		return new Row(item, selectable, placeholderLabel, action);
	}

	default Rows noRows() {
		return Rows.noRows();
	}

	default Rows fixedItemRows(boolean selectable, boolean actionable, List<Item> items) {
		//TODO check types of items
		return Rows.fixedItemRows(selectable, actionable, items);
	}

	default Rows fixedActionRows(boolean selectable, boolean actionable, Action... actions) {
		//TODO check types of actions
		return Rows.fixedActionRows(selectable, actionable, actions);
	}

	default Rows databaseRows(Inspector inspector, Selector selector, Order order, Function<Node, Row> mapping) {
		return Rows.databaseRows(inspector, selector, order, mapping);
	}

	// locations

	default Map<String, Location> locations() {
		//TODO needs to be cached
		return Location.canons();
	}

	default Location locationNamed(String name) {
		return Location.named(name);
	}

	// styles

	default Style noStyle() {
		return Style.noStyle();
	}

	default Style newStyle() {
		return new Style();
	}

	default Style marginStyle(IntMargins margins) {
		return new Style(margins);
	}

	// regex

	default Regex compileRegex(String pattern) {
		return Regex.compile(pattern);
	}

	//TODO offer convenience method that also takes strings?
	default Regex compileRegex(String pattern, Regex.Flag... flags) {
		return Regex.compile(pattern, flags);
	}

	// other

	default ActivityContext currentActivityContext() {
		return ActivityContext.current();
	}

	default Layout layout() {
		return Layout.single();
	}

	// interfaces

	<S,T> Function<S, T> asMapping(Object fn);

	Rows asRows(Object obj);

}
