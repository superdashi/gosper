package com.superdashi.gosper.config;

import java.util.ArrayList;
import java.util.List;

import com.tomgibara.fundament.Consumer;

public final class Configurator {

	private final Configurable rootStyleable;
	private final ConfigRules rules;

	public Configurator(Configurable rootStylable, ConfigRules rules) {
		if (rootStylable == null) throw new IllegalArgumentException("null rootStylable");
		if (rules == null) throw new IllegalArgumentException("null rules");
		this.rootStyleable = rootStylable;
		this.rules = rules;
	}

	public Configurable rootStylable() {
		return rootStyleable;
	}

	//TODO this needs replacing with something much more efficient
	public void configure() {
		// open targets
		List<Session> sessions = new ArrayList<>();
		visit(rootStyleable, s -> sessions.add(new Session(s)));
		//TODO need to apply inheritance separately
		// apply rules
		for (ConfigRule rule : rules.asList()) {
			sessions.stream().filter(s -> rule.selector().matches(s.configurable)).forEach(s -> rule.applyTo(s.target));
		}
		// complete
		sessions.forEach(s -> s.target.close());
	}

	private void visit(Configurable configurable, Consumer<Configurable> action) {
		action.consume(configurable);
		for (Configurable child : configurable.getStyleableChildren()) {
			visit(child, action);
		}
	}

	private static class Session {

		final Configurable configurable;
		final ConfigTarget target;

		public Session(Configurable configurable) {
			this.configurable = configurable;
			target = configurable.openTarget();
		}
	}
}
