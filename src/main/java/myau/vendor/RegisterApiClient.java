package myau.vendor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Restored from native function {@code 0x1805A32D0}.
 * Submits account registration requests with a license key ({@code cdkey}) to {@code /api/register}.
 */
public final class RegisterApiClient {

    public static final class RegisterResult {
        public final boolean success;
        public final int statusCode;
        public final String message;

        public RegisterResult(boolean success, int statusCode, String message) {
            this.success = success;
            this.statusCode = statusCode;
            this.message = message;
        }
    }

    private RegisterApiClient() {
    }

    public static String resolveSystemLanguageTag() {
        Locale loc = Locale.getDefault();
        if (loc == null) {
            return "Failed to get system language.";
        }
        String tag = loc.toLanguageTag();
        return (tag == null || tag.isEmpty()) ? "Failed to get system language." : tag;
    }

    public static RegisterResult registerAccount(String username, String password, String cdkey) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(VendorConfig.REGISTER_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", VendorConfig.USER_AGENT);
            conn.setRequestProperty("Client", VendorConfig.CLIENT_NAME);
            conn.setRequestProperty("Language", resolveSystemLanguageTag());
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String formBody = "username=" + URLEncoder.encode(username != null ? username : "", "UTF-8")
                    + "&password=" + URLEncoder.encode(password != null ? password : "", "UTF-8")
                    + "&cdkey=" + URLEncoder.encode(cdkey != null ? cdkey : "", "UTF-8");

            byte[] payload = formBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", Integer.toString(payload.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int httpStatus = conn.getResponseCode();
            InputStream stream = (httpStatus >= 200 && httpStatus < 400) ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(stream);

            if (httpStatus == 200) {
                JsonElement parsed = new JsonParser().parse(body);
                if (parsed != null && parsed.isJsonObject()) {
                    JsonObject obj = parsed.getAsJsonObject();
                    int code = obj.has("code") ? obj.get("code").getAsInt() : httpStatus;
                    String msg = obj.has("message") ? obj.get("message").getAsString() : body;
                    return new RegisterResult(code == 200, code, msg);
                }
                return new RegisterResult(true, 200, "Request successful: " + body);
            }
            return new RegisterResult(false, httpStatus, "Request failed. Status code: " + httpStatus);
        } catch (Exception e) {
            return new RegisterResult(false, -1, "Unexpected error during registration: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
