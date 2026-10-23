# Home Bottom App Bar

> Public semantic source: English. Chinese counterpart: [home-bottom-app-bar.zh-CN.md](home-bottom-app-bar.zh-CN.md). Exact visual values belong to the [Home presentation specification](../presentation/home.md#home-bottom-app-bar). The Settings entry belongs to [Settings behavior](../surfaces/settings.md).

## Purpose

The Home bottom app bar is one favorite position at the bottom of Home. It is not a favorite module, not a second favorite list, and not the removed edit dock. A favorite placed here is still a favorite. The bar decides only where that favorite is shown.

One stable launchable identity is favorited once across the whole of Home. It cannot occupy both the bar and a favorite module. Primary, cloned, and work-profile entries remain distinct identities.

The accepted Settings entry and page title are `Home bottom app bar` in English and `主页底部常驻应用栏` in Simplified Chinese. The Chinese word for a standing bar names the feature. It does not mean the bar can only use the fixed presentation. The bar shows no application names. Accessibility still announces each application name.

## Settings

- Settings places one primary entry directly below `Icon shape` and above the Data entries. It belongs to the same Launcher-settings grouping and opens its own page. It does not share a page with Icon shape.
- The page title is `Home bottom app bar`. From top to bottom it shows one master switch, one preview, and the remaining settings. The master switch is the only control that turns the feature on or off. It defaults to off. The page also shows a localized read-only capacity message for the selected icon tier and current Home width, such as `Up to 6 apps at this width`. This message updates whenever the width or selected icon tier changes.
- While the switch is off, Home shows no bar, no collapsed edge, and no reserved height. Membership, the fixed-or-semi-hidden choice, and the icon size remain saved. The settings below the preview use the shared disabled treatment and reject input. The preview stays visible and does not accept changes.
- While the switch is on and the bar has no favorite, ordinary Home still shows no bar and reserves no height. Edit mode is the exception defined below.
- The master switch, the fixed-or-semi-hidden choice, and the icon size are independent saved
  settings. When no valid saved presentation choice exists, the fixed-or-semi-hidden choice
  defaults to semi-hidden. When no valid saved icon tier exists, the icon size defaults to Medium.
  Turning the master switch off and on again restores the saved presentation choice and icon size;
  it does not reset either setting. Fixed means the bar stays at full height. Not fixed means
  semi-hidden. Icon size uses the existing Large, Medium, and Small icon tiers. There is no separate
  maximum-count setting and no membership editor. Home edit mode supports adding and removing.
  Adding and moving also follow the one-shot and Drawer drag entries defined below.
  Normal Home supports removal through the
  [application action sheet](../surfaces/app-action-sheet.md#launcher-actions).
  Reordering can also begin from a fully presented favorite in normal Home, as defined by
  [one-shot operation](../surfaces/home.md#one-shot-operation).
- The preview window is non-interactive and shows the current selected presentation: the complete bar when fixed, or the ordinary collapsed bar when semi-hidden. It does not expand, launch, or accept changes. While the master switch is off, the preview still shows that selected presentation, and the settings below it remain disabled. The preview does not decide capacity. Home's available width does. Exact preview geometry belongs to the [Settings presentation specification](../presentation/settings.md#home-bottom-app-bar-settings-page).
- Every settings change applies immediately and starts one exclusive save of the complete bar
  settings state. While that save is unresolved, other bar-setting changes are unavailable, but
  leaving the page remains available. A save failure restores the last successfully saved complete
  bar settings and shows the localized short Toast `Unable to save Home bottom app bar settings`;
  a page that has already closed is not reopened. A later valid change may retry. The page has no
  separate confirm action.

## Capacity and order

- The bar is one horizontal row ordered from the physical left. It does not wrap and does not scroll inside itself.
- Capacity is the number of whole icons that fit in the current Home safe width after the bar's own side insets and the gap between icons. A partial icon does not count. The count is not stored. Capacity is calculated separately for each icon tier, and changing the width or selected tier recalculates it immediately. The Settings page exposes the current result in its read-only capacity message.
- A selected icon tier must have capacity for every saved bar member. A tier that cannot hold the current membership is unavailable. The bar never silently shrinks icons, deletes members, wraps, or scrolls to solve a capacity change. Under the normal width and tier rules, the current membership remains within the selected tier's capacity.
- Switching between fixed and semi-hidden changes presentation only. Membership, order, and icon size stay the same.

## Home presentation states

- A disabled favorite keeps its place, remains saved, and gives the ordinary unavailable feedback. Selecting it does not launch it. Enabling it again restores it in place.
- A favorite is removed only after a successful refresh confirms permanent disappearance. Later favorites close the gap in their saved order. An uncertain read or a failed refresh does not delete or hide it.
- Ordinary launch uses Home's existing surface-wide rapid-activation window. The default-Launcher prompt, the basic-information quick actions, and the upward Home-to-Drawer gesture keep their existing ownership. The bar does not appear inside the prompt and does not take over those gestures.
- The collapsed bar is the only Home-to-Drawer exception: a tap or a short upward drag that begins on it expands the bar. It does not launch an application and does not start the Drawer transition. A collapsed bar cannot start one-shot operation. A downward drag on Home content still scrolls the favorite main list. It does not expand or collapse the bar.

## Semi-hidden and fixed

- A semi-hidden bar keeps its full height while expanded and while collapsed. Neither state increases or decreases that height. Collapsing does not compress the bar to `24dp` and does not recenter its icons inside the exposed area.
- While collapsed, the complete bar moves down along the Y axis until it meets the bottom. It exposes `16dp` of its full height, so the remainder appears cut off by the bottom of the screen. The exposed area therefore shows the upper portion of the icons and background. Large, Medium, and Small all expose the same `16dp`; they do not expose half of `64dp`, `56dp`, or `48dp`. A fixed `24dp` is not the collapsed height.
- The whole exposed `16dp` is the expand target. A tap or a short upward drag expands the bar. It does not launch an application. The bar does not use a separate reveal control.
- Expanding moves the bar up along the Y axis until it is completely visible. This changes the bar's position, not its height. Because the height occupied by the bar does not change, expanding and collapsing do not change the favorite main list's height or its logical scroll position.
- Selecting empty Home space, or dragging downward on the bar, collapses an expanded semi-hidden bar. In normal Home, when no style settings panel, application or module movement, or text editing is active, System Back collapses it before following the ordinary Home Back result. In every other Home state, the active panel, movement, or text-editing rule handles Back first.
- Fixed presentation always uses the full height and full opacity. It does not move or become cut off by the screen bottom. The favorite main list reserves that full height under the existing rule. The list keeps the ordinary module spacing above the bar. That spacing is not part of capacity.
- Edit mode shows a semi-hidden bar expanded. Leaving edit mode collapses it again. A fixed bar stays full height.
- Expanding and collapsing use the shared short-duration property animation. When the system requests reduced motion, the bar reaches its target position directly, without that animation. The collapsed bar uses `80%` opacity. Expanded and fixed bars use full opacity.
- The bar uses one white background at `24%` opacity, a `16dp` corner radius, and `16dp` outer margins on the start, end, and bottom. Its border reuses the favorite-bar border: `1dp` at `12%` opacity. It does not use a dark surface, wallpaper sampling, or blur. Icons on that background do not use the Home glyph shadow.

## Edit mode

- While the feature is on, edit mode shows the bar, including when it has no favorite. With no favorite it shows only the add control. One-shot operation may temporarily expand a collapsed bar, or temporarily show an empty bar while the feature is on, without entering edit mode. Both return to their previous presentation when that operation ends. A release on a full bar is invalid. A Drawer drag-to-favorite uses the same temporary destinations.
- A favorite on the bar uses the same edit treatment as a favorite in a module: a remove control, reordering, and movement between the bar and favorite modules. Selecting it does not launch it, and long-pressing it does not open the application action sheet.
- The remove control unfavorites the application. It does not merely take the application off the bar while leaving it favorited. Undo uses the existing favorite-removal Snackbar and restores the application to its former place on the bar when that place still fits. If that restoration no longer fits, Undo fails, keeps the current state, and uses the existing favorite-restoration failure feedback. The bar is not a module and has no whole-module unfavorite action.
- Movement uses the ordinary favorite insertion rules. The bar is a valid favorite destination. The add control is not an insertion destination. Expanding and collapsing do not change the height used for destination resolution. Internal reordering remains valid even when the bar is full; capacity rejects only a change that would add a member.
- While the bar is below capacity, edit mode shows one add control after the last favorite. The control opens the existing Drawer favorite multi-selection with the bar as its captured destination. The selection cannot exceed the number of remaining places captured when the mode opens. A later width change does not raise or lower that count. An identity that is already favorited stays unavailable. At confirmation, Launcher4Max rechecks the current capacity. If the selected identities no longer fit, the complete confirmation fails, nothing is added, and the selection remains available for retry or cancellation. Otherwise, confirming appends the selected identities to the bar in selection order and makes them favorites. Cancelling changes nothing.

## Backup

- The manual backup includes whether the bar is on, whether it is fixed, its selected icon size, and the ordered favorite identities placed on the bar. The calculated capacity is not stored.
- When restoring a valid backup, the bar keeps the first `N` identities from the backup's saved bar order, where `N` is the current capacity for the restored icon tier and Home width. If fewer than `N` valid identities remain, it keeps all of them. Identities after `N` are not placed in another module or silently reordered; they are omitted from the restored Home bar. The restore remains one atomic replacement, and the backup file itself is unchanged.
- A missing bar section restores the bar to off with no favorites placed on it. Favorites that the backup places in modules stay in those modules.
