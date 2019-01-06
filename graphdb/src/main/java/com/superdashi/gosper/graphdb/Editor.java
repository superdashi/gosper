package com.superdashi.gosper.graphdb;

public interface Editor extends Inspector {

	boolean isTypeAvailable(Type type);

	Edit edit();

}
