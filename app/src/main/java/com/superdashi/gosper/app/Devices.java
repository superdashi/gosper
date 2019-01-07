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
package com.superdashi.gosper.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.superdashi.gosper.adafruit.Adafruit2423;
import com.superdashi.gosper.adafruit.Adafruit3531;
import com.superdashi.gosper.awtd.AWTDevice;
import com.superdashi.gosper.awtd.AWTScreen;
import com.superdashi.gosper.config.Config;
import com.superdashi.gosper.data.DataContext;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.KeySet;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.linux.WPASupplicantWifi;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.logging.Loggers;
import com.superdashi.gosper.micro.Interfaces;
import com.superdashi.gosper.pimoroni.PimoroniGfxHat;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;

class Devices {

	private static final String WPA_SUPPLICANT_WIFI = "wpa_supplicant_wifi";
	private static final String DEMO_WIFI = "demo_wifi";
	private static final String AWT_SCREEN = "awt_screen";
	private static final String AWT_DEVICE = "awt_device";
	private static final String ADAFRUIT_2423 = "adafruit_2423";
	private static final String ADAFRUIT_3531 = "adafruit_3531";
	private static final String PIMORONI_GFXHAT = "pimoroni_gfxhat";

	private static final String SUFFIX = ".device";
	private static final Pattern PATTERN_COMPONENT = Pattern.compile("^\\[\\s*([^ ]+)\\s*\\]$");
	private static final Pattern PATTERN_PROPERTY = Pattern.compile("^\\s*([^ ]+)\\s*=\\s*([^ ]+(\\s+[^ ]+)*)\\s*$");

	//TODO need to find a good home for these keysets
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

	private static ScreenClass defaultScreenClass(IntDimensions dimensions) {
		if (dimensions.metBy(IntDimensions.of(128, 128))) return ScreenClass.MICRO;
		if (dimensions.metBy(IntDimensions.of(320, 320))) return ScreenClass.MINI;
		return ScreenClass.PC;
	}

	private final Loggers loggers;
	private final Path dir;

	private final Logger logger;

	private final Map<String, Device> devices = new HashMap<>();

	Devices(Loggers loggers, Path path) {
		this.loggers = loggers;
		this.dir = path;
		logger = loggers.loggerFor("devices");

		scanForDevices();
	}

	void addInterfaces(Interfaces interfaces, Function<Identity, DataContext> dbConnector) {
		devices.forEach((i,d) -> interfaces.addInterface(i, d, dbConnector));
	}

	private void scanForDevices() {
		try {
			Files.find(dir, 1, (p,a) -> p.getFileName().toString().endsWith(SUFFIX)).forEach(this::process);
		} catch (IOException e) {
			logger.error().message("failed to scan devices directory").filePath(dir).log();
		}
	}

	private void process(Path path) {
		// obtain the identifier
		String filename = path.getFileName().toString();
		String identifier = filename.substring(0, filename.length() - SUFFIX.length());
		if (identifier.isEmpty()) {
			logger.warning().message("ignoring empty device identfier").filePath(path).log();
			return;
		}

		// get the lines
		List<String> lines;
		try {
			lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.error().message("failed to read device definition file").filePath(path).log();
			return;
		}

		int lineNo = 0;
		String component = null;
		Map<String, String> properties = new HashMap<>();
		Components components = new Components(identifier);
		for (String line : lines) {
			line = line.trim();
			lineNo ++;
			if (line.isEmpty() || line.charAt(0) == '#') continue;
			{
				Matcher matcher = PATTERN_COMPONENT.matcher(line);
				if (matcher.matches()) {
					if (component == null) {
						// record settings
						components.settings(properties);
					} else {
						// record component
						components.add(component, properties);
					}
					component = matcher.group(1);
					properties = new HashMap<>();
					continue;
				}
			}
			{
				Matcher matcher = PATTERN_PROPERTY.matcher(line);
				if (matcher.matches()) {
					properties.put(matcher.group(1), matcher.group(2));
					continue;
				}
			}
			logger.error().message("invalid line").filePath(path).lineNumber(lineNo).log();
		}
		if (component == null) {
			logger.error().message("no component for device").filePath(path).log();
			return;
		}
		components.add(component, properties);
		component = null;
		properties = null;
		Device device = components.collate();
		if (device != null) devices.put(identifier, device);
	}

	private Path extractPath(Map<String, String> properties, String propertyName) {
		String path = properties.get(propertyName);
		if (path == null) return null;
		try {
			return Paths.get(path);
		} catch (IllegalArgumentException e) {
			logger.error().message("invalid {} path").values(propertyName).log();
			return null;
		}
	}

