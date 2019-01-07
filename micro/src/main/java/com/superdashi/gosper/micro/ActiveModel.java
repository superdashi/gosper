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
