package com.maxarcher.launcher.ui.home.components

import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.OrderedFavoriteAggregate
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.OrderedFavoriteModuleType

internal fun List<OrderedFavoriteModule>.withPresentationFrom(
    aggregate: OrderedFavoriteAggregate,
): List<OrderedFavoriteModule> {
    val presentedById = aggregate.modules.associateBy(OrderedFavoriteModule::id)
    return map { module ->
        val presented = presentedById[module.id]
        if (presented == null || presented.type != module.type) {
            module
        } else if (
            module.name == presented.name &&
            module.nameVisible == presented.nameVisible &&
            (module.type != OrderedFavoriteModuleType.Ribbon ||
                module.displayMode == presented.displayMode) &&
            (module.type != OrderedFavoriteModuleType.Vertical ||
                module.iconSize == presented.iconSize &&
                module.textSize == presented.textSize &&
                module.namePlacement == presented.namePlacement &&
                module.itemsPerRow == presented.itemsPerRow)
        ) {
            module
        } else if (module.type != OrderedFavoriteModuleType.Vertical) {
            module.copy(
                name = presented.name,
                nameVisible = presented.nameVisible,
                displayMode = presented.displayMode,
            )
        } else {
            module.copy(
                iconSize = presented.iconSize,
                textSize = presented.textSize,
                namePlacement = presented.namePlacement,
                itemsPerRow = presented.itemsPerRow,
                name = presented.name,
                nameVisible = presented.nameVisible,
            )
        }
    }
}

internal fun LaunchableIdentity.stableKey(): String =
    "$profileSerialNumber:${componentName.flattenToString()}"
