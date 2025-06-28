#!/bin/bash

# Script to build and install the library locally

echo "======================================"
echo "Installing Spring Boot Error Monitor"
echo "======================================"
echo ""

# Clean and install
echo "📦 Building and installing to local Maven repository..."
mvn clean install -DskipTests -Dgpg.skip=true

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Successfully installed!"
    echo ""
    echo "You can now use it in your projects with:"
    echo ""
    echo "Maven:"
    echo "------"
    echo "<dependency>"
    echo "    <groupId>io.github.nnegi88</groupId>"
    echo "    <artifactId>spring-boot-error-monitor-starter</artifactId>"
    echo "    <version>1.0.0-SNAPSHOT</version>"
    echo "</dependency>"
    echo ""
    echo "Gradle:"
    echo "-------"
    echo "implementation 'io.github.nnegi88:spring-boot-error-monitor-starter:1.0.0-SNAPSHOT'"
    echo ""
    echo "📍 Location: ~/.m2/repository/io/github/nnegi88/spring-boot-error-monitor-starter/"
else
    echo ""
    echo "❌ Build failed!"
    exit 1
fi