package online.githuboy.jrebel.mybatisplus.compatibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for mapper namespace-related operations.
 *
 * <p><b>定位说明：</b>本类是 runtime Javassist exact-cleanup 语义的
 * 纯 Java test mirror，用于表达和测试 namespace / exact-key cleanup 规则。
 * 实际 runtime 逻辑由 {@code XMLMapperBuilderCBP} 注入的 Javassist 代码执行，
 * 两者应保持语义一致。本类不被 runtime 调用，也不被增强后的 MyBatis 类依赖。
 *
 * @author TRAE
 */
public final class NamespaceUtil {

    private NamespaceUtil() {
    }

    /**
     * Build the fully-qualified key for a short ID under a namespace.
     * e.g. {@code "com.example.UserMapper"}, {@code "selectById"}
     * → {@code "com.example.UserMapper.selectById"}
     */
    private static String fqn(String namespace, String id) {
        return namespace + "." + id;
    }

    /**
     * Build the prefix used to match statement IDs belonging to a namespace.
     * e.g. {@code "com.example.UserMapper"} → {@code "com.example.UserMapper."}
     *
     * @param namespace the mapper namespace
     * @return the prefix, or empty string if namespace is null/empty
     */
    public static String buildPrefix(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            return "";
        }
        return namespace + ".";
    }

    /**
     * Check whether a key (statement ID, resultMap ID, etc.) belongs to
     * the given namespace.
     *
     * @param key       the statement/resultMap ID
     * @param namespace the mapper namespace
     * @return true if the key starts with {@code namespace + "."}
     */
    public static boolean belongsToNamespace(String key, String namespace) {
        if (key == null || namespace == null || namespace.isEmpty()) {
            return false;
        }
        return key.startsWith(namespace + ".");
    }

    /**
     * Check whether a key belongs to a namespace using a pre-built prefix.
     *
     * @param key    the statement/resultMap ID
     * @param prefix the namespace prefix (e.g. "com.example.UserMapper.")
     * @return true if the key starts with the prefix
     */
    public static boolean matchesPrefix(String key, String prefix) {
        if (key == null || prefix == null || prefix.isEmpty()) {
            return false;
        }
        return key.startsWith(prefix);
    }

    /**
     * Extract the short name from a statement ID.
     * e.g. {@code "com.example.UserMapper.selectById"} → {@code "selectById"}
     *
     * @param statementId the full statement ID
     * @return the short name, or the original if no dot is present
     */
    public static String getShortName(String statementId) {
        if (statementId == null) {
            return null;
        }
        int lastDot = statementId.lastIndexOf('.');
        if (lastDot < 0) {
            return statementId;
        }
        return statementId.substring(lastDot + 1);
    }

    // ===== XML ownership-based precise cleanup =====

    /**
     * Remove only the exact keys listed in {@code shortIds} from the given map.
     * Entries whose key is not in the ownership list are preserved.
     *
     * <p>This is the precise replacement for the old prefix-based
     * {@code cleanupMapByKeyPrefix}: MyBatis-Plus injected statements such as
     * {@code selectById}/{@code selectPage} share the same namespace prefix
     * but are NOT owned by the XML, so they must survive a reload.
     *
     * @param map        the target Configuration collection (mappedStatements,
     *                   resultMaps, parameterMaps, keyGenerators, sqlFragments)
     * @param namespace  the current mapper namespace
     * @param shortIds   the short IDs (e.g. "selectByTest") owned by the XML
     * @return number of entries removed
     */
    public static int removeOwnedKeys(Map<String, ?> map, String namespace, List<String> shortIds) {
        if (map == null || namespace == null || namespace.isEmpty() || shortIds == null || shortIds.isEmpty()) {
            return 0;
        }
        int removed = 0;
        synchronized (map) {
            for (String id : shortIds) {
                String key = fqn(namespace, id);
                if (map.containsKey(key)) {
                    map.remove(key);
                    removed++;
                }
            }
        }
        return removed;
    }

    /**
     * Build a full-qualified list of keys for a set of short IDs under a namespace.
     *
     * @param namespace the mapper namespace
     * @param shortIds  the short IDs
     * @return list of {@code namespace.id}
     */
    public static List<String> toFqnList(String namespace, List<String> shortIds) {
        List<String> result = new ArrayList<>();
        if (namespace == null || shortIds == null) {
            return result;
        }
        for (String id : shortIds) {
            if (id != null && !id.isEmpty()) {
                result.add(fqn(namespace, id));
            }
        }
        return result;
    }
}
