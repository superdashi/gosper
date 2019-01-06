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
