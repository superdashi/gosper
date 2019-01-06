package com.superdashi.gosper.device;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

//TODO consider adding digit identifier for touch - repurpose key?
//TODO how to identify mouse buttons? - repurpose key ?
public class Event {

	// private statics

	private static final int FLAG_KEY    = 0b00000001;
	private static final int FLAG_MOVE   = 0b00000010;
	private static final int FLAG_POINT  = 0b00000100;
	private static final int FLAG_DOWN   = 0b00001000;
	private static final int FLAG_SHIFT  = 0b00010000;
	private static final int FLAG_CTRL   = 0b00100000;
	private static final int FLAG_ALT    = 0b01000000;
	private static final int FLAG_REPEAT = 0b10000000;

	private static int addModifiers(int flags, boolean shift, boolean ctrl, boolean alt) {
		if (shift)  flags |= FLAG_SHIFT;
		if (ctrl)   flags |= FLAG_CTRL;
		if (alt)    flags |= FLAG_ALT;
		return flags;
	}

	private static void checkKey(Type type, int key) {
		switch (type) {
		case KEY:
			if (key <= KEY_NONE || key >= NON_KEYBOARD_KEYS) throw new IllegalArgumentException("invalid key");
			break;
		case POINT:
		case MOVE:
			if (key != KEY_NONE && (key < NON_KEYBOARD_KEYS || key >= MAX_KEY)) throw new IllegalArgumentException("invalid key");
			break;
		}
	}

	// package statics

	static void checkKey(int key) {
		if (key <= KEY_NONE || key >= MAX_KEY) throw new IllegalArgumentException("invalid key");
	}

	// public statics

	public enum Type {
		//TODO consider renaming KEY to PRESS
		KEY, MOVE, POINT;
		static Type[] values = values();
	}

	public enum Modifier {
		SHIFT, CTRL, ALT
	}

	public static boolean isValidKey(int key) {
		return key >= KEY_NONE && key <= MAX_KEY;
	}

	// key events

	public static Event newKeyEvent(int key, boolean down) {
		return newKeyEvent(key, down, false, Collections.emptySet());
	}

	public static Event newKeyEvent(int key, boolean down, long time) {
		return newKeyEvent(key, down, false, Collections.emptySet(), time);
	}

	public static Event newKeyEvent(int key, boolean down, boolean repeat, Set<Modifier> modifiers) {
		return newKeyEvent(key, down, repeat, modifiers, System.currentTimeMillis());
	}

	public static Event newKeyEvent(int key, boolean down, boolean repeat, Set<Modifier> modifiers, long time) {
		checkKey(Type.KEY, key);
		if (modifiers == null) throw new IllegalArgumentException("null modifiers");
		return newKeyEvent(
				key,
				down,
				repeat,
				modifiers.contains(Modifier.SHIFT),
				modifiers.contains(Modifier.CTRL),
				modifiers.contains(Modifier.ALT),
				time
				);
	}

	public static Event newKeyEvent(int key, boolean down, boolean repeat, boolean shift, boolean ctrl, boolean alt, long time) {
		checkKey(Type.KEY, key);

		int flags          = FLAG_KEY;
		if (repeat) flags |= FLAG_REPEAT;
		if (down)   flags |= FLAG_DOWN;
		flags = addModifiers(flags, shift, ctrl, alt);

		return new Event(flags, key, 0, 0, time);
	}

	// point events

	public static Event newPointEvent(int key, int x, int y) {
		return newPointEvent(key, x, y, System.currentTimeMillis());
	}

	public static Event newPointEvent(int key, int x, int y, long time) {
		checkKey(Type.POINT, key);
		return new Event(FLAG_POINT | FLAG_DOWN, key, x, y, time);
	}

	public static Event newPointEvent(int key, int x, int y, boolean shift, boolean ctrl, boolean alt, long time) {
		checkKey(Type.POINT, key);
		int flags = addModifiers(FLAG_POINT | FLAG_DOWN, shift, ctrl, alt);
		return new Event(flags, key, x, y, time);
	}

	// move methods

	public static Event newMoveEvent(int key, int x, int y, boolean down, boolean repeat) {
		return newMoveEvent(key, x, y, down, repeat, System.currentTimeMillis());
	}

	public static Event newMoveEvent(int key, int x, int y, boolean down, boolean repeat, long time) {
		return newMoveEvent(key, x, y, down, repeat, Collections.emptySet(), System.currentTimeMillis());
	}

	public static Event newMoveEvent(int key, int x, int y, boolean down, boolean repeat, Set<Modifier> modifiers) {
		return newMoveEvent(key, x, y, down, repeat, modifiers, System.currentTimeMillis());
	}

	public static Event newMoveEvent(int key, int x, int y, boolean down, boolean repeat, Set<Modifier> modifiers, long time) {
		return newMoveEvent(
				key,
				x,
				y,
				down,
				repeat,
				modifiers.contains(Modifier.SHIFT),
				modifiers.contains(Modifier.CTRL),
				modifiers.contains(Modifier.ALT),
				time
				);
	}

