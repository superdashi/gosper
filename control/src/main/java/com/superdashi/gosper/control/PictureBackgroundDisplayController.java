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
package com.superdashi.gosper.control;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.awt.DirectDataBufferInt.BufferedImageInt;
import com.jogamp.opengl.util.texture.TextureData;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.InfoAcquirer;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.display.DisplayContext;
import com.superdashi.gosper.display.Drawable;
import com.superdashi.gosper.display.DrawableGradient;
import com.superdashi.gosper.display.DrawableGradient.Gradient;
import com.superdashi.gosper.display.DrawableImage;
import com.superdashi.gosper.display.PictureBackgroundDisplay;
import com.superdashi.gosper.display.ShaderParams.FadeParams;
import com.superdashi.gosper.display.ShaderParams.TransParams;
import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Info;
import com.superdashi.gosper.item.Item;
import com.tomgibara.storage.Store;

public class PictureBackgroundDisplayController implements PictureBackgroundDisplay.Controller {

	private final Controller controller;
	private final Inspector inspector;
	private final InfoAcquirer infoAcquirer;

	private BufferedImageInt image;

	private Image[] images;
	private int nextImage = 0;
	private Drawable current = null;
	private Drawable next = null;

	public PictureBackgroundDisplayController(Controller controller, Inspector inspector, InfoAcquirer infoAcquirer) {
		this.controller = controller;
		this.inspector = inspector;
		this.infoAcquirer = infoAcquirer;
		images = new Image[] { };
	}

	@Override
	public TextureData init() {
		DashiLog.debug("initializing background images");
		// obtain images
		Info info = infoAcquirer.acquireInfo(inspector);
		Store<Item> items = info.items;
		List<Image> list = new ArrayList<>(items.size());
		for (Item item : items) {
			item.picture().ifPresent(list::add);
		}
		int imageCount = list.size();
		if (imageCount == 0) return null;
		images = new Image[imageCount];
		list.toArray(images);
		// prepare images
		DisplayContext context = controller.context();
		// choose a resolution
		Resolution res = Drawable.optimalResolution(context.getViewport().resolution);
		// generate the texture
		image = TransParams.createImage(res);
		// wrap it as a texture data
		return context.createTextureData(image);
	}

	@Override
	public void loadFirstImage() {
		DashiLog.debug("loading first background image {0}", nextImage);
		DisplayContext context = controller.context();
		// obtain the drawable
		next = DrawableImage.of(images[nextImage], context.getCache());
		// render it to the texture
		FadeParams.renderToImage(image, next, DrawableGradient.blackToWhite(Gradient.LEFT_TO_RIGHT));
	}

	@Override
	public void loadTransition() {
		DashiLog.debug("loading first background transition {0}", nextImage);
		// update current state
		current = next;
		next = null;
		nextImage = (nextImage + 1) % images.length;
		// obtain the drawable
		next = DrawableImage.of(images[nextImage], controller.context().getCache());
		// render it to the texture
		TransParams.renderToImage(image, current, next);
	}

}
