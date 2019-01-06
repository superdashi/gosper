package com.superdashi.gosper.micro;

public final class ActiveModel extends Model {

	private final Mutations mutations;

	protected ActiveModel(ActivityContext context) {
		super(context);
		this.mutations = new Mutations();
	}

	private ActiveModel(ActiveModel that) {
		super((ActivityContext) null);
		this.mutations = null;
		this.active = that.active;
	}

	private boolean active;

	public boolean active() {
		return active;
	}

	public void active(boolean active) {
		if (mutations == null) throw new IllegalStateException("immutable");
		this.active = active;
		mutations.count++;
		requestRedraw();
	}

	ActiveModel snapshot() {
		return mutations == null ? this : new ActiveModel(this);
	}

	@Override
	public int hashCode() {
		return active ? 1 : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ActiveModel)) return false;
		ActiveModel that = (ActiveModel) obj;
		return this.active == that.active;
	}

	@Override
	public String toString() {
		return active ? "active" : "inactive";
	}

}
