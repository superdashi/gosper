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
package com.superdashi.gosper.graphdb;

import java.util.PrimitiveIterator;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;

import com.superdashi.gosper.graphdb.NSNKey.NSNChange;
import com.superdashi.gosper.graphdb.NSNKey.OwnerChange;
import com.superdashi.gosper.graphdb.NSNKey.PermChange;
import com.superdashi.gosper.graphdb.NSNKey.TagChange;
import com.superdashi.gosper.graphdb.NSNKey.TypeChange;
import com.superdashi.gosper.item.Value;

final class PartIndices {

	private static final String MAP_NAME_PARTS_BY_OWNER = "partsByOwner" ; // maps owner ns code + ids to empty
	private static final String MAP_NAME_PARTS_BY_TYPE  = "partsByType"; // maps types + ids to empty
	private static final String MAP_NAME_PARTS_BY_PERM  = "partsByPerm" ; // maps owner ns code + perm code to empty
	private static final String MAP_NAME_PARTS_BY_TAG   = "partsByTag" ; // maps tags + ids to empty

	private final MVMap<NSNKey, Value> partsByOwner; // always maps to empty
	private final MVMap<NSNKey, Value> partsByType; // always maps to empty
	private final MVMap<NSNKey, Value> partsByPerm; // always maps to empty
	private final MVMap<NSNKey, Value> partsByTag; // always maps to empty

	private static String rename(String name, String typeName) {
		return name.replace("part", typeName);
	}

	PartIndices(MVStore store, String typeName) {
		Builder<NSNKey, Value> builder = new MVMap.Builder<>();
		builder.keyType(NSNKeyType.instance);
		builder.valueType(EmptyType.instance);
		partsByOwner = store.openMap(rename(MAP_NAME_PARTS_BY_OWNER, typeName), builder);
		partsByType  = store.openMap(rename(MAP_NAME_PARTS_BY_TYPE,  typeName), builder);
		partsByPerm  = store.openMap(rename(MAP_NAME_PARTS_BY_PERM,  typeName), builder);
		partsByTag   = store.openMap(rename(MAP_NAME_PARTS_BY_TAG,   typeName), builder);
	}

	private PartIndices(PartIndices that, long version) {
		this.partsByOwner = that.partsByOwner.openVersion(version);
		this.partsByType  = that.partsByType .openVersion(version);
		this.partsByPerm  = that.partsByPerm .openVersion(version);
		this.partsByTag   = that.partsByTag  .openVersion(version);
	}

	PartIndices version(long version) {
		return new PartIndices(this, version);
	}

	PrimitiveIterator.OfInt nodeIteratorOverOwner(long identityId) {
		return nodeIterator(partsByOwner, identityId);
	}

	PrimitiveIterator.OfInt nodeIteratorOverType(long typeId) {
		return nodeIterator(partsByType, typeId);
	}

	PrimitiveIterator.OfInt nodeIteratorOverPerm(long permId) {
		return nodeIterator(partsByPerm, permId);
	}

	PrimitiveIterator.OfInt nodeIteratorOverTag(long tagId) {
		return nodeIterator(partsByTag, tagId);
	}


	PrimitiveIterator.OfLong edgeIteratorOverOwner(long identityId) {
		return edgeIterator(partsByOwner, identityId);
	}

	PrimitiveIterator.OfLong edgeIteratorOverType(long typeId) {
		return edgeIterator(partsByType, typeId);
	}

	PrimitiveIterator.OfLong edgeIteratorOverPerm(long permId) {
		return edgeIterator(partsByPerm, permId);
	}

	PrimitiveIterator.OfLong edgeIteratorOverTag(long tagId) {
		return edgeIterator(partsByTag, tagId);
	}


	void applyOwnerChange(OwnerChange change) {
		applyNSNChange(partsByOwner, change);
	}

	void applyTypeChange(TypeChange change) {
		applyNSNChange(partsByType, change);
	}

	void applyPermChange(PermChange change) {
		applyNSNChange(partsByPerm, change);
	}

	void applyTagChange(TagChange change) {
		applyNSNChange(partsByTag, change);
	}

	void dump() {
		dump(partsByOwner);
		dump(partsByType);
		dump(partsByPerm);
		dump(partsByTag);
	}

	private void dump(MVMap<NSNKey, Value> map) {
		System.out.println(map.getName() + "(" + map.size() + ")");
		map.keySet().forEach(k -> System.out.println(k));
		System.out.println();
	}

	private PrimitiveIterator.OfInt nodeIterator(MVMap<NSNKey, Value> index, long id) {
		NSNKey from = index.ceilingKey(new NSNKey(id, 0));
		if (from == null || from.nsnId() != id) return NodeSequence.emptyIterator;
		long idIncr = (int) id == -1 ? 0x200000000L : 1;
		NSNKey to = index.ceilingKey(new NSNKey(id + idIncr, 0));
		if (from.equals(to)) return NodeSequence.singleIterator(from.sourceId);
		return new MappedIntIterator<>(
				index.cursor(from),
				NSNKey::nodeId,
				to);
	}

	private PrimitiveIterator.OfLong edgeIterator(MVMap<NSNKey, Value> index, long id) {
		NSNKey from = index.ceilingKey(new NSNKey(id, 0));
		if (from == null || from.nsnId() != id) return EdgeSequence.emptyIterator;
		long idIncr = (int) id == -1 ? 0x200000000L : 1;
		NSNKey to = index.ceilingKey(new NSNKey(id + idIncr, 0));
		if (from.equals(to)) return EdgeSequence.singleIterator(from.sourceId);
		return new MappedLongIterator<>(
				index.cursor(from),
				NSNKey::edgeId,
				to);
	}

	private void applyNSNChange(MVMap<NSNKey, Value> index, NSNChange c) {
		boolean modified = applyNSNChange(index, c.nsnKey, c.added);
		if (!modified) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "incorrect state to apply " + c);
	}

	private boolean applyNSNChange(MVMap<NSNKey, Value> index, NSNKey key, boolean added) {
		return added ?
			index.put(key, Value.empty()) == null :
			index.remove(key) != null;
	}

}
