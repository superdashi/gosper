package com.superdashi.gosper.studio;

import java.awt.Font;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.ColorPlane;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.ImageSurface;
import com.superdashi.gosper.studio.IntFontMetrics;
import com.superdashi.gosper.studio.LinearGradientPlane;
import com.superdashi.gosper.studio.LocalCanvas;
import com.superdashi.gosper.studio.Mask;
import com.superdashi.gosper.studio.Plane;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.Surface;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.TilingPlane;
import com.superdashi.gosper.studio.Typeface;
import com.superdashi.gosper.studio.TypefaceMetrics;
import com.superdashi.gosper.studio.Canvas.IntTextOps;
import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.curve.Spiral;
import com.tomgibara.geom.shape.Shape;
import com.tomgibara.geom.shape.WindingRule;
import com.tomgibara.geom.stroke.Cap;
import com.tomgibara.geom.stroke.Join;
import com.tomgibara.geom.stroke.Outline;
import com.tomgibara.geom.stroke.Stroke;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.streams.Streams;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalCanvasTest extends RenderTest {

	private static Font font(String name) throws Exception {
		return Font.createFont(Font.TRUETYPE_FONT, LocalCanvasTest.class.getResourceAsStream("/" + name));
	}

	private static Typeface testTypeface(float size) throws Exception {
		return Typeface.fromFonts(
				font("SourceSansPro-Regular.ttf").deriveFont(size),
				font("SourceSansPro-Italic.ttf").deriveFont(size),
				font("SourceSansPro-Bold.ttf").deriveFont(size),
				font("SourceSansPro-BoldItalic.ttf").deriveFont(size)
				);
	}

	private static StyledText sampleStyledText() {
		Style italic = new Style().textItalic(1).immutable();
		Style bold = new Style().textWeight(1).immutable();
		Style under = new Style().textUnderline(1).immutable();
		Style orange = new Style().colorFg(0xffff8000).immutable();

		StyledText st = new StyledText("Styling some text is lots of effort.");
		st.root().applyStyle(italic, "i", 8, 28);
		st.root().applyStyle(orange, "c", 8, 17);
		st.root().applyStyle(bold, "b", 13, 17);
		st.root().applyStyle(under, "u", 26, 28);

		return st;
	}

	@Test
	public void testIntLine() {
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(60, 40), true);
		Canvas canvas = surface.createCanvas();
		canvas
			.color(0xff0000ff)
			.fill()
			.color(0xff0080ff)
			.intOps()
			.strokeLine(5, 5, 55, 35)
			.canvas()
			.destroy();
		recordResult(surface, "intLine");
	}

	@Test
	public void testIntRect() {
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 40), true);
		Canvas canvas = surface.createCanvas();
		canvas
			.color(0xffff8000)
			.fill()
			.color(0xffff0000)
			.intOps()
			.fillRect(IntRect.rectangle(10, 10, 30, 20))
			.canvas()
			.destroy();
		recordResult(surface, "intRect");
	}

	@Test
	public void testIntEllipse() {
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 40), true);
		Canvas canvas = surface.createCanvas();
		canvas
			.color(0xffff8000)
			.fill()
			.color(0xffff0000)
			.intOps()
			.fillEllipse(IntRect.rectangle(10, 10, 30, 20))
			.canvas()
			.destroy();
		recordResult(surface, "intEllipse");
	}

	@Test
	public void testTileShader() {
		ImageSurface tile = simpleTile();
		TilingPlane tiling = new TilingPlane(tile);

		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 40), true);
		Canvas canvas = surface.createCanvas();
		canvas
			.color(0xffff0000)
			.fill()
			.shader(tiling.asShader())
			.intOps()
			.fillEllipse(IntRect.rectangle(5, 5, 70, 30))
			.canvas()
			.destroy();
		recordResult(surface, "tile");
	}

	@Test
	public void testTransform() {
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 40), true);
		surface
			.createCanvas()
			.color(0xffffff00)
			.fill()
			.floatOps()
			.transform(Transform.rotationAbout(new Point(60f, 20f), Angles.PI_BY_FOUR))
			.transform(Transform.scale(1f, 0.5f))
			.transform(Transform.translation(40f, 0f))
			.transform(Transform.translation(20f, 20f))
			.canvas()
			.color(0xff0000ff)
			.intOps()
			.fillEllipse(IntRect.centeredAtOrigin(20, 20))
			.canvas()
			.destroy();
		recordResult(surface, "transform");
	}

	@Test
	public void testFloatRect() {
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 40), true);
		surface
			.createCanvas()
			.color(0xffff0000)
			.fill()
			.color(0x8000ff00)
			.floatOps()
			.fillRect(Rect.atPoints(20, 10, 60, 40))
			.canvas()
			.destroy();
		recordResult(surface, "floatRect");
	}

	@Test
	public void testFloatEllipse() {
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 40), true);
		surface
			.createCanvas()
			.color(0xffff0000)
			.fill()
			.color(0x8000ff00)
			.floatOps()
			.fillEllipse(Ellipse.fromRect(Rect.atPoints(20, 10, 60, 40)))
			.canvas()
			.destroy();
		recordResult(surface, "floatEllipse");
	}

	@Test
	public void testFloatShape() {
		recordResult(createShapeSurface(true), "floatShape");
	}

	private Surface createShapeSurface(boolean opaque) {
		Spiral spiral = Spiral.from(new Point(40, 20), Angles.PI_BY_SIX, 2 * Angles.TWO_PI + Angles.PI_BY_FOUR, 10f, 20f);
		Stroke stroke = new Stroke(new Outline(Join.ROUND_JOIN, 3f), Cap.ROUND_CAP/*, PatternDash.single(10, 10)*/);
		List<Contour> contours = stroke.stroke(spiral.getPath());
		Shape shape = new Shape(WindingRule.EVEN_ODD, contours);

		Surface surface = ImageSurface.sized(IntDimensions.of(80, 40), opaque);
		surface
			.createCanvas()
			.color(0xffffffff)
			.floatOps()
			.fillShape(shape)
			.canvas()
			.destroy();
		return surface;
	}

	@Test
	public void testDrawFrame() {
		IntRect square = IntRect.atOrigin(40, 40);

		Surface image = ImageSurface.sized(square.dimensions(), false);
		image
			.createCanvas()
			.color(0xff0000ff).intOps().fillRect(IntRect.atOrigin(20, 40)).canvas()
			.color(0xffffff00).intOps().fillEllipse(IntRect.centeredAtOrigin(25, 25).translatedBy(20, 20))
			.canvas()
			.destroy();

		Frame color = new ColorPlane(0xff00c000).frame(square);

		Frame tiling = new TilingPlane(simpleTile()).frame(square.translatedBy(3, 3));

		Frame random = new RandomFrame(square.dimensions());

		Mask mask = image.toMask();

		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 120), true);
		surface
			.createCanvas()
			.drawFrame(image)
			.intOps().translate( 40,  0).canvas()
			.drawFrame(color)
			.intOps().translate(  0, 40).canvas()
			.drawFrame(tiling)
			.intOps().translate(-40,  0).canvas()
			.drawFrame(random)
			.intOps().translate(  0, 40).canvas()
			.drawFrame(mask)
			.destroy();
		recordResult(surface, "drawFrame");
	}

	@Test
	public void testPlotPixel() {
		IntDimensions dimensions = IntDimensions.of(51, 51);
		IntRect bounds = dimensions.toRect();
		ImageSurface surface = ImageSurface.sized(dimensions, true);
		LinearGradientPlane plane = new LinearGradientPlane(bounds.minimumCoords(), Argb.WHITE, bounds.maximumCoords(), Argb.BLACK);
		LocalCanvas canvas = surface.createCanvas();
		canvas.color(0xffff9090).fill().shader(plane.asShader());
		Consumer<IntCoords> plotter = canvas.intOps()::plotPixel;
		for (int y = 1; y < 51; y += 2) {
			for (int x = 1; x < 51; x += 2) {
				plotter.accept(IntCoords.at(x, y));
			}
		}
		canvas.destroy();
		recordResult(surface, "plotPixel");
	}

	@Test
	public void testSinePlane() {
		IntDimensions dimensions = IntDimensions.of(60, 60);
		ImageSurface surface = ImageSurface.sized(dimensions, true);
		surface.createCanvas().drawFrame(new SinePlane().frame(dimensions.toRect())).destroy();
		recordResult(surface, "sinePlane");
	}

	@Test
	public void testPorterDuff() {
		Arrays.stream(PorterDuff.Rule.values()).forEach(this::testPorterDuff);
	}

	private void testPorterDuff(PorterDuff.Rule rule) {
		IntDimensions dimensions = IntDimensions.of(80, 80);

		ImageSurface src = ImageSurface.sized(dimensions, false);
		src.createCanvas().color(0xff0040ff).floatOps().fillRect(Rect.atPoints(5, 20, 60, 75)).canvas().destroy();;

		ImageSurface dst = ImageSurface.sized(dimensions, false);
		LocalCanvas canvas = dst.createCanvas();
		canvas.color(0xffff4000).floatOps().fillEllipse(Ellipse.fromRadius(new Point(47.5f, 32.5f), 27.5f));

		canvas.composer(new PorterDuff(rule).asComposer()).drawFrame(src).destroy();
		recordResult(dst, "porterDuff_" + rule.name().toLowerCase());
	}

	@Test
	public void testErase() {
		ImageSurface surface = ImageSurface.sized(IntDimensions.of(80, 40), false);
		surface
			.createCanvas().color(Argb.BLUE).intOps().fillEllipse(IntRect.bounded(0, 0, 40, 40))
			.canvas().color(Argb.RED).intOps().fillRect(IntRect.bounded(40, 0, 80, 40))
			.canvas().erase().destroy();
		recordResult(surface, "erase");
	}

	@Test
	public void testFillFrameSurface() {
		ImageSurface src = ImageSurface.sized(IntDimensions.of(90, 90), false);
		IntRect sqr = IntRect.atOrigin(30, 30);
		LocalCanvas canvas = src.createCanvas();
		canvas.color(Argb.setAlpha(Argb.BLACK  , 0xff)).intOps().fillEllipse(sqr.translatedBy(0, 0));
		canvas.color(Argb.setAlpha(Argb.RED    , 0xee)).intOps().fillEllipse(sqr.translatedBy(30, 0));
		canvas.color(Argb.setAlpha(Argb.GREEN  , 0xdd)).intOps().fillEllipse(sqr.translatedBy(60, 0));
		canvas.color(Argb.setAlpha(Argb.BLUE   , 0xcc)).intOps().fillEllipse(sqr.translatedBy(0, 30));
		canvas.color(Argb.setAlpha(Argb.YELLOW , 0xbb)).intOps().fillEllipse(sqr.translatedBy(60, 30));
		canvas.color(Argb.setAlpha(Argb.CYAN   , 0xaa)).intOps().fillEllipse(sqr.translatedBy(0, 60));
		canvas.color(Argb.setAlpha(Argb.MAGENTA, 0x99)).intOps().fillEllipse(sqr.translatedBy(30, 60));
		canvas.color(Argb.setAlpha(Argb.WHITE  , 0x88)).intOps().fillEllipse(sqr.translatedBy(60, 60));
		canvas.destroy();
		ImageSurface dst = ImageSurface.sized(IntDimensions.of(200, 200), false);
		dst.createCanvas().intOps().drawFrame(src, IntCoords.at(5, 5));
		dst.createCanvas().shader(new ColorPlane(0xffff8000).asShader()).intOps().fillFrame(src, IntCoords.at(105, 5));
		dst.createCanvas().shader(new ColorPlane(0x80ff8000).asShader()).intOps().fillFrame(src, IntCoords.at(5, 105));
		LinearGradientPlane plane = new LinearGradientPlane(IntCoords.at(0, 0), 0x00000000, IntCoords.at(90, 90), 0x8000ff00);
		dst.createCanvas().shader(plane.asShader()).intOps().fillFrame(src, IntCoords.at(105, 105));
		recordResult(dst, "fillFrame_surface");
	}

	@Test
	public void testFillFrameMask() {
		Mask mask = createShapeSurface(false).toMask();
		IntRect bounds = mask.dimensions().toRect();
		LinearGradientPlane gradient = new LinearGradientPlane(bounds.minimumCoords(), 0x00000000, bounds.maximumCoords(), 0x8000ff00);
		TilingPlane tiling = new TilingPlane(simpleTile());
		Surface surface = Surface.create(mask.dimensions().scale(1, 3), true);
		surface.createCanvas()
			.color(0xff000000).fill()
			.color(0xffff0000).fillFrame(mask)
			.intOps().translate(0, bounds.maxY).canvas()
			.shader(gradient.asShader()).fillFrame(mask)
			.intOps().translate(0, bounds.maxY).canvas()
			.shader(tiling.asShader()).fillFrame(mask)
			.destroy();
		recordResult(surface, "fillFrame_mask");
	}

	@Test
	public void testDecodeSurface() throws IOException {
		Surface trans = Surface.decode(Streams.streamInput(LocalCanvasTest.class.getResourceAsStream("/test-trans.png")));
		Surface opaque = Surface.decode(Streams.streamInput(LocalCanvasTest.class.getResourceAsStream("/test-opaque.png")));
		Surface target = Surface.create(IntDimensions.of(160, 80), true);
		target.createCanvas().color(0xffffffff).fill().drawFrame(trans).intOps().translate(80, 0).canvas().drawFrame(opaque).destroy();
		recordResult(target, "decodeSurface");
	}

	@Test
	public void testRotate() throws IOException {
		Surface image = Surface.decode(Streams.streamInput(LocalCanvasTest.class.getResourceAsStream("/test-trans.png")));
		Surface target = Surface.create(IntDimensions.of(120, 120), true);
		target.createCanvas().floatOps().transform(Transform.rotationAbout(new Point(60, 60),  Angles.PI_BY_FOUR)).translate(20, 20).canvas().drawFrame(image).destroy();
		recordResult(target, "rotateFrame");
	}

	@Test
	public void testRotateAndFill() throws IOException {
		LinearGradientPlane plane = new LinearGradientPlane(IntCoords.at(0, 0), 0xffff8000, IntCoords.at(80, 80), 0xff0080ff);
		Surface image = Surface.decode(Streams.streamInput(LocalCanvasTest.class.getResourceAsStream("/test-trans.png")));
		Surface target = Surface.create(IntDimensions.of(120, 120), true);
		target.createCanvas().shader(plane.asShader()).floatOps().transform(Transform.rotationAbout(new Point(60, 60),  Angles.PI_BY_FOUR)).translate(20, 20).canvas().fillFrame(image).destroy();
		recordResult(target, "rotateAndFillFrame");
	}

	@Test
	public void testFontBasic() throws Exception {
		Surface surface = Surface.create(IntDimensions.of(200, 20), true);
		Typeface typeface = testTypeface(20f);
		surface.createCanvas().color(0xffff0080).fill().color(0xffffffff).intOps().translate(5, 15).canvas().intOps().newText(typeface).renderString("This is a test message.").canvas().destroy();
		recordResult(surface, "font_basic");
	}

	@Test
	public void testFontGradient() throws Exception {
		Surface surface = Surface.create(IntDimensions.of(200, 20), true);
		Typeface typeface = testTypeface(20f);
		surface.createCanvas().color(0xffff0080).fill().shader(new LinearGradientPlane(IntCoords.ORIGIN, 0xff000000, IntCoords.at(200, 0), 0xffffffff).asShader()).intOps().newText(typeface).moveTo(5, 15).renderString("This is a test message.").canvas().destroy();
		recordResult(surface, "font_gradient");
	}

	@Test
	public void testFontUnderline() throws Exception {
		Surface surface = Surface.create(IntDimensions.of(200, 20), true);
		Typeface typeface = testTypeface(20f);
		surface.createCanvas().color(0xffff0080).fill().color(0xffffffff).intOps().newText(typeface).moveTo(5, 15).renderString(TextStyle.regular().withUnderlined(true), "This is a test message.").canvas().destroy();
		recordResult(surface, "font_underline");
	}

	@Test
	public void testFontStyles() throws Exception {
		Surface surface = Surface.create(IntDimensions.of(200, 100), true);
		Canvas canvas = surface.createCanvas()
			.color(0xffff0080).fill()
			.color(0xffffffff);
		writeFont(canvas);
		canvas.destroy();
		recordResult(surface, "font_styles");
	}

	private void writeFont(Canvas canvas) throws Exception {
		Typeface typeface = testTypeface(20f);
		TypefaceMetrics metrics = typeface.metrics();
		IntTextOps ops = canvas.intOps().newText(typeface).moveTo(IntCoords.at(5, -5));
		BiConsumer<TextStyle, String> write = (t, s) -> {
			IntFontMetrics m = metrics.fontMetrics(t);
			ops.moveBy(0, m.baseline).renderString(t, s).moveBy(0, m.lineHeight - m.baseline);
		};
		write.accept(TextStyle.regular()                                , "This is in regular."   );
		write.accept(TextStyle.regular().withBold(true)                 , "I hope this is bold."  );
		write.accept(TextStyle.regular().withItalic(true)               , "This should be italic.");
		write.accept(TextStyle.regular().withBold(true).withItalic(true), "Bold and italic, yes?" );
	}

	@Test
	public void testEzoBasic() {
		Surface surface = Surface.create(IntDimensions.of(160, 40), true);
		Canvas canvas = surface.createCanvas().color(Argb.WHITE).fill().color(Argb.BLACK);
		writeEzo(canvas);
		canvas.destroy();
		recordResult(surface, "ezo_basic");
	}

	@Test
	public void testEzoScaled() {
		Surface surface = Surface.create(IntDimensions.of(640, 160), true);
		Canvas canvas = surface.createCanvas().color(Argb.WHITE).fill().color(Argb.BLACK);
		canvas.floatOps().transform(Transform.scale(4f));
		writeEzo(canvas);
		canvas.destroy();
		recordResult(surface, "ezo_scaled");
	}

	@Test
	public void testEzoShaded() {
		Surface surface = Surface.create(IntDimensions.of(160, 40), true);
		Canvas canvas = surface.createCanvas().color(Argb.RED).fill().shader(new SinePlane().asShader());
		writeEzo(canvas);
		canvas.destroy();
		recordResult(surface, "ezo_shaded");
	}

	private void writeEzo(Canvas canvas) {
		Typeface typeface = Typeface.ezo();
		IntTextOps ops = canvas.intOps().newText(typeface);
		int height = typeface.metrics().fontMetrics(TextStyle.regular()).lineHeight;
		ops.moveTo(2, 0)
				.moveBy(0, height).renderString("This is a single line of text in Ezo.")
				.moveBy(0, height).renderString(TextStyle.regular().withBold(true), "This is a second line of bold.")
				.moveBy(0, height).renderString(TextStyle.regular().withItalic(true), "All of this text should be slanted.")
				.moveBy(0, height).renderString(TextStyle.regular().withUnderlined(true), "The entire last line should be underlined.");
	}

	@Test
	public void testAccommodatedWidth() throws Exception {
		recordResult(testAccommodatedWidth(0), "accommodatedWidth");
	}

	@Test
	public void testAccommodatedWidthEllipsis() throws Exception {
		recordResult(testAccommodatedWidth(10), "accommodatedWidth_ellipsis");
	}

	private Surface testAccommodatedWidth(int ellipsisWidth) throws Exception {
		Surface surface = Surface.create(IntDimensions.of(160, 80), true);
		Canvas canvas = surface.createCanvas().color(Argb.BLUE).fill();
		Typeface typeface = testTypeface(10f);
		TextStyle textStyle = TextStyle.regular();
		TypefaceMetrics typeMetrics = typeface.metrics();
		IntFontMetrics fontMetrics = typeMetrics.fontMetrics(textStyle);
		IntTextOps text = canvas.intOps().newText(typeface);
		text.moveTo(0, fontMetrics.baseline);
		String str = "This is the test string we are using!";
		for (int w = 0; w < 5; w++) {
			int y = w * fontMetrics.lineHeight;
			int width = w * 40;
			canvas
				.color(Argb.BLACK).intOps().fillRect(IntRect.rectangle(0, y, Math.max(width - ellipsisWidth, 0), fontMetrics.lineHeight)).canvas()
				.color(Argb.CYAN).intOps().fillRect(IntRect.rectangle(width - ellipsisWidth, y, ellipsisWidth, fontMetrics.lineHeight)).canvas()
				.color(Argb.WHITE);
			int count = typeMetrics.accommodatedCharCount(textStyle, str, width, ellipsisWidth);
			text.renderString(str.substring(0, count)).moveBy(0, fontMetrics.lineHeight);
		}
		return surface;
	}

	@Test
	public void testAccommodatedWidthStyled() throws Exception {
		recordResult(testAccommodatedWidthStyled(IntDimensions.of(160, 80), testTypeface(10f), 40, 0), "accommodatedWidthStyled");
	}

	@Test
	public void testAccommodatedWidthStyledEllipsis() throws Exception {
		recordResult(testAccommodatedWidthStyled(IntDimensions.of(160, 80), testTypeface(10f), 40, 10), "accommodatedWidthStyled_ellipsis");
	}

	@Test
	public void testAccommodatedWidthEzoStyled() throws Exception {
		recordResult(testAccommodatedWidthStyled(IntDimensions.of(160, 50), Typeface.ezo(), 40, 0), "accommodatedEzoWidthStyled");
	}

	@Test
	public void testAccommodatedWidthEzoStyledEllipsis() throws Exception {
		recordResult(testAccommodatedWidthStyled(IntDimensions.of(160, 60), Typeface.ezo(), 35, 10), "accommodatedEzoWidthStyled_ellipsis");
	}

	private Surface testAccommodatedWidthStyled(IntDimensions dimensions, Typeface typeface, int step, int ellipsisWidth) throws Exception {
		Surface surface = Surface.create(dimensions, true);
		Canvas canvas = surface.createCanvas().color(Argb.BLUE).fill();
		IntTextOps text = canvas.intOps().newText(typeface);
		TextStyle textStyle = TextStyle.regular();
		TypefaceMetrics typeMetrics = typeface.metrics();
		IntFontMetrics fontMetrics = typeMetrics.fontMetrics(textStyle);
		text.moveTo(0, fontMetrics.baseline);
		StyledText st = sampleStyledText();
		for (int w = 0; w < 5; w++) {
			int y = w * fontMetrics.lineHeight;
			int width = w * step;
			canvas
				.color(Argb.BLACK).intOps().fillRect(IntRect.rectangle(0, y, Math.max(width - ellipsisWidth, 0), fontMetrics.lineHeight)).canvas()
				.color(Argb.CYAN).intOps().fillRect(IntRect.rectangle(width - ellipsisWidth, y, ellipsisWidth, fontMetrics.lineHeight)).canvas()
				.color(Argb.WHITE);
			int length = st.length();
			int count = typeMetrics.accommodatedCharCount(st, width, ellipsisWidth);
			StyledText st2 = st.mutableCopy();
			st2.root().deleteText(count, length);
			text.renderText(st2).moveBy(0, fontMetrics.lineHeight);
		}
		return surface;
	}

	@Test
	public void testEzoStyledText() {
		Surface surface = Surface.create(IntDimensions.of(200, 10), true);
		Canvas canvas = surface.createCanvas().color(Argb.BLACK).fill().color(Argb.WHITE);
		Typeface typeface = Typeface.ezo();
		IntTextOps ops = canvas.intOps().newText(typeface);
		int y = typeface.metrics().fontMetrics(TextStyle.regular()).baseline;
		ops.moveTo(2, y).renderText(sampleStyledText());
		recordResult(surface, "ezoStyledText");
	}

	@Test
	public void testFontStyledText() throws Exception {
		Surface surface = Surface.create(IntDimensions.of(300, 25), true);
		Canvas canvas = surface.createCanvas().color(Argb.BLACK).fill().color(Argb.WHITE);
		Typeface typeface = testTypeface(20f);
		IntTextOps ops = canvas.intOps().newText(typeface);
		int y = typeface.metrics().fontMetrics(TextStyle.regular()).baseline;
		ops.moveTo(2, y).renderText(sampleStyledText());
		recordResult(surface, "fontStyledText");
	}

	private ImageSurface simpleTile() {
		ImageSurface tile = ImageSurface.sized(IntDimensions.square(2), true);
		tile.writePixel(0,0,0xffffffff);
		tile.writePixel(1,1,0xffffffff);
		tile.writePixel(1,0,0xff000000);
		tile.writePixel(0,1,0xff000000);
		return tile;
	}

	private static final class RandomFrame implements Frame {

		private final IntDimensions dimensions;
		private final int[] colors;

		public RandomFrame(IntDimensions dimensions) {
			this.dimensions = dimensions;
			colors = IntStream.generate((new Random(0L))::nextInt).limit(dimensions.area()).toArray();
		}

		@Override
		public boolean opaque() {
			return true;
		}

		@Override
		public int readPixel(int x, int y) {
			return colors[y * dimensions.width + x];
		}

		@Override
		public IntDimensions dimensions() {
			return dimensions;
		}

	}

	private static final class SinePlane implements Plane {

		@Override
		public boolean opaque() {
			return true;
		}

		@Override
		public int readPixel(int x, int y) {
			int v = (int) (128.0 + Math.sin((x + y) / 4.0) * 128.0);
			return Argb.gray(v);
		}

	}
}
