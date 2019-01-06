package com.superdashi.gosper.micro;

import com.superdashi.gosper.bundle.Privileges;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.studio.Composition;
import com.tomgibara.intgeom.IntDimensions;

final class PrivilegedScreen implements Screen {

	private final Screen screen;
	private final Privileges privileges;

	PrivilegedScreen(Screen screen, Privileges privileges) {
		this.screen = screen;
		this.privileges = privileges;
	}

	@Override
	public IntDimensions dimensions() {
		privileges.check(Privileges.READ_SCREEN_DIMENSIONS);
		return screen.dimensions();
	}

	@Override
	public boolean opaque() {
		return screen.opaque();
	}

	@Override
	public boolean inverted() {
		return screen.inverted();
	}

	@Override
	public void inverted(boolean inverted) {
		privileges.check(Privileges.SYSTEM);
		screen.inverted(inverted);
	}

	@Override
	public float contrast() {
		privileges.check(Privileges.READ_SCREEN_CONTRAST);
		return screen.contrast();
	}

	@Override
	public void contrast(float contrast) {
		privileges.check(Privileges.WRITE_SCREEN_CONTRAST);
		screen.contrast(contrast);
	}

	@Override
	public float brightness() {
		privileges.check(Privileges.SYSTEM);
		return screen.brightness();
	}

	@Override
	public void brightness(float brightness) {
		privileges.check(Privileges.SYSTEM);
		screen.brightness(brightness);
	}

	@Override
	public int ambience() {
		privileges.check(Privileges.SYSTEM);
		return screen.ambience();
	}

	@Override
	public void ambience(int color) {
		privileges.check(Privileges.SYSTEM);
		screen.ambience(color);
	}

	@Override
	public void begin() {
		privileges.check(Privileges.SYSTEM);
		screen.begin();
	}

	@Override
	public void end() {
		privileges.check(Privileges.SYSTEM);
		screen.end();
	}

	@Override
	public void reset() {
		privileges.check(Privileges.SYSTEM);
		screen.reset();
	}

	@Override
	public void clear() {
		privileges.check(Privileges.SYSTEM);
		screen.clear();
	}

	@Override
	public void composite(Composition composition) {
		privileges.check(Privileges.SYSTEM);
		screen.composite(composition);
	}

	@Override
	public void blank() {
		privileges.check(Privileges.SYSTEM);
		screen.blank();
	}

	@Override
	public void update() {
		privileges.check(Privileges.SYSTEM);
		screen.update();
	}

}