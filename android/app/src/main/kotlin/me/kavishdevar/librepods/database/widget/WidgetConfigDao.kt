package me.kavishdevar.librepods.database.widget

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import androidx.room3.Transaction

@Dao
interface WidgetConfigDao {

    @Query("SELECT * FROM WidgetConfigEntity")
    suspend fun getAll(): List<WidgetConfigEntity>

    @Query("SELECT * FROM WidgetConfigEntity WHERE appWidgetId = :appWidgetId")
    suspend fun getWidgetById(appWidgetId: Int): WidgetConfigEntity?

    @Upsert
    suspend fun upsert(widgetConfig: WidgetConfigEntity)
    @Query("DELETE FROM WidgetConfigEntity WHERE appWidgetId IN (:ids)")
    suspend fun delete(ids: List<Int>)

    @Transaction
    suspend fun restore(oldIds: List<Int>, newIds: List<Int>) {
        val previous = oldIds.map { getWidgetById(it) }
        val existing = newIds.filter { it !in oldIds }.mapNotNull { getWidgetById(it)?.appWidgetId }.toSet()
        delete(oldIds)
        newIds.zip(previous).forEach { (id, old) ->
            if (old != null && id !in existing) upsert(old.copy(appWidgetId = id))
        }
    }
}
