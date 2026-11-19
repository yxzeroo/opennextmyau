package myau.vendor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Restored hardware fingerprint generator ({@code 0x1805AA010}, {@code 0x1805AA9E0},
 * and {@code 0x1805AA8D0}).
 *
 * <p>Concatenates four host attributes with no delimiters and hashes the UTF-8
 * string with MD5, returning a 32-character uppercase hexadecimal digest:
 * <ol>
 *   <li>{@code CPUID(0x80000002, 0)} {@code [EAX, EBX, ECX, EDX]} — first 16 ASCII
 *       bytes of the processor brand string unpacked as 4 little-endian signed
 *       32-bit decimal integers ({@code std::to_string((int32_t) reg)}).</li>
 *   <li>{@code GetVolumeInformationA("C:\\", ...)} volume serial number formatted
 *       as an unsigned 32-bit decimal integer ({@code std::to_string((uint32_t) serial)}).</li>
 *   <li>{@code GetComputerNameA} NetBIOS computer name (up to 15 characters).</li>
 *   <li>{@code CPUID(0, 0)} {@code [EBX, EDX, ECX]} 12-character CPU vendor string
 *       (e.g. {@code GenuineIntel} or {@code AuthenticAMD}).</li>
 * </ol>
 */
public final class HardwareIdGenerator {
    private static final String CPU_REG_KEY = "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0";
    private static final Pattern VOL_SERIAL_PATTERN = Pattern.compile("([0-9A-Fa-f]{4})-([0-9A-Fa-f]{4})");

    private HardwareIdGenerator() {
    }

    public static String generateHwid() {
        String rawMaterial = buildRawFingerprintMaterial();
        return md5UpperHex(rawMaterial);
    }

    public static String buildRawFingerprintMaterial() {
        StringBuilder sb = new StringBuilder(96);

        int[] brandRegs = queryCpuBrandRegisters();
        for (int reg : brandRegs) {
            sb.append(reg);
        }

        int volumeSerial = queryDriveCVolumeSerial();
        sb.append(Integer.toUnsignedString(volumeSerial));

        sb.append(queryComputerName());
        sb.append(queryCpuVendorString());
        return sb.toString();
    }

    private static int[] queryCpuBrandRegisters() {
        String brand = queryWindowsRegistryValue(CPU_REG_KEY, "ProcessorNameString");
        if (brand == null || brand.isEmpty()) {
            brand = System.getenv("PROCESSOR_IDENTIFIER");
        }
        byte[] raw = new byte[16];
        if (brand != null) {
            byte[] src = brand.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, raw, 0, Math.min(src.length, 16));
        }
        int[] regs = new int[4];
        for (int i = 0; i < 4; i++) {
            int base = i << 2;
            regs[i] = (raw[base] & 0xFF)
                    | ((raw[base + 1] & 0xFF) << 8)
                    | ((raw[base + 2] & 0xFF) << 16)
                    | ((raw[base + 3] & 0xFF) << 24);
        }
        return regs;
    }

    private static String queryCpuVendorString() {
        String vendor = queryWindowsRegistryValue(CPU_REG_KEY, "VendorIdentifier");
        if (vendor != null && !vendor.isEmpty()) {
            return vendor.length() > 12 ? vendor.substring(0, 12) : vendor;
        }
        String ident = System.getenv("PROCESSOR_IDENTIFIER");
        if (ident != null) {
            if (ident.contains("AuthenticAMD") || ident.contains("AMD")) {
                return "AuthenticAMD";
            }
            if (ident.contains("GenuineIntel") || ident.contains("Intel")) {
                return "GenuineIntel";
            }
        }
        return "";
    }

    private static int queryDriveCVolumeSerial() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return 0;
        }
        try {
            Process proc = new ProcessBuilder("cmd.exe", "/c", "vol", "C:")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = VOL_SERIAL_PATTERN.matcher(line);
                    if (matcher.find()) {
                        long hi = Long.parseLong(matcher.group(1), 16);
                        long lo = Long.parseLong(matcher.group(2), 16);
                        return (int) ((hi << 16) | lo);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static String queryComputerName() {
        String name = System.getenv("COMPUTERNAME");
        if (name == null || name.isEmpty()) {
            name = System.getenv("HOSTNAME");
        }
        if (name == null) {
            return "";
        }
        return name.length() > 15 ? name.substring(0, 15) : name;
    }

    static String queryWindowsRegistryValue(String keyPath, String valueName) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return null;
        }
        try {
            Process proc = new ProcessBuilder("reg.exe", "query", keyPath, "/v", valueName)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith(valueName)) {
                        int idx = trimmed.indexOf("REG_");
                        if (idx >= 0) {
                            int spaceAfterType = trimmed.indexOf(' ', idx);
                            if (spaceAfterType >= 0) {
                                return trimmed.substring(spaceAfterType).trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String md5UpperHex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            char[] hexChars = "0123456789ABCDEF".toCharArray();
            char[] out = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                int v = digest[i] & 0xFF;
                out[i * 2] = hexChars[v >>> 4];
                out[i * 2 + 1] = hexChars[v & 0x0F];
            }
            return new String(out);
        } catch (Exception e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }
}
