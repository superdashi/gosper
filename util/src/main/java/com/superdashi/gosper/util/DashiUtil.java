package com.superdashi.gosper.util;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DashiUtil {

	//TODO eliminate
	public static final Charset UTF8 = Charset.forName("UTF8");

	public static <E extends Enum<?>> Map<String, E> enumToMap(Class<E> clss) {
		E[] es = clss.getEnumConstants();
		HashMap<String, E> map = new HashMap<>();
		for (E e : es) {
			map.put(e.name(), e);
		}
		return Collections.unmodifiableMap(map);
	}

	private DashiUtil() {}

}
