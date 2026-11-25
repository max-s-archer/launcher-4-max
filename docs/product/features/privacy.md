# Privacy and Data Handling

> Public semantic source: English. Chinese counterpart: [privacy.zh-CN.md](privacy.zh-CN.md). Settings presentation is defined in [settings.md](../surfaces/settings.md); the optional accessibility capability is defined in [double-tap-lock.md](double-tap-lock.md).

## Purpose and authority

This document defines Launcher4Max's current user-visible Privacy statement and the separate prominent disclosure required before an enable-oriented handoff for screen locking. It describes the current product contract rather than making a legal, store-approval, or implementation-completeness claim.

The Privacy statement and prominent disclosure are separate presentations. Showing or reading Privacy does not count as consent to accessibility access. The prominent disclosure appears in the normal screen-locking enablement flow immediately before Launcher4Max offers to open Android accessibility settings.

Both presentations are bundled with the application, remain available offline, and use the English or Simplified Chinese resource selected from the system locale, with English fallback. The application must not fetch either text from GitHub or another network source.

## User-visible Privacy statement

### Privacy

Launcher4Max is designed to keep its current product data and processing on your device.

### Information used on your device

Launcher4Max processes the application entries that Android exposes to it under the current Launcher role and permission boundary. This can include application names, icons, launchable components, Android profile identities, platform-provided badges, and application shortcuts. Launcher4Max uses this information to show applications, distinguish launchable identities, launch the entry you select, and present applicable shortcuts.

When you add a Home favorite, Launcher4Max locally stores its launchable identity, module destination and order. It also stores module type, each module's name and whether that name is shown, and applicable vertical-module icon size, name (text) size, name placement, and items-per-row settings. A temporary latest-removal Undo snapshot may retain either the most recently removed favorite or the most recently removed whole module only until its Snackbar is replaced, dismissed, or invalidated by the documented lifecycle; it is not undo history. Removing the final favorite from a module may therefore retain the module data needed to restore that favorite and its original placement. Removing a whole module retains the module data needed to restore that module once. Launcher4Max exposes no separate durable module-removal history. Inventory and shortcut information outside saved favorites is processed as needed and is not retained as Launcher4Max-owned application-usage history.

Launcher4Max stores your selected Drawer application icon size, application name size, name placement, items-per-row count, section-anchor presentation, and background-opacity percentage locally on the device. These display settings are used only to restore the presentation you selected and are not application-usage history.

Launcher4Max also reads current system information needed for its interface and controls, including time, date, system language, default-Launcher state, and whether the optional Launcher4Max accessibility service is enabled and connected. Time and date are not retained as history, and these system states are not used for behavioral analytics.

### Storage, backup, and deletion

Favorites, including which favorites are placed on the Home bottom app bar and that bar's on, fixed, and icon-size settings, Drawer display settings, quick-action bindings, and the icon-shape preference remain on the device until you change them or Android clears Launcher4Max's application data. The icon-shape preference is its clip mode and saved custom corner-radius proportion. Launcher4Max provides a manual local backup and restore for these data. Back up writes one JSON file containing your current favorites, Home bottom app bar placement and settings, Drawer display settings, quick-action bindings, and icon-shape preference to a location you choose through the system document picker; the file stays under your control and is never uploaded. Restore reads a backup file you select and, after your explicit confirmation, replaces the current favorites, bar placement and settings, display settings, bindings, and icon-shape preference in one atomic operation. Launcher4Max does not provide an in-app clear-all action, automatic backup, cloud synchronization, cloud backup, or device-to-device transfer of its application data. Android cloud backup and device-transfer backup must remain disabled for Launcher4Max-owned favorite and display-setting data under the current product contract.

Launcher4Max also stores one non-content preference: the local date on which you dismissed the Home default-Launcher prompt, so that the prompt stays closed for the remainder of that day. It is user-controlled presentation state rather than application-usage history, and it is not part of the backup file.

The one-time adoption of the current Home module model starts Home favorites empty rather than migrating the former favorite structure. This author-accepted product transition does not clear independent Drawer display settings or other unrelated local configuration.

