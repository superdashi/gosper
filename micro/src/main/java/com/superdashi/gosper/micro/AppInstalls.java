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
package com.superdashi.gosper.micro;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.superdashi.gosper.bundle.AppRole;
import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.bundle.BundleCollation;
import com.superdashi.gosper.bundle.BundleFileUtil;
import com.superdashi.gosper.bundle.BundleFiles;
import com.superdashi.gosper.bundle.BundleFilesMem;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.util.DoneFuture;
import com.tomgibara.radix4.Radix4;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

public final class AppInstalls {

	private static final String ANONYMOUS_APP_NAME = "anonymous";
	private static final Path SETTINGS_DIR = Paths.get("settings");
	private static final String SETTINGS_SUFFIX = ".settings";

	private static final int INSTANCE_ID_SIZE = 12;
	private static final SecureRandom random = new SecureRandom();
	private static final byte[] randomBytes = new byte[INSTANCE_ID_SIZE];
	private static final char[] randomChars = new char[INSTANCE_ID_SIZE * 2];
	private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static final Radix4 radix4 = Radix4.block().configure().setTerminated(false).use();

	private static final Maps<URI, Bundle> bundlesByUriMaps = Collect.setsOf(URI.class).mappedTo(Bundle.class);
	private static final Maps<Identity, Bundle> bundlesByIdentityMaps = Collect.setsOf(Identity.class).mappedTo(Bundle.class);
	private static final Maps<Namespace, Item> itemMaps = Collect.setsOf(Namespace.class).mappedTo(Item.class);

	private static BundleFiles rootBundleFiles = BundleFilesMem.newBuilder(URI.create("internal://env/rootApp"))
			.addFile(BundleFileUtil.newLinesFile("external/meta.items",
					"application_root name='Root application', gosper:kind=application, gosper:type=com.superdashi.gosper.micro.RootApplication, gosper:role=base",
					"activity_root name='Root', gosper:kind=activity, gosper:launch=true"))
			.addFile(BundleFileUtil.newEmptyFile(BundleCollation.NAME_REQ_PRIV))
			.addFile(BundleFileUtil.newLinesFile(BundleCollation.NAME_BUNDLE_SETTINGS, "namespace=www.superdashi.com/gosper"))
			.build();

	private static String generateInstanceId() {
		synchronized (random) {
			random.nextBytes(randomBytes);
			for (int i = 0, j = 0; i < randomBytes.length; i++) {
				randomChars[j++] = hexChars[ (randomBytes[i] >> 4) & 0xf ];
				randomChars[j++] = hexChars[ (randomBytes[i]     ) & 0xf ];
			}
			return new String(randomChars);
		}
	}

	//TODO how to deal with names of data URLs?
	private static String nameOf(URI uri) {
		String name = null;
		String path = uri.getPath();
		if (!path.isEmpty() && !path.equals("/")) {
			int i = path.lastIndexOf('/');
			if (i == -1) {
				name = path;
			} else if (i < path.length() - 1) {
				name = path.substring(i + 1);
			} else {
				int j = path.lastIndexOf('/', i - 1);
				name = j == i - 1 ? null : path.substring(j + 1, i);
			}
		}
		if (name == null) {
			name = uri.getAuthority();
		}
		if (name == null) {
			name = ANONYMOUS_APP_NAME;
		}
		return name;
	}

	// exposed for use by runtime and interfaces
	final Object lifetime = new Object();
	final ClassLoader classLoader = AppInstalls.class.getClassLoader();

	private final Path appDataDir;
	private final Logger logger;
	private final ScheduledExecutorService backgroundExecutor;

	private final Path settingsDir;

	private final Map<URI, Bundle> bundlesByUri = bundlesByUriMaps.newMap();
	private final Map<Identity, Bundle> bundlesByIdentity = bundlesByIdentityMaps.newMap();
	private final Map<Identity, Item> settings = new ConcurrentHashMap<>();

	//TODO should be configurable
	final Bundle baseBundle;

	private final boolean available;

	//TODO should be pluggable
	BundleFiles badBundleFiles = BundleFilesMem.newBuilder()
			.addFile(BundleFileUtil.newResourceFile("internal/pictures/face_sad.png", Runtime.class.getClassLoader(), "face_sad.png"))
			.addFile(BundleFileUtil.newLinesFile("external/meta.items",
					"application_bad name='Bad application', gosper:kind=application, gosper:type=com.superdashi.gosper.micro.BadApplication",
					"activity_launch name='Failed launch', gosper:kind=activity, gosper:launch=true",
					"activity_details name='Failure Details', gosper:kind=activity")
					)
			.addFile(BundleFileUtil.newEmptyFile(BundleCollation.NAME_REQ_PRIV))
			.addFile(BundleFileUtil.newLinesFile(BundleCollation.NAME_BUNDLE_SETTINGS, "namespace=www.superdashi.com/gosper"))
			.build();

