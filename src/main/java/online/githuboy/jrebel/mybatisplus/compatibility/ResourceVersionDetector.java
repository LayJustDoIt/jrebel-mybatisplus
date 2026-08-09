package online.githuboy.jrebel.mybatisplus.compatibility;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Pure resource-based artifact version detector.
 *
 * <p>It never triggers JVM class loading of the target class. Detection only
 * relies on {@link ClassLoader#getResource(String)}, jar {@link Manifest}
 * {@code Implementation-Version} and Maven {@code pom.properties}.
 *
 * <p>This is critical because JRebel ClassBytecodeProcessor registration must
 * happen before the target class is loaded. Any {@code Class.forName} during
 * plugin preinit would load the class ahead of CBP registration and silently
 * disable bytecode enhancement for that class.
 *
 * @author since 1.0.8
 */
public final class ResourceVersionDetector {

    private ResourceVersionDetector() {
    }

    /**
     * Detect artifact version without loading the class.
     *
     * <p>Detection order:
     * <ol>
     *   <li>Maven {@code META-INF/maven/<groupId>/<artifactId>/pom.properties} {@code version}</li>
     *   <li>Jar {@code META-INF/MANIFEST.MF} {@code Implementation-Version}</li>
     *   <li>{@code "unknown"} fallback</li>
     * </ol>
     *
     * @param classLoader          class loader used for resource lookup
     * @param classResource        class resource path, e.g. {@code com/baomidou/mybatisplus/core/MybatisConfiguration.class}
     * @param pomPropertiesResource maven pom.properties resource path, e.g. {@code META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties}
     * @return detected version string, never {@code null}
     */
    public static String detectVersion(ClassLoader classLoader,
                                       String classResource,
                                       String pomPropertiesResource) {
        if (classLoader == null) {
            return "unknown";
        }

        // 1. Maven pom.properties (preferred: accurate for shaded/non-shaded jars)
        String pomVersion = readPomPropertiesVersion(classLoader, pomPropertiesResource);
        if (pomVersion != null && !pomVersion.isEmpty()) {
            return pomVersion;
        }

        // 2. Jar MANIFEST.MF Implementation-Version (fallback)
        String manifestVersion = readManifestImplementationVersion(classLoader, classResource);
        if (manifestVersion != null && !manifestVersion.isEmpty()) {
            return manifestVersion;
        }

        return "unknown";
    }

    /**
     * Read {@code version} property from Maven {@code pom.properties}.
     */
    private static String readPomPropertiesVersion(ClassLoader classLoader, String pomPropertiesResource) {
        if (pomPropertiesResource == null || pomPropertiesResource.isEmpty()) {
            return null;
        }
        InputStream in = null;
        try {
            in = classLoader.getResourceAsStream(pomPropertiesResource);
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version");
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Read {@code Implementation-Version} attribute from the jar manifest
     * that contains the target class resource. Resolves the jar via the
     * class resource URL without loading the class.
     */
    private static String readManifestImplementationVersion(ClassLoader classLoader, String classResource) {
        if (classResource == null || classResource.isEmpty()) {
            return null;
        }
        URL classUrl = classLoader.getResource(classResource);
        if (classUrl == null) {
            return null;
        }
        URLConnection conn = null;
        try {
            conn = classUrl.openConnection();
            if (!(conn instanceof JarURLConnection)) {
                return null;
            }
            Manifest manifest = ((JarURLConnection) conn).getManifest();
            if (manifest == null) {
                return null;
            }
            Attributes mainAttrs = manifest.getMainAttributes();
            return mainAttrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (IOException e) {
            return null;
        } finally {
            // JarURLConnection does not hold a persistent stream; nothing to close.
        }
    }
}
