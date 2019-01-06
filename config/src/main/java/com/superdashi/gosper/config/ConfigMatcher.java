package com.superdashi.gosper.config;

@FunctionalInterface
public interface ConfigMatcher {

	boolean matches(String value);

}
