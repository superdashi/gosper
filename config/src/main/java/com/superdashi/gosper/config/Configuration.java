package com.superdashi.gosper.config;

public final class Configuration {

	private final ConfigProperty property;
	private final Config config;

	Configuration(ConfigProperty property, Config config) {
		this.property = property;
		this.config = config;
	}

	public ConfigProperty getProperty() {
		return property;
	}

	public boolean isInherited() {
		return config == null;
	}

	public Config getConfig() {
		return config;
	}

	public void applyTo(ConfigTarget target) {
		if (config == null) {
			target.inheritStyling(property);
		} else {
			config.applyTo(target, property);
		}
	}
}
