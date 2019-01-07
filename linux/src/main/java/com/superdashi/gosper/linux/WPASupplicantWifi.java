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
package com.superdashi.gosper.linux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.device.network.WifiAccessPoint;
import com.superdashi.gosper.device.network.WifiEntry;
import com.superdashi.gosper.device.network.WifiProtocol;
import com.superdashi.gosper.device.network.WifiStatus;
import com.superdashi.gosper.device.network.WifiUtil;
import com.superdashi.gosper.logging.Logger;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;

public class WPASupplicantWifi implements Wifi {

	// statics

	private static final String ID_PREFIX = "gosper_";
	private static final String PASSPHRASE_PREFIX = "#passphrase ";
	private static final Path defaultSupplicantPath = Paths.get("/etc/wpa_supplicant/wpa_supplicant.conf");
	private static final Path defaultSupplicantBackupPath = deriveBackupPath(defaultSupplicantPath);

	private static final Pattern CELL         = Pattern.compile( "\\s*Cell \\d\\d - (.*)"       );
	private static final Pattern KEY_VALUE    = Pattern.compile( "\\s*(\\S[^:]+):\\s*(.*)"      );
	private static final Pattern QUALITY      = Pattern.compile( "\\s*Quality=(\\d+)/(\\d+) .*" );

	private static final Pattern NETWORK_LINE = Pattern.compile( "^\\s*network\\s*=\\s*\\{\\s*"  );
	private static final Pattern VALUE_LINE   = Pattern.compile( "^\\s*([^ =]+)\\s*=(.*)"        );
	private static final Pattern PREFIX       = Pattern.compile( "^\\s*"                         );

	private static final Pattern KV_PAIR      = Pattern.compile( "^([^=]+)=([^=]+)$" );

	private static final Hasher<String> ssidHasher = Hashing.murmur3Int().hasher((s,w) -> w.writeChars(s));

	private static String iface = null;

	private static Path deriveBackupPath(Path path) {
		return path.resolveSibling(path.getFileName() + ".backup");
	}
	private static String computeId(WifiEntry entry) {
		return ID_PREFIX + WifiUtil.toHex( ssidHasher.hash(entry.ssid()).bytesValue() );
	}

	private static final String iface() throws IOException {
		if (iface == null) {
			iface = "";
			Path path = Paths.get("/proc/net/wireless");
			if (Files.exists(path)) {
				List<String> lines = Files.readAllLines( path );
				if (lines.size() >= 3) {
					String line = lines.get(2);
					int i = line.indexOf(':');
					if (i >= 0) {
						iface = line.substring(0, i).trim();
					}
				}
			}
		}
		return iface;
	}

	private static List<String> entryToLines(WifiEntry entry) {
		List<String> lines = new ArrayList<>();
		lines.add("network={");
		lines.add("\tid_str=\"" + computeId(entry) + "\"");
		lines.add("\tssid=" + LinuxUtil.quote(entry.ssid()));
		entry.passphrase().ifPresent(p -> lines.add("\t" + PASSPHRASE_PREFIX + p));
		switch (entry.protocol()) {
		case WPA_PSK:
			lines.add("\tproto=WPA");
			lines.add("\tkey_mgmt=WPA-PSK");
			entry.wpaKey().ifPresent(k -> lines.add("\tpsk=" + k));
			break;
		case WPA2_PSK:
			lines.add("\tproto=WPA2");
			lines.add("\tkey_mgmt=WPA-PSK");
			entry.wpaKey().ifPresent(k -> lines.add("\tpsk=" + k));
			break;
			default: throw new UnsupportedOperationException("Not currently supported: " + entry.protocol());
		}
		if (entry.disabled()) lines.add("\tdisabled=1");
		if (entry.priority() > 0) lines.add("\tpriority=" + entry.priority());
		lines.add("}");
		return lines;
	}

	public static boolean hasWifi() throws IOException {
		return iface() != null;
	}

	// fields

	private final Path supplicantPath;
	private final Path supplicantBackupPath;
	private final Logger logger;

