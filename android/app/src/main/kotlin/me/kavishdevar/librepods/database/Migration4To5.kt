package me.kavishdevar.librepods.database

import androidx.room3.DeleteColumn
import androidx.room3.migration.AutoMigrationSpec

/**
 * Drops the BLE scan tuning columns. Nearby detection now uses the system's
 * low-power batched scan, so these values no longer influence anything.
 */
@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "AppSettingsEntity",
        columnName = "bleScanMode"
    ),
    DeleteColumn(
        tableName = "AppSettingsEntity",
        columnName = "bleReportDelay"
    )
)
internal class Migration4To5 : AutoMigrationSpec
