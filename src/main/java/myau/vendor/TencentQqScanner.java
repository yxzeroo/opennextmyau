package myau.vendor;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Restored local Tencent QQ account UIN scanner ({@code 0x1805CA720},
 * {@code 0x1805CA8E0}, and {@code WS_Message_Callback} blocks {@code B0495}/{@code B0544}).
 *
 * <p>Collects 5-to-10-digit QQ UINs ({@code \d{5,10}}) from two local locations:
 * <ul>
 *   <li><b>NT QQ</b> ({@code 0x1805CA8E0}): {@code <Documents>\Tencent Files\nt_qq\global\nt_data\Login},
 *       matching regular file extensions (with leading {@code '.'} stripped) against {@code \d{5,10}}.</li>
 *   <li><b>Legacy PC QQ</b> ({@code 0x1809E0230}): {@code \Tencent\Users\*.*}, matching subdirectory
 *       names against {@code \d{5,10}}.</li>
 * </ul>
 */
public final class TencentQqScanner {
    private static final Pattern QQ_UIN_PATTERN = Pattern.compile("\\d{5,10}");
    private static final String SHELL_FOLDERS_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders";

    private TencentQqScanner() {
    }

    public static List<String> scanLoggedQqAccounts() {
        Set<String> found = new LinkedHashSet<>();
        scanNtQqLoginFiles(found);
        scanLegacyQqUserDirs(found);
        return new ArrayList<>(found);
    }

    private static void scanNtQqLoginFiles(Set<String> out) {
        File documentsDir = resolveWindowsDocumentsDirectory();
        File ntLoginDir = new File(documentsDir, "Tencent Files\\nt_qq\\global\\nt_data\\Login");
        File[] files = ntLoginDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < name.length()) {
                String ext = name.substring(dot + 1);
                if (QQ_UIN_PATTERN.matcher(ext).matches()) {
                    out.add(ext);
                }
            }
        }
    }

    private static void scanLegacyQqUserDirs(Set<String> out) {
        List<File> candidateRoots = new ArrayList<>();
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            candidateRoots.add(new File(appData, "Tencent\\Users"));
        }
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isEmpty()) {
            candidateRoots.add(new File(userProfile, "Tencent\\Users"));
            candidateRoots.add(new File(userProfile, "Documents\\Tencent\\Users"));
        }
        String publicDir = System.getenv("PUBLIC");
        if (publicDir != null && !publicDir.isEmpty()) {
            candidateRoots.add(new File(publicDir, "Documents\\Tencent\\Users"));
        }

        for (File usersDir : candidateRoots) {
            File[] entries = usersDir.listFiles();
            if (entries == null) {
                continue;
            }
            for (File entry : entries) {
                String name = entry.getName();
                if (QQ_UIN_PATTERN.matcher(name).matches()) {
                    out.add(name);
                }
            }
        }
    }

    private static File resolveWindowsDocumentsDirectory() {
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile == null || userProfile.isEmpty()) {
            userProfile = System.getProperty("user.home", ".");
        }
        String regPersonal = HardwareIdGenerator.queryWindowsRegistryValue(SHELL_FOLDERS_KEY, "Personal");
        if (regPersonal != null && !regPersonal.isEmpty()) {
            String expanded = regPersonal.replace("%USERPROFILE%", userProfile);
            return new File(expanded);
        }
        return new File(userProfile, "Documents");
    }
}
