# Drawer Presentation Specification

> Public semantic source: English. Chinese counterpart: [drawer.zh-CN.md](drawer.zh-CN.md).

## Responsibility

This document owns exact Drawer layout, typography, component geometry, and visual-state values. [Drawer behavior](../surfaces/drawer.md) owns inventory, sorting, state, selection, and action results; [navigation](../navigation.md) owns transition thresholds; [design foundations](../design-foundations.md) owns shared color and text-size tokens; the [style settings panel presentation](style-settings-panel.md) owns the panel surface and controls shared with Home; the [icon shape contract](../features/icon-shape.md) owns the application-icon clip mode and corner-radius proportion.

## Surface and application content

- Drawer uses a `56dp`-high top app bar below the status-bar safe inset. The bar remains visually transparent over the selected Drawer background rather than creating its own surface. Its Back arrow uses `24dp` artwork centered in a `48dp` interaction target that starts `12dp` from the safe start edge, so the artwork's start edge sits `24dp` from the safe start edge; in ordinary mode the target is horizontally centered within the start side reservation, preserving that same artwork position.
- One full-width `1dp` divider using shared `secondaryTextColor` separates the top app bar from the content below it in every bar state — ordinary, search, and favorite multi-selection — using the shared top-app-bar divider treatment defined by [design foundations](../design-foundations.md). The divider renders the same at every background-opacity percentage and does not make the bar an opaque surface.
- Right-side-name items compose the application icon size and the application name (text) size selected independently through the [style settings panel](style-settings-panel.md) size block. The visible icon is `48dp`, `40dp`, or `32dp` for the Large, Medium, or Small icon tier, placed `8dp` from the item's top and `12dp` from its bottom. The item is exactly `8dp` above the icon and `12dp` below it, and grows only when the selected name line does not fit between those same boundaries, in which case the name line is vertically centered with `8dp` above and `12dp` below it. The complete item remains the selection and long-press target, and the press Ripple covers the complete item without being reduced by the item's padding.
- In right-side-name arrangement, every icon-and-text combination uses a `12dp` icon-to-name gap. The name uses normal-weight shared `largeAppNameFontSize`, `primaryTextFontSize`, or `secondaryTextFontSize` for the Large, Medium, or Small text tier, each with its shared line height and `primaryTextColor`.
- In below-icon-name arrangement, each item uses `8dp` above its icon, a `12dp` icon-to-name gap, and `12dp` below the one-line name, so the item height is exactly `8dp` + the selected icon size + `12dp` + the selected name line height + `12dp`. The name occupies one text box spanning the complete item content width with exactly `8dp` visible-content inset on each side, and the text is horizontally centered within that box; the icon is horizontally centered in the item. Adjacent items therefore keep their name text boxes exactly `16dp` apart without a separate column gap. In hidden-name arrangement, no name is rendered: the icon is placed `8dp` from the item's top and `12dp` from its bottom and horizontally centered in the item, so the item height is exactly `8dp` + the selected icon size + `12dp`, and the text tier has no effect on hidden-name items.
- The application grid boundary begins `8dp` from the safe start edge and ends `8dp` before the safe-end AlphabetIndex reservation. Every item adds its own `8dp` start and end visible-content inset. The resulting first and last visible-content boundaries are `16dp` from their applicable outer boundary, and adjacent items produce `16dp` between visible content without a separate column gap. Implementations must not add another container and item inset that changes these observable totals.
- Columns use `0dp` separate spacing and divide the available application-grid width by the
  width-constrained effective items-per-row count. Every rendered name uses one static line and
  end ellipsis.
- The selected icon and text sizes remain unchanged when the available width becomes narrower.
  The effective items-per-row count is the selected count limited by the current name-placement
  range and the width that can contain complete items, with a minimum of one column. A narrow
  window therefore keeps one column rather than compressing or clipping an item. All applications
  remain available through additional rows; reducing the effective column count does not remove
  applications or change their order. The left-side anchor, AlphabetIndex reservation, outer
  margins, and item insets all contribute to the available application-grid width.
- At every icon and text tier combination, the selected count ranges from one to two for
  right-side names, one to four for below-icon names, and one to six for hidden names.
  Actual equal-width columns use the effective count above; narrowing the window does not
  rewrite the saved selection. The stepper displays and operates on the selected count.
  At a selected-count boundary its targets retain ordinary geometry but use the shared disabled
  presentation and expose disabled state rather than actionable Ripple.
  When controls are not temporarily locked by animation or saving, their boundary presentation
  updates in the same frame as a label-placement change. These examples refer to the selected
  count, not necessarily the rendered columns: switching to `Below` at two enables increment;
  switching to `Right` from three or four displays two with increment disabled; switching to
  `Hidden` preserves the count and enables increment below six; switching from `Hidden` to
  `Right` or `Below` clamps a count above the destination maximum to two or four respectively,
  with increment disabled.

## Background opacity and contrast

