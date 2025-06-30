#!/bin/bash

# Git Commit Message Format Plugin - Build Script
# Tác giả: Assistant AI
# Phiên bản: 1.0

echo "🚀 Bắt đầu build Git Commit Message Format Plugin..."
echo "=================================================="

# Màu sắc cho terminal
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function để log với màu
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Kiểm tra Java
log_info "Kiểm tra Java environment..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    log_success "Java version: $JAVA_VERSION"
else
    log_error "Java không được tìm thấy! Vui lòng cài đặt Java."
    exit 1
fi

# Kiểm tra Gradle wrapper
if [ ! -f "./gradlew" ]; then
    log_error "Gradle wrapper không tìm thấy!"
    exit 1
fi

log_success "Gradle wrapper tìm thấy"

# Clean project
log_info "Làm sạch project cũ..."
./gradlew clean --no-configuration-cache --quiet
if [ $? -eq 0 ]; then
    log_success "Clean project thành công"
else
    log_error "Clean project thất bại"
    exit 1
fi

# Build plugin với disable configuration cache
log_info "Build plugin (có thể mất vài phút)..."
./gradlew buildPlugin --no-configuration-cache --quiet
if [ $? -eq 0 ]; then
    log_success "Build plugin thành công!"
else
    log_error "Build plugin thất bại"
    exit 1
fi

# Tìm file zip
ZIP_FILE=$(find ./build/distributions -name "*.zip" -type f | head -n 1)

if [ -n "$ZIP_FILE" ]; then
    ZIP_SIZE=$(du -h "$ZIP_FILE" | cut -f1)
    ZIP_NAME=$(basename "$ZIP_FILE")
    
    log_success "Plugin đã được build thành công!"
    echo "=================================================="
    echo -e "${GREEN}📦 File plugin: ${NC}$ZIP_FILE"
    echo -e "${GREEN}📏 Kích thước: ${NC}$ZIP_SIZE"
    echo -e "${GREEN}📝 Tên file: ${NC}$ZIP_NAME"
    echo "=================================================="
    
    # Hướng dẫn cài đặt
    echo -e "${BLUE}🔧 Hướng dẫn cài đặt:${NC}"
    echo "1. Mở IntelliJ IDEA"
    echo "2. Vào File → Settings → Plugins"
    echo "3. Click vào ⚙️ → Install Plugin from Disk"
    echo "4. Chọn file: $ZIP_FILE"
    echo "5. Restart IDE"
    echo ""
    
    # Test template directory
    if [ -d "./.github/PULL_REQUEST_TEMPLATE" ]; then
        TEMPLATE_COUNT=$(find ./.github/PULL_REQUEST_TEMPLATE -name "*.md" | wc -l)
        log_success "Tìm thấy $TEMPLATE_COUNT template files trong .github/PULL_REQUEST_TEMPLATE/"
        echo -e "${BLUE}📋 Templates sẵn có:${NC}"
        find ./.github/PULL_REQUEST_TEMPLATE -name "*.md" -exec basename {} .md \; | sed 's/^/  - /'
    else
        log_warning "Chưa có thư mục .github/PULL_REQUEST_TEMPLATE"
        echo "Plugin sẽ tạo format commit cơ bản nếu không có template"
    fi
    
    echo ""
    log_success "🎉 Hoàn thành! Plugin đã sẵn sàng để cài đặt."
    
else
    log_error "Không tìm thấy file plugin zip!"
    exit 1
fi 