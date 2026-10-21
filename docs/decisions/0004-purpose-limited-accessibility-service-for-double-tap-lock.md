# ADR-0004: Purpose-Limited AccessibilityService Boundary for Bound Screen Lock

## Status

- Value: `Active`
- Updated: 2026-09-20
- Basis: The author authorized production implementation of the binding-driven screen-lock trigger and the mainline service move as Iteration 32. The purpose-limited service boundary accepted in Iteration 10 remains in force.

## Date

2026-08-16

## Context

Screen locking is an optional Home capability. A Home quick-action slot bound to Screen lock requests one `GLOBAL_ACTION_LOCK_SCREEN` action through Android's `AccessibilityService` API after the user explicitly enables the service in system settings and reviews the local Privacy and prominent disclosure.

An accessibility service is a privileged platform capability with significant privacy, security, and platform-policy implications. Stores and platform reviews impose strict constraints on when, how, and why such a service may be used. The current product contract explicitly limits the service to a single purpose and boundary.

Iteration 10 first implemented this boundary as a debug-only double-tap lock. Iteration 32 generalizes the trigger to the bound Home quick-action slots and moves the same service, manifest declaration, and configuration into the mainline so the capability exists in every build type.

## Decision

Launcher4Max's accessibility service exists only to support the bound Screen lock quick action. Its boundary is:

- **Sole purpose**: Perform one `GLOBAL_ACTION_LOCK_SCREEN` action after an explicit user gesture on the Home basic-information blank-space slot currently bound to Screen lock.
- **Manifest declaration**: The service uses the minimum `accessibilityService` XML configuration required to request global actions. It does not declare `flagRequestFilterKeyEvents`, `flagIncludeNotImportantViews`, `flagReportViewIds`, `flagRetrieveInteractiveWindows`, or any capability that provides window-content retrieval.
- **Permissions**: The service component is protected by `android.permission.BIND_ACCESSIBILITY_SERVICE` so only the system can bind. The application does not declare a `<uses-permission>` for that permission, `SYSTEM_ALERT_WINDOW`, or any device-administration permission.
- **Data access**: The service does not read or retain any window content, screen content, accessibility events, or data from other applications. `AccessibilityLockProbeService` ignores `onAccessibilityEvent`, `onInterrupt`, and all other event callbacks except `onServiceConnected`, `onUnbind`, and `onDestroy`.
- **Global actions**: The service requests only `GLOBAL_ACTION_LOCK_SCREEN` when the bound Screen lock gesture is recognized. It performs no other global action, background automation, or continuous monitoring.
- **Fail-closed behavior**: If the service is disabled, revoked, disconnected, or the lock action is unavailable or rejected, the capability is silently unavailable except for the contracted failure toast when a connected request is rejected. All independent Launcher paths (Home, Drawer, application launching, Settings) remain unaffected.
- **User control**: The service is disabled until the user explicitly enables it in Android accessibility settings. The user can disable it at any time without losing independent Launcher functionality. No in-app toggle exists; control is through the platform's settings path.
- **Disclosure**: Before the user enables the service, the Quick action settings page presents the current local Privacy description and a separate prominent disclosure that explains the service's sole purpose, data-access boundary, and disable path. The disclosure uses `Cancel` and `Agree and continue` choices and is not retained as acknowledged history.
- **Home gesture boundary**: Bound-gesture recognition is enabled only in eligible basic-information blank space when `editMode` is `false`. Time, date, favorites, edit surfaces, and all other interactive targets are excluded. A slot bound to No action performs no action.
- **Connection model**: `AccessibilityLockConnection` is a process-local application-to-service seam in every build type. It owns no `Context`, event, or window data and does not persist state outside the application process. `AccessibilityLockProbeService` calls `AccessibilityLockConnection.connected(this)` on `onServiceConnected` and `disconnected(this)` on `onUnbind`/`onDestroy`.
- **Production integration**: The service class, manifest declaration, and `accessibilityService` XML live in the mainline source set and are packaged in every build type. The application graph wires `AndroidAccessibilityLockController` in every build type under the same fail-closed request behavior.

## Rationale

This boundary ensures the accessibility service is a narrow, user-controlled utility that performs exactly one action on demand. It keeps the service's purpose, permissions, and data-access behavior transparent and reviewable. The fail-closed design guarantees that any service-state change cannot break core Launcher functionality.

Using the platform's accessibility settings and disclosure system keeps user authorization explicit and platform-native. The current product Privacy statement and prominent disclosure text provide the contract for what the service does and does not do.

The connection model keeps the service stateless and does not introduce a persistent background bridge or shared data store that would require broader architectural review.

Binding the trigger to a Home quick-action slot, rather than a hard-coded double tap, keeps the same service boundary while matching the accepted quick-actions contract.

## Considered Options

### Use Device Administrator API

