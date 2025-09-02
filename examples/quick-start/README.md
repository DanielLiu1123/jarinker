# Jarinker Quick Start

```bash
# Using Jarinker CLI from source code (specify JAR file directly)
./gradlew :jarinker-cli:run --args="analyze -s $(pwd)/examples/quick-start/build/classes/java/main -d $(pwd)/examples/quick-start/libs/guava-33.4.8-jre.jar --verbose"
./gradlew :jarinker-cli:run --args="shrink -s $(pwd)/examples/quick-start/build/classes/java/main -d $(pwd)/examples/quick-start/libs/guava-33.4.8-jre.jar -o $(pwd)/examples/quick-start/shrunk-libs --verbose"
du -sh examples/quick-start/libs/guava-*.jar examples/quick-start/shrunk-libs/guava-*.jar
```
