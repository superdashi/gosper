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
