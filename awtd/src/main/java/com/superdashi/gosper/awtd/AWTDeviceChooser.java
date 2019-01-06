package com.superdashi.gosper.awtd;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.KeySet;
import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.device.network.WifiAccessPoint;
import com.superdashi.gosper.device.network.WifiEntry;
import com.superdashi.gosper.device.network.WifiProtocol;
import com.superdashi.gosper.device.network.WifiStatus;
import com.superdashi.gosper.device.network.WifiStatus.State;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;

public class AWTDeviceChooser {

	private static final KeySet defaultKeySet;
	static {
		KeySet tmp = KeySet.create();
		tmp.addKeyRange(Event.KEY_A, Event.KEY_Z + 1);
		//TODO hacky - no guarantee these will be contiguous
		tmp.addKeyRange(Event.KEY_UP, Event.KEY_CANCEL + 1);
		tmp.removeKey(Event.KEY_CENTER);
		defaultKeySet = tmp.immutableView();
	}

	private static final KeySet joystickKeySet;
	static {
		KeySet tmp = KeySet.create();
		//TODO hacky - no guarantee these will be contiguous
		tmp.addKeyRange(Event.KEY_UP, Event.KEY_CONFIRM+ 1);
		tmp.removeKey(Event.KEY_CENTER);
		joystickKeySet = tmp.immutableView();
	}

	public static AWTDevice choose(Wifi wifi) {

		ScreenClass[] screenOpts = ScreenClass.values();
		IntDimensions[] sizeOpts = {IntDimensions.of(128, 64), IntDimensions.of(320, 240)};
		ScreenColor[] colorOpts = ScreenColor.values();
		Keys[] keyOpts = { new Keys("Default", defaultKeySet), new Keys("None", KeySet.empty()), new Keys("Joystick", joystickKeySet)};

		Config config = new Config();
		config.title = "Default";
		config.screen = ScreenClass.MICRO;
		config.size = IntDimensions.of(128, 64);
		config.color = ScreenColor.COLOR;
		config.keys = keyOpts[0];
		config.touch = true;

		AtomicReference<AWTDevice> deviceHolder = new AtomicReference<>();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame();
			frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

			JTextField title = new JTextField();
			title.setText(config.title);
			frame.add(title);

			JComboBox<ScreenClass> screens = new JComboBox<>(screenOpts);
			screens.setSelectedItem(config.screen);
			frame.add(screens);

			JComboBox<IntDimensions> sizes = new JComboBox<>(sizeOpts);
			sizes.setSelectedItem(config.size);
			frame.add(sizes);

			JComboBox<ScreenColor> colors = new JComboBox<>(colorOpts);
			colors.setSelectedItem(config.color);
			frame.add(colors);

			JComboBox<Keys> keys = new JComboBox<>(keyOpts);
			keys.setSelectedItem(config.keys);
			frame.add(keys);

			JCheckBox touch = new JCheckBox("Supports touch");
			touch.setSelected(config.touch);
			frame.add(touch);

			JButton button = new JButton("Launch");
			button.setAction(new AbstractAction("Launch") {
				@Override public void actionPerformed(ActionEvent e) {
					IntDimensions dimensions = (IntDimensions) sizes.getSelectedItem();
					int scale = 640 / (Math.max(dimensions.width, dimensions.height));
					AWTScreen screen = new AWTScreen((ScreenClass) screens.getSelectedItem(), (ScreenColor) colors.getSelectedItem(), dimensions, scale, IntMargins.uniform(2 * scale), 0xff202020, null);
					AWTDevice device = new AWTDevice(title.getText(), screen, wifi, ((Keys) keys.getSelectedItem()).keySet, touch.isSelected());
					deviceHolder.set(device);
					frame.setVisible(false);
					frame.dispose();
					synchronized (deviceHolder) {
						deviceHolder.notify();
					}
				}});
			frame.add(button);
			frame.setVisible(true);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.addWindowListener(new WindowListener() {
				@Override public void windowOpened(WindowEvent e) { }
				@Override public void windowIconified(WindowEvent e) { }
				@Override public void windowDeiconified(WindowEvent e) { }
				@Override public void windowDeactivated(WindowEvent e) { }
				@Override public void windowClosing(WindowEvent e) { frame.dispose(); }
				@Override public void windowActivated(WindowEvent e) { }
				@Override public void windowClosed(WindowEvent e) {
					synchronized (deviceHolder) {
						deviceHolder.notify();
					}
				}
			});
		});
		try {
			synchronized (deviceHolder) {
				deviceHolder.wait();
			}
		} catch (InterruptedException e) {
			//TODO should close outstanding window
		}
		return deviceHolder.get();
	}

	private static class Keys {

		String name;
		KeySet keySet;

		Keys(String name, KeySet keySet) {
			this.name = name;
			this.keySet = keySet;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class Config {
		String title;
		ScreenClass screen;
		IntDimensions size;
		ScreenColor color;
		Keys keys;
		boolean touch;
	}
}
