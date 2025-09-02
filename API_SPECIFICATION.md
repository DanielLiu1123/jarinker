# Jarinker API 规格文档 v2.0

## 概述

Jarinker 是一个智能的 JAR 文件精简工具，通过分析 Java 应用程序的类依赖关系，移除未使用的类来减少 JAR 文件大小。本文档定义了重新设计的 API 规格。

## 设计原则

1. **用户友好**：提供直观、易用的 API 接口
2. **可配置性**：支持灵活的配置选项和策略
3. **可扩展性**：支持插件机制和自定义策略
4. **集成友好**：易于与构建工具和 IDE 集成
5. **类型安全**：充分利用 Java 类型系统

## 核心 API

### 1. JarinkerBuilder - 构建器模式

```java
public class JarinkerBuilder {
    // 创建构建器实例
    public static JarinkerBuilder create();
    
    // 配置源路径
    public JarinkerBuilder withSource(Path... paths);
    public JarinkerBuilder withSource(Collection<Path> paths);
    
    // 配置依赖路径
    public JarinkerBuilder withDependencies(Path... paths);
    public JarinkerBuilder withDependencies(Collection<Path> paths);
    
    // 配置输出目录（可选，默认原地操作）
    public JarinkerBuilder withOutputDirectory(Path outputDir);

    // 启用原地操作（默认行为）
    public JarinkerBuilder enableInPlaceOperation();

    // 禁用原地操作，必须指定输出目录
    public JarinkerBuilder disableInPlaceOperation();
    
    // 配置精简策略（暂时只有默认策略）
    public JarinkerBuilder withStrategy(ShrinkStrategy strategy);
    
    // 配置详细选项
    public JarinkerBuilder withConfiguration(JarinkerConfig config);
    
    // 添加包含规则
    public JarinkerBuilder includePattern(String pattern);
    public JarinkerBuilder includePatterns(Collection<String> patterns);
    
    // 添加排除规则
    public JarinkerBuilder excludePattern(String pattern);
    public JarinkerBuilder excludePatterns(Collection<String> patterns);
    
    // 构建 Jarinker 实例
    public Jarinker build() throws JarinkerConfigurationException;
}
```

### 2. Jarinker - 主执行类

```java
public class Jarinker {
    // 执行依赖分析
    public AnalysisResult analyze() {
        // 分析逻辑，出错时抛出 RuntimeException
    }

    // 执行 JAR 精简
    public ShrinkResult shrink() {
        // 精简逻辑，出错时抛出 RuntimeException
    }
}
```

### 3. JarinkerConfig - 配置类

```java
public class JarinkerConfig {
    // 构建器
    public static Builder builder() {
        return new Builder();
    }

    // 配置属性
    private final ShrinkStrategy strategy;
    private final Set<String> includePatterns;
    private final Set<String> excludePatterns;
    private final Optional<Path> outputDirectory; // 空表示原地操作
    private final boolean inPlaceOperation; // 是否启用原地操作
    private final boolean verbose;
    private final boolean showProgress;
    private final Duration timeout;

    private JarinkerConfig(Builder builder) {
        this.strategy = builder.strategy;
        this.includePatterns = new HashSet<>(builder.includePatterns);
        this.excludePatterns = new HashSet<>(builder.excludePatterns);
        this.outputDirectory = builder.outputDirectory;
        this.inPlaceOperation = builder.inPlaceOperation;
        this.verbose = builder.verbose;
        this.showProgress = builder.showProgress;
        this.timeout = builder.timeout;
    }

    // Getter 方法
    public ShrinkStrategy getStrategy() { return strategy; }
    public Set<String> getIncludePatterns() { return new HashSet<>(includePatterns); }
    public Set<String> getExcludePatterns() { return new HashSet<>(excludePatterns); }
    public Optional<Path> getOutputDirectory() { return outputDirectory; }
    public boolean isInPlaceOperation() { return inPlaceOperation; }
    public boolean isVerbose() { return verbose; }
    public boolean isShowProgress() { return showProgress; }
    public Duration getTimeout() { return timeout; }

    // Builder 类
    public static class Builder {
        private ShrinkStrategy strategy = ShrinkStrategy.DEFAULT;
        private Set<String> includePatterns = new HashSet<>();
        private Set<String> excludePatterns = new HashSet<>();
        private Optional<Path> outputDirectory = Optional.empty();
        private boolean inPlaceOperation = true;
        private boolean verbose = false;
        private boolean showProgress = true;
        private Duration timeout = Duration.ofMinutes(1);

        public Builder strategy(ShrinkStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder includePattern(String pattern) {
            this.includePatterns.add(pattern);
            return this;
        }

        public Builder includePatterns(Collection<String> patterns) {
            this.includePatterns.addAll(patterns);
            return this;
        }

        public Builder excludePattern(String pattern) {
            this.excludePatterns.add(pattern);
            return this;
        }

        public Builder excludePatterns(Collection<String> patterns) {
            this.excludePatterns.addAll(patterns);
            return this;
        }

        public Builder outputDirectory(Path path) {
            this.outputDirectory = Optional.ofNullable(path);
            return this;
        }

        public Builder enableInPlaceOperation() {
            this.inPlaceOperation = true;
            this.outputDirectory = Optional.empty();
            return this;
        }

        public Builder disableInPlaceOperation() {
            this.inPlaceOperation = false;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder showProgress(boolean show) {
            this.showProgress = show;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public JarinkerConfig build() {
            return new JarinkerConfig(this);
        }
    }
}
```

