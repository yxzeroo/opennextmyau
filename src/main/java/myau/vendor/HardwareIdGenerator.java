package myau.vendor;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Restored from the native hardware fingerprint routine in {@code x64-windows.dll}.
 * Combines CPU brand/identifier, system volume serial attribute, computer name,
 * and processor vendor into an uppercase 32-character MD5 digest.
 */
public final class HardwareIdGenerator {
    private static final char[] UPPER_HEX = "0123456789ABCDEF".toCharArray();

    private HardwareIdGenerator() {
    }

    public static String computeHardwareId() {
        String cpuBrand = getenvOrDefault("PROCESSOR_IDENTIFIER", "UnknownCPU");
        String cpuVendor = getenvOrDefault("PROCESSOR_ARCHITECTURE", "AMD64");
        String computerName = resolveComputerName();
        String volumeSerial = resolveSystemVolumeSerial();

        String rawMaterial = cpuBrand + "|" + volumeSerial + "|" + computerName + "|" + cpuVendor;
        return md5UpperHex(rawMaterial.getBytes(StandardCharsets.UTF_8));
    }

    private static String resolveComputerName() {
        String envName = System.getenv("COMPUTERNAME");
        if (envName != null && !envName.isEmpty()) {
            return envName;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "UNKNOWN";
        }
    }

    private static String resolveSystemVolumeSerial() {
        try {
            FileStore store = Files.getFileStore(Paths.get("C:\\"));
            Object vsn = store.getAttribute("volume:vsn");
            if (vsn instanceof Integer) {
                return String.format(Locale.ROOT, "%08X", (Integer) vsn);
            }
            if (vsn != null) {
                return String.valueOf(vsn);
            }
        } catch (Exception ignored) {
        }
        return "00000000";
    }

    private static String getenvOrDefault(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }

    public static String md5UpperHex(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            char[] chars = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                int v = digest[i] & 0xFF;
                chars[i * 2] = UPPER_HEX[v >>> 4];
                chars[i * 2 + 1] = UPPER_HEX[v & 0x0F];
            }
            return new String(chars);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }
}
