package com.superdashi.gosper.config;

import java.util.Optional;

import com.tomgibara.storage.Store;

public interface Configurable {

	enum Type {

		BACKGROUND,
		BAR,
		CLOCK,
		PANEL,
		PLATE,
		DATA,
		DESIGN,
		;

		private final String str;

		private Type() {
			str = name().toLowerCase();
		}

		@Override
		public String toString() {
			return str;
		}
	}

	Type getStyleType();

	//TODO needs to expose namespace too
	String getId();

	Optional<String> getRole();

	ConfigTarget openTarget();

	default Store<? extends Configurable> getStyleableChildren() {
		return ConfigUtil.none();
	}

	//TODO change to optional
	default Configurable getStyleableParent() {
		return null;
	}

	// zero indexed is parent
	default Store<? extends Configurable> getStyleableAncestors() {
		//TODO want a helper method for this
		final Configurable p0 = getStyleableParent();
		if (p0 == null) return ConfigUtil.none();
		final Configurable p1 = p0.getStyleableParent();
		if (p1 == null) return ConfigUtil.singleton(p0);
		Store<Configurable> store = ConfigUtil.storeType.storage().newStore(4);
		int i = 0;
		store.set(i++, p0);
		Configurable p = p1;
		do {
			if (i == store.size()) {
				store = store.resizedCopy(i * 2);
			}
			store.set(i++, p);
			p = p.getStyleableParent();
		} while (p != null);
		return store.immutableView().range(0, i);
	}

}
