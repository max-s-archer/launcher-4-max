# Validation Guide

> Semantic source: English. Chinese counterpart: [validation.zh-CN.md](validation.zh-CN.md).

## Purpose

This document defines the minimum current validation baseline for Launcher4Max: which checks exist, who may initiate them, how device observations are recorded, and how results are reported. It separates an available command from evidence that the command has actually passed.

## Execution authority

- The project author normally performs build, installation, and device-use checks and may report the observed result as iteration or version evidence.
- An agent runs Gradle only when the author explicitly requests it or an authorized formal-version or focused-validation task requires it.
- Authorization to implement an iteration includes proportionate local validation, but it does not override the preceding Gradle restriction.
- A command documented below is an authoritative entry point. Its result remains `Not run` or `Unknown` until current evidence records an actual execution.

## Available automated checks

Run commands from the repository root using `./gradlew` on a POSIX shell or `gradlew.bat` on Windows.

| Check | Gradle task | Current scope |
| --- | --- | --- |
| Assemble debug APK | `assembleDebug` | Compiles and packages the debug application |
| JVM unit tests | `testDebugUnitTest` | Runs tests in `app/src/test` when present |
| Android lint | `lintDebug` | Runs configured Android lint for the debug variant |
| Connected Android tests | `connectedDebugAndroidTest` | Runs tests in `app/src/androidTest` on connected compatible targets |

The build currently contains Android instrumented tests and no repository JVM unit-test source directory. A successful task with no applicable tests must be reported accurately and must not be described as behavioral coverage that does not exist. No separate static-analysis tool beyond configured Android lint is currently established.

The task names above are derived from the checked-in Android application configuration. They have not been executed as part of establishing this document. If Gradle reports that a documented task is unavailable or unsuitable, preserve that result, correct the guide from repository evidence, and do not silently substitute a broader task.

## Focused manual validation

For a product behavior change, use the applicable product or iteration acceptance criteria to select the smallest meaningful manual journey. Record:

- APK or build identity and source commit;
- device model, Android version, and API level;
- exact starting state and performed steps;
- observed result, including failures or uncertainty;
- relevant application state before and after the check; and
- whether the observation was made by the author or an agent.

An emulator result does not establish physical-device behavior. One device result does not establish compatibility on another OEM, Android version, profile type, or clone environment.

### Switching between debug and release installations

Use the [local signing baseline](development.md#local-signing) for configuration prerequisites.
The following is a validation method, not a record of a passed build or installation:

1. Obtain both APKs through an authorized build. Inspect APK metadata and signing certificates
   with Android SDK tools, such as APK Analyzer and `apksigner verify --print-certs`.
   Confirm that both APKs and the installed app have the same `applicationId` and signing
   certificate. Record certificate fingerprints, version codes, and debuggable attributes.
2. Check version codes before each replacement: the incoming APK must have a `versionCode`
   equal to or greater than the installed app for a normal replacement. Matching identity
   and certificate do not bypass Android downgrade restrictions; do not force a downgrade
   or change the project version just to complete this check.
3. If the installed app uses the previous default debug certificate, direct replacement with
   these APKs is not possible. Back up needed data/settings through an available, verified
   method before an author-controlled uninstall and reinstall. Do not uninstall automatically;
   if backup is unavailable, stop and ask the author how to preserve the data.
4. With a compatible debug installation, record representative saved settings, then replace
   it with release without uninstalling or clearing app data. Reopen the app, verify that
   settings persist, and inspect the installed package to confirm it is not debuggable.
5. Repeat in the reverse direction, release -> debug, after checking the same installation
   prerequisites. Verify that saved settings persist and the installed package is debuggable.
   If version codes prevent either direction, report that direction as not run with the reason.

Record each direction separately using the evidence fields above and result statuses below.

## Validation proportionality

- Documentation-only changes: inspect the relevant Git diff, run `git diff --check`, and verify affected local Markdown links. Do not run Gradle unless separately required.
- Focused implementation changes: use the narrowest relevant build, automated check, and manual journey allowed by the current authorization and environment.
- Iteration completion: satisfy its acceptance evidence and record every skipped, unavailable, failed, or unknown check as required by the iteration format.
- Version completion: apply only the gates selected by the version's delivery level and contract. Recommended higher-level evidence does not become mandatory by implication.
- Device and compatibility gates: the applicable version contract identifies each mandatory environment. Recommended API, device, OEM, or profile evidence that is not performed must be recorded as `Unknown`, `Not run`, or `Unavailable` and does not become a completion blocker merely because it was listed. If a recommended check is performed and exposes a failure on an included path, record and resolve it or return it for explicit author disposition; do not ignore the result because the check was recommended.
- Formal release artifacts: apply the additional matrix, automation, review, signing, traceability, and retention gates explicitly defined by [release governance](release.md) and the applicable version contract.

## Result reporting

Report each relevant check as one of:

- `Passed`: the stated command or scenario ran and its acceptance condition succeeded.
- `Failed`: it ran and produced a failing result.
- `Not run`: it was intentionally not executed.
- `Unavailable`: it could not run because a required tool, environment, device, credential, or dependency was unavailable.
- `Unknown`: evidence is insufficient to determine a result.

Record the command or scenario, executor, environment, date when useful, and concise evidence. Do not infer a pass from code inspection, a previous machine, an author report without attribution, or the absence of an observed failure. A newer-version advisory is maintenance information, not a failure.

## Maintaining this baseline

Update this guide when Gradle task names, configured quality tools, device requirements, execution authority, evidence fields, or version-level validation responsibilities change. Keep feature-specific acceptance criteria in product and iteration documents; keep actual historical results in the applicable iteration or version record rather than accumulating them here.
