# Home Presentation Specification

> Public semantic source: English. Chinese counterpart: [home.zh-CN.md](home.zh-CN.md).

## Responsibility

This document owns exact Home layout, typography, component geometry, and visual states. [Home behavior](../surfaces/home.md) owns state and action results; [design foundations](../design-foundations.md) owns shared principles and reusable tokens; the [style settings panel presentation](style-settings-panel.md) owns the panel surface and controls shared with Drawer; the [icon shape contract](../features/icon-shape.md) owns the application-icon clip mode and corner-radius proportion.

## Surface and basic information

- Home preserves wallpaper without a full-screen dim, tint, Scrim, glass surface, or blur. Foreground text and monochrome artwork use shared `primaryTextColor` and may use one fixed glyph- or artwork-following dark shadow — black `65%` opacity (`#A6000000`), `0dp` horizontal offset, `1dp` vertical offset, `2dp` blur radius. Main-list application names use that same accepted glyph shadow. The default-Launcher prompt defined below is the exception: it supplies its own opaque surface and assigns its own text colors.
- Home applies uniform `8dp` content padding inside `safeDrawing`; adjacent visible modules use `8dp` spacing.
- The basic-information module adds no padding. Its time and date rows use `8dp` start and end margins inside the padded boundary, producing `16dp` row boundaries from safe edges. The time row adds `8dp` top margin for a final `16dp` top distance. Date text adds another `8dp` start inset and begins `24dp` from the safe start edge while its complete row target remains at `16dp`.
- The time row is at least `64dp` high and uses `57sp/64sp` bold display text. The date row is `48dp` high and uses normal-weight shared `primaryTextFontSize`, its line height, and `primaryTextColor`.
- The favorite main list and its modules add no outer padding beyond Home content padding.
- The shared Home edit-region background is `primaryTextColor` at `20%` opacity (`#33FFFFFF` in the current dark theme). While in edit mode, the basic-information region, when present, and each vertical favorite module use this background once at their region level. It is not a full-screen wallpaper treatment and is absent from those regions in normal mode. Main-list add-favorite entries reuse this background as specified below; child entries must not duplicate the parent's background layer.
- A favorite whose latest reliable state is disabled keeps its ordinary measured geometry in either module type while its application icon and name use the shared `38%` unavailable opacity. Application-level remove controls and movement feedback remain at their ordinary opacity because those editing actions are still available.

## Default-launcher prompt

- The prompt is one Home layout region between the basic-information region and the favorite main list. It sits inside Home content padding, so its leading and trailing edges are `8dp` from the safe edges, and it uses the shared `8dp` spacing above and below.
- It is at least `72dp` high. It uses the shared `darkSurfaceBaseColor` fill with exactly `12dp` corners and one `1dp` boundary of `primaryTextColor` at `12%` opacity — the boundary treatment used by a horizontal favorite ribbon entry.
- Its content uses `16dp` horizontal content padding. The title uses the shared `primaryTextFontSize`, its line height, normal weight, and `primaryTextColor`. The supporting line uses the shared `secondaryTextFontSize`, its line height, normal weight, and `secondaryTextColor`.
- Because the prompt supplies its own opaque surface, its title and supporting line carry no glyph shadow.
- The trailing region holds the `24dp` functional-arrow hint and then the dismiss control with `8dp` between them. The dismiss control places its `24dp` artwork in a `48dp x 48dp` target whose trailing and vertical edges are flush with the prompt's trailing and vertical edges, so that artwork sits `12dp` from the prompt's trailing edge.
- The complete prompt surface outside the dismiss control is one interaction target with the shared Material ripple clipped to the prompt's `12dp` rounded outline. The dismiss control owns its own separate ripple, and the prompt's ripple does not extend beneath it.
- The prompt's removal on edit-mode entry and its restoration on edit-mode exit are absorbed by the surrounding layout change. The prompt uses no separate enter or exit fade, slide, or scale effect.

## Vertical favorite modules

- Modules use the complete padded content width. Their item geometry and right-side, below-icon, and hidden name arrangements use the applicable Drawer application-item presentation values, composed from the module's selected icon and name (text) tiers.
- One-item-per-row with a right-side name follows the established Home list geometry, composed exactly as the Drawer right-side-name items from the module's selected icon and text tiers. Each icon is placed `8dp` from the item's top and `12dp` from its bottom. The name uses normal-weight shared `largeAppNameFontSize`, `primaryTextFontSize`, or `secondaryTextFontSize` for the Large, Medium, or Small text tier, each with its shared line height and `primaryTextColor`; all retain the Drawer single-line ellipsis rule. The press Ripple covers the complete item without being reduced by item padding.
- Every size permits a selected count of one or two with right-side names, one through four with
  below-icon names, and one through six with hidden names. Exact multi-column item geometry reuses
  the applicable Drawer presentation values. Step controls display and operate on the selected
  count, using the shared panel geometry and boundary presentation at one and the placement's
  maximum. When controls are not temporarily locked by animation or saving, they update in the
  same frame as a name-placement change. These examples refer to the selected count, not
  necessarily the rendered columns: right-to-below at two enables increment; below-to-right from
  three or four displays two with increment disabled; hidden-to-right or hidden-to-below clamps
  a count above the destination maximum to that boundary, with increment disabled.
