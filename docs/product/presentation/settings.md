# Settings Presentation Specification

> Public semantic source: English. Chinese counterpart: [settings.zh-CN.md](settings.zh-CN.md).

## Responsibility

This document owns exact Settings typography and row geometry. [Settings behavior](../surfaces/settings.md) owns page content, navigation, and action results; [design foundations](../design-foundations.md) owns shared color and text-size tokens.

## Top app bar

- The fixed top app bar is at least `56dp` high and uses `16dp` horizontal padding for its title and trailing content.
- Its visible Back artwork uses the shared `24dp` functional-icon token centered in a standard `48dp x 48dp` icon-button target that starts `12dp` from the safe start edge, so the artwork's start edge sits `24dp` from the safe start edge, matching the [Drawer top app bar](drawer.md). The Back control provides a localized accessibility name.
- The title uses Material 3 `titleLarge` and shared `primaryTextColor`. A full-width `1dp` divider using shared `secondaryTextColor` separates the app bar from the scrolling item list, using the shared top-app-bar divider treatment defined by [design foundations](../design-foundations.md).

## Primary items

- Primary titles use the shared `primaryTextFontSize` and line height, equivalent to Material 3 `titleMedium`, with medium weight and `primaryTextColor`.
- Supporting text uses the shared `secondaryTextFontSize` and line height, equivalent to Material 3 `bodyMedium`, with normal weight and `secondaryTextColor`.
- A two-line item is at least `72dp` high. A one-line item is at least `56dp` high and vertically centers its title.
- Primary items use `16dp` horizontal content padding and a trailing `24dp` Android or Material arrow where the behavior contract includes one.

## Secondary items

- Secondary items use the shared `secondaryTextFontSize` and line height, equivalent to centered Material 3 `titleSmall`, with medium weight and `secondaryTextColor`.
- Each secondary item is at least `40dp` high and centers its content horizontally and vertically. A selectable secondary item uses that complete full-width row as its interaction target; its ordinary minimum target is `40dp` and does not expand invisibly to `48dp`, although the row may grow when system font scaling requires more height. This author-accepted target-size exception applies only to Settings secondary items and does not alter primary items, the Back control, or other ordinary icon controls. Version information keeps the same visual row geometry but is not interactive.

## Quick action settings page and selection dialog