## 策略系统

### ShrinkStrategy 接口

```java
public interface ShrinkStrategy {
    // 确定需要保留的类
    Set<String> determineRequiredClasses(AnalysisResult analysis);

    // 获取策略名称
    String getName();

    // 获取策略描述
    String getDescription();

    // 默认策略
    ShrinkStrategy DEFAULT = new DefaultStrategy();
}
```

### 默认策略

```java
// 默认策略 - 激进的精简策略
public class DefaultStrategy implements ShrinkStrategy {
    @Override
    public Set<String> determineRequiredClasses(AnalysisResult analysis) {
        // 只保留直接依赖的类，最大程度精简
        Set<String> requiredClasses = new HashSet<>();

        // 从入口点开始，递归收集所有被使用的类
        for (String entryPoint : analysis.getEntryPoints()) {
            collectRequiredClasses(entryPoint, analysis, requiredClasses, new HashSet<>());
        }

        return requiredClasses;
    }

    private void collectRequiredClasses(String className, AnalysisResult analysis,
                                       Set<String> required, Set<String> visited) {
        if (!visited.add(className)) {
            return; // 已经访问过
        }

        required.add(className);

        // 递归收集依赖的类
        Set<String> dependencies = analysis.getDependencyGraph().get(className);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                collectRequiredClasses(dependency, analysis, required, visited);
            }
        }
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public String getDescription() {
        return "Default aggressive shrink strategy that removes all unused classes";
    }
}
```

## 结果对象

### AnalysisResult - 分析结果

```java
public class AnalysisResult {
    // 所有发现的类信息
    private final Map<String, ClassInfo> allClasses;

    // 依赖关系图
    private final Map<String, Set<String>> dependencyGraph;

    // 入口点类
    private final Set<String> entryPoints;

    // 分析警告
    private final List<AnalysisWarning> warnings;

    // 分析统计
    private final AnalysisStatistics statistics;

    public AnalysisResult(Map<String, ClassInfo> allClasses,
                         Map<String, Set<String>> dependencyGraph,
                         Set<String> entryPoints,
                         List<AnalysisWarning> warnings,
                         AnalysisStatistics statistics) {
        this.allClasses = new HashMap<>(allClasses);
        this.dependencyGraph = new HashMap<>(dependencyGraph);
        this.entryPoints = new HashSet<>(entryPoints);
        this.warnings = new ArrayList<>(warnings);
        this.statistics = statistics;
    }

    // Getter 方法
    public Map<String, ClassInfo> getAllClasses() {
        return new HashMap<>(allClasses);
    }

    public Map<String, Set<String>> getDependencyGraph() {
        return new HashMap<>(dependencyGraph);
    }

    public Set<String> getEntryPoints() {
        return new HashSet<>(entryPoints);
    }

    public List<AnalysisWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public AnalysisStatistics getStatistics() {
        return statistics;
    }
    
    // 查询方法
    public Set<String> getUnusedClasses();
    public Set<String> getDependencies(String className);
    public Set<String> getDependents(String className);
    public ClassInfo getClassInfo(String className);
    public boolean isClassUsed(String className);
    public List<String> getDependencyPath(String from, String to);
    
    // 统计方法
    public int getTotalClassCount();
    public int getUsedClassCount();
    public int getUnusedClassCount();
    public double getUsageRatio();
}
```

