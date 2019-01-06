package com.superdashi.gosper.micro;

class BadApplication implements Application {

	private final String instanceId;
	private final String reason;
	private Environment env;

	BadApplication(String instanceId, String reason) {
		this.instanceId = instanceId;
		this.reason = reason;
	}

	@Override
	public void init(Environment environment) {
		this.env = environment;
	}

	@Override
	public Activity createActivity(String activityId) {
		switch (activityId) {
		case "activity_launch" : return new BadAppActivity(instanceId, reason);
		case "activity_details" : return new BadAppDetailsActivity(instanceId, reason);
		default: return null;
		}
	}

	@Override
	public void destroy() {
		env = null;
	}

}
