package online.githuboy.jrebel.mybatisplus.compatibility;

/**
 * Utility for mapper namespace-related operations.
 * <p>
 * Extracted from Javassist-generated code so it can be unit-tested
 * independently of the JRebel runtime.
 *
 * @author TRAE
 */
public final class NamespaceUtil {

    private NamespaceUtil() {
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
}
