package myau.vendor;

/**
 * Restored constants from the native loader binary ({@code x64-windows.dll}).
 * Stripped from active execution; preserved strictly for structural reference.
 */
public final class VendorConfig {
    public static final String CLIENT_NAME = "MYAU";
    public static final String CLIENT_ID = "myau";
    public static final String CLIENT_VERSION = "260729";
    public static final String INI_FILE_NAME = "myau.ini";

    public static final String API_HOST = "foodbyte.cc";
    public static final String REGISTER_PATH = "/api/register";
    public static final String REGISTER_URL = "http://" + API_HOST + REGISTER_PATH;

    public static final String WS_BASE_URL = "wss://" + API_HOST + ":443/" + CLIENT_NAME;
    public static final String HANDSHAKE_PROTOCOL_TAG = "$v2";
    public static final String PROTOCOL_DELIMITER = "$";
    public static final String HEARTBEAT_PONG_JSON = "{ \"@class\" : \".pong\" }";

    public static final String WS_AUTHORIZATION = "15db07114504480519240fcc892fcd25e357cedf";
    public static final String WS_COOKIE = "fblogin=0O0O0O00o0o";
    public static final String WS_SEC_KEY = "x3JJHMbDL1EzLkh9GBhXDw==";
    public static final String WS_VERSION = "13";

    public static final String HTTP_USER_AGENT = "FSMClient";
    public static final String LANGUAGE_FALLBACK = "Failed to get system language.";
    public static final String INITIAL_KEY = "中国";

    public static final String LOADER_RESOURCE_NAMESPACE = "antiLeak-1a2786b1-7d43-4a96-b4bc-752eb89b1600";
    public static final String LOADER_CLASS_INTERNAL_NAME = LOADER_RESOURCE_NAMESPACE + "/Loader";
    public static final String BOOTSTRAP_CLASS_INTERNAL_NAME = LOADER_RESOURCE_NAMESPACE + "/bootstrap/Bootstrap0";

    private VendorConfig() {
    }
}