If stored favorite data cannot be read reliably, Launcher4Max preserves the original unreadable data and disables favorite changes rather than silently replacing it with an empty list. You can retry the read. Clearing Launcher4Max's application data through Android system settings or uninstalling Launcher4Max removes its locally stored application data; the current product does not provide recovery afterward.

### Data collection, sharing, and network use

Launcher4Max does not provide an account, advertising, analytics, crash-report upload, cloud service, or Launcher4Max-operated server. It does not transmit, sell, or share the locally processed information described above. Core Home, Drawer, application launching, and Settings behavior remains available without a network connection.

Current weather is the network exception. A weather request starts only while the feature is on and the user has supplied an API Key and selected a city. Launcher4Max sends that request to the selected weather provider, initially Amap, to obtain the current weather icon and temperature. It does not send location and does not use a public proxy or Launcher4Max-operated backend. The feature switch, user-supplied Key, selected provider, selected city, and returned weather data are privacy-relevant data. Weather data is cached by city on the device and is not included in the favorite backup. Provider terms, quota, and authorization for public distribution are `To be decided`.

Selecting a GitHub link asks Android to open the fixed URL with a system browser or another compatible application. Launcher4Max sends only that URL to the selected handler. The external application and any network service it uses operate under their own privacy practices.

### Screen locking and accessibility access

Screen locking is optional and remains inactive until you bind a Home quick-action slot to it and enable Launcher4Max's purpose-limited accessibility service in Android system settings. Launcher4Max uses that service only to request one system lock action after you perform your bound gesture in eligible blank space on Home.

The service does not retrieve screen or window content, inspect other applications' interfaces, collect accessibility events for analytics, observe application usage, automate background actions, or send or share data. It performs no global action other than the explicit lock request. Launcher4Max does not use Device Administrator as a fallback. You can disable the service at any time in Android accessibility settings without losing independent Launcher functionality.

### Permissions and profile boundary

Launcher4Max uses only Android capabilities required by its current product contract. It does not request `ACCESS_HIDDEN_PROFILES`, access or display Android Private Space entries that require that permission, or provide Private Space management. Ordinary, work-profile, and cloned launchable entries may appear when Android exposes them within Launcher4Max's current role and least-privilege boundary.

### Contact

For privacy questions, open the Launcher4Max GitHub Issues page:

`https://github.com/max-archer-m/launcher-4-max/issues`

Selecting this address uses an implicit system browser action. If no compatible handler can open it, Launcher4Max shows the localized short message `Unable to open privacy contact` and keeps the Privacy presentation open.

## Screen-locking prominent disclosure

The following disclosure is displayed separately immediately before an enable-oriented handoff to Android accessibility settings. It is not merged into the Privacy Bottom Sheet or another unrelated disclosure.

### Title

`Accessibility access for screen lock`

### Body

`Launcher4Max uses Android AccessibilityService only to lock the screen when you perform your bound quick-action gesture in eligible blank space on Home. The service does not access or collect personal or sensitive data. It does not read screen or window content, observe activity in other applications, collect accessibility events, or send or share data. Screen locking is optional, and you can disable the service at any time in Android settings without affecting other Launcher features.`

### Actions

- `Cancel` closes the disclosure and stays in Launcher4Max without opening system settings.
- `Agree and continue` records no retained acknowledgement and only confirms the current handoff to Android accessibility settings. Android remains authoritative for whether the service is enabled.
- Dismissing the disclosure through Back, a scrim action, or another cancellation path is not consent and does not open system settings.

## Change boundary

The Privacy statement and prominent disclosure must be reviewed and updated before Launcher4Max adds or changes any network-backed feature, account, advertising, analytics, crash reporting, monitoring SDK, cloud or device-transfer behavior, data category, external recipient, permission, accessibility purpose, Private Space support, distribution channel, or other behavior that would make the current text incomplete.

Future public-store distribution requires a renewed privacy, security, platform-policy, listing, consent, and specialist review. The current text does not claim Google Play or another distributor has approved the application or its accessibility-service use.
