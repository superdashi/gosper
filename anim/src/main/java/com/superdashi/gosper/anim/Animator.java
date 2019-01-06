package com.superdashi.gosper.anim;

public class Animator {

	public enum Terminal {
		NONE,
		HOLD,
		LOOP,
		BOUNCE;
	}

	public final Animation animation;
	public final long startTime;
	public final long finishTime;
	public final Terminal startTerminal;
	public final Terminal finishTerminal;

	final long duration;
	final float denominator;

	Animator(Animation animation, long startTime, long finishTime, Terminal startTerminal, Terminal finishTerminal) {
		this.animation = animation;

		this.startTime = startTime;
		this.finishTime = finishTime;
		this.startTerminal = startTerminal;
		this.finishTerminal = finishTerminal;

		duration = finishTime - startTime + 1;
		denominator = 1f / (finishTime - startTime);
	}

	public void applyAtTime(long t, AnimState state) {
		if (t < startTime) {
			switch (startTerminal) {
			case NONE: return;
			case HOLD: applyAtTime(0f, state); return;
			case LOOP: t = ((t - startTime) % duration) + duration;
			case BOUNCE:
				t = (t - startTime) % (2*duration);
				t = t < -duration ? t + 2*duration : -t;
			}
		} else if (t > finishTime) {
			switch (finishTerminal) {
			case NONE: return;
			case HOLD: applyAtTime(1f, state); return;
			case LOOP: t = (t - finishTime) % duration;
			case BOUNCE:
				t = (t - finishTime) % (2 * duration);
				t = t < duration ? duration - t : t - duration;
			}
		} else {
			t -= startTime;
		}
		applyAtTime(t * denominator, state);
	}

	private void applyAtTime(float t, AnimState state) {
		animation.applyAtTime(t, state);
	}
}
