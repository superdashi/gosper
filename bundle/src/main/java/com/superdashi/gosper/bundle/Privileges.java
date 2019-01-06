package com.superdashi.gosper.bundle;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.superdashi.gosper.util.AdaptedSet;
import com.tomgibara.bits.BitVector;
import com.tomgibara.bits.Bits;
import com.tomgibara.collect.Collect;
import com.tomgibara.fundament.Bijection;

//TODO make unavailable unavailable
public final class Privileges {

	private static final String[] names = {
			"UNAVAILABLE",
			"SYSTEM",
			"READ_SETTINGS",
			"WRITE_SETTINGS",
			"READ_APPLICATIONS",
			"LAUNCH_APPLICATIONS",
			"KNOW_DEVICE_SPEC", //TODO this is now redundant
			"READ_SCREEN_DIMENSIONS",
			"READ_SCREEN_CONTRAST",
			"WRITE_SCREEN_CONTRAST",
			"KNOW_WIFI_STATUS",
			"SCAN_WIFI_CONNECTIONS",
			"KNOW_WIFI_CONNECTIONS",
			"ADD_WIFI_CONNECTION",
			"REMOVE_WIFI_CONNECTION",
			"CHOOSE_WIFI_CONNECTION",
			"RECONFIGURE_WIFI",
			"OPEN_DB_CONNECTION",
			"START_SHUTDOWN",
	};

	private static final Set<String> nameSet = Collect.setsOf(String.class).newSet(Arrays.asList(names)).immutable();

	public static final int UNAVAILABLE             =  0;
	public static final int SYSTEM                  =  1;
	public static final int READ_SETTINGS           =  2;
	public static final int WRITE_SETTINGS          =  3;
	public static final int READ_APPLICATIONS       =  4;
	public static final int LAUNCH_APPLICATIONS     =  5;
	public static final int KNOW_DEVICE_SPEC        =  6;
	public static final int READ_SCREEN_DIMENSIONS  =  7; //TODO rename to READ_SCREEN_CHARACTERISTICS?
	public static final int READ_SCREEN_CONTRAST    =  8;
	public static final int WRITE_SCREEN_CONTRAST   =  9;
	public static final int KNOW_WIFI_STATUS        = 10;
	public static final int SCAN_WIFI_CONNECTIONS   = 11;
	public static final int KNOW_WIFI_CONNECTIONS   = 12;
	public static final int ADD_WIFI_CONNECTION     = 13;
	public static final int REMOVE_WIFI_CONNECTION  = 14;
	public static final int CHOOSE_WIFI_CONNECTION  = 15;
	public static final int RECONFIGURE_WIFI        = 16;
	public static final int OPEN_DB_CONNECTION      = 17;
	public static final int START_SHUTDOWN          = 18;

	static final int COUNT = 19;

	private static int[] basics = { KNOW_DEVICE_SPEC };

	private static Bijection<Integer, String> adapter = new Bijection<Integer, String>() {

		@Override
		public Class<String> rangeType() {
			return String.class;
		}

		@Override
		public Class<Integer> domainType() {
			return Integer.class;
		}

		@Override
		public boolean isInRange(Object obj) {
			return nameSet.contains(obj);
		}

		@Override
		public boolean isInDomain(Object obj) {
			if (!(obj instanceof Integer)) return false;
			int i = (int) obj;
			return i>= 0 && i < COUNT;
		}

		@Override
		public String apply(Integer t) {
			return names[t];
		}

		@Override
		public Integer disapply(String r) {
			for (int i = 0; i < names.length; i++) {
				if (names[i].equals(r)) return i;
			}
			throw new IllegalArgumentException();
		}

	};

	// set up a singleton privileges instances
	private static final Privileges all = new Privileges( BitVector.fromStore(Bits.oneBits(COUNT)) );
	private static final Privileges none = new Privileges( new BitVector(COUNT) );
	private static final Privileges basic;
	static {
		BitVector v = new BitVector(COUNT);
		for (int b : basics) {
			v.setBit(b, true);
		}
		basic = new Privileges(v);
	}

	static final Privileges all() { return all; }
	static final Privileges none() { return none; }
	static final Privileges basic() { return basic; }

