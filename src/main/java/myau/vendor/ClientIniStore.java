package myau.vendor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Restored from native function {@code 0x1805CE7C0}.
 * Reads and persists login credentials in {@code %USERPROFILE%\client.ini} under {@code [info]}.
 */
public final class ClientIniStore {
    private static final String SECTION_NAME = "info";

    public static final class SavedCredentials {
        public boolean remember;
        public String username = "";
        public String password = "";
    }

    private ClientIniStore() {
    }

    public static Path getIniPath() {
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isEmpty()) {
            return Paths.get(userProfile, "client.ini");
        }
        return Paths.get(System.getProperty("user.home", "."), "client.ini");
    }

    public static SavedCredentials load() {
        SavedCredentials creds = new SavedCredentials();
        Path path = getIniPath();
        if (!Files.exists(path)) {
            return creds;
        }

        boolean inInfoSection = false;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String sec = trimmed.substring(1, trimmed.length() - 1).trim();
                    inInfoSection = SECTION_NAME.equalsIgnoreCase(sec);
                    continue;
                }
                if (!inInfoSection) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String val = trimmed.substring(eq + 1).trim();
                if ("Remember".equalsIgnoreCase(key)) {
                    creds.remember = "1".equals(val) || Boolean.parseBoolean(val);
                } else if ("username".equalsIgnoreCase(key)) {
                    creds.username = val;
                } else if ("password".equalsIgnoreCase(key)) {
                    creds.password = val;
                }
            }
        } catch (IOException ignored) {
        }
        return creds;
    }

    public static void save(boolean remember, String username, String password) throws IOException {
        Path path = getIniPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("[" + SECTION_NAME + "]");
            writer.newLine();
            writer.write("Remember=" + (remember ? "true" : "false"));
            writer.newLine();
            writer.write("username=" + (remember && username != null ? username : ""));
            writer.newLine();
            writer.write("password=" + (remember && password != null ? password : ""));
            writer.newLine();
        }
    }
}
