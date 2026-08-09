package online.githuboy.jrebel.mybatisplus.compatibility;

import org.junit.Test;
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
}
