package com.superdashi.gosper.micro;

public interface Application {

	default void init(Environment environment) {}

	Activity createActivity(String activityId);

	default void destroy() {}
}
