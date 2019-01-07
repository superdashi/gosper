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
package com.superdashi.gosper.data;

import java.util.Optional;

import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Configurable;
import com.superdashi.gosper.config.Config.PathConfig;
import com.superdashi.gosper.config.Config.ScheduleConfig;
import com.superdashi.gosper.core.Component;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.util.Schedule;

final class DataComponent implements Component, Configurable {

	private final DataRecorder recorder;
	private final Details details;
	private final DataConfig style = new DataConfig();

	DataComponent(DataRecorder recorder, Details details) {
		this.recorder = recorder;
		this.details = details;
	}

	// component methods

	public void init(DataContext context) {
		recorder.init(context);
	}

	@Override
	public Details details() {
		return details;
	}

	public void destroy() {
		recorder.destroy();
	}

	public Schedule schedule() {
		return style.schedule;
	}

	public String path() {
		return style.path;
	}

	public DataRecorder recorder() {
		return recorder;
	}

	// styleable methods

	@Override
	public Type getStyleType() {
		return Type.DATA;
	}

	@Override
	public Optional<String> getRole() {
		return details.role();
	}

	@Override
	public String getId() {
		return details.identity().name;
	}

	@Override
	public ConfigTarget openTarget() {
		return new DataConfig();
	}

	// inner classes
	private class DataConfig implements ConfigTarget {

		private Schedule schedule = null;
		private String path = null;

		@Override
		public void applyStyling(ConfigProperty property, PathConfig path) {
			switch (property.name) {
			case "request-path" : this.path = path.asString();
			}
		}

		@Override
		public void applyStyling(ConfigProperty property, ScheduleConfig style) {
			switch (property.name) {
			case "schedule" : this.schedule = style.asSchedule();
			}
		}

		@Override
		public void close() {
			style.schedule = schedule;
			style.path = path;
		}
	}
}