- The Drawer background composites one full-surface layer of `darkSurfaceBaseColor` over the wallpaper at the user-selected opacity percentage. `0` leaves the wallpaper clear across the complete Drawer, and `100` presents the complete solid surface color; intermediate percentages present that color blended over the wallpaper. The background adds no blur, glass effect, or additional scrim, does not sample the wallpaper, and does not change by list position, pointer position, search result, or local luminance.
- Text and monochrome foreground artwork use `primaryTextColor` and one fixed dark glyph- or artwork-following shadow at every percentage, using the accepted parameters below. At low percentages the foreground contrast relies on these treatments; readability over bright wallpapers is the user's selected trade-off, judged by author-device acceptance.
- The background opacity changes only the Drawer background and applicable contrast treatment. Top-app-bar, application, anchor, search, AlphabetIndex, Settings, multi-selection, modal, and interaction geometry remain unchanged.

### Accepted background parameters

The author accepted the following current delivery values on 2026-09-06; the 2026-09-07 pure-blur decision and its parameters were superseded on 2026-09-09 by the continuous background-opacity decision. Further optimization remains follow-up work and does not block delivery:

| Treatment | Accepted value |
| --- | --- |
| Foreground glyph shadow (text and monochrome artwork) | Black at `65%` opacity (`#A6000000`), `0dp` horizontal offset, `1dp` vertical offset, `2dp` blur radius |
| Background color | Shared `darkSurfaceBaseColor`, composited over the wallpaper at the selected percentage |
| Background-opacity granularity | Whole percentages `0` through `100`; tentatively accepted |

Future parameter changes require author acceptance and an update to this specification.

## Search field and matching emphasis

- The top app bar centers one `40dp`-high search field independently of the unequal side controls. It remains centered when ordinary Back and display-settings controls are replaced by the search-mode empty-left reservation and right-side `Cancel` action. Side reservations and the field may adapt to the safe width, but they must not overlap or shift the field away from the screen center.
- The field uses a transparent interior and `20dp` corner radius. Its leading magnifying-glass and conditional trailing X use visible `20dp` artwork. Internal start and end insets are `12dp`, and the icon-to-text gap is `8dp`. The X is centered in an interaction target of at least `48dp x 48dp` that remains inside the complete `56dp` top-app-bar interaction region even though the visible field is shorter.
- Hint and query text use normal-weight shared `primaryTextFontSize` and its line height. The English hint is `Search apps`; the Simplified Chinese hint is `搜索应用`. Hint color uses `secondaryTextColor`; entered text uses `primaryTextColor`.
- Outside search mode, the field uses a `1dp` Material `outline` boundary. Search mode animates that boundary to `2dp` using `primaryTextColor` and the shared short-duration color/property animation token. Empty query, hidden keyboard, and temporary loss of text focus do not remove the active boundary while search mode remains active.
- The physical-right `Cancel` text uses medium-weight shared `secondaryTextFontSize`, its line height, and `primaryTextColor`, with one interaction target of at least `48dp` high and wide enough for the complete localized label. The corresponding physical-left reservation matches the right reservation width so the field remains centered, but exposes no visible control, interaction, focus, or accessibility node.
- A matched application-name span uses `primaryTextColor` and medium weight. Unmatched text retains the applicable normal style. Emphasis does not add a background, underline, independent padding, separate semantic node, or geometry change.
- The no-match message is centered in the available application region and uses normal-weight shared `primaryTextFontSize`, its line height, and `primaryTextColor`, without an error icon, progress indicator, or Retry presentation.

## Section anchors and alphabet index

- Section anchors use bold shared `largeAppNameFontSize`, its line height, and `primaryTextColor`. An Inline anchor occupies a full-width `40dp` row, vertically centers its text, and places the text start exactly `16dp` from the safe start edge; it scrolls without pinning. A Left-side anchor occupies a `40dp`-wide column with `0dp` gap before the remaining application grid. Its text is horizontally centered and begins `8dp` below the owning section's top until section-bounded pinning places that same geometry below the top app bar. The Settings left-side anchor replaces text with `16dp x 16dp` `primaryTextColor` gear artwork, horizontally centered with the same `8dp` top offset. Neither anchor presentation changes layout while pinning or exposes an interaction target.
- The AlphabetIndex occupies a fixed `32dp` width at the safe end edge and is vertically centered in the available application region. Labels use `11sp` medium-weight text in fixed `20dp` slots. Its Settings gear uses `11dp` artwork inside one complete `20dp` slot.
- The maximum index model contains 28 fixed slots. The Drawer behavior contract owns the resulting available-height threshold and scrolling decision.
- The active-token bubble has a `64dp` circular body and a `12dp` tail pointing toward the AlphabetIndex, for a complete `76dp x 64dp` shape. It uses one light-gray `#CC8A8A8A` background. The bubble remains vertically centered in the available application region, and its tail sits immediately before the AlphabetIndex with a `16dp` gap. Its `32sp` medium-weight text or `32dp` Settings gear is centered in the circular body. The bubble does not follow the active slot vertically.
- Drawer Loading uses the shared `48dp x 48dp` progress-indicator size, and Error uses the shared `48dp x 48dp` non-interactive status-icon size. Their message uses normal-weight shared `primaryTextFontSize`, its line height, and `primaryTextColor`. Indicator or icon to message spacing is `16dp`; Error message to Retry spacing is also `16dp`.

