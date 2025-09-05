# Jarinker API 设计文档

## 概述

Jarinker 是一个用于分析 JAR 依赖关系并移除未使用类的工具。本文档描述了整体的 API 架构设计。

## 模块结构

### jarinker-core
核心 API 模块，提供所有分析和收缩功能。

### jarinker-cli  
CLI 接口模块，使用 Picocli 构建命令行界面。

## 核心数据模型 (jarinker.core.model)

### ArtifactId
```java
public record ArtifactId(String value) {}
```
工件的唯一标识符。

### Artifact
```java
public final class Artifact {
    private final ArtifactId id;
    private final Path path;
    private final boolean multiRelease;
}
```
工件元数据，包含 ID、路径和 Multi-Release JAR 标志。

### DependencyGraph
```java
public final class DependencyGraph {
    private final Set<ArtifactId> nodes;
    private final Map<ArtifactId, Set<ArtifactId>> edges;
}
```
工件级依赖图，包含节点和边的关系。

### AnalyzeReport
```java
public record AnalyzeReport(
    DependencyGraph graph,
    Map<ArtifactId, String> idToPath
) {}
```
依赖分析报告。

### ShrinkPlan
```java
public final class ShrinkPlan {
    private final Map<ArtifactId, Set<String>> keepClasses;
    private final Map<ArtifactId, Path> targetOutputs;
}
```
收缩计划，指定每个工件要保留的类和输出路径。

## 依赖分析 (jarinker.core.analysis)

### DependencyAnalyzer
主要的依赖分析接口，封装 jdeps 内部 API。

```java
public interface DependencyAnalyzer {
    AnalyzeReport analyze(List<Path> sources, List<Path> classpath) throws AnalysisException;
    CompletableFuture<AnalyzeReport> analyzeAsync(List<Path> sources, List<Path> classpath);
    
    static Builder builder() { return new DependencyAnalyzerImpl.Builder(); }
}
```

### ClasspathScanner
扫描类路径条目，构建工件元数据和倒排索引。

```java
public interface ClasspathScanner {
    List<Artifact> scan(List<Path> paths) throws AnalysisException;
    Map<String, ArtifactId> buildReverseIndex(List<Artifact> artifacts) throws AnalysisException;
    boolean isMultiReleaseJar(Path jarPath) throws AnalysisException;
}
```

### ArtifactResolver
将类级依赖解析为工件级依赖。

```java
public interface ArtifactResolver {
    DependencyGraph resolve(
        Map<String, Set<String>> classDependencies,
        Map<String, ArtifactId> reverseIndex,
        Set<ArtifactId> sourceArtifacts
    ) throws AnalysisException;
}
```

## 类级可达性分析 (jarinker.core.reachability)

### ClassReachabilityAnalyzer
使用 JDK 24 Class-File API 分析类级可达性。

```java
public interface ClassReachabilityAnalyzer {
    Map<ArtifactId, Set<String>> analyze(List<Artifact> artifacts, Set<String> entryPoints) 
        throws ReachabilityException;
    
    Set<String> extractClassReferences(byte[] classBytes, String className) 
        throws ReachabilityException;
}
```

### BytecodeParser
解析字节码提取类引用。

```java
public interface BytecodeParser {
    Set<String> parseClassReferences(byte[] classBytes, String className) throws ReachabilityException;
    Set<String> extractConstantPoolReferences(byte[] classBytes) throws ReachabilityException;
    Set<String> extractSignatureReferences(byte[] classBytes) throws ReachabilityException;
    Set<String> extractInheritanceReferences(byte[] classBytes) throws ReachabilityException;
    Set<String> extractInstructionReferences(byte[] classBytes) throws ReachabilityException;
    Set<String> extractAnnotationReferences(byte[] classBytes) throws ReachabilityException;
}
```

## 收缩执行引擎 (jarinker.core.shrink)

### ShrinkPlanGenerator
基于可达性分析生成收缩计划。

```java
public interface ShrinkPlanGenerator {
    ShrinkPlan generatePlan(
        List<Artifact> artifacts,
        DependencyGraph dependencyGraph,
        Map<ArtifactId, Set<String>> reachableClasses,
        OutputStrategy outputStrategy
    ) throws ShrinkException;
}
```

### ShrinkExecutor
执行收缩计划。

```java
public interface ShrinkExecutor {
    ShrinkResult execute(ShrinkPlan plan) throws ShrinkException;
    
    record ShrinkResult(
        int processedArtifacts,
        long originalSize,
        long shrunkSize,
        int removedClasses,
        long executionTimeMs
    ) {}
}
```

