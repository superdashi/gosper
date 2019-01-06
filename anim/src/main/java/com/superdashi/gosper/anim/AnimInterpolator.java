package com.superdashi.gosper.anim;

public interface AnimInterpolator {

	float interpolate(float t);

	default AnimInterpolator repeated(int reps) {
		return compose(AnimInterpolators.repeat(reps));
	}

	default AnimInterpolator compose(AnimInterpolator before) {
		return t -> interpolate( before.interpolate(t) );
	}

}
