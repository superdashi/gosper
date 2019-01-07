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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;

final class KeyboardDesign {

	private static final Map<String, KeyboardDesign> resourceDesigns = new HashMap<>();

	//TODO could be more efficient - but worth changing?
	//TODO should check string length
	private static final int[][] split(String str, int stateTotal, State... states) {
		int stateCount = states.length;
		if (stateCount == 0) throw new IllegalArgumentException();
		int keyCount = str.length() / stateCount;
		int[][] chars = new int[keyCount][stateTotal];
		for (int[] cs : chars) {
			Arrays.fill(cs, -1);
		}
		int[] codes = str.codePoints().toArray();
		for (int i = 0; i < codes.length; i++) {
			chars[i / stateCount][states[i % stateCount].ordinal] = codes[i];
		}
		return chars;
	}

	private static int[] parseInts(String str, int len) {
		String[] parts = str.split(",");
		if (len > 0 && parts.length != len) throw new IllegalArgumentException("incorrect number of values");
		int[] ints = new int[parts.length];
		for (int i = 0; i < ints.length; i++) {
			String part = parts[i];
			try {
				ints[i] = Integer.parseInt(part);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("not an integer: " + part);
			}
		}
		return ints;
	}

	private static IntRect parseRect(String str) {
		int[] ints = parseInts(str, 4);
		return IntRect.rectangle(ints[0], ints[1], ints[2], ints[3]);
	}

	private static IntMargins parseMargins(String str) {
		int[] ints = parseInts(str, 0);
		switch (ints.length) {
		case 1: return IntMargins.uniform(ints[0]);
		case 2: return IntMargins.widths(ints[0], ints[0], ints[1], ints[1]);
		case 3: break;
		case 4: return IntMargins.widths(ints[0], ints[1], ints[2], ints[3]);
		default:
		}
		throw new IllegalArgumentException("incorrect number of margin values");
	}

	// null indicates all states
	private static State[] parseStates(String str, Map<String, State> stateMap) {
		if (str.equals("*")) return stateMap.values().toArray(new State[stateMap.size()]);
		String[] parts = str.split(",");
		State[] states = new State[parts.length];
		HashSet<String> check = new HashSet<>();
		for (int i = 0; i < states.length; i++) {
			String part = parts[i];
			if (!check.add(part)) throw new IllegalArgumentException("duplicate state: " + part);
			State state = stateMap.get(part);
			if (state == null) throw new IllegalArgumentException("invalid state: " + part);
			states[i] = state;
		}
		return states;
	}

	private static IntVector parseVector(String str) {
		int[] ints = parseInts(str, 2);
		return IntVector.to(ints[0], ints[1]);
	}

