# Changelog

## [2025-11-21]

### ✨ 新功能

#### 图片方向过滤
- 新增图片方向过滤器功能，支持按横屏/竖屏筛选图片
- 在图片选择页面（PickerScreen）添加方向过滤器 UI，支持"全部"、"横屏"、"竖屏"三种模式
- 在首页（HomeScreen）和预览页面（PreviewScreen）应用方向过滤器
- 图片方向信息异步加载，提升性能

#### 竖屏照片显示策略
- 新增三种竖屏照片显示模式：
  - **填满屏幕（裁剪）**：竖屏照片填满屏幕，可能被裁剪
  - **完整显示（留黑）**：竖屏照片完整显示，两侧留黑边
  - **智能旋转（填满）**：竖屏照片自动旋转 90 度以填满横屏屏幕
- 在设置页面（SettingsScreen）添加竖屏照片显示策略配置
- 在图片预览组件（ImagePreviewPager）和屏保服务（PhotoDreamService）中实现显示策略

#### 横屏模式优化
- 屏保服务（PhotoDreamService）强制横屏显示，使用 `SENSOR_LANDSCAPE` 方向
- 预览页面（PreviewScreen）强制横屏显示
- AndroidManifest.xml 中为屏保服务添加横屏方向限制

### 🔧 改进

#### EXIF 信息读取优化
- 改进光圈值（F-number）解析，支持 Rational 格式（如 "35/10"）
- 改进曝光时间解析，支持 Rational 格式（如 "1/60"），并优化显示格式
- 改进焦距读取，优先使用等效焦距（35mm 格式），支持小数显示
- 改进 ISO 读取，支持更多 ISO 标签类型（包括 RW2_ISO）

#### 用户体验优化
- 图片选择页面显示过滤后的图片数量（如 "5/10 张"）
- 首页显示过滤后的图片数量
- 优化图片方向信息的异步加载和缓存机制
- 删除图片时同步清除方向缓存

### 📝 技术改进

- 在 `PlaybackConfig` 数据模型中添加 `imgDisplayMode` 字段
- 在 `SettingsRepository` 中添加方向过滤器状态管理
- 在 `ImagePreviewPager` 中实现图片旋转变换（RotationTransformation）
- 优化图片加载逻辑，根据图片方向和配置动态调整显示方式

### 📊 统计

- **12 个文件**被修改
- **693 行**新增代码
- **72 行**删除代码
- 净增加 **621 行**代码

---

## 修改文件列表

1. `app/src/main/AndroidManifest.xml` - 添加屏保服务横屏限制
2. `app/src/main/java/com/example/pixlwallo/data/SettingsRepository.kt` - 添加显示模式和方向过滤器支持
3. `app/src/main/java/com/example/pixlwallo/dream/PhotoDreamService.kt` - 实现竖屏照片智能显示和横屏强制
4. `app/src/main/java/com/example/pixlwallo/model/Models.kt` - 添加 ImgDisplayMode 和 OrientationFilter 枚举
5. `app/src/main/java/com/example/pixlwallo/ui/components/ImagePreviewPager.kt` - 实现竖屏照片智能显示
6. `app/src/main/java/com/example/pixlwallo/ui/screens/HomeScreen.kt` - 添加方向过滤器功能
7. `app/src/main/java/com/example/pixlwallo/ui/screens/PickerScreen.kt` - 添加方向过滤器 UI
8. `app/src/main/java/com/example/pixlwallo/ui/screens/PreviewScreen.kt` - 添加方向过滤器和横屏强制
9. `app/src/main/java/com/example/pixlwallo/ui/screens/SettingsScreen.kt` - 添加竖屏照片显示策略设置
10. `app/src/main/java/com/example/pixlwallo/util/ExifReader.kt` - 优化 EXIF 信息读取
11. `app/src/main/java/com/example/pixlwallo/util/DreamHelper.kt` - 代码格式调整
12. `app/src/main/java/com/example/pixlwallo/util/PermissionHelper.kt` - 代码格式调整

