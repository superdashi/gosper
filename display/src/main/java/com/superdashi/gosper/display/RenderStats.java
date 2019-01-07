/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
