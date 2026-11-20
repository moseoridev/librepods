package me.kavishdevar.librepods.migration

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.room3.withWriteTransaction
import me.kavishdevar.librepods.database.LibrePodsDatabase
import me.kavishdevar.librepods.presentation.widgets.NoiseControlWidget

/** Runs before repositories load. Commit the checkpoint only after the atomic DB import. */
object LegacyPreferencesMigration {
    suspend fun migrate(context: Context, database: LibrePodsDatabase) {
        val checkpoint = context.getSharedPreferences("rewrite_migration", Context.MODE_PRIVATE)
        if (checkpoint.getBoolean("legacy_settings_v1", false)) return
        val legacy = context.getSharedPreferences("settings", Context.MODE_PRIVATE).all
        val controls = context.getSharedPreferences("widgets_controls", Context.MODE_PRIVATE).all
        val manager = AppWidgetManager.getInstance(context)
        val installed = manager.getAppWidgetIds(ComponentName(context, NoiseControlWidget::class.java)).toSet()
        val plan = LegacySettingsMapper.plan(legacy, installed + LegacySettingsMapper.widgetIds(controls))
            ?: return
        database.withWriteTransaction {
            if (database.appSettingsDao().get() == null) database.appSettingsDao().upsert(plan.appSettings)
            if (database.appStateDao().get() == null) database.appStateDao().upsert(plan.appState)
            plan.device?.let { imported ->
                val current = database.appleDao().get(imported.macAddress)
                val merged = LegacySettingsMapper.mergeDevice(current, imported)
                if (merged !== current) database.appleDao().upsert(merged)
            }
            plan.widgets.forEach { widget ->
                if (database.widgetConfigDao().getWidgetById(widget.appWidgetId) == null)
                    database.widgetConfigDao().upsert(widget)
            }
        }
        // The controls widget reuses its appearance preferences. Keep those and the source settings/keys
        // so the import is reversible.
        check(checkpoint.edit().putBoolean("legacy_settings_v1", true).commit()) { "Cannot store migration checkpoint" }
    }
}
