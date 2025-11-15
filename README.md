# PixlWallo

一个现代化的 Android 壁纸轮播应用，支持自动切换壁纸、电子相册模式和屏保功能。

## 📱 功能特性

### 核心功能
- **壁纸轮播**：自动定时切换锁屏/主屏壁纸
- **电子相册模式**：全屏沉浸式图片浏览，支持自动播放
- **屏保模式**：支持 Android Daydream 屏保功能
- **图片选择**：支持单张图片和文件夹批量选择
- **EXIF 信息显示**：查看图片的拍摄信息（时间、位置等）

### 高级特性
- **播放顺序**：支持顺序播放和随机播放
- **应用范围**：可选择应用到锁屏、主屏或两者
- **自定义时长**：可设置每张图片的显示时长
- **屏幕常亮**：电子相册模式下自动保持屏幕常亮
- **沉浸式体验**：全屏显示，隐藏系统栏
- **通知控制**：通过通知栏快速控制播放

## 🛠️ 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构模式**：MVVM + Repository Pattern
- **异步处理**：Kotlin Coroutines + Flow
- **数据存储**：DataStore Preferences
- **图片加载**：Coil
- **导航**：Navigation Compose
- **权限管理**：Accompanist Permissions

## 📋 系统要求

- **最低 Android 版本**：Android 8.1 (API 27)
- **目标 Android 版本**：Android 15 (API 35)
- **编译 SDK**：35

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 11 或更高版本
- Gradle 8.7.3

### 构建步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd PixlWallo
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目
   - 等待 Gradle 同步完成

3. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击运行按钮或使用快捷键 `Shift+F10`

### 首次使用

1. **选择图片**
   - 点击"选择图片"按钮
   - 可以选择单张图片或整个文件夹
   - 支持递归扫描文件夹中的所有图片

2. **配置设置**
   - 进入设置页面
   - 配置播放时长、顺序、应用范围等

3. **开始使用**
   - **壁纸轮播**：点击"开始播放"按钮，应用会自动切换壁纸
   - **电子相册**：点击"电子相框"按钮，进入全屏浏览模式
   - **屏保模式**：在系统设置中启用屏保功能

## 📖 使用说明

### 壁纸轮播

1. 选择至少一张图片
2. 在设置中配置播放参数：
   - **每张时长**：每张图片显示的时长（秒/分钟/小时）
   - **播放顺序**：顺序或随机
   - **应用范围**：锁屏、主屏或两者
3. 点击"开始播放"按钮
4. 通过通知栏可以控制播放：上一张、下一张、暂停/继续、停止

### 电子相册模式

1. 选择图片后，点击"电子相框"按钮
2. 进入全屏浏览模式，图片会自动切换
3. 点击屏幕可以查看/隐藏 EXIF 信息
4. 屏幕会自动保持常亮，不会熄屏

### 屏保模式

1. 在应用首页点击"打开屏保设置"
2. 在系统设置中选择 "PixlWallo" 作为屏保
3. 设置屏保触发时间
4. 当设备进入屏保状态时，会自动显示选中的图片

## 🔧 权限说明

应用需要以下权限：

- **SET_WALLPAPER**：设置壁纸
- **SET_WALLPAPER_HINTS**：设置壁纸提示
- **FOREGROUND_SERVICE**：前台服务（用于壁纸轮播）
- **POST_NOTIFICATIONS**：显示通知（Android 13+）
- **RECEIVE_BOOT_COMPLETED**：开机自启动（可选）
- **存储权限**：通过 SAF (Storage Access Framework) 访问图片

## 📁 项目结构

```
app/src/main/java/com/example/pixlwallo/
├── data/                    # 数据层
│   ├── PreferencesDataStore.kt
│   ├── SelectionRepository.kt
│   └── SettingsRepository.kt
├── dream/                   # 屏保功能
│   └── PhotoDreamService.kt
├── model/                   # 数据模型
│   └── Models.kt
├── slideshow/               # 壁纸轮播服务
│   ├── BootReceiver.kt
│   ├── Notifications.kt
│   └── SlideshowService.kt
├── ui/                      # UI 层
│   ├── AppRoot.kt
│   ├── components/
│   │   └── ImagePreviewPager.kt
│   ├── immersive/
│   │   └── Immersive.kt
│   ├── screens/
│   │   ├── HomeScreen.kt
│   │   ├── PickerScreen.kt
│   │   ├── PreviewScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── util/                    # 工具类
│   ├── DreamHelper.kt
│   ├── ExifReader.kt
│   └── PermissionHelper.kt
└── wallpaper/               # 壁纸应用
    └── WallpaperApplier.kt
```

## 🎨 界面预览

- **首页**：显示已选图片数量、播放配置，提供快速操作入口
- **图片选择页**：支持单张选择和文件夹选择，实时预览
- **设置页**：详细的播放配置选项
- **电子相册**：全屏沉浸式图片浏览

## 🔄 更新日志

### v1.0
- 初始版本发布
- 支持壁纸轮播功能
- 支持电子相册模式
- 支持屏保功能
- 支持 EXIF 信息显示

## 📝 开发说明

详细的架构和开发文档请参考 [ARCHITECTURE.md](./ARCHITECTURE.md)

## 📄 许可证

[在此添加许可证信息]

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📮 联系方式

[在此添加联系方式]

