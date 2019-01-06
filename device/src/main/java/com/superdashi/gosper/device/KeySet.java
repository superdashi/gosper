package com.superdashi.gosper.device;

import java.util.Set;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.tomgibara.fundament.Mutability;

public final class KeySet implements Mutability<KeySet> {

	// statics

	private static final KeySet empty = new KeySet(Bits.zeroBits(Event.MAX_KEY + 1));

	private static BitStore newStore() {
		return Bits.store(Event.MAX_KEY + 1);
	}

	public static KeySet empty() {
		return empty;
	}

	public static KeySet create() {
		return new KeySet(newStore());
	}

	public static KeySet of(int... keys) {
		if (keys == null) throw new IllegalArgumentException("null keys");
		BitStore store = newStore();
		try {
			for (int key : keys) {
				store.setBit(key, true);
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key");
		}
		return new KeySet(store);
	}

	// fields

	private final BitStore keys;

	// constructors

	// creates an empty mutable set
	private KeySet(BitStore keys) {
		this.keys = keys;
	}

	// methods

	public int keyCount() {
		return keys.ones().count();
	}

	public boolean containsKey(int key) {
		try {
			return keys.getBit(key);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key");
		}
	}

	public boolean containsAnyKeyOf(int... keys) {
		if (keys == null) throw new IllegalArgumentException("null keys");
		try {
			for (int key : keys) {
				if (this.keys.getBit(key)) return true;
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key");
		}
		return false;
	}

	public boolean containsAllKeysOf(int... keys) {
		if (keys == null) throw new IllegalArgumentException("null keys");
		try {
			for (int key : keys) {
				if (!this.keys.getBit(key)) return false;
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key");
		}
		return true;
	}

	public boolean containsAllKeysInRange(int from, int to) {
		try {
			return keys.range(from, to + 1).ones().isAll();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key range");
		}
	}

	public Set<Integer> asSet() {
		return keys.ones().asSet();
	}

	public boolean addKey(int key) {
		try {
			return !keys.getThenSetBit(key, true);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key");
		}
	}

	public boolean removeKey(int key) {
		try {
			return keys.getThenSetBit(key, false);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key");
		}
	}

	public boolean addKeyRange(int fromKey, int toKey) {
		BitStore range;
		try {
			range = keys.range(fromKey, toKey);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid key range");
		}
		if (range.ones().isAll()) return false;
		range.setAll(true);
		return true;
	}

	// mutability methods

	@Override
	public boolean isMutable() {
		return keys.isMutable();
	}

	@Override
	public KeySet mutableCopy() {
		return new KeySet(keys.mutableCopy());
	}

	@Override
	public KeySet immutableCopy() {
		return new KeySet(keys.immutableCopy());
	}

	@Override
	public KeySet immutableView() {
		return keys.isMutable() ? new KeySet(keys.immutableView()) : this;
	}

	// object methods

	@Override
	public int hashCode() {
		return keys.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof KeySet)) return false;
		KeySet that = (KeySet) obj;
		return this.keys.equals(that.keys);
	}

	public String toString() {
		return asSet().toString();
	}
}