	private long previousScanTime = -1L; // -1 indicates never before scanned
	private final List<WifiAccessPoint> previousScan = new ArrayList<>();

	// constructors

	public WPASupplicantWifi(Logger logger) {
		this(defaultSupplicantPath, defaultSupplicantBackupPath, logger);
	}

	public WPASupplicantWifi(Path supplicantPath, Logger logger) {
		if (logger == null) throw new IllegalArgumentException("null logger");
		if (supplicantPath == null) throw new IllegalArgumentException("null supplicantPath");
		this.logger = logger;
		this.supplicantPath = supplicantPath;
		this.supplicantBackupPath = deriveBackupPath(supplicantPath);
	}

	public WPASupplicantWifi(Path supplicantPath, Path supplicantBackupPath, Logger logger) {
		if (logger == null) throw new IllegalArgumentException("null logger");
		if (supplicantPath == null) throw new IllegalArgumentException("null supplicantPath");
		if (supplicantBackupPath == null) throw new IllegalArgumentException("null supplicantBackupPath");
		this.logger = logger;
		this.supplicantPath = supplicantPath;
		this.supplicantBackupPath = supplicantBackupPath;
	}

	// wifi methods

	//TODO needs error handling
	@Override
	public List<WifiAccessPoint> scan(long newerThan) throws IOException {
		if (newerThan < 0L) throw new IllegalArgumentException("negative newerThan");
		// use previous scan if good enough
		synchronized (previousScan) {
			if (newerThan <= previousScanTime) {
				logger.debug().message("using previous scan from time {}").values(previousScanTime).log();
				return Collections.unmodifiableList( new ArrayList<>(previousScan) );
			}
		}
		// conduct a fresh scan
		List<WifiAccessPoint> result = scanImpl();
		long now = System.currentTimeMillis();
		synchronized (previousScan) {
			// concurrent scans could have occurred, so keep only the latest
			if (now > previousScanTime) {
				logger.debug().message("caching scan result at time {}").values(now).log();
				previousScan.clear();
				previousScan.addAll(result);
				previousScanTime = now;
			}
		}
		return result;
	}

	@Override
	public List<WifiEntry> entries() throws IOException {
		//TODO should be configurable
		if (!Files.isRegularFile(supplicantPath)) {
			logger.debug().message("supplicant file not available").filePath(supplicantPath.toString()).log();
			return Collections.emptyList();
		}
		logger.debug().message("processing Wi-Fi entries from {}").values(supplicantPath).log();
		return entriesFromLines(Files.readAllLines(supplicantPath), false).stream().map(e -> e.wifi.immutableView()).filter(w -> w.isValid()).collect(Collectors.toList());
	}

	@Override
	public void updateEntry(WifiEntry entry) throws IOException {
		if (entry == null) throw new IllegalArgumentException("null entry");
		if (!entry.isValid()) throw new IllegalArgumentException("invalid entry");
		checkSupplicantsWritable();

		logger.info().message("adding wifi entry {}").values(entry).log();
		String ssid = entry.ssid();
		processEntries(e -> e.wifi.ssid().equals(ssid), true, (lines, entries, opt) -> {
			// convert the entry into lines
			List<String> entryLines = entryToLines(entry);
			if (opt.isPresent()) {
				// insert the network block
				SuppEntry se = opt.get();
				logger.info().message("replacing matching entry for SSID {}").values(ssid).filePath(supplicantPath).lineNumber(se.startLine + 1).log();
				List<String> sublist = lines.subList(se.startLine, se.endLine);
				sublist.clear();
				sublist.addAll(entryLines);
			} else {
				logger.info().message("appending new entry for SSID {}").values(ssid).filePath(supplicantPath).log();
				// append the network block
				lines.add("");
				lines.addAll(entryLines);
			}
			return true;
		});
	}