- The `Quick action settings` page reuses the Settings top app bar and the primary-item row geometry defined above. Its title is `Quick action settings` and each row carries a title plus supporting text.
- Every row on the page carries the standard trailing arrow of the primary-item treatment, matching the Settings page. The two slot rows carry it as well even though they open a local selection popup rather than another destination.
- The selection popup is one anchored dark popup presenting a single vertical sequence of option rows. The specification describes the observable result only and does not prescribe a container type.
- The popup uses the shared `darkSurfaceBaseColor` background with exactly `12dp`
  corners and reuses the shared edge treatment defined in the
  [style settings panel presentation](style-settings-panel.md#shared-panel-surface).
  Each option row is `48dp` high with `16dp` horizontal padding and contains one
  leading `20dp` radio single-selection indicator followed by the action label
  with `12dp` between them. The radio and the label both use `primaryTextColor`,
  and the label uses the shared `primaryTextFontSize`. The current binding is
  marked in its radio.
- Each option row provides the shared Material ripple, clipped so no ripple extends beyond the popup's rounded corners; the top and bottom rows therefore follow the popup's `12dp` corners.
- The popup presents no scrim and does not dim the page. It is anchored to the tapped row rather than centered on the screen: its top edge aligns with the vertical midpoint of that row, and its trailing edge sits `16dp` from the safe trailing screen edge. Because the option list is short, no bottom-edge alignment or bottom clamping is defined.
- The popup width wraps its content. Together with its fixed trailing `16dp` inset, the popup remains inside the safe screen area and never forces its own horizontal scrolling.

## Icon shape settings page

- The `Icon shape` page reuses the Settings top app bar and page structure. Its title is
  `Icon shape`. The page itself does not scroll. The preview region fills all available height
  between the app bar divider and the settings block anchored at the bottom.
- The preview region contains one fully transparent rounded-rectangle window with `12dp` corners.
  The window is inset `64dp` from all four edges of the region, shrinking both width and height.
  Available parent bounds constrain the window to a nonnegative size,
  potentially zero on short screens; preview drawing stays inside the region and window bounds,
  prioritizing settings-block visibility.
  The window does not read, sample, copy, or draw wallpaper; the system wallpaper shows through.
  It adds no dimming, scrim, blur, or overlay.
  The Home bottom app bar preview window is the other transparent Settings exception.
  Every other region keeps the standard opaque Settings surface.
- Inside the window, three vertically stacked non-interactive rows form a group centered
  horizontally and vertically, with `16dp` between rows. The group height follows its row
  contents and spacing, allowing text to grow with system font scaling.
  Each row renders one sample icon with the current shape preference at its icon-size stop:
  large `48dp`, medium `40dp`, small `32dp`.
- The names to the right of the icons are `Sample app (Large)`, `Sample app (Medium)`, and
  `Sample app (Small)` in English; Chinese uses `示例应用（大）`, `示例应用（中）`, and
  `示例应用（小）`, with full-width parentheses. The icon-to-name gap is `16dp`.
  Each row centers the icon and name as one group and aligns them vertically at their centers.
  Names stay on one line and use end ellipsis when available width is exhausted.
- Names reuse the matching Large, Medium, or Small Home application-name text tier from
  [Home vertical favorite modules](home.md#vertical-favorite-modules): its shared font size,
  line height, normal weight, and `primaryTextColor`. They also reuse the glyph shadow owned by
  [Home surface and basic information](home.md#surface-and-basic-information).
  The row provides one localized accessibility name
  identifying its size stop, without duplicate announcements from the icon and text.
- Below the preview region, the settings block has `16dp` horizontal margins and reuses the shared
  [style settings panel presentation](style-settings-panel.md#shared-panel-surface):
  `darkSurfaceBaseColor`, exactly `12dp` corners, the shared edge treatment, `32dp` block-title
  rows, `48dp` content rows, and `12dp` content insets.
- The page reserves `32dp` of bottom padding after the settings block inside the safe area.
  Surplus vertical space belongs to the preview region, keeping the settings block at the bottom.
- The `Clip mode` block title row is followed by one content row carrying the compact dual-option
  selector with the options `OEM clip` and `Freeform clip`. The selector reuses the shared compact
  dual-option treatment with one page-owned size exception: a `232dp x 40dp` frame, start-aligned
  in the content row, with the shared `2dp` internal padding and two equal `114dp x 36dp` thumbs.
  The `232dp` width is a tentatively accepted value. Its height, boundary, corner treatment,
  label treatment, and targets remain the shared treatment.
- The `Corner radius` block title row is followed by one content row reusing the shared background-row geometry: a fixed-width integer-percentage readout measured at the widest value `100%`, a `16dp` gap, and one continuous slider filling the remaining row width with a full-row-height interaction target. The readout uses the shared secondary text treatment and `primaryTextColor`; the slider's active track, thumb, and announcement behavior follow the shared slider treatment. While `OEM clip` is selected, the slider reports unavailable with the shared disabled presentation and the saved percentage is unchanged.

## Home bottom app bar settings page

- The `Home bottom app bar` page reuses the Settings top app bar, non-scrolling page, preview-region
  placement, settings-block placement, and `32dp` safe-area bottom padding defined for the
  [Icon shape settings page](#icon-shape-settings-page).
- Its preview window is inset `16dp` from all four edges of the preview region. That page inset is
  fixed and is not scaled. The window is fully transparent, uses `12dp` corners, and reveals the
  system wallpaper without reading, sampling, copying, or drawing it. Parent bounds may reduce it
  to zero while keeping the settings block visible.
- The window's bottom edge represents the screen bottom. The preview bar is aligned to that edge.
  Fixed presentation shows the complete bar. Semi-hidden presentation shows the ordinary collapsed
  bar, with its lower portion cut off by the window's bottom edge. The preview does not expand.
- The preview count is the current maximum capacity for the selected icon tier and the real Home
  safe width. It is not recalculated from the scaled preview dimensions. The icons use the default
  platform application icon and the current icon-shape preference. They show no names.
- One scale applies to the complete Home bar presentation:
  `preview-window interior width / Home safe width`. It is recalculated only when either width
  changes. The scale covers icon size, icon gap, the bar's fixed outer margins and inner padding,
  corner radius, border width, bar height, and collapsed exposure together. The preview does not
  reuse those Home dimensions at their unscaled values.

## Informational Bottom Sheets

- The Screen-lock explanation, Privacy, Launcher4Max License, and Third-party notices reuse the exterior shell geometry defined by the [application action sheet presentation specification](app-action-sheet.md): complete available portrait-phone width, `12dp` top corner radii, natural height up to the status-bar safe boundary, `0dp` global content padding, and the shared `32dp x 4dp` handle with `12dp` vertical padding above and below.
- These informational sheets do not inherit the application identity row, shortcut rows, five Launcher-action slots, or badge. They use one fixed title row at least `48dp` high with `16dp` horizontal content insets and one body region with `16dp` content padding. Only the body scrolls when content exceeds the available height; the title and drag handle remain visible.
- The accessibility prominent disclosure remains a separate Material 3 Dialog with its own affirmative and cancellation actions. It does not inherit Bottom Sheet geometry.
- Settings exposes no visible `About` group heading. `About` in the behavior document is organizational text only; secondary-item order provides the visible grouping.
