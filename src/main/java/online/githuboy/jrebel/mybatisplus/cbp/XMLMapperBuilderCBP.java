package online.githuboy.jrebel.mybatisplus.cbp;

import org.zeroturnaround.bundled.javassist.*;
import org.zeroturnaround.bundled.javassist.expr.ExprEditor;
import org.zeroturnaround.bundled.javassist.expr.MethodCall;
import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.integration.support.JavassistClassBytecodeProcessor;

/**
 * XMLMapperBuilder class hook.
 * <p>
 * Adds two capabilities:
 * <ol>
 *   <li>{@code clearInCompleteStatement()} — removes incomplete statement builders
 *       for the current namespace (prevents stale partial-parse state).</li>
 *   <li>{@code cleanupNamespace()} — removes already-registered MappedStatements,
 *       ResultMaps, ParameterMaps, KeyGenerators, and SqlFragments whose IDs belong
 *       to the current namespace.  Called only during a reload, this ensures a
 *       clean slate even if a previous (possibly broken) XML parse left partial
 *       data behind.</li>
 * </ol>
 *
 * @author suchu
 * @author TRAE
 * @since 2019/6/26 11:25
 */
public class XMLMapperBuilderCBP extends JavassistClassBytecodeProcessor {

    @Override
    public void process(ClassPool cp, ClassLoader cl, CtClass ctClass) throws Exception {
        LoggerFactory.getLogger("MyBatisPlus").info(
                "[JRebel MyBatisPlus] XMLMapperBuilderCBP processing class: " + ctClass.getName());
        try {
            ctClass.addField(CtField.make(
                    "private static final org.slf4j.Logger logger = " +
                    "org.slf4j.LoggerFactory.getLogger(org.apache.ibatis.builder.xml.XMLMapperBuilder.class);",
                    ctClass));
            // Order matters: helper methods must be added BEFORE methods that call them
            addFindFieldInHierarchyMethod(ctClass);
            addCleanupMapByKeyPrefixMethod(ctClass);
            addClearMethod(ctClass);
            addCleanupNamespaceMethod(ctClass);
            hookConfigurationElementMethod(ctClass);
            LoggerFactory.getLogger("MyBatisPlus").info(
                    "[JRebel MyBatisPlus] XMLMapperBuilder enhanced successfully");
        } catch (Exception e) {
            LoggerFactory.getLogger("MyBatisPlus").warn(
                    "[JRebel MyBatisPlus] Failed to enhance XMLMapperBuilder. " +
                    "Mapper XML reload may not work correctly. Reason: " + e.getMessage());
            throw e;
        }
    }

