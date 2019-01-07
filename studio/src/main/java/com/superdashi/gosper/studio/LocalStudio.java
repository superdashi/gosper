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
