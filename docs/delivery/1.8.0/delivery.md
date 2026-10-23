# Launcher4Max 1.8.0 Delivery

> Semantic source: English. Chinese counterpart: [delivery.zh-CN.md](delivery.zh-CN.md).
>
> This record defines the planned delivery boundary for 1.8.0. It does not authorize production implementation, commits, pushes, tags, or releases.

## Version intent

`1.8.0` consolidates the accepted product updates accumulated after the 1.7.0 public release. It improves icon-shape customization and Home structure, editing, placement, and loading behavior through five independently reviewable iterations. The already completed Drawer search-result bug fix in `a19eba4` is retained as prior work and is not selected as an iteration in this version.

## Delivery level

`Author daily-use baseline`, as defined by [release governance](../../release.md). This version is intended to be installed and accepted by the author on the primary device. A future public artifact, tag, or GitHub Release is outside this version unless separately authorized.

The version identity will be allocated at version closure. No `versionName` or `versionCode` is authorized by this planning record.

## Product references

- [Product overview](../../../overview.md)
- [Product foundation](../../requirements/product-foundation.md)
- [Home behavior](../../product/surfaces/home.md)
- [Drawer behavior](../../product/surfaces/drawer.md)
- [Settings behavior](../../product/surfaces/settings.md)
- [Application action sheet](../../product/surfaces/app-action-sheet.md)
- [Home bottom app bar](../../product/features/home-bottom-app-bar.md)
- [Icon shape](../../product/features/icon-shape.md)
- [Quick actions](../../product/features/quick-actions.md)
- [Home presentation](../../product/presentation/home.md)
- [Drawer presentation](../../product/presentation/drawer.md)
- [Settings presentation](../../product/presentation/settings.md)
- [Navigation](../../product/navigation.md)
- [Validation guide](../../validation.md)

## Included scope and user journey

The author can configure the contracted icon shape and see the result consistently in Home and Drawer. The author can use the Home bottom app bar and the revised Home edit-mode interaction, place favorites through the accepted placement rules, and use the resulting Flow or ribbon layout. Home loading, refresh, restoration, and integration issues discovered within these selected contracts are resolved in the final closure iteration without adding new product capability.

## Exclusions

- Module list collapse and expansion, collapsed-list state, collapsed-list persistence, and related interaction. This remains outside 1.8.0 until a complete product definition is accepted.
- Landscape, foldable-device adaptation, desktop shortcuts, and X4Max naming governance.
- The completed Drawer search-result action fix from `a19eba4`, which is prior work rather than an included iteration.
- New product capabilities discovered during closure; they require a new scope and destination.
- Public release artifact, GitHub Release, APK upload, and repository publication.

## Technical approach and risks

The five iterations run serially because Home layout, editing, placement, and loading share state and presentation surfaces. Each implementation line must contain the accepted product baseline recorded below before production implementation. The final iteration may correct only already-defined behavior and must return newly proposed capability to a separately authorized scope.

Primary risks are interaction overlap between the bottom app bar and edit mode, preserving favorite placement and layout invariants across existing module arrangements, icon-shape consistency across OEM-rendered application icons, and separating loading fixes from new product behavior.

## Included iterations

| Iteration | Status | Updated | Basis |
| --- | --- | --- | --- |
| [Iteration 35: Icon Shape Customization](iteration-35-icon-shape-customization.md) | `Completed` | 2026-10-16 | All changes accepted by the author and merged into `main`. |
| [Iteration 36: Home Bottom App Bar](iteration-36-home-bottom-app-bar.md) | `Completed` | 2026-10-20 | The author accepted the bar implementation. |
| [Iteration 37: Home Edit Mode Interaction](iteration-37-home-edit-mode-interaction.md) | `In Progress` | 2026-10-23 | The author authorized production implementation. |
| [Iteration 38: Home Favorite Placement and Layout](iteration-38-home-favorite-placement-layout.md) | `Planned` | 2026-10-14 | The accepted Home placement and layout updates form an independently reviewable placement journey. |
| [Iteration 39: Home Loading and Closure](iteration-39-home-loading-closure.md) | `Planned` | 2026-10-14 | The author requested a final iteration for Home loading and small defined-scope issues. |

## Iteration evidence and results

### Iteration 35: Icon Shape Customization

[Contract](iteration-35-icon-shape-customization.md). Status: `Completed`.
The author authorized implementation and the remaining code, test, and delivery-document closure.
Author-reported acceptance of all iteration 35 changes: `Passed` on 2026-10-16.

- Contract traceability: the original baseline `0e6ac891f26f0d7ea5d4e483d4d0916a99de23b6`
  and the accepted preview amendment `0ebf612e0553385bdce9e794a1f57500bf5feb8a` are both
  ancestors of final implementation `0647ce4d4c313b7dd6d60775429193f264fb2581`.
  The bounded 64dp/16dp/label and accessibility amendment was requested and accepted by the
  author within the existing scope; it is recorded in the iteration contract.
- Implementation commits: `a6c1226` (preview controls), `6a9ec2a` (preference persistence),
  `e4c198f` (shared icon application), and `4795bfb` (backup compatibility).
  Final implementation `0647ce4d4c313b7dd6d60775429193f264fb2581` aligns the shared inward
  panel edge across Home, Drawer, icon-shape settings, and the Quick action popup, and adds
  focused store and slider regression coverage.
