package online.githuboy.jrebel.mybatisplus.compatibility;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NamespaceUtil}.
 *
 * @author TRAE
 */
public class NamespaceUtilTest {

    @Test
    public void buildPrefix_normalNamespace() {
        assertEquals("com.example.UserMapper.", NamespaceUtil.buildPrefix("com.example.UserMapper"));
    }

    @Test
    public void buildPrefix_nullNamespace() {
        assertEquals("", NamespaceUtil.buildPrefix(null));
    }

    @Test
    public void buildPrefix_emptyNamespace() {
        assertEquals("", NamespaceUtil.buildPrefix(""));
    }

    @Test
    public void belongsToNamespace_matchingKey() {
        assertTrue(NamespaceUtil.belongsToNamespace(
                "com.example.UserMapper.selectById", "com.example.UserMapper"));
    }

    @Test
    public void belongsToNamespace_nonMatchingKey() {
        assertFalse(NamespaceUtil.belongsToNamespace(
                "com.example.OrderMapper.selectById", "com.example.UserMapper"));
    }

    @Test
    public void belongsToNamespace_prefixOnlyNoId() {
        assertFalse(NamespaceUtil.belongsToNamespace(
                "com.example.UserMapper", "com.example.UserMapper"));
    }

    @Test
    public void belongsToNamespace_nullKey() {
        assertFalse(NamespaceUtil.belongsToNamespace(null, "com.example.UserMapper"));
    }

    @Test
    public void belongsToNamespace_nullNamespace() {
        assertFalse(NamespaceUtil.belongsToNamespace("com.example.UserMapper.selectById", null));
    }

    @Test
    public void matchesPrefix_matching() {
        assertTrue(NamespaceUtil.matchesPrefix(
                "com.example.UserMapper.selectById", "com.example.UserMapper."));
    }

    @Test
    public void matchesPrefix_nonMatching() {
        assertFalse(NamespaceUtil.matchesPrefix(
                "com.example.OrderMapper.selectById", "com.example.UserMapper."));
    }

    @Test
    public void matchesPrefix_nullKey() {
        assertFalse(NamespaceUtil.matchesPrefix(null, "com.example.UserMapper."));
    }

    @Test
    public void matchesPrefix_emptyPrefix() {
        assertFalse(NamespaceUtil.matchesPrefix("com.example.UserMapper.selectById", ""));
    }

    @Test
    public void getShortName_normalId() {
        assertEquals("selectById", NamespaceUtil.getShortName("com.example.UserMapper.selectById"));
    }

    @Test
    public void getShortName_noDot() {
        assertEquals("selectById", NamespaceUtil.getShortName("selectById"));
    }

    @Test
    public void getShortName_null() {
        assertNull(NamespaceUtil.getShortName(null));
    }

    @Test
    public void getShortName_multipleDots() {
        assertEquals("selectById", NamespaceUtil.getShortName("com.example.v2.UserMapper.selectById"));
    }

    // ===== removeOwnedKeys: precise cleanup =====

    /**
     * Simulates the real-world scenario: a namespace contains both
     * XML-declared statements AND MyBatis-Plus injected statements.
     * After removeOwnedKeys, only the XML-owned ones should be removed.
     */
    @Test
    public void removeOwnedKeys_removesOnlyXmlOwned_preservesInjected() {
        Map<String, Object> map = new LinkedHashMap<>();
        // XML-owned statements
        map.put("com.example.UserMapper.selectByTest", new Object());
        map.put("com.example.UserMapper.updateByTest", new Object());
        // MyBatis-Plus injected statements (must be preserved)
        map.put("com.example.UserMapper.selectById", new Object());
        map.put("com.example.UserMapper.selectPage", new Object());
        map.put("com.example.UserMapper.selectList", new Object());
        map.put("com.example.UserMapper.insert", new Object());
        map.put("com.example.UserMapper.updateById", new Object());
        map.put("com.example.UserMapper.deleteById", new Object());

        List<String> ownedIds = Arrays.asList("selectByTest", "updateByTest");
        int removed = NamespaceUtil.removeOwnedKeys(map, "com.example.UserMapper", ownedIds);

        assertEquals(2, removed);
        assertFalse(map.containsKey("com.example.UserMapper.selectByTest"));
        assertFalse(map.containsKey("com.example.UserMapper.updateByTest"));
        // Injected statements must survive
        assertTrue(map.containsKey("com.example.UserMapper.selectById"));
        assertTrue(map.containsKey("com.example.UserMapper.selectPage"));
        assertTrue(map.containsKey("com.example.UserMapper.selectList"));
        assertTrue(map.containsKey("com.example.UserMapper.insert"));
        assertTrue(map.containsKey("com.example.UserMapper.updateById"));
        assertTrue(map.containsKey("com.example.UserMapper.deleteById"));
    }

