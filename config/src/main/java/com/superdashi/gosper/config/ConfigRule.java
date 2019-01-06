package com.superdashi.gosper.config;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public final class ConfigRule {

	public static Comparator<ConfigRule> byImportance() {
		return (s,t) -> Float.compare(t.importance, s.importance);
	}

	private final ConfigSelector selector;
	private final float importance;
	private final Configuration[] stylings;

	public ConfigRule(ConfigSelector selector, float importance, Configuration... stylings) {
		//TODO null check
		this(selector, importance, Arrays.asList(stylings));
	}

	//TODO should take a list of stylings and remove dupes
	public ConfigRule(ConfigSelector selector, float importance, List<Configuration> stylings) {
		if (selector == null) throw new IllegalArgumentException("null selector");
		if (importance < 0f) throw new IllegalArgumentException("negative importance");
		if (Float.isNaN(importance)) throw new IllegalArgumentException("NaN importance");
		if (stylings == null) throw new IllegalArgumentException("null stylings");

		TreeMap<ConfigProperty, Configuration> map = new TreeMap<>(ConfigProperty.byName());
		for (Configuration styling : stylings) {
			if (styling == null) throw new IllegalArgumentException("null styling");
			map.put(styling.getProperty(), styling);
		}

		this.selector = selector;
		this.importance = importance;
		this.stylings = (Configuration[]) map.values().toArray(new Configuration[map.size()]);
	}

	public ConfigSelector selector() {
		return selector;
	}

	public float importance() {
		return importance;
	}

	public void applyTo(ConfigTarget target) {
		if (target == null) throw new IllegalArgumentException("null target");
		//TODO use property's target type to apply relevant subset
		for (Configuration styling : stylings) {
			styling.applyTo(target);
		}
	}
}
