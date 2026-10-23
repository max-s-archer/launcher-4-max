# Icon Shape Interaction Specification

> Public semantic source: English. Chinese counterpart: [icon-shape.zh-CN.md](icon-shape.zh-CN.md). The Settings entry, its position, and the Settings transparency exceptions are recorded in the [Settings interaction specification](../surfaces/settings.md); exact page and preview geometry belongs to the [Settings presentation specification](../presentation/settings.md#icon-shape-settings-page).

## Purpose and scope

Icon shape lets the user control how application icons are clipped, instead of fixing the clip to the device default. The contract registers exactly one global shape preference with two modes:

- `OEM clip`: application icons keep the device-default clip result delivered today. This is the default for every installation, new or upgraded.
- `Freeform clip`: the square base of each icon is clipped with one user-chosen corner-radius proportion.

There is one shape preference for the whole product. It is not per-surface, per-module, or per-icon; mixed shapes within one surface are outside the current contract, as are shape presets, per-module shape overrides, and any second shape dimension. Future additions are additive contract changes.

## Shape preference

- The preference resolves to exactly one of the two modes. Switching modes applies immediately and persists locally with the complete settings state.
- The last saved freeform proportion is retained across mode switches: selecting `OEM clip` does not reset it, and selecting `Freeform clip` again restores it.
- On a fresh configuration that has never saved a proportion, selecting `Freeform clip` for the first time starts at `50`.
- In `Freeform clip` mode the proportion is one integer percentage from `0` through `100`: `0` renders the square base with no rounding and `100` renders the full circle. The rendered corner radius is the selected proportion of half the applicable icon-slot size, so the same proportion produces the same relative shape at every icon size.
- In `OEM clip` mode the saved proportion is not applied and is not changed by the mode switch. The corner-radius control reports unavailable with the shared disabled presentation while `OEM clip` is selected.
- Mode changes take effect immediately and start one exclusive save of the complete shape
  preference. While that save is unresolved, further shape changes are unavailable, but the top
  app bar Back control and system Back remain available. The page provides no separate confirm
  action. A corner-radius drag previews the value in real time without changing the saved state;
  releasing commits the value once, while pointer cancellation, Back, or external interruption
  restores the last successfully saved value. A save failure restores the last successfully saved
  complete preference, shows the localized short Toast `Unable to save icon shape`, and does not
  reopen a page that has already closed. A later valid change may retry.

## Scope of application

- The shape preference applies to application icons in Home vertical modules, horizontal ribbon
  entries, the Home bottom app bar, and Drawer in every state of that list, including search
  results and favorite multi-selection.
- Platform shortcut icons in the application action sheet and every functional glyph in the product (add-favorite artwork, arrows, anchors, status and service artwork) are not application icons and do not follow the shape preference.
- The preference changes only the icon clip. It does not change icon-slot sizes, module geometry, launch behavior, or any other presentation value owned by other contracts.

## Settings surface

Settings contains one primary item titled `Icon shape`, placed directly below `Quick action settings` and above the Data entries, which opens the dedicated page defined here. The page reuses the Settings page structure and top app bar, including its Back control, and Back returns to Settings at its prior position.

- The page presents one live-preview window above one settings block. Exact geometry belongs to the [Settings presentation specification](../presentation/settings.md#icon-shape-settings-page); the spatial sketch is the [icon-shape settings wireframe](../wireframes/icon-shape-settings.txt).
- **Live-preview window:** one rounded rectangle whose interior is fully transparent.
  It does not read, sample, copy, or draw the wallpaper. The system wallpaper remains visible
  through that interior. The preview region fills the space between the app bar and the settings
  block; the window is inset within that region as defined by the
  [Settings presentation specification](../presentation/settings.md#icon-shape-settings-page).
  The page itself does not scroll. This window and the Home bottom app bar preview window are
  the Settings regions that reveal wallpaper.
  Inside the window, exactly three vertically stacked, non-interactive rows are centered
  horizontally and vertically, one per icon-size stop: large, medium, small. Each renders the
  fixed sample icon, the system default application icon, with the current preference result.
  Real installed applications are not used as preview samples. The rows update in real time while
  the corner-radius control is dragged, and each carries a localized accessibility name
  identifying its size stop.
- **Settings block:** the block below the preview follows the shared style-settings-panel language of block titles and content rows. It contains one `Clip mode` content row with a compact dual-option selector offering `OEM clip` and `Freeform clip`, and one `Corner radius` content row combining an integer-percentage readout with one continuous slider exposing `0`–`100`. The readout, slider range handling, and disabled presentation reuse the shared controls; the slider is non-interactive while `OEM clip` is selected.
- The page itself does not scroll; the preview region absorbs the remaining vertical space.
  Settings' own entry, return, and state-refresh behavior is unchanged.
- Each preview row displays a localized sample application name with its size tier to the
  right of the icon, with the icon and name centered as one group. The localized accessibility
  name for each tier is announced once. Exact wording and styling belong to the
  [Settings presentation specification](../presentation/settings.md#icon-shape-settings-page).

## Backup and restore

- The shape preference — its mode and saved proportion — is part of the manual backup file and follows its schema rules.
- A missing preference section restores the complete default shape preference: `OEM clip` mode
  with a saved proportion of `50` for a future switch to `Freeform clip`. A missing mode field
  uses `OEM clip`. A present `Freeform clip` mode with a missing proportion keeps that mode and
  uses proportion `50`. A missing or invalid proportion in `OEM clip` uses proportion `50`
  without affecting the current OEM rendering. An unknown, empty, or otherwise invalid mode
  uses `OEM clip` while other valid backup sections remain restorable. An out-of-range,
  non-integer, empty, or otherwise invalid proportion uses `50` while other valid backup
  sections remain restorable. If the mode and proportion cannot be resolved into one valid
  shape preference together, the complete shape preference falls back to its default while
  other valid backup sections remain restorable. No invalid proportion is clamped.

## Acceptance intent

- Given the default preference, when icons in Home vertical modules, ribbon entries, the Home
  bottom app bar, or Drawer are presented, then every application icon keeps the device-default
  clip result and no custom rounding is applied.
- Given `Freeform clip` with any saved proportion, when large, medium, and small icons are
  presented across Home vertical modules, ribbon entries, the Home bottom app bar, and Drawer,
  then every application icon shows the same relative corner shape: the proportion of half its
  slot size.
- Given `Freeform clip` at `100`, when any application icon is presented, then it renders as a full circle; at `0`, it renders as a square.
- Given the page is open with `Freeform clip`, when the user drags the corner-radius slider, then all three preview rows update in real time without changing the saved state; when the pointer is released, the final value is committed once without any confirm action. Cancelling the drag restores the last successfully saved value.
- Given `OEM clip` is selected, when the page is shown, then the corner-radius slider reports unavailable with the disabled presentation and the saved proportion is unchanged.
- Given the user switches `OEM clip` to `Freeform clip` after having saved a proportion, then that saved proportion applies again.
- Given a fresh configuration, when `Freeform clip` is selected for the first time, then the proportion starts at `50`.
- Given the page is open, when any region outside the preview window is presented, then it keeps the standard opaque Settings surface. The preview window does not read or draw the wallpaper; its interior is fully transparent, so the system wallpaper shows through behind the three sample icons.
- Given a backup containing a valid preference, when restore completes, then Home vertical modules,
  ribbon entries, the Home bottom app bar, and Drawer present exactly the restored shape.
  Given a missing preference section, the mode is
  `OEM clip` and the saved proportion is `50`; given a missing proportion in a present
  `Freeform clip` section, the mode remains `Freeform clip` and the proportion is `50`.
  Given an invalid mode or proportion, the invalid field falls back to its defined default;
  if the fields cannot form one valid preference, the complete shape preference falls back to
  its default. Other valid backup sections remain restored, and invalid proportions are not
  clamped.
- Given `Freeform clip` is selected, when platform shortcut icons are presented in the application action sheet, then they keep their platform rendering and do not follow the proportion.
- Given the user invokes system Back or the Back control on the page, then Settings opens at
  its prior position. Successfully saved changes remain saved. A corner-radius preview whose
  pointer has not been released is cancelled and restores the last successfully saved value
  without starting a save. An already-started save resolves independently: success retains the
  new preference; failure restores the last successfully saved complete preference and shows
  `Unable to save icon shape`. Neither result reopens the page.
