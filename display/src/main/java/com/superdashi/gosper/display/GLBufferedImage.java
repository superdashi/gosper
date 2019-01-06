package com.superdashi.gosper.display;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;

public class GLBufferedImage extends BufferedImage {

	// copied from AWTTextureData to make a compatible model thus avoiding image conversion
	private static final java.awt.image.ColorModel rgbaColorModel =
			new ComponentColorModel(java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_sRGB),
									new int[] {8, 8, 8, 8}, true, true,
									Transparency.TRANSLUCENT,
									DataBuffer.TYPE_BYTE);

	public GLBufferedImage(int width, int height) {
		super(rgbaColorModel, rgbaColorModel.createCompatibleWritableRaster(width, height), true, null);
	}

}