	@Override
	public boolean removeEntry(WifiEntry entry) throws IOException {
		if (entry == null) throw new IllegalArgumentException("null entry");
		checkSupplicantsWritable();

		logger.info().message("removing wifi entry {}").values(entry).log();
		String ssid = entry.ssid();
		WifiProtocol protocol = entry.protocol(); // note we test on protocol too, in case there are multiples with same SSID
		return processEntries(e -> e.wifi.ssid().equals(ssid) && e.wifi.protocol().equals(protocol), true, (lines, entries, opt) -> {
			if (!opt.isPresent()) {
				logger.info().message("cannot remove, found no wifi entry with SSID {} and protocol {}").values(ssid, protocol).log();
				return false;
			}
			SuppEntry match = opt.get();
			lines.subList(match.startLine, match.endLine).clear();
			return true;
		});
	}

	@Override
	public boolean enableOnly(WifiEntry entry) throws IOException {
		if (entry == null) throw new IllegalArgumentException("null entry");
		checkSupplicantsWritable();

		logger.info().message("enabling only wifi entry {}").values(entry).log();
		String ssid = entry.ssid();
		WifiProtocol protocol = entry.protocol(); // note we test on protocol too, in case there are multiples with same SSID
		return processEntries(e -> e.wifi.ssid().equals(ssid) && e.wifi.protocol().equals(protocol), false, (lines, entries, opt) -> {
			if (!opt.isPresent()) {
				logger.info().message("cannot enable, found no wifi entry with SSID {} and protocol {}").values(ssid, protocol).log();
				return false;
			}
			SuppEntry match = opt.get();
			int linesAdded = 0;
			for (SuppEntry existing : entries) {
				boolean enabled = existing == match;
				String disabledValue = enabled ? "0" : "1";
				boolean disabledSet = false;
				String prefix = null;
				// iterate over all lines except first and last
				// we have to adjust for  line number changes from previous additions
				int from = existing.startLine + linesAdded + 1;
				int to = existing.endLine + linesAdded - 1;
				for (int i = from; i < to; i++) {
					String line = lines.get(i);
					Matcher matcher = VALUE_LINE.matcher(line);
					if (!matcher.matches()) continue; // ignore - not a property line
					if (prefix == null) { // use this as an opportunity to be nice and match the indentation of the existing file
						Matcher pm = PREFIX.matcher(line);
						if (pm.lookingAt()) prefix = pm.group(); // should always match
					}
					if (!matcher.group(1).equals("disabled")) continue; // ignore - not a disabled flag line
					if (disabledSet) { // we have two disabled lines! - just comment out the second
						line = "#" + line;
						lines.set(i, line);
						continue;
					}
					// trim the value off the line (matcher matches to line-end)
					line = line.substring(0, line.length() - matcher.group(2).length());
					// append the new value onto the line
					line += disabledValue;
					// replace the existing line
					lines.set(i, line);
					// and record that the new line has been added
					disabledSet = true;
				}
				if (!disabledSet) { // have to insert a new disabled flag
					// default to a single tab if no prefix was identified
					if (prefix == null) prefix = "\t";
					// create a new line
					String line = prefix + "disabled=" + disabledValue;
					// insert the new line
					lines.subList(from, to).add(line);
					linesAdded ++;
					disabledSet = true;
				}
			}
			return true;
		});
	}

	@Override
	public boolean reconfigure() throws IOException {
		String iface = iface();
		if (iface == null) {
			logger.debug().message("no wireless interface to reconfigure").log();
			return false; // cannot reconfigure without an interface
		}

		logger.debug().message("reconfiguring wireless interface {}").values(iface).log();
		ProcessBuilder pb = new ProcessBuilder("wpa_cli", "reconfigure", "-i", iface);
		Process proc = pb.start();
		String response = LinuxUtil.readFully(proc);
		if (response.startsWith("OK")) return true;
		logger.warning().message("wireless reconfiguration reported: {}").message(response).log();
		if (response.startsWith("FAIL")) return false;
		throw new IOException("Unexpected response from wireless reconfiguration");
	}

