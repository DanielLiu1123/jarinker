# Jarinker Quick Start

```bash
# Build the quick-start project first
./gradlew :examples:quick-start:build

# Using Jarinker CLI from source code
# Analyze dependencies
./gradlew :jarinker-cli:run --args="analyze -cp $(pwd)/examples/quick-start/libs/guava-33.4.8-jre.jar $(pwd)/examples/quick-start/build/libs/quick-start-0.1.0.jar"

# Shrink JAR files
./gradlew :jarinker-cli:run --args="shrink -cp $(pwd)/examples/quick-start/libs/guava-33.4.8-jre.jar -o $(pwd)/examples/quick-start/shrunk-libs  $(pwd)/examples/quick-start/build/libs/quick-start-0.1.0.jar"

# Compare file sizes
du -sh examples/quick-start/libs/guava-*.jar examples/quick-start/shrunk-libs/guava-*.jar
```
