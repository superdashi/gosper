package com.superdashi.gosper.config;

import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public class ConfigUtil {

	public static final StoreType<Configurable> storeType = StoreType.of(Configurable.class);

	public static final Store<Configurable> none() {
		return storeType.emptyStore();
	}

	public static final Store<Configurable> singleton(Configurable s) {
		if (s == null) throw new IllegalArgumentException("null s");
		return storeType.constantStore(s, 1);
	}

	public static final Store<Configurable> array(Configurable... configurables) {
		return storeType.arrayAsStore(configurables);
	}

	public static final Store<Configurable> size(int size) {
		return storeType.storage().newStore(size);
	}

}
