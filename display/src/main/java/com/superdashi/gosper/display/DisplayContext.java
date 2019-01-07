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

import java.awt.Font;

import com.jogamp.nativewindow.awt.DirectDataBufferInt.BufferedImageInt;
import com.jogamp.opengl.util.texture.TextureData;
import com.superdashi.gosper.color.Palette;
import com.superdashi.gosper.core.Cache;
import com.superdashi.gosper.core.Layout.Place;

public interface DisplayContext {

	Viewport getViewport();

	Font getDefaultFont();

	Palette getDefaultPalette();

	CharMap getDefaultCharMap();

	DashiCamera.Params getParams();

	ArtPlane getDashPlane();

	ArtPlane getBackPlane();

	ArtPlane getOverPlane();

	DynamicAtlas<Place> getPlaceAtlas();

	Cache getCache();

	float getTimeNow();

	long millisBetweenTimes(float s, float t);

	TextureData createTextureData(BufferedImageInt image);

	RenderPalette createPalette(Palette palette);

}
