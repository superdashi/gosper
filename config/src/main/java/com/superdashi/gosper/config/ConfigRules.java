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
