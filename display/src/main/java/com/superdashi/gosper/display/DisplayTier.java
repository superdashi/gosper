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
package com.superdashi.gosper.display;

import com.jogamp.common.os.Platform;
import com.superdashi.gosper.core.CoreContext;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.core.worker.WorkerControl;

public class DisplayTier {

	private final CoreContext context;
	private final WorkerControl<RenderContext> workerCtrl;
	private final DashiRendering rendering;
	private final Screen screen;

	public DisplayTier(CoreContext context, Resolution screenResolution) {
		DashiLog.info("CPU Family {0}, CPU Type: {1}, ABI Type: {2}", Platform.getCPUFamily(), Platform.getCPUType(), Platform.getABIType());
		this.context = context;
		workerCtrl = new WorkerControl<>();
		rendering = new DashiRendering(workerCtrl.getWorker(), workerCtrl.collection(), context.cache());
		screen = new Screen(context, screenResolution, rendering);
	}

	public void start() {
		workerCtrl.start();
		screen.start();
	}

	public void stop() {
		screen.stop();
		workerCtrl.stop(0L);
	}

	public DisplayConduit getConduit() {
		return rendering;
	}
}
