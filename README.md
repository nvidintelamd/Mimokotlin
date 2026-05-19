# Mimokotlin - 可配置的多 Tab WebView 壳

把任意 1~4 个网页打包成 Android APP，支持文件上传和下载。

## 使用方法

### 1. 修改配置

编辑 `app/src/main/assets/config.json`：

```json
{
  "appName": "我的APP",
  "tabs": [
    {"label": "首页", "icon": "🏠", "url": "https://example.com"},
    {"label": "设置", "icon": "⚙️", "url": "https://example.com/settings"}
  ]
}
```

- `appName`: 应用名称
- `tabs`: 1~4 个标签页
  - `label`: 底部标签文字
  - `icon`: 预留字段（当前使用系统图标）
  - `url`: 要加载的网页地址

### 2. 构建 APK

**方式一：Android Studio（推荐）**
1. 用 Android Studio 打开项目目录
2. 等待 Gradle 同步完成
3. Build > Build Bundle(s) / APK(s) > Build APK(s)

**方式二：命令行**
```bash
# 生成 gradle wrapper（仅首次）
bash setup-wrapper.sh

# 构建 debug APK
./gradlew assembleDebug

# 构建 release APK
./gradlew assembleRelease
```

APK 输出位置: `app/build/outputs/apk/`

### 3. 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 功能

- 底部 Tab 导航（1~4 个，数量自适应）
- WebView 完整支持（JS / Cookie / 缓存 / 缩放）
- 文件上传（点击网页中的文件选择按钮）
- 文件下载（通过系统下载管理器，通知栏显示进度）
- 返回键智能处理（网页导航 → Tab 切换 → 退出）
- 顶部加载进度条

## 项目结构

```
app/src/main/
├── assets/
│   └── config.json          ← 唯一需要改的文件
├── java/com/mimokotlin/app/
│   ├── MainActivity.kt      # Tab + ViewPager 容器
│   ├── WebViewFragment.kt   # WebView 核心（可复用）
│   └── Config.kt            # 配置解析
├── res/
│   ├── layout/              # 布局
│   ├── menu/                # 底部导航菜单
│   └── values/              # 颜色/主题/字符串
└── AndroidManifest.xml
```

## 系统要求

- JDK 17+
- Android SDK (compileSdk 34)
- Android Studio 2023.1+ 或 Gradle 8.4+
