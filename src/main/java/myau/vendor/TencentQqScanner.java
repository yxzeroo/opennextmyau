package myau.vendor;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Restored from native function {@code 0x1805CA8E0} and {@code 0x1805CA720}.
 * Resolves the user's Documents directory and enumerates files under
 * {@code Tencent Files\nt_qq\global\nt_data\Login}, extracting 5-to-10-digit QQ account IDs
 * from file extensions matching {@code \d{5,10}}.
 */
public final class TencentQqScanner {
    private static final String QQ_LOGIN_SUBPATH = "Tencent Files\\nt_qq\\global\\nt_data\\Login";
    private static final Pattern QQ_NUMBER_PATTERN = Pattern.compile("\\d{5,10}");

    private TencentQqScanner() {
    }

    public static Path resolveDocumentsDirectory() {
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isEmpty()) {
            return Paths.get(userProfile, "Documents");
        }
        return Paths.get(System.getProperty("user.home", "."), "Documents");
    }

    public static List<String> scanLoggedQqAccounts() {
        Set<String> accounts = new LinkedHashSet<>();
        Path loginDir = resolveDocumentsDirectory().resolve(QQ_LOGIN_SUBPATH);
        if (!Files.exists(loginDir) || !Files.isDirectory(loginDir)) {
            return new ArrayList<>(accounts);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(loginDir)) {
            for (Path entry : stream) {
                File file = entry.toFile();
                if (!file.isFile()) {
                    continue;
                }
                String fileName = file.getName();
                int dotIdx = fileName.lastIndexOf('.');
                if (dotIdx >= 0 && dotIdx + 1 < fileName.length()) {
                    String ext = fileName.substring(dotIdx + 1);
                    if (QQ_NUMBER_PATTERN.matcher(ext).matches()) {
                        accounts.add(ext);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(accounts);
    }
}