## Settings fixed item

- The fixed Settings item is at least `56dp` high. It uses a `40dp` `primaryTextColor` gear, a `16dp` icon-to-name gap, and a normal-weight shared `primaryTextFontSize` name with its line height and `primaryTextColor`.
- The item begins `16dp` from the safe start edge. Its content ends before the AlphabetIndex reservation, which consists of `16dp` safe-end content inset plus the fixed `32dp` index width. The complete item is the interaction target; the gear exposes no duplicate target or description.

## Error layout

- Below the fixed top app bar, the status icon, message, and Retry form a centered vertical
  stack in the safe application region. They use the Loading/Error sizes and gaps above;
  Retry reuses the standard Material 3 text button and shared interaction-target rules.
- In ordinary Error, one Settings item follows Retry with the same `16dp` gap already used
  between the message and Retry. The complete stack, including Settings, is vertically centered.
  The Settings row reuses the [fixed item](#settings-fixed-item) height, gear, text, gap,
  horizontal geometry, and combined interaction target, including its safe-end reservation.
  That reservation stays blank; Error renders no section heading, anchor, or AlphabetIndex.
- Favorite multi-selection Error uses the same status stack without the Settings row, following
  [Drawer behavior](../surfaces/drawer.md#availability-states-during-multi-selection).

## Favorite multi-selection

- The multi-selection top app bar remains `56dp` high and uses start, center, and end content regions. The start `Cancel` and end `Confirm` controls each occupy a vertically centered `48dp`-high target flush with their applicable safe edge, with `12dp` start and end content padding inside that target. The text itself retains its shared font size and natural line height rather than being assigned a `48dp` text height.
- The two side reservations use the wider measured width of the complete localized `Cancel` and `Confirm` targets, so they remain symmetric and the destination description stays centered on the physical screen. The center description uses the remaining width, remains one line with end ellipsis, and cannot overlap either action. All three regions are vertically centered.
- Multi-selection items reuse the ordinary inventory's items-per-row grid and arrangements; item geometry matches ordinary Content, and the selection state changes no layout. Multi-selection items show no press Ripple; the selection scale and outline treatment is their visible response instead.
- An available unselected item shows no selection indicator; the multi-selection mode is expressed by the top app bar.
- A selected item is scaled to approximately `90%` as a purely visual transform with unchanged layout bounds, restoring `100%` on deselection; the scale animation uses the shared short-duration property-animation token. It also draws one complete `2dp` `primaryTextColor` selection outline with `12dp` corners, and carries one overlaid order badge at its top-left corner.
- The order badge overlays the item without occupying layout space: `primaryTextColor` background, one-based plain-text order in medium-weight shared `secondaryTextFontSize` and line height using `darkSurfaceBaseColor`, `12dp` corners at the top-left and bottom-right only with the other two corners square, and a minimum size of approximately `20dp` adapting to its content with `4dp` horizontal content padding. The badge is not an independent interaction target.
- Disabled already-favorited items receive no badge, outline, or scaling and use the shared disabled semantic opacity for icon and name.

## Display settings

- Drawer hosts the shared [style settings panel](style-settings-panel.md) as a compact custom surface that may enter from the bottom. It uses the shared horizontal outer margin and adds a Drawer-specific bottom margin. The panel opens and closes with one height property animation — growing from `0` to its content height on open and retracting back on close — using the shared short-duration property-animation token with its approximately `200ms` reference duration, matching the Home style panel. A transparent modal layer blocks underlying input without dimming the exposed Drawer or system background; the shared edge border must preserve visible separation from both exposed layers.
- The Drawer-specific bottom margin follows the current implementation's appearance accepted
  by the author on a physical device; no exact value must be predetermined in this specification.
  The implementation maintains this parameter and may adjust it under author device acceptance.
  Account for the bottom safe-area inset and host padding once, without duplicating either
  in the panel's bottom spacing.
- Drawer presents four setting blocks through the shared panel: section anchors, application arrangement, application size, and Drawer background. The section-anchor control uses the shared compact two-option selector; the name-placement (`Right` / `Below` / `Hidden`) control and the items-per-row stepper share the arrangement block's content line using the shared compact selector and stepper; the application-size block uses the shared dual tapering-slider control selecting the icon and text tiers. Drawer retains its own setting names, option labels, availability, and save behavior.
- The Drawer-background block presents one percentage readout and one background-opacity slider on its content line using the shared background-row geometry, readout treatment, and slider accent defined by the [style settings panel presentation](style-settings-panel.md): a fixed-width percentage readout at the physical left, a `16dp` gap, and a continuous slider filling the remaining width.
