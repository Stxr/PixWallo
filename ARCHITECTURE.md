# PixlWallo 架构与开发文档

本文档详细介绍了 PixlWallo 应用的架构设计、模块功能以及开发指南。

## 📐 架构概览

PixlWallo 采用 **MVVM (Model-View-ViewModel) + Repository Pattern** 架构模式，结合 Jetpack Compose 构建现代化的 Android 应用。

### 架构层次

```
┌─────────────────────────────────────┐
│         UI Layer (Compose)          │
│  (Screens, Components, Theme)       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Repository Layer               │
│  (SelectionRepository,              │
│   SettingsRepository)               │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Data Layer                      │
│  (DataStore, Models)                │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Service Layer                   │
│  (SlideshowService,                  │
│   PhotoDreamService)                 │
└─────────────────────────────────────┘
```

## 📦 模块详解

### 1. UI 层 (`ui/`)

#### 1.1 屏幕组件 (`ui/screens/`)

##### HomeScreen.kt
**功能**：应用主界面
- 显示已选图片统计
- 显示播放配置信息（每张时长、播放顺序）
- 提供快速操作入口：
  - 选择图片
  - 电子相框
  - 开始/停止播放
  - 打开屏保设置

**关键特性**：
- 随机选择一张图片作为背景
- 响应式布局，适配不同屏幕尺寸
- Material 3 设计风格

##### PickerScreen.kt
**功能**：图片选择界面
- 支持单张图片选择
- 支持文件夹选择（递归扫描）
- 图片预览和删除功能
- 使用 SAF (Storage Access Framework) 访问文件

**关键实现**：
```kotlin
// 文件夹扫描
fun scanImagesFromFolder(treeUri: Uri): List<Uri>
// 持久化 URI 权限
fun takePersistableTree(uri: Uri)
```

##### PreviewScreen.kt
**功能**：电子相册模式
- 全屏沉浸式显示
- 自动播放图片
- 支持 EXIF 信息显示
- 屏幕常亮功能

**关键特性**：
- 使用 `ImmersiveMode` 实现全屏
- 使用 `ImagePreviewPager` 实现图片浏览
- 自动保持屏幕常亮

##### SettingsScreen.kt
**功能**：设置界面
- 播放配置：
  - 每张图片时长
  - 最大播放时长
  - 播放顺序（顺序/随机）
  - 应用范围（锁屏/主屏/两者）
- EXIF 显示配置：
  - 是否启用点击显示 EXIF
  - EXIF 信息位置

#### 1.2 UI 组件 (`ui/components/`)

##### ImagePreviewPager.kt
**功能**：可复用的图片预览组件
- 使用 `HorizontalPager` 实现左右滑动
- 支持自动播放
- 支持 EXIF 信息显示
- 错误处理和图片预加载

**参数**：
- `images: List<Uri>` - 图片列表
- `initialPage: Int` - 初始页面
- `autoPlay: Boolean` - 是否自动播放
- `showExif: Boolean` - 是否显示 EXIF
- `onDismiss: (() -> Unit)?` - 关闭回调

#### 1.3 沉浸式模式 (`ui/immersive/`)

##### Immersive.kt
**功能**：实现全屏沉浸式体验
- 隐藏系统栏（状态栏、导航栏）
- 保持屏幕常亮
- 使用 `DisposableEffect` 管理生命周期

**实现细节**：
```kotlin
// 隐藏系统栏
controller.hide(WindowInsetsCompat.Type.systemBars())
// 保持屏幕常亮（双重保障）
view.keepScreenOn = true
activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
```

### 2. 数据层 (`data/`)

#### 2.1 SelectionRepository.kt
**功能**：管理选中的图片列表
- 使用 DataStore 持久化存储图片 URI
- 提供 Flow 流式数据访问
- 处理 URI 权限持久化
- 支持文件夹递归扫描

**关键方法**：
- `selectedFlow: Flow<List<Uri>>` - 图片列表流
- `setSelection(uris: List<Uri>)` - 设置选中图片
- `scanImagesFromFolder(treeUri: Uri)` - 扫描文件夹

**数据存储格式**：
```
selected_uris: "uri1\nuri2\nuri3"
```

#### 2.2 SettingsRepository.kt
**功能**：管理应用设置
- 播放配置管理
- EXIF 显示配置管理
- 使用 DataStore 持久化

**配置项**：
- `perItemMs` - 每张图片时长（毫秒）
- `maxDurationMs` - 最大播放时长（可选）
- `idleStopMs` - 空闲停止时长（可选）
- `order` - 播放顺序（SELECTED/RANDOM）
- `applyScope` - 应用范围（LOCK/SYSTEM/BOTH）
- `enableExifTap` - 是否启用 EXIF 点击显示
- `exifPosition` - EXIF 信息位置

#### 2.3 PreferencesDataStore.kt
**功能**：DataStore 单例创建
- 提供统一的 DataStore 实例
- 确保单例模式

### 3. 模型层 (`model/`)

#### Models.kt
**数据模型定义**：

```kotlin
// 播放顺序
enum class PlaybackOrder { SELECTED, RANDOM }

// 应用范围
enum class ApplyScope { LOCK, SYSTEM, BOTH }

// EXIF 位置
enum class ExifPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER }

// 播放配置
data class PlaybackConfig(
    val perItemMs: Long = 10_000,
    val maxDurationMs: Long? = null,
    val idleStopMs: Long? = 30 * 60_000,
    val order: PlaybackOrder = PlaybackOrder.SELECTED,
    val applyScope: ApplyScope = ApplyScope.LOCK,
    val enableExifTap: Boolean = true,
    val exifPosition: ExifPosition = ExifPosition.BOTTOM_RIGHT
)
```

