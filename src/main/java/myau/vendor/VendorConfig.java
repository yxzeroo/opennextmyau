package myau.vendor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Constants extracted from the native authentication and telemetry module (x64-windows.dll).
 * Restored for inspection only; not referenced anywhere by the mod.
 */
public final class VendorConfig {
    public static final String CLIENT_NAME = "MYAU";
    public static final String CLIENT_VERSION = "260729";
    public static final String LOADER_TAG = "<3 qwq";
    public static final String POWERED_BY = "Powered by \u82cf\u8fb0";

    public static final String HOST = "foodbyte.cc";
    public static final int PORT = 443;
    public static final String REGISTER_ENDPOINT = "/api/register";
    public static final String REGISTER_URL = "https://foodbyte.cc/api/register";
    public static final String WS_BASE_URL = "wss://foodbyte.cc:443/MYAU";

    public static final String USER_AGENT = "FSMClient";
    public static final String WS_AUTHORIZATION = "15db07114504480519240fcc892fcd25e357cedf";
    public static final String WS_COOKIE = "fblogin=0O0O0O00o0o";
    public static final String WS_SEC_KEY = "x3JJHMbDL1EzLkh9GBhXDw==";
    public static final String PROTOCOL_VERSION_SUFFIX = "$v2";
    public static final String HEARTBEAT_PONG = ".pong";

    /**
     * Initial 16-byte AES-128 key derived from UTF-8 bytes of "\u4e2d\u56fd" zero-padded to 16 bytes.
     */
    public static byte[] getInitialKeyBytes() {
        byte[] raw = "\u4e2d\u56fd".getBytes(StandardCharsets.UTF_8);
        return Arrays.copyOf(raw, 16);
    }

    private VendorConfig() {
    }
}
