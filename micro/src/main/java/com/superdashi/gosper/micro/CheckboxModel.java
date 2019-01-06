package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Item;

public final class CheckboxModel extends Model {

	// fields

	final Mutations mutations;
	private Item item;
	private ItemModel itemModel;
	private boolean checked;

	// constructors

	// constructor for non-contextual snapshotted models
	CheckboxModel(Item item) {
		super((ActivityContext) null);
		mutations = null;
		this.item = item;
		itemModel = new ItemModel(item);
		checked = false;
	}

	CheckboxModel(ActivityContext context, Item item, Mutations mutations) {
		super(context);
		this.mutations = mutations;
		this.item = item;
		this.itemModel = new ItemModel(context, item, mutations);
		checked = false;
	}

	private CheckboxModel(CheckboxModel that, Mutations mutations) {
		super(that);
		this.mutations = mutations;
		this.item = that.item;
		this.itemModel = that.itemModel.copy(mutations);
		this.checked = that.checked;
	}

	// public accessors

	public Item item() {
		return item;
	}

	public void item(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
		if (item.equals(this.item)) return;
		this.item = item;
		Models models = models();
		this.itemModel = models == null ? new ItemModel(null, item, mutations) : models.itemModel(item, mutations);
		mutations.count ++;
		requestRedraw();
	}

	public boolean checked() {
		return checked;
	}

	public void toggleChecked() {
		checked = !checked;
		mutations.count ++;
		requestRedraw();
	}

	public void checked(boolean checked) {
		if (this.checked == checked) return;
		this.checked = checked;
		mutations.count ++;
		requestRedraw();
	}

	// object methods

	//TODO is this correct?? perhaps hashCode/equals should be default implementations

	@Override
	public int hashCode() {
		return Boolean.hashCode(checked) + item.hashCode() + 31 * itemModel.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof CheckboxModel)) return false;
		CheckboxModel that = (CheckboxModel) obj;
		return this.item.equals(that.item) && this.itemModel.equals(that.itemModel) && this.checked == that.checked;
	}

	@Override
	public String toString() {
		return item.toString();
	}

	// package scoped methods

	long revision() {
		return mutations == null ? 0 : mutations.count;
	}

	// used to make detached snapshots of actions
	CheckboxModel copy(Mutations mutations) {
		return new CheckboxModel(this, mutations);
	}

	ItemModel itemModel() {
		return itemModel;
	}

}
