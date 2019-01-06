package com.superdashi.gosper.micro;

//NOTE this application is created reflectively
//TODO can we hide this API-wise
public class RootApplication implements Application {

	@Override
	public Activity createActivity(String activityId) {
		return ss -> {
			ActivityContext.current().configureDisplay().hasTopBar(false).layoutDisplay();
		};
	}

}
