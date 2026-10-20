# Navigation and Surface Model

> Public semantic source: English. Chinese counterpart: [navigation.zh-CN.md](navigation.zh-CN.md).

## Responsibility

This document defines the current relationship, entry, exit, restoration, and shared transition behavior of Home, Drawer, Settings, and the application action sheet. It does not prescribe whether Home and Drawer are implemented as separate composable destinations or as coordinated regions of one implementation surface.

## Initial surface

- Home is the initial surface whenever the Launcher starts or restores after process recreation.
- Home and Drawer are distinct product surfaces even if implementation later uses one draggable container.
- Settings is reachable only through ordinary Drawer's Settings item: in the final section
  during Content or below Retry during Error, as defined by [Drawer behavior](surfaces/drawer.md).
  The AlphabetIndex gear navigates to the Content section and does not open Settings directly.
- Home has no Settings entry.

## Transition model

Home and Drawer use one reversible direct-manipulation transition. The following terms define its observable behavior:

- **Gesture displacement:** The accumulated drag distance in the direction of the target surface. Opposite movement reduces the accumulated distance toward zero.
- **Gesture progress:** Gesture displacement divided by 200dp, clamped to the range from 0 to 1. The 200dp value is the current full gesture distance and may be reconsidered through a later product change.
- **Interactive Drawer displacement:** While the pointer remains down, Drawer moves 1.5dp for each 1dp of gesture displacement. This 1:1.5 mapping applies in both directions.
- **Surface travel:** Drawer travels between its off-screen position below the viewport and its final position at the top of the viewport. The 200dp gesture distance does not mean that the viewport is 300dp high.
- **Settle animation:** After the release decision, the system animates the remaining screen distance from the current interactive position to the selected endpoint. The user is never required to drag Drawer across the full physical screen height.

### Opacity

Opacity is derived from gesture progress, not from Drawer screen-travel percentage. For gesture progress `p`:

- Home content opacity is `max(0, 1 - 2p)`.
- Drawer content opacity is `min(1, 2p)`.
- At 0% progress, Home is fully visible and Drawer is transparent.
- At 25% progress, both surfaces have 50% content opacity.
- At 50% progress, Home is transparent and Drawer is fully visible.
- From 50% through 100%, Home remains transparent and Drawer remains fully visible while position continues to change.

The reverse transition uses the same values in reverse. Rebound and completion animations continue from the current position and opacity without a jump.

### Release decision

- Reaching or exceeding 60% gesture progress, equivalent to 120dp, completes the transition to the target surface.
- Releasing below 60% returns to the origin surface unless the release constitutes a fling in the target direction.
- A target-directed fling completes the transition even below 60%. The velocity threshold and detector are implementation details that must preserve this observable result.
- A fling in the opposite direction does not complete toward the target; it returns toward the origin.
- Cancellation or system interruption returns to the origin through the same position and opacity path.
- Additional pointers have no product action. They must not cause a position jump, reverse the transition, activate an application, or create a second simultaneous transition.

## Home to Drawer

