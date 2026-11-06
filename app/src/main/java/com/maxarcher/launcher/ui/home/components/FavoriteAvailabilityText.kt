package com.maxarcher.launcher.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.R

/** The one favorite-availability label used by lists, ribbons, and movement previews. */
@Composable
internal fun favoriteAvailabilityDisplayText(
    availability: FavoriteAvailability,
): String {
    val entry = availability.presentationEntry
    return when (availability) {
        is FavoriteAvailability.Available -> availability.entry.label
        is FavoriteAvailability.Disabled -> entry?.let {
            stringResource(R.string.favorite_disabled_format, it.label)
        } ?: stringResource(R.string.favorite_application_disabled)

        is FavoriteAvailability.TemporarilyUnavailable,
        is FavoriteAvailability.Unknown,
        -> entry?.let {
            stringResource(R.string.favorite_unavailable_format, it.label)
        } ?: stringResource(R.string.favorite_application_unavailable)

        FavoriteAvailability.ConfirmedRemoved ->
            stringResource(R.string.favorite_application_unavailable)
    }
}
