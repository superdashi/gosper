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

import org.junit.Test;

import com.superdashi.gosper.studio.ImageSurface;
import com.superdashi.gosper.studio.Surface;
import com.tomgibara.intgeom.IntRect;

public class SurfaceTest extends RenderTest {

	private ImageSurface createTestSurface() {
		BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 50, 25);
		g.setColor(Color.RED);
		g.fillRect(50, 0, 50, 25);
		g.setColor(Color.BLUE);
		g.fillRect(0, 25, 50, 25);
		g.setColor(Color.YELLOW);
		g.fillRect(50, 25, 50, 25);
		g.dispose();
		return ImageSurface.over(image);
	}

	@Test
	public void testWholeView() {
		ImageSurface surface = createTestSurface();
		Surface wholeView = surface.view(surface.dimensions().toRect());
		recordResult(wholeView, "wholeView");
	}

	@Test
	public void testSubCopy() {
		ImageSurface surface = createTestSurface();
		Surface subCopy = surface.view(IntRect.rectangle(25, 20, 50, 10)).mutableCopy();
		recordResult(subCopy, "subCopy");
	}

}
