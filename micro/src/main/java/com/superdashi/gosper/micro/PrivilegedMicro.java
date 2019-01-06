package com.superdashi.gosper.micro;

import java.util.List;

import com.superdashi.gosper.bundle.Privileges;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.item.Qualifier;

final class PrivilegedMicro implements Micro {

	private final Micro micro;
	private final Privileges privileges;

	PrivilegedMicro(Micro micro, Privileges privileges) {
		this.micro = micro;
		this.privileges = privileges;
	}

	@Override
	public List<Details> allAppDetails() {
		privileges.check(Privileges.READ_APPLICATIONS);
		return micro.allAppDetails();
	}

	@Override
	public List<MicroAppLauncher> appLaunchers(Details appDetails, Qualifier qualifier) {
		privileges.check(Privileges.LAUNCH_APPLICATIONS);
		return micro.appLaunchers(appDetails, qualifier);
	}
}
