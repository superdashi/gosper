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
package com.superdashi.gosper.core;

import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Config.DurationConfig;

public class BackgroundConfig implements ConfigTarget {

	public static final long TRANS_DURATION_DEFAULT = 250L;
	public static final long SHOW_DURATION_DEFAULT = 5000L;

	public long transDuration = TRANS_DURATION_DEFAULT;
	public long showDuration = SHOW_DURATION_DEFAULT;

	public void adopt(BackgroundConfig that) {
		this.transDuration = that.transDuration;
		this.showDuration = that.showDuration;
	}

	// style target

	@Override
	public void applyStyling(ConfigProperty property, DurationConfig duration) {
		switch (property.name) {
		case "transition-duration" :
			transDuration = duration.asMillis();
			break;
		case "display-duration" :
			showDuration = duration.asMillis();
			break;
		}
	}
}
