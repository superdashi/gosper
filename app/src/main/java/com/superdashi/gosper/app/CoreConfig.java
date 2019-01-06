package com.superdashi.gosper.app;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.superdashi.gosper.core.Cache;
import com.superdashi.gosper.core.CacheControl;
import com.superdashi.gosper.core.CachePolicy;
import com.superdashi.gosper.core.CoreContext;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.logging.Loggers;

class CoreConfig implements CoreContext {

	private static final ZoneId  DEFAULT_ZONE_ID = ZoneId.systemDefault();
	private static final String  DEFAULT_APP_TITLE = "Dashi";
	private static final int     DEFAULT_SERVER_PORT = 8000;
	private static final boolean DEFAULT_PRES_ENABLED = true;

	private static final String propZoneId = "zoneId";
	private static final String propAppTitle = "appTitle";
	private static final String propServerPort = "serverPort";
	private static final String propDatabase="database";
	private static final String propPresEnabled = "presentationEnabled";
	private static final String propPresResolution = "presentationResolution";
	private static final String propAppDataPath = "applicationDataPath";

	// returns null if no context possible
	static final CoreConfig parseCoreConfig(Logger logger, Path path, Runnable shutdown, Loggers loggers) {
		// check file exists
		if (!Files.isRegularFile(path)) {
			logger.error().message("missing core configuration").filePath(path).log();
			return null;
		}

		// read as properties
		Properties properties = new Properties();
		try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException e) {
			logger.error().message("error reading core configuration").filePath(path).stacktrace(e).log();
			return null;
		}

		return new CoreConfig(properties, logger, path, shutdown, loggers);
	}

	private final Loggers loggers;
	private final Runnable shutdown;

	private final ZoneId zoneId;
	private final String appTitle;
	private final int serverPort;
	private final URI database;
	private final Clock clock;
	final CacheControl cacheControl;
	private final ScheduledExecutorService backgroundExecutor;

	private final Resolution resolution;
	private final boolean presEnabled;

	private final Path appDataPath;

	private CoreConfig(Properties properties, Logger logger, Path path, Runnable shutdown, Loggers loggers) {
		if (shutdown == null) throw new IllegalArgumentException("null shutdown");
		if (loggers == null) throw new IllegalArgumentException("null loggers");
		this.loggers = loggers;
		this.shutdown = shutdown;
		boolean headless = GraphicsEnvironment.isHeadless();

		// convert properties to values
		{
			String zoneIdStr = properties.getProperty(propZoneId);
			ZoneId z = null;
			if (zoneIdStr != null && !zoneIdStr.isEmpty()) try {
				z = ZoneId.of(zoneIdStr);
			} catch (DateTimeException e) {
				logger.warning().message("ignoring invalid time zone {}, defaulting to {}").values(zoneIdStr, DEFAULT_ZONE_ID).filePath(path).stacktrace(e).log();
			}
			zoneId = z == null ? DEFAULT_ZONE_ID : z;
		}

		{
			String t = properties.getProperty(propAppTitle);
			appTitle = t == null || t.isEmpty() ? DEFAULT_APP_TITLE : t;
		}

		{
			String p = properties.getProperty(propServerPort);
			if (p == null || p.isEmpty()) {
				serverPort = DEFAULT_SERVER_PORT;
			} else {
				Integer n;
				try {
					n = Integer.parseInt(p);
				} catch (IllegalArgumentException e) {
					n = null;
				}
				if (n < 1 || n >= 16384) {
					n = null;
				}
				if (n == null) {
					logger.warning().message("ignoring invalid server port number {}, defaulting to {}").values(p, DEFAULT_SERVER_PORT).log();
					serverPort = DEFAULT_SERVER_PORT;
				} else {
					serverPort = n;
				}
			}

		}

		{
			String d = properties.getProperty(propDatabase);
			if (d == null || d.isEmpty()) {
				database = null;
			} else {
				URI uri;
				try {
					uri = URI.create(d);
				} catch (IllegalArgumentException e) {
					logger.warning().message("ignoring invalid database URI: {}").values(d).log();
					uri = null;
				}
				database = uri;
			}
		}

		{
			Resolution r = null;
			String s = properties.getProperty(propPresResolution);
			if (s != null) {
				int i = s.indexOf(',');
				if (i != -1) {
					String h = s.substring(0, i).trim();
					String v = s.substring(i + 1).trim();
					try {
						r = Resolution.sized(Integer.parseInt(h), Integer.parseInt(v));
					} catch (IllegalArgumentException e) {
					}
				}
			}
			if (r == null) {
				if (GraphicsEnvironment.isHeadless()) {
					resolution = null;
				} else {
					DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
					resolution = Resolution.sized(mode.getWidth(), mode.getHeight());
					if (s != null) logger.warning().message("ignoring invalid resolution {}, defaulting to {}").values(s, resolution).log();
				}
			} else {
				resolution = r;
			}
		}

		{
			String b = properties.getProperty(propPresEnabled, "true").trim();
			if (b.isEmpty() || b.equalsIgnoreCase("true")) {
				presEnabled = !headless;
			} else if (b.equalsIgnoreCase("false")) {
				presEnabled = false;
			} else {
				presEnabled = DEFAULT_PRES_ENABLED && !headless;
				logger.warning().message("ignoring invalid presentation enabled property {}, defaulting to {}").values(b, presEnabled).log();
			}
		}

		{
			String p = properties.getProperty(propAppDataPath, "");
			Path pth = null;
			if (p.isEmpty()) {
				logger.warning().message("no path configured for {}").values(propAppDataPath).log();
			} else try {
				pth = Paths.get(p);
				if (!Files.isDirectory(pth)) {
					logger.warning().message("path configured for {} does not exist, or is not a readable directory: {}").values(propAppDataPath, pth).log();
					pth = null;
				}
			} catch (InvalidPathException e) {
				logger.warning().message("path configured for {} is invalid: {}").values(propAppDataPath, p).log();
			}
			appDataPath = pth;
		}
		clock = Clock.system(zoneId);

		//TODO cache properties
		cacheControl = CacheControl.createCache(CachePolicy.TRIVIAL);

		backgroundExecutor = Executors.newSingleThreadScheduledExecutor(); //TODO make configurable?
	}

	@Override public Loggers loggers() { return loggers; }
	@Override public ZoneId zoneId() { return zoneId; }
	@Override public String title() { return appTitle; }
	@Override public Runnable shutdown() { return shutdown; }
	@Override public Clock clock() { return clock; }
	@Override public Cache cache() { return cacheControl.getCache(); }
	@Override public ScheduledExecutorService backgroundExecutor() { return backgroundExecutor; }
	@Override public int serverPort() { return serverPort; }

	Tiers.Builder newTiersBuilder() {
		return Tiers.builder(this).dbUrl(database).screenResolution(resolution).presentationEnabled(presEnabled).appDataPath(appDataPath);
	}
}
