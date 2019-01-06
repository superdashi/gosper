package com.superdashi.gosper.display;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URI;

import org.junit.Test;

import com.tomgibara.intgeom.IntRect;
import com.superdashi.gosper.core.CacheControl;
import com.superdashi.gosper.core.CachePolicy;
import com.superdashi.gosper.display.DrawableImage;
import com.superdashi.gosper.display.GLBufferedImage;
import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.layout.Alignment2D;
import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Position.Fit;

public class DrawableImageTest {

	//@Test
	public void testBasic() throws Exception {
		int w = 100;
		int h = 100;
		IntRect rect = IntRect.atOrigin(w, h);
		BufferedImage img = new GLBufferedImage(w, h);
		Graphics2D g = img.createGraphics();
		Image image = new Image(new URI("http://www.superdashi.com/images/test3-1024x683.jpg"));
		DrawableImage di = DrawableImage.of(image, CacheControl.createCache(CachePolicy.TRIVIAL).getCache());
		di.drawTo(g, rect, Position.from(Fit.MATCH, Fit.COVER, Alignment2D.pair(Alignment.MID, Alignment.MID)));
		g.dispose();
		byte[] data = new byte[w * h * 4];
		img.getData().getDataElements(0, 0, w, h, data);
		// check opaque
		for (int i = 3; i < w * h * 4; i += 4) {
			assertEquals((byte) -1, data[i]);
		}
		// check not black
		for (int i = 0; i < w * h * 4; i += 4) {
			int s = Math.abs(data[i+0]) + Math.abs(data[i+1]) + Math.abs(data[i+2]);
			assertTrue(s > 0);
		}
	}
}
