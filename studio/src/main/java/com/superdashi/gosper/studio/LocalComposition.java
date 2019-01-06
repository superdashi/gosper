package com.superdashi.gosper.studio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.superdashi.gosper.studio.Target.SurfaceTarget;
import com.tomgibara.intgeom.IntDimensions;

final class LocalComposition implements Composition {

	final LocalStudio studio;
	private final LocalSurfacePool pool;
	private final List<LocalPane> panes = new ArrayList<>();
	private final Set<LocalPanel> panels = new HashSet<>();
	private boolean panesDisordered = false;
	private boolean destroyed = false;

	LocalComposition(LocalStudio studio, LocalSurfacePool pool) {
		this.studio = studio;
		this.pool = pool;
	}

	@Override
	public void compositeTo(Target target) {
		if (target == null) throw new IllegalArgumentException("null target");
		// only one target type atm
		Surface surface = ((SurfaceTarget) target).surface;

//		System.out.println("STARTING COMPOSITION");
		if (panes.isEmpty()) return;
		//if (!dimensions.equals(target.dimensions())) throw new IllegalArgumentException("incompatible target");
		orderPanes();
		//TODO want proper compositing
		//TODO also want dirty tracking
		//TODO may want to render shadows
		Canvas canvas = surface.createCanvas();
		for (LocalPane pane : panes) {
			if (pane.invalid()) continue;
//			System.out.println("DRAWING " + pane.surface().dimensions() + " TO " + pane.coords + "(" + pane.hashCode() + ")");
			canvas.intOps().drawFrame(pane.surface(), pane.coords);
		}
		canvas.destroy();
	}

	@Override
	public LocalPanel createPanel(IntDimensions dimensions, boolean opaque) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		LocalPanel panel = new LocalPanel(this, dimensions, opaque);
		panel.autoValidate(pool);
		panels.add(panel);
		return panel;
	}

	@Override
	public void releaseResources() {
		for (LocalPanel panel : panels) {
			ImageSurface surface = panel.invalidate();
			if (surface != null) pool.addSurface(surface);
		}
	}

	@Override
	public void captureResources() {
		for (LocalPanel panel : panels) {
			if (panel.invalid()) {
				panel.autoValidate(pool);
			}
		}
	}

	@Override
	public boolean destroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (!destroyed) {
			destroyed = true;
			LocalPanel[] copy = (LocalPanel[]) panels.toArray(new LocalPanel[panels.size()]);
			for (LocalPanel panel : copy) {
				panel.destroy();
			}
			panesDisordered = false;
		}
	}

	void add(LocalPane pane) {
		int size = panes.size();
		pane.ordinal = size;
		panes.add(pane);
		panesDisordered = size > 0;
	}

	void remove(LocalPane pane) {
		panes.remove(pane.ordinal);
	}

	void remove(LocalPanel panel) {
		panels.remove(panel);
	}

	private void orderPanes() {
		if (panesDisordered) {
			panes.sort((a,b) -> a.elevation - b.elevation);
			panesDisordered = false;
		}
	}

}
