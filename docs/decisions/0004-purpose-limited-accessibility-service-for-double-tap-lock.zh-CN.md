# ADR-0004：用于绑定锁屏的用途受限 AccessibilityService 边界

## 状态

- 值：`Active`
- 更新日期：2026-09-20
- 依据：作者授权将绑定驱动的锁屏触发与服务 mainline 化作为迭代 32 的生产实现。迭代 10 已验收的用途受限服务边界仍然有效。

## 日期

2026-08-16

## 背景

锁屏是可选的 Home 能力。绑定为锁屏的 Home 快捷操作槽位，在用户通过系统设置明确启用服务并查看本地 Privacy 和显著披露后，通过 Android 的 `AccessibilityService` API 请求一次 `GLOBAL_ACTION_LOCK_SCREEN` 操作。

无障碍服务是具有重大隐私、安全和平台政策影响的特权平台能力。商店和平台审核对此类服务的使用方式、使用时机和使用原因施加严格约束。当前产品契约明确将服务限制于单一用途和边界。

迭代 10 首先以 debug-only 双击锁屏实现此边界。迭代 32 将触发泛化为已绑定的 Home 快捷操作槽位，并把同一服务、manifest 声明与配置移入 mainline，使该能力存在于每一种构建类型。

## 决策

Launcher4Max 的无障碍服务仅用于支持已绑定的锁屏快捷操作。其边界如下：

- **唯一用途**：在用户于当前绑定为锁屏的 Home 基础信息空白槽位上执行明确手势后，执行一次 `GLOBAL_ACTION_LOCK_SCREEN` 操作。
- **Manifest 声明**：服务使用请求全局操作所需的最小 `accessibilityService` XML 配置。不声明 `flagRequestFilterKeyEvents`、`flagIncludeNotImportantViews`、`flagReportViewIds`、`flagRetrieveInteractiveWindows` 或任何提供窗口内容检索的能力。
- **权限**：服务组件受 `android.permission.BIND_ACCESSIBILITY_SERVICE` 保护，仅系统可以绑定。应用不声明该权限、`SYSTEM_ALERT_WINDOW` 或任何设备管理权限的 `<uses-permission>`。
- **数据访问**：服务不读取或保留任何窗口内容、屏幕内容、无障碍事件或其他应用数据。`AccessibilityLockProbeService` 忽略 `onAccessibilityEvent`、`onInterrupt` 和除 `onServiceConnected`、`onUnbind`、`onDestroy` 以外的所有其他事件回调。
- **全局操作**：服务仅在已绑定的锁屏手势被识别时请求 `GLOBAL_ACTION_LOCK_SCREEN`。不执行任何其他全局操作、后台自动化或持续监控。
- **安全失败行为**：如果服务被禁用、撤销、断连，或锁屏操作不可用或被拒绝，该能力静默不可用；已连接请求被拒绝时仅显示契约规定的失败 toast。所有独立 Launcher 路径（Home、Drawer、应用启动、Settings）保持不受影响。
- **用户控制**：服务保持禁用状态，直到用户在 Android 无障碍设置中明确启用。用户可随时禁用而不影响独立 Launcher 功能。不存在应用内开关；控制通过平台设置路径进行。
- **披露**：用户启用服务前，快捷操作设置页展示当前本地 Privacy 描述和独立显著披露，说明服务唯一用途、数据访问边界和关闭路径。披露使用 `取消` 和 `同意并继续` 选项，不保留已确认历史。
- **Home 手势边界**：绑定手势识别仅在 `editMode` 为 `false` 时在基础信息区符合条件的空白位置启用。时间、日期、收藏、编辑表面和所有其他交互目标从识别中排除。绑定为无操作的槽位不执行任何动作。
- **连接模型**：`AccessibilityLockConnection` 是每一种构建类型中的进程内应用-服务连接点。不拥有任何 `Context`、事件或窗口数据，也不在应用进程外持久化状态。`AccessibilityLockProbeService` 在 `onServiceConnected` 时调用 `AccessibilityLockConnection.connected(this)`，在 `onUnbind`/`onDestroy` 时调用 `disconnected(this)`。
- **生产集成**：服务类、manifest 声明和 `accessibilityService` XML 位于 mainline 源集，并打包进每一种构建类型。应用图在每一种构建类型中接线 `AndroidAccessibilityLockController`，并保持相同的 fail-closed 请求行为。

## 理由

此边界确保无障碍服务是一个狭窄的、用户控制的工具，按需执行仅一个操作。它使服务用途、权限和数据访问行为保持透明和可审查。安全失败设计保证任何服务状态变更都不会破坏核心 Launcher 功能。

使用平台的无障碍设置和披露系统保持用户授权明确且平台原生。当前产品 Privacy 声明和显著披露正文提供了服务做什么和不做什么的契约。

连接模型使服务保持无状态，不引入持久后台桥接或共享数据存储，避免需要更广泛的架构审查。

将触发绑定到 Home 快捷操作槽位，而不是硬编码双击，可在保持同一服务边界的同时匹配已验收的快捷操作契约。

