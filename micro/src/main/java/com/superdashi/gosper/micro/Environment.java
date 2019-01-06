package com.superdashi.gosper.micro;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.superdashi.gosper.bundle.AppData;
import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.bundle.BundleFile;
import com.superdashi.gosper.bundle.PrivilegeException;
import com.superdashi.gosper.bundle.Privileges;
import com.superdashi.gosper.data.DataContext;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.util.Time;
import com.tomgibara.fundament.Producer;
import com.tomgibara.streams.ReadStream;

// the interface through which applications can access 'persistent capabilities'

//TODO can pre-apply security checks based on requesting application
//TODO should expose methods for identifying installed apps
public final class Environment {

	private static final Duration minute = Duration.ofMinutes(1L);

	static final String INTERNAL_SCHEME = "internal";
	static final String RUNTIME_AUTHORITY = "runtime";
	static final String APP_AUTHORITY = "app";
	static final String LOGGER_SOURCE_TYPE = "app";

	private final Interface face;
	//TODO flatten appInstance and remove from environment
	final AppInstance appInstance;
	private final Runtime runtime;
	final Directory directory;
	private final Micro micro;
	private final Device device;
	private final String internalPrefix; // based on the app's instance id
	private final Logger logger;
	private final Items items;
	private final Items metaItems;

	// caches clock ticked by minutes
	private Clock minuteClock = null;
	private DataContext dataContext = null;

	Environment(Interface face, AppInstance appInstance) {
		this.face = face;
		this.appInstance = appInstance;
		Privileges privileges = appInstance.privileges;
		this.runtime = face.runtime;
		this.directory = face.directory();
		this.micro = new PrivilegedMicro(directory, privileges);
		this.device = new PrivilegedDevice(face.device, privileges); // device wrapped but not exposed
		internalPrefix = "/" + appInstance.instanceId + "/";
		logger = runtime.context.loggers().loggerFor(LOGGER_SOURCE_TYPE, appInstance.instanceId);
		items = new Items(appInstance.bundle, face.qualifier(), false);
		metaItems = new Items(appInstance.bundle, face.qualifier(), true);
	}

	public Clock clock() {
		return face.runtime.context.clock();
	}

	public Logger logger() {
		return logger;
	}

	public AppData appData() {
		return appInstance.bundle.appData();
	}

	public Items items() {
		return items;
	}

	public Items metaItems() {
		return metaItems;
	}

	public Locale locale() {
		return face.qualifier().lang;
	}

	public Time time() {
		return new Time(locale(), face.runtime.context.zoneId());
	}

	public void executeInBackground(Runnable runnable) {
		runtime.context.backgroundExecutor().execute(runnable);
	}

	public Future<?> submitBackground(Runnable runnable) {
		return runtime.context.backgroundExecutor().submit(runnable);
	}

	public <T> Future<?> submitBackground(Callable<T> callable) {
		return runtime.context.backgroundExecutor().submit(callable);
	}

	//TODO consider exposing scheduled executions

	//TODO have a dedicated ResourceRef interface for this purpose
	public Producer<ReadStream> requestReadStream(URI uri) {
		if (uri == null) throw new IllegalArgumentException("null uri");
		logger.debug().message("request to read stream").filePath(uri).log();
		String scheme = uri.getScheme();
		// uri has no scheme
		if (scheme == null) throw new PrivilegeException("no scheme");
		switch (scheme.toLowerCase()) {
		case INTERNAL_SCHEME:
			String suppliedAuthority = uri.getAuthority();
			// uri specifies no authority
			if (suppliedAuthority == null) throw new PrivilegeException("no authority");
			suppliedAuthority = suppliedAuthority.toLowerCase();
			switch (suppliedAuthority) {
			case RUNTIME_AUTHORITY:
				// no env resources at present
				return () -> { throw new ResourceException("not environment resources available"); };
			case APP_AUTHORITY:
				String path = uri.getPath();
				// there must be a path
				if (path == null || path.isEmpty() || path.charAt(0) != '/') throw new ResourceException("missing or invalid path");
				Bundle bundle = null;
				if (path.startsWith(internalPrefix)) {
					bundle = appInstance.bundle;
				} else if (appInstance.privileges.readApplications) {
					int i = path.indexOf('/', 1);
					if (i != -1) {
						String appInstId = path.substring(1, i);
						int j = path.indexOf('/', i + 1);
						if (j != -1) {
							String items = path.substring(i + 1, j);
							if (items.equals("external"))  {
								bundle = directory.envForInstanceId(appInstId).appInstance.bundle;
							}
						}
					}
				}
				if (bundle == null) throw new ResourceException("access denied");
				path = path.substring(internalPrefix.length());
				BundleFile file = bundle.files().file(path);
				return () -> {
					try {
						return file.openAsReadStream();
					} catch (IOException e) {
						logger.warning().message("failed to open resource").filePath(uri).stacktrace(e).log();
						//TODO need to be careful that these exceptions don't leak data
						throw new ResourceException("failed to open resource", e);
					}
				};
				default:
					// unsupported authority
					throw new IllegalArgumentException();
			}
		default:
			throw new IllegalArgumentException(); // unsupported scheme
		}
	}