### ShrinkResult - 精简结果

```java
public class ShrinkResult {
    // 精简后的 JAR 文件信息
    private final List<ShrunkJar> shrunkJars;

    // 精简统计信息
    private final ShrinkStatistics statistics;

    // 精简警告
    private final List<ShrinkWarning> warnings;

    // 精简报告
    private final Optional<ShrinkReport> report;

    public ShrinkResult(List<ShrunkJar> shrunkJars,
                       ShrinkStatistics statistics,
                       List<ShrinkWarning> warnings,
                       Optional<ShrinkReport> report) {
        this.shrunkJars = new ArrayList<>(shrunkJars);
        this.statistics = statistics;
        this.warnings = new ArrayList<>(warnings);
        this.report = report;
    }

    // Getter 方法
    public List<ShrunkJar> getShrunkJars() {
        return new ArrayList<>(shrunkJars);
    }

    public ShrinkStatistics getStatistics() {
        return statistics;
    }

    public List<ShrinkWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public Optional<ShrinkReport> getReport() {
        return report;
    }
    
    // 查询方法
    public long getTotalSizeBefore();
    public long getTotalSizeAfter();
    public long getSizeSaved();
    public double getShrinkRatio();
    public Duration getProcessingTime();
    
    // 结果验证
    public boolean isSuccessful();
    public List<String> getErrors();
}
```

## 异常处理

使用标准的 Java 异常，不需要自定义异常类：

```java
// 使用标准异常
public class Jarinker {
    public AnalysisResult analyze() {
        // 分析逻辑，出错时抛出 RuntimeException
    }

    public ShrinkResult shrink() {
        // 精简逻辑，出错时抛出 RuntimeException
    }

    public ShrinkResult analyzeAndShrink() {
        // 组合操作，出错时抛出 RuntimeException
    }
}

// 配置验证
public class JarinkerBuilder {
    public Jarinker build() {
        // 验证配置，如有问题抛出 RuntimeException
    }
}
```

## 使用示例

### 基础使用

```java
// 最简单的使用方式 - 原地操作（默认）
try {
    Jarinker jarinker = JarinkerBuilder.create()
        .withSource(Paths.get("./target/classes"))
        .withDependencies(Paths.get("./lib"))
        .build();

    ShrinkResult result = jarinker.shrink();

    System.out.printf("精简完成！节省了 %.2f%% 的空间%n",
                     result.getShrinkRatio() * 100);
} catch (RuntimeException e) {
    System.err.println("精简失败: " + e.getMessage());
}

// 输出到指定目录
try {
    Jarinker jarinker = JarinkerBuilder.create()
        .withSource(Paths.get("./target/classes"))
        .withDependencies(Paths.get("./lib"))
        .withOutputDirectory(Paths.get("./optimized-jars"))
        .build();

    ShrinkResult result = jarinker.shrink();

    System.out.printf("精简完成！输出到: %s%n", "./optimized-jars");
} catch (RuntimeException e) {
    System.err.println("精简失败: " + e.getMessage());
}
```

### 高级配置使用

