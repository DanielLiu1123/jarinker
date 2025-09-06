# Jarinker 规格说明（SPEC · Final）

## 1. 概述
- **名称**：Jarinker
- **定位**：CLI 工具，用于分析依赖、移除未使用类，缩减 JAR/产物体积。
- **用户**：Java 开发者、构建/平台团队。

---

## 2. 技术栈
- **JDK baseline**：24
- **依赖分析**：`jdeps` 内部 API（`DepsAnalyzer`, `JdepsConfiguration`, `JdepsFilter`, `JdepsWriter`），粒度：类级别。
- **字节码解析**：JDK 24 Class-File API（预览，需 `--enable-preview`）。
- **CLI**：Picocli
- **并发**：虚拟线程

---

## 3. 项目结构
- **core**：核心 API
    - `JdepsAnalyzer`：直接封装 `DepsAnalyzer`，返回 jdeps 的 `Graph<DepsAnalyzer.Node>`
    - `ClassReachabilityAnalyzer`：基于 Class-File API 的类级可达性分析
    - `JarShrinker`：JAR 文件收缩执行器
- **cli**：用户接口
    - 子命令：`analyze`、`shrink`
    - 参数解析与调用 core

---

## 4. CLI 规范
### analyze
```
jarinker analyze -c PATH [-c PATH ...] [OPTIONS] SOURCES...
```
- `-c, --cp`：classpath 条目（必填，可多次）
- `SOURCES...`：根产物（必填，classes 目录或 JAR）

#### 过滤选项
- `--filter-pattern <pattern>`：过滤匹配给定模式的依赖关系
- `--regex <pattern>`：查找匹配给定模式的依赖关系
- `--filter-same-package`：过滤同一包内的依赖关系（默认：true）
- `--filter-same-archive`：过滤同一归档内的依赖关系（默认：false）
- `--find-jdk-internals`：查找对 JDK 内部 API 的类级依赖关系
- `--find-missing-deps`：查找缺失的依赖关系

#### 源过滤选项
- `--include-pattern <pattern>`：限制分析匹配模式的类
- `--requires <module>`：查找匹配给定模块名的依赖关系（可多次指定）
- `--target-packages <package>`：查找匹配给定包名的依赖关系（可多次指定）



### shrink
```
jarinker shrink -c PATH [-c PATH ...] [-o PATH | --in-place] SOURCES...
```
- `-c, --cp`：classpath 条目（必填）
- `SOURCES...`：根产物（必填）
- `-o, --output`：输出目录（非原地）
- `--in-place`：原地收缩（默认 true，`-o` 优先生效）

异常处理：使用 JDK 标准异常，如 `IllegalArgumentException`（参数错误）、`IOException`（IO 失败）、`RuntimeException`（其他运行时错误）。

---

## 5. 依赖分析
- **直接使用 jdeps**：`JdepsAnalyzer` 直接封装 jdeps 的 `DepsAnalyzer`
- **返回 jdeps 图**：直接返回 `Graph<DepsAnalyzer.Node>`，无需自定义数据模型
- **配置支持**：支持 verbose、includeJdk 等配置选项

示例：
```java
var analyzer = JdepsAnalyzer.builder()
    .verbose(true)
    .includeJdk(false)
    .build();

Graph<DepsAnalyzer.Node> graph = analyzer.analyze(sources, classpath);
```

---

## 6. Shrink
- **类级可达性**：`ClassReachabilityAnalyzer` 使用 Class-File API 分析可达类
- **JAR 收缩**：`JarShrinker` 直接处理 JAR 文件收缩
- **简化流程**：
    - 分析依赖关系（jdeps）
    - 分析类级可达性（Class-File API）
    - 执行 JAR 收缩
- **支持模式**：原地收缩或输出到指定目录

---

## 7. 数据模型
```java
// 直接使用 jdeps 的数据模型
Graph<DepsAnalyzer.Node> // jdeps 依赖图
Map<String, Set<String>> // 类级可达性映射
record ShrinkResult(int processedJars, long originalSize, long shrunkSize) // 收缩结果
```

---

## 8. 输出
- **analyze**：打印节点/边统计、未引用工件；后续支持 JSON/DOT。
- **shrink**：打印原始/收缩大小、删除数量；后续支持 JSON 报告。

---

## 9. 验收标准
1. `analyze` 正确输出依赖图，能识别未引用工件。
2. `shrink` 产物可运行，体积减少，无缺失类错误。
3. Multi-Release JAR 分析正确。

---

## 10. 后续规划
- 配置文件：入口点/白名单/资源保留。
- 输出格式：JSON、DOT。
- 构建集成：Gradle/Maven 插件。
- IDE 可视化。  