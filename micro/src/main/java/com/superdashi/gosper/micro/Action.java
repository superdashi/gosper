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
import java.util.Arrays;
import java.util.Optional;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.Keyboard;
import com.superdashi.gosper.item.Item;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

//TODO can we guard reserved action ids? Should we?
//TODO make serializable
public class Action implements Serializable {

	// statics

	public static final String ID_NO_ACTION         = "gosper:no_action";
	public static final String ID_EDIT_FIELD        = "gosper:edit_field";
	public static final String ID_CHANGE_VALUE      = "gosper:change_value";
	public static final String ID_SELECT_ROW        = "gosper:select_row";
	public static final String ID_DIALOG_OPTION     = "gosper:dialog_option";
	public static final String ID_ACTIVATE_TOGGLE   = "gosper:activate_toggle";
	public static final String ID_DEACTIVATE_TOGGLE = "gosper:deactivate_toggle";
	public static final String ID_NEUTRALIZE_TOGGLE = "gosper:neutralize_toggle";

	private static final int[] NO_KEYS = {};
	private static final Action NO_ACTION = new Action(ID_NO_ACTION, Item.nothing(), Optional.empty(), NO_KEYS);

	//TODO how to limit valid action ids?
	// must be at least as permissive as items
	private static void checkId(String id) {
		if (id == null) throw new IllegalArgumentException("null id");
		if (id.isEmpty()) throw new IllegalArgumentException("empty id");
	}

	private static void checkItem(Item item) {
		if (item == null) throw new IllegalArgumentException("null item");
	}

	static int[] checkedKeys(boolean clone, int... keys) {
		if (keys == null) throw new IllegalArgumentException("null keys");
		int len = keys.length;
		if (len == 0) return NO_KEYS;
		if (clone) keys = keys.clone();
		if (len > 1) Arrays.sort(keys);
		if (keys[0] < 0 || keys[len -1] > Event.MAX_KEY) throw new IllegalArgumentException("invalid key");
		if (len > 1) {
			for (int i = 1; i < len; i++) {
				if (keys[i] == keys[i - 1]) throw new IllegalArgumentException("duplicate key");
			}
		}
		return keys;
	}

	private static int[] checkedKeys(int... keys) {
		return keys == NO_KEYS ? NO_KEYS : checkedKeys(true, keys);
	}

	private static int[] checkParams(String id, Item item, int[] keys) {
		checkId(id);
		checkItem(item);
		return checkedKeys(keys);
	}

	public static Action noAction() {
		return NO_ACTION;
	}

	public static Action create(String id, String label) {
		return create(id, label, null);
	}

	public static Action create(String id, String label, DeferredActivity deferredActivity) {
		return create(id, label, deferredActivity, NO_KEYS);
	}

	public static Action create(String id, String label, DeferredActivity deferredActivity, int... keys) {
		return create(id, Item.fromLabel(label), deferredActivity, keys);
	}

	public static Action create(String id, Item item) {
		return create(id, item, null);
	}

	public static Action create(String id, Item item, DeferredActivity deferredActivity) {
		return create(id, item, deferredActivity, NO_KEYS);
	}

	public static Action create(String id, Item item, DeferredActivity deferredActivity, int... keys) {
		keys = checkParams(id, item, keys);
		return new Action(id, item, Optional.ofNullable(deferredActivity), keys);
	}

	public static Action deserialize(ReadStream r) {
		String id = r.readChars();
		Item item = Item.deserialize(r);
		DeferredActivity deferredActivity = r.readBoolean() ? DeferredActivity.deserialize(r) : null;
		int[] keys = new int[r.readInt()];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = r.readInt();
		}
		return create(id, item, deferredActivity, keys);
	}

	// fields

	public final String id;
	public final Item item;
	//TODO hide behind accessor
	public final Optional<DeferredActivity> deferredActivity;
	private final int[] keys;

	// constructors

	private Action(String id, Item item, Optional<DeferredActivity> deferredActivity, int[] keys) {
		this.id = id;
		this.item = item;
		this.deferredActivity = deferredActivity;
		this.keys = keys;
	}

	// accessors

	// convenience method for accessing name of item
	public String label() {
		return item.label().orElse("");
	}

	public int[] keys() {
		return keys.clone();
	}

	// public methods

	public Action withId(String id) {
		checkId(id);
		return id.equals(this.id) ? this : new Action(id, item, deferredActivity, keys);
	}

	public Action withDeferredActivity(DeferredActivity deferredActivity) {
		Optional<DeferredActivity> opt = Optional.of(deferredActivity);
		return opt.equals(deferredActivity) ? this : new Action(id, item, opt, keys);
	}

	public Action withItem(Item item) {
		checkItem(item);
		return item.equals(this.item) ? this : new Action(id, item, deferredActivity, keys);
	}

	public Action withKeys(int... keys) {
		keys = checkedKeys(keys);
		return Arrays.equals(this.keys, keys) ? this : new Action(id, item, deferredActivity, keys);
	}

	public boolean matchesKey(int key) {
		for (int k : keys) {
			if (k == key) return true;
		}
		return false;
	}

	public boolean isAccessibleFrom(Keyboard keyboard) {
		if (keyboard == null) throw new IllegalArgumentException("null keyboard");
		// slightly naughty: in theory keyboard could mutate keys
		return keyboard.keySet.containsAnyKeyOf(keys);
	}

	public void serialize(WriteStream w) {
		w.writeChars(id);
		item.serialize(w);
		if (deferredActivity.isPresent()) {
			w.writeBoolean(true);
			deferredActivity.get().serialize(w);
		} else {
			w.writeBoolean(false);
		}
		w.writeInt(keys.length);
		for (int key : keys) w.writeInt(key);
	}

	// object methods

	@Override
	public int hashCode() {
		return id.hashCode() + item.hashCode() + deferredActivity.hashCode() + Arrays.hashCode(keys);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Action)) return false;
		Action that = (Action) obj;
		return this.id.equals(that.id) && this.item.equals(that.item) && this.deferredActivity.equals(that.deferredActivity) && Arrays.equals(this.keys, that.keys);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Action [id: ").append(id).append(", item: ").append(item);
		if (deferredActivity.isPresent()) sb.append(", activity: ").append(deferredActivity.get());
		if (keys.length > 0) sb.append(", keys: ").append(Arrays.toString(keys));
		return sb.append(']').toString();
	}

}
