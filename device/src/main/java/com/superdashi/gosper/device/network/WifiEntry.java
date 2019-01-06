package com.superdashi.gosper.device.network;

import java.io.Serializable;
import java.util.Optional;

import com.tomgibara.fundament.Mutability;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

public class WifiEntry implements Mutability<WifiEntry>, Serializable {

	// statics

	private static final long serialVersionUID = 685156084652771873L;

	private static final String[] NO_WEP_KEYS = new String[] {};

	public static WifiEntry blank() {
		return new WifiEntry(new Fields(), true);
	}

	public static WifiEntry forAP(WifiAccessPoint ap) {
		if (ap == null) throw new IllegalArgumentException("null ap");
		Fields fields = new Fields();
		fields.ssid = ap.ssid();
		fields.protocol = ap.protocol();
		return new WifiEntry(fields, true);
	}

	public static void serialize(WifiEntry entry, WriteStream s) {
		s.writeChars(entry.ssid());
		s.writeInt(entry.protocol().ordinal());
		s.writeChars(entry.passphrase().orElse(""));
		s.writeChars(entry.wpaKey().orElse(""));
		String[] wepKeys = entry.wepKeys();
		s.writeInt(wepKeys.length);
		for (String wepKey : wepKeys) s.writeChars(wepKey);
		s.writeBoolean(entry.disabled());
		s.writeInt(entry.priority());
		s.writeBoolean(entry.known());
	}

	public static WifiEntry deserialize(ReadStream r) {
		WifiEntry entry = blank()
				.ssid(r.readChars())
				.protocol(WifiProtocol.valueOf(r.readInt()))
				.passphrase(r.readChars())
				.wpaKey(r.readChars());
		String[] wepKeys = new String[r.readInt()];
		for (int i = 0; i < wepKeys.length; i++) wepKeys[i] = r.readChars();
		return entry
			.wepKeys(wepKeys)
			.disabled(r.readBoolean())
			.priority(r.readInt())
			.known(r.readBoolean());
	}

	private static boolean isHex(String str) {
		if ((str.length() & 1) != 0) return false;
		return !str.chars().filter(c -> !(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')).findAny().isPresent();
	}
	private final Fields fields;
	private final boolean mutable;

	private WifiEntry(Fields fields, boolean mutable) {
		this.fields = fields;
		this.mutable = mutable;
	}

	// accessors

	public String ssid() { return fields.ssid; }

	public WifiProtocol protocol() { return fields.protocol; }

	public Optional<String> passphrase() { return Optional.ofNullable(fields.passphrase); }

	public Optional<String> wpaKey() { return Optional.ofNullable(fields.wpaKey); }

	public String[] wepKeys() { return fields.wepKeys == null ? NO_WEP_KEYS : fields.wepKeys.clone(); }

	public boolean disabled() { return fields.disabled; }

	public int priority() { return fields.priority; }

	public boolean known() { return fields.known; }

	public WifiEntry ssid(String ssid) {
		if (ssid == null) throw new IllegalArgumentException("null ssid");
		if (ssid.isEmpty()) throw new IllegalArgumentException("empty ssid");
		if (!WifiUtil.isValidSSID(ssid)) throw new IllegalArgumentException("invalid ssid");
		checkMutable();
		fields.ssid = ssid;
		return this;
	}

	public WifiEntry protocol(WifiProtocol protocol) {
		if (protocol == null) throw new IllegalArgumentException("null protocol");
		checkMutable();
		fields.protocol = protocol;
		return this;
	}

	public WifiEntry passphrase(String passphrase) {
		if (passphrase != null && passphrase.isEmpty()) passphrase = null;
		if (passphrase != null && !WifiUtil.isValidWpaPassphrase(passphrase)) throw new IllegalArgumentException("invalid passphrase");
		checkMutable();
		fields.passphrase = passphrase;
		return this;
	}

	public WifiEntry wpaKey(String wpaKey) {
		if (wpaKey != null && wpaKey.isEmpty()) wpaKey = null;
		if (wpaKey != null && !isHex(wpaKey)) throw new IllegalArgumentException("wpaKey not hex");
		checkMutable();
		fields.wpaKey = wpaKey == null ? null : wpaKey.toLowerCase();
		return this;
	}

	public WifiEntry wepKeys(String[] wepKeys) {
		if (wepKeys != null) {
			if (wepKeys.length == 0) {
				wepKeys = null;
			} else if (wepKeys.length > 4) {
				throw new IllegalArgumentException("too many wep keys");
			} else {
				wepKeys = wepKeys.clone();
				for (int i = 0; i < wepKeys.length; i++) {
					String wepKey = wepKeys[i];
					if (wepKey != null) {
						if (wepKey.isEmpty()) {
							wepKey = null;
						} else if (!isHex(wepKey)) {
							throw new IllegalArgumentException("wep key not hex");
						} else {
							wepKeys[i] = wepKey.toLowerCase();
						}
					}
				}
			}
		}
		checkMutable();
		fields.wepKeys = wepKeys;
		return this;
	}

	public WifiEntry disabled(boolean disabled) {
		checkMutable();
		fields.disabled = disabled;
		return this;
	}

	public WifiEntry priority(int priority) {
		if (priority < 0) throw new IllegalArgumentException("negative priority");
		checkMutable();
		fields.priority = priority;
		return this;
	}

	public WifiEntry known(boolean known) {
		checkMutable();
		fields.known = known;
		return this;
	}

	// mutability

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public WifiEntry immutableCopy() {
		return new WifiEntry(fields.clone(), false);
	}

	@Override
	public WifiEntry immutableView() {
		return new WifiEntry(fields, false);
	}

	@Override
	public WifiEntry mutableCopy() {
		return new WifiEntry(fields.clone(), true);
	}

	// object methods

	@Override
	public int hashCode() {
		return fields.hashCode();
	}

	@Override
	public String toString() {
		return fields.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof WifiEntry)) return false;
		WifiEntry that = (WifiEntry) obj;
		return this.fields.equals(that.fields);
	}

	// additional methods

	// valid if it has an ssid and a protocol
	public boolean isValid() {
		return fields.ssid != null && fields.protocol != null;
	}

	// private helper methods

	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("mutable");
	}

	// inner classes

	private static final class Fields implements Cloneable {
		private String ssid;
		// ideally, this should be a list of protocols, with separate encryption protocols
		// but that level of fidelity probably isn't needed
		private WifiProtocol protocol;
		private String passphrase;
		private String wpaKey; // as hex
		private String[] wepKeys; // as hex
		private boolean disabled;
		private int priority;
		private boolean known;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("priority: ").append(priority);
			if (ssid != null) sb.append(", ssid: ").append(ssid);
			if (protocol != null) sb.append(", protocol: ").append(protocol);
			if (passphrase != null) sb.append(", passphrase: <redacted>");
			if (wpaKey != null) sb.append(", wpaKey: ").append(wpaKey);
			if (wepKeys != null) sb.append(", wepKeys: ").append(wepKeys);
			sb.append(", disabled: ").append(disabled);
			sb.append(", known: ").append(known);
			return sb.toString();
		}

		//TODO should support value based equality & hashcode, but a low priority

		@Override
		protected Fields clone() {
			try {
				return (Fields) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e); // impossible
			}
		}

	}
}
