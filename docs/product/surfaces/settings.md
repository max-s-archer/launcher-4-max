# Settings Interaction Specification

> Public semantic source: English. Chinese counterpart: [settings.zh-CN.md](settings.zh-CN.md). Exact visual values are defined in the [Settings presentation specification](../presentation/settings.md); the spatial sketch is the shared [Settings wireframe](../wireframes/settings.txt), with reading rules in the [wireframe index](../low-fidelity-wireframes.md).

## Entry and return

- The page title is `Settings`.
- A fixed top app bar displays the page title and one visible Back control. Selecting that control has the same result as system Back; neither path opens another destination or resets Drawer position.
- Settings opens only from the Settings item in ordinary Drawer: in the final list section
  during Content or below Retry during Error. The AlphabetIndex gear only navigates to the
  Content section; it does not open Settings.
- Back returns to Drawer during the same process, preserving its prior Content list position
  or its Error state according to the [Drawer error contract](drawer.md#empty-or-failed-result).
  A Settings round trip does not retry or clear that Error.
- Settings entries, including backup, restore, quick-action settings, and Privacy, remain
  available from Error under their existing rules without waiting for inventory recovery.
  Backup and restore operate on saved favorites and settings; neither repairs inventory failure.
- Settings uses an opaque standard Material 3 dark color scheme. Unlike Home and Drawer, it paints its Material surface background rather than exposing the system background beneath the application. Two preview windows are transparent exceptions: the icon-shape window defined by [icon-shape.md](../features/icon-shape.md), and the Home bottom app bar window defined by [home-bottom-app-bar.md](../features/home-bottom-app-bar.md). Both reveal the system wallpaper without reading it. Every other region of Settings remains opaque.

## Current contract

### Launcher settings

- **Default home application:** Displays current default-Launcher state and opens the system default-home application settings.
- The title is `Default home application`. The supporting text is `Launcher4Max is the default launcher` or `Launcher4Max is not the default launcher` according to current system state.
- Returning from the system destination refreshes the supporting text immediately.
- **Quick action settings:** Opens the quick-action settings page defined by [quick-actions.md](../features/quick-actions.md). That page lists the two basic-information slot rows with their current bindings and the always-visible screen-lock service state row that carries the accessibility authorization entry and its disclosure flow. Selecting `Screen lock` while the service is off enters that flow without affecting the saved binding.
- Each slot binds independently to `No action`, `Edit mode`, or `Screen lock`. Both slots default to `No action`, and Launcher4Max applies no uniqueness, exclusivity, or cross-slot validation. A binding change applies immediately and persists locally.
- Settings displays no separate screen-lock item. The page and its selection dialog own their behavior in [quick-actions.md](../features/quick-actions.md).
- **Icon shape:** Opens the icon-shape settings page defined by [icon-shape.md](../features/icon-shape.md). The entry is a primary settings item titled `Icon shape`, placed directly below `Quick action settings` and above the Data entries. The page, its live-preview window, and its mode and corner-radius controls own their behavior in that contract.
- **Home bottom app bar:** Opens the page defined by [home-bottom-app-bar.md](../features/home-bottom-app-bar.md). The entry is a primary settings item titled `Home bottom app bar`, placed directly below `Icon shape` and above the Data entries. That page owns the master switch, preview, fixed presentation, and icon size.

### Language behavior

- The current product provides English and Simplified Chinese resources for every user-visible string.
- Launcher4Max automatically resolves its resources from the current system locale. Simplified Chinese uses the Simplified Chinese resources; English uses the English resources; unsupported locales fall back to English.
- Settings does not contain an application-language item or manual language selector.
- A system-language change updates Launcher4Max to the corresponding supported resource set without requiring an Launcher4Max-specific selection.
- Manual application-language selection is an additive future capability and is outside the current contract.

### Data

This heading organizes the behavior contract and is not a visible Settings group heading. The two entries below appear as stacked primary settings items in the order listed, Back up favorites and settings above Restore from backup. Each opens a system surface and therefore shows the trailing arrow of the primary settings-item presentation. Neither entry carries supporting text.

- **Back up favorites and settings:** Selecting it opens the system document picker to choose a local save location. Launcher4Max supplies the suggested display name `launcher4max-backup-<version-name>-<yyyyMMddHHmm>`, for example `launcher4max-backup-1.6.0-202609071913.json`, built from the current application version name and the device's local date and time; the system picker owns the final name and location. The written file is one JSON file containing the complete current Home favorite state (ordered favorite modules with their type, order, stable identities, style, and per-module application order, plus the Home bottom app bar state: whether it is on, whether it is fixed, its selected icon size, and the ordered favorite identities placed on it) and the complete Drawer display settings (application icon size, application name size, name placement, items per row, section-anchor presentation, and background-opacity percentage) and the complete quick-action bindings (the action bound to each basic-information quick-action slot) and the complete icon-shape preference (clip mode and saved custom corner-radius proportion). The file carries a schema version and is not encrypted. No new permission is required. The exported file is user-managed and lives outside Launcher4Max's app-private storage.
  - Canceling or dismissing the document picker writes nothing, changes nothing, and shows no message.
  - While the write is in progress, both Data entries are non-interactive. An interrupted backup leaves the current application state unchanged; a partially written file is simply an invalid backup under the restore rules.
  - A successful backup shows the localized short message `Favorites and settings backed up`. A failed write shows the localized short message `Unable to back up favorites and settings`.
- **Restore from backup:** Selecting it opens the system document picker to choose a backup file.
  Canceling or dismissing the picker changes nothing and shows no message. After a file is selected,
  a confirmation dialog states that confirming replaces the current Home favorites (including
  the Home bottom app bar's members and settings), Drawer display settings, quick-action bindings,
  and icon-shape preference with the backup contents, and that the action cannot be undone.
  It offers `Restore` and `Cancel`; `Cancel`, dismissing the dialog, and Back change nothing.
  Confirming replaces the complete current state with the backup state as one atomic operation
  and shows the localized short message `Favorites and settings restored`. Settings remains
  displayed; Home and Drawer present the restored state on their next view.
  - Restore does not validate the backup against the current application inventory; afterwards the ordinary inventory-refresh rules apply, so disabled identities are retained, reliably disappeared identities are removed, and failed launches show the ordinary launch-failure feedback.
  - Schema compatibility is directional: a backup whose schema version is higher than the current application schema is version-incompatible. A backup whose schema version is equal to or lower than the current schema is accepted and interpreted under the current schema.
  - A backup missing a state section is valid: a missing favorites section restores an empty favorite state, a missing Drawer display-settings section restores display settings to Launcher4Max's default values, a missing quick-action-bindings section restores every slot to `No action`, a missing icon-shape section restores the complete icon-shape default (`OEM clip` with saved proportion `50`), and a missing Home bottom app bar section restores that bar to off with no favorites placed on it. A missing field inside a present section receives its field-specific default under the owning feature contract. For icon shape, a missing mode uses `OEM clip`, a missing proportion in `Freeform clip` uses `50`, and invalid mode or proportion values fall back to their field defaults; if the two fields cannot form one valid shape preference, the complete icon-shape preference falls back to its default. Invalid proportions are not clamped, and other valid backup sections remain restorable. When a valid Home bottom app bar section contains more identities than the current capacity for its restored icon tier and Home width, restore keeps the first `N` valid identities in the backup's saved bar order and omits the rest from the restored bar without moving them to another module. An empty backup with no modules is valid.
  - An unreadable, malformed, or version-incompatible file fails without overwriting the current state and shows the localized short message `Unable to restore from backup`.
  - Restore does not trigger the one-time Home-model adoption reset, and Launcher4Max performs no automatic backup or upload of the file.

### About

This heading organizes the behavior contract and is not a visible Settings group heading.

- **Privacy:** Opens the local Privacy Bottom Sheet defined by [privacy.md](../features/privacy.md).
- **Launcher4Max License:** Opens the local Launcher4Max License Bottom Sheet; the English label uses `License`.
- **Third-party notices:** Opens a separate local Bottom Sheet. The sheet lists the components shipped in the release application and states that each is covered by the Apache License 2.0. Its body is the offline text in `res/raw/third_party_notices.txt`. Development-only and test-only components are not included. The entry stays separate from Launcher4Max License.
- **Project repository:** Opens the configured repository URL through an implicit system browser action.
- **Version information:** Displays `v<version-name>(<version-code>)`, for example `v1.6.0(7)`. It is not interactive and cannot be copied.

### Support and diagnostics

Manual local backup and restore are supported as defined above. Complex logs, update checks,
automatic backup, cloud synchronization, diagnostic export, and copying version or device
information are outside the current product contract.

## Privacy presentation

- Selecting Privacy opens a dark modal Bottom Sheet containing a local, readable privacy statement. A dedicated Privacy page is not part of the current scope.
- Its exterior shell, fixed title, and scrolling body follow the [Settings presentation specification](../presentation/settings.md#informational-bottom-sheets).
- The statement must remain available offline.
- The sheet uses the same scrim, top drag handle, drag-to-dismiss, scrim-tap dismissal, and Back dismissal behavior as the application action sheet.
- Privacy content scrolls vertically when it exceeds the available sheet height. Closing returns to the same Settings position.
- The displayed text is the current user-visible Privacy statement in [privacy.md](../features/privacy.md). It includes the current local data, backup, deletion, external-link, permission, and screen-lock boundaries.
- The GitHub Issues contact address is selectable and uses the implicit browser and localized failure behavior defined by that Privacy contract.

## License presentation

- Launcher4Max License and Third-party notices are separate Settings entries.
- Each opens a dark, local, offline-readable presentation. Long content scrolls vertically.
- Their exterior shell, fixed title, and scrolling body follow the [Settings presentation specification](../presentation/settings.md#informational-bottom-sheets).
- Their modal dismissal and Settings-position restoration match the Privacy Bottom Sheet.

## Settings item roles

### Primary settings items

- A primary item contains a title, optional supporting text, and a trailing Android or Material arrow when it opens another destination. The quick-action settings page rows keep that arrow for visual consistency with the Settings page even though they open a local selection popup.
- Primary item typography, color roles, row geometry, padding, and arrow size belong to the [Settings presentation specification](../presentation/settings.md).

### Secondary information items

- Privacy, Launcher4Max License, Third-party notices, Project repository, and Version information use the secondary presentation.
- They do not show a trailing arrow. Clickable entries remain clickable despite the intentionally secondary presentation; Version information is not clickable.
- Secondary item typography, color roles, row geometry, and alignment belong to the Settings presentation specification.

## Project repository

- Launcher4Max does not preflight network connectivity. The system browser owns offline or network-error presentation after it opens.
- If no system handler can open the repository URL or the implicit action fails, show the short localized Toast `Unable to open project link` and retain the current Settings position.
- The repository URL has no copy action in the current scope.

## State refresh

- Returning from a system settings destination refreshes affected Launcher state.
- Settings does not restore a previously open modal sheet after leaving for a system surface.