	public static Event newMoveEvent(int key, int x, int y, boolean down, boolean repeat, boolean shift, boolean ctrl, boolean alt, long time) {
		checkKey(Type.MOVE, key);
		int flags          = FLAG_MOVE;
		if (repeat) flags |= FLAG_REPEAT;
		if (down)   flags |= FLAG_DOWN;
		flags = addModifiers(flags, shift, ctrl, alt);
		return new Event(flags, key, x, y, time);
	}

	// directions
	public static final int KEY_NONE     = 0;
	public static final int KEY_UP       = 1;
	public static final int KEY_DOWN     = 2;
	public static final int KEY_LEFT     = 3;
	public static final int KEY_RIGHT    = 4;
	public static final int KEY_CENTER   = 5;
	// general action
	public static final int KEY_CONFIRM  = 6;
	public static final int KEY_CANCEL   = 7;

	// alphas
	public static final int KEY_A = 65;
	public static final int KEY_B = 66;
	public static final int KEY_C = 67;
	public static final int KEY_D = 68;
	public static final int KEY_E = 69;
	public static final int KEY_F = 70;
	public static final int KEY_G = 71;
	public static final int KEY_H = 72;
	public static final int KEY_I = 73;
	public static final int KEY_J = 74;
	public static final int KEY_K = 75;
	public static final int KEY_L = 76;
	public static final int KEY_M = 77;
	public static final int KEY_N = 78;
	public static final int KEY_O = 79;
	public static final int KEY_P = 80;
	public static final int KEY_Q = 81;
	public static final int KEY_R = 82;
	public static final int KEY_S = 83;
	public static final int KEY_T = 84;
	public static final int KEY_U = 85;
	public static final int KEY_V = 86;
	public static final int KEY_W = 87;
	public static final int KEY_X = 88;
	public static final int KEY_Y = 89;
	public static final int KEY_Z = 90;

	// mouse
	public static final int KEY_MOUSE_1 = 128;
	public static final int KEY_MOUSE_2 = 129;
	public static final int KEY_MOUSE_3 = 130;

	//TODO more keys

	public static final int NON_KEYBOARD_KEYS = 128;
	public static final int MAX_KEY = 255;

	// fields

	final int flags;

	public final int key;
	public final int x;
	public final int y;
	public final long time;

	// constructors

	private Event(int flags, int key, int x, int y, long time) {
		this.flags = flags;
		this.key   = key;
		this.x     = x;
		this.y     = y;
		this.time = time;
	}

	// accessors

	public boolean isKey() {
		return isSet(FLAG_KEY);
	}

	public boolean isMove() {
		return isSet(FLAG_MOVE);
	}

	public boolean isPoint() {
		return isSet(FLAG_POINT);
	}

	public boolean isDown() {
		return isSet(FLAG_DOWN);
	}

	public boolean isShift() {
		return isSet(FLAG_SHIFT);
	}

	public boolean isCtrl() {
		return isSet(FLAG_CTRL);
	}

	public boolean isAlt() {
		return isSet(FLAG_ALT);
	}

	public boolean isRepeat() {
		return isSet(FLAG_REPEAT);
	}

	// friendly accessors

	public Type type() {
		return Type.values[Integer.numberOfTrailingZeros(flags & 7)];
	}

	public Set<Modifier> modifiers() {
		return new AbstractSet<Event.Modifier>() {
			private Modifier[] array = null;
			@Override public int size() { return array().length; }
			@Override public Iterator<Modifier> iterator() { return Arrays.stream(array()).iterator(); }

			@Override public boolean contains(Object obj) {
				if (!(obj instanceof Modifier)) return false;
				return isSet( 1 << (4 + ((Modifier) obj).ordinal()) );
			}

			private Modifier[] array() {
				if (array == null) {
					int length = 0;
					if (isShift()) length ++;
					if (isCtrl())  length ++;
					if (isAlt())   length ++;
					array = new Modifier[length];
					if (isAlt())   array[--length] = Modifier.ALT;
					if (isCtrl())  array[--length] = Modifier.CTRL;
					if (isShift()) array[--length] = Modifier.SHIFT;
				}
				return array;
			}
		};
	}

	// public method

	public Event withKeyState(boolean down, boolean repeat) {
		int flags = this.flags & ~(FLAG_DOWN | FLAG_REPEAT);
		if (down) flags |= FLAG_DOWN;
		if (repeat) flags |= FLAG_REPEAT;
		return flags == this.flags ? this : new Event(flags, key, x, y, time);
	}

	// object methods

	@Override
	public String toString() {
		Type type = type();
		StringBuilder sb = new StringBuilder();
		sb.append(type.toString()).append(' ');
		switch (type) {
		case KEY:
			sb.append(key).append(' ');
			break;
		case MOVE:
		case POINT:
			sb.append(x).append(',').append(y).append(' ');
			break;
		}
		sb
			.append(isRepeat() ? "continued-" : "initial-")
			.append(isDown() ? "down" : "up")
			.append(' ')
			.append(modifiers())
			.append(" @ ")
			.append(new Date(time));
		return sb.toString();
	}

	// private utility methods

	private boolean isSet(int flag) {
		return (flags & flag) != 0;
	}

	private String downText() {
		boolean down = isDown();
		boolean repeat = isRepeat();
		if (down) {
			return repeat ? "continued-down" : "continued-up";
		} else {
			return repeat ? "initial down" : "initial up";
		}
	}
}