    @Test
    public void removeOwnedKeys_emptyIdList_removesNothing() {
        Map<String, Object> map = new HashMap<>();
        map.put("com.example.UserMapper.selectById", new Object());
        int removed = NamespaceUtil.removeOwnedKeys(map, "com.example.UserMapper",
                java.util.Collections.emptyList());
        assertEquals(0, removed);
        assertEquals(1, map.size());
    }

    @Test
    public void removeOwnedKeys_nullMap_returnsZero() {
        int removed = NamespaceUtil.removeOwnedKeys(null, "ns",
                Arrays.asList("a"));
        assertEquals(0, removed);
    }

    @Test
    public void removeOwnedKeys_nullNamespace_returnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("ns.a", new Object());
        int removed = NamespaceUtil.removeOwnedKeys(map, null, Arrays.asList("a"));
        assertEquals(0, removed);
        assertEquals(1, map.size());
    }

    @Test
    public void removeOwnedKeys_nullIds_returnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("ns.a", new Object());
        int removed = NamespaceUtil.removeOwnedKeys(map, "ns", null);
        assertEquals(0, removed);
        assertEquals(1, map.size());
    }

    @Test
    public void removeOwnedKeys_keyNotPresent_returnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("com.example.UserMapper.selectById", new Object());
        int removed = NamespaceUtil.removeOwnedKeys(map, "com.example.UserMapper",
                Arrays.asList("nonExistentStmt"));
        assertEquals(0, removed);
        assertEquals(1, map.size());
    }

    @Test
    public void removeOwnedKeys_selectKeyStatementId() {
        // selectKey generates MappedStatement with key: stmtId + "!selectKey"
        Map<String, Object> map = new HashMap<>();
        map.put("com.example.UserMapper.insertUser", new Object());
        map.put("com.example.UserMapper.insertUser!selectKey", new Object());

        List<String> ownedIds = Arrays.asList("insertUser", "insertUser!selectKey");
        int removed = NamespaceUtil.removeOwnedKeys(map, "com.example.UserMapper", ownedIds);

        assertEquals(2, removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void removeOwnedKeys_resultMapPreciseCleanup() {
        Map<String, Object> map = new HashMap<>();
        map.put("com.example.UserMapper.userResult", new Object());
        map.put("com.example.UserMapper.baseResult", new Object());
        // These are NOT resultMap entries but share the namespace — must survive
        map.put("com.example.UserMapper.selectById", new Object());

        List<String> ownedResultMapIds = Arrays.asList("userResult", "baseResult");
        int removed = NamespaceUtil.removeOwnedKeys(map, "com.example.UserMapper", ownedResultMapIds);

        assertEquals(2, removed);
        assertFalse(map.containsKey("com.example.UserMapper.userResult"));
        assertFalse(map.containsKey("com.example.UserMapper.baseResult"));
        assertTrue(map.containsKey("com.example.UserMapper.selectById"));
    }

    @Test
    public void removeOwnedKeys_sqlFragmentPreciseCleanup() {
        Map<String, Object> map = new HashMap<>();
        map.put("com.example.UserMapper.baseColumns", new Object());
        map.put("com.example.UserMapper.whereClause", new Object());
        // Injected statement must survive
        map.put("com.example.UserMapper.selectList", new Object());

        List<String> ownedSqlIds = Arrays.asList("baseColumns", "whereClause");
        int removed = NamespaceUtil.removeOwnedKeys(map, "com.example.UserMapper", ownedSqlIds);

        assertEquals(2, removed);
        assertFalse(map.containsKey("com.example.UserMapper.baseColumns"));
        assertFalse(map.containsKey("com.example.UserMapper.whereClause"));
        assertTrue(map.containsKey("com.example.UserMapper.selectList"));
    }

    @Test
    public void removeOwnedKeys_twoMappersIsolation() {
        // Modifying UserMapper's XML should NOT affect ProductMapper's entries
        Map<String, Object> map = new HashMap<>();
        // UserMapper entries
        map.put("com.example.UserMapper.selectByTest", new Object());
        map.put("com.example.UserMapper.selectById", new Object());
        // ProductMapper entries
        map.put("com.example.ProductMapper.selectByTest", new Object());
        map.put("com.example.ProductMapper.selectById", new Object());

        List<String> userOwnedIds = Arrays.asList("selectByTest");
        int removed = NamespaceUtil.removeOwnedKeys(map, "com.example.UserMapper", userOwnedIds);

        assertEquals(1, removed);
        assertFalse(map.containsKey("com.example.UserMapper.selectByTest"));
        assertTrue(map.containsKey("com.example.UserMapper.selectById"));
        // ProductMapper untouched
        assertTrue(map.containsKey("com.example.ProductMapper.selectByTest"));
        assertTrue(map.containsKey("com.example.ProductMapper.selectById"));
    }

    @Test
    public void removeOwnedKeys_multipleOwnedIds() {
        Map<String, Object> map = new HashMap<>();
        map.put("ns.a", new Object());
        map.put("ns.b", new Object());
        map.put("ns.c", new Object());
        map.put("ns.d", new Object());
        // Not owned
        map.put("ns.selectById", new Object());

        List<String> ownedIds = Arrays.asList("a", "b", "c", "d");
        int removed = NamespaceUtil.removeOwnedKeys(map, "ns", ownedIds);

        assertEquals(4, removed);
        assertTrue(map.containsKey("ns.selectById"));
        assertEquals(1, map.size());
    }

    // ===== toFqnList =====

    @Test
    public void toFqnList_normalIds() {
        List<String> result = NamespaceUtil.toFqnList("com.example.UserMapper",
                Arrays.asList("selectByTest", "updateByTest"));
        assertEquals(2, result.size());
        assertEquals("com.example.UserMapper.selectByTest", result.get(0));
        assertEquals("com.example.UserMapper.updateByTest", result.get(1));
    }

    @Test
    public void toFqnList_emptyIds() {
        List<String> result = NamespaceUtil.toFqnList("ns",
                java.util.Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void toFqnList_nullIds() {
        List<String> result = NamespaceUtil.toFqnList("ns", null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void toFqnList_nullNamespace() {
        List<String> result = NamespaceUtil.toFqnList(null,
                Arrays.asList("a", "b"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void toFqnList_skipsNullAndEmptyIds() {
        List<String> result = NamespaceUtil.toFqnList("ns",
                Arrays.asList("a", null, "", "b"));
        assertEquals(2, result.size());
        assertEquals("ns.a", result.get(0));
        assertEquals("ns.b", result.get(1));
    }

    @Test
    public void toFqnList_selectKeySuffix() {
        List<String> result = NamespaceUtil.toFqnList("com.example.UserMapper",
                Arrays.asList("insertUser!selectKey"));
        assertEquals(1, result.size());
        assertEquals("com.example.UserMapper.insertUser!selectKey", result.get(0));
    }
}
