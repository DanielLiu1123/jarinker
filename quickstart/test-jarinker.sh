#!/bin/bash

set -e

echo "=== Jarinker Quickstart Test ==="

# Build the quickstart project
echo "Building quickstart project..."
./gradlew clean build fatJar

# Check if JAR was created
if [ ! -f "build/libs/quickstart-1.0.0-fat.jar" ]; then
    echo "Error: Fat JAR was not created"
    exit 1
fi

echo "Original JAR size:"
ls -lh build/libs/quickstart-1.0.0-fat.jar

# Test the original JAR
echo "Testing original JAR..."
java -jar build/libs/quickstart-1.0.0-fat.jar

# Build jarinker
echo "Building jarinker..."
cd ..
./gradlew build

# Test analyze command
echo "Running jarinker analyze..."
./gradlew :jarinker-cli:run --args="analyze -c quickstart/build/libs/quickstart-1.0.0-fat.jar quickstart/build/libs/quickstart-1.0.0-fat.jar"

# Test shrink command
echo "Running jarinker shrink..."
mkdir -p quickstart/shrunk
./gradlew :jarinker-cli:run --args="shrink -c quickstart/build/libs/quickstart-1.0.0-fat.jar -o quickstart/shrunk --entry-points com.example.quickstart.Main quickstart/build/libs/quickstart-1.0.0-fat.jar"

# Check shrunk JAR
if [ -f "quickstart/shrunk/quickstart-1.0.0-fat.jar" ]; then
    echo "Shrunk JAR size:"
    ls -lh quickstart/shrunk/quickstart-1.0.0-fat.jar
    
    echo "Testing shrunk JAR..."
    java -jar quickstart/shrunk/quickstart-1.0.0-fat.jar
else
    echo "Warning: Shrunk JAR was not created"
fi

echo "Test completed!"
