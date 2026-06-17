# MIMO_APP_Kotlin

可配置的多 Tab WebView 壳，把 1~4 个网页打包成 Android APP。

## 功能

- 底部 Tab 导航（1~4 个，数量自适应）
- 自定义 SVG 矢量图标
- WebView 完整支持（JS / Cookie / 缓存 / 缩放）
- 文件上传（点击网页中的文件选择按钮）
- 文件下载（通过系统下载管理器，通知栏显示进度）
- 返回键智能处理（WebView 历史 → Tab 切换 → 退出）
- 双击 Tab 手动刷新页面
- 顶部加载进度条
- 内部域名白名单（xiaomimimo.com / mi.com / xiaomi.com）

## 配置

编辑 `app/src/main/assets/config.json`：

```json
{
  "appName": "MIMO",
  "tabs": [
    {"label": "对话", "url": "https://example.com"},
    {"label": "设置", "url": "https://example.com/settings"}
  ]
}
```

- `appName`: 应用名称
- `tabs`: 1~4 个标签页
  - `label`: 底部标签文字
  - `url`: 要加载的网页地址

## 构建

**命令行**

```bash
# 生成 gradle wrapper（仅首次）
bash setup-wrapper.sh

# 构建 debug APK
./gradlew assembleDebug

# 构建 release APK
./gradlew assembleRelease
```

APK 输出位置: `app/build/outputs/apk/`

**安装到手机**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/
├── assets/
│   └── config.json              ← 唯一需要改的配置文件
├── java/com/mimokotlin/app/
│   ├── MainActivity.kt          # Tab + ViewPager 容器
│   ├── WebViewFragment.kt       # WebView 核心
│   ├── TopSwipeRefreshLayout.kt # 下拉刷新
│   └── Config.kt                # JSON 配置解析
├── res/
│   ├── drawable/                # SVG 矢量图标
│   ├── layout/                  # 布局文件
│   ├── menu/                    # 底部导航菜单
│   └── values/                  # 颜色/主题/字符串
└── AndroidManifest.xml
```

## 系统要求

- JDK 17+
- Android SDK (compileSdk 34, minSdk 24)
- Android Studio 2023.1+ 或 Gradle 8.4+
