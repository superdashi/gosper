package com.superdashi.gosper.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ConfigRules {

	private static final ConfigRules NONE = new ConfigRules();
	public static ConfigRules none() { return NONE; }

	private final ConfigRule[] rules;
	private final List<ConfigRule> list;

	public ConfigRules(ConfigRule... rules) {
		if (rules == null) throw new IllegalArgumentException("null rules");
		this.rules = rules.clone();
		Arrays.sort(rules, ConfigRule.byImportance());
		list = Collections.unmodifiableList(Arrays.asList(rules));
	}

	public List<ConfigRule> asList() {
		return list;
	}
}