- Agent static validation on macOS/zsh: `git diff --check`, added Kotlin line-length and
  Unicode checks, resource XML parsing and duplicate/reference checks, and affected local
  Markdown link checks. These checks concern source consistency, not runtime acceptance.
- Agent build, lint, and instrumented tests: `Not run`; no Gradle command was executed under
  the author's instruction. Test sources include store defaults, reload, invalid fields,
  read/write failure and retry, and shared slider cancellation and refresh.
- Author-reported regression and final change acceptance: `Passed` on 2026-10-16.
- Author-approved closure on 2026-10-16 is recorded in documentation commit
  `6843126a9f649ce5d2471940a450112c1264c007`.
  The implementation and closure documentation are synchronized to shared history and merged
  into `main` by `f8eb08e394e8eff42d3c8dbe96f5ba92fa5b2d01`.
- Iteration 36 retains responsibility for integration of its new bottom app bar.

### Iteration 36: Home Bottom App Bar

[Contract](iteration-36-home-bottom-app-bar.md). Status: `Completed`. The author authorized production implementation on 2026-10-16 within the accepted contract. The Settings persistence slice was accepted on 2026-10-19. The backup slice adds schema v6 Home bottom app bar fields, old-backup defaults, duplicate-identity rejection, and width-based restore truncation. Author-reported device acceptance of this backup slice: `Passed` on 2026-10-19. The Home presentation slice reads the persisted bar, renders fixed and semi-hidden states, and adds edit-mode add, remove, and in-bar reorder. The movement slice accepts a Drawer drop, moves favorites between the bar and modules, rejects a new member when the bar is full, and ends movement on Back before collapsing the bar. Focused checks cover insertion, full-bar rejection, reorder, Drawer drop, and exact-fit capacity. Author-reported device acceptance of these slices: `Passed` on 2026-10-20. Ordinary Home one-shot placement remains Iteration 38. The author closed the iteration on 2026-10-20.

### Iteration 37: Home Edit Mode Interaction

[Contract](iteration-37-home-edit-mode-interaction.md). Status: `In Progress`. The author authorized production implementation on 2026-10-23 within the accepted contract, in three slices. The module-name slice adds a persisted name and name visibility to every module. Ordered-favorite store schema v5 and backup schema v7 carry both fields. Earlier data and backups restore unnamed modules with hidden names. Present fields of the wrong type reject the backup. Ordinary Home shows a non-interactive `40dp` title row only while the name is shown, and uses the default label when the module is unnamed. Author-reported build and run of the changed tests: `Passed` on 2026-10-23. Device acceptance: not yet reported. Follow-up owner: project author. Final author decision: not applicable yet.

### Iteration 38: Home Favorite Placement and Layout

[Contract](iteration-38-home-favorite-placement-layout.md). Status: `Planned`. No implementation evidence exists yet. Follow-up owner: project author. Final author decision: not applicable yet.

### Iteration 39: Home Loading and Closure

[Contract](iteration-39-home-loading-closure.md). Status: `Planned`. No implementation evidence exists yet. Follow-up owner: project author. Final author decision: not applicable yet.

## Dependencies and sequence

Iteration 35 and Iteration 36 may be implemented and validated independently after the baseline is accepted; Iteration 36 must include the icon-shape integration for the bottom app bar. Iteration 37 depends on the layout and interaction boundary established by Iteration 36. Iteration 38 depends on the edit-mode interaction rules from Iteration 37. Iteration 39 is limited to regressions directly caused by Iterations 35–38 and depends on their integrated result. This sequence does not bind work to a branch, terminal, or contributor.

## Validation

The mandatory environment is the author's primary physical device. Each iteration requires author acceptance of its selected journey, with focused automated checks recommended for state, serialization, navigation, layout, and regression behavior. Gradle execution remains author-controlled. Unperformed checks must be recorded as `Not run`, `Unavailable`, or `Unknown`.

## Artifact and release requirements

This version uses the `Author daily-use baseline` gates. The accepted APK must record its application identity, version identifiers, source commit, signing category, installation result, known gaps, and primary-device acceptance. Formal release signing, artifact retention, tag, GitHub Release, and publication are not promoted gates for this version.

## Known limitations and legacy issues

- Broader OEM, API, locale, and device coverage remains unknown until performed.
- The parked Drawer slider test cluster remains a known limitation of regression signal.
- Module collapse and expansion remain intentionally outside this version.

## Completion criteria

- All five included iterations are `Completed` with author acceptance.
- The selected product contracts and implementation have no unresolved material mismatch.
- The primary-device journeys for icon shape, bottom app bar, edit mode, favorite placement, and Home loading are accepted.
- Version identifiers, source commit, signing category, known gaps, and validation evidence are recorded.
- The implementation and all applicable 1.8.0 delivery documentation are committed and synchronized to the author-designated shared Git history.

## Completion result

Version 1.8.0 is not yet complete. As of 2026-10-23, Iterations 35 and 36 are `Completed` and accepted by the author. Iteration 37 is `In Progress`. Iterations 38 and 39 remain `Planned`.
