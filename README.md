
# jrebel-mybatisplus

JRebel MyBatis-Plus runtime plugin — supports hot-reloading modified MyBatis-Plus Mapper XML SQL maps without losing MyBatis-Plus injected statements (e.g. `selectPage`).

(中文|[English](README-en.md))

# 前置条件

1. **你的IDE安装了[JRebel插件](https://jrebel.com/software/jrebel/download/prev-releases/)**

2. `mybatis-plus:3.1.1+`

3. IDEA Run/Debug Configurations 配置

   > On 'Update' actions: Update classes and resources

   > On frame deactivation:  Update classes and resources

# 版本说明

## v1.0.8

主要修复：

### 问题 1：插件初始化阶段 ClassCastException

旧版本初始化时为了输出版本信息，通过 `Class.forName` 提前加载 `MybatisConfiguration`：

```java
Class.forName(
    "com.baomidou.mybatisplus.core.MybatisConfiguration",
    false,
    classLoader
)
```

这会导致 `MybatisConfigurationCBP` 注册时已经错过类加载阶段，
`MybatisConfiguration` 没有被增强为 `JrConfiguration`，
最终在 JRebel 运行时抛出：

```
MybatisConfiguration cannot be cast to JrConfiguration
```

v1.0.8 修复：

- 新增 `ResourceVersionDetector`，通过 `pom.properties` + `Manifest`
  纯资源方式获取 MyBatis-Plus 版本，不再通过 `Class.forName` 加载框架类
- 调整 JRebel processor 注册顺序：processor first，version detection later

### 问题 2：XML reload 误删 MyBatis-Plus 自动注入 MappedStatement

旧版本 Mapper XML reload 时按 namespace 前缀全量清理 Configuration 内容：

```
namespace + ".*"
→ mappedStatements
→ resultMaps
→ parameterMaps
→ keyGenerators
→ sqlFragments
```

这会导致 MyBatis-Plus `SqlInjector` 自动注入的
`selectPage` / `selectById` / `selectList` 等被一起删除。
`XMLMapperBuilder` 重新解析 XML 时又不会重新执行 `SqlInjector`，
最终在调用时抛出：

```
Mapped Statements collection does not contain value for xxx.selectPage
```

v1.0.8 修复：

- Mapper XML reload 改为 **XML-owned exact cleanup**
- 只删除当前 XML 自己声明的资源：
  - `select` / `insert` / `update` / `delete` → mappedStatements
  - `resultMap` → resultMaps
  - `parameterMap` → parameterMaps
  - `sql` → sqlFragments
  - `selectKey` → 对应的 MappedStatement + KeyGenerator
  - 当前 resource → loadedResources（精确移除，非 clear）
- `knownMappers` 完全不修改
- 不使用 `Class.forName`，不加载 Mapper 接口

## v1.0.7

主要修复：

- 兼容 MyBatis-Plus 3.5.7+
- 修复 Mapper XML 修改后热加载失败的问题
- 修复 reload 时 MappedStatement 无法重新注册的问题
- reload 前按 namespace 清理：
  - mappedStatements
  - resultMaps
  - sqlFragments
- StrictMap 兼容逻辑改为运行时 CapabilityDetector 检测
- 支持 AMBIGUITY_INSTANCE 字段变体
- 未识别运行时结构时安全降级
- CBP 增加 fail-safe，避免插件增强异常影响应用启动
- 增加单元测试
- 增加 Spring Boot + MyBatis-Plus 3.5.7 + H2 integration-demo

# 兼容性

| MyBatis-Plus 版本 | 状态 | 说明 |
| --- | --- | --- |
| 3.5.2 | 已验证 | integration-demo + 真实业务项目验证 Mapper XML 热加载 |
| 3.5.7 | 已验证 | integration-demo 验证 Mapper XML 热加载 |
| 3.1.1 ~ 3.5.1、3.5.3 ~ 3.5.6 | 历史支持范围，本次未逐版本重新验证 | 不做本次兼容承诺 |

> 已验证多次 Mapper XML reload 后，MyBatis-Plus 自动注入的
> `selectPage` / `selectById` / `selectList` 持续可用。
> Mapper XML 热加载已在 MyBatis-Plus 3.5.2、3.5.7 验证。

# 如何使用

推荐安装 IDEA 插件 [MyBatis-Plus Reload](https://github.com/LayJustDoIt/jrebel-mybatisplus-idea-plugin)，安装后通过 JRebel Run / Debug 启动即可自动加载本 runtime，无需手工配置 `-Drebel.plugins`。

## 构建插件

```shell
git clone https://github.com/LayJustDoIt/jrebel-mybatisplus.git
cd jrebel-mybatisplus
mvn clean package
```

将构建好的插件 `target/jr-mybatisplus-1.0.8.jar` 拷贝至任意目录，比如: `/path/to/jr-mybatisplus-1.0.8.jar`

## 手工使用（不安装 IDEA 插件）

打开你的 IDE (IntelliJ IDEA or Eclipse)，修改运行配置，增加 VM 参数：`-Drebel.plugins=/path/to/jr-mybatisplus-1.0.8.jar`，然后以 JRebel 方式启动。

检查插件是否生效：

修改你项目中的 mapper xml 文件后，重新编译，如果重新请求接口，你应该会看到控制台输出 "Reloading SQL maps"。

## integration-demo

`integration-demo/` 目录提供了一个最小可运行的集成验证项目，用于验证 Spring Boot + MyBatis-Plus 3.5.7 环境下 Mapper XML 热加载是否生效。

技术栈：Spring Boot 2.7.x + MyBatis-Plus 3.5.7 + H2 内存数据库（无需外部数据库）。

使用方式：

```shell
cd integration-demo
mvn clean package
```

启动时需要加载本插件 jar，VM 参数示例：

```
-Drebel.plugins=/path/to/jr-mybatisplus-1.0.8.jar
```

启动后访问 `GET /user/{id}`，修改 `mapper/UserMapper.xml` 中的 SQL，重新编译资源（不重启 JVM），再次请求接口即可看到结果变化。

## debug相关

### 输出ctClass到本地

```java
public class YourClassCBP extends JavassistClassBytecodeProcessor {
   public void process(ClassPool cp, ClassLoader cl, CtClass ctClass) throws Exception {
      //TODO class modify
      String output = "/tmp/workspace/dump";
      ctClass.writeFile(output);
      if (ctClass.isFrozen()) {
         ctClass.defrost();
      }
   }
}
```

# Credits

This project is based on the original jrebel-mybatisplus project created by **suchu / SweetInk**.

- Original project: https://github.com/SweetInk/jrebel-mybatisplus
- Current maintenance: https://github.com/LayJustDoIt/jrebel-mybatisplus

# 参考

[Custom JRebel plugins](http://manuals.zeroturnaround.com/jrebel/advanced/custom.html#jrebelcustom)

[Getting Started with Javassist](http://www.javassist.org/tutorial/tutorial.html)
