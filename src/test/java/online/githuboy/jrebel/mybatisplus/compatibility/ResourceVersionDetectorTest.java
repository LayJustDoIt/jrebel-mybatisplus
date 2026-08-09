package online.githuboy.jrebel.mybatisplus.compatibility;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ResourceVersionDetector}.
 *
 * <p>Builds in-memory/empty jar fixtures on disk so that
 * {@link ResourceVersionDetector#detectVersion} can be exercised without
 * triggering JVM class loading of the target class.
 *
 * @author TRAE
 */
public class ResourceVersionDetectorTest {

    private File tempJar;

    @Before
    public void setUp() throws IOException {
        tempJar = File.createTempFile("rvd-test-", ".jar");
        tempJar.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempJar != null && tempJar.exists()) {
            tempJar.delete();
        }
    }

    @Test
    public void detectFromPomProperties() throws IOException {
        String pomProps = "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties";
        writeJar(tempJar,
                null,
                pomProps,
                "groupId=com.baomidou\nartifactId=mybatis-plus-core\nversion=3.5.2\n");

        TrackingClassLoader cl = new TrackingClassLoader(new URL[]{tempJar.toURI().toURL()});
        String version = ResourceVersionDetector.detectVersion(
                cl,
                "com/baomidou/mybatisplus/core/MybatisConfiguration.class",
                pomProps);
        assertEquals("3.5.2", version);
    }

    @Test
    public void detectFromManifestWhenPomPropertiesMissing() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "3.5.7");

        writeJar(tempJar, manifest, null, null);

        TrackingClassLoader cl = new TrackingClassLoader(new URL[]{tempJar.toURI().toURL()});
        String version = ResourceVersionDetector.detectVersion(
                cl,
                "com/baomidou/mybatisplus/core/MybatisConfiguration.class",
                "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties");
        assertEquals("3.5.7", version);
    }

    @Test
    public void pomPropertiesTakesPrecedenceOverManifest() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "3.5.7");

        String pomProps = "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties";
        writeJar(tempJar,
                manifest,
                pomProps,
                "groupId=com.baomidou\nartifactId=mybatis-plus-core\nversion=3.5.2\n");

        TrackingClassLoader cl = new TrackingClassLoader(new URL[]{tempJar.toURI().toURL()});
        String version = ResourceVersionDetector.detectVersion(
                cl,
                "com/baomidou/mybatisplus/core/MybatisConfiguration.class",
                pomProps);
        // pom.properties should win
        assertEquals("3.5.2", version);
    }

    @Test
    public void returnsUnknownWhenResourceMissing() throws IOException {
        writeJar(tempJar, null, null, null);

        TrackingClassLoader cl = new TrackingClassLoader(new URL[]{tempJar.toURI().toURL()});
        String version = ResourceVersionDetector.detectVersion(
                cl,
                "com/baomidou/mybatisplus/core/MybatisConfiguration.class",
                "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties");
        assertEquals("unknown", version);
    }

    @Test
    public void returnsUnknownWhenPomPropertiesEmptyVersion() throws IOException {
        String pomProps = "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties";
        writeJar(tempJar,
                null,
                pomProps,
                "groupId=com.baomidou\nartifactId=mybatis-plus-core\nversion=\n");

        TrackingClassLoader cl = new TrackingClassLoader(new URL[]{tempJar.toURI().toURL()});
        String version = ResourceVersionDetector.detectVersion(
                cl,
                "com/baomidou/mybatisplus/core/MybatisConfiguration.class",
                pomProps);
        assertEquals("unknown", version);
    }

    @Test
    public void returnsUnknownWhenClassLoaderNull() {
        String version = ResourceVersionDetector.detectVersion(
                null,
                "com/baomidou/mybatisplus/core/MybatisConfiguration.class",
                "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties");
        assertEquals("unknown", version);
    }

    @Test
    public void neverTriggersClassLoading() throws Exception {
        // Build a jar with a .class entry that triggers a visible side-effect
        // if loaded. Use a simple empty class entry; we only assert that
        // detectVersion returns without requiring the class to be loadable.
        String pomProps = "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties";
        writeJar(tempJar, null, pomProps,
                "groupId=com.baomidou\nartifactId=mybatis-plus-core\nversion=3.5.2\n");

        TrackingClassLoader cl = new TrackingClassLoader(new URL[]{tempJar.toURI().toURL()});
        String version = ResourceVersionDetector.detectVersion(
                cl,
                "com/baomidou/mybatisplus/core/MybatisConfiguration.class",
                pomProps);

        assertEquals("3.5.2", version);
        assertTrue("detectVersion must not call loadClass on the target class",
                cl.loadClassCalls == 0);
    }

    private void writeJar(File target,
                          Manifest manifest,
                          String pomPropsEntry,
                          String pomPropsContent) throws IOException {
        OutputStream out = new FileOutputStream(target);
        JarOutputStream jar;
        if (manifest != null) {
            jar = new JarOutputStream(out, manifest);
        } else {
            jar = new JarOutputStream(out);
        }
        try {
            // empty class entry so resource URL resolves as jar: URL
            JarEntry classEntry = new JarEntry("com/baomidou/mybatisplus/core/MybatisConfiguration.class");
            jar.putNextEntry(classEntry);
            jar.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jar.closeEntry();

            if (pomPropsEntry != null && pomPropsContent != null) {
                JarEntry propsEntry = new JarEntry(pomPropsEntry);
                jar.putNextEntry(propsEntry);
                jar.write(pomPropsContent.getBytes("UTF-8"));
                jar.closeEntry();
            }
        } finally {
            jar.close();
        }
    }

    /**
     * ClassLoader that tracks calls to {@link #loadClass(String)} and
     * resolves resources from its own URLs first, without delegating to
     * the parent. This isolates test fixtures from the host classpath
     * (which may contain a different mybatis-plus version).
     */
    private static final class TrackingClassLoader extends URLClassLoader {
        volatile int loadClassCalls;

        TrackingClassLoader(URL[] urls) {
            // parent = null so resource lookups stay isolated
            super(urls, ClassLoader.getSystemClassLoader().getParent());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name != null && name.contains("MybatisConfiguration")) {
                loadClassCalls++;
            }
            return super.loadClass(name, resolve);
        }

        @Override
        public URL getResource(String name) {
            // search own URLs first (child-first) to avoid host classpath leaks
            URL url = findResource(name);
            if (url != null) {
                return url;
            }
            return super.getResource(name);
        }
    }
}
