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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.studio.ImageSurface;
import com.superdashi.gosper.studio.TilingPlane;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

public class TilingTest extends RenderTest {

	@Test
	public void testChequerTiling() {
		ImageSurface tile = ImageSurface.sized(IntDimensions.square(2), true);
		tile.writePixel(0,0,0xffffffff);
		tile.writePixel(1,1,0xffffffff);
		tile.writePixel(1,0,0xff000000);
		tile.writePixel(0,1,0xff000000);
		TilingPlane tiling = new TilingPlane(tile);
		recordResult(tiling.view(IntRect.rectangle(1, 0, 20, 10)), "chequerTiling");
	}

	@Test
	public void testDummy() throws IOException {
		BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillOval(0, 0, 50, 50);
		g.dispose();
		StreamBytes bytes = Streams.bytes();
		WriteStream w = bytes.writeStream();
		w.writeChars("PROLOG");
		long start = w.position();
		OutputStream out = w.asOutputStream();
		ImageIO.write(image, "PNG", out);
		out.flush();
		long finish = w.position();
		System.out.println(start + " " + finish);
		w.writeChars("EPILOG");
		w.close();

		ReadStream r = bytes.readStream();
		Assert.assertEquals("PROLOG", r.readChars());
		long start2 = r.position();
		BufferedImage copy = ImageIO.read(r.bounded(finish-start).asInputStream());
		long finish2 = r.position();
		r.readBytes(new byte[16]);
		System.out.println(start2 + " " + finish2);
		Assert.assertEquals("EPILOG", r.readChars());
	}

}
