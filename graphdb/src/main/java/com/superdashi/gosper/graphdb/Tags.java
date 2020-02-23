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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class Tags {

	private final Part part;
	private final ArrayList<Tag> list;

	Tags(Part part) {
		this.part = part;
		list = part.data.extractTagList(part);
	}

	public int count() {
		part.checkNotDeleted();
		return list.size();
	}

	public boolean contains(Tag tag) {
		if (tag == null) throw new IllegalArgumentException("null tag");
		part.checkNotDeleted();
		//TODO could use binary search
		return list.contains(tag);
	}

	public boolean add(Tag tag) {
		if (tag == null) throw new IllegalArgumentException("null tag");
		part.checkNotDeleted();
		part.checkModifiable();
		if (list.contains(tag)) return false;
		list.add(tag);
		part.recordDirty(PartData.FLAG_DIRTY_TAGS);
		return true;
	}

	public boolean add(String tag) {
		return add(newTag(tag));
	}

	public boolean remove(Tag tag) {
		if (tag == null) throw new IllegalArgumentException("null tag");
		part.checkNotDeleted();
		part.checkModifiable();
		if (!list.remove(tag)) return false;
		part.recordDirty(PartData.FLAG_DIRTY_TAGS);
		return true;
	}

	public boolean remove(String tag) {
		return remove(newTag(tag));
	}

	public Stream<Tag> stream() {
		return Arrays.stream(list.toArray(new Tag[0]));
	}

	void populateData(List<Change> changes) {
		part.data.applyTagList(list, part, changes);
	}

	String toStringImpl() {
		return list.toString();
	}

	private Tag newTag(String tagName) {
		return part.visit.newTag(tagName);
	}

}