	public Settings settings() {
		return new PrivilegedSettings(runtime.appInstalls.settingsFor(appInstance.identity), appInstance.privileges);
	}

	public Micro micro() {
		return micro;
	}

	public Optional<DataContext> dataContext() {
		appInstance.privileges.check(Privileges.OPEN_DB_CONNECTION);
		if (face.dbConnector == null) return Optional.empty();
		if (dataContext == null) {
			dataContext = face.dbConnector.apply(appInstance.identity);
		}
		return Optional.of(dataContext);
	}

	//TODO move onto a ScreenControl class?
	//TODO rename to constrast?
	public void screenBright(boolean bright) {
		device.getScreen().ifPresent(s -> s.contrast(bright ? 255 : 0));
	}

	public boolean screenBright() {
		Optional<Screen> screen = device.getScreen();
		if (!screen.isPresent()) return false;
		return screen.get().contrast() < 0x80 ? false : true;
	}

	public Optional<Wifi> wifi() {
		return device.getWifi();
	}

	public boolean startShutdown() {
		appInstance.privileges.check(Privileges.START_SHUTDOWN);
		return runtime.startShutdown();
	}

	Clock minuteClock() {
		if (minuteClock == null) {
			minuteClock = Clock.tick(clock(), minute);
		}
		return minuteClock;
	}

	Identity parseIdentity(String id) {
		String name;
		Namespace ns;
		int i = id.indexOf(':');
		AppData appData = appInstance.bundle.appData();
		if (i == -1) {
			ns = appData.namespace();
			name = id;
		} else {
			ns = appData.namespacesByPrefix().get( id.substring(0, i) );
			if (ns == null) throw new IllegalArgumentException("unregistered prefix");
			name = id.substring(i + 1);
		}
		try {
			return new Identity(ns, name);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid activity id", e);
		}
	}

	Optional<AppInstance> appInstanceForIdentity(Identity appId) {
		Optional<Environment> env = directory.envForIdentity(appId);
		if (!env.isPresent()) {
			logger.info().message("no application with identity {} for instance {}").values(appId, appInstance.instanceId).log();
		}
		return env.map(e -> e.appInstance);
	}

	Optional<ActivityInstance> instantiateActivity(AppInstance appInstance, String actId) {
		return appInstance.instantiateActivity(actId, logger);
	}

	Optional<ActivityInstance> instantiateActivity(DeferredActivity deferred) {
		ActivityInstance activity = Activities.instantiatedStandardActivity(deferred.activityId);
		if (activity == null && deferred.appIdentity != null) {
			return appInstanceForIdentity(deferred.appIdentity)
					.flatMap(inst -> instantiateActivity(inst, deferred.activityIdentity.name));
		}
		return Optional.ofNullable(activity);
	}

	Optional<ActivityLaunch> createActivityLaunch(DeferredActivity deferred, ActivityInstance activityInstance) {
		Optional<AppInstance> appInst = Optional.empty();
		boolean relaunch = activityInstance != null;
		// try to create an activity if one is not being reused
		if (!relaunch) {
			activityInstance = Activities.instantiatedStandardActivity(deferred.activityId);
			if (activityInstance == null && deferred.appIdentity != null) {
				appInst = appInstanceForIdentity(deferred.appIdentity);
				activityInstance = appInst.flatMap(inst -> instantiateActivity(inst, deferred.activityIdentity.name)).orElse(null);
			}
		}
		// if we fail to obtain the activity, then fail
		// (we don't communicate this to the app, because we don't want to leak info)
		if (activityInstance == null) {
			//TODO should log
			return Optional.empty(); //TODO should notify?
		}
		// fallback to the launching app instance if not explicit
		return Optional.of( new ActivityLaunch(appInst.orElse(appInstance), activityInstance, relaunch, deferred) );
	}

}
