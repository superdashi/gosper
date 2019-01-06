package com.superdashi.gosper.awtd;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.KeySet;
import com.superdashi.gosper.device.Keyboard;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.device.DeviceSpec.Builder;
import com.superdashi.gosper.device.network.Wifi;
import com.tomgibara.fundament.Producer;
import com.tomgibara.intgeom.IntCoords;

public class AWTDevice implements Device, KeyListener {

	private static int mapKeyCode(int k) {
		if (k >= KeyEvent.VK_A && k <= KeyEvent.VK_Z) return k; // same, based on ASCII
		switch (k) {
		case KeyEvent.VK_UP : return Event.KEY_UP;
		case KeyEvent.VK_KP_UP : return Event.KEY_UP;
		case KeyEvent.VK_DOWN : return Event.KEY_DOWN;
		case KeyEvent.VK_KP_DOWN : return Event.KEY_DOWN;
		case KeyEvent.VK_LEFT : return Event.KEY_LEFT;
		case KeyEvent.VK_KP_LEFT : return Event.KEY_LEFT;
		case KeyEvent.VK_RIGHT : return Event.KEY_RIGHT;
		case KeyEvent.VK_KP_RIGHT : return Event.KEY_RIGHT;
		case KeyEvent.VK_ENTER : return Event.KEY_CONFIRM;
		case KeyEvent.VK_ESCAPE : return Event.KEY_CANCEL;
		//case KeyEvent.VK_SPACE : return Event.KEY_CENTER;
		default: return -1;
		}
	}

	private static int instances = 0;

	private final String title;
	private final AWTScreen screen;
	private final Wifi wifi;

	private final DeviceSpec spec;
	private final MouseAdapter adapter;
	private JFrame frame = null;

	private Consumer<Event> eventConsumer;

	public AWTDevice(String title, AWTScreen screen, Wifi wifi, KeySet keySet, boolean touch) {
		if (title == null) throw new IllegalArgumentException("null title");
		this.title = title;
		this.screen = screen;
		this.wifi = wifi;
		adapter = new MouseAdapter(touch);
		Keyboard keyboard = Keyboard.newBuilder(keySet).build();
		Builder builder = DeviceSpec.newBuilder().addDPad().addKeyboard(keyboard);
		if (touch) {
			builder.addTouch();
		} else {
			builder.addMouse();
		}
		if (wifi != null) {
			builder.addWifi();
		}
		spec = builder.setScreen(screen.screenClass(), screen.screenColor()).addScreenAmbience().addScreenBrightness().addScreenContrast().build();
	}

	// device methods

	@Override
	public DeviceSpec getSpec() {
		return spec;
	}

	@Override
	public void capture() {
		try {
			SwingUtilities.invokeAndWait(this::captureImpl);
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void relinquish() {
		try {
			SwingUtilities.invokeAndWait(this::relinquishImpl);
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<Screen> getScreen() {
		return Optional.of(screen);
	}

	@Override
	public Optional<Wifi> getWifi() {
		return Optional.ofNullable(wifi);
	}

	@Override
	public Optional<Producer<List<Event>>> events() {
		return Optional.empty();
	}

	@Override
	public void setEventConsumer(Consumer<Event> eventConsumer) {
		this.eventConsumer = eventConsumer;
	}

	@Override
	public void setSpecConsumer(Consumer<DeviceSpec> specConsumer) {
		// doesn't change yet
	}

	// key listener methods

	@Override
	public void keyPressed(KeyEvent e) {
		sendEvent(e, true);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		sendEvent(e, false);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		/* ignored */
	}

	// private helper methods

	private void sendEvent(KeyEvent e, boolean down) {
		if (eventConsumer == null) return;
		int key = mapKeyCode(e.getKeyCode());
		if (key == -1) return;
		if (!spec.keyboard.keySet.containsKey(key)) return;
		Event event = Event.newKeyEvent(key, down, false, e.isShiftDown(), e.isAltDown() || e.isAltGraphDown(), e.isControlDown(), e.getWhen());
		eventConsumer.accept(event);
	}

	private void captureImpl() {
		if (frame != null) return;
		frame = new JFrame(title);
		frame.setUndecorated(false);


		frame.add(screen);
		frame.setVisible(true);
		frame.pack();
		frame.addKeyListener(this);
		frame.addMouseListener(adapter);
		frame.addMouseMotionListener(adapter);
		frame.setLocationRelativeTo(null);
	}

	private void relinquishImpl() {
		if (frame == null) return;
		frame.removeKeyListener(this);
		frame.setVisible(false);
		frame.remove(screen);
		frame.dispose();
		frame = null;
	}

	// inner classes

	private class MouseAdapter implements MouseListener, MouseMotionListener {

		private final boolean touch;

		MouseAdapter(boolean touch) {
			this.touch = touch;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			deliverMove(e, true, true);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (!touch) deliverMove(e, false, true);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (eventConsumer == null) return;
			IntCoords coords = coordsForEvent(e);
			Event event = Event.newPointEvent(keyForEvent(e), coords.x, coords.y, e.getWhen());
			eventConsumer.accept(event);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			deliverMove(e, true, false);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			deliverMove(e, false, false);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			/* ignored */
		}

		@Override
		public void mouseExited(MouseEvent e) {
			/* ignored */
		}

		private void deliverMove(MouseEvent e, boolean down, boolean repeat) {
			if (eventConsumer == null) return;
			IntCoords coords = coordsForEvent(e);
			Event event = Event.newMoveEvent(keyForEvent(e), coords.x, coords.y, down, repeat, e.getWhen());
			eventConsumer.accept(event);
		}

		private IntCoords coordsForEvent(MouseEvent e) {
			return screen.translate(IntCoords.at(e.getX(), e.getY()));
		}
		private int keyForEvent(MouseEvent e) {
			if (touch) return Event.KEY_NONE;
			switch (e.getButton()) {
			case MouseEvent.NOBUTTON: return Event.KEY_NONE;
			case MouseEvent.BUTTON1 : return Event.KEY_MOUSE_1;
			case MouseEvent.BUTTON2 : return Event.KEY_MOUSE_2;
			case MouseEvent.BUTTON3 : return Event.KEY_MOUSE_3;
			default: return Event.KEY_NONE;
			}
		}
	}
}
