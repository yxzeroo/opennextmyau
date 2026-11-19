package myau.vendor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Restored from {@code antiLeak-1a2786b1-7d43-4a96-b4bc-752eb89b1600/Loader.class}.
 *
 * <p>In the original JAR, {@code Loader}'s static initializer ({@code <clinit>})
 * derived the resource prefix from {@code Loader.class.getName().split("\\.")[0]},
 * extracted {@code /antiLeak-1a2786b1-7d43-4a96-b4bc-752eb89b1600/x64-windows.dll}
 * into {@code native/<random6>.lib}, and called {@link System#load(String)}.
 * The 32 native-obfuscated client classes then called
 * {@link #registerNativesForClass(int, Class)} from their {@code <clinit>} blocks.
 *
 * <p>Moved into {@code myau.vendor} and extracted out of {@code static {}} so it
 * remains inert and never executes during normal mod startup.
 */
public class Loader {
    public static String By_SuChen = "<3 qwq";
    private static final String RESOURCE_NAMESPACE = VendorConfig.LOADER_RESOURCE_NAMESPACE;

    public static native void registerNativesForClass(int classId, Class<?> targetClass);

    public static String getlinkNo() {
        String linkNo = "";
        String model = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMINOPQRSTUVWXYZ";
        char[] m = model.toCharArray();
        for (int j = 0; j < 6; ++j) {
            char c = m[(int) (Math.random() * (double) model.length() - 1.0D)];
            if (linkNo.contains(String.valueOf(c))) {
                --j;
            } else {
                linkNo = linkNo + c;
            }
        }
        return linkNo;
    }

    public static void extractAndLoadNative() {
        String osName = System.getProperty("os.name").toLowerCase();
        String platform = System.getProperty("os.arch").toLowerCase();
        String platformTypeName;
        switch (platform) {
            case "x86_64":
            case "amd64":
                platformTypeName = "x64";
                break;
            case "aarch64":
                platformTypeName = "arm64";
                break;
            case "arm":
                platformTypeName = "arm32";
                break;
            case "x86":
                platformTypeName = "x86";
                break;
            default:
                platformTypeName = "raw" + platform;
                break;
        }

        String osTypeName;
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            osTypeName = "linux.so";
        } else if (osName.contains("win")) {
            osTypeName = "windows.dll";
        } else if (osName.contains("mac")) {
            osTypeName = "macos.dylib";
        } else {
            osTypeName = "raw" + osName;
        }

        String libFileName = String.format("/%s/%s-%s", RESOURCE_NAMESPACE, platformTypeName, osTypeName);
        File libFile = new File("native", getlinkNo() + ".lib");
        File parentDir = libFile.getParentFile();
        if (parentDir.exists()) {
            if (!parentDir.isDirectory()) {
                parentDir.delete();
            }
            File[] existing = parentDir.listFiles();
            if (existing != null) {
                for (File file : existing) {
                    if (file.getName().endsWith(".lib")) {
                        file.delete();
                    }
                }
            }
        } else {
            parentDir.mkdir();
        }

        byte[] buffer = new byte[2048];
        try (InputStream inputStream = Loader.class.getResourceAsStream(libFileName)) {
            if (inputStream == null) {
                throw new UnsatisfiedLinkError(String.format("Failed to open lib file: %s", libFileName));
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(libFile)) {
                int size;
                while ((size = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, size);
                }
            }
        } catch (IOException e) {
            throw new UnsatisfiedLinkError(String.format("Failed to copy file: %s", e.getMessage()));
        }

        System.load(libFile.getAbsolutePath());
    }
}
