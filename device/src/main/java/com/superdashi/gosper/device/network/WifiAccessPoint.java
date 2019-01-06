package com.superdashi.gosper.device.network;

import java.io.Serializable;
import java.util.Optional;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

//TODO add cipher info
//TODO should add frequency
public final class WifiAccessPoint implements Serializable {

	private static final long serialVersionUID = 6571653050798090317L;

	public static WifiAccessPoint create(String ssid, WifiProtocol protocol) {
		return create(ssid, protocol, true);
	}

	public static WifiAccessPoint create(String ssid, WifiProtocol protocol, boolean encrypted) {
		return create(ssid, protocol, encrypted, Float.NaN);
	}

	public static WifiAccessPoint create(String ssid, WifiProtocol protocol, boolean encrypted, float quality) {
		return create(ssid, protocol, encrypted, quality, null, -1);
	}

	public static WifiAccessPoint create(String ssid, WifiProtocol protocol, boolean encrypted, float quality, String macAddress, int channel) {
		if (ssid == null) throw new IllegalArgumentException("null ssid");
		if (protocol == null) throw new IllegalArgumentException("null protocol");
		if (quality < 0) throw new IllegalArgumentException("negative quality");
		if (channel < 0) channel = -1;
		return new WifiAccessPoint(ssid, protocol, encrypted, quality, macAddress, channel);
	}

	public static WifiAccessPoint deserialize(ReadStream s) {
		if (s == null) throw new IllegalArgumentException("null s");
		String ssid           = s.readChars()  ;
		WifiProtocol protocol = WifiProtocol.valueOf(s.readByte());
		boolean encrypted     = s.readBoolean();
		float quality         = s.readFloat()  ;
		String macAddress     = s.readChars()  ;
		int channel           = s.readInt()    ;
		return create(ssid, protocol, encrypted, quality, macAddress.isEmpty() ? null : macAddress, channel);
	}

	public static void serialize(WifiAccessPoint wap, WriteStream s) {
		if (wap == null) throw new IllegalArgumentException("null wap");
		if (s == null) throw new IllegalArgumentException("null s");
		s.writeChars  (wap.ssid      );
		s.writeByte((byte) wap.protocol.ordinal());
		s.writeBoolean(wap.encrypted );
		s.writeFloat  (wap.quality   );
		s.writeChars  (wap.macAddress == null ? "" : wap.macAddress);
		s.writeInt    (wap.channel   );
	}

	//TODO create a better type for this
	private final String ssid;
	private final WifiProtocol protocol;
	private final boolean encrypted;
	private final float quality;
	private final String macAddress;
	//TODO maybe record frequency?
	private final int channel;

	private WifiAccessPoint(String ssid, WifiProtocol protocol, boolean encrypted, float quality, String macAddress, int channel) {
		if (ssid != null && !WifiUtil.isValidSSID(ssid)) throw new IllegalArgumentException("invalid ssid");
		this.ssid = ssid;
		this.protocol = protocol;
		this.encrypted = encrypted;
		this.quality = quality;
		this.macAddress = macAddress;
		this.channel = channel;
	}

	// accessors

	public String ssid() {
		return ssid;
	}

	public WifiProtocol protocol() {
		return protocol;
	}

	public boolean encrypted() {
		return encrypted;
	}

	public Optional<Float> quality() {
		return Float.isNaN(quality) ? Optional.empty() : Optional.of(quality);
	}

	public Optional<String> macAddress() {
		return Optional.ofNullable( macAddress );
	}

	public Optional<Integer> channel() {
		return channel < 0 ? Optional.empty() : Optional.of(channel);
	}

	// externalizable methods

	// object methods
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SSID: ").append(ssid).append(", protocol: ").append(protocol).append(", encrypted: ").append(encrypted);
		quality().ifPresent(q -> sb.append(", quality: ").append(q));
		macAddress().ifPresent(m -> sb.append(", mac: ").append(m));
		channel().ifPresent(c -> sb.append(", channel: ").append(c));
		return sb.toString();
	}

}
