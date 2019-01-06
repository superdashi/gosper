package com.superdashi.gosper.display;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

final class AtomicDisplay implements ElementDisplay {

	private final Set<Element> els;

	AtomicDisplay(Element el) {
		if (el == null) throw new IllegalArgumentException("null el");
		els = Collections.singleton(el);
	}

	@Override
	public Collection<Element> getElements() {
		return els;
	}

}
