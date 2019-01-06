package com.superdashi.gosper.logging;

public final class Loggers {

	private final LogDomain domain;

	Loggers(LogDomain domain) {
		this.domain = domain;
	}

	public Logger loggerFor(String firstName, String... moreNames) {
		return domain.loggerFor(LogIdentity.create(firstName, moreNames));
	}

	public Logger loggerFor(LogIdentity identity) {
		if (identity == null) throw new IllegalArgumentException("null identity");
		return domain.loggerFor(identity);
	}

}
