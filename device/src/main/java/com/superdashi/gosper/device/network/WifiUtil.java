package com.superdashi.gosper.device.network;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class WifiUtil {

	private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	public static final Pattern VALID_WPA_PASSWORD = Pattern.compile("^\\p{ASCII}{3,63}$");
	public static final Pattern VALID_SSID = Pattern.compile("^[^!#;+\\]/\"\\t\\s]|([^!#;+\\]/\"\\t]([^+\\]/\"\\t]{0,30}[^+\\]/\"\\t\\s]))$");

	//TODO doesn't belong here
	public static String toHex(byte bytes[]) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);

		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xff;
			sb.append(HEX_CHARS[v >> 4]);
			sb.append(HEX_CHARS[v & 15]);
		}

		return sb.toString();
	}

	public static boolean isValidSSID(String ssid) {
		return ssid != null && !ssid.isEmpty() && VALID_SSID.matcher(ssid).matches();

	}

	public static boolean isValidWpaPassphrase(String passphrase) {
		if (passphrase == null) throw new IllegalArgumentException("null passphrase");
		return VALID_WPA_PASSWORD.matcher(passphrase).matches();
	}

	public static String wpaKey(String passphrase, String ssid) {
		byte[] bytes = pbkdf2(passphrase.toCharArray(), ssid.getBytes(), 4096, 32);
		return toHex(bytes);
	}

	private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			return skf.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("unable to perform pbkdf2", e);
		}
	}

}
