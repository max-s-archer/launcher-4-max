# Home Interaction Specification

> Public semantic source: English. Chinese counterpart: [home.zh-CN.md](home.zh-CN.md). Shared navigation is defined in [navigation.md](../navigation.md); exact visual values are defined in the [Home presentation specification](../presentation/home.md).

## Purpose and structure

Home is one fixed, non-pageable, non-collapsible Launcher surface. In normal Home and
collapsed-panel editing, a fixed basic-information region shows time, date, and weekday;
expanding the inline style settings panel preserves that region. One full-width favorite main
list occupies the remaining content area and scrolls vertically. A dismissible default-Launcher
prompt, defined below, occupies its own region between the basic-information region and that
list, and it is present only while Launcher4Max is not the device's default Launcher.

The main list is one ordered, heterogeneous sequence of peer modules. A module is either a vertical favorite module or a horizontal favorite ribbon. Both occupy the complete Home content width and may be added, repeated, and ordered without a product-defined module-count limit. Home has no primary, secondary, side-by-side, folder, or page-based favorite regions.

Only the favorite main list scrolls vertically. A vertical module expands naturally and has
no independent vertical scroll position. A horizontal favorite ribbon uses either the
one-row Ribbon layout, which may scroll horizontally on overflow, or the multi-row Flow
layout, which wraps within the available module width and does not scroll horizontally.
Remaining space is transparent Home background, not another module.

## Time, date, and weekday

- Time follows the system 12-hour or 24-hour preference and does not show seconds. Date and weekday follow the active locale.
- Selecting time opens an exposed system Clock main surface, then falls back to the system alarm destination. Selecting date invokes an implicit Calendar destination. An unavailable destination produces localized non-blocking feedback.
- The eligible blank space of the full-width information region hosts the two configurable quick-action slots defined in [quick-actions.md](../features/quick-actions.md); a slot bound to `No action` produces no product behavior. Interactive targets remain excluded.

## Default-launcher prompt

- When Launcher4Max is not the device's current default Launcher, Home presents one dismissible prompt between the basic-information region and the favorite main list. The prompt is a bounded Home element rather than a module: it never joins the favorite main list, never holds a favorite, and is absent while Launcher4Max is the default Launcher.
- The prompt is presented in normal Home only. It is hidden for the complete duration of edit mode in both panel states, and it never appears on Drawer or in Settings.
- It shows the title `Not set as the default launcher yet` and the supporting line `Set as default to return here`, followed by one non-interactive trailing arrow hint and one dismiss control. The arrow is a hint only: it has no independent interaction target and no separate accessibility name, and activating it belongs to the prompt as a whole. The dismiss control is its own target and carries the localized accessibility name `Dismiss`.
- Selecting the prompt anywhere outside the dismiss control opens the same system destination with the same result as the `Default home application` entry in [Settings](settings.md). That selection changes no Launcher4Max setting and no default-Launcher state by itself.
- Selecting the dismiss control closes the prompt immediately and performs no other action: it neither opens the system destination nor changes the default-Launcher state.
- Visibility is evaluated once when Home enters the foreground, including at cold start, from the then-current default-Launcher state. The prompt is not inserted later within the same foreground session, so losing the default-Launcher role while Home stays visible does not add it. It is removed immediately when Launcher4Max becomes the default Launcher, including while Home remains visible after returning from the system destination.
- A dismissal is remembered for the remainder of the device's current local day, during which the prompt is not presented again. The automatic removal that follows becoming the default Launcher records no dismissal, so losing the default-Launcher role later in that same local day presents the prompt again.
- No reminder-attempt limit and no permanent "do not show again" state exist; only the current local day is suppressed.
- The prompt changes no favorite, module, Drawer display setting, quick-action binding, or default-Launcher state, and its removal preserves the favorite main list's logical scroll position. The resulting content shift is presented as one layout change rather than a per-entry animation. Entering edit mode removes the prompt and leaving edit mode restores it as part of that same single layout change, subject to the visibility already established for the current foreground session.
- Home presents no second default-Launcher prompt. The Settings `Default home application` state text and the [application action sheet](app-action-sheet.md) informational line remain independent of this prompt: neither derives from it nor controls it.

## Favorite identity and module lifecycle

- A new installation or the one-time adoption of this Home model starts with an empty favorite main list. The former vertical-list and favorite-bar model is not migrated, retained, or run in parallel. This author-accepted favorite reset does not clear Drawer display settings or unrelated local configuration.
- Every favorite belongs to exactly one persisted destination: one favorite module or the Home bottom app bar defined by [home-bottom-app-bar.md](../features/home-bottom-app-bar.md). The same stable launchable identity cannot appear more than once across Home. Primary, cloned, and work-profile entries remain distinct identities. The bar is a favorite position, not a module, and it does not create a second favorite.
- Every module persists one stable one-dimensional application order. A vertical module wraps that order from start to end and then top to bottom according to its current items-per-row value; a ribbon presents the same order from start to end. Changing application size, name placement, or items per row changes presentation only and never rewrites the persisted application order.
- Every persisted module contains at least one favorite. A provisional empty module is persisted only after its first valid application is added and is otherwise discarded. Losing the final favorite deletes a persisted module.
- Each horizontal favorite ribbon persists one display mode: Ribbon or Flow. Existing
  ribbons retain Ribbon when this capability becomes available, and a newly created
  horizontal favorite module defaults to Ribbon. Changing the display mode changes
  presentation only; it does not change module membership, application order, or module
  order.
