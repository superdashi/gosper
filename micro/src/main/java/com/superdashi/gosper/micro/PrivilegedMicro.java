/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
