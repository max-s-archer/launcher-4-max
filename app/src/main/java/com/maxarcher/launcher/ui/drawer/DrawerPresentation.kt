package com.maxarcher.launcher.ui.drawer

import android.icu.text.Transliterator
import java.text.Collator
import java.util.Locale
import com.maxarcher.launcher.LaunchableEntry
import com.maxarcher.launcher.LaunchableIdentity

internal data class DrawerSection(
    val label: String,
    val entries: List<LaunchableEntry>,
)

internal data class DrawerListPosition(
    val sectionLabel: String,
    val relativeItemIndex: Int,
    val scrollOffset: Int,
    val applicationIdentity: LaunchableIdentity? = null,
)

internal data class DrawerRestorationTarget(
    val itemIndex: Int,
    val scrollOffset: Int,
)

internal fun filterDrawerSections(
    sections: List<DrawerSection>,
    query: String,
): List<DrawerSection> {
    if (query.isBlank()) return sections

    return sections.mapNotNull { section ->
        val matchingEntries = section.entries.filter { entry ->
            drawerSearchMatchRanges(label = entry.label, query = query).isNotEmpty()
        }
        if (matchingEntries.isEmpty()) {
            null
        } else {
            section.copy(entries = matchingEntries)
        }
    }
}

internal fun drawerSearchMatchRanges(label: String, query: String): List<IntRange> {
    val normalizedQuery = normalizeDrawerSearchText(text = query.trim()).text
    if (normalizedQuery.isEmpty()) return emptyList()

    val normalizedLabel = normalizeDrawerSearchText(text = label)
    val ranges = mutableListOf<IntRange>()
    var searchStart = 0
    while (searchStart <= normalizedLabel.text.length - normalizedQuery.length) {
        val matchStart = normalizedLabel.text.indexOf(
            string = normalizedQuery,
            startIndex = searchStart,
        )
        if (matchStart < 0) break
        val matchEnd = matchStart + normalizedQuery.length - 1
        ranges += normalizedLabel.originalStarts[matchStart] until
            normalizedLabel.originalEnds[matchEnd]
        searchStart = matchStart + normalizedQuery.length
    }
    return ranges
}

private data class NormalizedDrawerSearchText(
    val text: String,
    val originalStarts: List<Int>,
    val originalEnds: List<Int>,
)

private fun normalizeDrawerSearchText(text: String): NormalizedDrawerSearchText {
    val normalizer = checkNotNull(drawerSearchLatinNormalizer.get())
    val normalized = StringBuilder()
    val starts = mutableListOf<Int>()
    val ends = mutableListOf<Int>()
    var originalIndex = 0
    while (originalIndex < text.length) {
        val codePoint = text.codePointAt(originalIndex)
        val originalLength = Character.charCount(codePoint)
        val folded = normalizer.transliterate(
            String(Character.toChars(codePoint)),
        )
        folded.forEach { character ->
            val type = Character.getType(character)
            if (type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt() &&
                type != Character.ENCLOSING_MARK.toInt()
            ) {
                normalized.append(character)
                starts += originalIndex
                ends += originalIndex + originalLength
            }
        }
        originalIndex += originalLength
    }
    return NormalizedDrawerSearchText(
        text = normalized.toString(),
        originalStarts = starts,
        originalEnds = ends,
    )
}

private val drawerSearchLatinNormalizer = ThreadLocal.withInitial {
    Transliterator.getInstance("Latin-ASCII; Lower")
}

internal class LaunchableLabelNormalizer {
    private val transliterator = Transliterator.getInstance("Han-Latin; Latin-ASCII; Lower")

    fun normalize(label: String): String = transliterator.transliterate(label)

    fun sectionFor(label: String): String {
        val firstCharacter = normalize(label).firstOrNull()?.uppercaseChar()
        return if (firstCharacter != null && firstCharacter in 'A'..'Z') {
            firstCharacter.toString()
        } else {
            NUMBER_SECTION_LABEL
        }
    }

    private companion object {
        const val NUMBER_SECTION_LABEL = "#"
    }
}