	@Override
	public WifiStatus status() throws IOException {
		String iface = iface();
		if (iface == null) {
			logger.debug().message("no wireless interface found").log();
			return WifiStatus.disconnected();
		}

		logger.debug().message("probing status on interface {}").values(iface).log();
		ProcessBuilder pb = new ProcessBuilder("wpa_cli", "status", "-i", iface);
		Process proc = pb.start();
		WifiStatus.State state = null;
		String ssid = null;
		WifiProtocol protocol = null;
		boolean encrypted = false;
		String ipAddress = null;
		List<String> lines = LinuxUtil.readFullyAsLines(proc);
		for (String line : lines) {
			Matcher matcher = KV_PAIR.matcher(line);
			if (!matcher.matches()) continue;
			String key = matcher.group(1);
			String value = matcher.group(2);
			switch (key) {
			case "wpa_state":
				state = WifiStatus.State.valueOf(value);
				break;
			case "ssid":
				ssid = value;
				break;
			case "key_mgmt":
				//TODO should properly deal with this
				switch (value) {
				case "WPA2-PSK" : protocol = WifiProtocol.WPA2_PSK; encrypted = true; break;
				case "WPA-PSK" : protocol = WifiProtocol.WPA_PSK; encrypted = true; break;
				case "WEP" : protocol = WifiProtocol.WEP; encrypted = true; break; //TODO confirm this
				case "WPA-NONE" : protocol = WifiProtocol.WPA_PSK; encrypted = false; break;
				case "WPA2-NONE" : protocol = WifiProtocol.WPA2_PSK; encrypted = false; break;
				}
				break;
			case "ip_address":
				ipAddress = value;
				break;
			default: /* ignore this key */
			}
		}
		WifiStatus status;
		try {
			if (state == null) state = WifiStatus.State.DISCONNECTED;
			WifiAccessPoint accessPoint = ssid == null || protocol == null ? null : WifiAccessPoint.create(ssid, protocol, encrypted);
			status = WifiStatus.create(state, accessPoint, ipAddress);
		} catch (IllegalArgumentException e) {
			logger.warning().message("status probe failed: {}").values(e.getMessage()).log();
			if (logger.isDebugLogged()) {
				for (String line : lines) {
					logger.debug().message(line).log();
				}
			}
			throw new IOException("unexpected value set returned", e);
		}
		logger.debug().message("probe returned status {}").values(status).log();
		return status;
	}

	// private helper methods

	private List<SuppEntry> entriesFromLines(List<String> lines, boolean gosperOnly) {
		List<SuppEntry> entries = new ArrayList<>();
		int count = lines.size();
		SuppEntry entry = null;
		for (int i = 0; i < count; i++) {
			String line = lines.get(i);
			if (NETWORK_LINE.matcher(line).matches()) {
				entry = new SuppEntry();
				entry.startLine = i;
				continue;
			}
			if (entry == null) {
				continue;
			}
			Matcher matcher = VALUE_LINE.matcher(line);
			if (matcher.matches()) {
				String key = matcher.group(1);
				String value = matcher.group(2).trim();
				try {
					switch (key) {
					case "id_str" :
						String id = LinuxUtil.unquote(value);
						entry.gosper = id.startsWith(ID_PREFIX);
						continue;
					case "ssid":
						if (LinuxUtil.isQuoted(value)) {
							entry.wifi.ssid(LinuxUtil.unquote(value));
						} else {
							//TODO need to support hex encoded
							throw new UnsupportedOperationException("TODO");
						}
						continue;
					case "proto":
						if (value.contains("WPA2") || value.contains("RSN")) {
							entry.wifi.protocol(WifiProtocol.WPA2_PSK);
						} else if (value.contains("WPA")) {
							entry.wifi.protocol(WifiProtocol.WPA_PSK);
						}
						continue;
					case "key_mgmt":
						String[] values = value.split("\\s+");
						//TODO should properly deal with this
						continue;
					case "psk":
						String psk = LinuxUtil.unquote(value);
						if (WifiUtil.isValidWpaPassphrase(psk)) {
							entry.wifi.passphrase(psk);
						} else {
							entry.wifi.wpaKey(value);
						}
						continue;
					case "wep_key0":
					case "wep_key1":
					case "wep_key2":
					case "wep_key3":
						//TODO should deal with these
						continue;
					}
				} catch (IllegalArgumentException e) {
					//TODO is this safe to log?
					logger.error().message("invalid entry data").stacktrace(e).log();
					continue;
				}
			} else {
				line = line.trim();
				if (line.equals("}")) {
					entry.endLine = i + 1;
					if (!gosperOnly || entry.gosper) {
						WifiEntry wifi = entry.wifi;
						if (wifi.protocol() == null) {
							wifi.protocol(WifiProtocol.WPA_PSK); // matches default of supplicant conf file
						}
						if (!wifi.wpaKey().isPresent() && wifi.passphrase().isPresent() && wifi.ssid() != null) { // we need to compute the hex
							wifi.wpaKey(WifiUtil.wpaKey(wifi.passphrase().get(), wifi.ssid()));
						}
						entries.add(entry);
					}
					entry = null;
					continue;
				} else if (line.startsWith(PASSPHRASE_PREFIX)) {
					// special case - we note the original passphrase in a comment in the entry
					entry.wifi.passphrase(line.substring(12));
				}
			}
		}
		if (logger.isDebugLogged()) {
			logger.debug().message("identified {} existing network entries (gosper-only? {})").filePath(supplicantPath.toString()).values(entries.size(), gosperOnly).log();
			for (SuppEntry e : entries) {
				logger.debug().message("existing supplicant entry {}").values(e.wifi).filePath(supplicantPath.toString()).lineNumber(e.startLine + 1).log();
			}
		}
		return entries;
	}