### 4. 服务层

#### 4.1 SlideshowService.kt (`slideshow/`)
**功能**：壁纸轮播服务
- 前台服务，持续运行
- 定时切换壁纸
- 通知栏控制
- 支持暂停/继续、上一张/下一张

**关键实现**：
```kotlin
// 启动轮播
private fun startLoop() {
    // 创建协程任务
    job = scope.launch {
        // 获取配置和图片列表
        // 循环切换壁纸
        while (isActive) {
            applyUri(uri, cfg.applyScope)
            delay(cfg.perItemMs)
        }
    }
}
```

**通知操作**：
- `ACTION_PREV` - 上一张
- `ACTION_NEXT` - 下一张
- `ACTION_TOGGLE` - 暂停/继续
- `ACTION_STOP` - 停止

#### 4.2 PhotoDreamService.kt (`dream/`)
**功能**：Android Daydream 屏保服务
- 继承 `DreamService`
- 全屏显示图片
- 显示时间和日期
- 自动切换图片

**关键特性**：
- 非交互式屏保
- 全屏显示
- 屏幕亮度设置
- StandBy 风格的时间显示

#### 4.3 WallpaperApplier.kt (`wallpaper/`)
**功能**：壁纸应用器
- 加载图片并裁剪到合适尺寸
- 应用到锁屏/主屏
- 使用 Coil 加载图片

**实现细节**：
```kotlin
suspend fun setFromUri(uri: Uri, scope: ApplyScope) {
    val target = desiredSize()  // 获取壁纸目标尺寸
    val bmp = loadCroppedBitmap(uri, target)  // 加载并裁剪
    // 根据 scope 应用到相应位置
}
```

### 5. 工具类 (`util/`)

#### ExifReader.kt
**功能**：读取图片 EXIF 信息
- 拍摄时间
- 相机信息
- GPS 位置信息
- 图片尺寸和方向

#### DreamHelper.kt
**功能**：屏保相关辅助功能

#### PermissionHelper.kt
**功能**：权限管理辅助

## 🔄 数据流

### 图片选择流程

```
用户选择图片
    ↓
PickerScreen 调用 SelectionRepository.setSelection()
    ↓
DataStore 保存 URI 列表
    ↓
selectedFlow 发出新数据
    ↓
HomeScreen/PreviewScreen 自动更新
```

### 壁纸轮播流程

```
用户点击"开始播放"
    ↓
SlideshowService.start()
    ↓
启动前台服务
    ↓
读取 SelectionRepository 和 SettingsRepository
    ↓
创建协程循环
    ↓
定时调用 WallpaperApplier.setFromUri()
    ↓
WallpaperManager 应用壁纸
```

### 设置更新流程

```
用户在 SettingsScreen 修改配置
    ↓
SettingsRepository.update()
    ↓
DataStore 保存新配置
    ↓
configFlow 发出新数据
    ↓
SlideshowService 读取新配置并应用
```

## 🎯 设计模式

### Repository Pattern
- **SelectionRepository**：管理图片选择数据
- **SettingsRepository**：管理应用配置数据
- 提供统一的数据访问接口
- 隐藏数据存储实现细节

### Observer Pattern
- 使用 Kotlin Flow 实现响应式数据流
- UI 组件通过 `collectAsState()` 订阅数据变化
- 自动更新 UI

### Service Pattern
- **SlideshowService**：后台壁纸轮播
- **PhotoDreamService**：屏保服务
- 使用前台服务确保后台运行

## 🔧 开发指南

### 添加新功能

#### 1. 添加新的设置项

1. 在 `Models.kt` 中扩展 `PlaybackConfig`
2. 在 `SettingsRepository.kt` 中添加对应的 Key 和映射逻辑
3. 在 `SettingsScreen.kt` 中添加 UI 控件

#### 2. 添加新的图片源

1. 扩展 `SelectionRepository` 添加新的选择方法
2. 在 `PickerScreen.kt` 中添加对应的 UI

#### 3. 自定义壁纸应用逻辑

1. 修改 `WallpaperApplier.kt`
2. 可以添加图片处理、滤镜等功能

### 代码规范

- **命名**：使用清晰的 Kotlin 命名约定
- **函数**：单一职责原则
- **注释**：关键功能添加 KDoc 注释
- **错误处理**：使用 try-catch 处理异常

### 测试建议

- **单元测试**：Repository 层的数据操作
- **UI 测试**：关键用户流程
- **集成测试**：服务启动和壁纸应用

## 🐛 常见问题

### 1. 图片无法加载
- 检查 URI 权限是否持久化
- 确认文件是否存在
- 检查存储权限

### 2. 壁纸无法应用
- 确认 `SET_WALLPAPER` 权限
- 检查图片尺寸是否合适
- 查看日志错误信息

### 3. 屏保不工作
- 确认在系统设置中启用了屏保
- 检查 `PhotoDreamService` 是否正确注册
- 确认 `dream_info.xml` 配置正确

## 📚 依赖库说明

- **Coil**：图片加载库，支持 URI、网络图片等
- **DataStore**：现代化数据存储，替代 SharedPreferences
- **Navigation Compose**：声明式导航
- **Accompanist Permissions**：权限请求辅助库

## 🔮 未来规划

- [ ] 支持网络图片
- [ ] 添加图片编辑功能
- [ ] 支持更多图片格式
- [ ] 添加主题切换
- [ ] 支持云同步
- [ ] 添加统计功能

## 📝 更新日志

### v1.0
- 基础架构搭建
- 实现核心功能
- 完成文档编写

---

**最后更新**：2025-11-15

