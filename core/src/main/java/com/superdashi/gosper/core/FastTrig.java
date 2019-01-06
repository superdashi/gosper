package com.superdashi.gosper.core;

import java.util.Random;

import com.tomgibara.geom.core.Angles;


public class FastTrig {

	private static final int POWER = 14;
	private static final int SCALE = 1 << POWER;
	private static final int A_MASK = SCALE - 1;
	private static final int Q_MASK = 3 << POWER;
	private static final int MASK = A_MASK | Q_MASK;
	private static final float SCALE_DOWN = 90f / SCALE;
	private static final float SCALE_UP = SCALE / 90f;
	private static final float[] SINES = new float[SCALE];

	private static final int S0 = 0 * SCALE;
	private static final int S1 = 2 * SCALE - 1;
	private static final int S2 = 2 * SCALE;
	private static final int S3 = 4 * SCALE - 1;

	private static final int C0 = 1 * SCALE - 1;
	private static final int C1 = 1 * SCALE;
	private static final int C2 = 3 * SCALE - 1;
	private static final int C3 = 3 * SCALE;

	static {
		for (int i = 0; i < SINES.length; i++) {
			float degrees = i * SCALE_DOWN;
			float radians = Angles.toRadians(degrees);
			float sine = (float) Math.sin(radians);
			SINES[i] = sine;
		}
	}

	public static float sin(float degrees) {
		int i = Math.round(degrees * SCALE_UP) & MASK;
		switch ((i & Q_MASK) >> POWER) {
		case 0: return  SINES[i - S0];
		case 1: return  SINES[S1 - i];
		case 2: return -SINES[i - S2];
		case 3: return -SINES[S3 - i];
		default: return 0f; /* not possible */
		}
	}

	public static float cos(float degrees) {
		int i = Math.round(degrees * SCALE_UP) & MASK;
		switch ((i & Q_MASK) >> POWER) {
		case 0: return  SINES[C0 - i];
		case 1: return -SINES[i - C1];
		case 2: return -SINES[C2 - i];
		case 3: return  SINES[i - C3];
		default: return 0f; /* not possible */
		}
	}

	public static void main(String... args) {
		checkCorrectness();
		checkPerformance();
	}

	private static void checkCorrectness() {
		Random r = new Random(0L);
		float maxErrS = 0f;
		float maxErrC = 0f;
		for (int i = 0; i < 10000; i++) {
			float degrees = (r.nextInt(20000000) - 10000000) * 0.0001f;
			float radians = Angles.toRadians(degrees);
			float s0 = (float) Math.sin(radians);
			float s1 = sin(degrees);
			float c0 = (float) Math.cos(radians);
			float c1 = cos(degrees);
			float errS = Math.abs(s0 - s1);
			float errC = Math.abs(c0 - c1);
			maxErrS = Math.max(errS, maxErrS);
			maxErrC = Math.max(errC, maxErrC);
		}
		if (maxErrS >= 0.001) throw new IllegalStateException("sine max err: " + maxErrS);
		if (maxErrC >= 0.001) throw new IllegalStateException("cosine max err: " + maxErrC);
	}

	private static void checkPerformance() {
		float sum = 0f;
		for (int i = 0; i < 20; i++) {
			sum = checkPerformance(sum);
		}
		System.out.println(sum);
	}

	private static float checkPerformance(float sum) {
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < 100; i++) {
				sum += checkPerformanceA();
			}
			long finish = System.currentTimeMillis();
			System.out.println("Native:" + (finish - start));
		}
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < 100; i++) {
				sum += checkPerformanceB();
			}
			long finish = System.currentTimeMillis();
			System.out.println("Approx:" + (finish - start));
		}
		return sum;
	}

	private static float checkPerformanceA() {
		float sum = 0;
		for (float d = 0; d < 10000; d++) {
			sum += (float) Math.sin(d);
			sum += (float) Math.cos(d);
		}
		return sum;
	}

	private static float checkPerformanceB() {
		float sum = 0;
		for (float d = 0; d < 10000; d++) {
			sum += sin(d);
			sum += cos(d);
		}
		return sum;
	}
}
