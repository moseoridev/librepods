package me.kavishdevar.librepods.database

import androidx.room3.DeleteColumn
import androidx.room3.migration.AutoMigrationSpec

/**
 * Drops the purchase columns. The fork has no paid tier, so no stored value
 * can enable or expire anything.
 */
@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "AppStateEntity",
        columnName = "isPremium"
    ),
    DeleteColumn(
        tableName = "AppStateEntity",
        columnName = "timeUntilFOSSPremiumExpiry"
    )
)
internal class Migration5To6 : AutoMigrationSpec
