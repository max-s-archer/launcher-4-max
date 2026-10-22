# Development Guide

> Semantic source: English. Chinese counterpart: [development.zh-CN.md](development.zh-CN.md).

## Purpose

This document records the minimum current development baseline for Launcher4Max. It describes project configuration and available entry points; it does not claim that a command has run successfully unless corresponding validation evidence records that result.

## Current project configuration

- Repository root and Android project root: the repository root.
- Project structure: one Gradle application module, `:app`.
- Build scripts: Kotlin DSL.
- Gradle wrapper distribution: Gradle `9.4.1`, currently configured through the Aliyun distribution mirror.
- Android Gradle Plugin: `9.2.1`.
- Kotlin: `2.3.10`.
- Java language level: JDK 17.
- Android configuration: `minSdk 31`, `targetSdk 36`, and `compileSdk 37`.
- Application identity and namespace: `com.maxarcher.launcher`.
- Main and instrumented-test Kotlin packages use `com.maxarcher.launcher`.
- The default test namespace and test APK identity are `com.maxarcher.launcher.test`.
- See [release migration guidance](release.md#application-identity-migration) before replacing
  an installation with the previous application identity.
- Current configured version identity: `versionName 1.7.2`, configured `versionCode 10`; `1.7.0` is closed in [its delivery record](delivery/1.7.0/delivery.md).
- Dependency repositories currently use Aliyun mirrors. `settings.gradle.kts` retains commented official-upstream alternatives for deliberate manual switching.

Treat these values as current repository configuration, not promises that every host already has the matching JDK, Android SDK, emulator, device connection, credentials, or cached dependencies.

## Prerequisites

- A JDK 17 installation.
- An Android SDK that supplies the platform and build tools required by the checked-in configuration.
- Network or local cache access sufficient to resolve the configured Gradle distribution, plugins, and dependencies.
- For installation or instrumented validation, an Android device or emulator visible to the Android tooling.

The exact Android SDK package list, supported IDE version, and host-specific environment-variable setup are not yet established as project-wide requirements. Record a verified requirement here when it becomes necessary; do not infer it from one machine.

## Local signing

Both `debug` and `release` build types use the local `release` signing config and the same
`applicationId` listed above. `debug` is debuggable; `release` is not debuggable.

Before Gradle configuration, provide an untracked `keystore.properties` at the repository root
and the actual keystore file. All four properties are required and must be nonblank:

- `storeFile`: the keystore path, resolved by `rootProject.file`; relative paths are relative
  to the repository root, and absolute paths retain their host-specific meaning.
- `storePassword`: the keystore password.
- `keyAlias`: the signing key alias.
- `keyPassword`: the signing key password.

Keep credentials and the keystore out of version control; do not put real secrets in documentation.
If the properties file is missing, a required value is missing or blank, or the keystore path
does not identify an existing file, Gradle fails during configuration, including IDE Sync.
There is no fallback to the default debug signing key.

## Development entry points

Run Gradle from the repository root with the checked-in wrapper:

- macOS, Linux, or another POSIX shell: `./gradlew <task>`
- Windows Command Prompt or PowerShell: `gradlew.bat <task>`

Common configured task intentions include assembling an installable build, running JVM tests, running lint, and running connected Android tests. The authoritative task names and execution rules are maintained in [validation.md](validation.md).

An agent does not run Gradle for routine authoring or review unless the project author explicitly requests it or an authorized formal-version or focused-validation task requires it. This execution rule does not prevent the author from running the same wrapper tasks directly.

## Configuration changes

- Keep the Android project at the repository root and preserve the current single-module structure unless an authorized technical change requires otherwise.
- Treat changes to SDK levels, application identity, version identity, signing, repositories, wrapper, build plugins, or dependency versions as explicit build-configuration changes.
- Update this guide and [validation.md](validation.md) when a configuration change alters prerequisites, commands, validation coverage, or observed limitations.
- Do not treat an available newer tool or dependency version as a required upgrade or a failed build.

## Troubleshooting boundary

When a build or run fails, record the exact command, host operating system and shell, relevant JDK and Android SDK identity, first actionable error, and whether dependency resolution used mirrors or official upstream repositories. Keep transient logs outside authoritative documentation. Add a durable troubleshooting rule here only after the cause and remedy have been confirmed and are likely to recur.
