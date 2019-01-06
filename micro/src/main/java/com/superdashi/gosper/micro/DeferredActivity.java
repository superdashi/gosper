package com.superdashi.gosper.micro;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.micro.Display.Situation;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

//TODO expose nulls optional?
//TODO needs object methods
public final class DeferredActivity implements Cloneable {

	// statics

	static final int[] NO_ANCESTOR_IDS = {};

	// supports null
	private static void writeIdentity(Identity identity, WriteStream s) {
		if (identity == null) {
			s.writeBoolean(false);
			return;
		}
		s.writeBoolean(true);
		s.writeChars(identity.ns.toString());
		s.writeChars(identity.name);
	}

	private static Identity readIdentity(ReadStream r) {
		if (!r.readBoolean()) return null;
		return new Identity(new Namespace(r.readChars()), r.readChars());
	}

	public static DeferredActivity deserialize(ReadStream s) {
		if (s == null) throw new IllegalArgumentException("null s");
		String activityId = s.readChars();
		Identity appIdentity = readIdentity(s);
		Identity activityIdentity = readIdentity(s);
		//TODO need to guard this from maliciously overlarge arrays
		int[] ids = new int[s.readInt()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = s.readInt();
		}
		DeferredActivity deferred = new DeferredActivity(activityId, appIdentity, activityIdentity, ids);
		// these values are 'unchecked' application scoped params
		deferred.requestId = s.readChars();
		if (deferred.requestId.isEmpty()) deferred.requestId = null;
		deferred.launchData = s.readBoolean() ? DataInput.deserialize(s).toOutput() : null;
		deferred.mode = ActivityMode.valueOf(s.readInt());
		return deferred;
	}

	// fields

	final String activityId;
	// these are hidden from application
	final Identity appIdentity;
	final Identity activityIdentity;
	final int[] currentAncestorIds;

	// fields mutable by applications via ActivityRequest
	String respondToComponent;
	String requestId = null;
	DataOutput launchData = null;
	ActivityMode mode = ActivityMode.SUCCEED_TOP;

	// constructors

	DeferredActivity(String activityId, Identity appIdentity, Identity activityIdentity, int[] currentAncestorIds) {
		assert activityId.startsWith("gosper_runtime") || activityIdentity != null;
		this.activityId = activityId;
		this.appIdentity = appIdentity;
		this.activityIdentity = activityIdentity;
		this.currentAncestorIds = currentAncestorIds;
	}

	// public accessors

	public String requestId() {
		return requestId;
	}

	public String activityId() {
		return activityId;
	}

	public DataOutput launchData() {
		return launchData;
	}

	public ActivityMode mode() {
		return mode;
	}

	// public methods

	public void serialize(WriteStream s) {
		if (s == null) throw new IllegalArgumentException("null s");
		s.writeChars(activityId);
		writeIdentity(appIdentity, s);
		writeIdentity(activityIdentity, s);
		s.writeInt(currentAncestorIds.length);
		for (int i = 0; i < currentAncestorIds.length; i++) {
			s.writeInt(currentAncestorIds[i]);
		}
		s.writeChars(requestId == null ? "" : requestId);
		if (launchData == null) {
			s.writeBoolean(false);
		} else {
			s.writeBoolean(true);
			launchData.toInput().serialize(s);
		}
		s.writeInt(mode.ordinal());
	}

	// object methods

	@Override
	public String toString() {
		return "activityId: " + activityId + ", appIdentity: " + appIdentity + ", activityIdentity: " + activityIdentity;
	}

	// package scoped accessors

	DeferredActivity copy() {
		try {
			return (DeferredActivity) this.clone();
		} catch (CloneNotSupportedException e) {
			// not possible
			throw new RuntimeException(e);
		}
	}

	DataOutput certainLaunchData() {
		return launchData == null ? launchData = new DataOutput() : launchData;
	}

	void respondToComponent(Situation situation) {
		respondToComponent = situation.location.name;
	}
}
