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

import java.util.Collection;
import java.util.Comparator;

import com.jogamp.opengl.GL2ES2;
import com.superdashi.gosper.core.DashiLog;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public class ElementRenderer {

	private static final int DEFAULT_CAPACITY = 10;

	private static final Storage<Element> storage = StoreType.of(Element.class).storage();

	private static int zElOrder(Element a, Element b) {
		return Float.compare(a.getHandle().z, b.getHandle().z);
	}

	private static int phaseElOrder(Element a, Element b) {
		return b.getRenderPhase().ordinal() - a.getRenderPhase().ordinal();
	}

	private static int grossElOrder(Element a, Element b) {
		boolean ao = a.isOpaque();
		boolean bo = b.isOpaque();
		if (ao && bo) return phaseElOrder(b, a);
		if (ao) return -1;
		if (bo) return  1;
		int c = phaseElOrder(a, b);
		return c == 0 ? zElOrder(a, b) : c;
	}

	//TODO mode not mandatory
	private static final Comparator<Element> COMPARATOR = (a,b) -> {
		int c = grossElOrder(a, b);
		return c == 0 ? a.getRequiredState().getMode().ordinal() - b.getRequiredState().getMode().ordinal() : c;
	};

	private Store<Element> els = storage.newStore(DEFAULT_CAPACITY);
	private boolean compact = true;

	public void add(Element el) {
		if (el == null) throw new IllegalArgumentException("null el");
		if (el.index != -1) throw new IllegalArgumentException("Element already added to a renderer");

		compact();
		int index = els.count();
		el.index = index;
		if (index == els.size()) els = els.resizedCopy(index * 2);
		els.set(index, el);
	}

	//TODO could optimize
	public void addAll(Collection<Element> els) {
		for (Element el : els) {
			add(el);
		}
	}

	public void clear() {
		els.forEach(el -> el.index = -1);
		els.clear();
		compact = true;
	}

	public void remove(Element el) {
		if (el == null) throw new IllegalArgumentException("null el");
		int index = el.index;
		if (index == -1) throw new IllegalArgumentException("el not added to a renderer");
		if (index >= els.size()) throw new IllegalArgumentException("el has invalid index: " + index);
		Element e = els.get(index);
		if (e != el) throw new IllegalArgumentException("el not added to this renderer");
		els.set(index, null);
		if (index != els.count()) {
			compact = false;
		}
	}

	public void prepare(ElementData eld) {
		//TODO want a more efficient way to sort
		els.asList().subList(0, els.count()).sort(COMPARATOR);
		if (DashiLog.isTrace()) {
			DashiLog.trace("Start of render order");
			els.forEach(el -> DashiLog.trace(el.toString()));
			DashiLog.trace("End of render order");
		}
		eld.populate(els.immutableView());
	}

	public void render(GL2ES2 gl, ModeShaders mshdrs, ElementData eld) {
		eld.bind(gl);
		eld.enable(gl);

		long now = System.currentTimeMillis();
		mshdrs.prepare(now, els);
		RenderState currentState = new RenderState();
		int start = 0;
		int finish = 0;
		int count = 0;
		for (Element el : els) {
			RenderState requiredState = el.getRequiredState();
			int requiredBucket = el.animBucket;
			boolean stateOkay = currentState.satisfies(requiredState);
			boolean bucketOkay = requiredBucket == -1 || requiredBucket == mshdrs.currentBucket();
			if (!stateOkay || !bucketOkay) {
				if (finish > start) {
					DashiLog.trace("drawing {0} element vertices", finish - start);
					if (eld.draw(gl, finish - start)) count ++;
					DashiLog.trace("draw call returned");
					start = finish;
				}
				// note: important to set the state first,
				// so that the bucket is applied to the correct shader
				if (!stateOkay) {
					requiredState.applyTo(gl, mshdrs);
					requiredState.applyTo(currentState);
				}
				if (!bucketOkay) {
					mshdrs.useAnimBucket(gl, requiredBucket);
				}
			}
			finish += el.getTriangleCount();
		}

		DashiLog.trace("finishing drawing {0} element vertices", finish - start);
		if (eld.finish(gl)) count ++;
		DashiLog.trace("drew {0} elements in {1} draw calls", els.count(), count);
		eld.disable(gl);
		mshdrs.complete();
	}


	public void compact() {
		if (!compact) {
			// reassign indices if compaction moved an element
			if ( els.compact() ) els.forEach((i,el) -> el.index = i);
			compact = true;
		}
	}

}
