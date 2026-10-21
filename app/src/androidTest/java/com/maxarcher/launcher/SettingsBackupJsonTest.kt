package com.maxarcher.launcher

import android.content.ComponentName
import java.time.LocalDateTime
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import com.maxarcher.launcher.ui.drawer.DrawerApplicationSize
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerNamePlacement
import com.maxarcher.launcher.ui.drawer.DrawerSectionAnchorPresentation

class SettingsBackupJsonTest {
    @Test
    fun roundTripPreservesSavedShapeInBothModes() {
        IconShapeClipMode.entries.forEach { mode ->
            listOf(0, 37, 100).forEach { clip ->
                val shape = IconShapeSettings(mode, clip)
                val document = SettingsBackupJson.serialize(
                    aggregate = testAggregate(),
                    settings = testSettings(),
                    iconShape = shape,
                )
                assertEquals(shape, SettingsBackupJson.parse(document)?.iconShape)
                val section = JSONObject(document).getJSONObject("iconShape")
                assertEquals(2, section.length())
                assertEquals(mode.storageValue, section.getString("mode"))
                assertEquals(clip, section.getInt("freeformClip"))
            }
        }
    }

    @Test
    fun oldSchemasWithoutShapeRestoreCompleteDefault() {
        (1..SettingsBackupJson.BACKUP_SCHEMA_VERSION).forEach { schema ->
            val document = validBackupDocument().put("schemaVersion", schema)
            document.remove("iconShape")
            assertEquals(
                IconShapeSettings(),
                SettingsBackupJson.parse(document.toString())?.iconShape,
            )
        }
    }

    @Test
    fun invalidShapeModeDefaultsIndependentlyOfValidProportion() {
        listOf(null, JSONObject.NULL, "", "unknown", 7, true, JSONObject()).forEach { mode ->
            val section = JSONObject().put("mode", mode).put("freeformClip", 23)
            val parsed = SettingsBackupJson.parse(
                validBackupDocument().put("iconShape", section).toString(),
            )
            assertEquals(IconShapeSettings(IconShapeClipMode.Oem, 23), parsed?.iconShape)
            assertEquals(testAggregate(), parsed?.aggregate)
            assertEquals(testSettings(), parsed?.settings)
        }
    }

    @Test
    fun invalidProportionsDefaultWithoutClampingOrChangingMode() {
        val invalid = listOf(
            null, JSONObject.NULL, "", "25", 25.5, -1, 101,
            Long.MAX_VALUE, true, JSONObject(), org.json.JSONArray(),
        )
        IconShapeClipMode.entries.forEach { mode ->
            invalid.forEach { clip ->
                val section = JSONObject().put("mode", mode.storageValue)
                    .put("freeformClip", clip)
                val document = validBackupDocument().put("iconShape", section)
                document.getJSONObject("quickActionBindings")
                    .put("doubleTap", "screen_lock")
                val parsed = SettingsBackupJson.parse(document.toString())
                assertEquals(IconShapeSettings(mode, 50), parsed?.iconShape)
                assertEquals(testAggregate(), parsed?.aggregate)
                assertEquals(testSettings(), parsed?.settings)
                assertEquals(QuickAction.ScreenLock, parsed?.bindings?.doubleTap)
            }
        }
    }

    @Test
    fun invalidShapeSectionDefaultsWithoutRejectingOtherSections() {
        listOf(JSONObject.NULL, "", 7, true, org.json.JSONArray()).forEach { section ->
            val parsed = SettingsBackupJson.parse(
                validBackupDocument().put("iconShape", section).toString(),
            )
            assertEquals(IconShapeSettings(), parsed?.iconShape)
            assertEquals(testAggregate(), parsed?.aggregate)
            assertEquals(testSettings(), parsed?.settings)
        }
    }

