#!/bin/bash
# ============================================================
#  Gradle Wrapper 引导脚本
#  如果 gradle-wrapper.jar 不存在，自动下载并生成
# ============================================================

set -e
cd "$(dirname "$0")"

WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
GRADLE_VERSION="8.4"

if [ -f "$WRAPPER_JAR" ]; then
    echo "gradle-wrapper.jar 已存在，跳过。"
    exit 0
fi

echo "=== Gradle Wrapper 初始化 ==="
echo "下载 Gradle $GRADLE_VERSION ..."

GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
TEMP_DIR=$(mktemp -d)

# 下载 gradle
if command -v curl &>/dev/null; then
    curl -L -o "$TEMP_DIR/gradle.zip" "$GRADLE_URL"
elif command -v wget &>/dev/null; then
    wget -O "$TEMP_DIR/gradle.zip" "$GRADLE_URL"
else
    echo "错误: 需要 curl 或 wget"
    exit 1
fi

echo "解压 ..."
unzip -q "$TEMP_DIR/gradle.zip" -d "$TEMP_DIR"

echo "生成 wrapper ..."
"$TEMP_DIR/gradle-${GRADLE_VERSION}/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION"

# 清理
rm -rf "$TEMP_DIR"

echo ""
echo "=== 完成 ==="
echo "现在可以用 ./gradlew assembleDebug 构建项目了"
