package com.superdashi.gosper.studio;

import org.junit.Test;

import com.superdashi.gosper.studio.Mask;
import com.tomgibara.intgeom.IntDimensions;

public class MaskTest extends RenderTest {

	@Test
	public void testMaskOverBytes() {
		int w = 60;
		int h = 30;
		byte[] bytes = new byte[w*h];
		int i = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				bytes[i++] = (byte) (128 + 127 * Math.sin((x*x) / (y+30.0)));
			}
		}
		Mask mask = Mask.overByteGray(IntDimensions.of(w, h), bytes);
		recordResult(mask, "maskOverBytes");
	}
}