- There is no artificial total or per-type module limit. Available unique launchable identities form the natural capacity boundary. When all are favorited, no add flow may claim another module can be completed.
- Names, icons, badges, and launchability resolve from the latest reliable Android inventory; they are not per-favorite visual overrides.
- In normal Home mode, selecting an application in either module type launches it.
  Long-pressing a fully presented favorite produces the platform-standard long-press haptic
  and opens the application action sheet. Movement after that haptic does not reorder the
  favorite or enter one-shot operation.
- Home uses one surface-wide `600ms` rapid-activation window for ordinary application launch targets. The first accepted activation suppresses every later Home application activation until that window expires, regardless of identity.
- If an application launch handoff fails, Home keeps its current main-list position and
  any Ribbon-layout ribbon positions, shows the localized short Toast
  `Unable to open application`, and requests one non-blocking inventory refresh. A
  Flow-layout ribbon has no independent horizontal position. The failure itself is not
  evidence of uninstall, permanent disappearance, or favorite invalidity. A failed
  refresh adds no second Toast or blocking state; later reliable inventory is reconciled
  through the ordinary rules below.

## Vertical favorite modules

- One module-level application icon size, application name (text) size, name placement, and items-per-row value applies to every entry. Individual entries cannot override the module style.
- A new module defaults to medium icon and text sizes, names on the right, and one item per row.
- The style vocabulary follows the applicable Drawer application-item subset but does not import search, anchors, AlphabetIndex, background, or Settings structure.
- All icon and text tier combinations use the same arrangement and transition rules as Drawer. Right-side names allow one or two items per row; below-icon names allow one through four; hidden names allow one through six. Decrement is disabled at one; increment is disabled at two for right-side names, four for below-icon names, and six for hidden names, and neither action crosses its current name-placement boundary. Count changes never change name placement. Switching from right-side to below-icon names preserves one or two and enables increment when the count is two. Switching from below-icon to right-side names preserves one or two but atomically clamps three or four to two and disables increment. Switching to hidden names preserves the current count of one through four and raises the maximum to six, enabling increment below six. Switching from hidden names to right-side names atomically clamps a count above two to two, and switching from hidden names to below-icon names atomically clamps a count above four to four. Hidden names hide only the displayed name; accessibility still announces the complete application name. The placement change and required clamp preview and save as one complete module-style change. Available width does not increase these bounds.

## Horizontal favorite ribbons

- A horizontal favorite ribbon is one full-width module whose display mode is either
  Ribbon or Flow. Both modes use the same favorite identities, stable order, and entry
  style.
- Ribbon mode presents one horizontal row of content-measured entries. It remains
  stationary when content fits and scrolls horizontally without looping on overflow.
- Flow mode presents the same entries in start-to-end order, wrapping them into additional
  rows within the available module width. It uses the Home main-list vertical scroll rather
  than an independent vertical or horizontal scroll region.
- The entry style remains fixed across both modes: medium application presentation, name
  on the right, and the presentation-defined entry width, maximum, ellipsis, background,
  boundary, and radius. Display mode, members, module order, module name, and name visibility
  are editable for a horizontal favorite ribbon.

## Loading, inventory changes, and failure

- Loading, unreadable, and valid-empty states apply once to the complete favorite main list. While Loading, the main list presents one list-level loading state — the shared progress indicator with the localized message `Loading favorites…` — instead of any per-entry loading indicator or label, and the basic-information region remains visible. A favorite's per-entry unavailable feedback is a separate contract concern and never doubles as a loading indicator.
- An unreadable persisted state preserves the unreadable data, disables favorite mutation, offers Retry, and remains distinct from valid empty data without blocking Drawer.
- A favorite is removed automatically only after a successful refresh confirms permanent disappearance. A disabled entry remains stored and provides localized unavailable feedback. An uncertain or failed read does not delete or hide favorites.
- A failed mutation restores the last reliably persisted complete favorite state, does not overwrite newer reliable inventory facts, and provides non-blocking feedback.

## Home states

Normal Home, edit mode, and one-shot operation present the same Home content. They differ only in the editing affordances that are visible and in when the state ends. This table is their comparison. The sections below own the detailed rules. The table adds no fourth state and no implementation structure.

| Comparison | Normal Home | Edit mode | One-shot operation |
| --- | --- | --- | --- |
| How it is entered | It is ordinary Home | The Edit Launcher action, or a quick-action slot bound to `Edit mode` | A recognized Drawer drag-to-favorite |
| How it ends | It remains until another state is entered | Back, after an open style panel closes, or an existing interruption. One completed movement does not end it | As soon as that one operation ends, including release, cancellation, Back, save failure, and interruption |
| Editing affordances | A module name title only when that module shows its name | Every editing affordance | Only the moving preview, one insertion candidate, and the two main-list add-favorite entries used as drop destinations |
| Add entries | Absent | Both main-list and trailing entries are present. A main-list entry can be selected and opens favorite multi-selection | The two main-list entries are drop destinations only and cannot be selected. Trailing entries and the bar add control stay absent |
| Bottom bar | A collapsed bar stays collapsed. An empty bar stays absent | While the feature is on, the bar is shown expanded, including when it is empty | A collapsed bar, and an empty bar while the feature is on, are temporary drop destinations and then return to their previous presentation |
| Selecting a favorite | Launches it | Does not launch it | Does not launch it |
| Long-pressing a favorite | Opens the action sheet. Later movement does not reorder it | Starts movement and remains in edit mode | The operation has already begun |

## Home edit mode

