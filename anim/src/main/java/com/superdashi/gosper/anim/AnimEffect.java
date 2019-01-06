package com.superdashi.gosper.anim;

import com.superdashi.gosper.anim.Animator.Terminal;

@FunctionalInterface
public interface AnimEffect {

	AnimState stateAtTime(float t);

	default Animation interpolate() {
		return new Animation(this, AnimInterpolators.linear());
	}

	default Animation interpolate(AnimInterpolator interpolator) {
		if (interpolator == null) throw new IllegalArgumentException("null interpolator");
		return new Animation(this, interpolator);
	}

	default Animator at(long time) {
		return interpolate().animator(time, time, Terminal.HOLD, Terminal.HOLD);
	}
}
