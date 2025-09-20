# jarinker

A JAR shrinker that analyzes dependencies and removes unused classes.

## Quick Start

Install jarinker to your PATH:

```bash
curl -sSL https://raw.githubusercontent.com/DanielLiu1123/jarinker/main/scripts/jarinker | bash
```

Use jarinker:

```bash
jarinker analyze -cp libs/ build/classes/java/main
jarinker shrink -cp libs/ -o shrunk-libs/ build/classes/java/main
```

## Direct Execution

Use without installation:

```bash
curl -sSL https://raw.githubusercontent.com/DanielLiu1123/jarinker/main/scripts/jarinker | bash -s -- analyze -cp libs/ build/classes/java/main
```

## Requirements

- Java 17+

## Commands

### analyze

Analyze JAR dependencies:

```bash
jarinker analyze -cp "libs/*" build/classes/java/main
jarinker analyze --type package -cp "libs/*" build/classes/java/main
jarinker analyze --show-jdk-deps -cp "libs/*" build/classes/java/main
```

### shrink

Remove unused classes from JARs:

```bash
jarinker shrink -cp "libs/*" -o shrunk-libs/ build/classes/java/main
jarinker shrink -cp "libs/*" build/classes/java/main  # in-place
jarinker shrink --jar-pattern "guava-*.jar" -cp "libs/*" -o shrunk-libs/ build/classes/java/main
```

## Build from Source

```bash
./gradlew build
./gradlew :jarinker-cli:installDist
./jarinker-cli/build/install/jarinker/bin/jarinker --help
```
