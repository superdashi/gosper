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
package com.superdashi.gosper.app;

import java.net.URI;
import java.nio.file.Path;

import com.superdashi.gosper.control.ControlTier;
import com.superdashi.gosper.core.CoreContext;
import com.superdashi.gosper.core.CoreTier;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.data.DataTier;
import com.superdashi.gosper.display.DisplayTier;
import com.superdashi.gosper.micro.AppInstalls;
import com.superdashi.gosper.micro.MicroTier;
import com.superdashi.gosper.micro.Visuals;
import com.superdashi.gosper.scripting.ScriptEngine;

//TODO make data tier optional too
class Tiers {

	static class Builder {

		private final CoreContext context;
		private Resolution screenResolution = null;
		private boolean presentationEnabled = false;
		private Visuals visuals = null;
		private ScriptEngine scriptEngine = null;
		private URI dbUri = null;
		private Path appDataPath = null;

		private Builder(CoreContext context) {
			this.context = context;
		}

		Builder presentationEnabled(boolean presentationEnabled) {
			this.presentationEnabled = presentationEnabled;
			return this;
		}

		Builder screenResolution(Resolution screenResolution) {
			this.screenResolution = screenResolution;
			return this;
		}

		Builder visuals(Visuals visuals) {
			this.visuals = visuals;
			return this;
		}

		Builder scriptEngine(ScriptEngine scriptEngine) {
			this.scriptEngine = scriptEngine;
			return this;
		}

		Builder dbUrl(URI dbUri) {
			this.dbUri = dbUri;
			return this;
		}

		Builder appDataPath(Path appDataPath) {
			this.appDataPath = appDataPath;
			return this;
		}

		Tiers build() {
			CoreTier coreTier = new CoreTier(context);
			DataTier dataTier = null;
			if (dbUri != null) try {
				dataTier = new DataTier(coreTier, dbUri);
			}  catch (IllegalArgumentException e) { // thrown if uri is bad
				context.loggers().loggerFor("data").error().message("invalid database URI; {}").values(dbUri).stacktrace(e).log();
			}
			DisplayTier displayTier = null;
			ControlTier controlTier = null;
			if (presentationEnabled && screenResolution != null) {
				displayTier = new DisplayTier(context, screenResolution);
				controlTier = new ControlTier(dataTier, displayTier);
			}
			MicroTier microTier = null;
			if (visuals != null && appDataPath != null) {
				AppInstalls appInstalls = new AppInstalls(appDataPath, context.backgroundExecutor(), context.loggers().loggerFor("app-inst"));
				microTier = new MicroTier(coreTier, dataTier, displayTier, appInstalls, visuals, scriptEngine);
			}
			return new Tiers(coreTier, dataTier, displayTier, controlTier, microTier);
		}
	}

	static Builder builder(CoreContext context) {
		if (context == null) throw new IllegalArgumentException("null context");
		return new Builder(context);
	}

	//TODO hide behind accessors?
	final CoreTier coreTier;
	final DataTier dataTier;
	final DisplayTier displayTier;
	final ControlTier controlTier;
	final MicroTier microTier;

	private Tiers(
			CoreTier coreTier,
			DataTier dataTier,
			DisplayTier displayTier,
			ControlTier controlTier,
			MicroTier microTier
			) {
		this.coreTier = coreTier;
		this.dataTier = dataTier;
		this.displayTier = displayTier;
		this.controlTier = controlTier;
		this.microTier = microTier;
	}

	void start() throws InterruptedException {
		if (dataTier != null) dataTier.start(); //TODO hacky - data tier needs to register handler before coreTier starts
		coreTier.start();
		if (displayTier != null) displayTier.start();
		if (microTier != null) microTier.start();
	}

	void stop(long timeout) throws InterruptedException {
		if (microTier != null) microTier.stop();
		if (displayTier != null) displayTier.stop();
		coreTier.stop(timeout);
		if (dataTier != null) dataTier.stop();
	}

}
