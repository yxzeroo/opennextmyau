package myau.vendor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Restored {@code CSimpleIniA} credential store ({@code 0x1805CE7C0}).
 *
 * <p>Resolves {@code %USERPROFILE%\myau.ini} (replacing the static default
 * {@code "client"} stem with {@link VendorConfig#CLIENT_ID} {@code "myau"}) and
 * reads/writes {@code Remember}, {@code username}, and {@code password} under
 * the {@code [info]} section.
 */
public final class ClientIniStore {
    private static final String SECTION_INFO = "info";
    private static final String KEY_REMEMBER = "Remember";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    public static final class Credentials {
        public boolean remember;
        public String username = "";
        public String password = "";
    }

    public File resolveIniFile() {
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile == null || userProfile.isEmpty()) {
            userProfile = System.getProperty("user.home", ".");
        }
        return new File(userProfile, VendorConfig.INI_FILE_NAME);
    }

    public Credentials load() {
        Credentials creds = new Credentials();
        File iniFile = resolveIniFile();
        if (!iniFile.isFile()) {
            return creds;
        }

        String rawRemember = null;
        String rawUsername = "";
        String rawPassword = "";
        boolean inInfoSection = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(iniFile), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                firstLine = false;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String section = trimmed.substring(1, trimmed.length() - 1).trim();
                    inInfoSection = SECTION_INFO.equalsIgnoreCase(section);
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
                String value = trimmed.substring(eq + 1).trim();
                if (KEY_REMEMBER.equalsIgnoreCase(key)) {
                    rawRemember = value;
                } else if (KEY_USERNAME.equalsIgnoreCase(key)) {
                    rawUsername = value;
                } else if (KEY_PASSWORD.equalsIgnoreCase(key)) {
                    rawPassword = value;
                }
            }
        } catch (IOException ignored) {
        }

        creds.remember = parseSimpleIniBool(rawRemember, false);
        if (creds.remember) {
            creds.username = rawUsername;
            creds.password = rawPassword;
        }
        return creds;
    }

    public void save(Credentials creds) {
        File iniFile = resolveIniFile();
        File parent = iniFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(iniFile), StandardCharsets.UTF_8))) {
            writer.write("[" + SECTION_INFO + "]\r\n");
            writer.write(KEY_REMEMBER + " = " + Boolean.toString(creds.remember) + "\r\n");
            if (creds.remember) {
                writer.write(KEY_USERNAME + " = " + (creds.username != null ? creds.username : "") + "\r\n");
                writer.write(KEY_PASSWORD + " = " + (creds.password != null ? creds.password : "") + "\r\n");
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Exact {@code CSimpleIniA::GetBoolValue} logic ({@code 0x1805CEA20}):
     * returns {@code true} for values starting with {@code '1'}, {@code 't'}/{@code 'T'},
     * {@code 'y'}/{@code 'Y'}, or equal to {@code "on"} (case-insensitive).
     */
    public static boolean parseSimpleIniBool(String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        char first = value.charAt(0);
        if (first == '1' || first == 't' || first == 'T' || first == 'y' || first == 'Y') {
            return true;
        }
        if ("on".equalsIgnoreCase(value)) {
            return true;
        }
        return false;
    }
}