    @Test
    fun invalidShapeFieldsTogetherRestoreCompleteDefault() {
        val section = JSONObject().put("mode", "unknown").put("freeformClip", 101)
        val parsed = SettingsBackupJson.parse(
            validBackupDocument().put("iconShape", section).toString(),
        )
        assertEquals(IconShapeSettings(), parsed?.iconShape)
        assertEquals(testAggregate(), parsed?.aggregate)
        assertEquals(testSettings(), parsed?.settings)
    }

    @Test
    fun roundTripPreservesCompleteFavoriteDisplaySettingsAndBindings() {
        val aggregate = testAggregate()
        val settings = testSettings()
        val bindings = QuickActionBindings(
            doubleTap = QuickAction.ScreenLock,
            longPress = QuickAction.EditMode,
        )

        val parsed = SettingsBackupJson.parse(
            SettingsBackupJson.serialize(
                aggregate = aggregate,
                settings = settings,
                bindings = bindings,
            ),
        )

        assertNotNull(parsed)
        assertEquals(aggregate, parsed?.aggregate)
        assertEquals(settings, parsed?.settings)
        assertEquals(bindings, parsed?.bindings)
    }

    @Test
    fun higherSchemaVersionIsRejected() {
        val document = validBackupDocument()
            .put("schemaVersion", SettingsBackupJson.BACKUP_SCHEMA_VERSION + 1)

        assertNull(SettingsBackupJson.parse(document.toString()))
    }

    @Test
    fun lowerSchemaVersionIsAcceptedUnderCurrentSchema() {
        val document = validBackupDocument()
            .put("schemaVersion", SettingsBackupJson.BACKUP_SCHEMA_VERSION - 1)

        val parsed = SettingsBackupJson.parse(document.toString())

        assertNotNull(parsed)
    }

    @Test
    fun missingQuickActionBindingsSectionRestoresNoActionDefaults() {
        val document = validBackupDocument()
        document.remove("quickActionBindings")

        val parsed = SettingsBackupJson.parse(document.toString())

        assertNotNull(parsed)
        assertEquals(QuickActionBindings(), parsed?.bindings)
    }

    @Test
    fun schemaVersionOneWithoutBindingsImportsAsNoActionDefaults() {
        val document = validBackupDocument()
            .put("schemaVersion", 1)
        document.remove("quickActionBindings")

        val parsed = SettingsBackupJson.parse(document.toString())

        assertNotNull(parsed)
        assertEquals(QuickActionBindings(), parsed?.bindings)
    }

    @Test
    fun unrecognizedBindingValueFails() {
        val document = validBackupDocument()
        document.getJSONObject("quickActionBindings")
            .put("doubleTap", "launch_camera")

        assertNull(SettingsBackupJson.parse(document.toString()))
    }

    @Test
    fun missingFavoritesSectionRestoresAnEmptyFavoriteState() {
        val document = validBackupDocument()
        document.remove("favorites")

        val parsed = SettingsBackupJson.parse(document.toString())

        assertNotNull(parsed)
        assertEquals(OrderedFavoriteAggregate(), parsed?.aggregate)
    }

    @Test
    fun missingDisplaySettingsSectionRestoresDefaultSettings() {
        val document = validBackupDocument()
        document.remove("displaySettings")

        val parsed = SettingsBackupJson.parse(document.toString())

        assertNotNull(parsed)
        assertEquals(DrawerDisplaySettings(), parsed?.settings)
    }

    @Test
    fun missingFieldInsidePresentSectionRestoresThatFieldDefault() {
        val displaySettings = validBackupDocument()
            .getJSONObject("displaySettings")
        displaySettings.remove("backgroundOpacity")
        val document = validBackupDocument()
            .put("displaySettings", displaySettings)

        val parsed = SettingsBackupJson.parse(document.toString())

        assertNotNull(parsed)
        assertEquals(
            DrawerDisplaySettings.DEFAULT_BACKGROUND_OPACITY,
            parsed?.settings?.backgroundOpacity,
        )
    }