	public AppInstalls(Path appDataDir, ScheduledExecutorService backgroundExecutor, Logger logger) {
		if (appDataDir == null) throw new IllegalArgumentException("null appDataDir");
		if (backgroundExecutor == null) throw new IllegalArgumentException("null backgroundExecutor");
		if (logger == null) throw new IllegalArgumentException("null logger");
		this.appDataDir = appDataDir;
		this.backgroundExecutor = backgroundExecutor;
		this.logger = logger;

		baseBundle = new BundleCollation("", rootBundleFiles, logger.child("root-app"), classLoader, true).bundle;

		boolean available = false;
		settingsDir = appDataDir.resolve(SETTINGS_DIR);
		if (!Files.exists(settingsDir)) try {
			Files.createDirectories(settingsDir);
			logger.info().message("created directory for settings at {}").values(settingsDir).log();
		} catch (IOException e) {
			logger.warning().message("failed to create directory for application configuration: {}").values(settingsDir).stacktrace(e).log();
		} else try {
			Files.list(settingsDir)
				.filter(p -> p.getFileName().toString().endsWith(SETTINGS_SUFFIX))
				.forEach(p -> {
					String name = p.getFileName().toString();
					String enc = name.substring(0, name.length() - SETTINGS_SUFFIX.length());
					byte[] bytes = radix4.coding().decodeFromString(enc);
					//TODO switch to CharsetDecoder
					String idStr = new String(bytes, StandardCharsets.UTF_8);
					Identity.maybeFromString(idStr).ifPresent(id -> {
						Item config;
						try (ReadStream s = Streams.streamInput(Files.newInputStream(p))) {
							config = Item.deserialize(s);
						} catch (IOException e) {
							logger.warning().message("failed to read config from file {} for {}").values(p, id).stacktrace(e).log();
							return;
						}
						settings.put(id, config);
					});
				});
			available = true;
		} catch (IOException e) {
			logger.warning().message("failed to list configuration files in directory {}").values(settingsDir).stacktrace(e).log();
		}
		this.available = available;
	}

	public URI addApplication(BundleFiles files) {
		if (files == null) throw new IllegalArgumentException("null files");

		URI uri = files.uri();
		// generate an id for the new application instance
		String id = generateInstanceId();
		// extract name
		String name = nameOf(uri);
		// create a logger for any issues that arise
		Logger logger = this.logger.child(id);
		// process the app directory
		Bundle bundle = new BundleCollation(id, files, logger, classLoader, false).bundle;
		// obtain the identity of the bundle
		Identity identity = bundle.appData().appDetails().identity();
		synchronized (lifetime) {
			// check for duplicate identities and reject
			if ( bundlesByIdentity.containsKey(identity) ) {
				throw new IllegalArgumentException("Duplicate bundle identity: " + identity);
			}
			// record the application data
			bundlesByUri.put(uri, bundle);
			bundlesByIdentity.put(identity, bundle);
		}
		return uri;
	}

	public Bundle bundleForUri(URI appUri) {
		Bundle bundle = bundlesByUri.get(appUri);
		if (bundle == null) throw new IllegalArgumentException("unknown URI");
		return bundle;
	}

	public Optional<Bundle> bundleForRole(AppRole role) {
		if (role == null) throw new IllegalArgumentException("null role");
		//TODO should be explicit since user may want to choose
		//TODO or perhaps return a list?
		return bundlesByUri.values().stream().filter(d -> d.appData().role() == role).findFirst();
	}

	Settings settingsFor(Identity identity) {
		return new Settings() {

			@Override
			public Future<Item> saveSettings(Item item) {
				if (item == null) throw new IllegalArgumentException("null item");
				checkAvailable();
				//TODO needs to wait for previous save to finish
				synchronized (settings) {
					Item oldConfig = settings.get(identity);
					if (item.equals(oldConfig)) return new DoneFuture<>(item);
					settings.put(identity, item);
				}
				String str = identity.toString();
				byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
				String name = radix4.coding().encodeToString(bytes) + SETTINGS_SUFFIX;
				Path path = settingsDir.resolve(name);
				return backgroundExecutor.submit(() -> {
					try (WriteStream s = Streams.streamOutput(Files.newOutputStream(path))) {
						item.serialize(s);
					} catch (IOException e) {
						throw new RuntimeException("Failed to write settings to file:  " + path);
					}
					return item;
				});
			}

			@Override
			public Item loadSettings() {
				checkAvailable();
				synchronized (settings) {
					// first try to get from cache
					Item item = settings.get(identity);
					if (item != null) return item;
					// then check bundle is present
					Bundle bundle = bundlesByIdentity.get(identity);
					if (bundle == null) {
						logger.error().message("settings requested for unregistered bundle identity {}").values(identity).log();
						return Item.nothing();
					}
					// then populate with default settings
					item = bundle.defaultSettings().orElse(Item.nothing());
					settings.put(identity, item);
					//TODO possibly merge default settings with cached settings somehow?
					return item;
				}
			}

			@Override
			public boolean loadingAvailable() {
				return available;
			}

			@Override
			public boolean savingAvailable() {
				return available;
			}

			private void checkAvailable() {
				if (!available) throw new IllegalStateException("not available");
			}
		};
	}
}
