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

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.Collections;

import com.jogamp.opengl.GL;
import com.superdashi.gosper.anim.AnimEffects;
import com.superdashi.gosper.anim.Animator.Terminal;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.core.Plate;
import com.superdashi.gosper.core.Layout.Place;
import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.model.Normal;
import com.superdashi.gosper.model.Vector3;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntRect;

public class PlateDisplay {

	private static final Transform t = Transform.scale(1f, -1f);

	private final Plate plate;
	private final DynamicAtlas<Place>.Updater updater;
	private final IntRect atlasRect;
	private final Rect unitRect; // the bounds of the plate as a fraction of the unit square
	private int contentIndex = -1;
	private ContentDisplay contentDisplay = null;

	// the gutter supplied here is normalized as a fraction i
	PlateDisplay(Plate plate, Rect unitRect, DynamicAtlas<Place>.Updater updater, DisplayContext context) {
		this.plate = plate;
		this.updater = updater;
		//TODO adjust this according to styling
		this.unitRect = unitRect;
		atlasRect = IntRect.atOrigin(updater.getWidth(), updater.getHeight());
		setContentIndex(0, context);
	}

	Collection<Element> getElement(ArtPlane dash, float inset) {

		Rect rect = unitRect.apply(dash.trans).apply(t);
		if (contentDisplay == null) return Collections.emptySet();
		PlateElement pe = new PlateElement(rect, 0f, DisplayUtil.testColoring, Coloring.flat(0x00ffffff), updater);
		pe.anim = new ElementAnim();
		int opt = (int) (Math.random() * 3);
		long period;
		switch (opt) {
		case 0: period = 2000L; break;
		case 1: period = 3000L; break;
		case 2: period = 5000L; break;
		default: period = 10000L; break;
		}
		//TODO eliminate
		Normal v = new Vector3((float) Math.random(), (float) Math.random(), (float) Math.random()).math().toNormal();
		pe.anim.addAnimator(AnimEffects.rotate(v.x, v.y, v.z, 0, Angles.TWO_PI).interpolate().animator(0L, period, Terminal.LOOP, Terminal.LOOP));
		return Collections.singleton(pe);
	}

	// test only
	void blankPlate(int b) {
		Graphics2D g = updater.getGraphics();
		g.setColor(Color.BLACK);
		int w = updater.getWidth();
		int h = updater.getHeight();
		g.fillRect(0, 0, w, h);
		g.setColor(Color.WHITE);
		g.fillRect(b, b, w-b*2, h-b*2);
		g.setColor(Color.BLACK);
		g.drawString("" + plate.place.index, 20, 40);
		g.setColor(Color.RED);
		int r = 30;
		g.fillOval(w/2-r, h/2-r, r*2, r*2);
	}

	public boolean requiresInfo() {
		return contentDisplay != null && contentDisplay.requiresInfo();
	}

	public void acquireInfo(Inspector inspector) {
		if (contentDisplay != null) contentDisplay.acquireInfo(inspector);
	}

	public boolean requiresRender() {
		return (contentDisplay != null && contentDisplay.requiresRender());
	}

	public void renderInitial() {
		if (contentDisplay != null) contentDisplay.renderInitial();
	}
	public void renderUpdate() {
		if (contentDisplay != null) contentDisplay.renderUpdate();
	}

	public void applyUpdate(GL gl) {
		updater.apply(gl);
	}

	private void setContentIndex(int newIndex, DisplayContext context) {
		if (contentIndex == newIndex) return;
		if (contentDisplay != null) {
			//TODO lifecycle
		}
		contentIndex = newIndex;
		if (contentIndex >= plate.content.size()) {
			contentDisplay = null;
		} else {
			contentDisplay = new ContentDisplay(plate.content.get(newIndex), updater.getGraphics(), atlasRect, context);
			//TODO lifecycle
		}
	}

}