	private <E extends Enum<E>> E extracEnum(Class<E> clss, Map<String, String> properties, String propertyName) {
		String name = properties.get(propertyName);
		if (name == null) return null;
		try {
			return Enum.valueOf(clss, name.toUpperCase());
		} catch (IllegalArgumentException e) {
			logger.error().message("invalid value for {}").values(propertyName).log();
			return null;
		}
	}

	private Integer extractInteger(Map<String, String> properties, String propertyName) {
		String str = properties.get(propertyName);
		if (str == null) return null;
		try {
			return Integer.valueOf(str);
		} catch (IllegalArgumentException e) {
			logger.error().message("invalid integer number for {}").values(propertyName).log();
			return null;
		}
	}

	private Float extractFloat(Map<String, String> properties, String propertyName) {
		String str = properties.get(propertyName);
		if (str == null) return null;
		try {
			return Float.valueOf(str);
		} catch (IllegalArgumentException e) {
			logger.error().message("invalid floating point number for {}").values(propertyName).log();
			return null;
		}
	}

	private Boolean extractBoolean(Map<String, String> properties, String propertyName) {
		String str = properties.get(propertyName);
		if (str == null) return null;
		if (str.equalsIgnoreCase("true")) return Boolean.TRUE;
		if (str.equalsIgnoreCase("false")) return Boolean.FALSE;
		logger.error().message("invalid boolean for {}").values(propertyName).log();
		return null;
	}

	private Integer extractColor(Map<String, String> properties, String propertyName) {
		String str = properties.get(propertyName);
		if (str == null) return null;
		try {
			return Config.ColorConfig.parse(str).asInt();
		} catch (IllegalArgumentException e) {
			logger.error().message("invalid color for {}").values(propertyName).log();
			return null;
		}
	}

	private boolean checkEnabled(Map<String, String> properties) {
		Boolean enabled = extractBoolean(properties, "enabled");
		return enabled == null || enabled.booleanValue();
	}

	//TODO should log with filepath
	private final class Components {

		private final String identifier;
		private final Map<String, String> settings = new HashMap<>();
		private final Map<String, Map<String, String>> properties = new HashMap<>();
		private final List<Object> components = new ArrayList<>();

		Components(String identifier) {
			this.identifier = identifier;
		}

		void settings(Map<String, String> settings) {
			this.settings.putAll(settings);
		}

		void add(String component, Map<String, String> properties) {
			this.properties.put(component, properties);
		}

		Device collate() {
			logger.debug().message("setting for device {} are {}").values(identifier, settings).log();
			if (!checkEnabled(settings)) {
				logger.info().message("skipping disabled device {}").values(identifier).log();
				return null;
			}
			processWPAWifi();
			processDemoWifi();
			processAWTScreen();
			processAWTDevice();
			processAdafruit2423();
			processAdafruit3531();
			processPimoroniGfxHat();
			Device device = find(Device.class);
			if (device == null) {
				logger.error().message("no device defined for {}").values(identifier).log();
			}
			return device;
		}

		private void processWPAWifi() {
			if (!properties.containsKey(WPA_SUPPLICANT_WIFI)) return;
			Map<String, String> properties = this.properties.get(WPA_SUPPLICANT_WIFI);
			if (!checkEnabled(properties)) return;
			Path supplicantPath = extractPath(properties, "supplicantPath");
			Path supplicantBackupPath = extractPath(properties, "supplicantBackupPath");
			WPASupplicantWifi wifi;
			Logger logger = loggers.loggerFor("wifi");
			if (supplicantPath == null && supplicantBackupPath == null) {
				wifi = new WPASupplicantWifi(logger);
			} else if (supplicantBackupPath == null) {
				wifi = new WPASupplicantWifi(supplicantPath, logger);
			} else if (supplicantPath != null) {
				wifi = new WPASupplicantWifi(supplicantPath, supplicantBackupPath, logger);
			} else {
				wifi = new WPASupplicantWifi(logger);
			}
			components.add(wifi);
		}

		private void processDemoWifi() {
			if (!properties.containsKey(DEMO_WIFI)) return;
			Map<String, String> properties = this.properties.get(DEMO_WIFI);
			if (!checkEnabled(properties)) return;
			//TODO could make seed configurable
			components.add(new TestWifi(new Random()));
		}

