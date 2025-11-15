## 背景与目标
- 目标：实现一款支持锁屏壁纸轮播的 Android 应用，涵盖相册选择、预览、自动/手动切换、播放参数设置与自动停止。
- 当前代码库：已启用 Jetpack Compose，minSdk 27，无壁纸/图片加载/DI 相关依赖；无 XML 布局。

## 技术选型
- UI：Jetpack Compose（Material3、Navigation-Compose、Accompanist-Permissions 可选）。
- 图片加载与解码：Coil（Compose 端 `AsyncImage`，后台端 `ImageLoader.decode`）。
- 媒体选择：Android Photo Picker（`ActivityResultContracts.PickVisualMedia`/`PickMultipleVisualMedia`）。在 API<33 自动回落到 SAF，不需存储权限。
- 壁纸设置：`WallpaperManager#setBitmap(..., which=FLAG_LOCK|FLAG_SYSTEM)`；按需设置 LOCK 与 SYSTEM。
- 后台与定时：前台服务 + Kotlin Coroutines 定时；通知栏操作按钮；可选 Quick Settings Tile。
- 配置持久化：Jetpack DataStore Preferences。
- 依赖注入：初期可不引入，后续可平滑接入 Hilt。
- 兼容与测试：Robolectric 对 `WallpaperManager` 进行 Shadow 单测；真机/模拟器手工验证。

## 权限与清单
- 常规：`android.permission.SET_WALLPAPER`、`android.permission.SET_WALLPAPER_HINTS`。
- 存储/媒体：使用 Photo Picker 与 SAF，无需 `READ_MEDIA_IMAGES`/`READ_EXTERNAL_STORAGE`。
- 启动项：可选 `RECEIVE_BOOT_COMPLETED`（用于在开机后恢复“自动轮播进行中”的状态）。
- 服务：前台服务声明，通知渠道。

## 架构与模块
- 层次：MVVM + 单 Activity + 多屏 Compose。
- 模块
  - `feature-picker`：图片选择、选中集管理、预览与移除。
  - `feature-slideshow`：播放控制（开始/暂停/下一张/上一张）、顺序与随机、定时器与前台服务。
  - `feature-settings`：时长设置、总时长、无操作自动关闭阈值。
  - `core-wallpaper`：尺寸适配、裁剪与缩放、`WallpaperManager` 封装、双屏（锁屏/主屏）设置策略。
  - `core-data`：DataStore、选中图片持久化、URI 权限持久化。
  - `core-ui`：主题、导航、通用组件。

## 数据模型
- `SelectedImage`: `uri: Uri`, `addedAt: Long`。
- `PlaybackOrder`: `SELECTED`, `RANDOM`。
- `PlaybackConfig`: `perItemMs: Long`, `maxDurationMs: Long?`, `idleStopMs: Long?`, `applyTo: Lock|System|Both`。
- `PlaybackState`: `index: Int`, `isPlaying: Boolean`, `startedAt: Long`, `lastUserActionAt: Long`。

## 关键流程与实现
- 图片选择与预览
  - 使用 `PickMultipleVisualMedia` 获取 `List<Uri>`。
  - 对于 `ACTION_OPEN_DOCUMENT` 回落，调用 `contentResolver.takePersistableUriPermission`；持久化到 DataStore/本地表。
  - 预览界面：网格 + 详情页（Pager），支持移除与重排。
- 壁纸适配与裁剪
  - 目标尺寸：`WallpaperManager.getDesiredMinimumWidth/Height` 与当前 `WindowMetrics`。
  - 解码：Coil 以目标尺寸 decode，`allowHardware(false)`，避免硬件位图。
  - 裁剪：计算居中裁剪矩阵，保持比例填充（类似 centerCrop），输出 `Bitmap`。
  - 设置：`wallpaperManager.setBitmap(bitmap, null, true, FLAG_LOCK or FLAG_SYSTEM)`；API>=24 分开设置锁屏与主屏。
