package online.githuboy.jrebel.mybatisplus.cbp;

import online.githuboy.jrebel.mybatisplus.compatibility.CapabilityDetector;
import online.githuboy.jrebel.mybatisplus.compatibility.CapabilityDetector.StrictMapVariant;
import org.zeroturnaround.bundled.javassist.CannotCompileException;
import org.zeroturnaround.bundled.javassist.ClassPool;
import org.zeroturnaround.bundled.javassist.CtClass;
import org.zeroturnaround.bundled.javassist.expr.ExprEditor;
import org.zeroturnaround.bundled.javassist.expr.MethodCall;
import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.integration.support.JavassistClassBytecodeProcessor;

/**
 * Process MybatisConfiguration$StrictMap class.
 * <p>
 * Hooks the {@code put} method so that during a mapper reload, the
 * {@code containsKey} check is bypassed (allowing overwrite of existing
 * statements) and the {@code get} method returns {@code null} for non-Ambiguity
 * values (so that {@code isResourceLoaded} returns false).
 * <p>
 * Uses {@link CapabilityDetector} to select the correct enhancement variant
 * based on runtime class structure, not version strings.
 *
 * @author suchu
 * @author TRAE
 */
public class StrictMapCBP extends JavassistClassBytecodeProcessor {

    private static final String AMBIGUITY_FQN =
            "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap$Ambiguity";

    private boolean proceed = false;

    public StrictMapCBP() {
    }

    @Override
    public void process(ClassPool classPool, ClassLoader classLoader, CtClass ctClass) throws Exception {
        if (proceed) {
            return;
        }
        try {
            enhanceStrictMap(classPool, ctClass);
            proceed = true;
        } catch (Exception e) {
            LoggerFactory.getLogger("MyBatisPlus").warn(
                    "[JRebel MyBatisPlus] StrictMap structure not recognized. " +
                    "Skip StrictMap enhancement. Reason: " + e.getMessage());
        }
    }

    private void enhanceStrictMap(ClassPool classPool, CtClass ctClass) throws Exception {
        StrictMapVariant variant = CapabilityDetector.detectStrictMapVariant(classPool);
        LoggerFactory.getLogger("MyBatisPlus").info(
                "[JRebel MyBatisPlus] StrictMap variant detected: " + variant);

        if (variant == StrictMapVariant.UNKNOWN) {
            LoggerFactory.getLogger("MyBatisPlus").warn(
                    "[JRebel MyBatisPlus] StrictMap structure not recognized. " +
                    "Skip legacy StrictMap enhancement.");
            return;
        }

        final String getReplaceBody = buildGetReplaceBody(variant);

        ctClass.getDeclaredMethod("put").instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if ("containsKey".equals(m.getMethodName())) {
                    m.replace("{  if (" + Constants.SqlMapReloaderClass
                            + ".isReloading())    $_ = false;  else    $_ = $proceed($$);}");
                } else if ("get".equals(m.getMethodName())) {
                    m.replace(getReplaceBody);
                }
            }
        });
    }

    /**
     * Build the Javassist replacement body for the {@code get} method call
     * inside StrictMap.put, based on the detected variant.
     */
    private String buildGetReplaceBody(StrictMapVariant variant) {
        switch (variant) {
            case LEGACY_AMBIGUITY_CLASS:
                // MyBatis-Plus < 3.5.7: Ambiguity is a public inner class
                return "{  $_ = $proceed($$);  if (" + Constants.SqlMapReloaderClass
                        + ".isReloading()       && !($_ instanceof " + AMBIGUITY_FQN
                        + "))    $_ = null;}";
            case AMBIGUITY_INSTANCE_FIELD:
                // MyBatis-Plus >= 3.5.7: AMBIGUITY_INSTANCE 字段变体
                return "{  $_ = $proceed($$);  if (" + Constants.SqlMapReloaderClass
                        + ".isReloading()       && !($_ == AMBIGUITY_INSTANCE))    $_ = null;}";
            default:
                // Should not reach here (UNKNOWN is handled by caller)
                return "{  $_ = $proceed($$);}";
        }
    }
}
