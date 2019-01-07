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
package com.superdashi.gosper.device.network;

import java.util.Optional;

public final class WifiStatus {

	// enums

	public enum State {
		DISCONNECTED,
		SCANNING,
		ASSOCIATING,
		FW_HANDSHAKE,
		COMPLETED
	}

	// statics

	private static final WifiStatus disconnected = new WifiStatus(State.DISCONNECTED, null, null);

	public static WifiStatus disconnected() {
		return disconnected;
	}

	// note, it's possible to have no ip address, even after connection is complete
	public static WifiStatus create(State state, WifiAccessPoint accessPoint, String ipAddress) {
		if (state == null) throw new IllegalArgumentException("null state");
		if (state.ordinal() >= State.ASSOCIATING.ordinal() != (accessPoint != null)) throw new IllegalArgumentException("accessPoint (null? " + (accessPoint == null) + ") inconsistent with state " + state);
		return state == State.DISCONNECTED ? disconnected : new WifiStatus(state, accessPoint, ipAddress);
	}

	// fields

	private final State state;
	private final WifiAccessPoint accessPoint;
	private final String ipAddress; // only available if completed

	// constructors

	private WifiStatus(State state, WifiAccessPoint accessPoint, String ipAddress) {
		this.state = state;
		this.accessPoint = accessPoint;
		this.ipAddress = ipAddress;
	}

	// public accessors

	public State state() {
		return state;
	}

	public Optional<WifiAccessPoint> accessPoint() {
		return Optional.ofNullable(accessPoint);
	}

	public Optional<String> ipAddress() {
		return Optional.ofNullable(ipAddress);
	}

	// object methods

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(state);
		if (accessPoint != null) sb.append(' ').append(accessPoint);
		if (ipAddress != null) sb.append(' ').append(ipAddress);
		return sb.toString();
	}
}
