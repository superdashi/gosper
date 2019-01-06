package com.superdashi.gosper.scripting;

public interface ScriptEngine {

	void runtimeStarted();
	void runtimeStopped();

	ScriptSession openSession(String faceId);

}
