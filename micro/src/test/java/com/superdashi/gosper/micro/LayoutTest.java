package com.superdashi.gosper.micro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.micro.Constraints;
import com.superdashi.gosper.micro.Layout;
import com.superdashi.gosper.micro.Location;
import com.superdashi.gosper.micro.Places;
import com.superdashi.gosper.micro.VisualMetrics;
import com.superdashi.gosper.micro.VisualQualifier;
import com.superdashi.gosper.micro.VisualSpec;
import com.superdashi.gosper.micro.VisualTheme;
import com.superdashi.gosper.micro.Layout.Direction;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public class LayoutTest {

	private static final IntDimensions screenDimensions = IntDimensions.of(128, 64);
	private static final Qualifier qualifier = Qualifier.with(Locale.getDefault(), ScreenClass.MICRO, ScreenColor.MONO, Flavor.GENERIC);
	private static final VisualQualifier visualQualifier = new VisualQualifier(qualifier, screenDimensions);
	private static final VisualTheme theme = new VisualTheme();
	private static final VisualMetrics metrics = new VisualMetrics();
	static final VisualSpec spec = VisualSpec.create(visualQualifier, true, theme, metrics);

	@Test
	public void testLocations() {
		// generate all possible layouts (1 + 4 + 16 + 64 + 256 = 341)
		List<Layout> all = new ArrayList<>();
		all.add(Layout.single());
		int skip = 0;
		for (int i = 0; i < 4; i++) {
			List<Layout> copy = new ArrayList<>(all.subList(skip, all.size()));
			skip = all.size();
			for (Layout layout : copy) {
				for (Direction dir : Direction.values()) {
					all.add(layout.add(dir));
				}
			}
		}
		// check that there are no null locations, and no dupes
		for (int i = 0; i < all.size(); i++) {
			Layout layout = all.get(i);
			//System.out.println(i + " > " + layout);
			List<Location> locations = Arrays.asList(layout.locations());
			//System.out.println(locations);
			Assert.assertFalse(locations.contains(null)); // check no nulls
			Assert.assertEquals(new HashSet<>(locations).size(), locations.size()); // check no dupes
		}
	}

	@Test
	public void testLocationName() {
		{
			Layout layout = Layout.single().withLocation("only");
			Assert.assertEquals("only", layout.locations()[0].name);
		}

		{
			Layout layout = Layout.single().addLeft().withLocation("first");
			Assert.assertArrayEquals(new Location[]{Location.named("right"), Location.named("first")}, layout.locations());
		}
	}

	@Test
	public void testMinimumDimensions() {
		Layout layoutA = Layout
			.single().withMinimumSize(10, 10)
			.addRight().withMinimumSize(2, 1)
			.addBelow().withMinimumSize(5, 3);
		Assert.assertEquals(IntDimensions.of(12, 13), layoutA.sizer(spec).minimumDimensions(new Constraints(spec.bounds)));
		Layout layoutB = Layout
				.single().withMinimumSize(10, 10)
				.addRight().withMinimumSize(2, 1)
				.addBelow().withMinimumSize(15, 3);
			Assert.assertEquals(IntDimensions.of(15, 13), layoutB.sizer(spec).minimumDimensions(new Constraints(spec.bounds)));
	}

	@Test
	public void testPlaces() {
		Optional<Places> placesA = Layout.single()
				.sizer(spec).computePlaces(new Constraints(IntRect.bounded(30, 7, 100, 200)));
		Assert.assertEquals(IntRect.bounded(30, 7, 100, 200), placesA.get().placeAtLocation(Location.center).outerBounds);

		Places placesB = Layout.single().addRight()
				.sizer(spec).computePlaces(new Constraints(IntRect.bounded(10, 10, 110, 60))).get();
		Assert.assertEquals(IntRect.bounded(10, 10, 60, 60), placesB.placeAtLocation(Location.left).outerBounds);
		Assert.assertEquals(IntRect.bounded(60, 10, 110, 60), placesB.placeAtLocation(Location.right).outerBounds);

		Places placesC = Layout.single().addBelow()
				.sizer(spec).computePlaces(new Constraints(IntRect.bounded(10, 10, 110, 60))).get();
		Assert.assertEquals(IntRect.bounded(10, 10, 110, 35), placesC.placeAtLocation(Location.top).outerBounds);
		Assert.assertEquals(IntRect.bounded(10, 35, 110, 60), placesC.placeAtLocation(Location.bottom).outerBounds);

		// remaining weight after 20 (vertical) evenly split, yielding 10/30 vertical split
		Places placesD = Layout.single().addRight().addBelow().withMinimumSize(1, 20)
				.sizer(spec).computePlaces(new Constraints(IntRect.bounded(0,0,40,40))).get();
		Assert.assertEquals(IntRect.bounded(0, 0, 20, 10), placesD.placeAtLocation(Location.left).outerBounds);
		Assert.assertEquals(IntRect.bounded(20, 0, 40, 10), placesD.placeAtLocation(Location.right).outerBounds);
		Assert.assertEquals(IntRect.bounded(0, 10, 40, 40), placesD.placeAtLocation(Location.bottom).outerBounds);

		// giving top group full weight, reduces bottom its minimum (20) for a 20/20 split
		Places placesE = Layout.single().addRight().group().withWeight(1f).addBelow().withMinimumSize(1, 20)
				.sizer(spec).computePlaces(new Constraints(IntRect.bounded(0,0,40,40))).get();
		Assert.assertEquals(IntRect.bounded(0, 0, 20, 20), placesE.placeAtLocation(Location.left).outerBounds);
		Assert.assertEquals(IntRect.bounded(20, 0, 40, 20), placesE.placeAtLocation(Location.right).outerBounds);
		Assert.assertEquals(IntRect.bounded(0, 20, 40, 40), placesE.placeAtLocation(Location.bottom).outerBounds);

		Places placesF = Layout.single().addRight().addBelow()
				.sizer(spec).computePlaces(new Constraints(IntRect.bounded(0,0,40,40))).get();
		System.out.println(placesF);
	}

	@Test
	public void testBadStyle() {
		Layout layout = Layout.single().withStyle(new Style().marginBottom(-2));
		try {
			layout.sizer(spec).computePlaces(new Constraints(IntRect.bounded(0, 0, 50, 50)));
			Assert.fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
	}

	@Test
	public void testMargins() {
		Layout layout = Layout.single().withMinimumSize(2,2).withStyle(new Style().marginLeft(1).marginRight(2).marginTop(1).marginBottom(2)).addBelow().withMinimumSize(2,2);
		IntDimensions minimum = IntDimensions.of(5, 7);
		Assert.assertEquals(minimum, layout.sizer(spec).minimumDimensions(new Constraints(spec.bounds)));
		Places places = layout.sizer(spec).computePlaces(new Constraints(IntRect.atOrigin(minimum))).get();
		Assert.assertEquals(IntRect.bounded(0, 5, 5, 7), places.placeAtLocation(Location.bottom).innerBounds);
		Assert.assertEquals(IntRect.bounded(1, 1, 3, 3), places.placeAtLocation(Location.top).innerBounds);
	}

}
