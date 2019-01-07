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

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;

import com.superdashi.gosper.color.Argb;

class CombinedPaint implements Paint {

	static Paint combine(Paint alpha, Paint color) {
		if (alpha.getTransparency() == Paint.OPAQUE) return color;
		return new CombinedPaint(alpha, color);
	}

	static Paint combineMask(Paint alpha, Paint color) {
		return new CombinedPaint(alpha, color);
	}

	private final Paint alpha;
	private final Paint color;

	private CombinedPaint(Paint alpha, Paint color) {
		this.alpha = alpha;
		this.color = color;
	}

	@Override
	public int getTransparency() {
		return Math.max(alpha.getTransparency(), color.getTransparency());
	}

	@Override
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
		PaintContext ac = alpha.createContext(cm, deviceBounds, userBounds, xform, hints);
		PaintContext cc = color.createContext(cm, deviceBounds, userBounds, xform, hints);
		return new PaintContext() {

			@Override
			public Raster getRaster(int x, int y, int w, int h) {
				Raster ar = ac.getRaster(x, y, w, h);
				Raster cr = cc.getRaster(x, y, w, h);

				// we assume the sizes match in the case where there's only one alpha component
				boolean mask = ac.getColorModel().getNumComponents() == 1;

				// texture paint sometimes chooses to return an oversized raster... so we have to labouriously guard against this...
				// note than only a width mismatch matters
				int aw = ar.getWidth();
				int cw = cr.getWidth();
				boolean matchedSizes = aw == w && cw == w;
				Raster mr = ImageUtil.obtainCompositeRaster(w, h);

				//note: assumption here that raster is writable - may not actually hold
				//TODO should copy into a writable raster if necessary
				DataBuffer ad = ar.getDataBuffer();
				DataBuffer cd = cr.getDataBuffer();
				DataBuffer md = mr.getDataBuffer();
				int size = w * h;

				if (matchedSizes) { // faster-case
					if (mask) {
						mergeBytes(ad, cd, md, size);
					} else {
						mergeInts(ad, cd, md, size);
					}
					return mr;
				}

				// slow case where indices are tracked independently
				int mw = mr.getWidth();
				if (mask) {
					switch (color.getTransparency()) {
					case Paint.OPAQUE:
						for (int s = 0; s < h; s++) {
							int ai = s * aw;
							int ci = s * cw;
							int mi = s * mw;
							for (int lim = w; lim > 0; lim--) {
								int a = ad.getElem(ai++);
								int c = cd.getElem(ci++);
								md.setElem(mi++, (a << 24) | (c & 0x00ffffff));
							}
						}
						break;
					case Paint.BITMASK:
						for (int s = 0; s < h; s++) {
							int ai = s * aw;
							int ci = s * cw;
							int mi = s * mw;
							for (int lim = w; lim > 0; lim--) {
								int a = ad.getElem(ai++);
								int c = cd.getElem(ci++);
								md.setElem(mi++, c < 0 ? (a  << 24) | (c & 0x00ffffff) : c & 0x00ffffff);
							}
						}
						break;
					case Paint.TRANSLUCENT:
						//TODO could provide an optimized version of this if color alpha known to be constant
						for (int s = 0; s < h; s++) {
							int ai = s * aw;
							int ci = s * cw;
							int mi = s * mw;
							for (int lim = w; lim > 0; lim--) {
								int c = cd.getElem(ci++);
								int a = ad.getElem(ai++);
								int ca = (a & 0xff) * Argb.alpha(c) / 255;
								md.setElem(mi++, Argb.transparent(c) | (ca << 24));
							}
						}
						break;
					}
				} else {
					switch (color.getTransparency()) {
					case Paint.OPAQUE:
						for (int s = 0; s < h; s++) {
							int ai = s * aw;
							int ci = s * cw;
							int mi = s * mw;
							for (int lim = w; lim > 0; lim--) {
								int a = ad.getElem(ai++);
								int c = cd.getElem(ci++);
								md.setElem(mi++, (a & 0xff000000) | (c & 0x00ffffff));
							}
						}
						break;
					case Paint.BITMASK:
						for (int s = 0; s < h; s++) {
							int ai = s * aw;
							int ci = s * cw;
							int mi = s * mw;
							for (int lim = w; lim > 0; lim--) {
								int a = ad.getElem(ai++);
								int c = cd.getElem(ci++);
								md.setElem(mi++, c < 0 ? (a & 0xff000000) | (c & 0x00ffffff) : c & 0x00ffffff);
							}
						}
						break;
					case Paint.TRANSLUCENT:
						//TODO could provide an optimized version of this if color alpha known to be constant
						for (int s = 0; s < h; s++) {
							int ai = s * aw;
							int ci = s * cw;
							int mi = s * mw;
							for (int lim = w; lim > 0; lim--) {
								int c = cd.getElem(ci++);
								int a = ad.getElem(ai++);
								int ca = Argb.alpha(a) * Argb.alpha(c) / 255;
								md.setElem(mi++, Argb.transparent(c) | (ca << 24));
							}
						}
						break;
					}
				}
				return mr;
			}

			private void mergeBytes(DataBuffer ad, DataBuffer cd, DataBuffer md, int size) {
				switch (color.getTransparency()) {
				case Paint.OPAQUE:
					for (int i = 0; i < size; i++) {
						int a = ad.getElem(i);
						int c = cd.getElem(i);
						md.setElem(i, (a << 24) | (c & 0x00ffffff));
					}
					break;
				case Paint.BITMASK:
					for (int i = 0; i < size; i++) {
						int a = ad.getElem(i);
						int c = cd.getElem(i);
						md.setElem(i, c < 0 ? (a << 24) | (c & 0x00ffffff) : c & 0x00ffffff);
					}
					break;
				case Paint.TRANSLUCENT:
					//TODO could provide an optimized version of this if color alpha known to be constant
					for (int i = 0; i < size; i++) {
						int c = cd.getElem(i);
						int a = ad.getElem(i);
						int ac = (a & 0xff) * Argb.alpha(c) / 255;
						md.setElem(i, Argb.transparent(c) | (ac << 24));
					}
					break;
				}
			}

			private void mergeInts(DataBuffer ad, DataBuffer cd, DataBuffer md, int size) {
				switch (color.getTransparency()) {
				case Paint.OPAQUE:
					for (int i = 0; i < size; i++) {
						int a = ad.getElem(i);
						int c = cd.getElem(i);
						md.setElem(i, (a & 0xff000000) | (c & 0x00ffffff));
					}
					break;
				case Paint.BITMASK:
					for (int i = 0; i < size; i++) {
						int a = ad.getElem(i);
						int c = cd.getElem(i);
						md.setElem(i, c < 0 ? (a & 0xff000000) | (c & 0x00ffffff) : c & 0x00ffffff);
					}
					break;
				case Paint.TRANSLUCENT:
					//TODO could provide an optimized version of this if color alpha known to be constant
					for (int i = 0; i < size; i++) {
						int c = cd.getElem(i);
						int a = ad.getElem(i);
						int ac = Argb.alpha(a) * Argb.alpha(c) / 255;
						md.setElem(i, Argb.transparent(c) | (ac << 24));
					}
					break;
				}
			}

			@Override
			public ColorModel getColorModel() {
				return ImageUtil.CM_INT_ARGB;
			}

			@Override
			public void dispose() {
				ac.dispose();
				cc.dispose();
			}
		};
	}

}