internal fun buildDrawerSections(
    entries: List<LaunchableEntry>,
    locale: Locale,
): List<DrawerSection> {
    val normalizer = LaunchableLabelNormalizer()
    val collator = Collator.getInstance(locale).apply {
        strength = Collator.PRIMARY
    }
    val normalizedEntries = entries.map { entry ->
        NormalizedDrawerEntry(
            entry = entry,
            normalizedLabel = normalizer.normalize(entry.label),
        )
    }.sortedWith(Comparator { left, right ->
        val labelOrder = collator.compare(left.normalizedLabel, right.normalizedLabel)
        if (labelOrder != 0) {
            labelOrder
        } else {
            compareValuesBy(
                left.entry,
                right.entry,
                { it.identity.profileSerialNumber },
                { it.identity.componentName.flattenToString() },
            )
        }
    })
    val entriesBySection = normalizedEntries.groupBy { normalized ->
        normalizer.sectionForNormalized(normalized.normalizedLabel)
    }

    return buildList {
        entriesBySection["#"]
            ?.takeIf { it.isNotEmpty() }
            ?.let { sectionEntries ->
                add(
                    DrawerSection(
                        label = "#",
                        entries = sectionEntries.map(NormalizedDrawerEntry::entry),
                    ),
                )
            }
        for (sectionCharacter in 'A'..'Z') {
            val label = sectionCharacter.toString()
            val sectionEntries = entriesBySection[label].orEmpty()
            if (sectionEntries.isNotEmpty()) {
                add(
                    DrawerSection(
                        label = label,
                        entries = sectionEntries.map(NormalizedDrawerEntry::entry),
                    ),
                )
            }
        }
    }
}

private data class NormalizedDrawerEntry(
    val entry: LaunchableEntry,
    val normalizedLabel: String,
)

private fun LaunchableLabelNormalizer.sectionForNormalized(normalizedLabel: String): String {
    val firstCharacter = normalizedLabel.firstOrNull()?.uppercaseChar()
    return if (firstCharacter != null && firstCharacter in 'A'..'Z') {
        firstCharacter.toString()
    } else {
        "#"
    }
}

internal data class DrawerSectionRange(
    val label: String,
    val startIndex: Int,
    val itemCount: Int,
    val isSettings: Boolean = false,
) {
    val endIndex: Int get() = startIndex + itemCount
}

internal val DrawerSectionAnchorPresentation.headerItemCount: Int
    get() = if (this == DrawerSectionAnchorPresentation.Inline) 1 else 0

internal fun drawerSectionRanges(
    sections: List<DrawerSection>,
    itemsPerRow: Int,
    anchorPresentation: DrawerSectionAnchorPresentation,
    includeSettings: Boolean,
): List<DrawerSectionRange> = buildList {
    require(itemsPerRow > 0)
    var startIndex = 0
    sections.forEach { section ->
        val itemCount = anchorPresentation.headerItemCount +
            drawerApplicationRowCount(section.entries.size, itemsPerRow)
        add(DrawerSectionRange(section.label, startIndex, itemCount))
        startIndex += itemCount
    }
    if (includeSettings) {
        add(DrawerSectionRange(
            label = SETTINGS_SECTION_POSITION_LABEL,
            startIndex = startIndex,
            itemCount = anchorPresentation.headerItemCount + 1,
            isSettings = true,
        ))
    }
}

internal fun captureDrawerListPosition(
    sections: List<DrawerSection>,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    itemsPerRow: Int = 1,
    anchorPresentation: DrawerSectionAnchorPresentation = DrawerSectionAnchorPresentation.Inline,
    preserveApplicationIdentity: Boolean = false,
): DrawerListPosition? {
    require(itemsPerRow > 0)
    val ranges = drawerSectionRanges(sections, itemsPerRow, anchorPresentation, false)
    ranges.forEachIndexed { index, range ->
        if (firstVisibleItemIndex in range.startIndex until range.endIndex) {
            val row = firstVisibleItemIndex - range.startIndex - anchorPresentation.headerItemCount
            val entryIndex = row * itemsPerRow
            return DrawerListPosition(
                sectionLabel = range.label,
                relativeItemIndex = if (row < 0) 0 else 1 + entryIndex,
                scrollOffset = firstVisibleItemScrollOffset,
                applicationIdentity = if (preserveApplicationIdentity && row >= 0) {
                    sections[index].entries.getOrNull(entryIndex)?.identity
                } else null,
            )
        }
    }
    return null
}

