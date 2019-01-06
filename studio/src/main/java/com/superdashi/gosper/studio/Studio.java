package com.superdashi.gosper.studio;

public interface Studio {

	StudioPlan plan();

	Gallery gallery();

	Composition createComposition();

	Composition createComposition(SurfacePool pool);

	SurfacePool createSurfacePool();

	boolean isClosed();

	void close();
}
