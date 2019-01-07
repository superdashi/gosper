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
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

public class Schedule {

	// statics

	public static Schedule at(TemporalUnit unit) {
		return at(1L, unit, Duration.ZERO);
	}

	public static Schedule at(long duration, TemporalUnit unit) {
		return at(duration, unit, Duration.ZERO);
	}

	public static Schedule at(long duration, TemporalUnit unit, TemporalAmount offset) {
		if (duration < 1L) throw new IllegalArgumentException("non-positive duration");
		if (unit == null) throw new IllegalArgumentException("null unit");
		if (offset == null) throw new IllegalArgumentException("null offset");
		return new Schedule(duration, unit, offset);
	}

	// fields

	final TemporalUnit unit; // smallest interval unit
	final long duration; // of interval in unit time
	final TemporalAmount offset;

	// constructors

	private Schedule(long duration, TemporalUnit unit, TemporalAmount offset) {
		this.unit = unit;
		this.duration = duration;
		this.offset = offset;
	}

	// accessors

	public TemporalUnit unit() {
		return unit;
	}

	public long duration() {
		return duration;
	}

	public TemporalAmount offset() {
		return offset;
	}

	// methods

	public Scheduler withDefaults() {
		return new Scheduler(ZoneId.systemDefault(), Clock.systemUTC(), this);
	}

	public Scheduler with(Clock clock) {
		if (clock == null) throw new IllegalArgumentException("null clock");
		return new Scheduler(ZoneId.systemDefault(), clock, this);
	}

	public Scheduler with(ZoneId zone, Clock clock) {
		if (zone == null) throw new IllegalArgumentException("null zone");
		if (clock == null) throw new IllegalArgumentException("null clock");
		return new Scheduler(zone, clock, this);
	}

}
