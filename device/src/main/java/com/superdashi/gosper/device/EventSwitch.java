package com.superdashi.gosper.device;

import java.util.Optional;

public final class EventSwitch<E> {

	private static EventMask[] checkedMasks(EventMask... masks) {
		if (masks == null) throw new IllegalArgumentException("null masks");
		for (EventMask mask : masks) {
			if (mask == null) throw new IllegalArgumentException("null mask");
		}
		return masks.clone();
	}

	public static EventSwitch<Void> over(EventMask... masks) {
		return new EventSwitch<>(checkedMasks(masks));
	}

	public static <E extends Enum<E>> EventSwitch<E> over(Class<E> clss, EventMask... masks) {
		EventMask[] checkedMasks = checkedMasks(masks);
		E[] enumConstants = clss.getEnumConstants();
		if (enumConstants.length != checkedMasks.length) throw new IllegalArgumentException("incorrect number of masks for enum");
		return new EventSwitch<>(checkedMasks, enumConstants);
	}

	private final EventMask[] masks;
	private final E[] values;

	private EventSwitch(EventMask[] masks) {
		this.masks = masks;
		values = null;
	}

	private EventSwitch(EventMask[] masks, E[] values) {
		this.masks = masks;
		this.values = values;
	}

	// -1 on no match
	public int ordinalFor(Event event) {
		if (event == null) throw new IllegalArgumentException("null event");
		for (int i = 0; i < masks.length; i++) {
			if (masks[i].test(event)) return i;
		}
		return -1;
	}

	public Optional<E> valueFor(Event event) {
		int i = ordinalFor(event);
		return i == -1 ? Optional.empty() : Optional.of(values[i]);
	}

}
