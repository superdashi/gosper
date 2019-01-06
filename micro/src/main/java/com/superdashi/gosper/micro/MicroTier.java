package com.superdashi.gosper.micro;

import com.superdashi.gosper.core.CoreTier;
import com.superdashi.gosper.data.DataTier;
import com.superdashi.gosper.display.DisplayTier;
import com.superdashi.gosper.scripting.ScriptEngine;

public final class MicroTier {

	private final CoreTier core;
	private final DataTier data;
	//TODO dependency should probably be other way?
	private final DisplayTier display;

	private final Runtime runtime;

	public MicroTier(CoreTier core, DataTier data, DisplayTier display, AppInstalls appInstalls, Visuals visuals, ScriptEngine scriptEngine) {
		this.core = core;
		this.data = data;
		this.display = display;

		runtime = new Runtime(core.context(), appInstalls, visuals, scriptEngine);
	}

	public void start() throws InterruptedException {
		//TODO runtime logic needs to change - shutdown needs to be initiated by external code
		runtime.start(core.context().shutdown());
	}

	public void stop() {
		runtime.stop();
	}

	public AppInstalls appInstalls() {
		return runtime.appInstalls;
	}

	public Interfaces interfaces() {
		return runtime.interfaces;
	}
}