	private List<WifiAccessPoint> scanImpl() throws IOException {
		logger.info().message("initiating Wi-Fi scan").log();

		String iface = iface();
		if (iface == null) {
			logger.debug().message("no wireless interface found").log();
			return Collections.emptyList();
		}

		logger.debug().message("scanning on interface {}").values(iface).log();
		ProcessBuilder pb = new ProcessBuilder("iwlist", iface, "scan");
		Process proc = pb.start();
		String out = LinuxUtil.readFully(proc);
		String[] lines = out.split("\n"); // fastpath split
		if (lines.length <= 1) return Collections.emptyList();
		return parseIwlistLines(lines);
	}

	// non-private for testing
	List<WifiAccessPoint> parseIwlistLines(String[] lines) {

		List<WifiAccessPoint> list = new ArrayList<>();

		boolean firstItem = true;
		String ssid = null;
		String protocolName = "";
		boolean encrypted = false;
		float quality = Float.NaN;
		String macAddress = null;
		int channel = -1;

		for (int lineno = 1; lineno < lines.length + 1; lineno++) {
			String line = lineno == lines.length ? "Cell 99 - DUMMY" : lines[lineno];
			//TODO would be safer to count indents
			Matcher matcherC = CELL.matcher(line);
			if (matcherC.matches()) {
				// indication of first line
				if (firstItem) {
					// no previous item to add
					firstItem = false;
				} else {
					// confirm protocol
					boolean dumpLines = false;
					WifiProtocol protocol;
					if (protocolName.isEmpty()) {
						logger.debug().message("ignoring access point with missing/unparsed protocol SSID: {}").values(ssid).log();
						dumpLines = true;
						protocol = null;
					} else {
						try {
							protocol = protocolName.isEmpty() ? null : WifiProtocol.valueOf(protocolName);
						} catch (IllegalArgumentException e) {
							logger.debug().message("ignoring access point with unrecognized/misparsed protocol SSID: {}, protocol name: {}").values(ssid, protocolName).log();
							dumpLines = true;
							protocol = null;
						}
					}
					// record existing data
					if (protocol != null) try {
						list.add(WifiAccessPoint.create(ssid, protocol, encrypted, quality, macAddress, channel));
					} catch (IllegalArgumentException e) {
						logger.error().message("invalid access point data").stacktrace(e).log();
						dumpLines = true;
					}
					// dump lines to log if requested
					//TODO find nicer way of logging
					if (dumpLines && logger.isDebugLogged()) {
						Arrays.stream(lines).forEach(l -> logger.debug().message(l).log());
					}
					//reset fields
					ssid = null;
					protocolName = "";
					encrypted = false;
					quality = Float.NaN;
					macAddress = null;
					channel = -1;
				}
				line = matcherC.group(1);
			}
			Matcher matcherKV = KEY_VALUE.matcher(line);
			if (matcherKV.matches()) {
				// key value pair
				String key = matcherKV.group(1);
				String value = matcherKV.group(2);
				switch (key) {
				case "Address" :
					macAddress = value;
					break;
				case "Channel" : try {
					channel = Integer.parseInt(value);
					} catch (NumberFormatException e) {
						logger.warning().message("non numerical channel: {}").values(e.getMessage()).log();
					}
				break;
				case "Encryption key":
					encrypted = value.equals("on");
					break;
				case "ESSID":
					//TODO could be hex sequence?
					if (LinuxUtil.isQuoted(value)) {
						ssid = LinuxUtil.unescape(value.substring(1, value.length() - 1));
					} else {
						throw new UnsupportedOperationException("TODO");
					}
					break;
					default:
						// hacky extraction of protocol properties
						if (value.contains("WPA2")) {
							protocolName = "WPA2" + protocolName;
						} else if (value.contains("WPA")) {
							protocolName = "WPA" + protocolName;
						} else if (key.startsWith("Authentication Suites") && protocolName.indexOf('_') == -1) {
							protocolName = protocolName + '_' + value;
						}
				}
			} else {
				Matcher matcherQ = QUALITY.matcher(line);
				if (matcherQ.matches()) {
					int num = Integer.parseInt(matcherQ.group(1));
					int den = Integer.parseInt(matcherQ.group(2));
					if (den != 0) quality = (float) num / den;
				}
			}
		}
		if (logger.isDebugLogged()) {
			logger.debug().message("scan found {} access points").values(list.size()).log();
			for (WifiAccessPoint ap : list) {
				logger.debug().message("scan found {}").values(ap).log();
			}
		}
		return Collections.unmodifiableList(list);
	}