## 考虑的选项

### 使用设备管理员 API

- 优势：`DevicePolicyManager.lockNow()` 是 Android 中程序化锁屏的既定机制。
- 权衡：需要 `DEVICE_ADMIN` 权限、设备管理注册和通过专门系统路径撤销。平台政策限制其使用，商店可能审查或拒绝非企业用途。当前产品不需要其更广泛的管理能力。

### 添加 Home 覆盖层并手动管理触摸

- 优势：完全避免无障碍服务权限和平台政策边界。
- 权衡：需要系统覆盖权限、复杂手势处理，且在其他覆盖层激活时不可用。还可能干扰 Android 原生手势系统，且不提供更清晰的授权故事。

### 使用用途受限的 AccessibilityService（已选择）

- 优势：使用平台明确的授权和设置路径，仅需一个最小权限，执行所需操作而不进行持续监控。当前产品边界和 Privacy 正文围绕此方法编写。
- 权衡：平台政策和商店可能审查其使用，且服务必须由用户明确启用。边界必须在代码、披露和验证中保持，以满足可接受的平台约束。

## 后果

- 不得通过新的活跃 ADR 将无障碍服务扩展为读取窗口内容、观察其他应用、收集无障碍事件、执行后台自动化或添加任何其他全局操作。
- 即使 GitHub 分发的构建已验收，商店分发仍需要重新审核平台政策和披露。
- 服务必须保持可选，且不得成为任何独立 Launcher 路径的要求。
- `android:accessibilityService` XML 不得扩展以请求超出当前边界所需的能力。
- 未来平台兼容性测试必须验证服务继续仅请求 `GLOBAL_ACTION_LOCK_SCREEN`，且在 API 31–37 之间保持安全失败行为。
- Release 构建现在声明该服务。用户进入授权流程后，release 安装可以出现在系统无障碍列表中；在此之前升级时不会发生行为变化。

## 验证证据和缺口

- 迭代 10 在作者主要设备上的物理设备验证确认，当服务启用时，Home 上符合条件的空白位置的有效双击请求一次锁屏操作，且撤销或禁用服务后独立 Launcher 路径不受影响。该证据仍支持用途受限的服务边界。
- 当前实现确认 `onAccessibilityEvent` 和 `onInterrupt` 为空操作，仅请求 `GLOBAL_ACTION_LOCK_SCREEN`，且连接模型不拥有持久数据。
- Settings UI、Privacy 呈现和显著披露显示当前绑定手势正文。
- 当前 mainline manifest 和 `accessibilityService` XML 确认未声明任何窗口内容、按键事件或扩展视图能力。
- 迭代 32 的绑定手势触发设备旅程已由作者于 2026-09-20 在主物理设备上接受。绑定锁屏手势的专用 release APK 验证在与迭代 34 产物工作协调前保持 `Unknown`。作者运行的 `./gradlew lint` 已通过；无需刷新 lint baseline。代理运行的 Gradle 仍为 `Not run`。
- API 31 和另一台 API 36 或 API 37 物理设备覆盖仍为建议的兼容性证据。未执行的 OEM 和 Private Space 场景本身不使本决定无效，但必须在版本记录中记录为 `Unknown`、`Not run` 或 `Unavailable`。

## 实现说明

- `AccessibilityLockProbeService`、其 manifest 声明和 `accessibility_lock_probe_config.xml` 位于 mainline 源集，XML 约束与迭代 10 验证的相同。
- `AccessibilityLockConnection` 是一个进程内单例，提供无状态 `LockRequestPort` 适配器。它不跨进程重启持久化，且不得转换为后台服务或持久桥接。
- 绑定手势识别使用 `detectTapGestures`，并将 `pointerInput` 限定在符合条件的 Home 空白位置，且仅当 `!editMode`。手势不得移动到更广泛的 Home 表面修饰符。由槽位解析而非硬编码双击决定已识别手势是否请求锁屏。

## 实现和验证证据

- 迭代 10 记录：`docs/delivery/1.1.0/iteration-10-double-tap-lock.md`
- 迭代 32 契约：`docs/delivery/1.7.0/iteration-32-quick-actions-screen-locking.md`
- 用途受限边界的物理设备验证：作者报告迭代 10 在主要设备上成功的锁屏行为、Settings 状态刷新、披露流程和撤销/安全失败行为
- 实现文件：
  `app/src/main/java/com/maxarcher/launcher/AccessibilityLockProbeService.kt`、
  `app/src/main/java/com/maxarcher/launcher/AccessibilityLock.kt`、
  `app/src/main/AndroidManifest.xml`、
  `app/src/main/res/xml/accessibility_lock_probe_config.xml`
- Settings 集成：`SettingsScreen.kt` 快捷操作设置页、Privacy 与披露资源
- Home 手势集成：`HomeBasicInformation.kt` 中符合条件的空白位置内的绑定槽位检测

## 替换

无

## 停用

- 日期：处于 `Active` 状态时不适用
- 原因：处于 `Active` 状态时不适用
- 替换为：无
- 后果：无
