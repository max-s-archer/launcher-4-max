# Iteration 36: Home Bottom App Bar

> Applicable version contract: [1.8.0 delivery](delivery.md). This contract defines scope only and does not authorize implementation, commits, pushes, tags, or releases.

## Objective

Deliver the Home bottom app bar as an independent, persistent presentation and interaction surface.

## Product and version references

- Product-contract baseline: `0e6ac891f26f0d7ea5d4e483d4d0916a99de23b6`
- Applicable product documents: [Home bottom app bar](../../product/features/home-bottom-app-bar.md), [Home behavior](../../product/surfaces/home.md), [Home presentation](../../product/presentation/home.md), [Navigation](../../product/navigation.md)
- Applicable version contract: [1.8.0 delivery](delivery.md)
- All linked product documents are read at product-contract baseline `0e6ac891f26f0d7ea5d4e483d4d0916a99de23b6`.

## Observable outcome

The author can observe the bottom app bar in its contracted default, enabled, fixed, and semi-hidden states without losing Home content or violating the defined content gesture boundary.

## Included work

- Bottom app bar visibility, global enablement defaulting to off, semi-hidden presentation default, Medium icon-size default, and fixed/semi-hidden behavior.
- Placement, content relationship, interaction targets, and state persistence defined by the current contract.
- Home navigation, presentation, wireframe, bilingual resources, and focused tests.

## Excluded work

- Module titles and general edit-mode interaction, which belong to Iteration 37.
- Module collapse and expansion.
- Favorite placement and Flow layout.

## Technical change areas

Home layout and state, bottom app bar presentation, navigation, resources, persistence if required by the contract, and tests.

## Dependencies and sequence

Iteration 35 precedes this iteration in the version sequence. Iteration 37 consumes the layout boundary established here.

## Migration and compatibility impact

Missing new state uses the contracted default. Existing Home favorite state remains unchanged.

## Security, privacy, permission, and licensing impact

No new permission, network, account, or external service is introduced.

## Risks and unresolved decisions

The app bar must not change the allocated Home content gesture or create a second editing entry point outside the contract.

## Acceptance criteria

- A fresh configuration has the bar disabled, semi-hidden presentation selected, and Medium icon size.
- Enabling and disabling the bar does not reset its presentation mode, icon size, or ordered bar favorites.
- Fixed and semi-hidden states render and behave correctly.
- Home content and existing navigation remain usable in every app-bar state.
- State survives the contract-defined recreation and restart paths.

## Validation requirements

Validate fresh defaults, all app-bar states and transitions on the primary device, including the 16dp semi-hidden exposure, Home scrolling, Back, edit-mode expansion, restart, backup/restore of the bar settings and ordered favorites, and accessibility traversal.

## Related decisions and technical assessments

- [Home bottom app bar](../../product/features/home-bottom-app-bar.md)
- [Home behavior](../../product/surfaces/home.md)
