package online.githuboy.jrebel.mybatisplus;

import online.githuboy.jrebel.mybatisplus.cbp.*;
import online.githuboy.jrebel.mybatisplus.compatibility.ResourceVersionDetector;
import org.zeroturnaround.javarebel.*;

import java.io.IOException;
import java.util.Properties;

/**
 * Plugin Main entry
 *
 * @author suchu
 * @author andresluuk
 * @since 2019/5/9 17:56
 */
public class MybatisPlusPlugin implements Plugin {
    private static final Logger log = LoggerFactory.getLogger("MyBatisPlus");

    /**
     * MyBatis-Plus artifact coordinates used for pure-resource version detection.
     * Never call {@code Class.forName} on these classes here: that would load
     * the class before the CBP is registered and silently disable enhancement.
     */
    private static final String MP_CLASS_RESOURCE =
            "com/baomidou/mybatisplus/core/MybatisConfiguration.class";
    private static final String MP_POM_PROPERTIES =
            "META-INF/maven/com.baomidou/mybatis-plus-core/pom.properties";
    private static final String MYBATIS_CLASS_RESOURCE =
            "org/apache/ibatis/session/Configuration.class";
    private static final String MYBATIS_POM_PROPERTIES =
            "META-INF/maven/org.mybatis/mybatis/pom.properties";

    @Override
    public void preinit() {
        Properties p = new Properties();
        String version = "";
        try {
            p.load(getClass().getClassLoader().getResourceAsStream("META-INF/maven/online.githuboy/jr-mybatisplus/pom.properties"));
            version = p.getProperty("version");
        } catch (IOException e) {
            log.error("Can not read jr-mybatisplus/pom.properties:", e.getMessage());
        }
        log.infoEcho("[JRebel MyBatisPlus] Plugin initialized (version=" + version + ")");
        ClassLoader classLoader = MybatisPlusPlugin.class.getClassLoader();
        Integration integration = IntegrationFactory.getInstance();

        // 1. Register all ClassBytecodeProcessors FIRST.
        //    This must happen before any code path that may load the target
        //    classes (e.g. version detection). Once a class is loaded by the
        //    JVM, JRebel can no longer instrument its bytecode.
        configMybatisPlusProcessor(integration, classLoader);
        configMybatisProcessor(integration, classLoader);

        // 2. After CBP registration, it is safe to detect versions.
        //    ResourceVersionDetector uses ClassLoader.getResource only and
        //    never triggers class loading.
        logVersionInfo(classLoader);
    }

    private void logVersionInfo(ClassLoader classLoader) {
        try {
            String mpVersion = ResourceVersionDetector.detectVersion(
                    classLoader, MP_CLASS_RESOURCE, MP_POM_PROPERTIES);
            log.infoEcho("[JRebel MyBatisPlus] MyBatis-Plus detected: " + mpVersion);
        } catch (Throwable t) {
            log.infoEcho("[JRebel MyBatisPlus] MyBatis-Plus version not resolvable");
        }
        try {
            String mybatisVersion = ResourceVersionDetector.detectVersion(
                    classLoader, MYBATIS_CLASS_RESOURCE, MYBATIS_POM_PROPERTIES);
            log.infoEcho("[JRebel MyBatisPlus] MyBatis detected: " + mybatisVersion);
        } catch (Throwable t) {
            log.infoEcho("[JRebel MyBatisPlus] MyBatis version not resolvable");
        }
    }

    private void configMybatisPlusProcessor(Integration integration, ClassLoader classLoader) {
        log.infoEcho("[JRebel MyBatisPlus] Add CBP for mybatis-plus core classes...");
        integration.addIntegrationProcessor(classLoader, "com.baomidou.mybatisplus.core.MybatisConfiguration", new MybatisConfigurationCBP());
        integration.addIntegrationProcessor(classLoader, "com.baomidou.mybatisplus.core.MybatisMapperAnnotationBuilder", new MybatisMapperAnnotationBuilderCBP());
        integration.addIntegrationProcessor(classLoader, "com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean", new MybatisSqlSessionFactoryBeanCBP());
//        integration.addIntegrationProcessor(classLoader, "com.baomidou.mybatisplus.core.override.MybatisMapperProxy", new MybatisMapperProxyCBP());
        integration.addIntegrationProcessor(classLoader, "com.baomidou.mybatisplus.core.override.MybatisMapperProxyFactory", new MybatisMapperProxyFactoryCBP());
        integration.addIntegrationProcessor(classLoader, "com.baomidou.mybatisplus.core.MybatisConfiguration$StrictMap", new StrictMapCBP());
    }

    private void configMybatisProcessor(Integration integration, ClassLoader classLoader) {
        integration.addIntegrationProcessor(classLoader, "org.apache.ibatis.builder.xml.XMLMapperBuilder", new XMLMapperBuilderCBP());
    }

    @Override
    public boolean checkDependencies(ClassLoader classLoader, ClassResourceSource classResourceSource) {
        return classResourceSource.getClassResource("com.baomidou.mybatisplus.core.MybatisConfiguration") != null;
    }

    @Override
    public String getId() {
        return "mybatis_plus_plugin";
    }

    @Override
    public String getName() {
        return "MybatisPlus_plugin";
    }

    @Override
    public String getDescription() {
        return "<li>A hook plugin for Support MybatisPlus that reloads modified SQL maps.</li>";
    }

    @Override
    public String getAuthor() {
        return "suchu";
    }

    @Override
    public String getWebsite() {
        return "https://githuboy.online";
    }

    @Override
    public String getSupportedVersions() {
        return null;
    }

    @Override
    public String getTestedVersions() {
        return null;
    }
}
