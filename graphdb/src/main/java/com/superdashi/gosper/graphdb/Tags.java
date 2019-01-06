package com.superdashi.gosper.graphdb;

import java.util.ArrayList;
import java.util.List;

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
