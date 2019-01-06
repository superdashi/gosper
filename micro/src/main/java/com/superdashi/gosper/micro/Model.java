package com.superdashi.gosper.micro;

import java.util.function.Consumer;
import java.util.function.Function;

import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.studio.Frame;

public abstract class Model {

	private static Logger loggerForContext(ActivityContext context) {
		return context == null ? null : context.logger().child("models");
	}

	private final ActivityContext context;
	private final Logger logger;

	protected Model(ActivityContext context) {
		// null currently possible if immutable (can we avoid this?)
		// if (context == null) throw new IllegalArgumentException("null context");
		this.context = context;
		logger = loggerForContext(context);
	}

	protected Model(Model contextSource) {
		if (contextSource == null) throw new IllegalArgumentException("null contextSource");
		this.context = contextSource.context;
		logger = loggerForContext(context);
	}

	protected Frame loadImage(Image res) {
		if (res == null) throw new IllegalArgumentException("null res");
		return context.loadEnvImage(res);
	}

	protected <X,Y> void backgroundLoad(X address, Function<X, Y> loader, Consumer<Y> consumer) {
		if (address == null) throw new IllegalArgumentException("null address");
		if (loader == null) throw new IllegalArgumentException("null loader");
		if (consumer == null) throw new IllegalArgumentException("null consumer");
		context.backgroundLoad(address, loader, consumer, this::recordFailure);
	}

	protected boolean isSnapshot() {
		return context == null;
	}

	protected Models models() {
		return context.models();
	}

	Frame loadImageFromResource(String res) {
		return context.loadResImage(res);
	}

	//TODO replace this with messaging via mutations
	void requestRedraw() {
		context.requestRedraw();
	}

	// private helper methods

	private void recordFailure(RuntimeException e) {
		logger.error().message("failed to load resource").stacktrace(e).log();
	}

}