- In normal Home mode, an upward drag is the only user-gesture entry into ordinary Drawer. A Home edit-mode add-favorite entry may programmatically complete the same upward transition into Drawer favorite multi-selection mode; this does not enable Home-to-Drawer dragging during edit mode. A tap or short upward drag that begins on the exposed part of the collapsed Home bottom app bar expands that bar instead, as defined by [home-bottom-app-bar.md](features/home-bottom-app-bar.md). It does not launch an application or start this transition. After the long-press haptic on a fully presented favorite, movement beyond the platform touch slop owns [one-shot operation](surfaces/home.md#one-shot-operation) and neither scrolls the list nor starts this transition.
- In normal Home mode, the drag may begin anywhere inside Launcher4Max's Home interaction area, including over the time, date, favorite entries, empty state, loading state, error state, and Retry control. System-reserved inset gestures remain governed by Android.
- Vertical upward movement first scrolls the favorite main list toward its end. If the main list cannot consume displacement, its remaining displacement transfers from the same continuous gesture into the Home-to-Drawer transition without requiring the user to lift. Transition progress begins from the transferred displacement, not distance already consumed by list scrolling.
- Downward dragging scrolls the favorite main list toward its start and does not initiate
  a Home surface transition. A Ribbon-layout favorite ribbon consumes a gesture only
  after horizontal intent is established; a Flow-layout ribbon remains part of the
  main-list vertical scroll.
- Home-to-Drawer dragging is disabled while Home edit mode is active. A whole-application-item long press recognized in a module whose style panel is closed owns application movement. A module-reorder handle drag owns module movement whether or not a panel is open. Other vertical drags scroll the favorite main list.
- A pointer sequence can resolve to only one Home action. Before an interactive element's selection or long-press action has been committed, recognition of an upward drag transfers ownership to the Home-to-Drawer transition and suppresses that element's selection, long-press, and haptic response.
- In eligible basic-information blank space, a double tap or a long press resolves to the action bound to the corresponding quick-action slot defined in [quick-actions.md](features/quick-actions.md); a slot bound to `No action` resolves to nothing. Any drag that takes transition ownership cancels that recognition. Time, date-and-weekday, favorites, and other interactive targets never participate in bound blank-space gestures.
- Selection is committed only when the pointer is released without the sequence becoming an upward drag or a long-press. Once a long-press has been recognized and its action has opened, the same pointer sequence does not also begin the Drawer transition.
- A cancelled sequence performs no selection or long-press action and follows the transition cancellation behavior when a Home-to-Drawer drag had already taken ownership.

## Drawer to Home

- System Back returns Drawer to Home using the completion path of the reverse transition.
- Drawer's ordinary-mode top-app-bar Back arrow returns to Home through that same completion path.
- Downward dragging first scrolls the application list toward its top boundary.
- While list scroll offset is greater than zero, the list exclusively consumes downward displacement and Drawer does not begin to leave.
- When the list reaches offset zero during the same continuous gesture, remaining downward displacement transfers immediately into the Drawer-to-Home transition; the user does not need to lift and drag again.
- Once the surface transition owns the remaining gesture, the list stays at its top boundary until the transition settles.

## Back and restoration

- On Home, Back is resolved by the most specific active Home state before the ordinary Home result:
  active application or module movement cancels that movement and consumes Back; active module-name
  text editing ends the `EditText` session and hides the keyboard without reverting its already
  saved name; an open style settings panel closes while retaining edit mode; an expanded
  semi-hidden bottom app bar collapses only in normal Home when none of those states is active.
  After those state-specific results, Back has no visible effect when Launcher4Max is the system
  default Launcher. When Launcher4Max is not the default Launcher, Launcher4Max performs ordinary
  system Back behavior and does not prescribe the external destination.
- On Drawer, Back returns to Home.
- On Settings, Back returns to the previous Launcher4Max surface, currently Drawer.
- A Settings round trip within the same process preserves the originating Drawer Error state
  without retrying, as defined by [Drawer behavior](surfaces/drawer.md#empty-or-failed-result).
- The quick-action settings page opens only from its Settings item and is a Settings sub-page rather than a separate surface. On it, Back returns to Settings and preserves Settings' prior position; the page is not restored after process recreation, matching the other non-restored transient surfaces.
- On the application action sheet, Back dismisses the sheet and returns to the unchanged underlying surface.
- In Home edit mode, Back first closes an open style settings panel; otherwise it exits to normal Home. In Drawer favorite multi-selection mode, Back first cancels the complete unconfirmed selection and returns through the reverse transition to Home edit mode; it does not also exit edit mode.
- Process restoration begins at Home. Drawer, Settings, an action sheet, edit mode, and
  transient panel or selection state are not restored. Within the same process, ordinary
  Launcher4Max round trips preserve the favorite main list's meaningful vertical position
  and each Ribbon-layout ribbon's meaningful horizontal position. Process recreation
  resets them to their content starts.

## Home edit-mode interruption

- Home edit mode remains valid only while Home is the fully foreground, interactive Launcher4Max surface. A system or external interruption that covers Home input or moves interaction away from Home exits edit mode, including opening the notification shade or Quick Settings, showing Recents, locking or turning off the screen, opening another application, or presenting another system-owned surface.
- The interruption cancels any in-progress gesture or unpublished application or module movement, closes the style settings panel, and dismisses edit-only visual feedback. Successfully saved changes remain. An atomic mutation whose save already started follows its existing completion or failure rule, but neither result may restore edit mode. If Launcher4Max can observe the interruption result only when Home becomes interactive again, it must complete this reset before accepting new Home input. Dismissing the interruption therefore reveals normal Home, never the previous application- or module-level edit state.
- Launcher4Max-owned steps that belong to the same editing journey are not external interruptions: expanding or collapsing the inline style settings panel, showing an Launcher4Max confirmation or removal Snackbar, and entering or returning from Drawer favorite multi-selection follow their own documented state rules.

## Android system Home action and external application return

- When Launcher4Max is the selected default Launcher, the Android system Home action from
  any Launcher4Max surface, including Home edit mode, Drawer, Drawer favorite
  multi-selection mode, Settings, and the quick-action settings page, displays normal
  Launcher4Max Home. An active favorite multi-selection is cancelled and edit mode is
  exited. Within the same process, normal Home retains the latest meaningful main-list
  position and Ribbon-layout ribbon positions formed by successfully completed scrolling
  or editing; Flow-layout ribbons have no independent horizontal position. Cancelling
  an unfinished gesture does not create a new position. This is a system navigation
  result, not an Launcher4Max Back or Drawer-to-Home transition, and Launcher4Max does
  not reinterpret the Android system Home action as Back. Unqualified `Home` in this
  contract means the Launcher4Max product surface, never this platform action.
- Launching an ordinary application from Home or Drawer creates an external application excursion. When Launcher4Max returns in the same process, it displays Home rather than restoring Drawer, and it does not show Loading or require a new blocking initial read before showing the existing Home content.
- The same-process return preserves Home favorite state, the main list's meaningful
  vertical position, and each Ribbon-layout ribbon's meaningful horizontal position.
  Flow-layout ribbons have no independent horizontal position to restore. It does not
  restore Drawer, Settings, an action sheet, edit mode, or an in-progress Drawer position.
- Changes observed while the external application is active are reconciled without replacing the returning Home surface with Loading. The updated inventory and favorite state are available the next time Drawer or the relevant Home content is presented, subject to the live-update rules of those surfaces.
- If the Launcher process was recreated while the external application was active, the process-restoration rules apply instead of the same-process return rules.

## System surfaces

- Home and Drawer show the system status bar.
- Home preserves its transparent application surface. Drawer's application background follows its selected background mode and remains outside this navigation contract.
- Home and Drawer request transparent status-bar and navigation-bar regions and draw edge to edge beneath them. Launcher4Max does not add an independent opaque system-bar background; the applicable Home or Drawer surface treatment remains governed by its presentation specification.
- Launcher4Max leaves platform and device contrast enforcement at its default behavior. A system-provided translucent protection, including protection used by three-button navigation on applicable Android versions, is not replaced or forcibly disabled by Launcher4Max.
- The system navigation bar and controls follow the user's current navigation mode. Launcher4Max does not hide them or enter immersive mode.
- Home, Drawer, Settings, and every action sheet use the dark theme with light foreground elements. Opening a sheet must not switch system status-bar icons to a dark appearance.
- Content and interaction targets must respect system-bar, display-cutout, and gesture insets even though Home and Drawer draw edge to edge behind transparent system bars.

## Acceptance intent

- A 100dp drag produces 50% gesture progress and 150dp of interactive Drawer displacement before endpoint settling.
- Releasing at 119dp without a qualifying fling returns to the origin; releasing at 120dp or beyond completes to the target.
- A qualifying target-directed fling below 120dp completes to the target.
- Position and opacity remain continuous when the user reverses direction, releases, completes, or rebounds. Drawer-to-Home remains continuous when a gesture transfers from Drawer list scrolling.
- In normal Home mode, an upward drag can begin over every Launcher4Max-managed Home element without also activating or long-pressing that element. After the long-press haptic on a fully presented favorite, movement beyond the touch slop reorders instead of scrolling or opening Drawer.
- Drawer cannot be dismissed by a downward list gesture until its list has reached the top boundary; remaining displacement then transfers in the same gesture.
- Opening or closing Drawer does not accidentally launch, long-press, or scroll an application entry.
- With Launcher4Max selected as the default Launcher, the Android system Home action from Drawer and Settings displays Home; it does not depend on the Launcher4Max Back action.
- Returning from an ordinary application in the same process displays Home without Loading and without restoring Drawer.
- Returning from a system or external interruption never restores Home edit mode, its open style settings panel, or an unpublished movement.
- The implementation may choose any architecture that satisfies these observable behaviors.