    private void hookConfigurationElementMethod(CtClass ctClass) throws NotFoundException, CannotCompileException {
        ctClass.getDeclaredMethod("configurationElement").instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if ("setCurrentNamespace".equals(m.getMethodName())) {
                    m.replace("{$_=$proceed($$);this.clearInCompleteStatement();this.cleanupNamespace();}");
                }
            }
        });
    }

    private void addClearMethod(CtClass ctClass) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(
                "    public void clearInCompleteStatement() {\n" +
                "        try {\n" +
                "            java.util.Collection incompleteStatements = this.configuration.getIncompleteStatements();\n" +
                "            synchronized (incompleteStatements) {\n" +
                "                java.util.Iterator iterator = incompleteStatements.iterator();\n" +
                "                while (iterator.hasNext()) {\n" +
                "                    org.apache.ibatis.builder.xml.XMLStatementBuilder statementBuilder = " +
                "                        (org.apache.ibatis.builder.xml.XMLStatementBuilder) iterator.next();\n" +
                "                    try {\n" +
                "                        java.lang.reflect.Field field = statementBuilder.getClass().getDeclaredField(\"builderAssistant\");\n" +
                "                        field.setAccessible(true);\n" +
                "                        org.apache.ibatis.builder.MapperBuilderAssistant tempBuilderAssistant = " +
                "                            (org.apache.ibatis.builder.MapperBuilderAssistant) field.get(statementBuilder);\n" +
                "                        if (null != tempBuilderAssistant &&\n" +
                "                            tempBuilderAssistant.getCurrentNamespace().equals(builderAssistant.getCurrentNamespace())) {\n" +
                "                            logger.info(\"[JRebel MyBatisPlus] Cleaning {}'s incomplete statement\",\n" +
                "                                builderAssistant.getCurrentNamespace());\n" +
                "                            iterator.remove();\n" +
                "                        }\n" +
                "                    } catch (Exception e) {\n" +
                "                        logger.warn(\"[JRebel MyBatisPlus] clearInCompleteStatement error\", e);\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        } catch (Exception e) {\n" +
                "            logger.warn(\"[JRebel MyBatisPlus] clearInCompleteStatement failed\", e);\n" +
                "        }\n" +
                "    }", ctClass));
    }

    private void addCleanupNamespaceMethod(CtClass ctClass) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(
                "    public void cleanupNamespace() {\n" +
                "        String namespace = builderAssistant.getCurrentNamespace();\n" +
                "        if (namespace == null || namespace.isEmpty()) {\n" +
                "            return;\n" +
                "        }\n" +
                "        if (!" + Constants.SqlMapReloaderClass + ".isReloading()) {\n" +
                "            return;\n" +
                "        }\n" +
                "        String prefix = namespace + \".\";\n" +
                "        logger.info(\"[JRebel MyBatisPlus] Cleaning up namespace: {}\", namespace);\n" +
                "        try {\n" +
                "            cleanupMapByKeyPrefix(\"mappedStatements\", prefix);\n" +
                "            cleanupMapByKeyPrefix(\"resultMaps\", prefix);\n" +
                "            cleanupMapByKeyPrefix(\"parameterMaps\", prefix);\n" +
                "            cleanupMapByKeyPrefix(\"keyGenerators\", prefix);\n" +
                "            cleanupMapByKeyPrefix(\"sqlFragments\", prefix);\n" +
                "        } catch (Exception e) {\n" +
                "            logger.warn(\"[JRebel MyBatisPlus] Failed to cleanup namespace: {}\", namespace, e);\n" +
                "        }\n" +
                "    }", ctClass));
    }

    private void addCleanupMapByKeyPrefixMethod(CtClass ctClass) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(
                "    private void cleanupMapByKeyPrefix(String fieldName, String prefix) {\n" +
                "        try {\n" +
                "            java.lang.reflect.Field field = findFieldInHierarchy(configuration.getClass(), fieldName);\n" +
                "            if (field == null) {\n" +
                "                return;\n" +
                "            }\n" +
                "            field.setAccessible(true);\n" +
                "            Object mapObj = field.get(configuration);\n" +
                "            if (!(mapObj instanceof java.util.Map)) {\n" +
                "                return;\n" +
                "            }\n" +
                "            java.util.Map map = (java.util.Map) mapObj;\n" +
                "            synchronized (map) {\n" +
                "                java.util.Iterator it = map.keySet().iterator();\n" +
                "                int removed = 0;\n" +
                "                while (it.hasNext()) {\n" +
                "                    Object key = it.next();\n" +
                "                    if (key instanceof String && ((String) key).startsWith(prefix)) {\n" +
                "                        it.remove();\n" +
                "                        removed++;\n" +
                "                    }\n" +
                "                }\n" +
                "                if (removed > 0) {\n" +
                "                    logger.info(\"[JRebel MyBatisPlus] Cleaned {} entries from {}\", " +
                "                        java.lang.Integer.valueOf(removed), fieldName);\n" +
                "                }\n" +
                "            }\n" +
                "        } catch (Exception e) {\n" +
                "            logger.warn(\"[JRebel MyBatisPlus] cleanupMapByKeyPrefix failed for field: {}\", fieldName, e);\n" +
                "        }\n" +
                "    }", ctClass));
    }

    private void addFindFieldInHierarchyMethod(CtClass ctClass) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(
                "    private java.lang.reflect.Field findFieldInHierarchy(java.lang.Class clazz, String name) {\n" +
                "        java.lang.Class current = clazz;\n" +
                "        while (current != null) {\n" +
                "            try {\n" +
                "                return current.getDeclaredField(name);\n" +
                "            } catch (NoSuchFieldException e) {\n" +
                "                current = current.getSuperclass();\n" +
                "            }\n" +
                "        }\n" +
                "        return null;\n" +
                "    }", ctClass));
    }
}
