# Jarinker CLI Specification

## analyze

Analyze dependencies and generate dependency graph.

```
jarinker analyze [-hV] [--show-jdk-deps]
                        [--include-pattern=<includePattern>] [--regex=<regex>]
                        [--type=<type>] -cp=<classpath> [-cp=<classpath>]...
                        <sources>...
      <sources>...      Source artifacts to analyze (JAR files or class
                          directories)
      -cp, -classpath, --class-path=<classpath>
                        Classpath entries (can be specified multiple times)
  -h, --help            Show this help message and exit.
      --include-pattern=<includePattern>
                        Restrict analysis to classes matching pattern
      --regex=<regex>   Find dependencies matching the given pattern
      --show-jdk-deps   Show JDK dependencies, by default they are filtered out
      --type=<type>     Analysis type (class, package, module), see jarinker.
                          core.AnalyzerType
  -V, --version         Print version information and exit.
```

### Examples

```bash
# Basic package-level analysis
jarinker analyze -cp "libs/*" build/classes/java/main

# Class-level analysis with JDK dependencies
jarinker analyze --type class --show-jdk-deps -cp "libs/" build/classes/java/main
```

---

## shrink

Shrink artifacts by removing unused classes.

```
jarinker shrink [-hV] [-o=<outputDir>] -cp=<classpath>
                       [-cp=<classpath>]... [--jar=<jarPatterns>[,
                       <jarPatterns>...]]... <sources>...
      <sources>...           Source artifacts to shrink (JAR files or class
                               directories)
      -cp, -classpath, --class-path=<classpath>
                             Classpath entries (can be specified multiple times)
  -h, --help                 Show this help message and exit.
      --jar=<jarPatterns>[,<jarPatterns>...]
                             Shrink JAR files matching the given pattern,
                               shrink all jars by default. Supports
                               comma-separated multiple patterns.
  -o, --output=<outputDir>   Output directory for shrunk artifacts
  -V, --version              Print version information and exit.
```

### Examples

```bash
# Shrink to output directory
jarinker shrink -cp "libs/" -o shrunk-libs/ build/classes/java/main

# In-place shrinking
jarinker shrink -cp "libs/" build/classes/java/main

# Shrink specific JAR patterns
jarinker shrink --jar "guava-.*\.jar,commons-lang3-.*\.jar" -cp "libs/" -o shrunk-libs/ build/classes/java/main
```

---

## generate-completion

Generate bash/zsh completion script for jarinker.

Run the following command to give `jarinker` TAB completion in the current shell:
```
source <(jarinker generate-completion)
```
