#!/bin/bash

# Quick Build Script cho Git Commit Message Format Plugin

echo "🔨 Quick Build Plugin..."

# Clean và build
./gradlew clean buildPlugin --no-configuration-cache --quiet

# Tìm file zip
ZIP_FILE=$(find ./build/distributions -name "*.zip" -type f | head -n 1)

if [ -n "$ZIP_FILE" ]; then
    echo "✅ Build thành công!"
    echo "📦 File: $ZIP_FILE"
    echo "📏 Size: $(du -h "$ZIP_FILE" | cut -f1)"
else
    echo "❌ Build thất bại!"
    exit 1
fi 