	static Privileges fromNames(String... names) {
		if (names == null) throw new IllegalArgumentException("null names");
		return fromNames(Arrays.asList(names));
	}

	static Privileges fromNames(Collection<String> names) {
		if (names == null) throw new IllegalArgumentException("null names");
		BitVector flags = new BitVector(COUNT);
		new AdaptedSet<>( flags.ones().asSet(), adapter).addAll(names);
		return new Privileges(flags);
	}

	public final boolean system;

	public final boolean readSettings;
	public final boolean writeSettings;

	public final boolean knowDeviceSpec;

	public final boolean readApplications;
	public final boolean launchApplications;

	public final boolean readScreenDimensions;
	public final boolean readScreenContrast;
	public final boolean writeScreenContrast;

	public final boolean knowWifiStatus;
	public final boolean scanWifiConnections;
	public final boolean knowWifiConnections;
	public final boolean addWifiConnection;
	public final boolean removeWifiConnection;
	public final boolean chooseWifiConnection;
	public final boolean reconfigureWifi;

	public final boolean openDbConnection;

	public final boolean startShutdown;

	private final BitVector flags;

	private Privileges(BitVector flags) {
		if (flags.size() != COUNT) throw new IllegalArgumentException();
		system                = flags.getBit(SYSTEM                 );
		knowDeviceSpec        = flags.getBit(KNOW_DEVICE_SPEC       );
		readApplications      = flags.getBit(READ_APPLICATIONS      );
		launchApplications    = flags.getBit(LAUNCH_APPLICATIONS    );
		readSettings          = flags.getBit(READ_SETTINGS          );
		writeSettings         = flags.getBit(WRITE_SETTINGS         );
		readScreenDimensions  = flags.getBit(READ_SCREEN_DIMENSIONS );
		readScreenContrast    = flags.getBit(READ_SCREEN_CONTRAST   );
		writeScreenContrast   = flags.getBit(WRITE_SCREEN_CONTRAST  );
		knowWifiStatus        = flags.getBit(KNOW_WIFI_STATUS       );
		scanWifiConnections   = flags.getBit(SCAN_WIFI_CONNECTIONS  );
		knowWifiConnections   = flags.getBit(KNOW_WIFI_CONNECTIONS  );
		addWifiConnection     = flags.getBit(ADD_WIFI_CONNECTION    );
		removeWifiConnection  = flags.getBit(REMOVE_WIFI_CONNECTION );
		chooseWifiConnection  = flags.getBit(CHOOSE_WIFI_CONNECTION );
		reconfigureWifi       = flags.getBit(RECONFIGURE_WIFI       );
		openDbConnection      = flags.getBit(OPEN_DB_CONNECTION     );
		startShutdown         = flags.getBit(START_SHUTDOWN         );
		this.flags = flags.immutable();
	}

	public Set<String> asSet() {
		return new AdaptedSet<>(flags.ones().asSet(), adapter);
	}

	public Privileges restrict(Privileges that) {
		if (this.flags.contains().store(that.flags)) return that;
		if (that.flags.contains().store(this.flags)) return this;
		BitVector flags = this.flags.mutableCopy();
		flags.and().withStore(that.flags);
		return new Privileges(flags);
	}

	public Privileges augment(Privileges that) {
		if (this.flags.contains().store(that.flags)) return this;
		if (that.flags.contains().store(this.flags)) return that;
		BitVector flags = this.flags.mutableCopy();
		flags.or().withStore(that.flags);
		return new Privileges(flags);
	}

	public Privileges deny(Privileges that) {
		if (this.flags.excludes().store(that.flags)) return this;
		BitVector flags = this.flags.mutableCopy();
		flags.and().withStore(that.flags.flipped());
		return new Privileges(flags);
	}

	@Override
	public int hashCode() {
		return flags.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Privileges)) return false;
		Privileges that = (Privileges) obj;
		return this.flags.equals(that.flags);
	}

	@Override
	public String toString() {
		return asSet().toString();
	}

	public void check(int privilege) {
		if (!flags.getBit(privilege)) throw new PrivilegeException(names[privilege]);
	}
}
