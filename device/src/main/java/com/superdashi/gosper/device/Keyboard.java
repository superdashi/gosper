package com.superdashi.gosper.device;

import java.util.Arrays;

public final class Keyboard {

	// statics

	private static Keyboard withNoKeys = new Keyboard(KeySet.empty(), new int[0]);

	//                          unused->|<- key->SCA|<- character ->
	private static int KEY_MASK = 0b00000111111111110000000000000000;

	private static int pack(int key, boolean shift, boolean ctrl, boolean alt) {
		int v = key;
		v <<= 1;
		if (shift) v |= 1;
		v <<= 1;
		if (ctrl) v |= 1;
		v <<= 1;
		if (alt) v |= 1;
		v <<= 16;
		return v;
	}

	public static Keyboard withNoKeys() {
		return withNoKeys;
	}

	public static class Builder {

		private final KeySet keySet;
		private int[] map;
		private int count;

		private Builder(KeySet keySet) {
			this.keySet = keySet;
			map = new int[16];
			count = 0;
		}

		public void assign(int key, boolean shift, boolean ctrl, boolean alt, char c) {
			if (!keySet.containsKey(key)) throw new IllegalArgumentException("unsupported key");
			if (c == 0) throw new IllegalArgumentException("invalid c");
			if (count == map.length) map = Arrays.copyOf(map, map.length * 2);
			map[count++] = pack(key, shift, ctrl, alt) | c;
		}

		public Keyboard build() {
			// check for trivial case, count check is a guaranteed cheap pre-check
			if (count == 0 && keySet.keyCount() == 0) return withNoKeys;
			// trim map
			if (count < map.length) map = Arrays.copyOf(map, count);
			// sort for binary searching
			Arrays.sort(map);
			//check for dupes
			for (int i = 1; i < map.length; i++) {
				if ((map[i - 1] & KEY_MASK) == (map[i] & KEY_MASK)) throw new IllegalStateException("duplicate mapping for key: code = " + (map[i] >> 21));
			}
			return new Keyboard(keySet, map);
		}
	}

	public static Builder newBuilder(KeySet keySet) {
		if (keySet == null) throw new IllegalArgumentException("null keySet");
		return new Builder(keySet.immutableCopy());
	}

	// fields

	public final KeySet keySet;
	private final int[] map;

	public Keyboard(KeySet keySet, int[] map) {
		this.keySet = keySet;
		this.map = map;
	}

	// zero if not present
	public char charFor(int key, boolean shift, boolean ctrl, boolean alt) {
		int k = pack(key, shift, ctrl, alt);
		int i = Arrays.binarySearch(map, k); // note: cannot be +ve since character is never zero
		i = -1 - i;
		if (i == map.length) return 0;
		int v = map[i];
		if ((v & KEY_MASK) != k) return 0;
		return (char) v;
	}
}
