package com.superdashi.gosper.studio;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import com.tomgibara.intgeom.IntRect;

public interface Plane extends Planar {

	default Frame frame(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		return new PlaneFrame<>(Plane.this, bounds);
	}

	default Shader asShader() {
		return new Shader() {
			@Override
			Paint createPaint() {
				return new Paint() {
					@Override public int getTransparency() { return Plane.this.opaque() ? Paint.OPAQUE : Paint.TRANSLUCENT; }

					@Override
					public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
						//TODO need to handle xform
						return new PaintContext() {
							@Override
							public Raster getRaster(int x, int y, int w, int h) {
								WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
								int[] scanline = new int[w];
								for (int dy = 0; dy < h; dy++) {
									for (int dx = 0; dx < w; dx++) {
										scanline[dx] = Plane.this.readPixel(x + dx, y + dy);
									}
									raster.setDataElements(0, dy, w, 1, scanline);
								}
								return raster;
							}
							@Override public ColorModel getColorModel() { return cm; }
							@Override public void dispose() { }
						};
					}

				};
			}
		};
	}
}
