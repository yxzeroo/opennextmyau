package myau.vendor;

import java.nio.charset.StandardCharsets;

/**
 * Restored packet cipher used by the native WebSocket auth client ({@code 0x1805F0F80},
 * {@code 0x1805F1320}, {@code 0x1805F15A0}, and {@code 0x18063D300..0x18063D760}).
 *
 * <p>Uses XXTEA with a custom key-schedule constant {@code DELTA = 0x9E3779B1}
 * (instead of standard {@code 0x9E3779B9}), C-string NUL-truncated 16-byte key
 * normalization, an appended 32-bit plaintext byte-length word, and lowercase
 * hexadecimal wire encoding. Both {@code clientKey} and {@code serverKey} start
 * initialized to {@link VendorConfig#INITIAL_KEY} ({@code "中国"}).
 */
public final class PacketCipher {
    public static final int XXTEA_DELTA = 0x9E3779B1;
    private static final char[] HEX_LOWER = "0123456789abcdef".toCharArray();

    private PacketCipher() {
    }

    public static String encrypt(String plaintext, String keyString) {
        if (plaintext == null) {
            throw new RuntimeException("encrypt failed");
        }
        byte[] data = plaintext.getBytes(StandardCharsets.UTF_8);
        if (data.length == 0) {
            throw new RuntimeException("encrypt failed");
        }

        int[] v = toIntArray(data, true);
        int[] k = normalizeKeyWords(keyString);
        int n = v.length - 1;
        if (n >= 1) {
            int rounds = 6 + 52 / (n + 1);
            int z = v[n];
            int sum = 0;
            while (rounds-- > 0) {
                sum += XXTEA_DELTA;
                int e = (sum >>> 2) & 3;
                for (int p = 0; p < n; p++) {
                    int y = v[p + 1];
                    v[p] += mx(sum, y, z, p, e, k);
                    z = v[p];
                }
                int y = v[0];
                v[n] += mx(sum, y, z, n, e, k);
                z = v[n];
            }
        }

        byte[] encrypted = toByteArray(v, false);
        if (encrypted == null) {
            throw new RuntimeException("encrypt failed");
        }
        return bytesToHex(encrypted);
    }

    public static String decrypt(String cipherHex, String keyString) {
        byte[] raw = hexToBytes(cipherHex);
        if (raw.length == 0) {
            throw new RuntimeException("decrypt failed");
        }

        int[] v = toIntArray(raw, false);
        int[] k = normalizeKeyWords(keyString);
        int n = v.length - 1;
        if (n >= 1) {
            int rounds = 6 + 52 / (n + 1);
            int sum = rounds * XXTEA_DELTA;
            int y = v[0];
            while (sum != 0) {
                int e = (sum >>> 2) & 3;
                for (int p = n; p > 0; p--) {
                    int z = v[p - 1];
                    v[p] -= mx(sum, y, z, p, e, k);
                    y = v[p];
                }
                int z = v[n];
                v[0] -= mx(sum, y, z, 0, e, k);
                y = v[0];
                sum -= XXTEA_DELTA;
            }
        }

        byte[] plain = toByteArray(v, true);
        if (plain == null) {
            throw new RuntimeException("decrypt failed");
        }
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static int mx(int sum, int y, int z, int p, int e, int[] k) {
        return (((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4)))
                ^ ((sum ^ y) + (k[(p & 3) ^ e] ^ z));
    }

    private static int[] normalizeKeyWords(String keyString) {
        byte[] raw = keyString == null
                ? new byte[0]
                : keyString.getBytes(StandardCharsets.UTF_8);
        byte[] fixed = new byte[16];
        int limit = Math.min(raw.length, 16);
        for (int i = 0; i < limit; i++) {
            if (raw[i] == 0) {
                break;
            }
            fixed[i] = raw[i];
        }
        int[] k = new int[4];
        for (int i = 0; i < 16; i++) {
            k[i >>> 2] |= (fixed[i] & 0xFF) << ((i & 3) << 3);
        }
        return k;
    }

    private static int[] toIntArray(byte[] data, boolean includeLength) {
        int n = (data.length >>> 2) + ((data.length & 3) != 0 ? 1 : 0);
        int[] result;
        if (includeLength) {
            result = new int[n + 1];
            result[n] = data.length;
        } else {
            result = new int[Math.max(n, 1)];
        }
        for (int i = 0; i < data.length; i++) {
            result[i >>> 2] |= (data[i] & 0xFF) << ((i & 3) << 3);
        }
        return result;
    }

    private static byte[] toByteArray(int[] data, boolean includeLength) {
        int n = data.length << 2;
        if (includeLength) {
            int m = data[data.length - 1];
            n -= 4;
            if (m < n - 3 || m > n) {
                return null;
            }
            n = m;
        }
        byte[] result = new byte[n];
        for (int i = 0; i < n; i++) {
            result[i] = (byte) ((data[i >>> 2] >>> ((i & 3) << 3)) & 0xFF);
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_LOWER[v >>> 4];
            out[i * 2 + 1] = HEX_LOWER[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Invalid hex character");
        }
        int len = hex.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Hex string length must be even");
        }
        byte[] out = new byte[len >>> 1];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex character");
            }
            out[i >>> 1] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
