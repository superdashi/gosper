package com.superdashi.gosper.micro;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.script.ScriptException;

import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.core.CoreContext;
import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.micro.JSApplication.JSActivity;
import com.superdashi.gosper.scripting.ScriptEngine;

//TODO should constrain valid identifiers
final class Runtime {

	// supplied
	final CoreContext context;
	final Visuals visuals;
	final ScriptEngine scriptEngine;

	final AppInstalls appInstalls;
	final Interfaces interfaces;
	final Logger logger;

	// note: order significant
	//TODO make configurable?
	final AppHandler runtimeAppHandler = new RuntimeAppHandler();
	final AppHandler[] appHandlers = { runtimeAppHandler, new JSAppHandler(), new JavaAppHandler() };

	// status
	private boolean started = false;

	// assigned on startup
	private Runnable shutdown;

	// generated on start

	public Runtime(CoreContext context, AppInstalls appInstalls, Visuals visuals, ScriptEngine scriptEngine) {
		if (context == null) throw new IllegalArgumentException("null context");
		if (appInstalls == null) throw new IllegalArgumentException("null appInstalls");
		if (visuals == null) throw new IllegalArgumentException("null visuals");
		this.context = context;
		this.appInstalls = appInstalls;
		this.visuals = visuals;
		this.scriptEngine = scriptEngine;
		logger = context.loggers().loggerFor("runtime");
		Debug.logger(logger.child("debug"));
		interfaces = new Interfaces(this);
	}

	//IntExc if stopped before start finished
	// shutdown may be null
	// essentially creates an activity manager and connects it to the device
	//TODO should device be passed into this method?
	public boolean start(Runnable shutdown) throws InterruptedException {
		synchronized (appInstalls.lifetime) {
			if (started) throw new IllegalStateException("already started");
			started = true;
			this.shutdown = shutdown;
			interfaces.start();
		}
		return true;
	}

	public boolean stop() {
		synchronized (appInstalls.lifetime) {
			if (!started) throw new IllegalStateException("not started");
			started = false;
			interfaces.stop();
		}
		return true;
	}

	public void doWithLifetimeLock(Runnable runnable) {
		synchronized (runnable) {
			runnable.run();
		}
	}

	public <V> V doWithLifetimeLock(Callable<V> callable) {
		synchronized (appInstalls.lifetime) {
			try {
				return callable.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	//TODO needs better implementation
	boolean startShutdown() {
		synchronized (this) {
			if (shutdown == null) return false;
			new Thread(shutdown).start();
			shutdown = null;
			return true;
		}
	}

	// must be called with lock;
	boolean started() {
		return started;
	}

	// currently assumes trivial app is only one instantiated
	private static class RuntimeAppHandler implements AppHandler {
		@Override
		public boolean handles(Class<?> appClass) {
			return appClass == TrivialApplication.class;
		}
		@Override
		public AppInstance instantiate(Class<?> appClass, Bundle bundle, Logger logger) {
			return new AppInstance(bundle.instanceId, bundle, this, new TrivialApplication());
		}
	}

	private static class JavaAppHandler implements AppHandler {
		@Override
		public boolean handles(Class<?> appClass) {
			return true;
		}
		@Override
		public AppInstance instantiate(Class<?> appClass, Bundle bundle, Logger logger) {
			Application application;
			try {
				//note: class instance of application already checked before being stored in appData
				application = (Application) appClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				logger.error().message("failed to instantiate application instance {} of {}").values(bundle.instanceId, bundle.appData().appDetails()).stacktrace(e).filePath(bundle.files().uri()).log();
				return null;
			}
			//TODO identify default action listener
			return new AppInstance(bundle.instanceId, bundle, this, application);
		}

		@Override
		public ActionHandler defaultActionHandler(Activity activity) {
			return activity instanceof ActionHandler ? (ActionHandler) activity : null;
		}
	}

	//TODO could exclude classes in dashi packages?
	private static class JSAppHandler implements AppHandler {
		@Override
		public boolean handles(Class<?> appClass) {
			return appClass == JSApplication.class;
		}

		@Override
		public AppInstance instantiate(Class<?> appClass, Bundle bundle, Logger logger) {
			JSApplication application;
			try {
				application = new JSApplication(bundle);
			} catch (ScriptException e) {
				logger.error().message("failed to instantiate application instance {} of {}").values(bundle.instanceId, bundle.appData().appDetails()).stacktrace(e.getCause()).log();
				return null;
			} catch (IOException e) {
				logger.error().message("failed to read source file for application instance {}: {}").values(bundle.instanceId, e.getMessage()).log();
				return null;
			}
			return new AppInstance(bundle.instanceId, bundle, this, application);
		}

		@Override
		public ActionHandler defaultActionHandler(Activity activity) {
			return ((JSActivity) activity).actionHandler();
		}
	}
}
