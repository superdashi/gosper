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

import java.util.Optional;

import com.superdashi.gosper.bundle.ActivityDetails;
import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.bundle.Privileges;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.logging.Logger;

class AppInstance {

	final String instanceId;
	final Bundle bundle;
	final AppHandler handler;
	final Application application;
	// convenience fields
	final Details details;
	final Identity identity;
	final Privileges privileges;

	AppInstance(String instanceId, Bundle bundle, AppHandler handler, Application application) {
		if (instanceId == null) throw new IllegalArgumentException("null instanceId");
		if (bundle == null) throw new IllegalArgumentException("null appData");
		if (handler == null) throw new IllegalArgumentException("null handler");
		if (application == null) throw new IllegalArgumentException("null application");
		this.instanceId = instanceId;
		this.bundle = bundle;
		this.handler = handler;
		this.application = application;

		details = bundle.appData().appDetails();
		identity = details.identity();
		privileges = bundle.appData().privileges();
	}

	Optional<ActivityInstance> instantiateActivity(String actId, Logger logger) {
		Optional<ActivityDetails> optDetails = bundle.optionalActivityDetails(actId);
		if (!optDetails.isPresent()) {
			logger.info().message("no activity with id {} for instance {}").values(actId, instanceId).log();
			return Optional.empty();
		}
		Details details = optDetails.get().details;
		Activity activity;
		try {
			activity = application.createActivity(actId);
		} catch (RuntimeException e) {
			logger.error().message("error occurred instantiating activity {} for instance {}").values(actId, instanceId).stacktrace(e).log();
			return Optional.empty();
		}
		if (activity == null) {
			try {
				activity = (Activity) bundle.instantiate(details).orElse(null);
			} catch (ReflectiveOperationException e) {
				logger.error().message("failed to instantiate activity {} with type {} for application instance {}").values(actId, instanceId, details.type().identity.name).stacktrace(e).log();
			}
		}
		if (activity == null) {
			logger.warning().message("application instance {} returned no activity for id {}").values(instanceId, actId).log();
			return Optional.empty();
		}
		return Optional.of(new ActivityInstance(details.identity(), activity, handler.defaultActionHandler(activity)));
	}

}
