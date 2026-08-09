package online.githuboy.jrebel.mybatisplus.compatibility;

import online.githuboy.jrebel.mybatisplus.compatibility.CapabilityDetector.StrictMapVariant;
import org.junit.Test;
import org.zeroturnaround.bundled.javassist.ClassPool;
import org.zeroturnaround.bundled.javassist.CtClass;
import org.zeroturnaround.bundled.javassist.CtField;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CapabilityDetector}.
 * <p>
 * Uses isolated {@link ClassPool} instances (without system classpath)
 * to create mock class structures that mimic different MyBatis-Plus versions.
 *
 * @author TRAE
 */
public class CapabilityDetectorTest {

    private static final String STRICT_MAP =
            "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap";
    private static final String STRICT_MAP_AMBIGUITY =
            "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap$Ambiguity";
    private static final String MYBATIS_CONFIG =
            "com.baomidou.mybatisplus.core.MybatisConfiguration";

    // ===== detectStrictMapVariant =====

    @Test
    public void detectAmbiguityInstanceFieldVariant() throws Exception {
        ClassPool cp = new ClassPool(false);
        CtClass strictMap = cp.makeClass(STRICT_MAP);
        // Use int to avoid needing java.lang.Object on the classpath
        strictMap.addField(CtField.make(
                "private static final int AMBIGUITY_INSTANCE = 0;", strictMap));

        StrictMapVariant variant = CapabilityDetector.detectStrictMapVariant(cp);
        assertEquals(StrictMapVariant.AMBIGUITY_INSTANCE_FIELD, variant);
    }

    @Test
    public void detectLegacyAmbiguityClassVariant() throws Exception {
        ClassPool cp = new ClassPool(false);
        cp.makeClass(STRICT_MAP);            // StrictMap without AMBIGUITY_INSTANCE 字段变体
        cp.makeClass(STRICT_MAP_AMBIGUITY);  // Legacy Ambiguity inner class

        StrictMapVariant variant = CapabilityDetector.detectStrictMapVariant(cp);
        assertEquals(StrictMapVariant.LEGACY_AMBIGUITY_CLASS, variant);
    }

    @Test
    public void detectUnknownWhenStrictMapMissing() {
        ClassPool cp = new ClassPool(false);
        StrictMapVariant variant = CapabilityDetector.detectStrictMapVariant(cp);
        assertEquals(StrictMapVariant.UNKNOWN, variant);
    }

    @Test
    public void detectUnknownWhenNoAmbiguityAtAll() throws Exception {
        ClassPool cp = new ClassPool(false);
        cp.makeClass(STRICT_MAP);  // StrictMap exists but no Ambiguity class or field

        StrictMapVariant variant = CapabilityDetector.detectStrictMapVariant(cp);
        assertEquals(StrictMapVariant.UNKNOWN, variant);
    }

    @Test
    public void detectUnknownWhenClassPoolIsNull() {
        StrictMapVariant variant = CapabilityDetector.detectStrictMapVariant(null);
        assertEquals(StrictMapVariant.UNKNOWN, variant);
    }

    @Test
    public void ambiguityInstanceFieldTakesPrecedenceOverLegacyClass() throws Exception {
        // If both exist (edge case), AMBIGUITY_INSTANCE 字段变体 should be detected first
        ClassPool cp = new ClassPool(false);
        CtClass strictMap = cp.makeClass(STRICT_MAP);
        strictMap.addField(CtField.make(
                "private static final int AMBIGUITY_INSTANCE = 0;", strictMap));
        cp.makeClass(STRICT_MAP_AMBIGUITY);

        StrictMapVariant variant = CapabilityDetector.detectStrictMapVariant(cp);
        assertEquals(StrictMapVariant.AMBIGUITY_INSTANCE_FIELD, variant);
    }

    // ===== classExists =====

    @Test
    public void classExists_trueForPresentClass() throws Exception {
        ClassPool cp = new ClassPool(false);
        cp.makeClass("com.test.Foo");
        assertTrue(CapabilityDetector.classExists(cp, "com.test.Foo"));
    }

    @Test
    public void classExists_falseForAbsentClass() {
        ClassPool cp = new ClassPool(false);
        assertFalse(CapabilityDetector.classExists(cp, "com.test.NoSuchClass"));
    }

    @Test
    public void classExists_falseForNullPool() {
        assertFalse(CapabilityDetector.classExists(null, "com.test.Foo"));
    }

    // ===== hasField =====

    @Test
    public void hasField_trueForExistingField() throws Exception {
        ClassPool cp = new ClassPool(false);
        CtClass ct = cp.makeClass("com.test.Bar");
        ct.addField(CtField.make("private int value;", ct));
        assertTrue(CapabilityDetector.hasField(cp, "com.test.Bar", "value"));
    }

    @Test
    public void hasField_falseForMissingField() throws Exception {
        ClassPool cp = new ClassPool(false);
        cp.makeClass("com.test.Baz");
        assertFalse(CapabilityDetector.hasField(cp, "com.test.Baz", "nonExistent"));
    }

    @Test
    public void hasField_falseForMissingClass() {
        ClassPool cp = new ClassPool(false);
        assertFalse(CapabilityDetector.hasField(cp, "com.test.NoSuchClass", "field"));
    }

    @Test
    public void hasField_falseForNullPool() {
        assertFalse(CapabilityDetector.hasField(null, "com.test.Foo", "field"));
    }

    // ===== hasMybatisPlus =====

    @Test
    public void hasMybatisPlus_trueWhenClassExists() throws Exception {
        ClassPool cp = new ClassPool(false);
        cp.makeClass(MYBATIS_CONFIG);
        assertTrue(CapabilityDetector.hasMybatisPlus(cp));
    }

    @Test
    public void hasMybatisPlus_falseWhenClassMissing() {
        ClassPool cp = new ClassPool(false);
        assertFalse(CapabilityDetector.hasMybatisPlus(cp));
    }
}
