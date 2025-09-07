# Jarinker Quick Start

```bash
# Build the quick-start project first
./gradlew :examples:quick-start:build

# Analyze from sources
./gradlew :jarinker-cli:run --args="analyze \
  -cp $(pwd)/examples/quick-start/libs \
  $(pwd)/examples/quick-start/build/classes/java/main"
  
# Analyze from build
./gradlew :jarinker-cli:installDist
jarinker-cli/build/install/jarinker/bin/jarinker analyze \
  -cp $(pwd)/examples/quick-start/libs \
  $(pwd)/examples/quick-start/build/classes/java/main

# Shrink from sources
./gradlew :jarinker-cli:run --args="shrink \
  -cp $(pwd)/examples/quick-start/libs \
  -o $(pwd)/examples/quick-start/shrunk-libs \
  $(pwd)/examples/quick-start/build/classes/java/main"
  
# Shrink from build
./gradlew :jarinker-cli:installDist
jarinker-cli/build/install/jarinker/bin/jarinker shrink \
  -cp $(pwd)/examples/quick-start/libs \
  -o $(pwd)/examples/quick-start/shrunk-libs \
  $(pwd)/examples/quick-start/build/classes/java/main

# Compare file sizes
du -sh examples/quick-start/libs/guava-*.jar examples/quick-start/shrunk-libs/guava-*.jar
# Use shrunk libs still works
./gradlew :examples:quick-start:run
```
