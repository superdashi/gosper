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
