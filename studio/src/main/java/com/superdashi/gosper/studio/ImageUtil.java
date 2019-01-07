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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import com.tomgibara.intgeom.IntDimensions;

class ImageUtil {

	private static final Point ORIGIN = new Point(0,0);
	private static final byte[] BW = {(byte)0, (byte)0xff};
	private static final int[] BANDMASKS_RGB = { 0x00ff0000, 0x0000ff00, 0x000000ff };
	private static final int[] BANDMASKS_ARGB = { 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 };
	private static final int[] BANDMASKS_565RGB = { 0xf800, 0x07E0, 0x001F };
	private static final int[] GRAY_COMPONENTS = { 8 };
	private static final int[] GRAY_BAND_OFFSETS = { 0 };

	private static final int MAX_CACHED_WIDTH = 64;
	private static final int MAX_CACHED_HEIGHT = 64;

	private static final ThreadLocal<WritableRaster> compositeRaster = new ThreadLocal<>();
	private static final BufferedImage dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private static final Graphics2D dummyGraphics = dummyImage.createGraphics();
	static {
		configureGraphics(dummyGraphics);
	}

	static final ColorModel CM_INT_RGB = new DirectColorModel(24,
			0x00ff0000,
			0x0000ff00,
			0x000000ff,
			0x00000000
			);
	static final ColorModel CM_INT_ARGB = ColorModel.getRGBdefault();
	static final ColorModel CM_565_RGB = new DirectColorModel(16,
			0xf800,
			0x07e0,
			0x001f
			);
	static final ColorModel CM_BITMAP_BW = new IndexColorModel(1, 2, BW, BW, BW);
	static final ColorModel CM_BYTE_GRAY = new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			GRAY_COMPONENTS,
			false,
			true,
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
			);

	static WritableRaster createIntRGBRaster(DataBuffer dataBuffer, int width, int height) {
		return Raster.createPackedRaster(dataBuffer, width, height, width, BANDMASKS_RGB, ORIGIN);
	}

	static WritableRaster createIntARGBRaster(DataBuffer dataBuffer, int width, int height) {
		return Raster.createPackedRaster(dataBuffer, width, height, width, BANDMASKS_ARGB, ORIGIN);
	}

	static BufferedImage imageOverIntRGB(IntDimensions dimensions, int[] data) {
		DataBufferInt dataBuffer = new DataBufferInt(data, dimensions.area());
		WritableRaster raster = createIntRGBRaster(dataBuffer, dimensions.width, dimensions.height);
		return new BufferedImage(ImageUtil.CM_INT_RGB, raster, false, null);
	}

	static BufferedImage imageOverIntARGB(IntDimensions dimensions, int[] data) {
		DataBufferInt dataBuffer = new DataBufferInt(data, dimensions.area());
		WritableRaster raster = createIntARGBRaster(dataBuffer, dimensions.width, dimensions.height);
		return new BufferedImage(ImageUtil.CM_INT_ARGB, raster, false, null);
	}

	static BufferedImage imageOverShort565RGB(IntDimensions dimensions, short[] data) {
		DataBufferUShort dataBuffer = new DataBufferUShort(data, dimensions.area());
		WritableRaster raster = Raster.createPackedRaster(dataBuffer, dimensions.width, dimensions.height, dimensions.width, BANDMASKS_565RGB, ORIGIN);
		return new BufferedImage(ImageUtil.CM_565_RGB, raster, false, null);
	}

	static BufferedImage imageOverByteBitmap(IntDimensions dimensions, byte[] data) {
		DataBufferByte dataBuffer = new DataBufferByte(data, dimensions.area());
		WritableRaster raster = Raster.createPackedRaster(dataBuffer, dimensions.width, dimensions.height, 1, ORIGIN);
		return new BufferedImage(ImageUtil.CM_BITMAP_BW, raster, false, null);
	}

	static BufferedImage imageOverByteGray(IntDimensions dimensions, byte[] data) {
		DataBufferByte dataBuffer = new DataBufferByte(data, dimensions.area());
		WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, dimensions.width, dimensions.height, dimensions.width, 1, GRAY_BAND_OFFSETS, ORIGIN);
		return new BufferedImage(ImageUtil.CM_BYTE_GRAY, raster, false, null);
	}

	static BufferedImage convertImage(BufferedImage source, int type) {
		BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), type);
		Graphics2D g = target.createGraphics();
		g.drawImage(source, null, null);
		g.dispose();
		return target;
	}

	static BufferedImage copyImage(BufferedImage source) {
		return convertImage(source, source.getType());
	}

	static void checkSurfaceData(IntDimensions dimensions, Object data, int bbp) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		if (data == null) throw new IllegalArgumentException("null data");
		int requiredBits = dimensions.area();
		int actualBits;
		if (data instanceof int[]) {
			actualBits = ((int[]  )data).length * 32;
		} else if (data instanceof short[]) {
			actualBits = ((short[])data).length * 16;
		} else if (data instanceof byte[]) {
			actualBits = ((byte[] )data).length *  8;
		} else {
			throw new IllegalArgumentException("invalid data: " + data.getClass().getName());
		}
		if (actualBits < requiredBits) throw new IllegalArgumentException("data array too short");
	}

	static WritableRaster obtainCompositeRaster(int w, int h) {
		if (w > MAX_CACHED_WIDTH || h > MAX_CACHED_HEIGHT) {
			return CM_INT_ARGB.createCompatibleWritableRaster(w, h);
		}
		WritableRaster raster = compositeRaster.get();
		if (raster != null && raster.getWidth() >= w && raster.getHeight() >= h) {
			return raster;
		}
		raster = CM_INT_ARGB.createCompatibleWritableRaster(w, h);
		compositeRaster.set(raster);
		return raster;
	}

	static void configureGraphics(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		// considered for mono displays, but problems with inconsistent ascender thickness at small point sizes
		//g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}

	static FontMetrics fontMetrics(Font font) {
		return dummyGraphics.getFontMetrics(font);
	}
}