	static KeyboardDesign fromResource(String resourceName) {
		KeyboardDesign design = null;
		synchronized (resourceDesigns) {
			if (resourceDesigns.containsKey(resourceName)) {
				design = resourceDesigns.get(resourceName);
				if (design == null) throw new RuntimeException("failed to load keyboard design from resource: " + resourceName);
				return design;
			}
		}
		try {
			InputStream stream = KeyboardDesign.class.getClassLoader().getResourceAsStream(resourceName);
			if (stream == null) throw new IllegalArgumentException("no resource with name: " + resourceName);
			List<String> lines = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				while (true) {
					String line = reader.readLine();
					if (line == null) break;
					lines.add(line);
				}
			} catch (IOException e) {
				throw new RuntimeException("failed to load keyboard design from resource: " + resourceName, e);
			}
			design = new KeyboardDesign(lines);
		} finally {
			synchronized (resourceDesigns) {
				resourceDesigns.put(resourceName, design);
			}
		}
		return design;
	}

	//TODO use collect
	final List<State> states;
	final Map<String, State> statesByName;
	final ModelOp[] ops;
	private final String[] stateBackdrops;
	final boolean opaqueBackdrops;
	final State initialState;
	final Set<Key> keys;
	final IntRect textArea;
	final int textLines;

	private final Key[] initialKeys;

	KeyboardDesign(List<String> lines) {
		List<State> states = new ArrayList<>();
		List<ModelOp> ops = new ArrayList<>();
		Map<String, State> stateMap = new LinkedHashMap<>();
		Map<String, Integer> opMap = new HashMap<>();
		String[] stateBackdrops = null;
		boolean opaqueBackdrops = false;
		State initialState = null;
		Set<Key> keys = new HashSet<>();
		Key[] initialKeys = null;
		IntRect textArea = null;
		int textLines = 1;

		int mode = 0; // 0 states, 1 transitions
		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty()) continue;
			if (line.charAt(0) == '#') continue;
			switch (mode) {
			case 0: // state names
				String[] stateNames = line.split("\\s+");
				for (int i = 0; i < stateNames.length; i++) {
					//TODO check state names
					State state = new State(i, stateNames[i]);
					states.add(state);
					State old = stateMap.put(state.name, state);
					if (old != null) throw new IllegalArgumentException("duplicated state name");
				}
				stateBackdrops = new String[states.size()];
				mode = 1;
				break;
			case 1: // state transitions
				String[] parts = line.split("\\s+");
				switch (parts[0]) {
				case "on_char": {
					if (parts.length != 3) throw new IllegalArgumentException("invalid state transition");
					State from = stateMap.get(parts[1]);
					if (from == null) throw new IllegalArgumentException("unknown transition state: " + parts[1]);
					State to = stateMap.get(parts[2]);
					if (to == null) throw new IllegalArgumentException("unknown transition state: " + parts[2]);
					from.transTo = to;
					break;
				}
				case "state_op": {
					if (parts.length < 4) throw new IllegalArgumentException("insufficient state op parts");
					//TODO need to validate name
					String name = parts[1];
					// structured as 0->1, 2->3 ... n=default
					State[] opStates = new State[(parts.length - 3) * 2 + 1];
					State def = stateMap.get(parts[parts.length - 1]);
					if (def == null) throw new IllegalArgumentException("unknown default state: " + parts[parts.length - 1]);
					opStates[opStates.length - 1] = def;
					for (int i = 2; i < parts.length - 1; i++) {
						String part = parts[i];
						int j = part.indexOf("->");
						if (j == -1) throw new IllegalArgumentException("no transition in op state part: " + part);
						State from = stateMap.get(part.substring(0, j));
						State to = stateMap.get(part.substring(j + 2));
						if (from == null || to == null) throw new IllegalArgumentException("invalid op states: " + parts);
						opStates[(i-2)*2    ] = from;
						opStates[(i-2)*2 + 1] = to  ;
					}
					ModelOp op = new StateOp(opStates);
					opMap.put(name, ops.size());
					ops.add(op);
					break;
				}
				case "backdrop": {
					if (parts.length < 3) throw new IllegalArgumentException("insufficient backdrop parts");
					State state = stateMap.get(parts[1]);
					if (state == null) throw new IllegalArgumentException("unknown state: " + parts[1]);
					stateBackdrops[state.ordinal] = parts[2];
					break;
				}
				case "backdrops_opaque" : {
					if (parts.length != 2) throw new IllegalArgumentException("incorrect number of backdrop_opaque parts");
					opaqueBackdrops = Boolean.parseBoolean(parts[1]);
					break;
				}
				case "initial_state": {
					if (parts.length != 2) throw new IllegalArgumentException("incorrect number of initial_state parts");
					initialState = stateMap.get(parts[1]);
					if (initialState == null) throw new IllegalArgumentException("unknown state: " + parts[1]);
					break;
				}
				case "key_row": {
					//TODO repeating this is a bit ugly
					parts = line.split("\\s+", 6);
					if (parts.length < 6) throw new IllegalArgumentException("insufficient key row parts");
					//rect, border, step, states, characters
					IntRect first = parseRect(parts[1]);
					IntMargins border = parseMargins(parts[2]);
					IntVector step = parseVector(parts[3]);
					State[] keyStates = parseStates(parts[4], stateMap);
					int[][] chars = split(parts[5], states.size(), keyStates);
					Row row = new Row(first, border, step, keyStates, chars);
					keys.addAll(row.generateKeys());
					break;
				}
				case "key": {
					if (parts.length != 4) throw new IllegalArgumentException("incorrect number of key parts");
					IntRect rect = parseRect(parts[1]);
					IntMargins border = parseMargins(parts[2]);
					int[] chars = new int[states.size()];
					boolean first = true;
					for (String op : parts[3].split(",")) {
						String[] split = op.split(":", 2);
						if (split.length != 2) throw new IllegalArgumentException("invalid key operation");
						String stateName = split[0];
						State state;
						if (stateName.equals("*")) {
							if (!first) throw new IllegalArgumentException("wildcard state must be first");
							state = null; // indicates all states
						} else {
							state = stateMap.get(stateName);
							if (state == null) throw new IllegalArgumentException("invalid state for key: " + stateName);
						}
						String opName = split[1];
						Integer index = opMap.get(opName);
						int value;
						if (index == null) {
							try {
								value = Integer.parseInt(opName);
								if (value < 0) throw new IllegalArgumentException("negative character code");
							} catch (IllegalArgumentException e) {
								throw new IllegalArgumentException("invalid character code");
							}
						} else {
							value = -2 - index;
						}
						first = false;
						if (state == null) {
							Arrays.fill(chars, value);
							break;
						} else {
							if (first) Arrays.fill(chars, -1);
							chars[state.ordinal] = value;
						}
					}
					Key key = new Key(rect, border, chars);
					keys.add(key);
					break;
				}
				case "text_area": {
					if (parts.length != 2) throw new IllegalArgumentException("incorrect number of parts for text area");
					textArea = parseRect(parts[1]);
					break;
				}
				case "text_lines": {
					if (parts.length != 2) throw new IllegalArgumentException("incorrect number of parts for text lines");
					int i;
					try {
						i = Integer.parseInt(parts[1]);
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException("not an integer for text lines");
					}
					if (i < 1) throw new IllegalArgumentException("invalid value for text lines");
					textLines = i;
					break;
				}
				case "initial_keys": {
					if (initialKeys != null) throw new IllegalArgumentException("duplicate initial keys");
					if (parts.length != 2) throw new IllegalArgumentException("incorrect number of parts for initial_keys");
					int[] chars = parseInts(parts[1], states.size());
					initialKeys = new Key[chars.length];
					outer: for (int i = 0; i < chars.length; i++) {
						int c = chars[i];
						for (Key key : keys) {
							if (key.chars[i] == c) {
								initialKeys[i] = key;
								continue outer;
							}
						}
						throw new IllegalArgumentException("no key for character " + c);
					}
					mode = 2;
					break;
				}
					default: throw new IllegalArgumentException("invalid line type: " + parts[0]);
				}
				break;
			default:
				throw new IllegalArgumentException("unexpected line");
			}
		}

		if (mode != 2) throw new IllegalArgumentException("missing configuration");
		if (initialState == null) throw new IllegalArgumentException("no initial state");
		for (int i = 0; i < stateBackdrops.length; i++) {
			if (stateBackdrops[i] == null) throw new IllegalArgumentException("missing backdrop for state " + states.get(i).name);
		}
		if (textArea == null) throw new IllegalArgumentException("no text area set");

		this.states = states;
		this.statesByName = Collections.unmodifiableMap(stateMap);
		this.ops = ops.toArray(new ModelOp[ops.size()]);
		this.stateBackdrops = stateBackdrops;
		this.opaqueBackdrops = opaqueBackdrops;
		this.initialState = initialState;
		this.initialKeys = initialKeys;
		this.keys = Collections.unmodifiableSet(keys);
		this.textArea = textArea;
		this.textLines = textLines;
	}

	Key initialKeyForState(State state) {
		if (state == null) throw new IllegalArgumentException("null state");
		return initialKeys[state.ordinal];
	}

	String backdropForState(State state) {
		if (state == null) throw new IllegalArgumentException("null state");
		return stateBackdrops[state.ordinal];
	}

	Stream<Key> keysForState(State state) {
		if (state == null) throw new IllegalArgumentException("null state");
		return keys.stream().filter(k -> k.charInState(state) != -1);
	}

	void validateKey(Key key) {
		if (key.keyboard() != this) throw new IllegalArgumentException("invalid key");
	}

	void validateState(State state) {
		if (state.keyboard() != this) throw new IllegalArgumentException("invalid state");
	}

	final class State {

		final int ordinal;
		final String name;
		State transTo = null;

		State(int ordinal, String name) {
			this.ordinal = ordinal;
			this.name = name;
		}

		KeyboardDesign keyboard() {
			return KeyboardDesign.this;
		}

		@Override
		public int hashCode() {
			return ordinal + name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof KeyboardDesign.State)) return false;
			KeyboardDesign.State that = (KeyboardDesign.State) obj;
			return this.ordinal == that.ordinal && this.name.equals(that.name);
		}

		@Override
		public String toString() {
			return name;
		}

	}

	final class Key {

		final IntRect face;
		final IntMargins border;
		private final int[] chars;
		final int keyCode = -1; //TODO support

		Key(IntRect face, IntMargins border, int[] chars) {
			this.face = face;
			this.border = border;
			this.chars = chars;
		}

		KeyboardDesign keyboard() {
			return KeyboardDesign.this;
		}

		int charInState(State state) {
			return chars[state.ordinal];
		}
		ModelOp operationInState(State state) {
			int c = chars[state.ordinal];
			switch (c) {
			case  -1 : return ModelOp.NO_OP;
			case   8 : return KeyboardModel::deleteBehind;
			case  23 : return ModelOp.CONFIRM; //ETB
			case  24 : return ModelOp.CANCEL; //CAN
			case 127 : return KeyboardModel::deleteAhead;
			default:
				if (c >= 32) { // a regular key press
					if (state.transTo != null) {
						return m -> {
							m.state(state.transTo);
							return m.insertCharBehind(c);
						};
					} else { // just a regular character, nothing else
						return m -> m.insertCharBehind(c);
					}
				} else if (c < -1) {
					int i = -2 - c;
					if (i >= ops.length) throw new IllegalStateException("invalid model op index");
					return ops[i];
				} else {
					return ModelOp.NO_OP;
				}
			}
		}

	}

	final class Row {
		final IntRect first;
		final IntMargins border;
		final IntVector step;
		final int[][] chars;

		Row(IntRect first, IntMargins border, IntVector step, State[] states, int[][] chars) {
			this.first = first;
			this.border = border;
			this.step = step;
			this.chars = chars;
		}

		Collection<Key> generateKeys() {
			Key[] keys = new Key[chars.length];
			IntRect face = first;
			for (int i = 0; i < keys.length; i++) {
				keys[i] = new Key(face, border, chars[i]);
				face = face.translatedBy(step);
			}
			return Arrays.asList(keys);
		}

	}

	interface ModelOp {

		boolean operateOn(KeyboardModel model);

		default boolean isNoop() { return this == NO_OP; }
		default boolean isConfirm() { return this == CONFIRM; }
		default boolean isCancel() { return this == CANCEL; }

		static final ModelOp NO_OP = m -> false;
		static final ModelOp CONFIRM = m -> false;
		static final ModelOp CANCEL = m -> false;

	}

	private static class StateOp implements ModelOp {

		private final State[] states;

		StateOp(State... states) {
			this.states = states;
		}

		@Override
		public boolean operateOn(KeyboardModel model) {
			State state = model.state();
			for (int i = 0; i < states.length - 1; i += 2) {
				if (states[i] == state) {
					return model.state(states[i + 1]);
				}
			}
			return model.state(states[states.length - 1]);
		}
	}
}
