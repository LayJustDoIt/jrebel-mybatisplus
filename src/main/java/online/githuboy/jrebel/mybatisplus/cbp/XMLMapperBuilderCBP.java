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
 *   <li>{@code cleanupNamespace(XNode context)} — removes only the XML-owned
 *       MappedStatements, ResultMaps, ParameterMaps, KeyGenerators, and
 *       SqlFragments that are declared in the current mapper XML.  Entries
 *       injected by MyBatis-Plus SqlInjector (selectById, selectPage, insert,
 *       updateById, ...) are preserved because they share the namespace prefix
 *       but are NOT declared in the XML.</li>
 * </ol>
 *
 * <p>The owned IDs are extracted directly from the {@code <mapper>} XNode passed
 * to {@code configurationElement}.  No {@code Class.forName} or Mapper interface
 * loading is performed.
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
            addRemoveOwnedKeysMethod(ctClass);
            addRemoveLoadedResourceMethod(ctClass);
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
                    // Pass the 'context' XNode (parameter of configurationElement) to cleanupNamespace
                    m.replace("{$_=$proceed($$);this.clearInCompleteStatement();this.cleanupNamespace(context);}");
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

    /**
     * Precise cleanup: removes only entries that are declared in the current
     * mapper XML.  MyBatis-Plus injected statements (selectById, selectPage,
     * insert, updateById, deleteById, ...) are preserved because they are not
     * declared in the XML and therefore not in the owned-ID lists.
     *
     * <p>Owned IDs are extracted from the XNode using the same XPath expressions
     * that MyBatis itself uses in {@code configurationElement}:
     * <ul>
     *   <li>Statements: {@code "select|insert|update|delete"}</li>
     *   <li>ResultMaps: {@code "/mapper/resultMap"}</li>
     *   <li>Sql fragments: {@code "/mapper/sql"}</li>
     *   <li>ParameterMaps: {@code "/mapper/parameterMap"}</li>
     * </ul>
     *
     * <p>For each {@code <insert>} or {@code <update>} that contains a
     * {@code <selectKey>} child, MyBatis generates an additional MappedStatement
     * and a KeyGenerator keyed by {@code statementId + "!selectKey"}.  These
     * are also cleaned up precisely.
     */
    private void addCleanupNamespaceMethod(CtClass ctClass) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(
                "    public void cleanupNamespace(org.apache.ibatis.parsing.XNode context) {\n" +
                "        String namespace = builderAssistant.getCurrentNamespace();\n" +
                "        if (namespace == null || namespace.isEmpty()) {\n" +
                "            return;\n" +
                "        }\n" +
                "        if (!" + Constants.SqlMapReloaderClass + ".isReloading()) {\n" +
                "            return;\n" +
                "        }\n" +
                "        if (context == null) {\n" +
                "            return;\n" +
                "        }\n" +
                "        logger.info(\"[JRebel MyBatisPlus] Cleaning up XML-owned entries for namespace: {}\", namespace);\n" +
                "        try {\n" +
                "            java.util.List statementIds = new java.util.ArrayList();\n" +
                "            java.util.List resultMapIds = new java.util.ArrayList();\n" +
                "            java.util.List sqlFragmentIds = new java.util.ArrayList();\n" +
                "            java.util.List parameterMapIds = new java.util.ArrayList();\n" +
                "            java.util.List keyGeneratorIds = new java.util.ArrayList();\n" +
                "\n" +
                "            // Extract statement IDs (select|insert|update|delete)\n" +
                "            java.util.List stmtNodes = context.evalNodes(\"select|insert|update|delete\");\n" +
                "            for (int i = 0; i < stmtNodes.size(); i++) {\n" +
                "                org.apache.ibatis.parsing.XNode stmtNode =\n" +
                "                    (org.apache.ibatis.parsing.XNode) stmtNodes.get(i);\n" +
                "                String stmtId = stmtNode.getStringAttribute(\"id\");\n" +
                "                if (stmtId != null && !stmtId.isEmpty()) {\n" +
                "                    statementIds.add(stmtId);\n" +
                "                    // Check for selectKey child (insert/update only)\n" +
                "                    java.util.List selectKeyNodes = stmtNode.evalNodes(\"selectKey\");\n" +
                "                    if (!selectKeyNodes.isEmpty()) {\n" +
                "                        // MyBatis generates MappedStatement + KeyGenerator\n" +
                "                        // keyed by stmtId + \"!selectKey\"\n" +
                "                        String selectKeyStmtId = stmtId + \"!selectKey\";\n" +
                "                        statementIds.add(selectKeyStmtId);\n" +
                "                        keyGeneratorIds.add(selectKeyStmtId);\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            // Extract resultMap IDs\n" +
                "            java.util.List resultMapNodes = context.evalNodes(\"/mapper/resultMap\");\n" +
                "            for (int i = 0; i < resultMapNodes.size(); i++) {\n" +
                "                org.apache.ibatis.parsing.XNode rmNode =\n" +
                "                    (org.apache.ibatis.parsing.XNode) resultMapNodes.get(i);\n" +
                "                String rmId = rmNode.getStringAttribute(\"id\");\n" +
                "                if (rmId != null && !rmId.isEmpty()) {\n" +
                "                    resultMapIds.add(rmId);\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            // Extract sql fragment IDs\n" +
                "            java.util.List sqlNodes = context.evalNodes(\"/mapper/sql\");\n" +
                "            for (int i = 0; i < sqlNodes.size(); i++) {\n" +
                "                org.apache.ibatis.parsing.XNode sqlNode =\n" +
                "                    (org.apache.ibatis.parsing.XNode) sqlNodes.get(i);\n" +
                "                String sqlId = sqlNode.getStringAttribute(\"id\");\n" +
                "                if (sqlId != null && !sqlId.isEmpty()) {\n" +
                "                    sqlFragmentIds.add(sqlId);\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            // Extract parameterMap IDs\n" +
                "            java.util.List paramMapNodes = context.evalNodes(\"/mapper/parameterMap\");\n" +
                "            for (int i = 0; i < paramMapNodes.size(); i++) {\n" +
                "                org.apache.ibatis.parsing.XNode pmNode =\n" +
                "                    (org.apache.ibatis.parsing.XNode) paramMapNodes.get(i);\n" +
                "                String pmId = pmNode.getStringAttribute(\"id\");\n" +
                "                if (pmId != null && !pmId.isEmpty()) {\n" +
                "                    parameterMapIds.add(pmId);\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            // Remove only owned entries from each Configuration collection\n" +
                "            removeOwnedKeys(\"mappedStatements\", namespace, statementIds);\n" +
                "            removeOwnedKeys(\"resultMaps\", namespace, resultMapIds);\n" +
                "            removeOwnedKeys(\"parameterMaps\", namespace, parameterMapIds);\n" +
                "            removeOwnedKeys(\"keyGenerators\", namespace, keyGeneratorIds);\n" +
                "            removeOwnedKeys(\"sqlFragments\", namespace, sqlFragmentIds);\n" +
                "\n" +
                "            // Remove this resource from loadedResources (precise: only current resource)\n" +
                "            removeLoadedResource(resource);\n" +
                "        } catch (Exception e) {\n" +
                "            logger.warn(\"[JRebel MyBatisPlus] Failed to cleanup namespace: {}\", namespace, e);\n" +
                "        }\n" +
                "    }", ctClass));
    }

    /**
     * Remove only the exact keys listed in {@code ids} from the named
     * Configuration map field.  Each short ID is combined with the namespace
     * to form the fully-qualified key ({@code namespace + "." + id}).  Entries
     * whose key is not in the owned-ID list are preserved.
     *
     * <p>This replaces the old prefix-based {@code cleanupMapByKeyPrefix} which
     * deleted ALL entries starting with {@code namespace + "."}, including
     * MyBatis-Plus injected statements.
     */
    private void addRemoveOwnedKeysMethod(CtClass ctClass) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(
                "    private void removeOwnedKeys(String fieldName, String namespace, java.util.List ids) {\n" +
                "        if (ids == null || ids.isEmpty()) {\n" +
                "            return;\n" +
                "        }\n" +
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
                "            String prefix = namespace + \".\";\n" +
                "            int removed = 0;\n" +
                "            synchronized (map) {\n" +
                "                java.util.Iterator it = ids.iterator();\n" +
                "                while (it.hasNext()) {\n" +
                "                    String id = (String) it.next();\n" +
                "                    String fqn = prefix + id;\n" +
                "                    if (map.containsKey(fqn)) {\n" +
                "                        map.remove(fqn);\n" +
                "                        removed++;\n" +
                "                    }\n" +
                "                }\n" +
                "                if (removed > 0) {\n" +
                "                    logger.info(\"[JRebel MyBatisPlus] Removed {} owned entries from {}\",\n" +
                "                        java.lang.Integer.valueOf(removed), fieldName);\n" +
                "                }\n" +
                "            }\n" +
                "        } catch (Exception e) {\n" +
                "            logger.warn(\"[JRebel MyBatisPlus] removeOwnedKeys failed for field: {}\", fieldName, e);\n" +
                "        }\n" +
                "    }", ctClass));
    }

    /**
     * Remove only the current mapper resource from {@code loadedResources}.
     * This allows {@code XMLMapperBuilder.parse()} to re-parse the resource
     * without clearing the entire set.
     *
     * <p>Note: The {@code synchronized} block was removed because Javassist's
     * internal compiler could not balance the JVM stack when a {@code synchronized}
     * block with a single method-call body was combined with early returns inside
     * the surrounding {@code try-catch} (error: "inconsistent stack height 3").
     * During a JRebel reload the operation is single-threaded, so the absence of
     * synchronization is acceptable.
     */
    private void addRemoveLoadedResourceMethod(CtClass ctClass) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(
                "    private void removeLoadedResource(String resource) {\n" +
                "        if (resource == null || resource.isEmpty()) {\n" +
                "            return;\n" +
                "        }\n" +
                "        try {\n" +
                "            java.lang.reflect.Field field = findFieldInHierarchy(configuration.getClass(), \"loadedResources\");\n" +
                "            if (field == null) {\n" +
                "                return;\n" +
                "            }\n" +
                "            field.setAccessible(true);\n" +
                "            Object setObj = field.get(configuration);\n" +
                "            if (!(setObj instanceof java.util.Set)) {\n" +
                "                return;\n" +
                "            }\n" +
                "            java.util.Set loadedRes = (java.util.Set) setObj;\n" +
                "            loadedRes.remove(resource);\n" +
                "        } catch (Exception e) {\n" +
                "            logger.warn(\"[JRebel MyBatisPlus] removeLoadedResource failed for: {}\", resource, e);\n" +
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