- 轮播（自动/手动）
  - 前台服务（`SlideshowService`）：
    - `startForeground` 后使用 `CoroutineScope` + `delay(perItemMs)` 循环；
    - 通知提供 `Prev`、`Pause/Play`、`Next` Action；
    - 维护 `PlaybackState` 到 DataStore，供 UI 同步观察。
  - 顺序：
    - `SELECTED`：按列表顺序；
    - `RANDOM`：构建洗牌队列，遍历完再重建，避免重复；
  - 手动切换：
    - App 内：详情页左/右滑切换并立即调用设置；
    - 锁屏场景：通过通知 Action 或 Quick Settings Tile 触发切换；
- 播放参数设置
  - 每张时长：预设 5s/10s/30s + 自定义输入（秒→毫秒）。
  - 自动播放总时长：`maxDurationMs` 到期即 `stopSelf`；
  - 无操作自动关闭：跟踪 `lastUserActionAt`，当 `now - lastUserActionAt > idleStopMs` 则停止；
  - 交互来源：UI 交互、通知 Action、Tile 点击均更新 `lastUserActionAt`。

## 关键 API 片段（示例）
- 选择多图
```kotlin
val picker = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
    viewModel.onImagesPicked(uris)
}
```
- 设置锁屏壁纸
```kotlin
val wm = WallpaperManager.getInstance(context)
wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
```
- 前台服务循环
```kotlin
while (isActive && state.isPlaying) {
    applyNext()
    delay(config.perItemMs)
}
```

## UI 结构
- Home：状态总览（已选数量、当前播放状态）、入口到“选择图片/预览/设置”。
- Picker：多选栅格、顶部显示已选计数、完成入库。
- Preview：ViewPager2 或 Compose Pager，支持左右滑、移除、设为当前。
- Settings：
  - 播放顺序：按选中/随机；
  - 每张时长：5/10/30 秒 + 自定义；
  - 自动停止：总时长、无操作阈值；
  - 应用范围：锁屏/主屏/二者。
- 播放控制：开始/暂停/上一张/下一张；通知栏提供同等操作。

## 适配与边界
- API 兼容：minSdk 27；API 24+ 可单独设锁屏；低于 24 不支持（已覆盖）。
- 设备差异：部分 OEM 对锁屏更新节流；通过前台服务保证稳定。
- 性能：避免高频切换（建议 ≥10s）；Coil 目标尺寸解码，回收位图，避免 OOM。
- URI 失效：对不可访问的 URI 进行重试与跳过，并提示用户重新选择。

## 数据与状态持久化
- DataStore 保存：`SelectedImage` 列表（JSON/Proto 简化）、`PlaybackConfig`、`PlaybackState`。
- Persistable URI 权限：应用重启后仍可访问所选媒体。

## 通知与快速控制
- 前台通知：显示当前进度与时长倒计时；Action：Prev/Play-Pause/Next/Stop。
- Quick Settings Tile：一键下一张、切换播放状态。

## 测试与验证
- 单元：
  - 播放序列与随机洗牌不重复；
  - 定时器在暂停/恢复/停止下的边界；
  - 裁剪矩阵在不同宽高比的输出尺寸正确。
- 仪器：
  - 选择器回调与 URI 权限持久化；
  - 真机验证锁屏与主屏壁纸更新。

## 里程碑
- M1：基础框架与依赖、数据模型、选择与预览可用。
- M2：壁纸裁剪与单次设置流程，锁屏/主屏标签。
- M3：前台服务轮播、通知控制、播放参数生效。
- M4：设置页与数据持久化、开机恢复（可选）。
- M5：稳定性优化、功耗评估、测试覆盖。

## 风险与规避
- 高频更换壁纸的功耗与 OEM 限制：建议默认最小时长≥10s，前台服务明确告知耗电影响。
- 某些设备禁用锁屏单独设置：提供回退到“同时设置系统与锁屏”。
- URI 访问失效：落入重选流程，提供一键重新扫描已选项。

## 交付物
- Compose 界面与导航
- 壁纸设置核心库（尺寸适配、FLAG_LOCK/FLAG_SYSTEM 支持）
- 前台服务与通知控制
- Photo Picker 集成与 URI 权限持久化
- 设置页与 DataStore 配置
- 测试用例与验证清单