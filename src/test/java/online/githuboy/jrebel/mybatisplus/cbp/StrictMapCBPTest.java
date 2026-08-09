package online.githuboy.jrebel.mybatisplus.cbp;

import online.githuboy.jrebel.mybatisplus.compatibility.CapabilityDetector.StrictMapVariant;
import org.junit.Test;
import org.zeroturnaround.bundled.javassist.ClassPool;
import org.zeroturnaround.bundled.javassist.CtClass;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StrictMapCBP}.
 * <p>
 * Focuses on the version-specific enhancement body generation logic
 * (the part most likely to break across MyBatis-Plus upgrades) and the
 * fail-safe behavior when the StrictMap structure is unrecognized.
 *
 * @author TRAE
 */
public class StrictMapCBPTest {

    private static final String AMBIGUITY_FQN =
            "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap$Ambiguity";
    private static final String RELOADER_CLASS = Constants.SqlMapReloaderClass;

    /**
     * Invoke the private {@code buildGetReplaceBody} via reflection.
     */
    private String buildGetReplaceBody(StrictMapVariant variant) throws Exception {
        StrictMapCBP cbp = new StrictMapCBP();
        Method m = StrictMapCBP.class.getDeclaredMethod("buildGetReplaceBody", StrictMapVariant.class);
        m.setAccessible(true);
        return (String) m.invoke(cbp, variant);
    }

    // ===== buildGetReplaceBody: version-specific body generation =====

    @Test
    public void legacyVariantBodyReferencesAmbiguityInnerClass() throws Exception {
        String body = buildGetReplaceBody(StrictMapVariant.LEGACY_AMBIGUITY_CLASS);
        assertNotNull(body);
        // Legacy variant must reference the Ambiguity inner class FQN
        assertTrue("Legacy body should reference Ambiguity FQN",
                body.contains(AMBIGUITY_FQN));
        assertTrue("Body should call isReloading()",
                body.contains("isReloading()"));
        // Legacy uses instanceof check
        assertTrue("Legacy body should use instanceof", body.contains("instanceof"));
    }

    @Test
    public void currentVariantBodyReferencesAmbiguityInstanceField() throws Exception {
        String body = buildGetReplaceBody(StrictMapVariant.AMBIGUITY_INSTANCE_FIELD);
        assertNotNull(body);
        // Current variant must reference AMBIGUITY_INSTANCE 字段变体
        assertTrue("Current body should reference AMBIGUITY_INSTANCE field",
                body.contains("AMBIGUITY_INSTANCE"));
        assertTrue("Body should call isReloading()",
                body.contains("isReloading()"));
        // Current uses == comparison, not instanceof
        assertFalse("Current body should NOT use instanceof",
                body.contains("instanceof"));
    }

    @Test
    public void unknownVariantBodyIsSafeFallback() throws Exception {
        String body = buildGetReplaceBody(StrictMapVariant.UNKNOWN);
        assertNotNull(body);
        // Fallback should just proceed without modification
        assertTrue("Unknown body should use $proceed",
                body.contains("$proceed($$)"));
        // Should NOT reference either variant-specific construct
        assertFalse("Unknown body should not reference Ambiguity FQN",
                body.contains(AMBIGUITY_FQN));
        assertFalse("Unknown body should not reference AMBIGUITY_INSTANCE",
                body.contains("AMBIGUITY_INSTANCE"));
    }

    @Test
    public void allVariantBodiesContainProceedCall() throws Exception {
        // Every variant body must delegate to the original method via $proceed
        for (StrictMapVariant v : StrictMapVariant.values()) {
            String body = buildGetReplaceBody(v);
            assertTrue("Variant " + v + " body must contain $proceed($$)",
                    body.contains("$proceed($$)"));
        }
    }

    @Test
    public void legacyAndCurrentBodiesBothGateOnIsReloading() throws Exception {
        // Both known variants must gate the null-assignment behind isReloading()
        String legacy = buildGetReplaceBody(StrictMapVariant.LEGACY_AMBIGUITY_CLASS);
        String current = buildGetReplaceBody(StrictMapVariant.AMBIGUITY_INSTANCE_FIELD);
        assertTrue(legacy.contains(RELOADER_CLASS + ".isReloading()"));
        assertTrue(current.contains(RELOADER_CLASS + ".isReloading()"));
    }

    // ===== Fail-safe: process() with unrecognized structure =====

    @Test
    public void processDoesNotThrowWhenStructureUnknown() throws Exception {
        // StrictMap class exists but has neither Ambiguity inner class nor AMBIGUITY_INSTANCE field.
        // UNKNOWN variant returns before any method instrumentation, so no put method is needed.
        ClassPool cp = new ClassPool(false);
        CtClass strictMap = cp.makeClass(
                "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap");

        StrictMapCBP cbp = new StrictMapCBP();
        // Should not throw — UNKNOWN variant must be skipped safely
        try {
            cbp.process(cp, null, strictMap);
        } catch (Exception e) {
            fail("process() must not throw for UNKNOWN variant, but threw: " + e);
        }
    }

    @Test
    public void processSkipsEnhancementWhenStrictMapMissing() throws Exception {
        // No StrictMap class at all in the pool
        ClassPool cp = new ClassPool(false);
        CtClass fake = cp.makeClass("com.test.Fake");

        StrictMapCBP cbp = new StrictMapCBP();
        try {
            cbp.process(cp, null, fake);
        } catch (Exception e) {
            fail("process() must not throw when StrictMap is missing, but threw: " + e);
        }
    }
}
