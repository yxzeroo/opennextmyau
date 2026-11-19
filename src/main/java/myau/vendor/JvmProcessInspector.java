package myau.vendor;

import com.google.gson.JsonObject;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Restored JVM command-line telemetry parser ({@code 0x1805AC0F0}) and
 * {@code nlohmann/json 3.11.2} serializer ({@code 0x1805EDB00..0x1805EE2E3}),
 * plus the {@code JNI_OnLoad} {@code -xbootclasspath} anti-tamper check
 * ({@code 0x180620DF1}).
 */
public final class JvmProcessInspector {
    public static final class ProcessSnapshot {
        public String javaPath = "";
        public String gameVersion = "";
        public String gameDir = "";
        public String assetsDir = "";
        public String tweakClass = "";
        public String username = "";
        public int width = 0;
        public int height = 0;
        public boolean hasAgent = false;
        public String agentPath = "";

        public JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("javaPath", javaPath);
            root.addProperty("gameVersion", gameVersion);
            root.addProperty("gameDir", gameDir);
            root.addProperty("assetsDir", assetsDir);
            root.addProperty("tweakClass", tweakClass);
            root.addProperty("username", username);
            root.addProperty("width", width);
            root.addProperty("height", height);
            root.addProperty("hasAgent", hasAgent);
            if (hasAgent) {
                root.addProperty("agentPath", agentPath);
            }
            return root;
        }
    }

    private JvmProcessInspector() {
    }

    public static ProcessSnapshot inspectCurrentProcess() {
        String sunCmd = System.getProperty("sun.java.command", "");
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String javaHome = System.getProperty("java.home", "");

        StringBuilder fullCmd = new StringBuilder();
        if (!javaHome.isEmpty()) {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String bin = os.contains("win") ? "\\bin\\javaw.exe" : "/bin/java";
            fullCmd.append('"').append(javaHome).append(bin).append('"');
        }
        for (String arg : jvmArgs) {
            if (fullCmd.length() > 0) {
                fullCmd.append(' ');
            }
            fullCmd.append(arg);
        }
        if (!sunCmd.isEmpty()) {
            if (fullCmd.length() > 0) {
                fullCmd.append(' ');
            }
            fullCmd.append(sunCmd);
        }
        return parseCommandLine(fullCmd.toString());
    }

    /**
     * Parses a raw process command line matching {@code 0x1805AC0F0}:
     * extracts {@code java.exe}/{@code javaw.exe}, {@code -javaagent:},
     * {@code --version}, {@code --gameDir}, {@code --assetsDir},
     * {@code --tweakClass}, {@code --username}, {@code --width}, and {@code --height}.
     */
    public static ProcessSnapshot parseCommandLine(String commandLine) {
        ProcessSnapshot snap = new ProcessSnapshot();
        if (commandLine == null || commandLine.isEmpty()) {
            return snap;
        }

        List<String> tokens = tokenizeCommandLine(commandLine);
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String lower = token.toLowerCase(Locale.ROOT);
            if (snap.javaPath.isEmpty() && (lower.endsWith("java.exe") || lower.endsWith("javaw.exe") || lower.endsWith("/java"))) {
                snap.javaPath = token;
                continue;
            }
            if (lower.startsWith("-javaagent:")) {
                snap.hasAgent = true;
                snap.agentPath = token.substring("-javaagent:".length());
                continue;
            }
            if (i + 1 < tokens.size()) {
                String next = tokens.get(i + 1);
                switch (token) {
                    case "--version":
                        snap.gameVersion = next;
                        i++;
                        break;
                    case "--gameDir":
                        snap.gameDir = next;
                        i++;
                        break;
                    case "--assetsDir":
                        snap.assetsDir = next;
                        i++;
                        break;
                    case "--tweakClass":
                        snap.tweakClass = next;
                        i++;
                        break;
                    case "--username":
                        snap.username = next;
                        i++;
                        break;
                    case "--width":
                        try {
                            snap.width = Integer.parseInt(next);
                        } catch (NumberFormatException e) {
                            System.err.println("Debug - Width parse error: " + e.getMessage());
                        }
                        i++;
                        break;
                    case "--height":
                        try {
                            snap.height = Integer.parseInt(next);
                        } catch (NumberFormatException e) {
                            System.err.println("Debug - Height parse error: " + e.getMessage());
                        }
                        i++;
                        break;
                    default:
                        break;
                }
            }
        }
        return snap;
    }

    /**
     * Mirrors the {@code JNI_OnLoad} anti-tamper check ({@code 0x180620DF1})
     * that scans JVM input arguments for {@code -xbootclasspath}.
     */
    public static boolean hasBootClasspathTamper() {
        return hasBootClasspathTamper(ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    public static boolean hasBootClasspathTamper(List<String> jvmArgs) {
        if (jvmArgs == null) {
            return false;
        }
        for (String arg : jvmArgs) {
            if (arg != null && arg.toLowerCase(Locale.ROOT).startsWith("-xbootclasspath")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> tokenizeCommandLine(String cmd) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }
}
