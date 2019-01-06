package com.superdashi.gosper.studio;

import org.junit.Test;

import com.superdashi.gosper.studio.ColorPlane;
import com.tomgibara.intgeom.IntRect;

public class ColorPlaneTest extends RenderTest {

	@Test
	public void testPrimaryColors() {
		IntRect bounds = IntRect.bounded(10, 10, 60, 40);

		recordResult(new ColorPlane(0xffff0000).frame(bounds), "red");
		recordResult(new ColorPlane(0xff00ff00).frame(bounds), "green");
		recordResult(new ColorPlane(0xff0000ff).frame(bounds), "blue");

		recordResult(new ColorPlane(0x80ff0000).frame(bounds), "red50");
		recordResult(new ColorPlane(0x8000ff00).frame(bounds), "green50");
		recordResult(new ColorPlane(0x800000ff).frame(bounds), "blue50");
	}
}