- A narrower available Home width keeps the selected icon and text sizes unchanged. The effective
  items-per-row count is the selected count limited by the current name-placement range and the
  width that can contain complete items, with a minimum of one column. An extremely narrow window
  therefore uses one column; it does not compress or clip an item. All applications remain
  available through additional rows, and reducing the effective column count does not remove
  applications or change their order. Home content padding and the module's complete available
  width participate in this constraint.
- Actual columns divide the available module width by that effective count. A width change does
  not rewrite the saved selection or the stepper's selected-count boundaries.
- Normal mode has no module title row while the module name is hidden. When the name is shown,
  it uses the non-interactive title row defined in the Home interaction specification.
  While the style settings panel is collapsed, application-level editing adds one remove control
  but no application-reorder handle or reserved handle space; the non-remove portion of the
  complete item remains the long-press movement target.

## Horizontal favorite ribbons

- A horizontal favorite ribbon uses either Ribbon or Flow layout. Both layouts have no
  container padding, use the shared `8dp` module spacing from adjacent modules, and
  retain the same entry geometry.
- Ribbon layout is full width and one row, with `56dp` row height. It remains stationary
  when all entries fit and scrolls horizontally without looping when entries overflow.
- Flow layout is full width and wraps entries into additional `56dp` rows within the
  available module width. It uses `8dp` horizontal entry spacing and `8dp` vertical
  row spacing, and its module height grows naturally with the number of rows. It
  introduces no horizontal overflow treatment or independent scroll region.
- Each entry uses an `8dp` start inset, `40dp` icon, `8dp` icon-to-name gap, one
  normal-weight `primaryTextFontSize` line measured up to `64dp`, and `8dp` trailing
  inset. The name uses its shared line height and `primaryTextColor`. Total width is
  content-measured up to `128dp`; longer names use end ellipsis. Flow layout preserves
  this complete entry geometry and places entries in start-to-end order without clipping
  an entry or creating a partial entry at a row boundary. Entry spacing is `8dp`.
- Each entry uses `primaryTextColor` at `6%` for background, `12%` for a `1dp` boundary, and `12dp` radius. Press Ripple clips to that outline.

## Add-favorite entries

- The two main-list add-favorite entries form one row after the last module, using the shared `8dp` module spacing above that row. They divide the padded content width equally with `8dp` spacing between them and are at least `48dp` high.
- A trailing module add-favorite entry occupies the next entry position after its module's last application. In a vertical module it uses that module's current size, name-placement, items-per-row item geometry, and applicable icon-to-name gap; in a ribbon it uses the ribbon item geometry. Its icon slot therefore matches the owning module's application-icon slot: `48dp`, `40dp`, or `32dp` for a Large, Medium, or Small vertical module, and `40dp` for a ribbon. The add artwork remains the shared `24dp x 24dp` plus icon centered inside that slot. The label follows the owning module's right-side or below-icon name placement instead of forcing one horizontal arrangement. Neither case adds a separate row height or container padding beyond the module's own arrangement.
- A vertical module's trailing add-favorite entry normally shows only its plus icon and label. It adds no background fill, border, or icon backplate of its own, allowing the module's edit-region background to remain visible beneath it rather than clearing that background to expose the wallpaper. Its complete existing item remains the interaction target; icon and label geometry are unchanged. An enabled press retains the shared transient bounded Ripple within that item, not a persistent background.
- A ribbon's trailing add-favorite entry retains the background, border, corner radius, and clipped Ripple defined for [horizontal favorite ribbon entries](#horizontal-favorite-ribbons).
- Both main-list add-favorite entries use one layer of the shared Home edit-region background defined above, replacing rather than overlaying the ribbon entry background. They retain the same border color, opacity, width, and corner radius as horizontal favorite ribbon entries; press Ripple clips to that rounded outline. Their dimensions, icon-and-label layout, and interaction targets remain unchanged. No add-favorite entry introduces full-screen wallpaper dimming, tint, Scrim, or blur.
- Each main-list add-favorite entry combines the shared `24dp x 24dp` functional-icon plus artwork in `primaryTextColor` with its localized label in normal-weight shared `primaryTextFontSize`, its shared line height, and `primaryTextColor`. The icon and label are horizontally centered as one unit with an `8dp` icon-to-label gap. A trailing module entry instead uses the owning module's icon slot, name placement, gap, font size, line height, and label geometry defined above. Every label remains one line and uses end ellipsis. The plus character is not part of the localized string.
- During an exclusive unresolved save, an add-favorite entry uses the shared `38%` disabled opacity without an additional Loading state or progress indicator.
- While a moving application hovers a valid main-list add-favorite entry, that entry shows the same emphasis as a selected module: a complete `2dp` `primaryTextColor` outline with `12dp` corners drawn fully inside its bounds, retaining its ordinary background without an additional fill or tint and changing neither its measurement nor surrounding spacing. This drop emphasis replaces the insertion line for that release and never appears together with one, keeping destination release and boundary insertion visually distinct.
- During one-shot operation, the two temporary main-list add-favorite entries keep the geometry, border, label, and hover emphasis defined above. They do not use the Home edit-region background and do not bring any other edit-mode presentation with them. The moving preview keeps the existing lift treatment.

