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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.bundle.BundleFiles;
import com.superdashi.gosper.bundle.BundleFilesDir;
import com.superdashi.gosper.bundle.BundleFilesJar;
import com.superdashi.gosper.bundle.BundleFilesTxt;
import com.superdashi.gosper.data.DataTier;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.micro.AppInstalls;

//TODO only works with a single runtime instance, but this is not ensured
class InstalledApps {

	private static final Pattern splitter = Pattern.compile("\\s+");

	enum Packaging {
		DIR,
		JAR,
		TXT;
	}

	private final Logger logger;
	private final Path path;
	//TODO use collect?
	private final Set<InstalledApp> apps = new HashSet<>();
	private final Map<URI, Path> urisToPaths = new HashMap<>();
	private final Map<Path, URI> pathsToUris = new HashMap<>();

	public InstalledApps(Logger logger, Path path) {
		this.logger = logger;
		this.path = path;
		update();
	}

	boolean update() {
		// check for no file
		if (!Files.isRegularFile(path)) {
			logger.error().message("installed apps file not available").filePath(path).log();
			return false;
		}
		// read lines
		List<String> lines;
		try {
			lines = Files.readAllLines(path);
		} catch (IOException e) {
			logger.error().message("failed to read installed apps").filePath(path).log();
			return false;
		}
		// parse lines
		Set<InstalledApp> apps = new HashSet<>();
		int lineNo = 0;
		for (String line : lines) {
			lineNo ++;
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#")) continue;
			String[] parts = splitter.split(line);
			if (parts.length == 1) {
				logger.error().message("missing information for install").filePath(path).lineNumber(lineNo).log();
				continue;
			}
			Path appPath;
			String appPathStr = parts[parts.length - 1];
			try {
				appPath = Paths.get( appPathStr );
			} catch (IllegalArgumentException e) {
				logger.error().message("invalid path: {}").filePath(path).lineNumber(lineNo).values(appPathStr).log();
				continue;
			}
			Packaging packaging;
			String packagingStr = parts[0];
			try {
				packaging = Packaging.valueOf(packagingStr.toUpperCase());
			} catch (IllegalArgumentException e) {
				logger.error().message("unknown packaging: {}").filePath(path).lineNumber(lineNo).values(packagingStr).log();
				continue;
			}
			apps.add(new InstalledApp(packaging, appPath));
		}
		// update state
		if (apps.equals(this.apps)) return false; // no change
		this.apps.clear();
		this.apps.addAll(apps);
		return true;
	}

	void adjustApps(AppInstalls appInstalls, DataTier dataTier) {
		//TODO need the ability to remove apps too
		for (InstalledApp app : apps) {
			Path path = app.path;
			if (pathsToUris.containsKey(path)) continue; // already added
			URI uri = app.addTo(appInstalls);
			urisToPaths.put(uri, path);
			pathsToUris.put(path, uri);
			if (dataTier != null) {
				Bundle bundle = appInstalls.bundleForUri(uri);
				dataTier.registerViewer(bundle.appData().appDetails(), bundle.graphViewer());
			}
		}
	}

	Set<URI> appURIs() {
		return this.urisToPaths.keySet();
	}

	private static class InstalledApp {

		final Packaging packaging;
		final Path path;

		InstalledApp(Packaging packaging, Path path) {
			this.packaging = packaging;
			this.path = path;
		}

		URI addTo(AppInstalls appInstalls) {
			return appInstalls.addApplication(files());
		}

		@Override
		public int hashCode() {
			return packaging.ordinal() + path.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof InstalledApps.InstalledApp)) return false;
			InstalledApps.InstalledApp that = (InstalledApps.InstalledApp) obj;
			return this.packaging == that.packaging && this.path.equals(that.path);
		}

		@Override
		public String toString() {
			return packaging.name().toLowerCase() + " " + path;
		}

		private BundleFiles files() {
			switch (packaging) {
			case DIR: return new BundleFilesDir(path);
			case JAR: return new BundleFilesJar(path);
			case TXT: return new BundleFilesTxt(path);
			default : throw new IllegalStateException();
			}
		}
	}
}
