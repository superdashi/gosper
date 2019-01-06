package com.superdashi.gosper.studio;

import org.junit.Test;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.studio.Composition;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.ImageSurface;
import com.superdashi.gosper.studio.LinearGradientPlane;
import com.superdashi.gosper.studio.Pane;
import com.superdashi.gosper.studio.Panel;
import com.superdashi.gosper.studio.Studio;
import com.superdashi.gosper.studio.StudioPlan;
import com.superdashi.gosper.studio.Surface;
import com.superdashi.gosper.studio.Target;
import com.superdashi.gosper.studio.TilingPlane;
import com.superdashi.gosper.studio.Target.SurfaceTarget;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public class StudioTest extends RenderTest {

	static ImageSurface tile = ImageSurface.sized(IntDimensions.square(2), true);
	static {
		tile.writePixel(0,0,0xffffffff);
		tile.writePixel(1,1,0xffffffff);
		tile.writePixel(1,0,0xff000000);
		tile.writePixel(0,1,0xff000000);
	}

	@Test
	public void testBasic() {

		Studio studio = new StudioPlan().createLocalStudio();
		Composition comp = studio.createComposition();
		IntDimensions fullScreen = IntDimensions.of(320, 240);
		Panel common = comp.createPanel(fullScreen, false);
		Pane bar = common.createPane(IntRect.bounded(0, 0, 320, 16), IntCoords.ORIGIN, 0);
		bar.canvas().color(0xff0000ff).fill();
		Panel grad = comp.createPanel(IntDimensions.of(100, 20), true);
		Pane gradPane = grad.createEntirePane(IntCoords.at(50, 60), 1);
		gradPane.canvas().shader(new LinearGradientPlane(IntCoords.at(0,0), Argb.GREEN, IntCoords.at(100,0), Argb.RED).asShader()).fill();
		testComposition("basic", comp, fullScreen);
		comp.destroy();
	}

	private void testComposition(String name, Composition comp, IntDimensions fullScreen) {
		int area = fullScreen.area();
		testComposition(name, comp, (SurfaceTarget) Target.toSurface(Surface.create(fullScreen, false)), "translucent");
		testComposition(name, comp, (SurfaceTarget) Target.toSurface(Surface.create(fullScreen, true)), "opaque");
		testComposition(name, comp, (SurfaceTarget) Target.toByteBitmap(fullScreen, new byte[(area+7)/8]), "bitmap");
		testComposition(name, comp, (SurfaceTarget) Target.toIntRGB(fullScreen, new int[area]), "intRGB");
		testComposition(name, comp, (SurfaceTarget) Target.toIntARGB(fullScreen, new int[area]), "intARGB");
		testComposition(name, comp, (SurfaceTarget) Target.toShort565ARGB(fullScreen, new short[area]), "shortRGB");
	}

	private void testComposition(String name, Composition comp, SurfaceTarget target, String type) {
		Surface surface = target.surface;
		IntDimensions dimensions = surface.dimensions();
		Frame background = new TilingPlane(tile).frame(IntRect.atOrigin(dimensions.width / 2, dimensions.height));
		surface.createCanvas().drawFrame(background).destroy();
		comp.compositeTo(target);
		recordResult(surface, name + "_" + type);
	}
}
