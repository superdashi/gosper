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
package com.superdashi.gosper.micro;

class BadApplication implements Application {

	private final String instanceId;
	private final String reason;
	private Environment env;

	BadApplication(String instanceId, String reason) {
		this.instanceId = instanceId;
		this.reason = reason;
	}

	@Override
	public void init(Environment environment) {
		this.env = environment;
	}

	@Override
	public Activity createActivity(String activityId) {
		switch (activityId) {
		case "activity_launch" : return new BadAppActivity(instanceId, reason);
		case "activity_details" : return new BadAppDetailsActivity(instanceId, reason);
		default: return null;
		}
	}

	@Override
	public void destroy() {
		env = null;
	}

}
