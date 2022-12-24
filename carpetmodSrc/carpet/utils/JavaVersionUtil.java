package carpet.utils;

public final class JavaVersionUtil {
    public static final int JAVA_VERSION = getJavaVersion();

    private JavaVersionUtil() {
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            // old format (Java 8 and below)
            return version.charAt(2) - '0';
        } else {
            // new format (Java 9 and above)
            int dotIndex = version.indexOf('.');
            if (dotIndex == -1) {
                return Integer.parseInt(version);
            } else {
                return Integer.parseInt(version.substring(0, dotIndex));
            }
        }
    }
}
