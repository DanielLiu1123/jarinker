#!/bin/bash

set -e

echo "=== Jarinker Complete Test ==="

# Build the project
echo "Building Jarinker..."
./gradlew build

# Build the quick-start example
echo "Building quick-start example..."
./gradlew :examples:quick-start:build

# Check if files exist
echo "Checking generated files..."
ls -la examples/quick-start/build/libs/
ls -la examples/quick-start/libs/

# Test analyze command
echo "Testing analyze command..."
./gradlew :jarinker-cli:run --args="analyze -c $(pwd)/examples/quick-start/libs/guava-33.4.8-jre.jar $(pwd)/examples/quick-start/build/libs/quick-start-0.1.0.jar"

# Create output directory
mkdir -p examples/quick-start/shrunk-libs

# Test shrink command
echo "Testing shrink command..."
./gradlew :jarinker-cli:run --args="shrink -c $(pwd)/examples/quick-start/build/libs/quick-start-0.1.0.jar -o examples/quick-start/shrunk-libs --entry-points com.example.QuickStart $(pwd)/examples/quick-start/libs/guava-33.4.8-jre.jar"

# Compare file sizes
echo "Comparing file sizes..."
if [ -f "examples/quick-start/shrunk-libs/guava-33.4.8-jre.jar" ]; then
    echo "Original Guava JAR:"
    du -sh examples/quick-start/libs/guava-33.4.8-jre.jar
    echo "Shrunk Guava JAR:"
    du -sh examples/quick-start/shrunk-libs/guava-33.4.8-jre.jar
    
    # Calculate reduction
    original_size=$(stat -f%z examples/quick-start/libs/guava-33.4.8-jre.jar)
    shrunk_size=$(stat -f%z examples/quick-start/shrunk-libs/guava-33.4.8-jre.jar)
    reduction=$(echo "scale=1; ($original_size - $shrunk_size) * 100 / $original_size" | bc)
    echo "Size reduction: ${reduction}%"
else
    echo "Warning: Shrunk JAR was not created"
fi

echo "Test completed successfully!"
