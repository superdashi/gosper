package com.superdashi.gosper.micro;

import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.logging.Logger;

interface AppHandler {

	boolean handles(Class<?> appClass);

	AppInstance instantiate(Class<?> appClass, Bundle bundle, Logger logger);

	default ActionHandler defaultActionHandler(Activity activity) { return null; }

}