    @Test
    fun legacyApplicationSizeBackfillsIndependentIconAndTextSizes() {
        val displaySettings = validBackupDocument()
            .getJSONObject("displaySettings")
        displaySettings.remove("iconSize")
        displaySettings.remove("textSize")
        displaySettings.put("applicationSize", "large")

        val parsed = SettingsBackupJson.parse(
            validBackupDocument().put("displaySettings", displaySettings).toString(),
        )

        assertEquals(DrawerApplicationSize.Large, parsed?.settings?.iconSize)
        assertEquals(DrawerApplicationSize.Large, parsed?.settings?.textSize)
    }

    @Test
    fun legacyFavoriteApplicationSizeBackfillsIndependentIconAndTextSizes() {
        val document = validBackupDocument()
        val favorite = document
            .getJSONObject("favorites")
            .getJSONArray("modules")
            .getJSONObject(0)
        favorite.remove("iconSize")
        favorite.remove("textSize")
        favorite.put("applicationSize", "small")

        val parsed = SettingsBackupJson.parse(document.toString())

        assertEquals(FavoriteListSize.Small, parsed?.aggregate?.modules?.first()?.iconSize)
        assertEquals(FavoriteListSize.Small, parsed?.aggregate?.modules?.first()?.textSize)
    }

    @Test
    fun emptyBackupWithNoModulesIsValid() {
        val document = validBackupDocument()
            .put("favorites", JSONObject().put("modules", org.json.JSONArray()))

        val parsed = SettingsBackupJson.parse(document.toString())

        assertNotNull(parsed)
        assertEquals(0, parsed?.aggregate?.modules?.size)
    }

    @Test
    fun malformedJsonFails() {
        assertNull(SettingsBackupJson.parse("not a json document"))
    }

    @Test
    fun truncatedJsonFails() {
        val document = SettingsBackupJson.serialize(
            aggregate = testAggregate(),
            settings = testSettings(),
        )

        assertNull(SettingsBackupJson.parse(document.dropLast(8)))
    }

    @Test
    fun unrecognizedFieldValueFails() {
        val modules = validBackupDocument()
            .getJSONObject("favorites")
            .getJSONArray("modules")
        modules.getJSONObject(0).put("type", "spiral")

        assertNull(parseWithModules(modules))
    }

    @Test
    fun moduleWithEmptyIdentitiesFails() {
        val modules = validBackupDocument()
            .getJSONObject("favorites")
            .getJSONArray("modules")
        modules.getJSONObject(0).put("identities", org.json.JSONArray())

        assertNull(parseWithModules(modules))
    }

    @Test
    fun itemsPerRowOutsideValidRangeFails() {
        val modules = validBackupDocument()
            .getJSONObject("favorites")
            .getJSONArray("modules")
        modules.getJSONObject(0).put("itemsPerRow", 5)

        assertNull(parseWithModules(modules))
    }

    @Test
    fun hiddenPlacementAllowsUpToSixItemsPerRow() {
        val modules = validBackupDocument()
            .getJSONObject("favorites")
            .getJSONArray("modules")
        modules.getJSONObject(0)
            .put("namePlacement", "hidden")
            .put("itemsPerRow", 6)

        val parsed = parseWithModules(modules)

        assertEquals(FavoriteNamePlacement.Hidden, parsed?.aggregate?.modules?.first()?.namePlacement)
        assertEquals(6, parsed?.aggregate?.modules?.first()?.itemsPerRow)
    }

    @Test
    fun fileNameUsesContractedPrefixVersionAndTimestamp() {
        val name = backupFileName(
            versionName = "1.5.0",
            timestamp = LocalDateTime.of(2026, 9, 8, 9, 7),
        )

        assertEquals("launcher4max-backup-1.5.0-202609080907.json", name)
    }

    @Test
    fun fileNameTimestampIsStableAcrossLocalesAndHourFormats() {
        val defaultLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("th-TH-u-ca-buddhist-nu-thai"))

            val name = backupFileName(
                versionName = "1.5.0",
                timestamp = LocalDateTime.of(2026, 9, 8, 9, 7),
            )

