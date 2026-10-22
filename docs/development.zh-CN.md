# 开发指南

> 英文语义源：[development.md](development.md)。本文件是其中文对应版本。

## 目的

本文记录 Launcher4Max 当前最小开发基线。它描述项目配置和可用入口；除非对应验证证据记录了结果，否则不表示某条命令已经成功执行。

## 当前项目配置

- 仓库根目录同时是 Android 工程根目录。
- 工程结构：一个 Gradle 应用模块 `:app`。
- 构建脚本：Kotlin DSL。
- Gradle Wrapper 分发版本：Gradle `9.4.1`，当前通过阿里云分发镜像配置。
- Android Gradle Plugin：`9.2.1`。
- Kotlin：`2.3.10`。
- Java 语言级别：JDK 17。
- Android 配置：`minSdk 31`、`targetSdk 36`、`compileSdk 37`。
- 应用标识与 namespace：`com.maxarcher.launcher`。
- main 与仪器测试的 Kotlin 包使用 `com.maxarcher.launcher`。
- 默认测试 namespace 与测试 APK 标识为 `com.maxarcher.launcher.test`。
- 替换旧应用标识的安装前，请阅读[发布迁移说明](release.zh-CN.md#应用标识迁移)。
- 当前配置版本标识：`versionName 1.7.2`、配置的 `versionCode 10`；`1.7.0` 已在[交付记录](delivery/1.7.0/delivery.zh-CN.md)中关闭。
- 依赖仓库当前使用阿里云镜像。`settings.gradle.kts` 保留了被注释的官方上游替代配置，供有意识地手动切换。

这些值是当前仓库配置，不表示每台主机都已具备匹配的 JDK、Android SDK、模拟器、设备连接、凭据或依赖缓存。

## 前置条件

- JDK 17。
- 能够提供当前配置所需平台和构建工具的 Android SDK。
- 足以解析当前 Gradle 分发、插件和依赖的网络或本地缓存。
- 安装或执行 instrumentation 验证时，需要 Android 工具能够识别的设备或模拟器。

准确的 Android SDK 软件包清单、受支持 IDE 版本和主机特定环境变量设置，尚未成为项目统一要求。只有确认某项要求确实必要后才记录于此，不得根据单台设备推断。

## 本地签名

`debug` 与 `release` 两个 build type 共用本地 `release` signing config，
并使用上文所列的同一个 `applicationId`。`debug` 可调试，`release` 不可调试。

Gradle 配置前，需在仓库根目录提供不受版本控制跟踪的 `keystore.properties`，
并准备实际 keystore 文件。以下四个属性均为必填，且不得为空或仅含空白：

- `storeFile`：keystore 路径，由 `rootProject.file` 解析；相对路径以仓库根目录为基准，
  绝对路径保留其主机特定含义。
- `storePassword`：keystore 密码。
- `keyAlias`：签名密钥别名。
- `keyPassword`：签名密钥密码。

凭据与 keystore 不得纳入版本控制，文档中不得填写真实秘密。
属性文件缺失、必填值缺失或为空白、keystore 路径未指向现有文件时，
Gradle 会在配置阶段失败，包括 IDE Sync。不会回退到默认 debug 签名密钥。

## 开发入口

在仓库根目录使用已检入的 Wrapper 运行 Gradle：

- macOS、Linux 或其他 POSIX shell：`./gradlew <task>`
- Windows Command Prompt 或 PowerShell：`gradlew.bat <task>`

当前配置常见的任务意图包括组装可安装构建、运行 JVM 测试、运行 lint，以及运行已连接 Android 测试。权威任务名称和执行规则由[验证指南](validation.zh-CN.md)维护。

除非项目作者明确要求，或已授权正式版本或聚焦验证任务需要，否则 Agent 不为日常编写或审查运行 Gradle。该执行规则不妨碍作者直接运行相同 Wrapper 任务。

## 配置变更

- Android 工程保持在仓库根目录，并维持当前单模块结构，除非已授权技术变更另有需要。
- SDK 级别、应用标识、版本标识、签名、仓库、Wrapper、构建插件或依赖版本的变化均属于明确的构建配置变更。
- 当配置变化影响前置条件、命令、验证覆盖或已观察限制时，同步更新本文与[验证指南](validation.zh-CN.md)。
- 工具或依赖存在更新版本，不表示必须升级，也不表示构建失败。

## 故障排查边界

构建或运行失败时，记录准确命令、主机操作系统与 shell、相关 JDK 与 Android SDK 标识、首个可执行错误，以及依赖解析使用镜像还是官方上游。临时日志保留在权威文档之外。只有原因与解决方式得到确认且可能再次发生时，才在本文增加长期故障排查规则。
