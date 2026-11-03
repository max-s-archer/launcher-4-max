# Application Action Sheet

> Public semantic source: English. Chinese counterpart: [app-action-sheet.zh-CN.md](app-action-sheet.zh-CN.md). Exact visual values are defined in the [application action sheet presentation specification](../presentation/app-action-sheet.md); the spatial sketch is the shared [application action sheet wireframe](../wireframes/app-action-sheet.txt), with reading rules in the [wireframe index](../low-fidelity-wireframes.md).

## Presentation

The application action sheet is a modal bottom sheet opened by long-pressing a fully presented
favorite in normal Home mode, or by long-pressing an application in ordinary Drawer mode and then
releasing without beginning [drag-to-favorite](drawer.md#drag-to-favorite). Movement after the Home
long-press haptic does not enter [one-shot operation](home.md#one-shot-operation). Movement beyond
the touch slop after the Drawer long-press haptic begins drag-to-favorite as defined by the
[Drawer interaction specification](drawer.md).
Home has normal, edit, and one-shot operation states. In edit mode, application long press is
reserved for movement wherever application-level editing is available. An open style settings
panel blocks application-level targets only inside its owning module; other modules retain
application-level editing. Neither edit mode nor one-shot operation opens this sheet.

- It uses a dark surface with light text and preserves light system status-bar icons.
- A light short horizontal drag handle appears at the top.
- A gray scrim covers the underlying surface.
- The sheet adopts standard Material 3 modal-bottom-sheet drag thresholds, velocity handling, rebound, and nested-scroll arbitration rather than defining independent product values. Scrollable sheet content consumes movement while it can scroll; a downward continuation from its top boundary may dismiss the sheet through that standard behavior.
- Selecting the scrim, completing the standard downward dismissal, or pressing Back dismisses it. Every dismissal path performs no unselected action and preserves the underlying surface position.
- Until dismissal, the underlying application surface cannot receive clicks, long presses, or scrolling.
- Accepting an action dismisses the sheet as part of that action's handoff; each action-specific rule below owns the resulting destination and failure behavior. Current product definition does not add separate success feedback.
- Invoking the selected ordinary application from Home or Drawer closes the sheet and creates the external application excursion defined in [Navigation](../navigation.md#android-system-home-action-and-external-application-return). Returning in the same process displays Home without Loading rather than restoring Drawer.

## Content order

From top to bottom:

1. Application name aligned left and the application-information control aligned right.
2. A light inset divider.
3. An optional application-shortcut region containing platform-provided shortcuts, each displayed with an icon plus name. When Launcher actions follow it, the region ends with the same inset divider.
4. Launcher actions in a horizontal row, each with its icon above its label. Home shows remove favorite, edit, and uninstall; Drawer shows uninstall only.

Exact icon, divider, and badge geometry belongs to the [application action sheet presentation specification](../presentation/app-action-sheet.md).

One optional informational region sits directly below the application-identity divider and before the application-shortcut region. It renders up to two non-interactive read-only lines in this order: the no-actions line, then the not-default line. The no-actions line is shown when the sheet presents no application shortcuts and no Launcher actions. The not-default line is shown when Launcher4Max is not the device's default launcher; both hosts evaluate this state when the sheet opens. Each line is one localized string — `No actions available` for the no-actions line and `Not set as the default launcher` for the not-default line — and both are plain informational text: they are not interaction targets, add no Toast or confirmation, and do not change any dismissal or action behavior. Exact line geometry belongs to the [application action sheet presentation specification](../presentation/app-action-sheet.md).

If the platform exposes no application shortcuts, omit the complete application-shortcut region and any divider owned by that region. The divider below application identity remains as the boundary before Launcher actions on both hosts, and when the application-shortcut region is present, the same divider separates it from the Launcher-action row on both hosts.

The current contract does not define a dedicated overflow interaction for unusually many application shortcuts. Such an interaction becomes an additive capability only after a real application demonstrates the need. Regardless of count, implementation must not crash or draw through protected system UI.

When the sheet's content exceeds its available height, only the application-shortcut region scrolls vertically. The application-identity row, informational lines, and applicable Launcher-action row remain visible. This bounded scrolling is ordinary content accommodation, not the dedicated unusual-shortcut overflow capability excluded above.

## Application identity and information

- The application name is not interactive.
- No application icon is shown in the header.
- For a cloned application, display the platform-provided clone badge independently at the Bottom Sheet's bottom-right corner to follow the identity treatment observed on the author's Samsung device.
- The badge aligns to the Bottom Sheet's bottom-right corner without an outward offset. Its exact visual region belongs to the application action sheet presentation specification.
- The badge is decorative, cannot receive input, does not occupy a Launcher-action slot, and does not reserve content-layout space. Primary applications do not show it.
- Selecting the information icon opens the system application-information surface.
- Returning from system application information does not restore the sheet and refreshes the affected application state.
- If the system application-information surface cannot be opened, show the short localized Toast `Unable to open application information`, close the sheet, preserve the underlying Home or Drawer position, and do not change favorite state.

## Launcher actions

- Launcher actions are available on both hosts. Home shows remove favorite, edit, and uninstall;
  Drawer shows uninstall only, in the leftmost slot. Normal Home can remove a favorite directly
  through this sheet or enter edit mode through Edit. Other favorite-management entries follow
  the Home and Drawer interaction specifications. Application information remains the
  system-management path for a Drawer entry.
- Home Launcher actions use five fixed horizontal slots ordered from left to right. Visible actions compact into the leftmost available slots; hidden actions leave no internal gap, and unused slots remain empty on the right. Actions do not redistribute evenly across the full width.
- Each Launcher action's complete icon-and-label item is one interaction target. The icon is not an independent or smaller touch target.
- At most five Launcher actions are defined; this limit does not apply to platform application shortcuts.
- On Home the action order is remove favorite, edit, then uninstall. Edit remains available for a single favorite; uninstall is hidden when unavailable, and later visible actions shift left.
- Selecting Edit closes the sheet and enters collapsed-panel application-level Home edit
  mode at the current main-list position and any Ribbon-layout ribbon positions. It does
  not select a module, scroll, reveal another module, or expand the style settings panel.
  A Flow-layout ribbon has no independent horizontal position. The same action requests
  one non-blocking inventory refresh without delaying entry.
- If that Edit-triggered refresh reliably confirms that the source identity disappeared, Home reconciles the favorite through its ordinary inventory rule and shows the localized short Toast `Application is no longer available`. A reliable result already available before the edit transition prevents entry and leaves normal Home after reconciliation; a confirmation arriving after entry follows Home's edit-mode inventory rule. An uncertain or failed read neither produces this Toast nor removes the favorite.
- Removing a favorite from Home closes the sheet, removes that Home entry, and uses the same localized removal Snackbar, latest-removal Undo snapshot, replacement, invalidation, restoration, and failure behavior defined by the [Home interaction specification](home.md#adding-removing-and-moving-applications).
- Uninstall is hidden for applications the system does not allow the user to uninstall or only permits disabling.
- For a cloned application, show uninstall only when the platform can be confirmed to address removal of that selected clone without uninstalling the primary application. If that guarantee is unavailable, hide uninstall and leave system management available through application information. Never degrade a clone-removal action into primary-application uninstall.
- Destructive confirmation and actual removal remain under system control.
- If the user cancels the system uninstall confirmation, return to the originating Home or Drawer position, do not restore the sheet, and refresh the application state.
- If the system uninstall surface cannot be opened, show the short localized Toast `Unable to open uninstall`, close the sheet, preserve the underlying Home or Drawer position, and do not change favorite state.
- Current product scope does not add separate failure handling for invocation of a platform-provided application shortcut.

## Icons

- Remove favorite: broken heart.
- Edit: vertical-direction arrows.
- Uninstall: trash.
- Application information: information symbol.

Final icon assets and accessibility descriptions belong to the visual and implementation specifications.
