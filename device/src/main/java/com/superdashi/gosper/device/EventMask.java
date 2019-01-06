package com.superdashi.gosper.device;

import java.util.function.Predicate;

import com.superdashi.gosper.device.Event.Type;
import com.tomgibara.intgeom.IntRect;

public final class EventMask implements Predicate<Event> {

	//TODO find better names, or rename class - EventFilter? - implement predicate?
	private static final EventMask never = new EventMask(false);
	private static final EventMask always = new EventMask(true);
	private static final EventMask anyKey = builder().type(Type.KEY).build();
	private static final EventMask anyKeyDown = builder().type(Type.KEY).down(true).build();

	//TODO rename never/always to none/any
	public static final EventMask never()      { return never;      }
	public static final EventMask always()     { return always;     }
	public static final EventMask anyKey()     { return anyKey;     }
	public static final EventMask anyKeyDown() { return anyKeyDown; }

	public static class Builder {

		private int mask;
		private int pattern;
		private int key;
		private IntRect bounds;
		private EventMask alternative;

		private Builder() {
			reset();
		}

		public Builder any() {
			reset();
			return this;
		}

		public Builder type(Event.Type type) {
			if (type == null) throw new IllegalArgumentException("null type");
			mask |= 7;
			pattern &= ~7;
			pattern |= 1 << type.ordinal();
			return this;
		}

		public Builder anyType() {
			mask &= ~7;
			pattern &= ~7;
			return this;
		}

		public Builder down(boolean down) {
			mask |= 8;
			pattern = down ? pattern | 8 : pattern & ~ 8;
			return this;
		}

		public Builder anyDown() {
			mask &= ~8;
			pattern &= ~8;
			return this;
		}

		public Builder modifier(Event.Modifier modifier, boolean active) {
			if (modifier == null) throw new IllegalArgumentException("null modifier");
			int bit = 1 << (modifier.ordinal() + 4);
			mask |= bit;
			pattern = active ? pattern | bit : pattern & ~bit;
			return this;
		}

		public Builder anyModifier(Event.Modifier modifier) {
			if (modifier == null) throw new IllegalArgumentException("null modifier");
			int bit = 1 << (modifier.ordinal() + 4);
			mask &= ~ bit;
			pattern &= ~bit;
			return this;
		}

		public Builder repeat(boolean repeat) {
			mask |= 0x80;
			pattern = repeat ? pattern | 0x80 : pattern & ~ 0x80;
			return this;
		}

		public Builder anyRepeat() {
			mask &= ~0x80;
			pattern &= ~0x80;
			return this;
		}

		public Builder key(int key) {
			Event.checkKey(key);
			this.key = key;
			return this;
		}

		public Builder anyKey() {
			key = -1;
			return this;
		}

		//TODO disallow degenerate bounds?
		public Builder bounds(IntRect bounds) {
			if (bounds == null) throw new IllegalArgumentException("null bounds");
			this.bounds = bounds;
			return this;
		}

		public Builder anyBounds() {
			bounds = null;
			return this;
		}

		public Builder or() {
			EventMask mask = build();
			reset();
			alternative = mask;
			return this;
		}

		public EventMask build() {
			return new EventMask(this);
		}

		private void reset() {
			mask = 0;
			pattern = 0;
			key = -1;
			bounds = null;
			alternative = null;
		}
	}

	//TODO rename to newBuilder for consistency?
	public static Builder builder() { return new Builder(); }

	public static EventMask keyDown(int key) {
		Event.checkKey(key);
		return new EventMask(key, true);
	}

	public static EventMask keyUp(int key) {
		Event.checkKey(key);
		return new EventMask(key, false);
	}

	//TODO could change to use an int array instead of chaining if alternatives are used extensively
	private final int mask;
	private final int pattern;
	private final int key;
	private final IntRect bounds;
	private final EventMask alternative;


	private EventMask(boolean always) {
		if (always) {
			// always matches because there are no constraints
			mask = 0;
			pattern = 0;
			key = -1;
			bounds = null;
			alternative = null;
		} else {
			// never matches due to an illegal key code
			mask = 0;
			pattern = 0;
			key = Event.MAX_KEY + 1;
			bounds = null;
			alternative = null;
		}
	}

	private EventMask(int key, boolean down) {
		this.key = key;
		mask = 8;
		pattern = down ? 8 : 0;
		bounds = null;
		alternative = null;
	}

	private EventMask(Builder builder) {
		this.mask        = builder.mask;
		this.pattern     = builder.pattern;
		this.key         = builder.key;
		this.bounds      = builder.bounds;
		this.alternative = builder.alternative;
	}


	@Override
	public boolean test(Event event) {
		return matchesThis(event) || alternative != null && alternative.test(event);
	}

	private boolean matchesThis(Event event) {
		if (pattern != (event.flags & mask)) return false;
		if (key != -1 && key != event.key) return false;
		if (bounds != null && (event.isMove() || event.isPoint()) && !bounds.containsPoint(event.x, event.y)) return false;
		return true;
	}

}
