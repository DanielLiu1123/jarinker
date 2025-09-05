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
    - 依赖分析：封装 `DepsAnalyzer` → `DependencyGraph`
    - 类级可达性：基于 Class-File API
    - Shrink：生成并执行裁剪计划
- **cli**：用户接口
    - 子命令：`analyze`、`shrink`
    - 参数解析与调用 core

---

## 4. CLI 规范
### analyze
```
jarinker analyze -c PATH [-c PATH ...] SOURCES...
```
- `-c, --cp`：classpath 条目（必填，可多次）
- `SOURCES...`：根产物（必填，classes 目录或 JAR）

### shrink
```
jarinker shrink -c PATH [-c PATH ...] [-o PATH | --in-place] SOURCES...
```
- `-c, --cp`：classpath 条目（必填）
- `SOURCES...`：根产物（必填）
- `-o, --output`：输出目录（非原地）
- `--in-place`：原地收缩（默认 true，`-o` 优先生效）

退出码：`0` 成功，`2` 参数错误，`3` 分析失败，`4` 收缩失败。

---

## 5. 依赖分析
- **倒排索引**：遍历 cp，建立 `FQCN -> ArtifactId`，支持 Multi-Release JAR。
- **依赖图**：`DepsAnalyzer` 构建类依赖，映射为工件依赖；过滤 JDK 包。
- **递归**：从 sources 出发，BFS 递归追踪依赖工件。

示例：
```java
DepsAnalyzer depsAnalyzer = new DepsAnalyzer(
    new JdepsConfiguration.Builder().build(),
    new JdepsFilter.Builder().build(),
    JdepsWriter.newSimpleWriter(new PrintWriter(System.out), Analyzer.Type.CLASS),
    Analyzer.Type.CLASS,
    false
);
Graph<DepsAnalyzer.Node> nodeGraph = depsAnalyzer.dependenceGraph();
```

---

## 6. Shrink
- **工件级可达性**：图上 BFS，得到保留工件。
- **类级可达性**：Class-File API 解析常量池、签名、继承、指令引用；忽略 JDK 类。
- **ShrinkPlan**：按工件生成保留类/资源清单 → 输出目标路径。
- **执行**：
    - JAR：复制保留条目，剔除其余；默认移除签名文件。
    - DIR：复制或打包。
    - 原地模式：临时文件 + 原子替换。

---

## 7. 数据模型
```java
record ArtifactId(String value) {}
final class Artifact { ArtifactId id; Path path; boolean multiRelease; }
final class DependencyGraph { Set<ArtifactId> nodes; Map<ArtifactId, Set<ArtifactId>> edges; }
record AnalyzeReport(DependencyGraph graph, Map<ArtifactId,String> idToPath) {}
final class ShrinkPlan { Map<ArtifactId,Set<String>> keepClasses; Map<ArtifactId,Path> targetOutputs; }
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