### ArtifactProcessor
处理单个工件（JAR 或目录）。

```java
public interface ArtifactProcessor {
    ProcessingResult process(Artifact artifact, Set<String> keepClasses, Path outputPath) 
        throws ShrinkException;
    
    record ProcessingResult(
        long originalSize,
        long processedSize,
        int removedClasses,
        int keptClasses
    ) {}
}
```

## 并发支持 (jarinker.core.concurrent)

### VirtualThreadExecutor
虚拟线程执行工具。

```java
public final class VirtualThreadExecutor implements AutoCloseable {
    public <T> CompletableFuture<T> submit(Callable<T> task);
    public <T> CompletableFuture<List<T>> submitAll(Collection<Callable<T>> tasks);
    public <T, R> CompletableFuture<List<R>> mapParallel(Collection<T> items, Function<T, R> mapper);
}
```

### ConcurrentAnalyzer
并发分析操作。

```java
public final class ConcurrentAnalyzer {
    public <T, K, V> CompletableFuture<Map<K, V>> buildConcurrentMap(
        Collection<T> items,
        Function<T, K> keyMapper,
        Function<T, V> valueMapper
    );
    
    public <T> CompletableFuture<Set<T>> bfsParallel(
        Set<T> startNodes,
        Function<T, Set<T>> neighborFunction
    );
}
```

## 主要 API 入口点

### JarinkerAnalyzer
依赖分析的高级 API。

```java
public final class JarinkerAnalyzer {
    public AnalyzeReport analyze(List<Path> sources, List<Path> classpath) throws AnalysisException;
    public CompletableFuture<AnalyzeReport> analyzeAsync(List<Path> sources, List<Path> classpath);
    
    public static Builder builder() { return new Builder(); }
}
```

### JarinkerShrinker
收缩操作的高级 API。

```java
public final class JarinkerShrinker {
    public ShrinkResult shrink(
        List<Path> sources,
        List<Path> classpath,
        OutputStrategy outputStrategy,
        Set<String> entryPoints
    ) throws AnalysisException, ReachabilityException, ShrinkException;
    
    public ShrinkResult shrinkInPlace(List<Path> sources, List<Path> classpath, Set<String> entryPoints);
    public ShrinkResult shrinkToDirectory(List<Path> sources, List<Path> classpath, Path outputDir, Set<String> entryPoints);
    
    public static Builder builder() { return new Builder(); }
}
```

## CLI 命令结构 (jarinker.cli.cmd)

### RootCommand
根命令，包含 analyze 和 shrink 子命令。

### AnalyzeCommand
```bash
jarinker analyze -c PATH [-c PATH ...] SOURCES...
```

### ShrinkCommand
```bash
jarinker shrink -c PATH [-c PATH ...] [-o PATH | --in-place] SOURCES...
```

## 使用示例

### 依赖分析
```java
var analyzer = JarinkerAnalyzer.builder()
    .verbose(true)
    .threadCount(8)
    .build();

var report = analyzer.analyze(sources, classpath);
System.out.println("Found " + report.getStatistics().totalArtifacts() + " artifacts");
```

### JAR 收缩
```java
var shrinker = JarinkerShrinker.builder()
    .verbose(true)
    .removeSignatures(true)
    .build();

var result = shrinker.shrinkToDirectory(sources, classpath, outputDir, entryPoints);
System.out.println("Reduced size by " + result.getReductionPercentage() + "%");
```

## 设计原则

1. **不可变性**: 所有数据模型都是不可变的，确保线程安全
2. **Builder 模式**: 复杂对象使用 Builder 模式配置
3. **异步支持**: 所有耗时操作都提供异步版本
4. **虚拟线程**: 利用 JDK 24 虚拟线程提升 IO 密集型任务性能
5. **模块化**: 清晰的模块边界，核心 API 不依赖 CLI
6. **异常处理**: 定义特定的异常类型，映射到适当的退出码
7. **扩展性**: 为未来功能（JSON 输出、配置文件等）预留扩展点

## 技术特性

- **JDK 24 baseline**: 使用最新的 Java 特性
- **Class-File API**: 利用 JDK 24 预览特性进行字节码分析
- **jdeps 内部 API**: 封装 jdeps 进行依赖分析
- **虚拟线程**: 高效的并发处理
- **Multi-Release JAR**: 完整支持 Multi-Release JAR
- **原子操作**: 原地收缩使用临时文件和原子替换
