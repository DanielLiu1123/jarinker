# Jarinker Quick Start

```bash
# Build the quick-start project first
./gradlew :examples:quick-start:build

# Using Jarinker CLI from source code

# Analyze dependencies
./gradlew :jarinker-cli:run --args="analyze \
  -cp $(pwd)/examples/quick-start/libs \
  $(pwd)/examples/quick-start/build/classes/java/main"

# Shrink JAR files (with all runtime dependencies)
./gradlew :jarinker-cli:run --args="shrink \
  -cp $(pwd)/examples/quick-start/libs \
  -o $(pwd)/examples/quick-start/shrunk-libs \
  $(pwd)/examples/quick-start/build/classes/java/main"

# Compare file sizes
du -sh examples/quick-start/libs/guava-*.jar examples/quick-start/shrunk-libs/guava-*.jar
```
