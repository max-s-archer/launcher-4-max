# Changelog

User-visible changes to Launcher4Max, newest first. The [completed delivery records](docs/delivery/) remain authoritative for scope, evidence, and known limitations.

Launcher4Max was named Avenor Launcher before `1.7.0`. Versions `1.0.0` through `1.6.0` were developed as the author's daily-use baseline and were not published as release artifacts. Version numbers follow the project's own version model rather than the official Semantic Versioning specification.

## Unreleased

## 1.8.0 — 2026-11-04

[Delivery record](docs/delivery/1.8.0/delivery.md)

- Configure the application icon shape and apply it consistently in Home and Drawer.
- Add the Home bottom app bar, with fixed and semi-hidden presentation.
- Revise Home edit mode, including module controls, movement, and favorite editing.
- Place favorites through the accepted ribbon and Flow layouts.
- Show one list-level Home favorite loading state, and end it when the first inventory read fails.
- Clip a collapsed semi-hidden bottom bar to its upper `16dp`, resize the favorite list with it, and use an `8dp` bottom margin.

### 1.7.2

- Restore click, long-press, and drag actions on Drawer search results when the alphabet
  index contains enough sections to reach its height limit.

### 1.7.1

- Normalize application icons through one device-mask path, removing the legacy-icon wrapper
  heuristics and improving Drawer scrolling responsiveness on the author's primary device.
- Move the platform icon renderer into `platform/icons` and retain the replaceable rendering
  boundary for future custom icon shapes.

## 1.7.0 — 2026-09-29

- Bind the two Home basic-information gestures, double tap and long press, to No action, Edit mode, or Screen lock on a new Quick action settings page. Both slots default to No action.
- Screen locking is triggered by the gesture you bind and now ships in every build type. The Quick action settings page carries the always-visible screen-lock service state row and the accessibility authorization flow.
- Home shows a dismissible prompt while Launcher4Max is not the default launcher.
- The application-size block gains two three-stop tapering sliders.
- Home modules and Drawer gain independent icon and text sizes and a Hidden application-name placement.
- The standalone Double-tap-to-lock Settings entry is removed.
- **Breaking:** the application identity changed from `com.avenor.launcher` to `com.maxarchm.launcher`. Android treats `1.7.0` as a different application from `1.0.0` through `1.6.0`, so it cannot update an earlier installation in place. Move your data with the Settings backup file.

## 1.6.0 — 2026-09-10

[Delivery record](docs/delivery/1.6.0/delivery.md)

- Back up favorites and settings to one local JSON file, and restore from a backup after an explicit confirmation.
- Add a favorite by long-pressing and dragging an application from Drawer into Home edit mode.
- The Home edit dock sits at the top of safe content, and the style settings panel expands and collapses as an animated slot swap.
- The Drawer background becomes a continuous opacity percentage slider with a live percentage readout.
- Favorite multi-selection marks selected rows with overlaid order badges on scaled, outlined cells.

## 1.5.0

[Delivery record](docs/delivery/1.5.0/delivery.md)

- Search the Drawer inventory by displayed application name.
- Reach Settings from the final Drawer row.
- Configure application size, arrangement, section anchors, and the Drawer background from a revised stacked display-settings panel.

## 1.4.0

[Delivery record](docs/delivery/1.4.0/delivery.md)

- Home favorites become one ordered main list of vertical modules and horizontal ribbons that you can create, style, and reorder.
- Remove and move applications within or across modules, with failure-safe persistence.
- Add applications through a Drawer multi-selection bound to a destination module.

## 1.3.0

[Delivery record](docs/delivery/1.3.0/delivery.md)

- Existing readable favorites migrate into one unified destination model.
- One full-width or two equal-width vertical lists with independent list-level sizes.
- One Home edit session manages lists and favorites, with a destination-bound Drawer multi-selection for adding applications.
- Up to five favorite bars, same-container and cross-container application movement with target feedback, and two-axis edge auto-scroll.

## 1.2.0

[Delivery record](docs/delivery/1.2.0/delivery.md)

- Return to an already usable Home without unnecessary blocking reloads.
- Access, scroll, launch, reorder, move, and swap favorites across the primary and companion groups.
- Move continuously between Home and Drawer with scroll handoff and index-anchor navigation.

## 1.1.0

[Delivery record](docs/delivery/1.1.0/delivery.md)

- Edit and reorder full-width primary Home favorites.
- Invoke platform application shortcuts from the action sheet on Home and Drawer.
- Settings exposes the default-home state, the license, the project repository, and version information.
- Read the local Privacy statement and the prominent disclosure.
- Optionally enable double-tap lock through Android accessibility settings.

## 1.0.0

[Delivery record](docs/delivery/1.0.0/delivery.md)

- First usable version: an offline Home and Drawer loop with application launch, the application action sheet, and persistent favorites.
