
# jrebel-mybatisplus

Jrebel mybatisplus热加载插件，支持重新加载修改后的SQL映射

(中文|[English](README-en.md))

# 前置条件

1. **你的IDE安装了[JRebel插件](https://jrebel.com/software/jrebel/download/prev-releases/)**

2. `mybatis-plus:3.1.1+`

3. IDEA Run/Debug Configurations 配置

   > On 'Update' actions: Update classes and resources

   > On frame deactivation:  Update classes and resources



# 版本说明

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
| 3.5.7 | 已验证 | 已通过 integration-demo 验证 Mapper XML 热加载 |
| 3.1.1 ~ 3.5.6 | 历史支持范围，本次未逐版本回归验证 | 项目历史上支持该范围，但 v1.0.7 本次没有逐版本重新执行集成验证 |

> 仅 3.5.7 经过 integration-demo 真实运行验证，其他版本不做本次兼容承诺。


# 如何使用

已开发IDEA的插件 [jrebel-mybatisplus-idea-plugin](https://github.com/SweetInk/jrebel-mybatisplus-idea-plugin). 安装插件后即可使用，不需要再配置了。

## 构建插件

 ``` shell
git clone git@github.com:SweetInk/jrebel-mybatisplus.git
cd jrebel-mybatisplus
mvn -f jr-mybatisplus/pom.xml clean package
```

将构建好的插件`jrebel-mybatisplus\target\jr-mybatisplus.jar`拷贝至任意目录, 比如: `d:\jrebel\plugin\jr-mybatisplus.jar`

## 使用

打开你的IDE(Intellij IDEA or Eclipse),修改运行配置，增加VM参数:`-Drebel.plugins=d:\jrebel\plugin\jr-mybatisplus.jar`，然后以JRebel方式启动

检查插件是否生效:

修改你项目中的mapper xml 文件后，重新编译，如果重新请求接口，你应该会看到控制台输出 “Reloading SQL maps”

## integration-demo

`integration-demo/` 目录提供了一个最小可运行的集成验证项目，用于验证 Spring Boot + MyBatis-Plus 3.5.7 环境下 Mapper XML 热加载是否生效。

技术栈：Spring Boot 2.7.x + MyBatis-Plus 3.5.7 + H2 内存数据库（无需外部数据库）。

使用方式：

``` shell
cd integration-demo
mvn clean package
```

启动时需要加载本插件 jar，VM 参数示例：

```
-Drebel.plugins=/path/to/jr-mybatisplus-1.0.7.jar
```

启动后访问 `GET /user/{id}`，修改 `mapper/UserMapper.xml` 中的 SQL，重新编译资源（不重启 JVM），再次请求接口即可看到结果变化。

## debug相关

### 输出ctClass到本地

```java
public class YourClassCBP extends JavassistClassBytecodeProcessor {
   public void process(ClassPool cp, ClassLoader cl, CtClass ctClass) throws Exception {
      //TODO class modify
      String output = "X:\\workspace\\dump";
      ctClass.writeFile(output);
      if (ctClass.isFrozen()) {
         ctClass.defrost();
      }
   }
}

```


# 参考

[Custom JRebel plugins](http://manuals.zeroturnaround.com/jrebel/advanced/custom.html#jrebelcustom)

[Getting Started with Javassist](http://www.javassist.org/tutorial/tutorial.html)
