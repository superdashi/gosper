package com.superdashi.gosper.config;

public interface ConfigSelector {

	boolean matches(Configurable configurable);

}
