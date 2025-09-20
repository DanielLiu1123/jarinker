# jarinker

A JAR shrinker that analyzes dependencies and removes unused classes.

## Quick Start

```bash
curl -sSL https://raw.githubusercontent.com/DanielLiu1123/jarinker/main/scripts/jarinker | bash -s -- --help
```

## Commands

### analyze

Analyze dependencies:

```bash
jarinker analyze -cp "libs/*" build/classes/java/main
jarinker analyze --type package -cp "libs/*" build/classes/java/main
jarinker analyze --show-jdk-deps -cp "libs/*" build/classes/java/main
```

### shrink

Remove unused classes from JAR files:

```bash
jarinker shrink -cp "libs/*" build/classes/java/main  # in-place
jarinker shrink -cp "libs/*" -o shrunk-libs/ build/classes/java/main
jarinker shrink --jar "guava-.*\.jar,commons-lang3-.*\.jar" -cp "libs/*" -o shrunk-libs/ build/classes/java/main
```

## Build from Source

```bash
./gradlew :jarinker-cli:installDist
./jarinker-cli/build/install/jarinker/bin/jarinker --help
```

## License

MIT License.
