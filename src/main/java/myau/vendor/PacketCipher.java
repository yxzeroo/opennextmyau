package myau.vendor;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Restored from native functions {@code 0x1805F0F80}, {@code 0x1805F1320}, and {@code 0x1805F15A0}.
 * Performs AES-128 block encryption/decryption encoded as lowercase hexadecimal strings.
 */
public final class PacketCipher {
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private PacketCipher() {
    }

    public static byte[] normalizeKey16(byte[] keyBytes) {
        if (keyBytes == null) {
            return new byte[16];
        }
        return Arrays.copyOf(keyBytes, 16);
    }

    public static byte[] normalizeKey16(String key) {
        if (key == null) {
            return new byte[16];
        }
        return normalizeKey16(key.getBytes(StandardCharsets.UTF_8));
    }

    public static String encryptToHex(String plaintext, byte[] key16) {
        try {
            byte[] input = plaintext.getBytes(StandardCharsets.UTF_8);
            int paddedLen = ((input.length + 15) / 16) * 16;
            if (paddedLen == 0) {
                paddedLen = 16;
            }
            byte[] padded = Arrays.copyOf(input, paddedLen);
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(normalizeKey16(key16), "AES"));
            byte[] encrypted = cipher.doFinal(padded);
            return toHex(encrypted);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("encrypt failed", e);
        }
    }

    public static String decryptFromHex(String hexCiphertext, byte[] key16) {
        byte[] cipherBytes = fromHex(hexCiphertext);
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(normalizeKey16(key16), "AES"));
            byte[] decrypted = cipher.doFinal(cipherBytes);
            int end = decrypted.length;
            while (end > 0 && (decrypted[end - 1] == 0 || (decrypted[end - 1] & 0xFF) <= 16 && isPkcs7Tail(decrypted))) {
                if (decrypted[end - 1] == 0) {
                    end--;
                } else {
                    end -= (decrypted[decrypted.length - 1] & 0xFF);
                    break;
                }
            }
            return new String(decrypted, 0, Math.max(0, end), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("decrypt failed", e);
        }
    }

    private static boolean isPkcs7Tail(byte[] block) {
        if (block.length == 0) {
            return false;
        }
        int pad = block[block.length - 1] & 0xFF;
        if (pad < 1 || pad > 16 || pad > block.length) {
            return false;
        }
        for (int i = block.length - pad; i < block.length; i++) {
            if ((block[i] & 0xFF) != pad) {
                return false;
            }
        }
        return true;
    }

    public static String toHex(byte[] data) {
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            out[i * 2] = HEX_CHARS[b >>> 4];
            out[i * 2 + 1] = HEX_CHARS[b & 0x0F];
        }
        return new String(out);
    }

    public static byte[] fromHex(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex string length must be even");
        }
        int len = hex.length() >> 1;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = hexNibble(hex.charAt(i * 2));
            int lo = hexNibble(hex.charAt(i * 2 + 1));
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int hexNibble(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("Invalid hex character");
    }
}