Edit mode may be entered from the existing favorite action or through a basic-information quick-action slot bound to `Edit mode` ([quick-actions.md](../features/quick-actions.md)); with the default bindings no blank-space gesture enters edit mode. A Drawer drag-to-favorite does not enter edit mode. It enters [one-shot operation](#one-shot-operation), defined below. Eligible blank space excludes the complete time, date-and-weekday, application, module-control, Loading, Error, and Retry targets, together with any later interactive target placed in that region. No blank space in the favorite main list or the remaining Home layout enters edit mode. When the long-press slot is bound to `Edit mode`, holding eligible space through the platform long-press threshold while remaining within platform movement tolerance produces one platform-standard haptic and enters edit mode; when the double-tap slot is bound to `Edit mode`, a recognized double tap produces one platform-standard haptic and enters edit mode. Shared gesture ownership and cancellation follow [Navigation](../navigation.md). Edit mode does not add a fixed edit dock. The basic-information region remains visible, and the main-list viewport occupies the remaining content height. Empty Home exposes only the two main-list add-favorite entries defined below; it has no module and therefore no style settings panel. Edit mode shows every editing affordance and leaves only through its existing Back and interruption rules. It does not end when one movement ends.

### Drag-to-favorite journeys

- A recognized Drawer drag-to-favorite enters one-shot operation. It does not enter edit mode. The main list keeps its logical scroll position, and no entry Toast is shown. The visible destinations and the immediate return to normal Home are defined by [one-shot operation](#one-shot-operation).
- The moving identity originates from the Drawer inventory, so it is omitted from no source order and no source module can be emptied by the journey.
- A completed drag-to-favorite invalidates the latest-removal Undo snapshot through the existing rule above.

### Add-favorite entries

- Edit mode appends two peer add-favorite entries after the last module inside the favorite main list. They are list content rather than modules: they scroll with the list, are absent in normal Home, and appear in edit mode whether the style settings panel is collapsed or expanded. They never persist, never count toward module order, and never hold a favorite.
- One entry creates a vertical favorite module and one creates a horizontal favorite ribbon. Their accepted labels are `Add favorite list` and `Add favorite ribbon` in English and `新增收藏列表` and `新增收藏织带` in Simplified Chinese. Each label is preceded by a shared plus icon that belongs to the same single interaction and accessibility target as its label.
- Selecting either entry opens Drawer favorite multi-selection with a provisional destination of that module type captured. Drawer selection remains transient and does not create or persist that module. Only a successful Confirm containing at least one valid identity atomically creates the provisional module with every remaining valid identity in displayed selection order, then appends it as the final module of the main list. Cancel, Back, valid downward dismissal, an empty result after confirmation revalidation, or save failure leaves module order and content unchanged.
- Every module additionally exposes one trailing add-favorite entry after its last application. Its accepted labels are `Add favorite` in English and `新增收藏` in Simplified Chinese. Selecting it opens Drawer favorite multi-selection with that persisted module captured as the destination, so the successfully confirmed applications append to the existing module instead of creating another one.
- Entry position alone resolves whether an add action adds to an existing module or creates a new one. No panel state, prior selection, or inferred destination participates in that resolution.
- Add-favorite entries remain visible and enabled without pre-counting unfavorited inventory. When every available identity is already assigned, Drawer naturally presents no selectable entry and its confirmation stays disabled; Home adds no capacity state, Toast, or redirection.
- While the style settings panel is open for one module, it blocks that module's trailing add-favorite entry together with that module's other application targets. Other modules and the two main-list add-favorite entries remain available.
- While an exclusive save of favorite or module-style state is unresolved, every add-favorite entry uses the shared disabled opacity and rejects activation, following the same rule as other blocked mutations.
- Add-favorite entries appearing on edit-mode entry, disappearing on edit-mode exit, or changing the main list's total length preserve the list's logical scroll position. Home neither resets that position nor automatically scrolls, centers, or reveals an add-favorite entry.
- No add-favorite entry is a valid application-movement destination for insertion. Each is excluded from order-boundary resolution and shows no candidate insertion line. The final module's end boundary therefore resolves at that module's own trailing edge, above the main-list add-favorite entries. A vertical module's application end boundary resolves at the start-side edge of its trailing add-favorite entry. A ribbon's application end boundary means appending after its last application, before its trailing add-favorite entry; its indicator uses the ribbon gap geometry defined in [Home presentation](../presentation/home.md#drag-and-overflow-feedback), not the entry's own edge. Adding an application to the end of an existing module and creating a new module for it therefore remain separate reachable outcomes.

### Module header

- Every favorite module, including a horizontal favorite ribbon, has a module header. Collapse and expand of a module are outside this behavior and are not implied by the header.
- Every module persists its own name and whether that name is shown. Name visibility defaults to hidden, including for modules that already exist when this behavior is adopted. An unnamed module uses the stable default label `Favorite list` for a vertical favorite module and `Favorite ribbon` for a horizontal favorite ribbon. The default label is not derived from an application's name.
- While the name is hidden, ordinary Home adds no row and no reserved blank space above the module content. Its spacing remains the current spacing. While the name is shown, ordinary Home adds one non-interactive title row above that module's content and shows the persisted name, or the default label when the module is unnamed. The title row launches nothing and does not enter edit mode.
- Edit mode always shows one operation header row above every module's content, whether or not that module's style settings panel is open. The row contains the module name, a visibility affordance, a style affordance, a whole-module unfavorite action, and a module-reorder handle. The name remains in the row when name visibility is hidden, so it can still be edited. The visibility affordance and style affordance do not appear in ordinary Home. Leaving edit mode removes the operation header row.
- The module name in the operation header is an editable `EditText` field. Only edit mode can
  change it. Ordinary Home never presents that field. Every valid text change previews immediately
  and starts an exclusive save of the complete module-name state; the name is not rolled back
  merely because editing ends. Pressing the keyboard Enter key confirms the current text, ends
  `EditText` editing, and hides the keyboard. System Back while the keyboard or `EditText` is
  active ends editing and hides the keyboard without cancelling or undoing an already persisted
  name. An empty committed name keeps the module unnamed and continues to use its default label.
  Tapping another module target, opening a style panel, or beginning movement first ends the
  current text editing and hides the keyboard; it does not revert the persisted name. A name-save
  failure restores the last successfully saved name, shows the localized short Toast
  `Unable to save module name`, keeps the current `EditText` active when it is still visible,
  and allows later text changes to retry; it does not reopen an editor that has already closed.
  External interruption ends text editing and hides the keyboard while the already saved name
  remains durable; a save already in progress follows its success or failure result. The text
  field does not start module movement, remove the module, change name visibility, or open the
  style settings panel.
- The visibility affordance uses an eye-like icon and toggles only that module's persisted name visibility. It does not rename the module. The operation header exposes no further action.
- The style affordance is the only way to open a module's style settings panel. It appears on every favorite module, including a horizontal favorite ribbon. Its artwork is selected from the existing image resources during implementation. Selecting it opens that module's panel directly and gives that module the selection outline. Selecting it again collapses the panel and clears the outline. Selecting another module's style affordance closes the current panel and opens the other module's panel in the same state change. At most one panel and one selection outline are visible. Tapping the module surface does not select the module or open the panel. Empty Home has no style affordance and no panel.
- The whole-module unfavorite action removes that complete module in one atomic mutation
  and does not leave an empty module. It shows one localized long-duration removal Snackbar,
  `Removed the whole favorite group`, with the action `Undo`. The Snackbar does not time out;
  it remains only while its snapshot is valid under the Undo lifecycle below and closes
  immediately when that snapshot becomes invalid. Undo restores the complete module once,
  including its name, name visibility, module type, style, main-list order, and contained
  favorites in their saved order. The existing short removal Snackbar for one application
  is unchanged.
- The module-reorder handle is the only way to reorder a module. Long-pressing the module surface does not start module movement. Reorder remains insertion-only and never exchanges two modules. The handle drag follows the module-movement rules already defined in this document, including edge auto-scroll and one atomic save of the resulting order.
- Exact header dimensions and order belong to [Home presentation](../presentation/home.md). The header row is `40dp` high, its visible content is `32dp` high, and each edit-mode icon target is `40dp x 40dp`. From the physical left, the order is whole-module unfavorite, name, visibility, flexible space, style, and reorder handle. Icon artwork is selected during implementation and is not a product-contract value. Header spacing is defined by Home presentation: `32dp` content centered in each `40dp` target, `4dp` to the target edge, and an `8dp` visual gap between adjacent contents.
- Home does not use an edit dock. The former dock instructions are not retained.

### Style settings panel and module movement

- The header style affordance opens the inline style settings panel inside that module, between its operation header and its content, in one atomic state change. The panel scrolls with the main list. It does not cover the list, use a Scrim, become modal, or remove the basic-information region. The main list remains vertically scrollable.
- The panel measures to its current content and grows the owning module by that height. It does not use a reserved minimum main-list viewport or scroll inside itself. The expand and collapse animation is purely presentational. The underlying state change commits immediately, and an interruption—system Back, leaving Home, or process interruption—resolves to the final state without waiting for the animation. Activating the same affordance while an animation is in flight retargets it to the new state instead of being ignored or queued. When the system removes animations, the final state displays immediately. During the animation, interaction targets are hit-tested against their current visible bounds. Exact duration and easing belong to the [Home presentation specification](../presentation/home.md).
- Opening or closing the panel preserves the main list's logical scroll position and module order. It neither resets nor automatically scrolls, centers, or reveals another module. The basic-information region remains visible.
- The open panel blocks application-level targets only inside its owning module, including that module's trailing add-favorite entry. Other modules retain application-level editing. The panel itself is not an application-movement destination.
- The open module shows the presentation-defined selection outline. Unselected modules show no corner marks and no selection layer. Tapping a module surface neither selects it nor opens its panel. Long-pressing the module surface does not begin module movement. Dragging the module-reorder handle does, and an open panel is not required. Before that drag, vertical movement remains main-list scrolling.
- Tapping the open module or inter-module blank space leaves the open panel unchanged. Dragging the reorder handle of another module starts movement without opening its panel or closing the current one; the open panel and movement are independent.
- Recognition lifts a complete non-interactive module preview that preserves the pointer's original offset within the module and immediately removes the source module from list layout without retaining a placeholder. Remaining modules close the vacated space. An open panel stays with its owning module.
- Module sorting is insertion-only and never exchanges two modules. The touch point controls the candidate boundary: entering the first half of a module shows its start-side insertion line; entering the second half shows its end-side insertion line. In an inter-module gap, the nearest adjacent boundary applies.
- Module movement uses the shared edge auto-scroll rules below and may scroll only the Home main-list viewport vertically. Release performs one atomic save of the final inserted order; cancellation restores the saved order.
- The open panel remains bound to the same module identity across a successful reorder. Moving that module therefore carries its panel and selection outline to its new position. Cancellation or save failure restores the prior order and preserves that same open panel; reliable removal of the open module clears the panel through the ordinary inventory rule.
- Save failure restores the last successfully saved complete module list, retains Home edit mode, and shows the localized short Toast `Unable to save module order`. The user may retry by dragging a module handle again; no Dialog or separate Retry action is added.
- There is no panel state without an owning module and no empty-selection prompt. The panel exposes no add action; adding favorites and creating modules belong to the add-favorite entries defined above.
- A vertical module's panel exposes application icon size, application name (text) size, name placement, and items per row directly, without a module-type row. While the module's name placement is `Hidden`, its text-size slider uses the shared disabled opacity and rejects activation, and another placement restores it with the persisted text tier; the icon-size slider is unaffected. The panel exposes no complete-module deletion action.
- A horizontal favorite ribbon uses the same header style affordance and the same panel container. Its panel exposes the display-mode choice between Ribbon and Flow and shows no vertical-module controls. The accepted display-mode labels are `Ribbon` and `Flow` in English and `织带` and `流式布局` in Simplified Chinese. It shows no module-type row, application-size control, name-placement control, items-per-row control, or disabled placeholder for those unavailable controls. Switching the display mode follows the ordinary immediate-preview and complete module-style save rules.
- Every module persists its own name. In edit mode, the name is edited through the text field in that module's header.
- Every valid style change previews immediately and starts one exclusive save of the complete module-style state. While that save remains unresolved, style mutation, add or remove actions, Undo activation, opening or closing a panel, and application or module movement are unavailable; main-list scrolling, system Back, and external interruption remain available. The panel shows no Loading state or progress indicator, and no later style candidate or favorite mutation is queued, coalesced, or saved in parallel.
- Save success makes the preview durable and restores the blocked editing actions. Save failure restores the complete last successfully saved style and shows the localized short Toast `Unable to save module style`. An open panel refreshes in place and remains open when that module identity still exists; a panel closed while saving remains closed and is not reopened. A later valid change may retry normally.
- Selecting the same style affordance again, or system Back, collapses the panel and retains edit mode. Exiting edit mode clears the open panel.

### Adding, removing, and moving applications

- An add-favorite entry opens Drawer favorite multi-selection with the exact persisted or provisional destination captured. Already-favorited identities are unavailable. A provisional module is created only by a successful Confirm that retains at least one valid identity, and that same atomic save adds every retained identity in displayed selection order.
- Releasing a moving application onto a main-list add-favorite entry creates that entry's module type and adds the application as its first favorite in the same atomic mutation that removes the identity from its source order. The new module is appended as the final module of the main list. If that removal empties the source module, deletion of the source module belongs to the same mutation. This drop destination never duplicates or redirects an identity and never exposes an intermediate persisted state.
- A drop onto an add-favorite entry is a destination release rather than an order-boundary insertion. It shows the presentation-defined hover emphasis on that entry and never shows an insertion line. Releasing a moving application onto a trailing module add-favorite entry is not a valid destination; a module with remaining applications exposes its own end boundary separately. A temporarily application-empty source module follows the rule below.
- Application-level editing remains available on every module whose style settings panel is closed. An open panel blocks it only inside that module. Selecting an application item performs no action. Each item exposes one separate remove target; the rest of its complete item surface is the application-movement target, with no visible reorder handle or reserved handle space.
- Selecting the remove target removes that favorite through the existing atomic mutation and shows the localized removal Snackbar `Removed from favorites` with the action `Undo`. The target consumes its complete pointer sequence and cannot launch the application or begin movement. Removing a favorite through the Home application action sheet uses the same lifecycle.
- Application reorder is insertion-only; it never switches to a separate exchange operation. One movement means removing the source identity from its saved order and inserting it at one target-order boundary, so intervening identities shift naturally.
- Long-pressing the non-remove portion of the complete application item to the platform
  threshold produces one platform-standard long-press haptic and gives that active pointer
  exclusive ownership of application movement. Prior to recognition, vertical intent remains
  main-list scrolling. Horizontal intent within a Ribbon-layout ribbon remains Ribbon-layout
  scrolling; a Flow-layout ribbon has no horizontal scrolling. Movement that cancels
  platform long-press recognition does not begin application movement. After recognition,
  the same sequence cannot select the application, open its action sheet, open Drawer, or
  start module movement. Candidate-boundary changes produce no repeated haptic.
- A one-item-per-row vertical module divides a candidate item's bounds into top and bottom halves: the top half selects the boundary before the item and the bottom half selects the boundary after it. A multi-item row divides the candidate item's bounds into start-side and end-side halves: the start half selects before and the end half selects after. Current English and Simplified Chinese layouts resolve start to physical left and use left-to-right, then top-to-bottom reading order.
- In a partially filled final row, space after the last real item selects the boundary after that item; it never creates a candidate empty identity or artificial slot. In a gap between two rows, the nearer preceding-row end or following-row start boundary applies, with an exact tie resolving to the following-row start. The trailing add-favorite entry occupies the position after the last real item and is excluded from this resolution.
- A Ribbon-layout ribbon uses the same start-half-before and end-half-after rule. The
  preceding application's end half, the gap between adjacent applications, and the
  following application's start half all select the same order boundary between those
  applications. Switching between these hit regions alone must not relocate the insertion
  line to another item edge. Space before the first application or after the last
  application selects the module start or end boundary respectively, excluding its
  trailing add-favorite entry. The gap before that entry selects the end boundary;
  releasing on the entry itself remains invalid.
- A Flow-layout ribbon uses the same logical order and candidate-boundary rules as a
  multi-item vertical module. Its wrapped rows remain part of the Home main list, and it
  has no horizontal scrolling.
- Entering another module resolves a candidate through that destination module's own rules. The single insertion line appears within that destination geometry and is the only visual identification of both the target module and order boundary. In a gap between modules, the nearest preceding-module end or following-module start boundary applies, with an exact tie resolving to the following-module start. The basic-information region, open style settings panel, system-protected area, every add-favorite entry, and every region without candidate feedback are invalid insertion destinations. A main-list add-favorite entry remains a valid drop destination under its own rule above.
- At long-press recognition, the application is lifted into its preview and immediately
  omitted from the source layout without a placeholder. Remaining applications and the
  trailing add-favorite entry close the gap using the module's existing layout rules.
  This applies to single-column, multi-column, Ribbon-layout, and Flow-layout sources.
  It is a transient presentation change only: the saved favorite identities, application
  orders, module order, and module existence remain unchanged. Lifting is not removal
  from favorites and creates neither a removal Snackbar nor an Undo snapshot.
- All insertion candidates use the logical order with the moving source omitted. First and
  last applications, row wrapping, and neighboring gaps refer to those remaining
  applications, not an invisible source slot. During reflow or scrolling, hit testing and
  insertion feedback use the same current visible bounds, not pre-lift or final
  animation-target coordinates. Switching between equivalent hit regions within one
  ribbon alone does not move its line; the line follows its boundary's visual movement.
  Hovering shows one candidate insertion indicator without inserting the preview into the
  destination, repeatedly reordering the list, or saving a provisional result. A
  same-module release inserts the source once into the reduced order; a cross-module
  release inserts it once into the destination order. The lifted preview retains source
  presentation until handoff to the destination after a successful save.
- If the lifted application was its module's only application, the source module remains at its
  existing main-list order and retains its pre-operation module extent until the one-shot operation
  ends. The operation hides the module header and trailing add-favorite entry, retains no
  application slot or source placeholder, and creates no empty-module drop target. This
  temporarily application-empty module exposes no application insertion boundary, including a
  synthetic start or end boundary. If a module still has applications after the source leaves,
  its end boundary is resolved from the last remaining application's current visible geometry;
  it does not depend on the hidden trailing add-favorite entry. Cancelling the operation or
  releasing in an invalid region restores the application. Only a successful move to another
  module or a newly created module may remove this source module as part of the same atomic save.
  This transient display never persists an empty module or activates the ordinary empty-Home state.
- A valid release that changes the semantic order immediately starts one exclusive atomic save of the resolved destination and order, without waiting for layout animation. Subsequent visual movement cannot change that released destination. Movement within one module rewrites only that module's application order. Cross-module movement removes the identity from the source and inserts it into the explicit destination in the same mutation; it never duplicates, redirects, or exposes an intermediate persisted state. If the move removes the source module's final application, deletion of that source module belongs to the same atomic mutation.
- A valid release whose resulting application order equals the pre-movement saved order is a no-change completion: restore the source in that order and clear movement without saving, additional result messaging, or another haptic. Necessary visual restoration remains allowed. The comparison is by identity and order, not by the source's former screen coordinates. Releasing in an invalid region or receiving pointer cancellation restores the pre-movement saved arrangement without presenting a save failure.
- While the atomic save is unresolved, another application or module movement cannot begin. Save failure restores the last reliable complete favorite state, including a source module provisionally emptied by the failed move, without overwriting newer reliable inventory facts; Home stays in edit mode and provides non-blocking feedback.
- For an application move whose save remains unresolved while Home is visible, the preview stays at its release position. Success replaces the preview with the saved destination application in the same display update. Cancellation, invalid or unchanged release, and failure instead replace it with the applicable restored source; reliable inventory reconciliation still takes precedence when that identity no longer exists. The preview and real application never coexist visually. Leaving Home removes transient feedback rather than preserving a preview across navigation or process restoration.
- System Back during active movement cancels that movement and is consumed without also exiting edit mode. The Android system Home action, application backgrounding, lifecycle interruption, or process loss cancels the unpublished movement and then follows the existing navigation or restoration rule from the last saved state.
- Additional pointers never take ownership, change the candidate, or move the preview. Release or cancellation of the owning pointer alone completes or cancels the movement.
- Inventory changes do not turn an uncertain identity into a deletion while movement is active. If a reliable refresh confirms that the moving source identity permanently disappeared, Home cancels movement and then applies the existing confirmed-disappearance reconciliation rule.
- Application movement uses the shared edge auto-scroll rules below. The Home main-list
  viewport may scroll only vertically. A Ribbon-layout ribbon may scroll only horizontally
  when it is the active movement container; a Flow-layout ribbon uses the main-list
  vertical viewport. Candidate destinations, edge feedback, and application feedback must
  remain distinguishable from an open module panel and module-movement feedback.
- Removing a module's final application deletes that now-empty module as part of the same application-removal mutation because Home does not persist empty modules. That automatic deletion is not the whole-module unfavorite action and does not by itself create the whole-module Undo snapshot.
- An explicit user-initiated application removal and an explicit whole-module unfavorite are both Undo-eligible. Application movement, module reordering, and reliable-inventory automatic cleanup do not create an Undo snapshot.
- Only the latest eligible removal snapshot exists. A later explicit removal, of one application
  or of a whole module, replaces it. For an application-removal snapshot, Snackbar timeout or
  dismissal, another successfully committed favorite mutation, exiting edit mode, entering
  Drawer favorite multi-selection, the Android system Home action, an external interruption,
  or process recreation invalidates it.
- For a whole-module snapshot, the long-duration Snackbar does not time out. User dismissal,
  a later explicit removal, leaving Home, or process recreation invalidates it. Exiting edit
  mode and entering or returning from Drawer favorite multi-selection do not themselves
  invalidate it; that multi-selection journey is an exception to leaving Home.
  Any subsequent successfully saved addition of a favorite invalidates the whole snapshot,
  whether or not the added identity was in it and whether the destination is a module or the
  Home bottom app bar. Any subsequent change affecting the snapshot's stored module data or
  contained favorite identities also invalidates the whole snapshot. Invalidation closes its
  Snackbar immediately and removes Undo; it never edits the snapshot into a partial restore.
  Merely entering multi-selection, changing unconfirmed selections, cancelling, or failing to
  save an addition does not count as a successful addition. Other changes that neither add a
  favorite nor affect the snapshot's contents do not invalidate it.
- An ordinary inventory refresh does not itself invalidate either snapshot. A reliable result
  confirming that any removed identity permanently disappeared invalidates its snapshot;
  for a whole-module snapshot, the complete snapshot and its Snackbar are discarded together.
- Undo of an application removal restores that application to its original module and order
  position and restores the module type and style needed when the removal deleted its final
  item. Undo of a still-valid whole-module snapshot restores the complete module once,
  including its name, name visibility, module type, style, main-list order, and contained
  favorites in their saved order. An invalid whole-module snapshot offers no Undo and is
  never partially restored. A reliably disabled identity remains eligible for restoration;
  a reliably permanently absent identity invalidates the snapshot as specified above.
  Undo save failure keeps the currently durable state, shows the localized short Toast
  `Unable to restore favorite`, and does not retry automatically or add another recovery surface.
- [Content transitions](../presentation/home.md#content-transitions) never determine mutation success, delay an otherwise valid save or failure recovery, or change the Undo snapshot and expiry rules. Once the existing save rules permit another action, an unfinished visual transition adds no input lock. Interrupting or disabling an animation must yield the same reliable state; first display, return navigation, and scrolling do not create favorite mutations or Undo opportunities.

### Edge auto-scroll during movement

- Module and application movement share the edge-band size, activation delay, maximum speed, and linear speed curve defined by the [Home presentation specification](../presentation/home.md). No platform default or earlier implementation value substitutes for that contract.
- The main-list viewport owns vertical edge scrolling. A ribbon owns horizontal edge scrolling only while it is the active application-movement container. Module movement therefore uses only main-list vertical scrolling; application movement uses the axis of its current active main-list or ribbon container. At most one container and one axis may auto-scroll at a time.
- Auto-scroll begins only after the owning pointer remains continuously inside the same container, axis, and direction's edge band for the full activation delay. Moving within that band adjusts speed from pointer proximity without restarting the delay. Leaving the band, changing the active container, axis, or direction, or becoming ineligible stops the current request; a newly eligible request must satisfy the complete delay again.
- While active, speed increases linearly from zero at the edge band's inner boundary to the presentation-defined maximum at the viewport's outer edge. Edge scrolling changes only viewport position; the insertion candidate is recomputed against the resulting geometry and is never committed by scrolling alone.
- Auto-scroll stops immediately when the owning pointer is released or cancelled, movement ends or is interrupted, the relevant content boundary is reached, or the active viewport cannot consume further displacement. Edge scrolling adds no haptic beyond the one long-press haptic that began movement.

### Inventory changes during edit mode

- Installing or removing an unfavorited application does not end edit mode. A favorite name, icon, or badge change updates in place.
- Confirmed removal of a favorite identity, inability to confirm its identity, or full inventory failure ends edit mode, cancels unfinished movement, preserves already saved changes, and follows the loading and failure rules above.
- A disabled favorite remains saved in its original module and position, does not end edit mode, and may still be explicitly removed or moved. Selecting it in normal Home does not launch it and instead shows the localized short Toast `Unable to open application`. Re-enabling it restores normal presentation and launch behavior in place. Confirmed disappearance removes only that identity; an uncertain identity or failed inventory read remains saved.

## One-shot operation

The comparison of normal Home, edit mode, and one-shot operation is defined by [Home states](#home-states). One-shot operation is not edit mode. The Edit action on the application action sheet and a quick-action slot bound to `Edit mode` still enter edit mode. Movement that has already begun inside edit mode stays in edit mode.

One-shot operation has one entry. It begins when Drawer recognizes
[drag-to-favorite](drawer.md#drag-to-favorite). A long press on a fully presented favorite in
normal Home opens the application action sheet and cannot begin this operation.

- A favorite in normal Home, including one in a module or on the Home bottom app bar, cannot
  begin reordering. Movement before or after its long-press haptic follows the existing list
  and Ribbon scrolling rules. It never lifts the favorite or opens one-shot operation.
- The moving item lifts into a preview that keeps its source presentation. A Home favorite leaves its source layout, and the remaining favorites close the gap through the existing arrangement rules. A Drawer item keeps its source presentation until handoff. The operation shows only that preview and one insertion candidate. It does not show the edit-region background, a module header, a remove control, a trailing add-favorite entry, the bar's add control, a style affordance, or a module-reorder handle.
- While the operation is active, the two main-list add-favorite entries appear after the last module as drop destinations. They disappear when the operation ends. They are not selectable and do not open Drawer favorite multi-selection. A release on one creates that entry's module type with the moving item as its first favorite, in one atomic save. If reordering empties the source module, removing that module belongs to the same save.
- While the operation is active, a collapsed semi-hidden bar expands temporarily, and an empty
  bar is shown temporarily while the feature is on. Both are drop destinations. After a
  successful save, the bar's ordinary presentation is recomputed from its final membership and
  fixed-or-semi-hidden setting; it is not forced back to a previously empty or hidden state.
  After cancellation, an invalid release, an unchanged release, or a failed save, the bar
  returns to the presentation it had before the operation. While the feature is off, the empty
  bar stays absent. A full bar rejects a release that would add a member without displacing a
  saved favorite or changing the icon tier; internal reordering remains valid.
- Placement uses the existing insertion rules, not an exchange of two favorites. Shared edge auto-scroll is available only while the operation is active. Selecting a favorite during the operation does not launch it.
- A release that changes the saved favorite state commits one atomic save and returns to normal Home.
  It adds no success feedback and creates no Undo snapshot. An unchanged release, an invalid
  release, pointer cancellation, and system Back restore the applicable saved state and return
  to normal Home. They do not return to Drawer.
- Save failure restores the last reliable complete favorite state, shows the localized short Toast `Unable to save favorite`, and returns to normal Home. It does not enter edit mode. The Android system Home action and an external interruption cancel the unpublished operation and reveal normal Home.
- After the operation begins, further movement neither scrolls a list nor starts the Home-to-Drawer transition. One-shot operation cannot be entered from inside edit mode, and edit mode is not entered from it.

## Home–Drawer gesture arbitration

- In normal mode, an upward gesture may begin on any Launcher4Max-managed Home element.
  Vertical movement gives the favorite main list first opportunity to scroll and suppresses
  the originating click or long press. After the long-press haptic on a fully presented
  favorite does not own one-shot operation or reorder that favorite.
- At the main-list end, remaining displacement from the same continuous upward gesture
  transfers to the Home-to-Drawer transition without a second gesture or discontinuity.
- Ribbon-layout horizontal scrolling wins only after horizontal intent is established.
  Edit mode does not open Drawer through this gesture.

## Acceptance intent

- Home presents one fixed information region and one full-width ordered main list
  containing any interleaving of vertical modules and ribbons. A ribbon may use either its
  one-row Ribbon layout or its wrapped Flow layout. The information region remains visible
  in ordinary Home and throughout edit mode.
- Modules have no artificial count limit, persisted empty modules do not exist, and every stable identity appears in at most one module.
- Only the main list scrolls vertically. Ribbon-mode ribbons alone scroll horizontally;
  Flow-mode ribbons wrap within the main list and do not introduce horizontal scrolling.
  One continuous upward gesture can proceed into Drawer at the main-list end.
- Vertical style is uniform per module; Ribbon and Flow layouts share one fixed entry
  style. Right-side names allow one or two items per row, below-icon names allow one
  through four, and hidden names allow one through six. Count changes never switch
  placement; changing placement preserves an in-range count and clamps when the destination
  boundary is lower: entering right-side names clamps three or four (or a higher count
  from hidden names) to two, and entering below-icon names clamps a count above four to
  four. Stepper availability updates to the resulting boundary.
- Home uses no edit dock. A header style affordance opens that module's style panel between the operation header and the module content. The panel scrolls with the main list, grows the module to its content, and leaves the basic-information region visible. At most one panel is open. It blocks application-level actions only inside its owning module. The open module shows a selection outline; other modules show no corner marks. Module reorder uses only the header handle. The panel exposes no add action and no empty-selection state.
- Edit mode appends two add-favorite entries after the last module and one trailing add-favorite entry inside every module. Entry position alone resolves creating a new module against adding to an existing one. Every entry is excluded from insertion-boundary resolution, so adding to a module's end and creating a new module remain separate reachable outcomes. Dropping a moving application on a main-list entry creates that module type with the application as its first favorite in one atomic mutation.
- An open panel remains bound to its module identity and changes only through that module's style affordance or another module's style affordance. Module-style persistence is serialized as one complete state; unresolved saves temporarily block mutation without blocking scrolling, and failure restores the last durable style.
- Application editing outside an open panel makes item selection inert, uses one separate remove target, and starts insertion movement by long-pressing the remainder of the complete item without a reorder handle. The open panel blocks those actions only inside its owning module. The same stable insertion-order model applies across single-column, multi-column, ribbon, and cross-module movement: the source leaves layout without a placeholder, candidates use the compacted arrangement, and only one candidate boundary appears. A temporarily application-empty source module retains only its existing add entry without becoming an insertion destination. Only a changed valid release commits; no-change, cancellation, interruption, and failure restore the applicable saved arrangement while preserving atomicity and identity uniqueness.
- In normal Home, one-shot operation uses that same insertion model without entering edit mode.
  It receives only a Drawer drag-to-favorite and cannot begin from a Home long press. During
  the operation it exposes only the two main-list add-favorite entries, a temporarily expanded
  collapsed bar, and a temporarily shown empty bar while the feature is on. It hides every other
  editing affordance, hides module headers and trailing module add entries, and resolves a
  remaining module's end boundary from its visible final application. A temporarily empty source
  module retains its pre-operation extent without becoming a destination or exposing an insertion
  boundary. A successful save recomputes the ordinary bottom-bar presentation from final state;
  every cancellation, invalid release, unchanged release, or failure restores the prior
  presentation. A full bar rejects only a release that would add a member, and internal
  reordering remains valid. The operation returns to normal Home after every outcome.
- Explicit application removal exposes only one transient latest-removal Undo snapshot,
  shared with a later whole-module unfavorite. An application removal restores the original
  module and position when still valid. A valid whole-module snapshot restores the complete
  module once. Unrelated edits, editing-journey exit, process recreation, or reliable permanent
  disappearance invalidate an application snapshot. A whole-module snapshot survives
  edit-mode exit and the Drawer multi-selection journey itself, but any subsequent successful
  favorite addition or change affecting its contents invalidates it in full and closes its
  Snackbar. Leaving Home outside that journey, process recreation, user dismissal, or a later
  explicit removal also invalidates it. Unconfirmed selection, cancellation, or a failed
  addition alone does not invalidate it. Re-adding any removed application therefore prevents
  undoing the old whole-module removal; no duplicate identity or partial restore is produced.
- Content transitions preserve atomic state and existing Undo timing. During gap closure, insertion hit testing and drawing agree on current visible geometry. Lifting, successful release, and recovery each show only one visual instance of the moving application; disabling animations changes neither action availability nor final state.
- Module and application movement share deterministic edge scrolling: only the active viewport's own axis moves, activation requires continuous residence through the defined delay, speed follows pointer proximity, and every exit, ownership change, boundary, completion, or interruption stops the current request without another haptic.
- The new model starts empty without migrating former Home favorites while retaining unrelated local settings.
- Home presents one dismissible default-Launcher prompt between the basic-information region and the favorite main list while Launcher4Max is not the device's default Launcher, never during edit mode. Selecting the prompt opens the same system destination as the Settings default-home entry and changes no Launcher4Max state; selecting its dismiss control closes it for the remainder of that local day, and becoming the default Launcher removes it immediately without recording a dismissal.