```java
// 高级配置
JarinkerConfig config = JarinkerConfig.builder()
    .strategy(ShrinkStrategy.DEFAULT)
    .includePattern("com.mycompany.**")
    .excludePattern("**Test")
    .excludePattern("**Mock")
    .outputDirectory(Paths.get("./optimized-jars")) // 指定输出目录
    .disableInPlaceOperation() // 禁用原地操作
    .enableReflectionAnalysis(true)
    .generateReport(true)
    .logLevel(LogLevel.INFO)
    .showProgress(true)
    .timeout(Duration.ofMinutes(10))
    .build();

Jarinker jarinker = JarinkerBuilder.create()
    .withSource(Paths.get("./target/classes"))
    .withDependencies(Paths.get("./lib"))
    .withConfiguration(config)
    .build();

// 执行分析
AnalysisResult analysis = jarinker.analyze();
System.out.printf("发现 %d 个类，其中 %d 个未使用%n",
                 analysis.getTotalClassCount(),
                 analysis.getUnusedClassCount());

// 检查警告
if (!analysis.getWarnings().isEmpty()) {
    System.out.println("分析警告:");
    analysis.getWarnings().forEach(w ->
        System.out.println("  - " + w.getMessage()));
}

// 执行精简
ShrinkResult result = jarinker.shrink();
    
    // 输出结果
    System.out.printf("精简完成！%n");
    System.out.printf("  原始大小: %s%n", formatSize(result.getTotalSizeBefore()));
    System.out.printf("  精简后大小: %s%n", formatSize(result.getTotalSizeAfter()));
    System.out.printf("  节省空间: %s (%.2f%%)%n", 
                     formatSize(result.getSizeSaved()),
                     result.getShrinkRatio() * 100);
    System.out.printf("  处理时间: %s%n", result.getProcessingTime());

} catch (RuntimeException e) {
    System.err.println("处理失败: " + e.getMessage());
    e.printStackTrace();
}
```

## CLI 接口重新设计

### 新的命令结构

```bash
# 基础精简命令 - 原地操作（默认）
jarinker shrink --source ./target/classes --dependencies ./lib

# 输出到指定目录
jarinker shrink --source ./target/classes --dependencies ./lib --output ./optimized-jars

# 强制原地操作
jarinker shrink --source ./target/classes --dependencies ./lib --in-place

# 使用策略选项
jarinker shrink --source ./target/classes --dependencies ./lib --strategy default

# 分析模式
jarinker analyze --source ./target/classes --dependencies ./lib --report

# 包含/排除模式
jarinker shrink --source ./target/classes --dependencies ./lib --include "com.mycompany.**" --exclude "**Test"

# 启用反射分析
jarinker shrink --source ./target/classes --dependencies ./lib --enable-reflection-analysis
```

### CLI 选项详细说明

```
jarinker shrink [OPTIONS]

OPTIONS:
  -s, --sources PATH              Source paths (required, can be specified multiple times)
  -d, --dependencies PATH        Dependency paths (required, can be specified multiple times)
      --strategy STRATEGY        Shrink strategy: default (default: default)
      --include PATTERN          Include pattern (can be specified multiple times)
      --exclude PATTERN          Exclude pattern (can be specified multiple times)
      --verbose
  -o, --output PATH              Output directory (optional, default: in-place operation)
      --in-place                 Force in-place operation (default: true)
      --show-progress            Show progress information (default: true)
      --timeout DURATION         Processing timeout (default: 1m)
  -h, --help                     Show help message
  -V, --version                  Show version information

jarinker analyze [OPTIONS]

OPTIONS:
  -s, --sources PATH              Source paths (required, can be specified multiple times)
  -d, --dependencies PATH        Dependency paths (required, can be specified multiple times)
      --strategy STRATEGY        Shrink strategy: default (default: default)
      --include PATTERN          Include pattern (can be specified multiple times)
      --exclude PATTERN          Exclude pattern (can be specified multiple times)
      --verbose
  -h, --help                     Show help message

EXAMPLES:
  # Basic shrink with in-place operation
  jarinker shrink -s ./target/classes -d ./lib

  # Shrink with output to specific directory
  jarinker shrink -s ./target/classes -d ./lib -o ./optimized-jars

  # Shrink with default strategy and patterns
  jarinker shrink -s ./target/classes -d ./lib --strategy default --include "com.google.common.**"

  # Analysis only
  jarinker analyze -s ./target/classes -d ./lib
```



## 扩展点

### 自定义策略