## Module header and style settings panel

- Home uses no edit dock. The basic-information region remains visible throughout edit mode.
- A module header row is exactly `40dp` high in both ordinary Home, when the name is shown, and edit mode. Its visible content—the name and each icon—is `32dp` high and vertically centered. Icon artwork uses the shared `24dp x 24dp` functional-icon size inside that `32dp` content height; the artwork is not enlarged to `32dp`. The name uses normal-weight shared `secondaryTextFontSize`, its line height, and `primaryTextColor`, remains one line, and uses end ellipsis. `32dp` is the content height, not the text size. Exact icon artwork is selected from the existing image resources during implementation and is not a product-contract value.
- In edit mode, the header reads from the physical left: the whole-module unfavorite action flush with the start edge, then the editable name, then the visibility affordance immediately after the name, then the flexible blank space, then the style affordance, then the module-reorder handle flush with the end edge. The visibility affordance follows the name as the name grows. Once the blank space is exhausted, the name uses end ellipsis and the visibility affordance remains immediately after it. A short name leaves the visibility affordance beside the name; it does not move to the style affordance. The style affordance and reorder handle stay fixed at the end and are not displaced. Each icon target is `40dp x 40dp`, flush with the header's top and bottom edges, and adjacent targets meet without an added gap. The `32dp` content is centered in that target, leaving `4dp` to each edge of the `40dp` target. The first and last content therefore sit `4dp` from the header's start and end edges. Adjacent `32dp` contents have an `8dp` visual gap. The name keeps that same `8dp` visual gap from the unfavorite content before it and the visibility content after it, including after the name is ellipsized. The editable name is `40dp` high. Ordinary Home, when the name is shown, contains only the name.
- An open style panel hosts the shared [style settings panel](style-settings-panel.md) inside its owning module, between the operation header and the module content. It uses the shared horizontal outer margin, rounded surface, edge border, line geometry, and applicable controls. A vertical module exposes the application-arrangement block and application-size block. A horizontal favorite ribbon exposes its display-mode selector and no vertical-module controls or disabled placeholders. The panel scrolls with the main list, remains non-modal, has no Scrim, and does not remove the basic-information region.
- The panel measures to its current content and grows the owning module by that height. It does not reserve a minimum main-list viewport and does not scroll inside itself. Opening and closing use the shared short-duration property-animation token with its approximately `200ms` reference duration, and preserve the main list's logical scroll position.
- Edit mode shows no corner marks. The open module draws one complete `2dp` `primaryTextColor` outline using `12dp` corners. The outline is drawn fully inside the module bounds, adds no fill or tint, and changes neither module measurement nor inter-module spacing. During an exclusive unresolved edit save it uses the shared `38%` disabled opacity. The open panel, module movement, insertion position, and unavailable mutation remain visually distinct. The outline does not expose application-level press or drag feedback.
- The panel has no module-type row, reserves no visual space for a module name, and has no empty-selection prompt. The name belongs to the module header. The panel exposes no add action and no complete-module deletion control.
- Application remove controls use a white X on a solid error-red `20dp x 20dp` circle. The visible circle is also the author-accepted `20dp x 20dp` interaction target and must remain inside its owning item. With a right-side name, the icon begins `8dp` from the item's physical left and top, while the remove circle touches those item edges; the resulting overlap at the icon's top-left is `12dp x 12dp`. A ribbon uses the same geometry. With a below-icon name, the icon remains horizontally centered in the item and the remove circle is positioned `8dp` left and `8dp` above that icon's top-left, again producing a `12dp x 12dp` overlap.

