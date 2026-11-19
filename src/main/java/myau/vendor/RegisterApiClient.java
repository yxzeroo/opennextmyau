package myau.vendor;

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
 * Restored {@code cpp-httplib/0.14.0} registration client ({@code 0x1805A32D0}
 * and {@code 0x1805CE159}).
 *
 * <p>Sends a plain HTTP (port 80) {@code POST http://foodbyte.cc/api/register}
 * request with {@code User-Agent: FSMClient}, {@code Client: myau}, and
 * {@code Language: <BCP-47 locale>} headers.
 */
public final class RegisterApiClient {
    public static final class RegisterResponse {
        public final int httpStatus;
        public final int code;
        public final String message;

        public RegisterResponse(int httpStatus, int code, String message) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
        }
    }

    public RegisterResponse register(String username, String password, String cdKey) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(VendorConfig.REGISTER_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", VendorConfig.HTTP_USER_AGENT);
            conn.setRequestProperty("Client", VendorConfig.CLIENT_ID);
            conn.setRequestProperty("Language", resolveSystemLanguage());
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String formBody = "username=" + encodeFormComponent(username)
                    + "&password=" + encodeFormComponent(password)
                    + "&cdkey=" + encodeFormComponent(cdKey);

            byte[] payload = formBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(payload.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int status = conn.getResponseCode();
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = readStream(stream);

            if (status == 200 && !body.isEmpty()) {
                JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                int code = json.has("code") ? json.get("code").getAsInt() : -1;
                String msg = json.has("message") ? json.get("message").getAsString() : body;
                return new RegisterResponse(status, code, msg);
            }
            return new RegisterResponse(status, -1, "Request failed. Status code: " + status);
        } catch (Exception e) {
            return new RegisterResponse(-1, -1, "Caught exception: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Mirrors {@code GetUserDefaultLocaleName} wrapper ({@code 0x1805AAC30}),
     * falling back to {@link VendorConfig#LANGUAGE_FALLBACK}.
     */
    public static String resolveSystemLanguage() {
        try {
            Locale locale = Locale.getDefault();
            if (locale != null) {
                String tag = locale.toLanguageTag();
                if (tag != null && !tag.isEmpty() && !"und".equalsIgnoreCase(tag)) {
                    return tag;
                }
            }
        } catch (Exception ignored) {
        }
        return VendorConfig.LANGUAGE_FALLBACK;
    }

    private static String encodeFormComponent(String value) throws Exception {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