```java
public class CustomStrategy implements ShrinkStrategy {
    private final CustomStrategyConfig config;

    public CustomStrategy(CustomStrategyConfig config) {
        this.config = config;
    }

    @Override
    public Set<String> determineRequiredClasses(AnalysisResult analysis) {
        // 自定义逻辑
        System.out.println("Applying custom strategy with config: " + config);
        Set<String> requiredClasses = new HashSet<>();
        // 实现自定义逻辑
        return requiredClasses;
    }

    @Override
    public String getName() {
        return "custom";
    }

    @Override
    public String getDescription() {
        return "Custom shrink strategy";
    }
}

public class CustomStrategyConfig {
    private final boolean aggressive;
    private final Set<String> protectedPackages;
    private final double threshold;

    public CustomStrategyConfig(boolean aggressive, Set<String> protectedPackages, double threshold) {
        this.aggressive = aggressive;
        this.protectedPackages = new HashSet<>(protectedPackages);
        this.threshold = threshold;
    }

    // Getter 方法
    public boolean isAggressive() { return aggressive; }
    public Set<String> getProtectedPackages() { return new HashSet<>(protectedPackages); }
    public double getThreshold() { return threshold; }

    @Override
    public String toString() {
        return "CustomStrategyConfig{" +
                "aggressive=" + aggressive +
                ", protectedPackages=" + protectedPackages +
                ", threshold=" + threshold +
                '}';
    }
}
```

### 插件系统

```java
public interface JarinkerPlugin {
    void initialize(JarinkerContext context);
    void beforeAnalysis(AnalysisContext context);
    void afterAnalysis(AnalysisResult result);
    void beforeShrink(ShrinkContext context);
    void afterShrink(ShrinkResult result);
}
```

## 性能考虑

1. **并行处理**：支持多线程分析和处理
2. **内存优化**：大文件流式处理
3. **缓存机制**：分析结果缓存
4. **增量处理**：只处理变更的文件

## 向后兼容性

为了保持向后兼容，提供适配器类：

```java
@Deprecated
public class LegacyJarinker {
    public static void shrinkJars(List<Path> sourcePaths, List<Path> dependenciesPaths) {
        // 使用新 API 实现
        JarinkerBuilder.create()
            .withSource(sourcePaths)
            .withDependencies(dependenciesPaths)
            .build()
            .analyzeAndShrink();
    }
}
```

## 辅助类和工具类

### 统计信息类

```java
public class ShrinkStatistics {
    private final long totalSizeBefore;
    private final long totalSizeAfter;
    private final int totalClassesBefore;
    private final int totalClassesAfter;
    private final Duration processingTime;
    private final Map<String, Long> jarSizeReductions;

    public ShrinkStatistics(long totalSizeBefore, long totalSizeAfter,
                           int totalClassesBefore, int totalClassesAfter,
                           Duration processingTime, Map<String, Long> jarSizeReductions) {
        this.totalSizeBefore = totalSizeBefore;
        this.totalSizeAfter = totalSizeAfter;
        this.totalClassesBefore = totalClassesBefore;
        this.totalClassesAfter = totalClassesAfter;
        this.processingTime = processingTime;
        this.jarSizeReductions = new HashMap<>(jarSizeReductions);
    }

    // Getter 方法
    public long getTotalSizeBefore() { return totalSizeBefore; }
    public long getTotalSizeAfter() { return totalSizeAfter; }
    public int getTotalClassesBefore() { return totalClassesBefore; }
    public int getTotalClassesAfter() { return totalClassesAfter; }
    public Duration getProcessingTime() { return processingTime; }
    public Map<String, Long> getJarSizeReductions() { return new HashMap<>(jarSizeReductions); }

    public long getSizeSaved() {
        return totalSizeBefore - totalSizeAfter;
    }

    public double getShrinkRatio() {
        return totalSizeBefore == 0 ? 0.0 : (double) getSizeSaved() / totalSizeBefore;
    }
}

public class AnalysisStatistics {
    private final int totalClasses;
    private final int usedClasses;
    private final int unusedClasses;
    private final Duration analysisTime;
    private final Map<String, Integer> packageClassCounts;

    public AnalysisStatistics(int totalClasses, int usedClasses, int unusedClasses,
                             Duration analysisTime, Map<String, Integer> packageClassCounts) {
        this.totalClasses = totalClasses;
        this.usedClasses = usedClasses;
        this.unusedClasses = unusedClasses;
        this.analysisTime = analysisTime;
        this.packageClassCounts = new HashMap<>(packageClassCounts);
    }

    // Getter 方法
    public int getTotalClasses() { return totalClasses; }
    public int getUsedClasses() { return usedClasses; }
    public int getUnusedClasses() { return unusedClasses; }
    public Duration getAnalysisTime() { return analysisTime; }
    public Map<String, Integer> getPackageClassCounts() { return new HashMap<>(packageClassCounts); }

    public double getUsageRatio() {
        return totalClasses == 0 ? 0.0 : (double) usedClasses / totalClasses;
    }
}
```