internal fun captureDrawerOrdinaryListPosition(
    sections: List<DrawerSection>,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    itemsPerRow: Int = 1,
    anchorPresentation: DrawerSectionAnchorPresentation = DrawerSectionAnchorPresentation.Inline,
    preserveApplicationIdentity: Boolean = false,
): DrawerListPosition? {
    captureDrawerListPosition(
        sections, firstVisibleItemIndex, firstVisibleItemScrollOffset, itemsPerRow,
        anchorPresentation, preserveApplicationIdentity,
    )?.let { return it }
    val settings = drawerSectionRanges(sections, itemsPerRow, anchorPresentation, true).last()
    return if (firstVisibleItemIndex in settings.startIndex until settings.endIndex) {
        DrawerListPosition(
            sectionLabel = SETTINGS_SECTION_POSITION_LABEL,
            relativeItemIndex = firstVisibleItemIndex - settings.startIndex +
                1 - anchorPresentation.headerItemCount,
            scrollOffset = firstVisibleItemScrollOffset,
        )
    } else null
}

internal fun resolveDrawerRestorationTarget(
    position: DrawerListPosition,
    sections: List<DrawerSection>,
    itemsPerRow: Int = 1,
    anchorPresentation: DrawerSectionAnchorPresentation = DrawerSectionAnchorPresentation.Inline,
): DrawerRestorationTarget? {
    require(itemsPerRow > 0)
    // Geometry changes preserve exact identity, even if an inventory update moved it.
    position.applicationIdentity?.let { identity ->
        sections.forEach { section ->
            val index = section.entries.indexOfFirst { it.identity == identity }
            if (index >= 0) {
                return resolveDrawerRestorationTarget(
                    position.copy(
                        sectionLabel = section.label,
                        relativeItemIndex = index + 1,
                        applicationIdentity = null,
                    ),
                    sections, itemsPerRow, anchorPresentation,
                )
            }
        }
    }
    val ranges = drawerSectionRanges(sections, itemsPerRow, anchorPresentation, false)
    ranges.forEachIndexed { index, range ->
        if (range.label == position.sectionLabel) {
            val entryIndex = position.relativeItemIndex.coerceIn(0, sections[index].entries.size)
            val rowIndex = if (entryIndex == 0) 0 else {
                anchorPresentation.headerItemCount + (entryIndex - 1) / itemsPerRow
            }
            return DrawerRestorationTarget(
                itemIndex = range.startIndex + rowIndex,
                scrollOffset = if (entryIndex == position.relativeItemIndex) position.scrollOffset else 0,
            )
        }
    }
    ranges.firstOrNull { drawerSectionRank(it.label) > drawerSectionRank(position.sectionLabel) }
        ?.let { return DrawerRestorationTarget(it.startIndex, 0) }
    return ranges.lastOrNull()?.let { DrawerRestorationTarget(it.endIndex - 1, 0) }
}

internal fun resolveDrawerOrdinaryRestorationTarget(
    position: DrawerListPosition,
    sections: List<DrawerSection>,
    itemsPerRow: Int = 1,
    anchorPresentation: DrawerSectionAnchorPresentation = DrawerSectionAnchorPresentation.Inline,
): DrawerRestorationTarget? {
    if (position.sectionLabel != SETTINGS_SECTION_POSITION_LABEL) {
        return resolveDrawerRestorationTarget(position, sections, itemsPerRow, anchorPresentation)
    }
    val settings = drawerSectionRanges(sections, itemsPerRow, anchorPresentation, true).last()
    return DrawerRestorationTarget(
        itemIndex = settings.startIndex +
            position.relativeItemIndex.coerceIn(0, anchorPresentation.headerItemCount),
        scrollOffset = position.scrollOffset,
    )
}

internal fun drawerApplicationRowCount(entryCount: Int, itemsPerRow: Int): Int {
    require(entryCount >= 0)
    require(itemsPerRow > 0)
    return (entryCount + itemsPerRow - 1) / itemsPerRow
}

private fun drawerSectionRank(label: String): Int =
    if (label == "#") 0 else (label.firstOrNull()?.code ?: Int.MAX_VALUE)

private const val SETTINGS_SECTION_POSITION_LABEL = "\u0000settings"

internal class LaunchableEntryComparator(
    locale: Locale,
    private val normalizer: LaunchableLabelNormalizer = LaunchableLabelNormalizer(),
) : Comparator<LaunchableEntry> {
    private val collator = Collator.getInstance(locale).apply {
        strength = Collator.PRIMARY
    }

    override fun compare(left: LaunchableEntry, right: LaunchableEntry): Int {
        val labelOrder = collator.compare(
            normalizer.normalize(left.label),
            normalizer.normalize(right.label),
        )
        if (labelOrder != 0) return labelOrder

        return compareValuesBy(
            left,
            right,
            { it.identity.profileSerialNumber },
            { it.identity.componentName.flattenToString() },
        )
    }
}
