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

import com.superdashi.gosper.framework.Identity;

// combines an activity with other supporting objects that may be created when the activity is instantiated
final class ActivityInstance {

	final Identity identity; // may be null for built-in activities
	final Activity activity;
	final ActionHandler defaultActionHandler;

	ActivityInstance(Activity activity) {
		this.identity = null;
		this.activity = activity;
		this.defaultActionHandler = null;
	}

	ActivityInstance(Identity identity, Activity activity, ActionHandler defaultActionHandler) {
		this.identity = identity;
		this.activity = activity;
		this.defaultActionHandler = defaultActionHandler;
	}

}