### 警告和报告类

```java
public class AnalysisWarning {
    private final WarningType type;
    private final String message;
    private final String className;
    private final Optional<String> suggestion;

    public AnalysisWarning(WarningType type, String message, String className, Optional<String> suggestion) {
        this.type = type;
        this.message = message;
        this.className = className;
        this.suggestion = suggestion;
    }

    // Getter 方法
    public WarningType getType() { return type; }
    public String getMessage() { return message; }
    public String getClassName() { return className; }
    public Optional<String> getSuggestion() { return suggestion; }
}

public class ShrinkWarning {
    private final WarningType type;
    private final String message;
    private final String jarName;
    private final Optional<String> suggestion;

    public ShrinkWarning(WarningType type, String message, String jarName, Optional<String> suggestion) {
        this.type = type;
        this.message = message;
        this.jarName = jarName;
        this.suggestion = suggestion;
    }

    // Getter 方法
    public WarningType getType() { return type; }
    public String getMessage() { return message; }
    public String getJarName() { return jarName; }
    public Optional<String> getSuggestion() { return suggestion; }
}

public class ShrunkJar {
    private final String originalName;
    private final String shrunkName;
    private final long originalSize;
    private final long shrunkSize;
    private final int originalClassCount;
    private final int shrunkClassCount;
    private final Path outputPath;

    public ShrunkJar(String originalName, String shrunkName, long originalSize, long shrunkSize,
                     int originalClassCount, int shrunkClassCount, Path outputPath) {
        this.originalName = originalName;
        this.shrunkName = shrunkName;
        this.originalSize = originalSize;
        this.shrunkSize = shrunkSize;
        this.originalClassCount = originalClassCount;
        this.shrunkClassCount = shrunkClassCount;
        this.outputPath = outputPath;
    }

    // Getter 方法
    public String getOriginalName() { return originalName; }
    public String getShrunkName() { return shrunkName; }
    public long getOriginalSize() { return originalSize; }
    public long getShrunkSize() { return shrunkSize; }
    public int getOriginalClassCount() { return originalClassCount; }
    public int getShrunkClassCount() { return shrunkClassCount; }
    public Path getOutputPath() { return outputPath; }

    public long getSizeSaved() {
        return originalSize - shrunkSize;
    }

    public double getShrinkRatio() {
        return originalSize == 0 ? 0.0 : (double) getSizeSaved() / originalSize;
    }
}
```

## 总结

这个重新设计的 API 提供了：

1. **更好的用户体验**：直观的构建器模式和链式调用
2. **更强的可配置性**：丰富的配置选项和策略系统
3. **简化的错误处理**：使用标准 Java 异常，避免过度设计
4. **更强的扩展性**：插件系统和自定义策略支持
5. **更好的集成性**：支持多种构建工具和配置方式
6. **类型安全**：充分利用 Java 类型系统和 Optional
7. **灵活的操作模式**：支持原地操作和输出到指定目录
8. **简洁的代码设计**：避免过度使用框架，保持代码的可读性和可维护性

### 设计原则：

- **KISS 原则**：保持简单，避免过度工程化
- **标准化**：使用标准 Java 异常和常见设计模式
- **向后兼容**：提供适配器支持现有代码迁移
- **可测试性**：所有组件都易于单元测试
- **文档友好**：API 设计直观，易于理解和使用

这个设计既保持了原有功能的完整性，又大大提升了易用性和可扩展性，同时避免了过度设计的复杂性。