- Benefits: `DevicePolicyManager.lockNow()` is the established Android mechanism for programmatic screen locking.
- Trade-offs: Requires `DEVICE_ADMIN` permission, device-administration enrollment, and revocation through a specialized system path. Platform policy restricts its use, and stores may scrutinize or reject non-enterprise uses. The current product does not need its broader administrative capabilities.

### Add a Home-surface overlay and manage touches manually

- Benefits: Would avoid the accessibility-service permission and platform-policy boundary entirely.
- Trade-offs: Requires a system overlay permission, complex gesture handling, and would not be available while other overlays are active. It also risks interfering with Android's native gesture system and does not provide a cleaner authorization story.

### Use a purpose-limited AccessibilityService (selected)

- Benefits: Uses the platform's explicit authorization and settings path, requires one minimal permission, and performs only the needed action without persistent monitoring. The current product boundary and Privacy text are written around this approach.
- Trade-offs: Platform policy and stores may review the usage, and the service must be explicitly enabled by the user. The boundary must be preserved in code, disclosure, and validation to remain within acceptable platform constraints.

## Consequences

- No implementation may expand the accessibility service to read window content, observe other applications, collect accessibility events, perform background automation, or add any other global action without a new active ADR.
- Store distribution requires a fresh platform-policy and disclosure review even if a GitHub-distributed build is accepted.
- The service must remain optional and must not be required for any independent Launcher path.
- The `android:accessibilityService` XML must not be expanded to request capabilities beyond what the current boundary requires.
- Future platform compatibility testing must verify that the service continues to request only `GLOBAL_ACTION_LOCK_SCREEN` and that fail-closed behavior is preserved across API 31–37.
- Release builds now declare the service. A release installation can appear in the system accessibility list once the user enters the authorization flow; no upgrade-time behavior change occurs before that point.

## Validation Evidence and Gaps

- Physical-device validation on the author's primary device in Iteration 10 confirmed that a valid double tap on eligible Home blank space requested one lock action when the service was enabled, and that revocation or disabling the service left independent Launcher paths unaffected. That evidence still supports the purpose-limited service boundary.
- The current implementation confirms that `onAccessibilityEvent` and `onInterrupt` are no-ops, that only `GLOBAL_ACTION_LOCK_SCREEN` is requested, and that the connection model owns no persistent data.
- Settings UI, Privacy presentation, and prominent disclosure display the current bound-gesture copy.
- The current mainline manifest and `accessibilityService` XML confirm no window-content, key-event, or extended-view capabilities are declared.
- Iteration 32 device journeys for the bound-gesture trigger were accepted by the author on the primary physical device on 2026-09-20. Dedicated release-APK verification of the bound Screen lock gesture remains `Unknown` until coordinated with Iteration 34's artifact work. The author-run `./gradlew lint` passed; no lint-baseline refresh was required. Agent-run Gradle remains `Not run`.
- API 31 and one additional API 36 or API 37 physical-device coverage remain recommended compatibility evidence. Unperformed OEM and Private Space scenarios do not by themselves invalidate this decision but must be recorded as `Unknown`, `Not run`, or `Unavailable` in the version record.

## Implementation Notes

- `AccessibilityLockProbeService`, its manifest declaration, and `accessibility_lock_probe_config.xml` live in the mainline source set with the same XML constraints validated in Iteration 10.
- `AccessibilityLockConnection` is a process-local singleton that provides a stateless `LockRequestPort` adapter. It does not persist across process restarts and must not be converted into a background service or persistent bridge.
- Bound-gesture recognition uses `detectTapGestures` with `pointerInput` scoped to the eligible Home blank space and only when `!editMode`. The gesture must not be moved to a broader Home-surface modifier. Slot resolution, not a hard-coded double tap, decides whether a recognized gesture requests the lock.

## Implementation and Validation Evidence

- Iteration 10 record: `docs/delivery/1.1.0/iteration-10-double-tap-lock.md`
- Iteration 32 contract: `docs/delivery/1.7.0/iteration-32-quick-actions-screen-locking.md`
- Physical-device validation of the purpose-limited boundary: author-reported successful lock behavior, Settings state refresh, disclosure flow, and revocation/fail-closed behavior on the primary device in Iteration 10
- Implementation files:
  `app/src/main/java/com/maxarcher/launcher/AccessibilityLockProbeService.kt`,
  `app/src/main/java/com/maxarcher/launcher/AccessibilityLock.kt`,
  `app/src/main/AndroidManifest.xml`,
  `app/src/main/res/xml/accessibility_lock_probe_config.xml`
- Settings integration: `SettingsScreen.kt` Quick action settings page, Privacy, and disclosure resources
- Home gesture integration: `HomeBasicInformation.kt` bound-slot detection in eligible blank space

## Replaces

None

## Inactivation

- Date: Not applicable while `Active`
- Reason: Not applicable while `Active`
- Replaced by: None
- Consequences: None
