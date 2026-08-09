package online.githuboy.jrebel.mybatisplus.compatibility;

import org.zeroturnaround.bundled.javassist.ClassPool;
import org.zeroturnaround.bundled.javassist.CtClass;
import org.zeroturnaround.bundled.javassist.NotFoundException;

/**
 * Runtime structure detector for MyBatis-Plus / MyBatis compatibility.
 * <p>
 * Instead of hardcoding version numbers, this class inspects the actual
 * class structure available in the {@link ClassPool} to decide which
 * Javassist enhancement strategy should be applied.
 *
 * @author TRAE
 */
public final class CapabilityDetector {

    private static final String STRICT_MAP_CLASS =
            "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap";
    private static final String STRICT_MAP_AMBIGUITY_CLASS =
            "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap$Ambiguity";
    private static final String MYBATIS_CONFIGURATION_CLASS =
            "com.baomidou.mybatisplus.core.MybatisConfiguration";

    private CapabilityDetector() {
    }

    /**
     * StrictMap structural variant across MyBatis-Plus versions.
     */
    public enum StrictMapVariant {
        /** Legacy: Ambiguity is a public inner class (MyBatis-Plus &lt; 3.5.7). */
        LEGACY_AMBIGUITY_CLASS,
        /** Current: AMBIGUITY_INSTANCE 字段变体 (MyBatis-Plus &ge; 3.5.7). */
        AMBIGUITY_INSTANCE_FIELD,
        /** Unknown structure; enhancement should be skipped safely. */
        UNKNOWN
    }

    /**
     * Detect the StrictMap variant by inspecting the class structure.
     *
     * @param cp the Javassist ClassPool
     * @return the detected variant, never {@code null}
     */
    public static StrictMapVariant detectStrictMapVariant(ClassPool cp) {
        if (cp == null) {
            return StrictMapVariant.UNKNOWN;
        }
        CtClass strictMap = cp.getOrNull(STRICT_MAP_CLASS);
        if (strictMap == null) {
            return StrictMapVariant.UNKNOWN;
        }
        // Check for AMBIGUITY_INSTANCE 字段变体 first (3.5.7+)
        try {
            strictMap.getDeclaredField("AMBIGUITY_INSTANCE");
            return StrictMapVariant.AMBIGUITY_INSTANCE_FIELD;
        } catch (NotFoundException e) {
            // Field not found, fall through
        }
        // Check for legacy Ambiguity inner class
        CtClass ambiguityClass = cp.getOrNull(STRICT_MAP_AMBIGUITY_CLASS);
        if (ambiguityClass != null) {
            return StrictMapVariant.LEGACY_AMBIGUITY_CLASS;
        }
        return StrictMapVariant.UNKNOWN;
    }

    /**
     * Check whether a class exists in the class pool.
     *
     * @param cp       the Javassist ClassPool
     * @param className FQN of the class
     * @return true if the class is present
     */
    public static boolean classExists(ClassPool cp, String className) {
        return cp != null && cp.getOrNull(className) != null;
    }

    /**
     * Check whether a class has a declared field.
     *
     * @param cp        the Javassist ClassPool
     * @param className FQN of the class
     * @param fieldName name of the field
     * @return true if the field exists
     */
    public static boolean hasField(ClassPool cp, String className, String fieldName) {
        if (cp == null) {
            return false;
        }
        CtClass ct = cp.getOrNull(className);
        if (ct == null) {
            return false;
        }
        try {
            return ct.getDeclaredField(fieldName) != null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Check whether MybatisConfiguration is present (plugin dependency check).
     *
     * @param cp the Javassist ClassPool
     * @return true if MybatisConfiguration is on the classpath
     */
    public static boolean hasMybatisPlus(ClassPool cp) {
        return classExists(cp, MYBATIS_CONFIGURATION_CLASS);
    }
}
