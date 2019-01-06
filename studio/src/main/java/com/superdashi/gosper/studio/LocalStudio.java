package com.superdashi.gosper.studio;

final class LocalStudio implements Studio {

	final StudioPlan plan;
	final LocalGallery gallery = new LocalGallery(this);
	private boolean closed = false;

	LocalStudio(StudioPlan plan) {
		this.plan = plan;
	}

	@Override
	public StudioPlan plan() {
		return plan;
	}

	@Override
	public LocalGallery gallery() {
		return gallery;
	}

	@Override
	public LocalComposition createComposition() {
		return new LocalComposition(this, LocalSurfacePool.empty);
	}

	@Override
	public Composition createComposition(SurfacePool pool) {
		if (pool == null) throw new IllegalArgumentException("null pool");
		if (!(pool instanceof LocalSurfacePool)) throw new IllegalArgumentException("unsupported pool");
		return new LocalComposition(this, (LocalSurfacePool) pool);
	}

	@Override
	public SurfacePool createSurfacePool() {
		return new LocalSurfacePool();
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		if (!closed) {
			gallery.close();
			closed = true;
		}
	}

}