	private void writeSupplicantFile(List<String> lines) throws IOException {
		// make a back-up copy
		boolean deleted = Files.deleteIfExists(supplicantBackupPath);
		if (deleted) logger.debug().message("backup supplicant file deleted").filePath(supplicantBackupPath).log();
		Files.copy(supplicantPath, supplicantBackupPath);
		logger.debug().message("old supplicant file moved").filePath(supplicantBackupPath).log();
		// write the lines back out
		Files.write(supplicantPath, lines);
		logger.info().message("wrote new supplicant file").filePath(supplicantPath).log();
	}

	private boolean processEntries(Predicate<SuppEntry> filter, boolean gosperOnly, EntryProcessor processor) throws IOException {
		// read the file
		List<String> lines = new ArrayList<>(Files.readAllLines(supplicantPath));
		// identify the existing entries
		List<SuppEntry> entries = entriesFromLines(lines, gosperOnly);
		// check if an existing entry matches
		Optional<SuppEntry> opt = entries.stream().filter(filter).findAny();
		// call processor to change the lines
		boolean result = processor.process(lines, entries, opt);
		// check if the change should be written
		if (!result) {
			logger.debug().message("no changes made to supplicants file").filePath(supplicantPath).log();
			return false;
		}
		// log if necessary
		if (logger.isDebugLogged()) {
			logger.debug().message("entry merged into supplicants file").filePath(supplicantPath).log();
			lines.forEach(l -> logger.debug().message(l).log());
		}
		writeSupplicantFile(lines);
		return true;
	}

	private void checkSupplicantsWritable() throws IOException {
		if (!Files.isWritable(supplicantPath)) throw new IOException("cannot write " + supplicantPath);
	}

	// inner classes

	private static class SuppEntry {
		int startLine;
		int endLine; // index + 1
		boolean gosper;
		WifiEntry wifi = WifiEntry.blank().known(true); // any entry in the supplicant file is, by definition, known
	}

	@FunctionalInterface
	private interface EntryProcessor {

		boolean process(List<String> lines, List<SuppEntry> entries, Optional<SuppEntry> opt);

	}
}
