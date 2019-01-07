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
package com.superdashi.gosper.util;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class Scheduler {

	private final Schedule schedule;
	private final Clock clock;
	private final ZonedDateTime commencement;

	Scheduler(ZoneId zone, Clock clock, Schedule schedule) {
		this.clock = clock;
		this.schedule = schedule;
		ZonedDateTime now = ZonedDateTime.ofInstant(clock.instant(), zone);
		commencement = now.truncatedTo(schedule.unit);
	}

	public Schedule schedule() {
		return schedule;
	}

	public Clock clock() {
		return clock;
	}

	public ZonedDateTime commencement() {
		return commencement;
	}

	public Instant first() {
		return commencement.toInstant();
	}
	public Instant nextAfter(Instant instant) {
		long difference = commencement.until(instant.atZone( commencement.getZone() ), schedule.unit);
		long duration = schedule.duration;
		long delay = difference / duration * duration + duration;
		return commencement.plus(delay, schedule.unit).plus(schedule.offset).toInstant();
	}

	public Scheduling scheduling(Runnable runnable) {
		return new Scheduling(this, runnable);
	}
}
