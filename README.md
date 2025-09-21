# jarinker

A JAR shrinker that analyzes dependencies and removes unused classes.

## Quick Start

There is a script to let you quickly try out jarinker:

```bash
# Check out what jarinker can do
curl https://raw.githubusercontent.com/DanielLiu1123/jarinker/main/scripts/jarinker | bash -s -- --help

# My code (build/classes/java/main) only uses StringUtils from commons-lang3, shrink it
curl https://raw.githubusercontent.com/DanielLiu1123/jarinker/main/scripts/jarinker | bash -s -- shrink -cp=libs/ build/classes/java/main --jar=commons-lang3-.*

# Don't like it? Uninstall it
curl https://raw.githubusercontent.com/DanielLiu1123/jarinker/main/scripts/jarinker | bash -s -- uninstall
```

## Commands

### analyze

```bash
Usage: jarinker analyze [-hV] [--show-jdk-deps]
                        [--include-pattern=<includePattern>] [--regex=<regex>]
                        [--type=<type>] -cp=<classpath> [-cp=<classpath>]...
                        <sources>...
Analyze dependencies and generate dependency graph
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

### shrink

```bash
Usage: jarinker shrink [-hV] [-o=<outputDir>] -cp=<classpath>
                       [-cp=<classpath>]... [--jar=<jarPatterns>[,
                       <jarPatterns>...]]... <sources>...
Shrink jars by removing unused classes
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

## Build from Source

```bash
./gradlew :jarinker-cli:installDist
./jarinker-cli/build/install/jarinker/bin/jarinker --help
```

## License

MIT License.