            assertEquals("launcher4max-backup-1.5.0-202609080907.json", name)
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    @Test
    fun roundTripPreservesHomeBottomAppBarWithoutCapacity() {
        val aggregate = testAggregate().copy(
            homeBottomAppBar = HomeBottomAppBarState(
                enabled = true,
                presentation = HomeBottomAppBarPresentation.Fixed,
                iconSize = FavoriteListSize.Small,
                identities = listOf(identity(4)),
            ),
        )

        val parsed = SettingsBackupJson.parse(
            SettingsBackupJson.serialize(aggregate = aggregate, settings = testSettings()),
        )

        assertEquals(aggregate, parsed?.aggregate)
        val section = JSONObject(
            SettingsBackupJson.serialize(aggregate = aggregate, settings = testSettings()),
        ).getJSONObject("favorites").getJSONObject("homeBottomAppBar")
        assertEquals(false, section.has("capacity"))
    }

    @Test
    fun missingHomeBottomAppBarRestoresContractedDefaults() {
        val document = validBackupDocument()
        document.getJSONObject("favorites").remove("homeBottomAppBar")

        assertEquals(
            HomeBottomAppBarState(),
            SettingsBackupJson.parse(document.toString())?.aggregate?.homeBottomAppBar,
        )
    }

    @Test
    fun sharedHomeBottomAppBarIdentityRejectsBackup() {
        val document = validBackupDocument()
        val shared = JSONObject()
            .put("serial", 1)
            .put("component", identity(1).componentName.flattenToString())
        document.getJSONObject("favorites").getJSONObject("homeBottomAppBar")
            .put("identities", org.json.JSONArray().put(shared))

        assertNull(SettingsBackupJson.parse(document.toString()))
    }

    @Test
    fun restoreKeepsOnlyCapacityAndRejectsMissingWidth() {
        val identities = List(4) { index -> identity(index.toLong() + 10) }
        val aggregate = testAggregate().copy(
            homeBottomAppBar = HomeBottomAppBarState(identities = identities),
        )

        assertEquals(
            identities.take(2),
            aggregate.restoreHomeBottomAppBarForWidth(
                safeWidthDp = 120f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
                iconSizeDp = { 40f },
            )?.homeBottomAppBar?.identities,
        )
        assertNull(
            aggregate.restoreHomeBottomAppBarForWidth(
                safeWidthDp = 0f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
                iconSizeDp = { 40f },
            ),
        )
    }

    private fun validBackupDocument(): JSONObject =
        JSONObject(
            SettingsBackupJson.serialize(
                aggregate = testAggregate(),
                settings = testSettings(),
            ),
        )

    private fun parseWithModules(modules: org.json.JSONArray): SettingsBackupState? =
        SettingsBackupJson.parse(
            validBackupDocument()
                .put("favorites", JSONObject().put("modules", modules))
                .toString(),
        )

    private fun testAggregate() = OrderedFavoriteAggregate(
        modules = listOf(
            OrderedFavoriteModule(
                id = "vertical-list-1",
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(identity(1), identity(2)),
                iconSize = FavoriteListSize.Large,
                textSize = FavoriteListSize.Small,
                namePlacement = FavoriteNamePlacement.Below,
                itemsPerRow = 3,
            ),
            OrderedFavoriteModule(
                id = "favorite-bar-1",
                type = OrderedFavoriteModuleType.Ribbon,
                identities = listOf(identity(3)),
            ),
        ),
    )

    private fun testSettings() = DrawerDisplaySettings(
        iconSize = DrawerApplicationSize.Small,
        textSize = DrawerApplicationSize.Large,
        namePlacement = DrawerNamePlacement.Below,
        itemsPerRow = 2,
        sectionAnchorPresentation = DrawerSectionAnchorPresentation.LeftSide,
        backgroundOpacity = 100,
    )

    private fun identity(serial: Long) = LaunchableIdentity(
        serial,
        ComponentName("com.example", "Main"),
    )
}
