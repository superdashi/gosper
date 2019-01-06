package com.superdashi.gosper.display;

import com.superdashi.gosper.core.DashiLog;

public class RenderStats {

	private static final long timing[] = new long[60];
	private static int timingIndex = 0;

	// in ms
	private static int lastFrameTime;
	// in ms
	private static float avgFrameTime;

	private static float fps;

	private static int lastDisplayTime;

	private static int vboFloats;

	private static int triangleCount;

	private static int modeSwitches = 0;
	private static int animSwitches = 0;
	private static int lastModeSwitches = 0;
	private static int lastAnimSwitches = 0;


	public static float getFPS() {
		return fps;
	}

	public static void displayEnded() {
		long now = System.currentTimeMillis();
		if (avgFrameTime != 0f) {
			float time = now - timing[timingIndex == 0 ? timing.length - 1 : timingIndex - 1];
			if (time > (2 * avgFrameTime)) {
				DashiLog.debug("Slow frame: {0}ms compared to average {1}ms", time, avgFrameTime);
			}
		}
		timing[timingIndex ++] = now;
		if (timingIndex == timing.length) {
			timingIndex = 0;
			int count = timing.length - 1;
			long t = timing[count] - timing[0];
			float mspf = t / (float) count;
			RenderStats.avgFrameTime = mspf;
			RenderStats.fps = 1000f / mspf;
			RenderStats.lastFrameTime = (int) (timing[count] - timing[count - 1]);
			RenderStats.log();
		}

		lastAnimSwitches = animSwitches;
		lastModeSwitches = modeSwitches;
		animSwitches = 0;
		modeSwitches = 0;
	}

	public static void recordVBOFloats(int count) {
		vboFloats = count;
	}

	public static void recordTriangles(int count) {
		triangleCount = count;
	}

	public static void recordDisplayTime(int displayTime) {
		lastDisplayTime = displayTime;
	}

	public static void recordModeSwitch() {
		modeSwitches ++;
	}

	public static void recordAnimSwitch() {
		animSwitches ++;
	}

	public static void log() {
		if (DashiLog.isDebug()) DashiLog.debug(String.format("FPS: %3.3f  AFT: %3.3f  LFT: %4d  LDT: %4d  MSW: %4d  ASW: %4d  VBO: %6d  TRI: %6d", fps, avgFrameTime, lastFrameTime, lastDisplayTime, lastModeSwitches, lastAnimSwitches, vboFloats, triangleCount));
	}

}
