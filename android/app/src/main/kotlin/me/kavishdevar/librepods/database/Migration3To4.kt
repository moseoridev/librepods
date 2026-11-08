package me.kavishdevar.librepods.database

import androidx.room3.DeleteColumn
import androidx.room3.migration.AutoMigrationSpec

/**
 * Drops the swipe-anywhere setting now that back navigation uses the platform
 * predictive back gesture instead of a custom swipe handler.
 */
@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "AppSettingsEntity",
        columnName = "swipeAnywhereForBack"
    )
)
internal class Migration3To4 : AutoMigrationSpec
