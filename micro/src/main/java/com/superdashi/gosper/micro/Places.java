package com.superdashi.gosper.micro;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntRect;

public final class Places {

	//TODO hacky - used to indicate rogue values
	static final Places none = new Places(IntRect.UNIT_SQUARE);

	private final IntRect bounds;
	private final Place[] places;

	Places(IntRect bounds, Place... places) {
		this.bounds = bounds;
		this.places = places;
	}

	public IntRect bounds() {
		return bounds;
	}

	public Place placeAtLocation(Location location) {
		if (location == null) throw new IllegalArgumentException("null location");
		Place place = findPlace(location);
		if (place == null) throw new IllegalArgumentException("location unavailable");
		return place;
	}

	public Optional<Place> possiblePlaceForName(Location location) {
		if (location == null) throw new IllegalArgumentException("null location");
		return Optional.ofNullable(findPlace(location));
	}

	public Place placeAt(IntCoords coords) {
		if (coords == null) throw new IllegalArgumentException("null coords");
		Place place = findPlace(coords);
		if (place == null) throw new IllegalArgumentException("no place at coordinates");
		return place;
	}

	public Optional<Place> possiblePlaceAt(IntCoords coords) {
		if (coords == null) throw new IllegalArgumentException("null coords");
		return Optional.ofNullable(findPlace(coords));
	}

	public Stream<Place> stream() {
		return Arrays.stream(places);
	}

	// does not check if places overlap
	Places plus(Places that) {
		IntRect bounds = this.bounds.growToIncludeRect(that.bounds);
		Place[] places = new Place[this.places.length + that.places.length];
		System.arraycopy(this.places, 0, places, 0, this.places.length);
		System.arraycopy(that.places, 0, places, this.places.length, that.places.length);
		return new Places(bounds, places);
	}

	private Place findPlace(Location location) {
		// note, this list will be short, so we just go through and check each directly
		for (Place place : places) {
			if (place.location.equals(location)) return place;
		}
		return null;
	}

	private Place findPlace(IntCoords coords) {
		if (!bounds.containsUnit(coords)) return null;
		for (Place place : places) {
			if (place.outerBounds.containsPoint(coords)) return place;
		}
		return null;
	}

	@Override
	public String toString() {
		return bounds + " " + Arrays.toString(places);
	}

}