		private void processAWTScreen() {
			if (!properties.containsKey(AWT_SCREEN)) return;
			Map<String, String> properties = this.properties.get(AWT_SCREEN);
			if (!checkEnabled(properties)) return;
			Integer screenWidth = extractInteger(properties, "screenWidth");
			if (screenWidth == null) {
				logger.error().message("no valid screenWidth").log();
				return;
			}
			Integer screenHeight = extractInteger(properties, "screenHeight");
			if (screenHeight == null) {
				logger.error().message("no valid screenHeight").log();
				return;
			}
			IntDimensions screenDimensions;
			try {
				screenDimensions = IntDimensions.of(screenWidth, screenHeight);
				if (screenDimensions.isDegenerate()) throw new IllegalArgumentException();
			} catch (IllegalArgumentException e) {
				logger.error().message("invalid screen dimensions").log();
				return;
			}
			ScreenClass screenClass = extracEnum(ScreenClass.class, properties, "screenClass");
			if (screenClass == null) screenClass = defaultScreenClass(screenDimensions);
			ScreenColor screenColor = extracEnum(ScreenColor.class, properties, "screenColor");
			if (screenColor == null) screenColor = ScreenColor.COLOR;
			Integer screenScale = extractInteger(properties, "screenScale");
			if (screenScale == null) screenScale = 1;
			Integer borderWidth = extractInteger(properties, "borderWidth");
			if (borderWidth == null) borderWidth = 0;
			borderWidth = Math.max(0, borderWidth);
			IntMargins border = IntMargins.uniform(borderWidth);
			//TODO extract border color
			Integer borderColor = 0xff202020;
			AWTScreen screen = new AWTScreen(screenClass, screenColor, screenDimensions, screenScale, border, borderColor, null);
			applyScreenDefaults(screen, properties);
			components.add(screen);
		}

		private void processAWTDevice() {
			if (!properties.containsKey(AWT_DEVICE)) return;
			Map<String, String> properties = this.properties.get(AWT_DEVICE);
			if (!checkEnabled(properties)) return;
			String title = properties.getOrDefault("windowTitle", "Gosper");
			Boolean touch = extractBoolean(properties, "supportsTouch");
			if (touch == null) touch = true;
			Wifi wifi = find(Wifi.class);
			AWTScreen screen = find(AWTScreen.class);
			if (screen == null) {
				logger.error().message("no [{}] for [{}]").values(AWT_SCREEN, AWT_DEVICE).log();
				return;
			}
			//TODO make keyset configurable
			KeySet keySet = defaultKeySet;
			AWTDevice device = new AWTDevice(title, screen, wifi, keySet, touch);
			components.add(device);
		}

		private void processAdafruit2423() {
			if (!properties.containsKey(ADAFRUIT_2423)) return;
			Map<String, String> properties = this.properties.get(ADAFRUIT_2423);
			if (!checkEnabled(properties)) return;
			String screenDevice = properties.getOrDefault("screenDevice", "/dev/fb1");
			Wifi wifi = find(Wifi.class);
			Adafruit2423 device = new Adafruit2423(screenDevice, wifi, loggers);
			components.add(device);
		}

		private void processAdafruit3531() {
			if (!properties.containsKey(ADAFRUIT_3531)) return;
			Map<String, String> properties = this.properties.get(ADAFRUIT_3531);
			if (!checkEnabled(properties)) return;
			//TODO bus needs to be configurable
			//TODO make gpio pins configurable
			Wifi wifi = find(Wifi.class);
			Adafruit3531 device = new Adafruit3531(loggers, 1, wifi);
			applyScreenDefaults(device.getScreen().get(), properties);
			components.add(device);
		}

		private void processPimoroniGfxHat() {
			if (!properties.containsKey(PIMORONI_GFXHAT)) return;
			Map<String, String> properties = this.properties.get(PIMORONI_GFXHAT);
			if (!checkEnabled(properties)) return;
			Wifi wifi = find(Wifi.class);
			PimoroniGfxHat device = new PimoroniGfxHat(loggers, wifi);
			applyScreenDefaults(device.getScreen().get(), properties);
			components.add(device);
		}

		@SuppressWarnings("unchecked")
		private <T> T find(Class<T> type) {
			for (int i = components.size() - 1; i >= 0; i--) {
				Object component = components.get(i);
				if (type.isInstance(component)) return (T) component;
			}
			return null;
		}
	}

	private void applyScreenDefaults(Screen screen, Map<String, String> properties) {
		Boolean inverted = extractBoolean(properties, "screenInverted");
		if (inverted != null) screen.inverted(inverted);
		Float contrast = extractFloat(properties, "screenContrast");
		if (contrast != null) screen.contrast(contrast);
		Float brightness = extractFloat(properties, "screenBrightness");
		if (brightness != null) screen.brightness(brightness);
		Integer ambience = extractColor(properties, "screenAmbience");
		if (ambience != null) screen.ambience(ambience);
	}

}
