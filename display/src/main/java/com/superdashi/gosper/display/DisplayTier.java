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
