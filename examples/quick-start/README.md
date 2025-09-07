# Jarinker Quick Start

```bash
# Build the quick-start project first
./gradlew :examples:quick-start:build

# Analyze dependencies from sources
./gradlew :jarinker-cli:run --args="analyze \
  -cp $(pwd)/examples/quick-start/libs \
  $(pwd)/examples/quick-start/build/classes/java/main"
  
# Analyze dependencies from native binary
jarinker-cli/build/native/nativeCompile/jarinker analyze \
  -cp $(pwd)/examples/quick-start/libs \
  $(pwd)/examples/quick-start/build/classes/java/main

# Shrink dependencies from sources
./gradlew :jarinker-cli:run --args="shrink \
  -cp $(pwd)/examples/quick-start/libs \
  -o $(pwd)/examples/quick-start/shrunk-libs \
  $(pwd)/examples/quick-start/build/classes/java/main"
  
# Shrink dependencies from native binary
jarinker-cli/build/native/nativeCompile/jarinker shrink \
  -cp $(pwd)/examples/quick-start/libs \
  -o $(pwd)/examples/quick-start/shrunk-libs \
  $(pwd)/examples/quick-start/build/classes/java/main

# Compare file sizes
du -sh examples/quick-start/libs/guava-*.jar examples/quick-start/shrunk-libs/guava-*.jar
```
