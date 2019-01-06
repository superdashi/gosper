package com.superdashi.gosper.display;

import com.jogamp.opengl.GL2ES2;
import com.superdashi.gosper.core.worker.Worker;
import com.tomgibara.fundament.Consumer;

public interface RenderContext {

	GL2ES2 getGL();

	DisplayContext getDisplay();

	Worker<RenderContext> getWorker();

	void notBefore(float displayTime, Consumer<RenderContext> renderTask);
}
