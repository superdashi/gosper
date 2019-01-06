package com.superdashi.gosper.micro;

import java.io.IOException;
import java.io.Reader;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.bundle.BundleFile;
import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.logging.Logger;

//TODO shouldn't rely on exceptions to detect missing methods
public class JSApplication implements Application {

	private final ScriptEngine engine;
	private final CompiledScript script;
	private Logger logger;
	private Object jsApp;

	public JSApplication(Bundle bundle) throws ScriptException, IOException {
		// check for script
		//TODO make file path configurable in BundleSettings?
		String filePath = "src/application.js";
		BundleFile file = bundle.files().file(filePath);
		if (file == null) throw new IOException("missing source file: " + filePath);

		// compile script
		engine = JS.newEngine();
		try (Reader reader = file.openAsReader()) {
			script = ((Compilable) engine).compile(reader);
		} catch (IOException e) {
			Debug.logging().message("failed to read JS source file").filePath(filePath).stacktrace(e).log();
			throw e;
		}

		// evaluate script
		script.eval();

		// check if constructor exists
		String constructor = bundle.appData().appDetails().type().identity.name;
		Object cons = engine.get(constructor);
		if (cons == null) throw new ScriptException("no function to construct application: " + constructor);

		// construct app instance
		//TODO no apparent way to invoke cons as a constructor via script api
		//TODO security risk here
		jsApp = engine.eval("new " + constructor + "()");
	}

	@Override
	public void init(Environment environment) {
		logger = environment.logger();
		invoke("init", environment);
	}

	@Override
	public Activity createActivity(String activityId) {
		Object jsActivity = invoke("createActivity", activityId);
		if (jsActivity == null) return null;
		Activity activity = ((Invocable) engine).getInterface(jsActivity, Activity.class);
		if (activity == null) logger.error().message("returned activity missing required methods").log();
		return new JSActivity(jsActivity, activity);
	}

	@Override
	public void destroy() {
		invoke("destroy");
		logger = null;
	}

	private Object invoke(String methodName, Object... args) {
		try {
			return ((Invocable) engine).invokeMethod(jsApp, methodName, args);
		} catch (NoSuchMethodException e) { // not an error
			logger.debug().message("no init method defined").log();
			return null;
		} catch (ScriptException e) {
			logger.error().message("an exception occurred during {}: {}").values(methodName, e.getMessage()).lineNumber(e.getLineNumber()).filePath(e.getFileName()).stacktrace(e.getCause()).log();
			return null;
		}
	}

	//TODO rubbish that we have to wrap the activity just to have access to the underlying JS object
	final class JSActivity implements Activity {

		final Object jsActivity;
		private final Activity activity;

		JSActivity(Object jsActivity, Activity activity) {
			this.jsActivity = jsActivity;
			this.activity = activity;
		}

		@Override
		public void init() {
			activity.init();
		}

		@Override
		public void open(DataInput savedState) {
			activity.open(savedState);
		}

		@Override
		public void activate() {
			activity.activate();
		}

		@Override
		public void passivate() {
			activity.passivate();
		}

		@Override
		public void close(DataOutput state) {
			activity.close(state);
		}

		@Override
		public void destroy() {
			activity.destroy();
		}

		@Override
		public State relaunch(DataInput launchData) {
			return activity.relaunch(launchData);
		}

		ActionHandler actionHandler() {
			return ((Invocable) engine).getInterface(jsActivity, ActionHandler.class);
		}

	}
}