## Loading and failure presentation

- While the favorite main list presents its list-level Loading state, it shows the shared `48dp x 48dp` progress indicator with the localized `Loading favorites…` message. The message uses normal-weight shared `primaryTextFontSize`, its line height, and `primaryTextColor`, with `16dp` between the indicator and the message — the same status presentation as the [Drawer Loading state](drawer.md). The basic-information region keeps its ordinary presentation. No application entry renders a per-entry loading indicator or label; a favorite's unavailable presentation remains the separate treatment defined above.

## Content transitions

- Adding applications to an existing module or restoring them through Undo fades in the affected applications while surrounding entries move smoothly to their resulting positions. Removing an application fades it out while surrounding entries close the gap concurrently; gap closure does not wait for the fade to finish.
- Creating a module, automatically deleting an emptied module, or restoring a deleted module through Undo uses one module-level fade and smoothly repositions subsequent modules. Its children do not repeat the same enter or exit fade. Application movement uses the preview-handoff exception below, including when that movement creates a module.
- A successful application or module reorder animates only the necessary position changes, without fading the complete module out and back in. Ordinary icon, name, badge, or availability updates happen in place without item or module enter/exit animation; a resulting measurement change may use the necessary layout transition.
- An outgoing fade is a non-interactive visual remnant only: it reserves no layout space, exposes no accessibility action, and participates in neither hit testing nor insertion-boundary resolution. It is not a drag-source placeholder. Initial loading, returning to Home, and entries becoming visible through scrolling are not additions and do not trigger enter fades.
- These fades and layout transitions reuse the shared platform or theme short-duration property-animation token defined for [shared controls](style-settings-panel.md#shared-selectors-and-controls), without additional bounce or scale effects. A newer permitted change retargets from the current visible state rather than queueing obsolete transitions. When system animations are disabled, show the resulting state immediately with identical behavior and save results.
- Transitions visualize the state changes owned by [Home behavior](../surfaces/home.md); they neither defer persistence or recovery nor extend Undo availability. Changing edit mode or panel visibility does not by itself count as adding or removing favorites.

## Drag and overflow feedback

- Ribbon overflow may use the existing inward `48dp` `primaryTextColor` gradient from transparent to at most `8%` opacity. It changes no geometry or input ownership.
- At long-press recognition, module movement freezes a complete visual snapshot of the rendered source module. The lifted preview preserves the initial pointer offset, uses no source placeholder, remains at `100%` opacity and `1.0` scale, and does not live-update during that gesture. Remaining modules close the source gap without changing widths. The preview uses a `12dp` elevation-style shadow as its only additional lift treatment.
- Every movement candidate displays exactly one non-layout `2dp` `primaryTextColor` insertion line at the resolved order boundary. Module movement uses a horizontal line across the module's available width at the boundary between adjacent modules. A one-column vertical application layout uses a horizontal line across the candidate item's available width at its top or bottom edge. A multi-column vertical-module row uses a vertical line across the candidate item's measured height at its physical-left or physical-right edge. A wrapped-row boundary uses only the edge belonging to the currently resolved before-or-after candidate; it never duplicates the same semantic boundary at both rows. The line overlays content without reserving space or displacing items.
- During layout transitions, candidate hit testing and insertion-line drawing use the same current visible bounds, not the final animation-target coordinates. Logical neighbors follow the source-omitted order, while their coordinates follow their current visual positions. A ribbon line therefore follows the midpoint of the current visible gap; the `4dp` offset below applies when that gap has its ordinary `8dp` width. Equivalent hit regions never cause a jump between opposite item edges, but reflow and scrolling naturally move the line with its boundary. Such movement does not insert a provisional slot or initiate another hover-driven reorder.
- For application insertion within a Ribbon-layout ribbon, the vertical line's center axis
  lies halfway across the gap between the preceding item's physical-right edge and the
  following item's physical-left edge, not halfway between the item centers. With the
  contracted `8dp` entry spacing, its center axis is `4dp` from each adjacent item edge.
  The same order boundary always uses this same line position whether reached from the
  preceding item's end half, the gap, or the following item's start half. Before the first
  application, the center axis lies on that application's physical-left edge. After the
  last application, it lies at the center of the gap before the trailing add-favorite
  entry; the entry itself remains an invalid insertion destination. The line retains the
  shared thickness and color and spans the ribbon entry's full measured height before
  viewport clipping. It moves with its content boundary during scrolling and is clipped
  to the visible ribbon viewport; it never snaps to a viewport edge or substitutes the
  first or last visible item for the actual first or last application.
- Flow-layout ribbons use the multi-column vertical-module application insertion geometry
  and the Home main-list vertical edge auto-scroll rules. They do not use horizontal
  overflow feedback or a horizontal scroll position.
- Module and application movement use the same resource-backed edge auto-scroll values. Each applicable start/end or top/bottom edge band is `48dp`, capped at half of the viewport's corresponding dimension when that dimension is less than `96dp`. Continuous residence in one eligible band for `120ms` activates scrolling. Speed then increases linearly from `0dp/s` at the band's inner boundary to exactly `180dp/s` at the viewport's outer edge.
- Application movement renders no source placeholder, outline, empty slot, or source-specific reserved spacing. Remaining applications and the trailing add-favorite entry reflow with their existing size, name placement, items-per-row, and spacing values; the module does not freeze its pre-lift content extent. A source module temporarily left without an application measures from its existing add entry using the ordinary module layout, without an added empty-state illustration or reserved application area. The lifted preview retains its pre-lift dimensions, source presentation, and original pointer offset at `100%` opacity and `1.0` scale until a valid cross-module release saves successfully, adding only the same `12dp` elevation-style shadow used by module movement. The destination uses the insertion line defined above as its only candidate emphasis; apart from closing the source gap, hovering does not insert a preview slot, repeatedly reorder the list, or add a whole-module highlight.
- On recognition of an application lift, the preview takes over in the same display update that removes the source from layout; there is no simultaneous source fade-out. Remaining entries and affected modules smoothly close the gap using the content-transition rule. The preview follows the owning pointer immediately without a position tween, delayed catch-up, fade, or scale change. Closing the source gap does not authorize repeated hover-driven list rearrangement.
- On release, the application preview stays at its release position while the save is unresolved. Success removes that preview in the same display update that presents the application at its saved destination, using the destination's ordinary presentation without an additional enter fade. Cancellation, invalid or unchanged release, and save failure instead hand off to the applicable restored source; surrounding entries may smoothly return to their restored positions. No preview-to-destination flight animation is required, and preview and real application must not be displayed simultaneously. A module created by dropping an application appears without a module-level fade so that its moved application is not indirectly faded; other affected modules may reposition normally.
- While a movement pointer occupies an eligible edge-auto-scroll band, the applicable edge displays one inward `48dp` gradient from transparent to at most `8%` `primaryTextColor` at the outer edge. It is directional, non-layout feedback; it appears during the activation delay, remains while that edge request is eligible or active, and disappears immediately when eligibility ends. It does not replace the insertion line or add another haptic.

## Home bottom app bar

Behavior belongs to [home-bottom-app-bar.md](../features/home-bottom-app-bar.md). This section owns only the accepted geometry.

- The bar measures from the Home safe edges. It does not sit inside the favorite main list's `8dp` content padding. Its start and end insets are `16dp`. The gap between adjacent icon bounds is `16dp`. Each icon has `8dp` above and below it. The full height is therefore the selected icon tier plus `16dp`: `64dp`, `56dp`, or `48dp` for Large, Medium, or Small.
- Fewer favorites than capacity stay centered between the two `16dp` insets. The `16dp` gap does not grow to fill the row.
- A fixed bar, and a semi-hidden bar while expanded or collapsed, keep that full height. Expanding and collapsing do not change it. The favorite main list keeps its own `8dp` spacing above the bar. That spacing is not part of the bar's capacity.
- While semi-hidden and collapsed, the bar moves down along the Y axis until it meets the bottom and exposes `16dp` of its full height. The remainder appears cut off by the bottom of the screen. Large, Medium, and Small all expose the same `16dp`. Icons remain in their full-height positions and are not recentered in the exposed area. The collapsed bar uses `80%` opacity. Expanded and fixed bars use full opacity.
- Expanding moves the semi-hidden bar up along the Y axis to its fully visible position. This is a position change, not a height change, so the favorite main list's height and logical scroll position stay unchanged. A fixed bar stays fully visible: it does not move or become cut off by the screen bottom. The favorite main list still reserves the fixed bar's full height.
- The bar background is one white layer at `24%` opacity, with a `16dp` corner radius and `16dp` outer margins on the start, end, and bottom. Its border reuses the favorite-bar border: `1dp` at `12%` opacity. Icons on it do not use the Home glyph shadow.
- The edit-mode add control uses the same icon-tier slot and `16dp` gap as a favorite. It shows the shared `24dp` plus artwork centered in that slot and is not a separate control size.
